package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Fill level as a thin bar plus its figure. Colour follows the collection policy: red critical, amber due, green normal. */
public class FillBarRenderer extends JComponent implements TableCellRenderer {
    private int fill;
    private boolean selected;
    private JTable table;

    public FillBarRenderer() { setOpaque(true); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.selected = isSelected;
        this.fill = value instanceof Number num ? Math.max(0, Math.min(100, num.intValue())) : 0;
        setToolTipText(fill + "% full");
        return this;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Renderers.cellBackground(table, selected));
        g.fillRect(0, 0, getWidth(), getHeight());

        String barKey = fill >= CollectionPolicy.CRITICAL_FILL ? Theme.CRITICAL
                : fill >= CollectionPolicy.ROUTE_MIN_FILL ? Theme.WARNING_BAR : Theme.OK;
        String textKey = fill >= CollectionPolicy.CRITICAL_FILL ? Theme.CRITICAL_TEXT : Theme.INK;

        g.setFont(fill >= CollectionPolicy.CRITICAL_FILL ? Theme.monoBold(14) : Theme.mono(14));
        FontMetrics fm = g.getFontMetrics();
        String label = fill + "%";
        int textW = fm.stringWidth("100%");
        int x = 14, barH = 6, barW = Math.max(30, getWidth() - x - textW - 30), y = (getHeight() - barH) / 2;
        g.setColor(Theme.color(Theme.LINE2));
        g.fill(new RoundRectangle2D.Double(x, y, barW, barH, barH, barH));
        g.setColor(Theme.color(barKey));
        int w = (int) Math.round(barW * fill / 100.0);
        if (w > 0) g.fill(new RoundRectangle2D.Double(x, y, Math.max(w, barH), barH, barH, barH));
        g.setColor(Theme.color(textKey));
        g.drawString(label, x + barW + 12 + textW - fm.stringWidth(label), (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }
}
