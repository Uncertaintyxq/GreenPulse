package com.greenpulse.model;

/** Immutable snapshot of the type-specific sensor values of a bin. Unused sensors are {@code null}. */
public record SensorReading(Double temperature, Double moisture, Double contamination) {
}
