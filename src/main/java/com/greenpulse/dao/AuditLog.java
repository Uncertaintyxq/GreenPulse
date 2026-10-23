package com.greenpulse.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/** Writes audit rows inside the caller's transaction, so an action and its audit record commit or roll back together. */
final class AuditLog {
    private AuditLog() {}

    static void record(Connection conn, Integer userId, String action, String entityType, String entityId, String details) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO audit_log (user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)")) {
            if (userId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setString(4, entityId);
            ps.setString(5, details == null ? null : (details.length() > 255 ? details.substring(0, 255) : details));
            ps.executeUpdate();
        }
    }
}
