package com.greenpulse.dao;

import com.greenpulse.analytics.AnalyticsData;
import com.greenpulse.analytics.AnalyticsData.CollectionPoint;
import com.greenpulse.analytics.AnalyticsData.TelemetryPoint;
import com.greenpulse.analytics.AnalyticsDataSource;
import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.model.SmartBin;

import java.sql.*;
import java.util.*;

/**
 * Reads history for the analytics page. To keep long periods fast, only the last reading of each bin in each
 * time bucket is fetched, never every raw reading.
 */
public class AnalyticsDAO implements AnalyticsDataSource {

    private static final String POINTS_SQL =
            "SELECT l.bin_id, UNIX_TIMESTAMP(l.logged_at) AS ts, l.fill_percentage " +
            "FROM bin_telemetry_logs l " +
            "JOIN (SELECT bin_id, MAX(log_id) AS last_id FROM bin_telemetry_logs " +
            "      WHERE logged_at >= DATE_SUB(NOW(), INTERVAL ? SECOND) AND (? OR source <> 'SIMULATOR') " +
            "      GROUP BY bin_id, FLOOR(UNIX_TIMESTAMP(logged_at) / ?)) x ON x.last_id = l.log_id " +
            "JOIN smart_bins b ON b.bin_id = l.bin_id AND b.is_active = TRUE " +
            "ORDER BY l.log_id";

    private static final String BASELINE_SQL =
            "SELECT l.bin_id, l.fill_percentage " +
            "FROM bin_telemetry_logs l " +
            "JOIN (SELECT bin_id, MAX(log_id) AS last_id FROM bin_telemetry_logs " +
            "      WHERE logged_at < DATE_SUB(NOW(), INTERVAL ? SECOND) AND (? OR source <> 'SIMULATOR') " +
            "      GROUP BY bin_id) x ON x.last_id = l.log_id " +
            "JOIN smart_bins b ON b.bin_id = l.bin_id AND b.is_active = TRUE";

    private static final String COLLECTIONS_SQL =
            "SELECT e.bin_id, UNIX_TIMESTAMP(e.collected_at) AS ts, e.fill_before " +
            "FROM collection_events e JOIN smart_bins b ON b.bin_id = e.bin_id AND b.is_active = TRUE " +
            "WHERE e.collected_at >= DATE_SUB(NOW(), INTERVAL ? SECOND) ORDER BY e.collected_at";

    @Override
    public AnalyticsData load(long windowSeconds, boolean includeSimulated) throws Exception {
        long bucketSeconds = Math.max(30, windowSeconds / 60);
        List<SmartBin> bins = new SmartBinDAO().loadAll();
        try (Connection conn = DatabaseConnection.getConnection()) {
            long now;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT UNIX_TIMESTAMP()")) {
                rs.next();
                now = rs.getLong(1);
            }

            Map<String, Double> baseline = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(BASELINE_SQL)) {
                ps.setLong(1, windowSeconds);
                ps.setBoolean(2, includeSimulated);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) baseline.put(rs.getString(1), rs.getDouble(2));
                }
            }

            List<TelemetryPoint> points = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(POINTS_SQL)) {
                ps.setLong(1, windowSeconds);
                ps.setBoolean(2, includeSimulated);
                ps.setLong(3, bucketSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) points.add(new TelemetryPoint(rs.getString(1), rs.getLong(2), rs.getDouble(3)));
                }
            }

            List<CollectionPoint> collections = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(COLLECTIONS_SQL)) {
                ps.setLong(1, windowSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) collections.add(new CollectionPoint(rs.getString(1), rs.getLong(2), rs.getDouble(3)));
                }
            }
            return new AnalyticsData(now, now - windowSeconds, bins, baseline, points, collections);
        }
    }
}
