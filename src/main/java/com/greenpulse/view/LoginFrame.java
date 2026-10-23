package com.greenpulse.view;

import com.greenpulse.model.User;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color INK = new Color(24, 39, 55);
    private static final Color MUTED = new Color(100, 116, 139);

    public LoginFrame() { initUI(); }

    private void initUI() {
        setTitle("GreenPulse | Sign in"); setSize(940, 580); setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new GridLayout(1, 2)); root.setBackground(Color.WHITE);

        JPanel brand = new JPanel(new BorderLayout(0, 24)); brand.setBackground(new Color(15, 35, 45));
        brand.setBorder(new EmptyBorder(42, 42, 36, 42));
        JPanel brandTop = new JPanel(); brandTop.setOpaque(false); brandTop.setLayout(new BoxLayout(brandTop, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("♻  GreenPulse"); logo.setFont(new Font("SansSerif", Font.BOLD, 29)); logo.setForeground(new Color(52, 211, 153));
        JLabel kicker = new JLabel("SMART WASTE OPERATIONS"); kicker.setFont(new Font("SansSerif", Font.BOLD, 10)); kicker.setForeground(new Color(148, 163, 184));
        brandTop.add(logo); brandTop.add(Box.createVerticalStrut(10)); brandTop.add(kicker);
        JLabel intro = new JLabel("<html><div style='width:290px;line-height:1.5;color:#dbeafe;font-size:16px'><b>A cleaner way to manage collection.</b><br><br><span style='font-size:12px;color:#a8bac8'>Monitor smart bins, understand fill levels, and plan collection routes with one simple workspace.</span></div></html>");
        JPanel brandBottom = new JPanel(); brandBottom.setOpaque(false); brandBottom.setLayout(new BoxLayout(brandBottom, BoxLayout.Y_AXIS));
        JLabel demoTitle = new JLabel("QUICK DEMO ACCESS"); demoTitle.setFont(new Font("SansSerif", Font.BOLD, 10)); demoTitle.setForeground(new Color(148, 163, 184));
        brandBottom.add(demoTitle); brandBottom.add(Box.createVerticalStrut(10)); brandBottom.add(createQuickDemoBox());
        brand.add(brandTop, BorderLayout.NORTH); brand.add(intro, BorderLayout.CENTER); brand.add(brandBottom, BorderLayout.SOUTH);

        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS)); form.setBackground(Color.WHITE); form.setBorder(new EmptyBorder(56, 52, 48, 52));
        JLabel eyebrow = new JLabel("WELCOME BACK"); eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10)); eyebrow.setForeground(GREEN);
        JLabel title = new JLabel("Sign in to GreenPulse"); title.setFont(new Font("SansSerif", Font.BOLD, 25)); title.setForeground(INK);
        JLabel sub = new JLabel("Use your account details to continue."); sub.setFont(new Font("SansSerif", Font.PLAIN, 12)); sub.setForeground(MUTED);
        txtUsername = new JTextField(); txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 14)); txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        txtPassword = new JPasswordField(); txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 14)); txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        lblError = new JLabel(" "); lblError.setFont(new Font("SansSerif", Font.BOLD, 11)); lblError.setForeground(new Color(190, 24, 93));
        JButton login = new JButton("Sign in  →"); login.setFont(new Font("SansSerif", Font.BOLD, 13)); login.setBackground(GREEN); login.setForeground(Color.WHITE); login.setFocusPainted(false); login.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); login.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin()); txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        form.add(eyebrow); form.add(Box.createVerticalStrut(8)); form.add(title); form.add(Box.createVerticalStrut(8)); form.add(sub); form.add(Box.createVerticalStrut(34));
        form.add(fieldLabel("Username")); form.add(Box.createVerticalStrut(7)); form.add(txtUsername); form.add(Box.createVerticalStrut(18));
        form.add(fieldLabel("Password")); form.add(Box.createVerticalStrut(7)); form.add(txtPassword); form.add(Box.createVerticalStrut(10)); form.add(lblError); form.add(Box.createVerticalStrut(15)); form.add(login);
        JLabel footnote = new JLabel("Protected access · GreenPulse Waste Operations"); footnote.setFont(new Font("SansSerif", Font.PLAIN, 10)); footnote.setForeground(MUTED); footnote.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(22)); form.add(footnote);
        root.add(brand); root.add(form); setContentPane(root); getRootPane().setDefaultButton(login);
    }

    private JLabel fieldLabel(String text) { JLabel l = new JLabel(text); l.setFont(new Font("SansSerif", Font.BOLD, 12)); l.setForeground(INK); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l; }

    private JPanel createQuickDemoBox() {
        JPanel box = new JPanel(new GridLayout(1, 3, 8, 0)); box.setOpaque(false);
        box.add(createDemoButton("Admin", "admin", "admin123")); box.add(createDemoButton("Staff", "staff", "staff123")); box.add(createDemoButton("Driver", "driver", "driver123")); return box;
    }

    private JButton createDemoButton(String role, String username, String password) {
        JButton btn = new JButton("Use " + role); btn.setFont(new Font("SansSerif", Font.BOLD, 11)); btn.setBackground(new Color(30, 55, 65)); btn.setForeground(new Color(226, 232, 240)); btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> { txtUsername.setText(username); txtPassword.setText(password); performLogin(); }); return btn;
    }

    private void performLogin() {
        String username = txtUsername.getText().trim(); String password = new String(txtPassword.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) { lblError.setText("Please enter both your username and password."); return; }
        try {
            User loggedUser = new com.greenpulse.dao.UserDAO().authenticate(username, password);
            if (loggedUser != null) { new MainDashboardFrame(loggedUser).setVisible(true); dispose(); }
            else lblError.setText("Those details did not match. Please try again.");
        } catch (Exception ex) { lblError.setText("Cannot connect to MySQL. Check the database settings."); System.err.println("[Login] " + ex.getMessage()); }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
