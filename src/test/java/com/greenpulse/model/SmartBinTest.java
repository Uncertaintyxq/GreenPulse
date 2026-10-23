package com.greenpulse.model;

import com.greenpulse.exception.InvalidFillLevelException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SmartBinTest {

    @Test
    void recyclingPriorityWeighsFillAndContamination() throws Exception {
        RecyclingBin bin = new RecyclingBin("R1", 120, 80, "Plaza", 40);
        assertEquals(68, bin.calculatePriority()); // 80*0.7 + 40*0.3
        assertEquals(BinType.RECYCLING, bin.getBinType());
    }

    @Test
    void compostTemperatureIsNormalisedBeforeWeighting() throws Exception {
        CompostBin cool = new CompostBin("C1", 120, 50, "Market", 50, 15);
        CompostBin hot = new CompostBin("C2", 120, 50, "Market", 50, 60);
        assertEquals(35, cool.calculatePriority());  // 50*0.5 + 0*0.3 + 50*0.2
        assertEquals(65, hot.calculatePriority());   // 50*0.5 + 100*0.3 + 50*0.2
    }

    @Test
    void priorityNeverExceeds100() throws Exception {
        assertEquals(100, new CompostBin("C3", 120, 100, "X", 100, 100).calculatePriority());
    }

    @Test
    void fillLevelOutsideRangeIsRejected() throws Exception {
        RecyclingBin bin = new RecyclingBin("R2", 120, 10, "Plaza", 0);
        assertThrows(InvalidFillLevelException.class, () -> bin.setFillLevel(100.1));
        assertThrows(InvalidFillLevelException.class, () -> bin.setFillLevel(Double.NaN));
        assertEquals(10, bin.getFillPercentage());
    }

    @Test
    void coordinatesMustBeInsideThePhilippines() throws Exception {
        RecyclingBin bin = new RecyclingBin("R3", 120, 10, "Plaza", 0);
        assertThrows(IllegalArgumentException.class, () -> bin.setCoordinates(40.7, -74.0));
        bin.setCoordinates(7.19, 125.45);
        assertTrue(bin.hasCoordinates());
    }

    @Test
    void sensorReadingContainsOnlyTheSensorsTheTypeHas() throws Exception {
        SensorReading recycling = new RecyclingBin("R4", 120, 10, "Plaza", 12).getSensorReading();
        assertEquals(12.0, recycling.contamination());
        assertNull(recycling.temperature());
        SensorReading compost = new CompostBin("C4", 120, 10, "Market", 55, 30).getSensorReading();
        assertEquals(30.0, compost.temperature());
        assertEquals(55.0, compost.moisture());
        assertNull(compost.contamination());
    }

    @Test
    void simulatedDriftStaysInValidRange() throws Exception {
        CompostBin bin = new CompostBin("C5", 120, 10, "Market", 55, 59.9);
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) bin.simulateSensorDrift(random);
        assertTrue(bin.getTempCelsius() >= 15 && bin.getTempCelsius() <= 60);
    }

    @Test
    void factoryBuildsTheRightSubclassFromATypeName() throws Exception {
        assertInstanceOf(RecyclingBin.class, SmartBinFactory.createBin("Recycling", "F1", 100, 5, "A", 3, 0));
        assertInstanceOf(CompostBin.class, SmartBinFactory.createBin("CompostBin", "F2", 100, 5, "A", 40, 25));
        assertThrows(IllegalArgumentException.class, () -> SmartBinFactory.createBin("Glass", "F3", 100, 5, "A", 0, 0));
    }

    @Test
    void policyDecidesCriticalAndRouteBins() throws Exception {
        RecyclingBin full = new RecyclingBin("P1", 120, 90, "A", 0);
        RecyclingBin half = new RecyclingBin("P2", 120, 50, "A", 0);
        RecyclingBin busy = new RecyclingBin("P3", 120, 65, "A", 0);
        assertTrue(CollectionPolicy.isCritical(full));
        assertFalse(CollectionPolicy.isCritical(half));
        assertTrue(CollectionPolicy.needsRoute(busy));
        assertFalse(CollectionPolicy.needsRoute(half));
    }

    @Test
    void rolesDefineWhatEachUserMayDo() {
        assertTrue(Role.ADMIN.canArchiveBins());
        assertFalse(Role.STAFF.canArchiveBins());
        assertTrue(Role.STAFF.canRegisterBins());
        assertFalse(Role.DRIVER.canRegisterBins());
        assertTrue(Role.DRIVER.canRecordCollections());
        assertEquals(Role.DRIVER, new User(1, "d", "Driver", "driver").getRoleType());
    }
}
