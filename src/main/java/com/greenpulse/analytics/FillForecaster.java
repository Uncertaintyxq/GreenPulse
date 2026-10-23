package com.greenpulse.analytics;

import com.greenpulse.analytics.AnalyticsData.TelemetryPoint;
import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.model.SmartBin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Predicts when a bin will reach the critical fill level by fitting a straight line to its readings since it was
 * last emptied. It is a simple trend estimate, not a guarantee: irregular filling gives a rough answer.
 */
public final class FillForecaster {
    private FillForecaster() {}

    /** A fill drop bigger than this between two readings is treated as the bin having been emptied. */
    static final double RESET_DROP_POINTS = 10.0;
    static final int MIN_SAMPLES = 4;
    static final long MIN_SPAN_SECONDS = 120;
    /** Forecasts further away than this are reported as "not filling" rather than as a huge, meaningless number. */
    static final double MAX_HORIZON_SECONDS = 14 * 86400.0;
    /** Below this rate (percentage points per hour) a bin counts as not filling. */
    static final double MIN_RATE_PER_HOUR = 0.5;

    public enum Status { RISING, CRITICAL_NOW, NOT_FILLING, NOT_ENOUGH_DATA }

    /**
     * @param ratePerHour       fill rate in percentage points per hour (NaN without enough data)
     * @param secondsToCritical seconds until the critical level; 0 if already there; NaN if unknown
     * @param secondsToFull     seconds until 100%; NaN if unknown
     * @param rSquared          how well a straight line fits the readings, 0 to 1 (NaN without enough data)
     */
    public record Forecast(Status status, double ratePerHour, double secondsToCritical, double secondsToFull,
                           double rSquared, int samples) {
        /** True when the bin has been filling at a steady pace, so the estimate can be trusted more. */
        public boolean isSteady() { return !Double.isNaN(rSquared) && rSquared >= 0.8; }
    }

    public static Forecast forecast(AnalyticsData data, SmartBin bin) {
        List<TelemetryPoint> pts = new ArrayList<>();
        for (TelemetryPoint p : data.points()) if (p.binId().equals(bin.getBinID())) pts.add(p);
        pts.sort(Comparator.comparingLong(TelemetryPoint::epoch));
        // The bin's current fill is the most recent truth, so it closes the series.
        if (pts.isEmpty() || pts.get(pts.size() - 1).epoch() < data.nowEpoch()) {
            pts.add(new TelemetryPoint(bin.getBinID(), data.nowEpoch(), bin.getFillPercentage()));
        }

        int start = 0;
        for (int i = 1; i < pts.size(); i++) {
            if (pts.get(i).fill() < pts.get(i - 1).fill() - RESET_DROP_POINTS) start = i;
        }
        List<TelemetryPoint> run = pts.subList(start, pts.size());
        double current = bin.getFillPercentage();

        int n = run.size();
        long span = run.get(n - 1).epoch() - run.get(0).epoch();
        if (n < MIN_SAMPLES || span < MIN_SPAN_SECONDS) {
            return current >= CollectionPolicy.CRITICAL_FILL
                    ? new Forecast(Status.CRITICAL_NOW, Double.NaN, 0, Double.NaN, Double.NaN, n)
                    : new Forecast(Status.NOT_ENOUGH_DATA, Double.NaN, Double.NaN, Double.NaN, Double.NaN, n);
        }

        // Least-squares line through (seconds since first reading, fill).
        long t0 = run.get(0).epoch();
        double sumX = 0, sumY = 0;
        for (TelemetryPoint p : run) { sumX += p.epoch() - t0; sumY += p.fill(); }
        double meanX = sumX / n, meanY = sumY / n;
        double sxx = 0, sxy = 0, syy = 0;
        for (TelemetryPoint p : run) {
            double dx = (p.epoch() - t0) - meanX, dy = p.fill() - meanY;
            sxx += dx * dx; sxy += dx * dy; syy += dy * dy;
        }
        double slope = sxx == 0 ? 0 : sxy / sxx;                       // percentage points per second
        double r2 = (sxx == 0 || syy == 0) ? 0 : (sxy * sxy) / (sxx * syy);
        double perHour = slope * 3600.0;

        double toFull = slope > 0 ? (100.0 - current) / slope : Double.NaN;
        if (current >= CollectionPolicy.CRITICAL_FILL) {
            return new Forecast(Status.CRITICAL_NOW, perHour, 0, Math.max(0, toFull), r2, n);
        }
        double toCritical = slope > 0 ? (CollectionPolicy.CRITICAL_FILL - current) / slope : Double.NaN;
        if (perHour < MIN_RATE_PER_HOUR || Double.isNaN(toCritical) || toCritical > MAX_HORIZON_SECONDS) {
            return new Forecast(Status.NOT_FILLING, perHour, Double.NaN, Double.NaN, r2, n);
        }
        return new Forecast(Status.RISING, perHour, toCritical, toFull, r2, n);
    }

    /** Short text for the forecast table, e.g. "35 min", "now", "not filling". */
    public static String describeTimeToCritical(Forecast f) {
        return switch (f.status()) {
            case CRITICAL_NOW -> "now";
            case RISING -> "~" + AnalyticsCalculator.formatDuration(f.secondsToCritical());
            case NOT_FILLING -> "not filling";
            case NOT_ENOUGH_DATA -> "not enough data";
        };
    }

    public static String describeTimeToFull(Forecast f) {
        if (f.status() == Status.RISING || f.status() == Status.CRITICAL_NOW) {
            return Double.isNaN(f.secondsToFull()) ? "—" : "~" + AnalyticsCalculator.formatDuration(f.secondsToFull());
        }
        return "—";
    }

    public static String describeRate(Forecast f) {
        return Double.isNaN(f.ratePerHour()) ? "—" : String.format(Locale.US, "%+.1f%%/h", f.ratePerHour());
    }

    public static String describeConfidence(Forecast f) {
        return switch (f.status()) {
            case NOT_ENOUGH_DATA -> "need more readings";
            default -> f.isSteady() ? "steady" : "irregular";
        };
    }

    /** Forecasts for all given bins, soonest-to-critical first (critical now, then rising, then the rest). */
    public static List<Entry> rank(AnalyticsData data, List<SmartBin> bins) {
        List<Entry> entries = new ArrayList<>();
        for (SmartBin b : bins) entries.add(new Entry(b, forecast(data, b)));
        entries.sort(Comparator
                .comparingInt((Entry e) -> order(e.forecast().status()))
                .thenComparingDouble(e -> Double.isNaN(e.forecast().secondsToCritical()) ? Double.MAX_VALUE : e.forecast().secondsToCritical())
                .thenComparing(e -> e.bin().getBinID()));
        return entries;
    }

    public record Entry(SmartBin bin, Forecast forecast) {}

    private static int order(Status s) {
        return switch (s) { case CRITICAL_NOW -> 0; case RISING -> 1; case NOT_FILLING -> 2; case NOT_ENOUGH_DATA -> 3; };
    }
}
