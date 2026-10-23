package com.greenpulse.service;

import com.greenpulse.model.SmartBin;
import com.greenpulse.service.RoadRoutingService.Coordinate;

import java.util.List;

/**
 * Works out where along the drawn road route each stop is, so the truck animation knows when it has reached a bin.
 * Stops are matched in order: a later stop is never placed before an earlier one.
 */
public final class StopLocator {
    private static final double TIE_BREAK_METRES = 20.0;

    private StopLocator() {}

    /** For each stop, the index of the first route point that is (nearly) as close to the bin as any later point. */
    public static int[] locate(List<Coordinate> geometry, List<SmartBin> orderedStops) {
        int[] result = new int[orderedStops.size()];
        if (geometry.isEmpty()) return result;
        int previous = 0;
        for (int k = 0; k < orderedStops.size(); k++) {
            SmartBin bin = orderedStops.get(k);
            if (!bin.hasCoordinates()) { result[k] = previous; continue; }
            double best = Double.MAX_VALUE;
            for (int i = previous; i < geometry.size(); i++) {
                best = Math.min(best, distance(geometry.get(i), bin));
            }
            double limit = best + TIE_BREAK_METRES;
            int chosen = previous;
            for (int i = previous; i < geometry.size(); i++) {
                if (distance(geometry.get(i), bin) <= limit) { chosen = i; break; }
            }
            result[k] = chosen;
            previous = chosen;
        }
        return result;
    }

    /** Approximate ground distance in metres (flat-earth formula, accurate enough over a city). */
    private static double distance(Coordinate c, SmartBin bin) {
        double dLat = (c.latitude - bin.getLatitude()) * 110_540.0;
        double dLon = (c.longitude - bin.getLongitude()) * 111_320.0 * Math.cos(Math.toRadians(bin.getLatitude()));
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
