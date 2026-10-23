package com.greenpulse.model;

import com.greenpulse.exception.InvalidFillLevelException;
import java.util.Random;

public abstract class SmartBin {
    private String binID;
    private double capacityLitres;
    private double fillPercentage;
    private String location;
    private boolean isActive;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;

    public SmartBin(String binID, double capacityLitres, double fillPercentage, String location) throws InvalidFillLevelException {
        this(binID, capacityLitres, fillPercentage, location, Double.NaN, Double.NaN);
    }

    public SmartBin(String binID, double capacityLitres, double fillPercentage, String location,
                    double latitude, double longitude) throws InvalidFillLevelException {
        if (binID == null || binID.isBlank()) throw new IllegalArgumentException("Bin ID is required.");
        if (!Double.isFinite(capacityLitres) || capacityLitres <= 0) throw new IllegalArgumentException("Capacity must be greater than zero.");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("Bin location is required.");
        this.binID = binID.trim();
        this.capacityLitres = capacityLitres;
        this.location = location.trim();
        this.isActive = true;
        setCoordinates(latitude, longitude);
        setFillLevel(fillPercentage);
    }

    public void setCoordinates(double latitude, double longitude) {
        boolean bothMissing = Double.isNaN(latitude) && Double.isNaN(longitude);
        if (bothMissing) { this.latitude = Double.NaN; this.longitude = Double.NaN; return; }
        if (!Double.isFinite(latitude) || latitude < 4.5 || latitude > 21.5 ||
            !Double.isFinite(longitude) || longitude < 116.0 || longitude > 127.5) {
            throw new IllegalArgumentException("GreenPulse accepts coordinates inside the Philippines only (approx. latitude 4.5–21.5, longitude 116.0–127.5).");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setFillLevel(double newFillPercentage) throws InvalidFillLevelException {
        if (!Double.isFinite(newFillPercentage) || newFillPercentage < 0.0 || newFillPercentage > 100.0) {
            throw new InvalidFillLevelException("Fill percentage must be between 0% and 100%.");
        }
        this.fillPercentage = newFillPercentage;
    }

    /** Urgency score from 0 to 100. Each bin type weighs its own sensors differently. */
    public abstract int calculatePriority();

    /** Which kind of bin this is; used by the DAO and factory instead of {@code instanceof} checks. */
    public abstract BinType getBinType();

    /** The type-specific sensor values (unused sensors are null), ready to be stored or logged. */
    public abstract SensorReading getSensorReading();

    /** Brings the bin back to its just-emptied state after a collection. Subclasses reset their own sensors too. */
    public void resetAfterCollection() {
        try {
            setFillLevel(0.0);
        } catch (InvalidFillLevelException e) {
            throw new IllegalStateException(e); // 0% is always valid
        }
    }

    /** Identifier of the bin's dedicated sensor hardware, or null if it has none. */
    public String getSensorId() { return null; }

    /** Nudges this bin's type-specific sensors the way real readings would drift (used by the simulator). */
    public abstract void simulateSensorDrift(Random random);

    public String getBinID() { return binID; }
    public double getCapacityLitres() { return capacityLitres; }
    public double getFillPercentage() { return fillPercentage; }
    public String getLocation() { return location; }
    public boolean isActive() { return isActive; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean hasCoordinates() { return Double.isFinite(latitude) && Double.isFinite(longitude); }
    public void setActive(boolean active) { isActive = active; }
}
