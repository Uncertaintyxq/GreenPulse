package com.greenpulse.analytics;

/** Supplies analytics data. The database implements it; tests and previews can supply fixed data instead. */
@FunctionalInterface
public interface AnalyticsDataSource {
    AnalyticsData load(long windowSeconds, boolean includeSimulated) throws Exception;
}
