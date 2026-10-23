package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.model.User;
import com.greenpulse.service.PasswordUtil;
import java.sql.*;

public class UserDAO {
    public User authenticate(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) return null;
        String query = "SELECT user_id, username, password, full_name, role FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                String stored = rs.getString("password");
                int userId = rs.getInt("user_id");
                String fullName = rs.getString("full_name");
                String role = rs.getString("role");
                String foundUsername = rs.getString("username");
                if (!PasswordUtil.verify(password, stored)) return null;
                // Migrate old plaintext demo credentials to salted PBKDF2 after a successful login.
                if (!PasswordUtil.isHashed(stored)) {
                    try (PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
                        update.setString(1, PasswordUtil.hash(password));
                        update.setInt(2, userId);
                        update.executeUpdate();
                    }
                }
                try (PreparedStatement seen = conn.prepareStatement("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?")) {
                    seen.setInt(1, userId);
                    seen.executeUpdate();
                }
                AuditLog.record(conn, userId, "LOGIN", "USER", foundUsername, null);
                return new User(userId, foundUsername, fullName, role);
            }
        }
    }
}
