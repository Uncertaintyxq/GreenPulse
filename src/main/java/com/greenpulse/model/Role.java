package com.greenpulse.model;

/** User roles and what each one is allowed to do. Checked in {@code BinService}, not just hidden in the UI. */
public enum Role {
    ADMIN(true, true, true),
    STAFF(false, true, true),
    DRIVER(false, false, true);

    private final boolean canArchiveBins;
    private final boolean canRegisterBins;
    private final boolean canRecordCollections;

    Role(boolean canArchiveBins, boolean canRegisterBins, boolean canRecordCollections) {
        this.canArchiveBins = canArchiveBins;
        this.canRegisterBins = canRegisterBins;
        this.canRecordCollections = canRecordCollections;
    }

    public boolean canArchiveBins() { return canArchiveBins; }
    public boolean canRegisterBins() { return canRegisterBins; }
    public boolean canRecordCollections() { return canRecordCollections; }

    public static Role fromString(String text) {
        if (text != null) {
            for (Role r : values()) if (r.name().equalsIgnoreCase(text.trim())) return r;
        }
        throw new IllegalArgumentException("Unknown role: " + text);
    }
}
