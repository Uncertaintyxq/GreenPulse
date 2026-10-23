package com.greenpulse.model;

import com.greenpulse.model.RouteRun.StopStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteRunTest {

    private RouteRun threeStops() throws Exception {
        return new RouteRun(List.of(
                new RecyclingBin("A", 120, 90, "X", 0),
                new RecyclingBin("B", 120, 70, "X", 0),
                new CompostBin("C", 120, 60, "Y", 40, 30)));
    }

    @Test
    void stopsStartPendingAndProgressInOrder() throws Exception {
        RouteRun run = threeStops();
        assertEquals(3, run.count(StopStatus.PENDING));
        assertEquals(0, run.nextPending());
        run.markCollected(0);
        run.markSkipped(1, "Bin not accessible");
        assertEquals(2, run.nextPending());
        assertFalse(run.isFinished());
        run.markCollected(2);
        assertTrue(run.isFinished());
        assertEquals(2, run.count(StopStatus.COLLECTED));
        assertEquals("Bin not accessible", run.skipReason(1));
        assertEquals(-1, run.nextPending());
    }

    @Test
    void aStopCannotBeResolvedTwiceAndSkippingNeedsAReason() throws Exception {
        RouteRun run = threeStops();
        run.markCollected(0);
        assertThrows(IllegalStateException.class, () -> run.markCollected(0));
        assertThrows(IllegalStateException.class, () -> run.markSkipped(0, "late"));
        assertThrows(IllegalArgumentException.class, () -> run.markSkipped(1, "  "));
    }

    @Test
    void collectingAResetsTheBinToJustEmptied() throws Exception {
        RecyclingBin bin = new RecyclingBin("A", 120, 90, "X", 35);
        bin.resetAfterCollection();
        assertEquals(0.0, bin.getFillPercentage());
        assertEquals(0.0, bin.getContaminationPercentage());
        CompostBin compost = new CompostBin("C", 120, 80, "Y", 40, 33);
        compost.resetAfterCollection();
        assertEquals(0.0, compost.getFillPercentage());
        assertEquals(33.0, compost.getTempCelsius()); // temperature is a property of the compost, not the fill
    }
}
