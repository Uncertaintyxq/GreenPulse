package com.greenpulse.model;

/** Where a stored telemetry reading came from, so simulated and real data can be told apart in reports. */
public enum ReadingSource {
    SENSOR, SIMULATOR, MANUAL, COLLECTION
}
