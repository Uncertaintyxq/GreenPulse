package com.greenpulse.view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class StatusBadgeRenderer extends JLabel implements TableCellRenderer {

    public StatusBadgeRenderer() {
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(new Font("SansSerif", Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String status = value != null ? value.toString() : "NORMAL";
        setText(status);

        if ("CRITICAL".equalsIgnoreCase(status)) {
            setBackground(new Color(254, 226, 226));
            setForeground(new Color(190, 18, 60));
        } else {
            setBackground(new Color(209, 250, 229));
            setForeground(new Color(6, 95, 70));
        }
        return this;
    }
}
