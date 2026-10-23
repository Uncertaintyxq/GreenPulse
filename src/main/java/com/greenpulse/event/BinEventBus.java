package com.greenpulse.event;

import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Publishes bin updates to any registered {@link BinUpdateListener}. A failing listener never blocks the others. */
public final class BinEventBus {
    private static final Logger LOG = Logger.getLogger(BinEventBus.class.getName());
    private static final BinEventBus INSTANCE = new BinEventBus();

    private final List<BinUpdateListener> listeners = new CopyOnWriteArrayList<>();

    private BinEventBus() {}

    public static BinEventBus getInstance() { return INSTANCE; }

    public void subscribe(BinUpdateListener listener) { listeners.add(listener); }

    public void unsubscribe(BinUpdateListener listener) { listeners.remove(listener); }

    public void publish(SmartBin bin, ReadingSource source) {
        for (BinUpdateListener listener : listeners) {
            try {
                listener.onBinUpdated(bin, source);
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "A bin update listener failed", ex);
            }
        }
    }
}
