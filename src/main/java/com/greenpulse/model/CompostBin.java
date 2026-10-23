package com.greenpulse.model;

import com.greenpulse.exception.InvalidFillLevelException;
import java.util.Random;

public class CompostBin extends SmartBin {
    private double moistureLevel;
    private double tempCelsius;

    public CompostBin(String binID, double capacityLitres, double fillPercentage, String location, double moistureLevel, double tempCelsius) throws InvalidFillLevelException {
        this(binID, capacityLitres, fillPercentage, location, moistureLevel, tempCelsius, Double.NaN, Double.NaN);
    }

    public CompostBin(String binID, double capacityLitres, double fillPercentage, String location,
                      double moistureLevel, double tempCelsius, double latitude, double longitude) throws InvalidFillLevelException {
        super(binID, capacityLitres, fillPercentage, location, latitude, longitude);
        setMoistureLevel(moistureLevel);
        setTempCelsius(tempCelsius);
    }

    @Override
    public int calculatePriority() {
        // Temperature is first converted to a 0-100 score so it is on the same scale as fill and moisture.
        double span = CollectionPolicy.COMPOST_TEMP_HOT_C - CollectionPolicy.COMPOST_TEMP_SAFE_C;
        double tempScore = Math.max(0.0, Math.min(100.0, (tempCelsius - CollectionPolicy.COMPOST_TEMP_SAFE_C) / span * 100.0));
        double score = (getFillPercentage() * CollectionPolicy.COMPOST_FILL_WEIGHT)
                + (tempScore * CollectionPolicy.COMPOST_TEMPERATURE_WEIGHT)
                + (moistureLevel * CollectionPolicy.COMPOST_MOISTURE_WEIGHT);
        return (int) Math.min(100, Math.round(score));
    }

    @Override
    public BinType getBinType() { return BinType.COMPOST; }

    @Override
    public SensorReading getSensorReading() { return new SensorReading(tempCelsius, moistureLevel, null); }

    @Override
    public void simulateSensorDrift(Random random) {
        double delta = (random.nextDouble() * 2.0) - 0.5;
        setTempCelsius(Math.max(15.0, Math.min(60.0, tempCelsius + delta)));
    }

    public double getTempCelsius() { return tempCelsius; }
    public double getMoistureLevel() { return moistureLevel; }
    public void setTempCelsius(double temp) {
        if (!Double.isFinite(temp) || temp < -50 || temp > 100) throw new IllegalArgumentException("Temperature must be between -50 and 100°C.");
        this.tempCelsius = temp;
    }
    public void setMoistureLevel(double moisture) {
        if (!Double.isFinite(moisture) || moisture < 0 || moisture > 100) throw new IllegalArgumentException("Moisture must be between 0 and 100%.");
        this.moistureLevel = moisture;
    }
}
