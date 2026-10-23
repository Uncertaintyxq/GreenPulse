package com.greenpulse.event;

import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;

/** Observer interface: implement it to react whenever a bin receives a new reading or is emptied. */
@FunctionalInterface
public interface BinUpdateListener {
    void onBinUpdated(SmartBin bin, ReadingSource source);
}
