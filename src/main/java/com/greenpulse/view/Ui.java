package com.greenpulse.view;

import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Shared building blocks for every GreenPulse screen, so spacing, type and colour stay consistent. */
final class Ui {
    private Ui() {}

    static final int ARC = 12;
    /** Table client property: model column indexes whose header and figures align right. */
    static final String RIGHT_ALIGNED_COLUMNS = "gp.rightColumns";

    // ------------------------------------------------------------------ surfaces

    /** The page ground: a faint lattice like the prisms in reflective sign sheeting. */
    static class Ground extends JPanel {
        Ground(LayoutManager layout) { super(layout); setOpaque(true); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(Theme.color(Theme.GROUND));
            g.fillRect(0, 0, getWidth(), getHeight());
            paintLattice(g, getWidth(), getHeight(), Theme.color(Theme.LATTICE), 26);
            g.dispose();
        }
    }

    static void paintLattice(Graphics2D g, int w, int h, Color color, int spacing) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1f));
        double rise = Math.tan(Math.toRadians(60)) * 1.0;
        int span = (int) Math.ceil(h / rise) + w;
        for (int x = -span; x < w + span; x += spacing) {
            g.draw(new Line2D.Double(x, 0, x + h / rise, h));
            g.draw(new Line2D.Double(x, h, x + h / rise, 0));
        }
    }

    /** A white working surface with a hairline edge: tables, charts and forms sit on plates. */
    static class Plate extends JPanel {
        Plate(LayoutManager layout, int padding) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(padding, padding, padding, padding));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g.setColor(Theme.color(Theme.SURFACE));
            g.fill(shape);
            g.setColor(Theme.color(Theme.LINE));
            g.draw(shape);
            g.dispose();
        }
    }

    /** A guide-sign panel: sign green, a white inset border and the sheeting lattice. Text on it is white. */
    static class Sign extends JPanel {
        private final int inset;
        private boolean fullBleed;
        private String latticeKey = Theme.SIGN_LATTICE;

        /** Fills its whole area edge to edge (a window background) with a stronger lattice. */
        Sign fullBleed(String latticeKey) { this.fullBleed = true; this.latticeKey = latticeKey; return this; }

        Sign(LayoutManager layout, int inset, int padding) {
            super(layout);
            this.inset = inset;
            setOpaque(false);
            int p = inset + padding;
            setBorder(new EmptyBorder(p, p, p, p));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape outer = fullBleed ? new Rectangle(0, 0, getWidth(), getHeight())
                    : new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), ARC + 4, ARC + 4);
            g.setColor(Theme.color(Theme.SIGN));
            g.fill(outer);
            Shape clip = g.getClip();
            g.clip(outer);
            paintLattice(g, getWidth(), getHeight(), Theme.color(latticeKey), 22);
            g.setClip(clip);
            g.setColor(Theme.color(Theme.SIGN_INK));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(inset + 0.75, inset + 0.75, getWidth() - 2 * inset - 1.5,
                    getHeight() - 2 * inset - 1.5, ARC - 2, ARC - 2));
            g.dispose();
        }
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

    static JLabel title(String text) { return label(text, Theme.heavy(28), Theme.INK); }
    static JLabel subtitle(String text) { return label(text, Theme.regular(15), Theme.INK2); }
    static JLabel sectionTitle(String text) { return label(text, Theme.bold(17), Theme.INK); }
    static JLabel body(String text) { return label(text, Theme.regular(14), Theme.INK2); }
    static JLabel caption(String text) { return label(text, Theme.regular(13), Theme.INK3); }
    static JLabel fieldLabel(String text) { return label(text, Theme.semibold(13), Theme.INK2); }

    /** Text that wraps to the given width; it follows the label's own colour, so it survives theme switches. */
    static String wrap(String text, int widthPx) {
        String safe = text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + widthPx + "px'>" + safe + "</div></html>";
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

    /** A plate heading: title, optional explanation underneath. */
    static JPanel plateHeader(String title, String subtitle) {
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(sectionTitle(title));
        if (subtitle != null) { words.add(Box.createVerticalStrut(2)); words.add(caption(subtitle)); }
        return words;
    }

    /** A bin ID on its quiet chip, the same code style the tables use. */
    static JComponent codeChip(String code) {
        return new JComponent() {
            { setPreferredSize(new Dimension(textWidth() + 16, 26)); setToolTipText(code); }
            private int textWidth() { return getFontMetrics(Theme.monoBold(13)).stringWidth(code); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = textWidth() + 14, h = 24, y = (getHeight() - h) / 2;
                g.setColor(Theme.color(Theme.SURFACE2));
                g.fill(new RoundRectangle2D.Double(0, y, w, h, 6, 6));
                g.setColor(Theme.color(Theme.LINE));
                g.draw(new RoundRectangle2D.Double(0.5, y + 0.5, w - 1, h - 1, 6, 6));
                g.setFont(Theme.monoBold(13));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Theme.color(Theme.INK));
                g.drawString(code, 7, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
    }

    /** A small rounded status plate in a sign-class colour, like the table status badges. */
    static JComponent badge(String text, String fillKey, String inkKey) {
        return new JComponent() {
            { setPreferredSize(new Dimension(getFontMetrics(Theme.bold(13)).stringWidth(text) + 22, 26)); setToolTipText(text); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(Theme.bold(13));
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(text) + 20, h = 24, x = getWidth() - w, y = (getHeight() - h) / 2;
                g.setColor(Theme.color(fillKey));
                g.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
                g.setColor(Theme.color(inkKey));
                g.drawString(text, x + 10, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
    }

    // ------------------------------------------------------------------ controls

    enum Kind { PRIMARY, SECONDARY, DANGER, QUIET }

    static JButton button(String text, Glyph glyph, Kind kind) {
        JButton b = new JButton(text);
        if (glyph != null) { b.setIcon(Icons.of(glyph, 16)); b.setIconTextGap(Theme.S2); }
        b.setFont(Theme.semibold(14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Theme.onThemeChange(b, () -> styleButton(b, kind));
        return b;
    }

    static JButton primary(String text, Glyph glyph) { return button(text, glyph, Kind.PRIMARY); }
    static JButton secondary(String text, Glyph glyph) { return button(text, glyph, Kind.SECONDARY); }
    static JButton danger(String text, Glyph glyph) { return button(text, glyph, Kind.DANGER); }
    static JButton quiet(String text, Glyph glyph) { return button(text, glyph, Kind.QUIET); }

    private static void styleButton(JButton b, Kind kind) {
        switch (kind) {
            case PRIMARY -> b.putClientProperty("FlatLaf.style", "background: " + hex(Theme.color(Theme.isDark() ? Theme.SIGN_HOVER : Theme.SIGN))
                    + "; foreground: #FFFFFF; borderColor: " + hex(Theme.color(Theme.SIGN))
                    + "; hoverBackground: " + hex(Theme.color(Theme.isDark() ? Theme.SIGN : Theme.SIGN_HOVER))
                    + "; hoverBorderColor: " + hex(Theme.color(Theme.SIGN_HOVER))
                    + "; pressedBackground: " + hex(Theme.color(Theme.SIGN_DEEP)));
            case SECONDARY -> b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.INK)));
            case DANGER -> b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.CRITICAL_TEXT))
                    + "; borderColor: " + hex(Theme.color(Theme.CRITICAL_TEXT))
                    + "; hoverBorderColor: " + hex(Theme.color(Theme.CRITICAL))
                    + "; hoverBackground: " + hex(Theme.color(Theme.CRITICAL_SOFT)));
            case QUIET -> {
                b.putClientProperty("JButton.buttonType", "borderless");
                b.putClientProperty("FlatLaf.style", "foreground: " + hex(Theme.color(Theme.OK_TEXT)));
            }
        }
    }

    static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

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
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 10, 10);
            g.setColor(Theme.color(Theme.SURFACE));
            g.fill(shape);
            g.setColor(UIManager.getColor("Component.borderColor"));
            g.draw(shape);
            g.dispose();
        }
    }

    /** One option in a {@link Segmented}: the chosen one is a solid sign-green plate with white text. */
    private static final class SegmentButton extends JToggleButton {
        SegmentButton(String text) {
            super(text);
            setFont(Theme.semibold(13));
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
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 7, 7);
            if (isSelected()) {
                g.setColor(Theme.color(Theme.isDark() ? Theme.SIGN_HOVER : Theme.SIGN));
                g.fill(shape);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.color(Theme.SURFACE2));
                g.fill(shape);
            }
            if (isFocusOwner()) {
                g.setColor(Theme.color(Theme.OK));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 7, 7));
            }
            g.dispose();
            setForeground(Theme.color(isSelected() ? Theme.SIGN_INK : Theme.INK2));
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

    /** A table sitting directly on a plate: no outer border, header and body in the plate's colours. */
    static JScrollPane tableScroll(JTable table) {
        table.setFont(Theme.regular(14));
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.semibold(13));
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
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, right ? 24 : 12));
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
