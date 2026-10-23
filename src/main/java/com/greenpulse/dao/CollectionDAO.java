package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import java.sql.*;
import java.util.Optional;

/** Records that a bin was emptied: keeps the history and resets the bin so the dashboard reflects reality. */
public class CollectionDAO {

    /**
     * Records a collection event, resets the bin's fill level (and contamination for recycling bins), logs the new
     * reading and writes an audit row, all in one transaction.
     *
     * @return the fill level the bin had before it was emptied, or empty if the bin does not exist or is archived
     */
    public Optional<Double> recordCollection(String binId, Integer actorUserId, String routeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Double> before = recordCollection(conn, binId, actorUserId, routeId);
                if (before.isPresent()) conn.commit(); else conn.rollback();
                return before;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Same as above but inside the caller's transaction: the caller must commit or roll back. */
    Optional<Double> recordCollection(Connection conn, String binId, Integer actorUserId, String routeId) throws SQLException {
        if (binId == null || binId.isBlank()) throw new IllegalArgumentException("A bin ID is required.");
        String id = binId.trim();
        double fillBefore;
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT fill_percentage FROM smart_bins WHERE bin_id = ? AND is_active = TRUE FOR UPDATE")) {
            sel.setString(1, id);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                fillBefore = rs.getDouble(1);
            }
        }
        try (PreparedStatement ev = conn.prepareStatement(
                "INSERT INTO collection_events (bin_id, collected_by, route_id, fill_before) VALUES (?, ?, ?, ?)")) {
            ev.setString(1, id);
            if (actorUserId == null) ev.setNull(2, Types.INTEGER); else ev.setInt(2, actorUserId);
            if (routeId == null) ev.setNull(3, Types.VARCHAR); else ev.setString(3, routeId);
            ev.setDouble(4, fillBefore);
            ev.executeUpdate();
        }
        try (PreparedStatement reset = conn.prepareStatement(
                "UPDATE smart_bins SET fill_percentage = 0, " +
                "contamination_percentage = IF(contamination_percentage IS NULL, NULL, 0) WHERE bin_id = ?")) {
            reset.setString(1, id);
            reset.executeUpdate();
        }
        try (PreparedStatement log = conn.prepareStatement(
                "INSERT INTO bin_telemetry_logs (bin_id, fill_percentage, temperature, moisture, contamination, source) " +
                "SELECT bin_id, fill_percentage, temp_celsius, moisture_level, contamination_percentage, 'COLLECTION' " +
                "FROM smart_bins WHERE bin_id = ?")) {
            log.setString(1, id);
            log.executeUpdate();
        }
        AuditLog.record(conn, actorUserId, "BIN_COLLECTED", "BIN", id,
                String.format(java.util.Locale.US, "fill before: %.1f%%", fillBefore));
        return Optional.of(fillBefore);
    }
}
