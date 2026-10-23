package com.greenpulse.model;

public class Driver {
    private final String driverId;
    private final String driverName;
    private final String vehicleName;
    private final String phone;
    private final String fuelType;          // "DIESEL", "ELECTRIC" or null when no vehicle is assigned
    private final double consumptionPerKm;  // litres/km for DIESEL, kWh/km for ELECTRIC

    public Driver(String driverId, String driverName, String vehicleName, String phone,
                  String fuelType, double consumptionPerKm) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.vehicleName = vehicleName == null ? "No vehicle assigned" : vehicleName;
        this.phone = phone;
        this.fuelType = fuelType;
        this.consumptionPerKm = consumptionPerKm;
    }

    public String getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public String getVehicleName() { return vehicleName; }
    /** May be null when no phone number is on file. */
    public String getPhone() { return phone; }
    public boolean isElectric() { return "ELECTRIC".equalsIgnoreCase(fuelType); }
    public boolean hasFuelData() { return fuelType != null && consumptionPerKm > 0; }
    public double getConsumptionPerKm() { return consumptionPerKm; }

    /** Human-readable fuel or energy estimate for a trip, e.g. "Fuel: 8.2 L*". */
    public String describeFuelUse(double km) {
        if (!hasFuelData()) return "Fuel / energy estimate: —";
        double amount = km * consumptionPerKm;
        return isElectric()
                ? String.format(java.util.Locale.US, "Energy: %.1f kWh*", amount)
                : String.format(java.util.Locale.US, "Fuel: %.1f L*", amount);
    }

    @Override
    public String toString() { return driverName + " • " + vehicleName; }
}
