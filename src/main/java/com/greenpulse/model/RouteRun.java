package com.greenpulse.model;

import java.util.List;

/**
 * Progress of one route: every stop is pending until a driver collects it or skips it (with a reason).
 * This is the in-memory view; the database copy lives in the {@code route_stops} table.
 */
public final class RouteRun {
    public enum StopStatus { PENDING, COLLECTED, SKIPPED }

    /** Reasons offered to a driver when a stop cannot be collected. */
    public static final List<String> SKIP_REASONS = List.of(
            "Bin not accessible", "Bin not full enough", "Vehicle is full", "Out of time", "Bin damaged or missing");

    private final List<SmartBin> bins;
    private final StopStatus[] status;
    private final String[] reason;

    public RouteRun(List<SmartBin> orderedBins) {
        this.bins = List.copyOf(orderedBins);
        this.status = new StopStatus[bins.size()];
        this.reason = new String[bins.size()];
        java.util.Arrays.fill(status, StopStatus.PENDING);
    }

    public int size() { return bins.size(); }
    public SmartBin bin(int index) { return bins.get(index); }
    public StopStatus status(int index) { return status[index]; }
    /** The reason a stop was skipped, or null. */
    public String skipReason(int index) { return reason[index]; }

    public void markCollected(int index) {
        requirePending(index);
        status[index] = StopStatus.COLLECTED;
    }

    public void markSkipped(int index, String why) {
        requirePending(index);
        if (why == null || why.isBlank()) throw new IllegalArgumentException("A reason is required to skip a stop.");
        status[index] = StopStatus.SKIPPED;
        reason[index] = why.trim();
    }

    public int count(StopStatus wanted) {
        int n = 0;
        for (StopStatus s : status) if (s == wanted) n++;
        return n;
    }

    /** Index of the first stop still waiting, or -1 when every stop has been resolved. */
    public int nextPending() {
        for (int i = 0; i < status.length; i++) if (status[i] == StopStatus.PENDING) return i;
        return -1;
    }

    public boolean isFinished() { return nextPending() < 0; }

    private void requirePending(int index) {
        if (status[index] != StopStatus.PENDING) {
            throw new IllegalStateException("Stop " + (index + 1) + " (" + bins.get(index).getBinID() + ") is already " + status[index].name().toLowerCase() + ".");
        }
    }
}
