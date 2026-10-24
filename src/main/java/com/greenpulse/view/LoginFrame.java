package com.greenpulse.view;

import com.greenpulse.model.User;
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnLogin;
    private boolean signingIn = false;

    public LoginFrame() { initUI(); }

    private void initUI() {
        setTitle("GreenPulse | Sign in"); setSize(1100, 680); setMinimumSize(new Dimension(940, 600));
        setLocationRelativeTo(null); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // The whole window is the guide sign; the form sits on a plate fixed to it.
        JPanel root = new Ui.Sign(new GridLayout(1, 2, Theme.S6, 0), 14, 26).fullBleed(Theme.SIGN_LATTICE_STRONG);
        root.add(createBrandSign());
        root.add(createForm());
        setContentPane(root);
        getRootPane().setDefaultButton(btnLogin);
    }

    /** The left half: a guide sign naming the three jobs GreenPulse does, with the demo accounts at its foot. */
    private JComponent createBrandSign() {
        JPanel sign = Ui.transparent(new BorderLayout(0, Theme.S5));
        sign.setBorder(new EmptyBorder(Theme.S2, Theme.S2, Theme.S2, 0));

        JPanel lockup = Ui.column(
                Ui.label("GreenPulse", Theme.heavy(34), Theme.SIGN_INK),
                Box.createVerticalStrut(2),
                Ui.label("Waste collection console", Theme.semibold(15), Theme.SIGN_INK2));

        JPanel destinations = new JPanel(new GridLayout(3, 1));
        destinations.setOpaque(false);
        destinations.add(destination("Monitor fill levels", -90, true));
        destinations.add(destination("Dispatch collection routes", 0, true));
        destinations.add(destination("Forecast critical bins", -45, false));
        JPanel middle = Ui.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        middle.add(destinations, c);

        JPanel demo = Ui.transparent(new GridLayout(1, 3, Theme.S2, 0));
        demo.add(createDemoButton("Admin", "admin", "admin123"));
        demo.add(createDemoButton("Staff", "staff", "staff123"));
        demo.add(createDemoButton("Driver", "driver", "driver123"));
        JPanel foot = Ui.column(Ui.label("Demo accounts", Theme.semibold(13), Theme.SIGN_INK2), Box.createVerticalStrut(Theme.S2), demo);

        sign.add(lockup, BorderLayout.NORTH);
        sign.add(middle, BorderLayout.CENTER);
        sign.add(foot, BorderLayout.SOUTH);
        return sign;
    }

    private JComponent destination(String text, double arrowDegrees, boolean ruleBelow) {
        JPanel row = Ui.transparent(new BorderLayout(Theme.S4, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                ruleBelow ? Ui.hairline(Theme.SIGN_INK2, 0, 0, 1, 0) : BorderFactory.createEmptyBorder(),
                new EmptyBorder(Theme.S4, 2, Theme.S4, 2)));
        row.add(Ui.label(text, Theme.bold(22), Theme.SIGN_INK), BorderLayout.CENTER);
        row.add(new SignArrow(arrowDegrees), BorderLayout.EAST);
        return row;
    }

    /** A filled guide-sign arrow; 0 degrees points right, -90 points up. */
    private static final class SignArrow extends JComponent {
        private final double degrees;
        SignArrow(double degrees) { this.degrees = degrees; setPreferredSize(new Dimension(34, 34)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(-13, -3.5); arrow.lineTo(2, -3.5); arrow.lineTo(2, -10); arrow.lineTo(14, 0);
            arrow.lineTo(2, 10); arrow.lineTo(2, 3.5); arrow.lineTo(-13, 3.5); arrow.closePath();
            AffineTransform t = AffineTransform.getTranslateInstance(getWidth() / 2.0, getHeight() / 2.0);
            t.rotate(Math.toRadians(degrees));
            g.setColor(Theme.color(Theme.SIGN_INK));
            g.fill(t.createTransformedShape(arrow));
            g.dispose();
        }
    }

    private JComponent createForm() {
        txtUsername = Ui.field(new JTextField(), "e.g. admin");
        txtPassword = Ui.field(new JPasswordField(), null);
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        txtPassword.addActionListener(e -> performLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        lblError = Ui.label(" ", Theme.semibold(13), Theme.CRITICAL_TEXT);
        btnLogin = Ui.primary("Sign in", null);
        btnLogin.setFont(Theme.bold(15));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnLogin.setPreferredSize(new Dimension(320, 46));
        btnLogin.addActionListener(e -> performLogin());

        JPanel form = Ui.column(
                Ui.title("Sign in"),
                Box.createVerticalStrut(Theme.S1),
                Ui.subtitle("Use your GreenPulse account to continue."),
                Box.createVerticalStrut(Theme.S6),
                Ui.labelled("Username", txtUsername),
                Box.createVerticalStrut(Theme.S4),
                Ui.labelled("Password", txtPassword),
                Box.createVerticalStrut(Theme.S3),
                lblError,
                Box.createVerticalStrut(Theme.S3),
                btnLogin,
                Box.createVerticalStrut(Theme.S5),
                Ui.caption("Protected access for GreenPulse waste operations staff."));
        form.setPreferredSize(new Dimension(360, form.getPreferredSize().height));

        JPanel plate = new Ui.Plate(new GridBagLayout(), 0);
        plate.setBorder(new EmptyBorder(Theme.S6, 44, Theme.S6, 44));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        plate.add(form, c);
        return plate;
    }

    private JButton createDemoButton(String role, String username, String password) {
        JButton btn = new JButton(role);
        btn.setFont(Theme.semibold(14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Sign in as the " + role.toLowerCase() + " demo account");
        Theme.onThemeChange(btn, () -> btn.putClientProperty("FlatLaf.style", "background: " + Ui.hex(Theme.color(Theme.SIGN_DEEP))
                + "; foreground: " + Ui.hex(Theme.color(Theme.SIGN_INK)) + "; borderColor: " + Ui.hex(Theme.color(Theme.SIGN_INK2))
                + "; hoverBackground: " + Ui.hex(Theme.color(Theme.SIGN_HOVER)) + "; hoverBorderColor: " + Ui.hex(Theme.color(Theme.SIGN_INK))
                + "; pressedBackground: " + Ui.hex(Theme.color(Theme.SIGN)) + "; focusedBorderColor: " + Ui.hex(Theme.color(Theme.SIGN_INK))));
        btn.addActionListener(e -> { txtUsername.setText(username); txtPassword.setText(password); performLogin(); });
        return btn;
    }

    private void performLogin() {
        if (signingIn) return;
        String username = txtUsername.getText().trim(); String password = new String(txtPassword.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) { showError("Enter both your username and password."); return; }
        setSigningIn(true);
        // Talk to MySQL off the Swing thread, so the window keeps responding while the connection is made.
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception {
                return new com.greenpulse.dao.UserDAO().authenticate(username, password);
            }
            @Override protected void done() {
                setSigningIn(false);
                try {
                    User loggedUser = get();
                    if (loggedUser != null) { new MainDashboardFrame(loggedUser).setVisible(true); dispose(); }
                    else { showError("Those details did not match. Check them and try again."); txtPassword.selectAll(); txtPassword.requestFocusInWindow(); }
                } catch (Exception ex) {
                    showError("Cannot reach MySQL. Check that the database is running.");
                    System.err.println("[Login] " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void setSigningIn(boolean busy) {
        signingIn = busy;
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Signing in…" : "Sign in");
        if (busy) lblError.setText(" ");
    }

    private void showError(String message) {
        lblError.setIcon(Icons.of(Glyph.WARNING, 16));
        lblError.setText(message);
    }

    public static void main(String[] args) {
        com.greenpulse.App.main(args);
    }
}
