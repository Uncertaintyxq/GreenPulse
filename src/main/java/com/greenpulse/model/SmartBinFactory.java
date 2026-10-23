package com.greenpulse.model;

import com.greenpulse.exception.InvalidFillLevelException;

/** Creates bins. Prefer the typed methods; {@code createBin} is kept for callers that only have a type name. */
public final class SmartBinFactory {
    private SmartBinFactory() {}

    public static RecyclingBin createRecycling(String binID, double capacity, double fill, String location,
                                               double contamination, double latitude, double longitude) throws InvalidFillLevelException {
        return new RecyclingBin(binID, capacity, fill, location, contamination, latitude, longitude);
    }

    public static CompostBin createCompost(String binID, double capacity, double fill, String location,
                                           double moisture, double tempCelsius, double latitude, double longitude) throws InvalidFillLevelException {
        return new CompostBin(binID, capacity, fill, location, moisture, tempCelsius, latitude, longitude);
    }

    /**
     * Builds a bin from a type and its sensor values. {@code param1} is contamination for recycling bins and
     * moisture for compost bins; {@code param2} is the compost temperature (ignored for recycling bins).
     */
    public static SmartBin createBin(BinType type, String binID, double capacity, double fill, String location,
                                     double param1, double param2, double latitude, double longitude) throws InvalidFillLevelException {
        return switch (type) {
            case RECYCLING -> createRecycling(binID, capacity, fill, location, param1, latitude, longitude);
            case COMPOST -> createCompost(binID, capacity, fill, location, param1, param2, latitude, longitude);
        };
    }

    /** Rebuilds a bin from stored columns; each type picks the sensor values it needs from the reading. */
    public static SmartBin fromStoredReading(BinType type, String binID, double capacity, double fill, String location,
                                             SensorReading reading, double latitude, double longitude) throws InvalidFillLevelException {
        return switch (type) {
            case RECYCLING -> createRecycling(binID, capacity, fill, location, orZero(reading.contamination()), latitude, longitude);
            case COMPOST -> createCompost(binID, capacity, fill, location, orZero(reading.moisture()), orZero(reading.temperature()), latitude, longitude);
        };
    }

    private static double orZero(Double value) { return value == null ? 0.0 : value; }

    public static SmartBin createBin(String type, String binID, double capacity, double fill, String location,
                                     double param1, double param2) throws InvalidFillLevelException {
        return createBin(type, binID, capacity, fill, location, param1, param2, Double.NaN, Double.NaN);
    }

    public static SmartBin createBin(String type, String binID, double capacity, double fill, String location,
                                     double param1, double param2, double latitude, double longitude) throws InvalidFillLevelException {
        return createBin(BinType.fromString(type), binID, capacity, fill, location, param1, param2, latitude, longitude);
    }
}
