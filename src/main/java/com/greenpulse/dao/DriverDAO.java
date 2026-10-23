package com.greenpulse.dao;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.model.Driver;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {

    /** Loads drivers with their assigned vehicle. Database errors are reported, not hidden behind made-up drivers. */
    public List<Driver> loadAllDrivers() throws SQLException {
        String query = "SELECT d.driver_id, d.driver_name, COALESCE(v.vehicle_name, d.assigned_vehicle) AS vehicle_name, " +
                       "d.phone, v.fuel_type, v.consumption_per_km " +
                       "FROM drivers d LEFT JOIN vehicles v ON v.vehicle_id = d.vehicle_id ORDER BY d.driver_id";
        List<Driver> drivers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                drivers.add(new Driver(
                        rs.getString("driver_id"),
                        rs.getString("driver_name"),
                        rs.getString("vehicle_name"),
                        rs.getString("phone"),
                        rs.getString("fuel_type"),
                        rs.getDouble("consumption_per_km")));
            }
        }
        return drivers;
    }
}
