package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.exception.InvalidFillLevelException;
import com.greenpulse.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Database access for smart bins. This class never checks which subclass a bin is: it asks the bin for its
 * {@link BinType} and {@link SensorReading}, so adding a new bin type does not require changing the DAO.
 */
public class SmartBinDAO {
    private static final String SELECT_ACTIVE = "SELECT * FROM smart_bins WHERE is_active = TRUE";

    /** Inserts a new bin. An existing ID (active or archived) is rejected instead of silently overwritten. */
    public void register(SmartBin bin, Integer actorUserId) throws SQLException {
        String sql = "INSERT INTO smart_bins (bin_id, bin_type, capacity_litres, fill_percentage, location, " +
                     "contamination_percentage, moisture_level, temp_celsius, optical_sensor_id, latitude, longitude) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                SensorReading reading = bin.getSensorReading();
                stmt.setString(1, bin.getBinID());
                stmt.setString(2, bin.getBinType().getDbValue());
                stmt.setDouble(3, bin.getCapacityLitres());
                stmt.setDouble(4, bin.getFillPercentage());
                stmt.setString(5, bin.getLocation());
                setNullableDouble(stmt, 6, reading.contamination());
                setNullableDouble(stmt, 7, reading.moisture());
                setNullableDouble(stmt, 8, reading.temperature());
                stmt.setString(9, bin.getSensorId());
                setNullableDouble(stmt, 10, bin.hasCoordinates() ? bin.getLatitude() : null);
                setNullableDouble(stmt, 11, bin.hasCoordinates() ? bin.getLongitude() : null);
                stmt.executeUpdate();
                AuditLog.record(conn, actorUserId, "BIN_REGISTERED", "BIN", bin.getBinID(),
                        bin.getBinType().getDbValue() + " at " + bin.getLocation());
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                if (ex.getErrorCode() == 1062) { // MySQL duplicate-key error
                    throw new IllegalArgumentException("A bin with ID '" + bin.getBinID() + "' already exists. Choose a different ID.", ex);
                }
                throw ex;
            }
        }
    }

    /**
     * Stores a new sensor reading for an existing active bin and appends it to the telemetry log in one transaction.
     * Only the reading columns are updated, so a stale in-memory copy can never overwrite a bin's location or GPS.
     *
     * @return false if the bin does not exist or is archived
     */
    public boolean recordReading(SmartBin bin, ReadingSource source) throws SQLException {
        String update = "UPDATE smart_bins SET fill_percentage = ?, contamination_percentage = ?, moisture_level = ?, " +
                        "temp_celsius = ? WHERE bin_id = ? AND is_active = TRUE";
        String log = "INSERT INTO bin_telemetry_logs (bin_id, fill_percentage, temperature, moisture, contamination, source) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        SensorReading reading = bin.getSensorReading();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement upd = conn.prepareStatement(update);
                 PreparedStatement logStmt = conn.prepareStatement(log)) {
                upd.setDouble(1, bin.getFillPercentage());
                setNullableDouble(upd, 2, reading.contamination());
                setNullableDouble(upd, 3, reading.moisture());
                setNullableDouble(upd, 4, reading.temperature());
                upd.setString(5, bin.getBinID());
                if (upd.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                logStmt.setString(1, bin.getBinID());
                logStmt.setDouble(2, bin.getFillPercentage());
                setNullableDouble(logStmt, 3, reading.temperature());
                setNullableDouble(logStmt, 4, reading.moisture());
                setNullableDouble(logStmt, 5, reading.contamination());
                logStmt.setString(6, source.name());
                logStmt.executeUpdate();
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Updates GPS coordinates for an existing bin, including legacy bins without coordinates. */
    public boolean updateCoordinates(String binId, double latitude, double longitude) throws SQLException {
        if (binId == null || binId.isBlank()) throw new IllegalArgumentException("A bin ID is required.");
        if (!Double.isFinite(latitude) || latitude < 4.5 || latitude > 21.5 ||
            !Double.isFinite(longitude) || longitude < 116.0 || longitude > 127.5) {
            throw new IllegalArgumentException("GreenPulse accepts coordinates inside the Philippines only (approx. latitude 4.5–21.5, longitude 116.0–127.5).");
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE smart_bins SET latitude = ?, longitude = ? WHERE bin_id = ?")) {
            stmt.setDouble(1, latitude);
            stmt.setDouble(2, longitude);
            stmt.setString(3, binId.trim());
            return stmt.executeUpdate() > 0;
        }
    }

    /** Soft delete: the bin disappears from the app but its telemetry and collection history are kept for reports. */
    public boolean archive(String binId, Integer actorUserId) throws SQLException {
        if (binId == null || binId.isBlank()) throw new IllegalArgumentException("A bin ID is required.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE smart_bins SET is_active = FALSE WHERE bin_id = ? AND is_active = TRUE")) {
                stmt.setString(1, binId.trim());
                boolean changed = stmt.executeUpdate() > 0;
                if (changed) AuditLog.record(conn, actorUserId, "BIN_ARCHIVED", "BIN", binId.trim(), null);
                conn.commit();
                return changed;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Permanently deletes a bin together with its telemetry, route stops and collection events (via ON DELETE CASCADE). */
    public boolean purge(String binId, Integer actorUserId) throws SQLException {
        if (binId == null || binId.isBlank()) throw new IllegalArgumentException("A bin ID is required.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement logs = conn.prepareStatement("DELETE FROM bin_telemetry_logs WHERE bin_id = ?");
                 PreparedStatement bin = conn.prepareStatement("DELETE FROM smart_bins WHERE bin_id = ?")) {
                // Telemetry is deleted explicitly so this also works on older databases created without ON DELETE CASCADE.
                logs.setString(1, binId.trim());
                logs.executeUpdate();
                bin.setString(1, binId.trim());
                boolean deleted = bin.executeUpdate() > 0;
                if (deleted) AuditLog.record(conn, actorUserId, "BIN_PURGED", "BIN", binId.trim(), "permanent delete");
                conn.commit();
                return deleted;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public List<SmartBin> loadAll() throws SQLException, InvalidFillLevelException {
        List<SmartBin> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ACTIVE + " ORDER BY bin_id")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Optional<SmartBin> findById(String binId) throws SQLException, InvalidFillLevelException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACTIVE + " AND bin_id = ?")) {
            stmt.setString(1, binId == null ? "" : binId.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** One random active bin, picked by the database so the simulator does not have to load every bin each tick. */
    public Optional<SmartBin> findRandomActive() throws SQLException, InvalidFillLevelException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ACTIVE + " ORDER BY RAND() LIMIT 1")) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    }

    private SmartBin mapRow(ResultSet rs) throws SQLException, InvalidFillLevelException {
        double lat = rs.getDouble("latitude");
        if (rs.wasNull()) lat = Double.NaN;
        double lon = rs.getDouble("longitude");
        if (rs.wasNull()) lon = Double.NaN;
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) { lat = Double.NaN; lon = Double.NaN; }
        SensorReading reading = new SensorReading(
                getNullableDouble(rs, "temp_celsius"),
                getNullableDouble(rs, "moisture_level"),
                getNullableDouble(rs, "contamination_percentage"));
        return SmartBinFactory.fromStoredReading(
                BinType.fromString(rs.getString("bin_type")),
                rs.getString("bin_id"), rs.getDouble("capacity_litres"), rs.getDouble("fill_percentage"),
                rs.getString("location"), reading, lat, lon);
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private static void setNullableDouble(PreparedStatement stmt, int index, Double value) throws SQLException {
        if (value == null) stmt.setNull(index, Types.DOUBLE); else stmt.setDouble(index, value);
    }
}
