package com.greenpulse.config;

import com.greenpulse.service.PasswordUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates and upgrades the schema from the single {@code /schema.sql} resource, adds columns that
 * databases created by older GreenPulse versions are missing, and seeds demo data once.
 */
final class SchemaManager {
    private static final Logger LOG = Logger.getLogger(SchemaManager.class.getName());

    private SchemaManager() {}

    static void initialize(Connection conn) throws SQLException {
        runSchemaScript(conn);
        upgradeLegacyTables(conn);
        seedDemoData(conn);
    }

    private static void runSchemaScript(Connection conn) throws SQLException {
        for (String statement : readStatements()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(statement);
            }
        }
    }

    private static List<String> readStatements() throws SQLException {
        try (InputStream in = SchemaManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new SQLException("schema.sql was not found on the classpath.");
            StringBuilder sql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("--")) sql.append(line).append('\n');
                }
            }
            List<String> statements = new ArrayList<>();
            for (String part : sql.toString().split(";")) {
                if (!part.isBlank()) statements.add(part.trim());
            }
            return statements;
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql: " + e.getMessage(), e);
        }
    }

    /** Databases made by earlier versions already have the tables, so CREATE IF NOT EXISTS skipped the new columns. */
    private static void upgradeLegacyTables(Connection conn) throws SQLException {
        ensureColumn(conn, "smart_bins", "latitude", "DECIMAL(9,6) NULL");
        ensureColumn(conn, "smart_bins", "longitude", "DECIMAL(9,6) NULL");
        ensureColumn(conn, "smart_bins", "optical_sensor_id", "VARCHAR(50) NULL");
        ensureColumn(conn, "users", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(conn, "users", "last_login", "TIMESTAMP NULL DEFAULT NULL");
        ensureColumn(conn, "bin_telemetry_logs", "source",
                "ENUM('SENSOR','SIMULATOR','MANUAL','COLLECTION') NOT NULL DEFAULT 'SENSOR'");
        ensureColumn(conn, "drivers", "phone", "VARCHAR(30) NULL");
        ensureColumn(conn, "drivers", "user_id", "INT NULL");
        ensureColumn(conn, "drivers", "vehicle_id", "VARCHAR(30) NULL");
        ensureIndex(conn, "bin_telemetry_logs", "idx_telemetry_bin_time", "(bin_id, logged_at)");
        // Route progress tracking (added after routes/route_stops first shipped)
        ensureColumn(conn, "route_stops", "status", "ENUM('PENDING','COLLECTED','SKIPPED') NOT NULL DEFAULT 'PENDING'");
        ensureColumn(conn, "route_stops", "skip_reason", "VARCHAR(120) NULL");
        ensureColumn(conn, "route_stops", "resolved_at", "TIMESTAMP NULL DEFAULT NULL");
        ensureEnumValue(conn, "routes", "status", "IN_PROGRESS",
                "ENUM('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PLANNED'");
    }

    private static void ensureColumn(Connection conn, String table, String column, String definition) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            if (rs.next()) return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            LOG.info("Upgraded table " + table + ": added column " + column);
        }
    }

    /** Widens an ENUM column when an older database does not yet allow the given value. */
    private static void ensureEnumValue(Connection conn, String table, String column, String value, String newDefinition) throws SQLException {
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString(1).contains("'" + value + "'")) return;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + newDefinition);
            LOG.info("Upgraded table " + table + ": column " + column + " now allows " + value);
        }
    }

    private static void ensureIndex(Connection conn, String table, String index, String columns) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getIndexInfo(conn.getCatalog(), null, table, false, false)) {
            while (rs.next()) {
                if (index.equalsIgnoreCase(rs.getString("INDEX_NAME"))) return;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE INDEX " + index + " ON " + table + " " + columns);
        }
    }

    private static void seedDemoData(Connection conn) throws SQLException {
        int userCount;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            userCount = rs.getInt(1);
        }
        if (userCount == 0) {
            String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
            String[][] demo = {
                    {"admin", "admin123", "Ana Reyes", "ADMIN"},
                    {"staff", "staff123", "Juan Staff", "STAFF"},
                    {"driver", "driver123", "Carlos Driver", "DRIVER"}};
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String[] u : demo) {
                    ps.setString(1, u[0]);
                    ps.setString(2, PasswordUtil.hash(u[1]));
                    ps.setString(3, u[2]);
                    ps.setString(4, u[3]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            LOG.info("Created demo accounts admin / staff / driver. Change these passwords before any real use.");
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                INSERT INTO vehicles (vehicle_id, vehicle_name, fuel_type, consumption_per_km) VALUES
                ('VEH-01', 'EcoTruck-01 (Compactor)', 'DIESEL', 0.32),
                ('VEH-02', 'EcoTruck-02 (Electric)', 'ELECTRIC', 1.20),
                ('VEH-03', 'EcoTruck-03 (Heavy Duty)', 'DIESEL', 0.42)
                ON DUPLICATE KEY UPDATE vehicle_name = vehicle_name
                """);
            stmt.executeUpdate("""
                INSERT INTO drivers (driver_id, driver_name, assigned_vehicle, vehicle_id) VALUES
                ('DRV-101', 'Carlos Driver', 'EcoTruck-01 (Compactor)', 'VEH-01'),
                ('DRV-102', 'Elena Rostova', 'EcoTruck-02 (Electric)', 'VEH-02'),
                ('DRV-103', 'Marcus Vance', 'EcoTruck-03 (Heavy Duty)', 'VEH-03')
                ON DUPLICATE KEY UPDATE vehicle_id = COALESCE(vehicle_id, VALUES(vehicle_id))
                """);
            stmt.executeUpdate("""
                UPDATE drivers SET user_id = (SELECT user_id FROM users WHERE username = 'driver')
                WHERE driver_id = 'DRV-101' AND user_id IS NULL
                """);
        }
    }
}
