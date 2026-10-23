package com.greenpulse.view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class FillBarRenderer extends JProgressBar implements TableCellRenderer {

    public FillBarRenderer() {
        setOpaque(true);
        setStringPainted(true);
        setFont(new Font("SansSerif", Font.BOLD, 11));
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Number num) {
            int fill = num.intValue();
            setValue(fill);
            setString(fill + "%");

            if (fill > 85) {
                setForeground(new Color(225, 29, 72));
                setBackground(new Color(254, 226, 226));
            } else if (fill > 70) {
                setForeground(new Color(217, 119, 6));
                setBackground(new Color(254, 243, 199));
            } else {
                setForeground(new Color(16, 185, 129));
                setBackground(new Color(209, 250, 229));
            }
        }
        return this;
    }
}
