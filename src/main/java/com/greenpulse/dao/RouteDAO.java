package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.model.SmartBin;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stores planned routes and their ordered stops so dispatch history is not lost when the window closes. */
public class RouteDAO {
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Saves a route and its stops in one transaction and returns the new route ID. */
    public String savePlannedRoute(String driverId, Integer actorUserId, double depotLat, double depotLon,
                                   double distanceMeters, double durationSeconds, List<SmartBin> orderedBins) throws SQLException {
        String routeId = "RT-" + LocalDateTime.now().format(ID_TIME) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement route = conn.prepareStatement(
                         "INSERT INTO routes (route_id, driver_id, created_by, depot_latitude, depot_longitude, total_distance_m, est_duration_s) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)");
                 PreparedStatement stop = conn.prepareStatement(
                         "INSERT INTO route_stops (route_id, stop_sequence, bin_id, fill_at_planning, priority_at_planning) VALUES (?, ?, ?, ?, ?)")) {
                route.setString(1, routeId);
                if (driverId == null) route.setNull(2, Types.VARCHAR); else route.setString(2, driverId);
                if (actorUserId == null) route.setNull(3, Types.INTEGER); else route.setInt(3, actorUserId);
                route.setDouble(4, depotLat);
                route.setDouble(5, depotLon);
                route.setInt(6, (int) Math.round(distanceMeters));
                route.setInt(7, (int) Math.round(durationSeconds));
                route.executeUpdate();

                int seq = 1;
                for (SmartBin bin : orderedBins) {
                    stop.setString(1, routeId);
                    stop.setInt(2, seq++);
                    stop.setString(3, bin.getBinID());
                    stop.setDouble(4, bin.getFillPercentage());
                    stop.setInt(5, bin.calculatePriority());
                    stop.addBatch();
                }
                stop.executeBatch();
                AuditLog.record(conn, actorUserId, "ROUTE_PLANNED", "ROUTE", routeId, orderedBins.size() + " stop(s)");
                conn.commit();
                return routeId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /**
     * Collects one stop: records the collection, resets the bin, marks the stop done and updates the route's status,
     * all in one transaction. If the bin no longer exists the stop is closed as skipped.
     *
     * @return the bin's fill level before it was emptied, or empty if the bin was no longer in service
     * @throws IllegalStateException if the stop was already collected or skipped
     */
    public Optional<Double> collectStop(String routeId, String binId, Integer actorUserId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requirePendingStop(conn, routeId, binId);
                Optional<Double> before = new CollectionDAO().recordCollection(conn, binId, actorUserId, routeId);
                if (before.isPresent()) resolveStop(conn, routeId, binId, "COLLECTED", null);
                else resolveStop(conn, routeId, binId, "SKIPPED", "Bin no longer in service");
                refreshRouteStatus(conn, routeId);
                conn.commit();
                return before;
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Skips one stop with a reason (for example the bin was blocked) and updates the route's status. */
    public void skipStop(String routeId, String binId, String reason, Integer actorUserId) throws SQLException {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A reason is required to skip a stop.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requirePendingStop(conn, routeId, binId);
                resolveStop(conn, routeId, binId, "SKIPPED", reason.trim());
                refreshRouteStatus(conn, routeId);
                AuditLog.record(conn, actorUserId, "STOP_SKIPPED", "ROUTE", routeId, binId + ": " + reason.trim());
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private void requirePendingStop(Connection conn, String routeId, String binId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM route_stops WHERE route_id = ? AND bin_id = ? FOR UPDATE")) {
            ps.setString(1, routeId);
            ps.setString(2, binId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Bin " + binId + " is not a stop on route " + routeId + ".");
                String status = rs.getString(1);
                if (!"PENDING".equals(status)) {
                    throw new IllegalStateException("Stop " + binId + " was already " + status.toLowerCase() + ".");
                }
            }
        }
    }

    private void resolveStop(Connection conn, String routeId, String binId, String status, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE route_stops SET status = ?, skip_reason = ?, resolved_at = CURRENT_TIMESTAMP " +
                "WHERE route_id = ? AND bin_id = ? AND status = 'PENDING'")) {
            ps.setString(1, status);
            if (reason == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, reason);
            ps.setString(3, routeId);
            ps.setString(4, binId);
            ps.executeUpdate();
        }
    }

    /** A route is IN_PROGRESS once any stop is resolved and COMPLETED when none are left pending. */
    private void refreshRouteStatus(Connection conn, String routeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE routes SET status = CASE WHEN " +
                "(SELECT COUNT(*) FROM route_stops WHERE route_id = ? AND status = 'PENDING') = 0 " +
                "THEN 'COMPLETED' ELSE 'IN_PROGRESS' END " +
                "WHERE route_id = ? AND status IN ('PLANNED', 'IN_PROGRESS')")) {
            ps.setString(1, routeId);
            ps.setString(2, routeId);
            ps.executeUpdate();
        }
    }
}
