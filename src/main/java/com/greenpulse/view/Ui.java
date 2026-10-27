package com.greenpulse.view;

import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Shared building blocks for every GreenPulse screen, so spacing, type and colour stay consistent. */
final class Ui {
    private Ui() {}

    static final int ARC = 14;
    /** Room a sheet keeps below itself for its soft shadow. */
    static final int SHADOW = 3;
    /** Table client property: model column indexes whose header and figures align right. */
    static final String RIGHT_ALIGNED_COLUMNS = "gp.rightColumns";

    // ------------------------------------------------------------------ surfaces

    /** The page ground: one soft, flat tone under the white sheets. */
    static class Ground extends JPanel {
        Ground(LayoutManager layout) { super(layout); setOpaque(true); }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(Theme.color(Theme.GROUND));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * A white sheet with a hairline edge and a soft shadow: tables, charts and forms sit on sheets.
     * A padding of 0 or 1 means a table or figure grid fills the sheet; it is then kept clear of the rounded corners.
     */
    static class Plate extends JPanel {
        Plate(LayoutManager layout, int padding) {
            super(layout);
            setOpaque(false);
            setBorder(padding <= 1 ? new EmptyBorder(8, 1, 8 + SHADOW, 1)
                    : new EmptyBorder(padding, padding, padding + SHADOW, padding));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintSheet(g, 0, 0, getWidth(), getHeight() - SHADOW, ARC);
            g.dispose();
        }
    }

    /** Two soft shadow steps, the white surface, then a hairline: the one way a sheet is drawn. */
    static void paintSheet(Graphics2D g, double x, double y, double w, double h, int arc) {
        g.setColor(Theme.color(Theme.SHADOW_FAR));
        g.fill(new RoundRectangle2D.Double(x - 0.5, y + 2, w + 1, h + 1, arc + 2, arc + 2));
        g.setColor(Theme.color(Theme.SHADOW_NEAR));
        g.fill(new RoundRectangle2D.Double(x, y + 1, w, h, arc, arc));
        RoundRectangle2D sheet = new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, arc, arc);
        g.setColor(Theme.color(Theme.SURFACE));
        g.fill(sheet);
        g.setColor(Theme.color(Theme.LINE));
        g.draw(sheet);
    }

    /** A border that draws hairlines on the chosen sides in a colour token, read at paint time. */
    static AbstractBorder hairline(String key, int top, int left, int bottom, int right) {
        return new AbstractBorder() {
            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(Theme.color(key));
                if (top > 0) g.fillRect(x, y, w, top);
                if (bottom > 0) g.fillRect(x, y + h - bottom, w, bottom);
                if (left > 0) g.fillRect(x, y, left, h);
                if (right > 0) g.fillRect(x + w - right, y, right, h);
            }
            @Override public Insets getBorderInsets(Component c) { return new Insets(top, left, bottom, right); }
        };
    }

    // ------------------------------------------------------------------ text

    static JLabel label(String text, Font font, String colorKey) {
        JLabel l = new JLabel(text) {
            // Let labels in vertical stacks take the row's width, so text that grows (a timestamp) is never clipped.
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        l.setFont(font);
        Theme.fg(l, colorKey);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JLabel title(String text) { return label(text, Theme.display(26), Theme.INK); }
    static JLabel subtitle(String text) { return label(text, Theme.regular(14), Theme.INK2); }
    static JLabel sectionTitle(String text) { return label(text, Theme.semibold(16), Theme.INK); }
    static JLabel body(String text) { return label(text, Theme.regular(14), Theme.INK2); }
    static JLabel caption(String text) { return label(text, Theme.regular(13), Theme.INK3); }
    static JLabel fieldLabel(String text) { return label(text, Theme.medium(13), Theme.INK2); }

    /**
     * Text that wraps to the given width in screen pixels; it follows the label's own colour, so it survives theme
     * switches. Swing's HTML renderer counts a CSS px as 1.3 screen pixels and a pt as exactly one, hence pt.
     */
    static String wrap(String text, int widthPx) {
        String safe = text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + widthPx + "pt'>" + safe + "</div></html>";
    }

    /** Page title and one-line purpose, with optional controls on the right. */
    static JPanel pageHeader(String title, String subtitle, JComponent trailing) {
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(title(title));
        if (subtitle != null) { words.add(Box.createVerticalStrut(Theme.S1)); words.add(subtitle(subtitle)); }
        JPanel header = new JPanel(new BorderLayout(Theme.S4, 0));
        header.setOpaque(false);
        header.add(words, BorderLayout.CENTER);
        if (trailing != null) {
            JPanel right = new JPanel(new GridBagLayout());
            right.setOpaque(false);
            right.add(trailing);
            header.add(right, BorderLayout.EAST);
        }
        return header;
    }

    /** A sheet heading: title, optional explanation underneath. */
    static JPanel plateHeader(String title, String subtitle) {
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(sectionTitle(title));
        if (subtitle != null) { words.add(Box.createVerticalStrut(3)); words.add(caption(subtitle)); }
        return words;
    }

    /** A bin ID on its quiet chip, the same code style the tables use. */
    static JComponent codeChip(String code) {
        return new JComponent() {
            { setPreferredSize(new Dimension(textWidth() + 18, 26)); setToolTipText(code); }
            private int textWidth() { return getFontMetrics(Theme.monoBold(13)).stringWidth(code); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintCodeChip(g, code, 0, getHeight() / 2, false);
                g.dispose();
            }
        };
    }

    /** Draws a bin code chip with its left edge at x, centred on centerY; returns its width. */
    static int paintCodeChip(Graphics2D g, String code, int x, int centerY, boolean onSelection) {
        g.setFont(Theme.monoBold(13));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(code) + 16, h = 24, y = centerY - h / 2;
        g.setColor(Theme.color(onSelection ? Theme.SURFACE : Theme.SURFACE2));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 7, 7));
        g.setColor(Theme.color(Theme.LINE));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 7, 7));
        g.setColor(Theme.color(Theme.INK));
        g.drawString(code, x + 8, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        return w;
    }

    /** How urgently a bin needs someone; picks a status pill's colours. */
    enum Tone { CRITICAL, DUE, NORMAL }

    /** A status pill led by a state dot. */
    static JComponent pill(String text, Tone tone) {
        return new JComponent() {
            { setPreferredSize(new Dimension(pillWidth(getFontMetrics(Theme.medium(13)), text), 26)); setToolTipText(text); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(Theme.medium(13));
                paintPill(g, text, tone, getWidth() - pillWidth(g.getFontMetrics(), text), getHeight() / 2);
                g.dispose();
            }
        };
    }

    static int pillWidth(FontMetrics fm, String text) { return fm.stringWidth(text) + 31; }

    /** Draws a status pill with its left edge at x, centred on centerY, in the current font. */
    static void paintPill(Graphics2D g, String text, Tone tone, int x, int centerY) {
        String soft, ink, dot;
        switch (tone) {
            case CRITICAL -> { soft = Theme.CRITICAL_SOFT; ink = Theme.CRITICAL_TEXT; dot = Theme.CRITICAL; }
            case DUE -> { soft = Theme.WARNING_SOFT; ink = Theme.WARNING_TEXT; dot = Theme.WARNING_BAR; }
            default -> { soft = Theme.SURFACE2; ink = Theme.INK2; dot = Theme.OK; }
        }
        FontMetrics fm = g.getFontMetrics();
        int w = pillWidth(fm, text), h = 24, y = centerY - h / 2;
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, w, h, h, h);
        g.setColor(Theme.color(soft));
        g.fill(shape);
        if (tone == Tone.NORMAL) { // a neutral pill needs an edge to read on a white sheet
            g.setColor(Theme.color(Theme.LINE));
            g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, h - 1, h - 1));
        }
        g.setColor(Theme.color(dot));
        g.fill(new Ellipse2D.Double(x + 10, centerY - 3, 6, 6));
        g.setColor(Theme.color(ink));
        g.drawString(text, x + 22, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    /** A round avatar with the person's initials. */
    static final class Avatar extends JComponent {
        private final String initials;
        private final int size;

        Avatar(String fullName, int size) {
            this.initials = initials(fullName);
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            g.setColor(Theme.color(Theme.ACCENT_SOFT));
            g.fill(new Ellipse2D.Double(x, y, size, size));
            g.setFont(Theme.semibold(size * 0.38f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.color(Theme.ACCENT_TEXT));
            g.drawString(initials, x + (size - fm.stringWidth(initials)) / 2f, y + (size + fm.getAscent() - fm.getDescent()) / 2f);
            g.dispose();
        }

        static String initials(String name) {
            if (name == null || name.isBlank()) return "?";
            String[] parts = name.trim().split("\\s+");
            String first = parts[0].substring(0, 1), last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
            return (first + last).toUpperCase(Locale.ROOT);
        }
    }

    /** The GreenPulse mark: a rounded green tile with a white pulse line. */
    static final class LogoMark extends JComponent {
        private final int size;

        LogoMark(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate((getWidth() - size) / 2.0, (getHeight() - size) / 2.0);
            g.setColor(Theme.color(Theme.ACCENT));
            g.fill(new RoundRectangle2D.Double(0, 0, size, size, size * 0.34, size * 0.34));
            double s = size / 24.0;
            g.scale(s, s);
            Path2D pulse = new Path2D.Double();
            pulse.moveTo(4.5, 12.5); pulse.lineTo(8.5, 12.5); pulse.lineTo(10.5, 7.5);
            pulse.lineTo(13.5, 17); pulse.lineTo(15.5, 12.5); pulse.lineTo(19.5, 12.5);
            g.setColor(Theme.color(Theme.ACCENT_INK));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(pulse);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ controls

    enum Kind { PRIMARY, SECONDARY, DANGER, QUIET }

    static JButton button(String text, Glyph glyph, Kind kind) {
        JButton b = new JButton(text);
        if (glyph != null) { b.setIcon(Icons.of(glyph, 16)); b.setIconTextGap(Theme.S2); }
        b.setFont(Theme.medium(14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Theme.onThemeChange(b, () -> styleButton(b, kind));
        return b;
    }

    static JButton primary(String text, Glyph glyph) { return button(text, glyph, Kind.PRIMARY); }
    static JButton secondary(String text, Glyph glyph) { return button(text, glyph, Kind.SECONDARY); }
    static JButton danger(String text, Glyph glyph) { return button(text, glyph, Kind.DANGER); }
    static JButton quiet(String text, Glyph glyph) { return button(text, glyph, Kind.QUIET); }

    static void styleButton(JButton b, Kind kind) {
        switch (kind) {
            case PRIMARY -> b.putClientProperty("FlatLaf.style", "background: " + hex(Theme.color(Theme.ACCENT))
                    + "; foreground: " + hex(Theme.color(Theme.ACCENT_INK))
                    + "; borderColor: " + hex(Theme.color(Theme.ACCENT))
                    + "; hoverBackground: " + hex(Theme.color(Theme.ACCENT_HOVER))
                    + "; hoverBorderColor: " + hex(Theme.color(Theme.ACCENT_HOVER))
                    + "; pressedBackground: " + hex(Theme.color(Theme.ACCENT_PRESSED))
                    + "; focusedBorderColor: " + hex(Theme.color(Theme.ACCENT)));
            case SECONDARY -> b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.INK)));
            case DANGER -> b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.CRITICAL_TEXT))
                    + "; borderColor: " + hex(Theme.color(Theme.CRITICAL_TEXT), 0x66)
                    + "; hoverBorderColor: " + hex(Theme.color(Theme.CRITICAL_TEXT))
                    + "; hoverBackground: " + hex(Theme.color(Theme.CRITICAL_SOFT))
                    + "; pressedBackground: " + hex(Theme.color(Theme.CRITICAL_SOFT)));
            case QUIET -> {
                b.putClientProperty("JButton.buttonType", "borderless");
                b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.ACCENT_TEXT)));
            }
        }
    }

    static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }
    static String hex(Color c, int alpha) { return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), alpha); }

    /** Consistent text-field size and type; the placeholder replaces a separate hint line where it fits. */
    static <T extends JComponent> T field(T field, String placeholder) {
        field.setFont(Theme.regular(14));
        if (placeholder != null) field.putClientProperty("JTextField.placeholderText", placeholder);
        Dimension pref = field.getPreferredSize();
        field.setPreferredSize(new Dimension(Math.max(pref.width, 200), 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    /** A label above its field, with the gap the whole app uses. */
    static JPanel labelled(String label, JComponent field) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        JLabel l = fieldLabel(label);
        group.add(l);
        group.add(Box.createVerticalStrut(6));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(field);
        group.putClientProperty("gp.label", l);
        return group;
    }

    /** A row of mutually exclusive options that reads as one control (filters, theme, speed). */
    static final class Segmented extends JPanel {
        private final List<JToggleButton> buttons = new ArrayList<>();
        private IntConsumer onSelect = i -> {};

        Segmented(String... options) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(new EmptyBorder(3, 3, 3, 3));
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < options.length; i++) {
                final int index = i;
                JToggleButton t = new SegmentButton(options[i]);
                t.addActionListener(e -> { repaint(); onSelect.accept(index); });
                group.add(t);
                buttons.add(t);
                if (i > 0) add(Box.createHorizontalStrut(2));
                add(t);
            }
            buttons.get(0).setSelected(true);
        }

        void onSelect(IntConsumer listener) { this.onSelect = listener; }
        void select(int index) { buttons.get(index).setSelected(true); repaint(); }
        int selectedIndex() { for (int i = 0; i < buttons.size(); i++) if (buttons.get(i).isSelected()) return i; return -1; }
        JToggleButton button(int index) { return buttons.get(index); }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(Theme.color(Theme.SURFACE));
            g.fill(shape);
            g.setColor(UIManager.getColor("Button.borderColor"));
            g.draw(shape);
            g.dispose();
        }
    }

    /** One option in a {@link Segmented}: the chosen one sits on a soft green chip, like the current page. */
    private static final class SegmentButton extends JToggleButton {
        SegmentButton(String text) {
            super(text);
            setFont(Theme.medium(13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(7, 14, 7, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 9, 9);
            if (isSelected()) {
                g.setColor(Theme.color(Theme.ACCENT_SOFT));
                g.fill(shape);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.color(Theme.SURFACE2));
                g.fill(shape);
            }
            if (isFocusOwner()) {
                g.setColor(Theme.color(Theme.ACCENT));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 9, 9));
            }
            g.dispose();
            setForeground(Theme.color(isSelected() ? Theme.ACCENT_TEXT : Theme.INK2));
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------------ containers

    /** Page content that always fits the window's width and scrolls only vertically. */
    static class ScrollBody extends JPanel implements Scrollable {
        ScrollBody(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(18, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport v && v.getHeight() > getPreferredSize().height;
        }
    }

    /** A transparent vertical scroller for page content. */
    static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    /** A table sitting directly on a sheet: no outer border, header and body in the sheet's colours. */
    static JScrollPane tableScroll(JTable table) {
        table.setFont(Theme.regular(14));
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.medium(13));
        header.setReorderingAllowed(false);
        javax.swing.table.TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((t, value, selected, focused, row, column) -> {
            Component comp = base.getTableCellRendererComponent(t, value, selected, focused, row, column);
            if (comp instanceof JLabel l) {
                boolean right = false;
                if (t.getClientProperty(RIGHT_ALIGNED_COLUMNS) instanceof int[] cols) {
                    int model = t.convertColumnIndexToModel(column);
                    for (int c : cols) right |= c == model;
                }
                l.setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, right ? 24 : 14));
            }
            return comp;
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        Theme.onThemeChange(scroll, () -> scroll.getViewport().setBackground(Theme.color(Theme.SURFACE)));
        return scroll;
    }

    static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    static JPanel column(Component... children) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component c : children) {
            if (c instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(c);
        }
        return p;
    }
}
