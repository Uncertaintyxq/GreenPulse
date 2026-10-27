package com.greenpulse.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * The GreenPulse look: the Geist typeface, the colour tokens from {@code com/greenpulse/theme/*.properties},
 * and light/dark switching. Components remember which token they were painted with, so a theme switch
 * repaints every screen in the new colours instead of leaving white sheets on a dark background.
 */
public final class Theme {
    private Theme() {}

    // Colour tokens (defined in FlatLightLaf.properties and FlatDarkLaf.properties).
    public static final String GROUND = "GP.ground", SURFACE = "GP.surface", SURFACE2 = "GP.surface2", SIDEBAR = "GP.sidebar";
    public static final String LINE = "GP.line", LINE2 = "GP.line2", ROW_SELECTED = "GP.rowSelected";
    public static final String SHADOW_NEAR = "GP.shadowNear", SHADOW_FAR = "GP.shadowFar";
    public static final String INK = "GP.ink", INK2 = "GP.ink2", INK3 = "GP.ink3";
    public static final String ACCENT = "GP.accent", ACCENT_HOVER = "GP.accentHover", ACCENT_PRESSED = "GP.accentPressed";
    public static final String ACCENT_INK = "GP.accentInk", ACCENT_SOFT = "GP.accentSoft", ACCENT_TEXT = "GP.accentText";
    public static final String CRITICAL = "GP.critical", CRITICAL_INK = "GP.criticalInk", CRITICAL_TEXT = "GP.criticalText", CRITICAL_SOFT = "GP.criticalSoft";
    public static final String WARNING = "GP.warning", WARNING_INK = "GP.warningInk", WARNING_TEXT = "GP.warningText", WARNING_BAR = "GP.warningBar", WARNING_SOFT = "GP.warningSoft";
    public static final String OK = "GP.ok", OK_TEXT = "GP.okText", OK_SOFT = "GP.okSoft";
    public static final String RECYCLING = "GP.recycling", COMPOST = "GP.compost", FLEET = "GP.fleet";

    // Spacing scale (px).
    public static final int S1 = 4, S2 = 8, S3 = 12, S4 = 16, S5 = 24, S6 = 32, S7 = 48;

    private static final String BG_KEY = "gp.bg", FG_KEY = "gp.fg", REFRESH_KEY = "gp.refresh";
    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static boolean dark;
    private static boolean installed;
    private static Font regular, medium, semibold, bold, mono, monoBold;

    /** Sets up the look and feel with the theme the user chose last time (light by default). */
    public static void install() {
        boolean savedDark = false;
        try { savedDark = PREFS.getBoolean("darkTheme", false); } catch (RuntimeException ignored) { /* no preference store */ }
        loadFonts();
        if (!installed) { FlatLaf.registerCustomDefaultsSource("com.greenpulse.theme"); installed = true; }
        dark = savedDark;
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
    }

    public static boolean isDark() { return dark; }

    /** Switches every open window to the light or dark theme and remembers the choice. */
    public static void setDark(boolean darkMode) {
        if (darkMode == dark) return;
        dark = darkMode;
        try { PREFS.putBoolean("darkTheme", darkMode); } catch (RuntimeException ignored) { /* preference store unavailable */ }
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            reapply(window);
            window.repaint();
        }
    }

    public static Color color(String key) {
        Color c = UIManager.getColor(key);
        return c != null ? c : Color.MAGENTA; // a missing token is obvious on screen rather than silently grey
    }

    // ------------------------------------------------------------------ fonts

    private static void loadFonts() {
        if (regular != null) return;
        regular = load("Geist-Regular.ttf", Font.PLAIN);
        medium = load("Geist-Medium.ttf", Font.PLAIN);
        semibold = load("Geist-SemiBold.ttf", Font.BOLD);
        bold = load("Geist-Bold.ttf", Font.BOLD);
        mono = load("GeistMono-Medium.ttf", Font.PLAIN);
        monoBold = load("GeistMono-SemiBold.ttf", Font.BOLD);
    }

    private static Font load(String file, int fallbackStyle) {
        try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) throw new IllegalStateException("missing font " + file);
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception ex) {
            System.err.println("[Theme] Using the system font instead of " + file + ": " + ex.getMessage());
            return new Font(file.startsWith("GeistMono") ? Font.MONOSPACED : Font.SANS_SERIF, fallbackStyle, 1);
        }
    }

    public static Font regular(float size) { loadFonts(); return regular.deriveFont(size); }
    public static Font medium(float size) { loadFonts(); return medium.deriveFont(size); }
    public static Font semibold(float size) { loadFonts(); return semibold.deriveFont(size); }
    public static Font bold(float size) { loadFonts(); return bold.deriveFont(size); }
    public static Font mono(float size) { loadFonts(); return mono.deriveFont(size); }
    public static Font monoBold(float size) { loadFonts(); return monoBold.deriveFont(size); }

    /** Page titles and big figures: SemiBold, drawn a touch tighter so large Geist reads as one shape. */
    public static Font display(float size) {
        return semibold(size).deriveFont(Map.of(TextAttribute.TRACKING, -0.02f));
    }

    // ------------------------------------------------------------------ token binding

    /** Paints the background with a colour token and keeps it in step with theme switches. */
    public static <T extends JComponent> T bg(T c, String key) {
        c.putClientProperty(BG_KEY, key);
        c.setBackground(color(key));
        return c;
    }

    /** Paints the text with a colour token and keeps it in step with theme switches. */
    public static <T extends JComponent> T fg(T c, String key) {
        c.putClientProperty(FG_KEY, key);
        c.setForeground(color(key));
        return c;
    }

    /** Runs {@code restyle} now and again after every theme switch, for styling that depends on state. */
    public static void onThemeChange(JComponent c, Runnable restyle) {
        c.putClientProperty(REFRESH_KEY, restyle);
        restyle.run();
    }

    private static void reapply(Component component) {
        if (component instanceof JComponent jc) {
            if (jc.getClientProperty(BG_KEY) instanceof String key) jc.setBackground(color(key));
            if (jc.getClientProperty(FG_KEY) instanceof String key) jc.setForeground(color(key));
            if (jc.getClientProperty(REFRESH_KEY) instanceof Runnable restyle) restyle.run();
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) reapply(child);
        }
    }
}
