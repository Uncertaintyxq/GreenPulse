package com.greenpulse.event;

import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.RecyclingBin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThresholdAlertListenerTest {

    @Test
    void alertsOnceUntilTheBinDropsBelowTheThresholdAgain() throws Exception {
        List<String> alerts = new ArrayList<>();
        ThresholdAlertListener listener = new ThresholdAlertListener((subject, message) -> alerts.add(subject));
        RecyclingBin bin = new RecyclingBin("A1", 120, 50, "Plaza", 0);

        listener.onBinUpdated(bin, ReadingSource.SIMULATOR);          // 50% - fine
        bin.setFillLevel(88);
        listener.onBinUpdated(bin, ReadingSource.SIMULATOR);          // becomes critical - alert
        bin.setFillLevel(95);
        listener.onBinUpdated(bin, ReadingSource.SIMULATOR);          // still critical - no repeat
        assertEquals(1, alerts.size());

        bin.setFillLevel(0);
        listener.onBinUpdated(bin, ReadingSource.COLLECTION);         // emptied
        bin.setFillLevel(90);
        listener.onBinUpdated(bin, ReadingSource.SIMULATOR);          // critical again - alert
        assertEquals(2, alerts.size());
    }
}
