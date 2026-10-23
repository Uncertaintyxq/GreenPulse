package com.greenpulse.model;

/** One place for every collection threshold and priority weight, instead of magic numbers across the code. */
public final class CollectionPolicy {
    private CollectionPolicy() {}

    /** A bin at or above this fill level is critical and needs collection. */
    public static final double CRITICAL_FILL = 85.0;
    /** A bin with a priority score above this is critical even if its fill level is lower. */
    public static final int CRITICAL_PRIORITY = 75;
    /** Minimum fill level or priority score for a bin to be included in a collection route. */
    public static final double ROUTE_MIN_FILL = 60.0;
    public static final int ROUTE_MIN_PRIORITY = 60;

    // Priority weights (each group sums to 1.0)
    public static final double RECYCLING_FILL_WEIGHT = 0.7;
    public static final double RECYCLING_CONTAMINATION_WEIGHT = 0.3;
    public static final double COMPOST_FILL_WEIGHT = 0.5;
    public static final double COMPOST_TEMPERATURE_WEIGHT = 0.3;
    public static final double COMPOST_MOISTURE_WEIGHT = 0.2;

    /** Compost temperature range mapped onto a 0-100 score so it can be mixed with percentages. */
    public static final double COMPOST_TEMP_SAFE_C = 15.0;
    public static final double COMPOST_TEMP_HOT_C = 60.0;

    public static boolean isCritical(SmartBin bin) {
        return bin.getFillPercentage() >= CRITICAL_FILL || bin.calculatePriority() > CRITICAL_PRIORITY;
    }

    public static boolean needsRoute(SmartBin bin) {
        return bin.getFillPercentage() >= ROUTE_MIN_FILL || bin.calculatePriority() >= ROUTE_MIN_PRIORITY;
    }
}
