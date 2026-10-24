package com.greenpulse.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlainLanguageTest {

    @Test
    void fillLevelUsesPolicyThresholdsAtTheTop() {
        assertEquals("almost full", PlainLanguage.fillLevel(85));
        assertEquals("getting full", PlainLanguage.fillLevel(84.9));
        assertEquals("getting full", PlainLanguage.fillLevel(60));
        assertEquals("about half full", PlainLanguage.fillLevel(45));
        assertEquals("less than half full", PlainLanguage.fillLevel(20));
        assertEquals("nearly empty", PlainLanguage.fillLevel(0));
        assertEquals("unknown", PlainLanguage.fillLevel(Double.NaN));
    }

    @Test
    void durationRoundsToFriendlyUnits() {
        assertEquals("a few minutes", PlainLanguage.duration(90));
        assertEquals("about 20 minutes", PlainLanguage.duration(21 * 60));
        assertEquals("about an hour", PlainLanguage.duration(70 * 60));
        assertEquals("about 3 hours", PlainLanguage.duration(3 * 3600 + 600));
        assertEquals("about a day", PlainLanguage.duration(30 * 3600));
        assertEquals("about 2 days", PlainLanguage.duration(40 * 3600));
        assertEquals("about 3 days", PlainLanguage.duration(3 * 86400));
        assertEquals("an unknown time", PlainLanguage.duration(Double.NaN));
    }

    @Test
    void countsReadNaturally() {
        assertEquals("1 bin", PlainLanguage.bins(1));
        assertEquals("4 bins", PlainLanguage.bins(4));
        assertEquals("Almost full", PlainLanguage.capitalise("almost full"));
    }
}
