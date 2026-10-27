package com.greenpulse.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Opens MySQL connections. Each setting is read from, in order: a {@code -Dgreenpulse.<key>} system property, a
 * GREENPULSE_* environment variable, a greenpulse.properties file (in the folder the app starts in, then
 * ~/.greenpulse/), and finally the defaults for a local MySQL: user root with no password on localhost:3306.
 * IDEs start the app in the project folder, so greenpulse.properties there is the easy place to change them.
 */
public class DatabaseConnection {
    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000";
    private static final Properties FILE = loadSettingsFile();

    private static final String HOST = setting("mysql.host", "GREENPULSE_MYSQL_HOST", "localhost");
    private static final String PORT = setting("mysql.port", "GREENPULSE_MYSQL_PORT", "3306");
    private static final String DATABASE = databaseName(setting("mysql.database", "GREENPULSE_MYSQL_DATABASE", "greenpulse_db"));
    private static final String BASE_URL = setting("mysql.baseUrl", "GREENPULSE_MYSQL_BASE_URL", "jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTIONS);
    private static final String DB_URL = setting("mysql.url", "GREENPULSE_MYSQL_URL", "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + OPTIONS);
    private static final String USER = setting("mysql.user", "GREENPULSE_MYSQL_USER", "root");
    private static final String PASSWORD = setting("mysql.password", "GREENPULSE_MYSQL_PASSWORD", "");

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
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
            }
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                SchemaManager.initialize(conn);
            }
            initialized = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Please check Maven dependencies.", e);
        }
    }

    /** Turns a connection failure into one sentence that names the cause and what to change, for the sign-in screen. */
    public static String explain(Throwable failure) {
        SQLException sql = null;
        Throwable root = failure;
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof SocketTimeoutException) return notRunning();
            if (t instanceof UnknownHostException) return "Cannot find the database server \"" + HOST + "\". Check mysql.host in greenpulse.properties.";
            if (sql == null && t instanceof SQLException s) sql = s;
            root = t;
        }
        if (sql != null) {
            String state = sql.getSQLState() == null ? "" : sql.getSQLState();
            if (sql.getErrorCode() == 1045 || state.equals("28000"))
                return "MySQL refused the user \"" + USER + "\" or its password. Set mysql.user and mysql.password in greenpulse.properties.";
            if (sql.getErrorCode() == 1044 || sql.getErrorCode() == 1142)
                return "The MySQL user \"" + USER + "\" may not create or open " + DATABASE + ". Sign in to MySQL as root, or grant that user full rights.";
            if (state.startsWith("08")) return notRunning();
        }
        String message = root.getMessage();
        return "Cannot reach MySQL: " + (message == null || message.isBlank() ? root.getClass().getSimpleName() : message);
    }

    private static String notRunning() {
        return "MySQL is not answering at " + HOST + ":" + PORT + ". Start the MySQL server, then sign in again.";
    }

    // ------------------------------------------------------------------ settings

    private static String setting(String key, String envName, String fallback) {
        String value = System.getProperty("greenpulse." + key);
        if (value == null) value = System.getenv(envName);
        if (value == null) value = FILE.getProperty(key);
        return value != null ? value.trim() : fallback;
    }

    private static Properties loadSettingsFile() {
        Properties settings = new Properties();
        for (Path file : new Path[]{Path.of("greenpulse.properties"), Path.of(System.getProperty("user.home"), ".greenpulse", "greenpulse.properties")}) {
            if (!Files.isRegularFile(file)) continue;
            try (InputStream in = Files.newInputStream(file)) {
                settings.load(in);
                System.err.println("[Database] Using settings from " + file.toAbsolutePath());
                return settings;
            } catch (IOException ex) {
                System.err.println("[Database] Could not read " + file.toAbsolutePath() + ": " + ex.getMessage());
            }
        }
        return settings;
    }

    /** The name goes into CREATE DATABASE, so only plain identifiers are accepted. */
    private static String databaseName(String name) {
        if (name.matches("[A-Za-z0-9_]+")) return name;
        System.err.println("[Database] Ignoring database name \"" + name + "\": use letters, digits and _ only.");
        return "greenpulse_db";
    }
}
