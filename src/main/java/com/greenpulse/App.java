package com.greenpulse;

import com.greenpulse.view.LoginFrame;
import com.greenpulse.view.Theme;
import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try {
            Theme.install();
        } catch (Exception e) {
            System.err.println("Failed to initialize the GreenPulse theme: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
