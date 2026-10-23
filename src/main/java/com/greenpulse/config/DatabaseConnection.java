package com.greenpulse.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String BASE_URL = System.getenv().getOrDefault(
        "GREENPULSE_MYSQL_BASE_URL", "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_URL = System.getenv().getOrDefault(
        "GREENPULSE_MYSQL_URL", "jdbc:mysql://localhost:3306/greenpulse_db?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USER = System.getenv().getOrDefault("GREENPULSE_MYSQL_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("GREENPULSE_MYSQL_PASSWORD", "");

    private static volatile boolean initialized = false;

    private DatabaseConnection() {}

    /** Returns a caller-owned connection. Always close it with try-with-resources. */
    public static Connection getConnection() throws SQLException {
        ensureInitialized();
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    private static synchronized void ensureInitialized() throws SQLException {
        if (initialized) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection baseConn = DriverManager.getConnection(BASE_URL, USER, PASSWORD);
                 Statement stmt = baseConn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS greenpulse_db");
            }
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                SchemaManager.initialize(conn);
            }
            initialized = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Please check Maven dependencies.", e);
        }
    }
}
