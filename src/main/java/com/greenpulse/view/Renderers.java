package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

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
            Ui.paintCodeChip(g, text, 14, getHeight() / 2, selected);
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
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 24));
            return this;
        }
    }

    /** A quiet arrow-only cell for a row action that opens another page; the label is its tooltip. */
    static final class Link extends DefaultTableCellRenderer {
        private final String label;
        Link(String label) { this.label = label; }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            setBackground(cellBackground(table, isSelected));
            setIcon(Icons.of(Glyph.ARROW_RIGHT, 16, Theme.INK3));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText(label);
            getAccessibleContext().setAccessibleName(label);
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
            setFont(bold ? Theme.medium(14) : Theme.regular(14));
            setForeground(Theme.color(colorKey));
            setBackground(cellBackground(table, isSelected));
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setToolTipText(value == null ? null : value.toString());
            return this;
        }
    }
}
