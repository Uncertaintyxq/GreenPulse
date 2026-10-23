package com.greenpulse.model;

import com.greenpulse.exception.InvalidFillLevelException;
import java.util.Random;

public class RecyclingBin extends SmartBin {
    private double contaminationPercentage;
    private String opticalSensorID;

    public RecyclingBin(String binID, double capacityLitres, double fillPercentage, String location, double contaminationPercentage) throws InvalidFillLevelException {
        this(binID, capacityLitres, fillPercentage, location, contaminationPercentage, Double.NaN, Double.NaN);
    }

    public RecyclingBin(String binID, double capacityLitres, double fillPercentage, String location,
                        double contaminationPercentage, double latitude, double longitude) throws InvalidFillLevelException {
        super(binID, capacityLitres, fillPercentage, location, latitude, longitude);
        setContaminationPercentage(contaminationPercentage);
        this.opticalSensorID = "OPT-" + binID;
    }

    @Override
    public int calculatePriority() {
        double score = (getFillPercentage() * CollectionPolicy.RECYCLING_FILL_WEIGHT)
                + (contaminationPercentage * CollectionPolicy.RECYCLING_CONTAMINATION_WEIGHT);
        return (int) Math.min(100, Math.round(score));
    }

    @Override
    public BinType getBinType() { return BinType.RECYCLING; }

    @Override
    public SensorReading getSensorReading() { return new SensorReading(null, null, contaminationPercentage); }

    @Override
    public String getSensorId() { return opticalSensorID; }

    @Override
    public void simulateSensorDrift(Random random) {
        double delta = (random.nextDouble() * 4.0) - 1.0;
        setContaminationPercentage(Math.max(0.0, Math.min(100.0, contaminationPercentage + delta)));
    }

    @Override
    public void resetAfterCollection() {
        super.resetAfterCollection();
        setContaminationPercentage(0.0);
    }

    public double getContaminationPercentage() { return contaminationPercentage; }
    public void setContaminationPercentage(double val) {
        if (!Double.isFinite(val) || val < 0 || val > 100) throw new IllegalArgumentException("Contamination must be between 0 and 100%.");
        this.contaminationPercentage = val;
    }
    public String getOpticalSensorID() { return opticalSensorID; }
}
