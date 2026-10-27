package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.model.Role;
import com.greenpulse.model.User;
import com.greenpulse.service.AccountRules;
import com.greenpulse.service.PasswordUtil;
import java.sql.*;

public class UserDAO {
    /**
     * Creates a new account with a salted PBKDF2 password. A taken username (MySQL compares them without case)
     * is rejected with a message the sign-up form can show as it is.
     */
    public User register(String fullName, String username, String password, Role role) throws SQLException {
        String problem = AccountRules.problemWith(fullName, username, password, role);
        if (problem != null) throw new IllegalArgumentException(problem);
        String name = fullName.trim();
        String login = username.trim();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, login);
                stmt.setString(2, PasswordUtil.hash(password));
                stmt.setString(3, name);
                stmt.setString(4, role.name());
                stmt.executeUpdate();
                int userId;
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("MySQL did not return the new user's ID.");
                    userId = keys.getInt(1);
                }
                AuditLog.record(conn, userId, "ACCOUNT_CREATED", "USER", login, role.name());
                conn.commit();
                return new User(userId, login, name, role);
            } catch (SQLException ex) {
                conn.rollback();
                if (ex.getErrorCode() == 1062) { // MySQL duplicate-key error
                    throw new IllegalArgumentException("The username '" + login + "' is already taken. Choose another.", ex);
                }
                throw ex;
            }
        }
    }

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
