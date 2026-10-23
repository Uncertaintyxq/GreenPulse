package com.greenpulse;

import com.formdev.flatlaf.FlatLightLaf;
import com.greenpulse.view.LoginFrame;
import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("Button.default.background", new Color(5, 150, 105));
            UIManager.put("Button.default.foreground", Color.WHITE);
            UIManager.put("Component.focusColor", new Color(16, 185, 129));
            UIManager.put("Component.borderColor", new Color(225, 232, 238));
            UIManager.put("Table.selectionBackground", new Color(209, 250, 229));
            UIManager.put("Table.selectionForeground", new Color(24, 39, 55));
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
        } catch (Exception e) {
            System.err.println("Failed to initialize the modern UI theme: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
