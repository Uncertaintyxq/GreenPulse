package com.greenpulse.analytics;

import com.greenpulse.analytics.AnalyticsData.TelemetryPoint;
import com.greenpulse.analytics.FillForecaster.Forecast;
import com.greenpulse.analytics.FillForecaster.Status;
import com.greenpulse.model.RecyclingBin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FillForecasterTest {
    private static final long NOW = 2_000_000_000L;

    private static AnalyticsData data(List<TelemetryPoint> pts) {
        return new AnalyticsData(NOW, NOW - 86400, List.of(), Map.of(), pts, List.of());
    }

    /** A bin filling at exactly 10 points per hour, one reading every 10 minutes, ending at 40% now. */
    private static List<TelemetryPoint> steady() {
        List<TelemetryPoint> pts = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            long t = NOW - (12 - i) * 600L;
            pts.add(new TelemetryPoint("R1", t, 40 - (12 - i) * (10.0 / 6)));
        }
        return pts;
    }

    @Test
    void steadyFillGivesAnAccurateTimeToCritical() throws Exception {
        RecyclingBin bin = new RecyclingBin("R1", 120, 40, "A", 0);
        Forecast f = FillForecaster.forecast(data(steady()), bin);
        assertEquals(Status.RISING, f.status());
        assertEquals(10.0, f.ratePerHour(), 0.01);
        assertEquals(4.5 * 3600, f.secondsToCritical(), 5);   // (85 - 40) / 10 per hour
        assertEquals(6.0 * 3600, f.secondsToFull(), 5);       // (100 - 40) / 10 per hour
        assertTrue(f.isSteady());
    }

    @Test
    void onlyReadingsSinceTheLastCollectionAreUsed() throws Exception {
        List<TelemetryPoint> pts = new ArrayList<>();
        // old cycle: slow fill up to 90%, then emptied
        for (int i = 0; i < 6; i++) pts.add(new TelemetryPoint("R1", NOW - 20000 + i * 600L, 80 + i * 2));
        // new cycle starts empty 90 minutes ago and fills at 20 points per hour, reaching 30% now
        for (int i = 0; i <= 6; i++) pts.add(new TelemetryPoint("R1", NOW - 5400 + i * 900L, i * 5));
        RecyclingBin bin = new RecyclingBin("R1", 120, 30, "A", 0);
        Forecast f = FillForecaster.forecast(data(pts), bin);
        assertEquals(Status.RISING, f.status());
        assertEquals(20.0, f.ratePerHour(), 0.5);
    }

    @Test
    void binAlreadyAtTheThresholdIsCriticalNow() throws Exception {
        RecyclingBin bin = new RecyclingBin("R1", 120, 92, "A", 0);
        Forecast f = FillForecaster.forecast(data(steady()), bin);
        assertEquals(Status.CRITICAL_NOW, f.status());
        assertEquals(0.0, f.secondsToCritical());
        assertEquals("now", FillForecaster.describeTimeToCritical(f));
    }

    @Test
    void tooFewReadingsGivesNoForecast() throws Exception {
        RecyclingBin bin = new RecyclingBin("R1", 120, 30, "A", 0);
        Forecast f = FillForecaster.forecast(data(List.of(new TelemetryPoint("R1", NOW - 60, 28))), bin);
        assertEquals(Status.NOT_ENOUGH_DATA, f.status());
        assertEquals("not enough data", FillForecaster.describeTimeToCritical(f));
    }

    @Test
    void aFlatBinIsReportedAsNotFilling() throws Exception {
        List<TelemetryPoint> pts = new ArrayList<>();
        for (int i = 0; i < 8; i++) pts.add(new TelemetryPoint("R1", NOW - (8 - i) * 600L, 30));
        RecyclingBin bin = new RecyclingBin("R1", 120, 30, "A", 0);
        assertEquals(Status.NOT_FILLING, FillForecaster.forecast(data(pts), bin).status());
    }

    @Test
    void rankingPutsTheSoonestBinFirst() throws Exception {
        List<TelemetryPoint> pts = new ArrayList<>(steady());           // R1: reaches critical in 4.5 h
        for (int i = 0; i <= 12; i++) {                                 // R2: 30 points per hour, ends at 70%
            long t = NOW - (12 - i) * 600L;
            pts.add(new TelemetryPoint("R2", t, 70 - (12 - i) * 5.0));
        }
        RecyclingBin r1 = new RecyclingBin("R1", 120, 40, "A", 0);
        RecyclingBin r2 = new RecyclingBin("R2", 120, 70, "A", 0);
        List<FillForecaster.Entry> ranked = FillForecaster.rank(data(pts), List.of(r1, r2));
        assertEquals("R2", ranked.get(0).bin().getBinID());
        assertEquals("R1", ranked.get(1).bin().getBinID());
    }
}
