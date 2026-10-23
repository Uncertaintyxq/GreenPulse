package com.greenpulse.model;

/**
 * The kinds of smart bin the system understands. The {@code dbValue} is what is stored in
 * {@code smart_bins.bin_type}, so the database no longer depends on Java class names.
 */
public enum BinType {
    RECYCLING("RecyclingBin", "Recycling"),
    COMPOST("CompostBin", "Compost");

    private final String dbValue;
    private final String label;

    BinType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() { return dbValue; }
    public String getLabel() { return label; }

    /** Accepts "RecyclingBin", "Recycling", "RECYCLING" (any case); throws for anything else. */
    public static BinType fromString(String text) {
        if (text != null) {
            String t = text.trim();
            for (BinType type : values()) {
                if (type.dbValue.equalsIgnoreCase(t) || type.label.equalsIgnoreCase(t) || type.name().equalsIgnoreCase(t)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported bin type: " + text);
    }
}
