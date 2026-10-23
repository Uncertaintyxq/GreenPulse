package com.greenpulse.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.greenpulse.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UserProfilePanel extends JPanel {
    private final User user;
    private static final Color BG = new Color(245, 248, 250);
    private static final Color INK = new Color(24, 39, 55);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color BORDER = new Color(225, 232, 238);

    private static boolean isDarkMode = false;
    private JButton btnLightMode, btnDarkMode;

    public UserProfilePanel(User user) { 
        this.user = user; 
        initUI(); 
    }

    private void initUI() {
        setLayout(new BorderLayout()); 
        setBackground(BG); 
        setBorder(new EmptyBorder(28, 30, 28, 30));

        JPanel page = new JPanel(new BorderLayout(0, 20)); 
        page.setOpaque(false);

        // Header Section
        JPanel heading = new JPanel(); 
        heading.setOpaque(false); 
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("ACCOUNT & PREFERENCES"); 
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10)); 
        eyebrow.setForeground(GREEN);

        JLabel title = new JLabel("User Profile & Theme Settings"); 
        title.setFont(new Font("SansSerif", Font.BOLD, 25)); 
        title.setForeground(INK);

        JLabel sub = new JLabel("Manage your account details and select your preferred interface theme."); 
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13)); 
        sub.setForeground(MUTED);

        heading.add(eyebrow); 
        heading.add(Box.createVerticalStrut(5)); 
        heading.add(title); 
        heading.add(Box.createVerticalStrut(5)); 
        heading.add(sub);
        page.add(heading, BorderLayout.NORTH);

        // Main Center Layout Grid
        JPanel mainContent = new JPanel(new GridLayout(1, 2, 20, 0));
        mainContent.setOpaque(false);

        // Card 1: Account Profile Details
        JPanel userCard = new JPanel(new BorderLayout(0, 20)); 
        userCard.setBackground(Color.WHITE); 
        userCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), 
            new EmptyBorder(24, 26, 24, 26)
        ));

        JPanel identity = new JPanel(); 
        identity.setOpaque(false); 
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));

        JLabel avatar = new JLabel("👤", SwingConstants.CENTER); 
        avatar.setFont(new Font("SansSerif", Font.PLAIN, 42)); 
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(user.getFullName()); 
        name.setFont(new Font("SansSerif", Font.BOLD, 20)); 
        name.setForeground(INK); 
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel role = new JLabel(user.getRole().toUpperCase() + " ROLE"); 
        role.setFont(new Font("SansSerif", Font.BOLD, 11)); 
        role.setForeground(GREEN); 
        role.setAlignmentX(Component.LEFT_ALIGNMENT);

        identity.add(avatar); 
        identity.add(Box.createVerticalStrut(10)); 
        identity.add(name); 
        identity.add(Box.createVerticalStrut(4)); 
        identity.add(role);

        JPanel details = new JPanel(new GridLayout(3, 1, 0, 14)); 
        details.setOpaque(false);
        details.add(detailRow("USERNAME", user.getUsername())); 
        details.add(detailRow("FULL NAME", user.getFullName())); 
        details.add(detailRow("ACCOUNT ACCESS ROLE", user.getRole()));

        userCard.add(identity, BorderLayout.NORTH); 
        userCard.add(details, BorderLayout.CENTER);

        // Card 2: UI Appearance / Theme Selector (Light vs Dark)
        JPanel themeCard = new JPanel();
        themeCard.setLayout(new BoxLayout(themeCard, BoxLayout.Y_AXIS));
        themeCard.setBackground(Color.WHITE);
        themeCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), 
            new EmptyBorder(24, 26, 24, 26)
        ));

        JLabel themeTitle = new JLabel("🎨 Theme Preference");
        themeTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        themeTitle.setForeground(INK);

        JLabel themeSub = new JLabel("Switch between Light and Dark mode for the console interface.");
        themeSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        themeSub.setForeground(MUTED);

        JPanel themeBtnGroup = new JPanel(new GridLayout(1, 2, 12, 0));
        themeBtnGroup.setOpaque(false);
        themeBtnGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        btnLightMode = new JButton("☀️ Light Mode");
        btnDarkMode = new JButton("🌙 Dark Mode");

        styleThemeButton(btnLightMode, !isDarkMode);
        styleThemeButton(btnDarkMode, isDarkMode);

        btnLightMode.addActionListener(e -> applyTheme(false));
        btnDarkMode.addActionListener(e -> applyTheme(true));

        themeBtnGroup.add(btnLightMode);
        themeBtnGroup.add(btnDarkMode);

        JPanel note = new JPanel(new BorderLayout(10, 0)); 
        note.setBackground(new Color(236, 253, 245)); 
        note.setBorder(new EmptyBorder(12, 14, 12, 14));
        
        JLabel noteIcon = new JLabel("ℹ"); 
        noteIcon.setFont(new Font("SansSerif", Font.BOLD, 16)); 
        noteIcon.setForeground(GREEN);

        JLabel noteText = new JLabel("<html><body style='width:220px;color:#065f46;font-size:11px;'>Theme changes take effect instantly across all console panels and navigation tabs.</body></html>"); 
        note.add(noteIcon, BorderLayout.WEST); 
        note.add(noteText, BorderLayout.CENTER);

        themeCard.add(themeTitle);
        themeCard.add(Box.createVerticalStrut(6));
        themeCard.add(themeSub);
        themeCard.add(Box.createVerticalStrut(20));
        themeCard.add(themeBtnGroup);
        themeCard.add(Box.createVerticalStrut(24));
        themeCard.add(note);
        themeCard.add(Box.createVerticalGlue());

        mainContent.add(userCard);
        mainContent.add(themeCard);

        page.add(mainContent, BorderLayout.CENTER); 
        add(page, BorderLayout.CENTER);
    }

    private void styleThemeButton(JButton btn, boolean active) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            btn.setBackground(GREEN);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(226, 232, 240));
            btn.setForeground(new Color(51, 65, 85));
        }
    }

    private void applyTheme(boolean darkMode) {
        if (isDarkMode == darkMode) return;
        isDarkMode = darkMode;

        styleThemeButton(btnLightMode, !isDarkMode);
        styleThemeButton(btnDarkMode, isDarkMode);

        try {
            if (isDarkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception ex) {
            System.err.println("[UserProfilePanel] Could not update UI theme: " + ex.getMessage());
        }
    }

    private JPanel detailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(0, 4)); 
        row.setOpaque(false);
        JLabel l = new JLabel(label); 
        l.setFont(new Font("SansSerif", Font.BOLD, 10)); 
        l.setForeground(MUTED);
        JLabel v = new JLabel(value == null || value.isBlank() ? "—" : value); 
        v.setFont(new Font("SansSerif", Font.PLAIN, 14)); 
        v.setForeground(INK);
        row.add(l, BorderLayout.NORTH); 
        row.add(v, BorderLayout.CENTER); 
        return row;
    }
}
