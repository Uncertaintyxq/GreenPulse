package com.greenpulse.analytics;

import com.greenpulse.model.CollectionPolicy;

/**
 * Turns analytics figures into everyday words for the simple analytics view, so staff and drivers
 * read "almost full" and "in about 2 hours" instead of percentages and abbreviations.
 */
public final class PlainLanguage {
    private PlainLanguage() {}

    /** How full a bin is, in words. The two top bands follow the collection policy's thresholds. */
    public static String fillLevel(double percent) {
        if (Double.isNaN(percent)) return "unknown";
        if (percent >= CollectionPolicy.CRITICAL_FILL) return "almost full";
        if (percent >= CollectionPolicy.ROUTE_MIN_FILL) return "getting full";
        if (percent >= 40) return "about half full";
        if (percent >= 15) return "less than half full";
        return "nearly empty";
    }

    /** A rough, friendly duration such as "a few minutes", "about 20 minutes", "about 3 hours" or "about 2 days". */
    public static String duration(double seconds) {
        if (Double.isNaN(seconds) || seconds < 0) return "an unknown time";
        if (seconds < 5 * 60) return "a few minutes";
        double minutes = seconds / 60.0;
        if (minutes < 55) return "about " + Math.round(minutes / 5.0) * 5 + " minutes";
        double hours = minutes / 60.0;
        if (hours < 1.5) return "about an hour";
        if (hours < 23.5) return "about " + Math.round(hours) + " hours";
        long days = Math.max(1, Math.round(hours / 24.0));
        return days == 1 ? "about a day" : "about " + days + " days";
    }

    /** "1 bin" or "3 bins". */
    public static String bins(int count) {
        return count + (count == 1 ? " bin" : " bins");
    }

    /** Capitalises the first letter, for words used at the start of a sentence or as a label. */
    public static String capitalise(String text) {
        return text == null || text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
