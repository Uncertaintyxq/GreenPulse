package com.greenpulse.analytics;

import com.greenpulse.analytics.AnalyticsData.CollectionPoint;
import com.greenpulse.analytics.AnalyticsData.TelemetryPoint;
import com.greenpulse.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsCalculatorTest {
    private static final long SINCE = 1_000_000L;
    private static final long NOW = SINCE + 1000;

    private static AnalyticsData data(Map<String, Double> baseline, List<TelemetryPoint> points, List<CollectionPoint> collections) {
        return new AnalyticsData(NOW, SINCE, List.of(), baseline, points, collections);
    }

    @Test
    void trendCarriesLastKnownFillForwardPerType() {
        Map<String, BinType> selected = Map.of("R1", BinType.RECYCLING, "C1", BinType.COMPOST);
        AnalyticsData d = data(Map.of("R1", 20.0, "C1", 40.0),
                List.of(new TelemetryPoint("R1", SINCE + 600, 60.0)), List.of());
        AnalyticsCalculator.TrendSeries t = AnalyticsCalculator.trend(d, selected, 10);
        assertEquals(20.0, t.byType().get(BinType.RECYCLING)[0]);   // still the baseline
        assertEquals(60.0, t.byType().get(BinType.RECYCLING)[9]);   // after the new reading
        assertEquals(40.0, t.byType().get(BinType.COMPOST)[9]);     // compost never changed
        assertEquals(50.0, t.fleet()[9]);                           // (60 + 40) / 2
    }

    @Test
    void trendIgnoresBinsOutsideTheSelectionAndIsNaNWithoutData() {
        AnalyticsData d = data(Map.of("X", 90.0), List.of(), List.of());
        AnalyticsCalculator.TrendSeries t = AnalyticsCalculator.trend(d, Map.of("R1", BinType.RECYCLING), 5);
        assertTrue(Double.isNaN(t.fleet()[0]));
        assertTrue(Double.isNaN(t.byType().get(BinType.RECYCLING)[4]));
    }

    @Test
    void collectionsAreCountedPerBucketAndAveraged() {
        List<CollectionPoint> events = List.of(
                new CollectionPoint("R1", SINCE + 10, 90.0),
                new CollectionPoint("R1", SINCE + 990, 70.0),
                new CollectionPoint("OTHER", SINCE + 500, 100.0));
        AnalyticsData d = data(Map.of(), List.of(), events);
        int[] perBucket = AnalyticsCalculator.collectionsPerBucket(d, Set.of("R1"), 10);
        assertEquals(1, perBucket[0]);
        assertEquals(1, perBucket[9]);
        AnalyticsCalculator.CollectionStats stats = AnalyticsCalculator.collectionStats(d, Set.of("R1"));
        assertEquals(2, stats.count());
        assertEquals(80.0, stats.avgFillBefore());
    }

    @Test
    void criticalWaitMeasuresTimeUntilTheBinDropsBelowTheThreshold() {
        List<TelemetryPoint> pts = List.of(
                new TelemetryPoint("R1", SINCE + 100, 88.0),   // becomes critical
                new TelemetryPoint("R1", SINCE + 400, 95.0),
                new TelemetryPoint("R1", SINCE + 700, 0.0),    // collected: 600 s at critical
                new TelemetryPoint("R2", SINCE + 800, 91.0));  // still critical at NOW: 200 s
        AnalyticsCalculator.WaitStats w = AnalyticsCalculator.criticalWaits(data(Map.of(), pts, List.of()), Set.of("R1", "R2"));
        assertEquals(1, w.completed());
        assertEquals(600.0, w.avgCompletedSeconds());
        assertEquals(1, w.stillWaiting());
        assertEquals(200.0, w.longestWaitingSeconds());
    }

    @Test
    void binAlreadyCriticalAtPeriodStartIsCountedFromTheStart() {
        AnalyticsCalculator.WaitStats w = AnalyticsCalculator.criticalWaits(
                data(Map.of("R1", 90.0), List.of(), List.of()), Set.of("R1"));
        assertEquals(1, w.stillWaiting());
        assertEquals(1000.0, w.longestWaitingSeconds());
    }

    @Test
    void summaryKeepsRecyclingAndCompostSeparate() throws Exception {
        List<SmartBin> bins = List.of(
                new RecyclingBin("R1", 120, 90, "A", 20),
                new RecyclingBin("R2", 120, 30, "A", 40),
                new CompostBin("C1", 120, 50, "B", 60, 40));
        Map<BinType, AnalyticsCalculator.TypeSummary> s = AnalyticsCalculator.summarise(bins);
        assertEquals(2, s.get(BinType.RECYCLING).bins());
        assertEquals(60.0, s.get(BinType.RECYCLING).avgFill());
        assertEquals(30.0, s.get(BinType.RECYCLING).avgContamination());
        assertNull(s.get(BinType.RECYCLING).avgTemperature());
        assertEquals(1, s.get(BinType.COMPOST).bins());
        assertEquals(40.0, s.get(BinType.COMPOST).avgTemperature());
        assertNull(s.get(BinType.COMPOST).avgContamination());
        assertEquals("R1", AnalyticsCalculator.topByPriority(bins, 1).get(0).getBinID());
    }

    @Test
    void driversTextAndDurationsAreReadable() throws Exception {
        assertEquals("91% full, contamination 12%", AnalyticsCalculator.describeDrivers(new RecyclingBin("R", 120, 91, "A", 12)));
        assertEquals("50% full, 40°C, moisture 60%", AnalyticsCalculator.describeDrivers(new CompostBin("C", 120, 50, "A", 60, 40)));
        assertEquals("45 s", AnalyticsCalculator.formatDuration(45));
        assertEquals("10 min", AnalyticsCalculator.formatDuration(600));
        assertEquals("3.0 h", AnalyticsCalculator.formatDuration(3 * 3600));
        assertEquals("—", AnalyticsCalculator.formatDuration(Double.NaN));
    }
}
