package com.greenpulse.view;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads the real theme files and checks every text and control pair against WCAG AA, in both themes. */
class ThemeContrastTest {

    /** foreground, background, minimum ratio (4.5 for text, 3 for graphics and control borders). */
    private static final Object[][] PAIRS = {
            {"GP.ink", "GP.surface", 4.5}, {"GP.ink2", "GP.surface", 4.5}, {"GP.ink3", "GP.surface", 4.5},
            {"GP.ink2", "GP.ground", 4.5}, {"GP.ink3", "GP.ground", 4.5}, {"GP.ink3", "GP.surface2", 4.5},
            {"GP.ink2", "GP.surface2", 4.5}, {"GP.ink", "GP.rowSelected", 4.5}, {"GP.ink", "GP.sidebar", 4.5},
            {"GP.accentInk", "GP.accent", 4.5}, {"GP.accentInk", "GP.accentHover", 4.5},
            {"GP.accentText", "GP.accentSoft", 4.5}, {"GP.accentText", "GP.surface", 4.5},
            {"GP.criticalText", "GP.surface", 4.5}, {"GP.criticalText", "GP.criticalSoft", 4.5},
            {"GP.warningText", "GP.surface", 4.5}, {"GP.warningText", "GP.warningSoft", 4.5},
            {"GP.okText", "GP.okSoft", 4.5}, {"GP.okText", "GP.surface", 4.5},
            {"GP.critical", "GP.surface", 3.0}, {"GP.warningBar", "GP.surface", 3.0}, {"GP.ok", "GP.surface", 3.0},
            {"GP.recycling", "GP.surface", 3.0}, {"GP.compost", "GP.surface", 3.0},
            {"Component.borderColor", "GP.surface", 3.0}, {"Component.borderColor", "GP.ground", 3.0},
            {"Component.borderColor", "TextField.background", 3.0},
    };

    @ParameterizedTest
    @ValueSource(strings = {"FlatLightLaf.properties", "FlatDarkLaf.properties"})
    void everyPairMeetsWcagAa(String file) throws Exception {
        Properties theme = new Properties();
        try (InputStream in = ThemeContrastTest.class.getResourceAsStream("/com/greenpulse/theme/" + file)) {
            theme.load(in);
        }
        List<String> failures = new ArrayList<>();
        for (Object[] pair : PAIRS) {
            double ratio = contrast(rgb(theme, (String) pair[0]), rgb(theme, (String) pair[1]));
            if (ratio < (double) pair[2]) failures.add(String.format("%s on %s is %.2f:1 (needs %s)", pair[0], pair[1], ratio, pair[2]));
        }
        assertTrue(failures.isEmpty(), file + ": " + failures);
    }

    /** Resolves $references and reads #RRGGBB (an alpha suffix is ignored: these pairs are all opaque). */
    private static int[] rgb(Properties theme, String key) {
        String value = theme.getProperty(key);
        for (int hops = 0; value != null && value.startsWith("$") && hops < 5; hops++) value = theme.getProperty(value.substring(1));
        if (value == null || !value.startsWith("#") || value.length() < 7) throw new IllegalArgumentException("no colour for " + key);
        return new int[]{Integer.parseInt(value.substring(1, 3), 16), Integer.parseInt(value.substring(3, 5), 16), Integer.parseInt(value.substring(5, 7), 16)};
    }

    private static double luminance(int[] c) {
        double[] l = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = c[i] / 255.0;
            l[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
    }

    private static double contrast(int[] a, int[] b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }
}
