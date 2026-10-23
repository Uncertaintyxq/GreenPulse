package com.greenpulse.service;

import com.greenpulse.model.RecyclingBin;
import com.greenpulse.model.SmartBin;
import com.greenpulse.service.RoadRoutingService.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StopLocatorTest {

    /** A straight road heading east: one point every 0.001 degrees of longitude (about 110 m). */
    private static List<Coordinate> road() {
        List<Coordinate> g = new ArrayList<>();
        for (int i = 0; i <= 100; i++) g.add(new Coordinate(7.0, 125.0 + i * 0.001));
        return g;
    }

    private static SmartBin binAt(String id, double lon) throws Exception {
        return new RecyclingBin(id, 120, 50, "X", 0, 7.0001, lon);
    }

    @Test
    void stopsAreMatchedToTheNearestRoutePointInOrder() throws Exception {
        int[] idx = StopLocator.locate(road(), List.of(binAt("A", 125.020), binAt("B", 125.060), binAt("C", 125.095)));
        assertEquals(20, idx[0]);
        assertEquals(60, idx[1]);
        assertEquals(95, idx[2]);
    }

    @Test
    void aLaterStopIsNeverPlacedBeforeAnEarlierOne() throws Exception {
        // B is geographically before A, but it is visited second, so it must be placed at or after A.
        int[] idx = StopLocator.locate(road(), List.of(binAt("A", 125.070), binAt("B", 125.030)));
        assertTrue(idx[1] >= idx[0]);
    }

    @Test
    void emptyGeometryDoesNotCrash() throws Exception {
        assertEquals(1, StopLocator.locate(List.of(), List.of(binAt("A", 125.0))).length);
    }
}
