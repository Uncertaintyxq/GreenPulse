package com.greenpulse.analytics;

import com.greenpulse.model.SmartBin;

import java.util.List;
import java.util.Map;

/**
 * Everything the analytics page needs for one period: the current bins plus history from the database.
 * Times are epoch seconds taken from the database server, so they do not depend on time-zone settings.
 *
 * @param baseline last known fill level of each bin from before the period started
 * @param points   readings inside the period, at most one per bin per time bucket, oldest first
 */
public record AnalyticsData(long nowEpoch, long sinceEpoch, List<SmartBin> bins, Map<String, Double> baseline,
                            List<TelemetryPoint> points, List<CollectionPoint> collections) {

    public record TelemetryPoint(String binId, long epoch, double fill) {}

    public record CollectionPoint(String binId, long epoch, double fillBefore) {}
}
