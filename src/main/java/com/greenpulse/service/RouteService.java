package com.greenpulse.service;

import com.greenpulse.dao.RouteDAO;
import com.greenpulse.event.BinEventBus;
import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;
import com.greenpulse.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Business rules for running a route: saving it, collecting stops and skipping stops. Roles are checked here. */
public class RouteService {
    private final User user;
    private final RouteDAO routeDAO;

    public RouteService(User user) {
        this(user, new RouteDAO());
    }

    public RouteService(User user, RouteDAO routeDAO) {
        if (user == null) throw new IllegalArgumentException("A signed-in user is required.");
        this.user = user;
        this.routeDAO = routeDAO;
    }

    public String saveRoute(String driverId, double depotLat, double depotLon, double distanceMeters,
                            double durationSeconds, List<SmartBin> orderedBins) throws SQLException {
        return routeDAO.savePlannedRoute(driverId, user.getUserId(), depotLat, depotLon, distanceMeters, durationSeconds, orderedBins);
    }

    /**
     * Collects a stop. On success the in-memory bin is reset as well and listeners are told (so a critical alert
     * can fire again later). Returns the fill level the bin had before, or empty if the bin was no longer in service.
     */
    public Optional<Double> collectStop(String routeId, SmartBin bin) throws SQLException {
        requireCollector();
        Optional<Double> before = routeDAO.collectStop(routeId, bin.getBinID(), user.getUserId());
        if (before.isPresent()) {
            bin.resetAfterCollection();
            BinEventBus.getInstance().publish(bin, ReadingSource.COLLECTION);
        }
        return before;
    }

    public void skipStop(String routeId, String binId, String reason) throws SQLException {
        requireCollector();
        routeDAO.skipStop(routeId, binId, reason, user.getUserId());
    }

    private void requireCollector() {
        if (!user.getRoleType().canRecordCollections()) {
            throw new SecurityException("Your role (" + user.getRole() + ") is not allowed to record collections.");
        }
    }
}
