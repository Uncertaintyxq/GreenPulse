package com.greenpulse.view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Status as a pill led by a state dot: red for critical, amber for due, a quiet neutral pill for normal.
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
        g.setFont(Theme.medium(13));
        switch (status) {
            case "CRITICAL" -> Ui.paintPill(g, "Critical", Ui.Tone.CRITICAL, 14, getHeight() / 2);
            case "DUE" -> Ui.paintPill(g, "Due", Ui.Tone.DUE, 14, getHeight() / 2);
            default -> Ui.paintPill(g, "Normal", Ui.Tone.NORMAL, 14, getHeight() / 2);
        }
        g.dispose();
    }
}
