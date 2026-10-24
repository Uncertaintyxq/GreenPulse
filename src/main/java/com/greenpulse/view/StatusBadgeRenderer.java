package com.greenpulse.view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Status as a sign plate: regulatory red for critical, advisory yellow for due, a quiet green plate for normal.
 * The table keeps the raw values (CRITICAL, DUE, NORMAL) so filters can match them.
 */
public class StatusBadgeRenderer extends JComponent implements TableCellRenderer {
    private String status = "NORMAL";
    private boolean selected;
    private JTable table;

    public StatusBadgeRenderer() { setOpaque(true); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.selected = isSelected;
        this.status = value != null ? value.toString().toUpperCase(java.util.Locale.ROOT) : "NORMAL";
        setToolTipText(switch (status) {
            case "CRITICAL" -> "Needs collection now";
            case "DUE" -> "Included in the next route";
            default -> "No action needed";
        });
        return this;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Renderers.cellBackground(table, selected));
        g.fillRect(0, 0, getWidth(), getHeight());

        String text, fill, ink;
        switch (status) {
            case "CRITICAL" -> { text = "Critical"; fill = Theme.CRITICAL; ink = Theme.CRITICAL_INK; }
            case "DUE" -> { text = "Due"; fill = Theme.WARNING; ink = Theme.WARNING_INK; }
            default -> { text = "Normal"; fill = Theme.OK_SOFT; ink = Theme.OK_TEXT; }
        }
        g.setFont(Theme.bold(13));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 20, h = 24, x = 12, y = (getHeight() - h) / 2;
        g.setColor(Theme.color(fill));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
        g.setColor(Theme.color(ink));
        g.drawString(text, x + 10, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
