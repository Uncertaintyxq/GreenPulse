package com.greenpulse.analytics;

import com.greenpulse.analytics.AnalyticsData.CollectionPoint;
import com.greenpulse.analytics.AnalyticsData.TelemetryPoint;
import com.greenpulse.model.BinType;
import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.model.SensorReading;
import com.greenpulse.model.SmartBin;

import java.util.*;

/** Pure calculations behind the analytics page (no database or Swing), so they can be unit tested. */
public final class AnalyticsCalculator {
    private AnalyticsCalculator() {}

    /** Average fill level over time. Values are NaN where no selected bin had reported yet. */
    public record TrendSeries(double[] fleet, Map<BinType, double[]> byType) {}

    public record CollectionStats(int count, double avgFillBefore) {}

    public record WaitStats(int completed, double avgCompletedSeconds, int stillWaiting, double longestWaitingSeconds) {}

    public record TypeSummary(BinType type, int bins, int critical, double avgFill, double avgPriority,
                              Double avgContamination, Double avgTemperature, Double avgMoisture) {}

    /**
     * For each time step, averages the last known fill level of every selected bin (a bin that has not reported
     * keeps its previous value), per bin type and for the whole selection.
     */
    public static TrendSeries trend(AnalyticsData data, Map<String, BinType> selected, int buckets) {
        double[] fleet = new double[buckets];
        EnumMap<BinType, double[]> byType = new EnumMap<>(BinType.class);
        for (BinType type : new HashSet<>(selected.values())) byType.put(type, new double[buckets]);

        Map<String, Double> state = new HashMap<>();
        data.baseline().forEach((id, fill) -> { if (selected.containsKey(id)) state.put(id, fill); });
        List<TelemetryPoint> points = new ArrayList<>(data.points());
        points.sort(Comparator.comparingLong(TelemetryPoint::epoch));

        long span = Math.max(1, data.nowEpoch() - data.sinceEpoch());
        int next = 0;
        for (int i = 0; i < buckets; i++) {
            long stepEnd = data.sinceEpoch() + span * (i + 1) / buckets;
            while (next < points.size() && points.get(next).epoch() <= stepEnd) {
                TelemetryPoint p = points.get(next++);
                if (selected.containsKey(p.binId())) state.put(p.binId(), p.fill());
            }
            double allSum = 0; int allCount = 0;
            EnumMap<BinType, double[]> sums = new EnumMap<>(BinType.class); // {sum, count}
            for (Map.Entry<String, Double> e : state.entrySet()) {
                BinType type = selected.get(e.getKey());
                allSum += e.getValue(); allCount++;
                double[] s = sums.computeIfAbsent(type, t -> new double[2]);
                s[0] += e.getValue(); s[1]++;
            }
            fleet[i] = allCount == 0 ? Double.NaN : allSum / allCount;
            for (Map.Entry<BinType, double[]> e : byType.entrySet()) {
                double[] s = sums.get(e.getKey());
                e.getValue()[i] = (s == null || s[1] == 0) ? Double.NaN : s[0] / s[1];
            }
        }
        return new TrendSeries(fleet, byType);
    }

    /** Number of collections in each of {@code buckets} equal time slices of the period. */
    public static int[] collectionsPerBucket(AnalyticsData data, Set<String> ids, int buckets) {
        int[] counts = new int[buckets];
        long span = Math.max(1, data.nowEpoch() - data.sinceEpoch());
        for (CollectionPoint c : data.collections()) {
            if (!ids.contains(c.binId()) || c.epoch() < data.sinceEpoch() || c.epoch() > data.nowEpoch()) continue;
            int idx = (int) Math.min(buckets - 1, (c.epoch() - data.sinceEpoch()) * buckets / span);
            counts[idx]++;
        }
        return counts;
    }

    public static CollectionStats collectionStats(AnalyticsData data, Set<String> ids) {
        int count = 0; double sum = 0;
        for (CollectionPoint c : data.collections()) {
            if (!ids.contains(c.binId())) continue;
            count++; sum += c.fillBefore();
        }
        return new CollectionStats(count, count == 0 ? Double.NaN : sum / count);
    }

    /**
     * How long bins stay at or above the critical fill level. An episode starts at the first reading at or above
     * the threshold and ends at the next reading below it (usually a collection). Precision is limited by the
     * reading interval; a bin already critical when the period began is counted from the period start.
     */
    public static WaitStats criticalWaits(AnalyticsData data, Set<String> ids) {
        Map<String, List<TelemetryPoint>> byBin = new HashMap<>();
        for (TelemetryPoint p : data.points()) {
            if (ids.contains(p.binId())) byBin.computeIfAbsent(p.binId(), k -> new ArrayList<>()).add(p);
        }
        double threshold = CollectionPolicy.CRITICAL_FILL;
        List<Double> completed = new ArrayList<>();
        int waiting = 0; double longest = 0;
        for (String id : ids) {
            List<TelemetryPoint> pts = byBin.getOrDefault(id, List.of());
            Double base = data.baseline().get(id);
            Long start = (base != null && base >= threshold) ? Long.valueOf(data.sinceEpoch()) : null;
            List<TelemetryPoint> sorted = new ArrayList<>(pts);
            sorted.sort(Comparator.comparingLong(TelemetryPoint::epoch));
            for (TelemetryPoint p : sorted) {
                if (p.fill() >= threshold && start == null) {
                    start = p.epoch();
                } else if (p.fill() < threshold && start != null) {
                    completed.add((double) Math.max(0, p.epoch() - start));
                    start = null;
                }
            }
            if (start != null) {
                waiting++;
                longest = Math.max(longest, Math.max(0, data.nowEpoch() - start));
            }
        }
        double avg = completed.isEmpty() ? Double.NaN : completed.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        return new WaitStats(completed.size(), avg, waiting, longest);
    }

    /** Current-state summary per bin type, including the sensor values each type has. */
    public static Map<BinType, TypeSummary> summarise(Collection<SmartBin> bins) {
        EnumMap<BinType, TypeSummary> result = new EnumMap<>(BinType.class);
        for (BinType type : BinType.values()) {
            int count = 0, critical = 0;
            double fill = 0, priority = 0;
            double[] sums = new double[3]; int[] counts = new int[3]; // contamination, temperature, moisture
            for (SmartBin bin : bins) {
                if (bin.getBinType() != type) continue;
                count++;
                fill += bin.getFillPercentage();
                priority += bin.calculatePriority();
                if (CollectionPolicy.isCritical(bin)) critical++;
                SensorReading r = bin.getSensorReading();
                if (r.contamination() != null) { sums[0] += r.contamination(); counts[0]++; }
                if (r.temperature() != null) { sums[1] += r.temperature(); counts[1]++; }
                if (r.moisture() != null) { sums[2] += r.moisture(); counts[2]++; }
            }
            result.put(type, new TypeSummary(type, count, critical,
                    count == 0 ? Double.NaN : fill / count, count == 0 ? Double.NaN : priority / count,
                    counts[0] == 0 ? null : sums[0] / counts[0],
                    counts[1] == 0 ? null : sums[1] / counts[1],
                    counts[2] == 0 ? null : sums[2] / counts[2]));
        }
        return result;
    }

    public static List<SmartBin> topByPriority(Collection<SmartBin> bins, int n) {
        return bins.stream()
                .sorted(Comparator.comparingInt(SmartBin::calculatePriority).reversed()
                        .thenComparing(Comparator.comparingDouble(SmartBin::getFillPercentage).reversed()))
                .limit(n).toList();
    }

    /** Explains a bin's priority score in terms of the readings behind it, e.g. "91% full, contamination 12%". */
    public static String describeDrivers(SmartBin bin) {
        StringBuilder sb = new StringBuilder(String.format(Locale.US, "%.0f%% full", bin.getFillPercentage()));
        SensorReading r = bin.getSensorReading();
        if (r.contamination() != null) sb.append(String.format(Locale.US, ", contamination %.0f%%", r.contamination()));
        if (r.temperature() != null) sb.append(String.format(Locale.US, ", %.0f°C", r.temperature()));
        if (r.moisture() != null) sb.append(String.format(Locale.US, ", moisture %.0f%%", r.moisture()));
        return sb.toString();
    }

    public static String formatDuration(double seconds) {
        if (Double.isNaN(seconds)) return "—";
        if (seconds < 90) return Math.round(seconds) + " s";
        double minutes = seconds / 60.0;
        if (minutes < 90) return Math.round(minutes) + " min";
        double hours = minutes / 60.0;
        if (hours < 48) return String.format(Locale.US, "%.1f h", hours);
        return String.format(Locale.US, "%.1f days", hours / 24.0);
    }
}
