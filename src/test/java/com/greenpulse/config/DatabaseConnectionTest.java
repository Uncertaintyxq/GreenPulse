package com.greenpulse.config;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    @Test
    void stoppedServerIsReportedAsNotRunning() {
        SQLException linkFailure = new SQLException("Communications link failure", "08S01", 0, new ConnectException("Connection refused"));
        assertTrue(DatabaseConnection.explain(new RuntimeException(linkFailure)).startsWith("MySQL is not answering at"));
    }

    @Test
    void wrongPasswordPointsAtTheSettingsFile() {
        SQLException denied = new SQLException("Access denied for user 'root'@'localhost'", "28000", 1045);
        String message = DatabaseConnection.explain(denied);
        assertTrue(message.contains("refused the user"), message);
        assertTrue(message.contains("greenpulse.properties"), message);
    }

    @Test
    void unknownFailureKeepsTheDriverMessage() {
        String message = DatabaseConnection.explain(new SQLException("Table 'x' doesn't exist", "42S02", 1146));
        assertTrue(message.endsWith("Table 'x' doesn't exist"), message);
    }
}
