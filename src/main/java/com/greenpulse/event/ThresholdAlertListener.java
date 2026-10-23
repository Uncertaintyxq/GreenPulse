package com.greenpulse.event;

import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends one alert when a bin becomes critical, not on every reading while it stays critical.
 * Once the bin drops back below the threshold (for example after a collection) it can alert again.
 */
public class ThresholdAlertListener implements BinUpdateListener {
    private final NotificationChannel channel;
    private final Map<String, Boolean> lastCritical = new ConcurrentHashMap<>();

    public ThresholdAlertListener(NotificationChannel channel) {
        this.channel = channel;
    }

    @Override
    public void onBinUpdated(SmartBin bin, ReadingSource source) {
        boolean critical = bin.getFillPercentage() >= CollectionPolicy.CRITICAL_FILL;
        Boolean previous = lastCritical.put(bin.getBinID(), critical);
        if (critical && !Boolean.TRUE.equals(previous)) {
            channel.send("Critical fill: " + bin.getBinID(),
                    String.format(java.util.Locale.US, "%s at %s reached %.0f%% capacity. Collection is recommended.",
                            bin.getBinID(), bin.getLocation(), bin.getFillPercentage()));
        }
    }
}
