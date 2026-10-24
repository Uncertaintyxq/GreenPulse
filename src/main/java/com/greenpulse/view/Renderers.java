package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Table cell renderers shared by the Monitor and Analytics tables. Colours are read as each cell paints. */
final class Renderers {
    private Renderers() {}

    /** Background for a cell: the table's selection colour when selected, the plate colour otherwise. */
    static Color cellBackground(JTable table, boolean selected) {
        return selected ? table.getSelectionBackground() : Theme.color(Theme.SURFACE);
    }

    /** Bin IDs in one code style everywhere: monospaced on a quiet chip. */
    static final class BinCode extends JComponent implements javax.swing.table.TableCellRenderer {
        private String text = "";
        private boolean selected;
        private JTable table;

        BinCode() { setOpaque(true); }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.table = table; this.selected = isSelected; this.text = value == null ? "" : value.toString();
            setToolTipText(text);
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(cellBackground(table, selected));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(Theme.monoBold(13));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text) + 14, h = 24, x = 12, y = (getHeight() - h) / 2;
            g.setColor(Theme.color(selected ? Theme.SURFACE : Theme.SURFACE2));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
            g.setColor(Theme.color(Theme.LINE));
            g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 6, 6));
            g.setColor(Theme.color(Theme.INK));
            g.drawString(text, x + 7, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** Priority scores as right-aligned figures; scores past the critical line are bold red. */
    static final class Priority extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            boolean critical = value instanceof Integer p && p > CollectionPolicy.CRITICAL_PRIORITY;
            setFont(critical ? Theme.monoBold(14) : Theme.mono(14));
            setForeground(Theme.color(critical ? Theme.CRITICAL_TEXT : Theme.INK));
            setBackground(cellBackground(table, isSelected));
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 24));
            return this;
        }
    }

    /** A quiet link-style cell ("Dispatch →") for actions that open another page. */
    static final class Link extends DefaultTableCellRenderer {
        private final String label;
        Link(String label) { this.label = label; }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, label, isSelected, false, row, column);
            setFont(Theme.semibold(14));
            setForeground(Theme.color(Theme.OK_TEXT));
            setBackground(cellBackground(table, isSelected));
            setIcon(Icons.of(Glyph.ARROW_RIGHT, 14, Theme.OK_TEXT));
            setHorizontalTextPosition(SwingConstants.LEFT);
            setIconTextGap(6);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setToolTipText("Open Dispatch to plan a route");
            return this;
        }
    }

    /** Plain text cells in the table's body style. */
    static final class Text extends DefaultTableCellRenderer {
        private final String colorKey;
        private final boolean bold;
        Text(String colorKey, boolean bold) { this.colorKey = colorKey; this.bold = bold; }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            setFont(bold ? Theme.semibold(14) : Theme.regular(14));
            setForeground(Theme.color(colorKey));
            setBackground(cellBackground(table, isSelected));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setToolTipText(value == null ? null : value.toString());
            return this;
        }
    }
}
