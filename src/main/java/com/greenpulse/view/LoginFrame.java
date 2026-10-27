package com.greenpulse.view;

import com.greenpulse.config.DatabaseConnection;
import com.greenpulse.dao.UserDAO;
import com.greenpulse.model.Role;
import com.greenpulse.model.User;
import com.greenpulse.service.AccountRules;
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class LoginFrame extends JFrame {
    private static final int FORM_WIDTH = 340;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnLogin;
    private boolean signingIn = false;

    private JTextField txtFullName, txtNewUsername;
    private JPasswordField txtNewPassword;
    private JComboBox<String> comboRole;
    private JLabel lblSignUpError;
    private JButton btnCreate;
    private boolean creating = false;

    private JPanel sheet;
    private JComponent signInForm, signUpForm;

    public LoginFrame() { initUI(); }

    private void initUI() {
        setTitle("GreenPulse | Sign in"); setSize(1100, 680); setMinimumSize(new Dimension(940, 600));
        setLocationRelativeTo(null); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // A calm ground: what GreenPulse does on the left, the sign-in sheet on the right.
        JPanel root = new Ui.Ground(new BorderLayout(Theme.S7, 0));
        root.setBorder(new EmptyBorder(40, 56, 40, 48));
        root.add(createIntro(), BorderLayout.CENTER);
        root.add(createForm(), BorderLayout.EAST);
        setContentPane(root);
        getRootPane().setDefaultButton(btnLogin);
        // The demo buttons come first in focus order; start where a returning user types instead.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { txtUsername.requestFocusInWindow(); }
        });
    }

    /** The left side: the product in one sentence and three jobs, with the demo accounts at its foot. */
    private JComponent createIntro() {
        JPanel intro = Ui.transparent(new BorderLayout(0, Theme.S5));

        JPanel brand = Ui.transparent(null);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new Ui.LogoMark(34));
        brand.add(Box.createHorizontalStrut(Theme.S3));
        brand.add(Ui.label("GreenPulse", Theme.display(22), Theme.INK));

        JPanel pitch = Ui.column(
                Ui.label("Waste collection, planned", Theme.display(36), Theme.INK),
                Ui.label("on live fill levels.", Theme.display(36), Theme.INK),
                Box.createVerticalStrut(Theme.S3),
                Ui.label(Ui.wrap("See which bins need emptying, send a truck along a road route, and know which bins fill next.", 420),
                        Theme.regular(16), Theme.INK2),
                Box.createVerticalStrut(Theme.S6),
                feature(Glyph.MONITOR, "Monitor fill levels", "Every bin's latest reading, ranked by collection priority."),
                Box.createVerticalStrut(Theme.S4),
                feature(Glyph.DISPATCH, "Dispatch collection routes", "A road-based stop order on a real street map."),
                Box.createVerticalStrut(Theme.S4),
                feature(Glyph.ANALYTICS, "Forecast critical bins", "How long each bin has before it needs collecting."));
        JPanel middle = Ui.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        middle.add(pitch, c);

        JPanel demo = Ui.transparent(new GridLayout(1, 3, Theme.S2, 0));
        demo.add(createDemoButton("Admin", "admin", "admin123"));
        demo.add(createDemoButton("Staff", "staff", "staff123"));
        demo.add(createDemoButton("Driver", "driver", "driver123"));
        JPanel demoRow = Ui.transparent(new BorderLayout());
        demoRow.add(demo, BorderLayout.WEST);
        JPanel foot = Ui.column(Ui.caption("Demo accounts"), Box.createVerticalStrut(Theme.S2), demoRow);

        intro.add(brand, BorderLayout.NORTH);
        intro.add(middle, BorderLayout.CENTER);
        intro.add(foot, BorderLayout.SOUTH);
        return intro;
    }

    /** One job GreenPulse does: its icon on a small white tile, a title and one line. */
    private JComponent feature(Glyph glyph, String title, String detail) {
        JComponent tile = new JComponent() {
            { setPreferredSize(new Dimension(42, 42 + Ui.SHADOW)); setMaximumSize(getPreferredSize()); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Ui.paintSheet(g, 0, 0, 42, 42, 12);
                Icons.of(glyph, 20, Theme.ACCENT_TEXT).paintIcon(this, g, 11, 11);
                g.dispose();
            }
        };
        JPanel words = Ui.column(Ui.label(title, Theme.semibold(15), Theme.INK), Box.createVerticalStrut(2),
                Ui.label(detail, Theme.regular(14), Theme.INK3));
        JPanel wordsRow = Ui.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        wordsRow.add(words, c);
        JPanel row = Ui.transparent(new BorderLayout(Theme.S4, 0));
        row.add(tile, BorderLayout.WEST);
        row.add(wordsRow, BorderLayout.CENTER);
        return row;
    }

    private JComponent createForm() {
        txtUsername = Ui.field(new JTextField(), "e.g. admin");
        txtPassword = Ui.field(new JPasswordField(), null);
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        txtPassword.addActionListener(e -> performLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        lblError = Ui.label(" ", Theme.medium(13), Theme.CRITICAL_TEXT);
        lblError.setIconTextGap(Theme.S2);
        btnLogin = Ui.primary("Sign in", null);
        btnLogin.setFont(Theme.semibold(15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setPreferredSize(new Dimension(FORM_WIDTH, 44));
        btnLogin.addActionListener(e -> performLogin());

        signInForm = stack(
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
                switchRow("New to GreenPulse?", "Create an account", this::showSignUp));
        signUpForm = createSignUpForm();

        // The sheet holds one form at a time, so each keeps its own natural height.
        sheet = new Ui.Plate(new GridBagLayout(), Theme.S6);
        sheet.setBorder(new EmptyBorder(40, 44, 40 + Ui.SHADOW, 44));
        showForm(signInForm, btnLogin);
        JPanel holder = Ui.transparent(new GridBagLayout());
        holder.add(sheet);
        return holder;
    }

    /** The second form in the sheet: anyone can create a Staff or Driver account, then sign in with it. */
    private JComponent createSignUpForm() {
        txtFullName = Ui.field(new JTextField(), "e.g. Maria Santos");
        txtNewUsername = Ui.field(new JTextField(), "Letters, numbers, . _ -");
        txtNewPassword = Ui.field(new JPasswordField(), "At least " + AccountRules.MIN_PASSWORD_LENGTH + " characters");
        txtNewPassword.putClientProperty("JPasswordField.showRevealButton", true);
        comboRole = Ui.field(new JComboBox<>(new String[]{"Staff", "Driver"}), null);
        comboRole.setPreferredSize(new Dimension(116, 40));
        txtFullName.addActionListener(e -> txtNewUsername.requestFocusInWindow());
        txtNewUsername.addActionListener(e -> txtNewPassword.requestFocusInWindow());
        txtNewPassword.addActionListener(e -> performSignUp());

        lblSignUpError = Ui.label(" ", Theme.medium(13), Theme.CRITICAL_TEXT);
        lblSignUpError.setIconTextGap(Theme.S2);
        btnCreate = Ui.primary("Create account", null);
        btnCreate.setFont(Theme.semibold(15));
        btnCreate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnCreate.setPreferredSize(new Dimension(FORM_WIDTH, 44));
        btnCreate.addActionListener(e -> performSignUp());

        // Password and role share a row, so the sign-up sheet is no taller than the sign-in one.
        JPanel passwordRow = Ui.transparent(new BorderLayout(Theme.S3, 0));
        passwordRow.add(Ui.labelled("Password", txtNewPassword), BorderLayout.CENTER);
        passwordRow.add(Ui.labelled("Role", comboRole), BorderLayout.EAST);

        // The way back sits under the title, not at the foot, so the taller form still fits the 600px minimum window.
        return stack(
                Ui.title("Create an account"),
                Box.createVerticalStrut(Theme.S1),
                switchRow("Already have an account?", "Sign in", () -> showSignIn(null)),
                Box.createVerticalStrut(Theme.S4),
                Ui.labelled("Full name", txtFullName),
                Box.createVerticalStrut(Theme.S3),
                Ui.labelled("Username", txtNewUsername),
                Box.createVerticalStrut(Theme.S3),
                passwordRow,
                Box.createVerticalStrut(Theme.S3),
                lblSignUpError,
                Box.createVerticalStrut(Theme.S3),
                btnCreate);
    }

    /** Fixed width, natural height, so a two-line error pushes the button down instead of being clipped. */
    private static JComponent stack(Component... parts) {
        JPanel form = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(FORM_WIDTH, super.getPreferredSize().height); }
        };
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        for (Component part : parts) {
            if (part instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(part);
        }
        return form;
    }

    /** A caption with a text-style button after it, used to move between the two forms. */
    private static JComponent switchRow(String prompt, String action, Runnable onClick) {
        JButton link = Ui.quiet(action, null);
        link.setFont(Theme.semibold(13));
        link.setMargin(new Insets(2, 4, 2, 4));
        link.addActionListener(e -> onClick.run());
        JPanel row = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.add(Ui.caption(prompt));
        row.add(Box.createHorizontalStrut(Theme.S1));
        row.add(link);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void showForm(JComponent form, JButton defaultButton) {
        sheet.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        sheet.add(form, c);
        sheet.revalidate();
        sheet.repaint();
        getRootPane().setDefaultButton(defaultButton);
    }

    private void showSignUp() {
        clearMessage(lblSignUpError);
        showForm(signUpForm, btnCreate);
        txtFullName.requestFocusInWindow();
    }

    /** Back to sign-in; after a new account, its username is filled in and the next step is the password. */
    private void showSignIn(String newUsername) {
        showForm(signInForm, btnLogin);
        if (newUsername == null) {
            clearMessage(lblError);
            txtUsername.requestFocusInWindow();
            return;
        }
        txtUsername.setText(newUsername);
        txtPassword.setText("");
        showMessage(lblError, Glyph.CHECK, Theme.OK_TEXT, "Account created. Enter your password to sign in.");
        txtPassword.requestFocusInWindow();
    }

    private JButton createDemoButton(String role, String username, String password) {
        JButton btn = Ui.secondary(role, null);
        btn.setToolTipText("Sign in as the " + role.toLowerCase() + " demo account");
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
                User loggedUser;
                try {
                    loggedUser = get();
                } catch (Exception ex) { // only the database call is in here, so its message is the database's
                    showError(DatabaseConnection.explain(ex));
                    System.err.println("[Login] " + ex.getMessage());
                    return;
                }
                if (loggedUser == null) {
                    showError("Those details did not match. Check them and try again.");
                    txtPassword.selectAll();
                    txtPassword.requestFocusInWindow();
                    return;
                }
                new MainDashboardFrame(loggedUser).setVisible(true);
                dispose();
            }
        }.execute();
    }

    private void performSignUp() {
        if (creating) return;
        String fullName = txtFullName.getText().trim();
        String username = txtNewUsername.getText().trim();
        String password = new String(txtNewPassword.getPassword());
        Role role = Role.fromString((String) comboRole.getSelectedItem());
        String problem = AccountRules.problemWith(fullName, username, password, role);
        if (problem != null) { showMessage(lblSignUpError, Glyph.WARNING, Theme.CRITICAL_TEXT, problem); return; }
        setCreating(true);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() throws Exception {
                return new UserDAO().register(fullName, username, password, role);
            }
            @Override protected void done() {
                setCreating(false);
                User created;
                try {
                    created = get();
                } catch (ExecutionException ex) {
                    // A taken username comes back as IllegalArgumentException with words meant for the form.
                    String message = ex.getCause() instanceof IllegalArgumentException taken
                            ? taken.getMessage() : DatabaseConnection.explain(ex);
                    showMessage(lblSignUpError, Glyph.WARNING, Theme.CRITICAL_TEXT, message);
                    System.err.println("[Sign up] " + ex.getMessage());
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                txtFullName.setText("");
                txtNewUsername.setText("");
                txtNewPassword.setText("");
                comboRole.setSelectedIndex(0);
                showSignIn(created.getUsername());
            }
        }.execute();
    }

    private void setSigningIn(boolean busy) {
        signingIn = busy;
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Signing in…" : "Sign in");
        if (busy) clearMessage(lblError);
    }

    private void setCreating(boolean busy) {
        creating = busy;
        btnCreate.setEnabled(!busy);
        btnCreate.setText(busy ? "Creating account…" : "Create account");
        if (busy) clearMessage(lblSignUpError);
    }

    private void showError(String message) {
        showMessage(lblError, Glyph.WARNING, Theme.CRITICAL_TEXT, message);
    }

    /** One message line under a form: a warning in the critical colour, or a confirmation in the OK colour. */
    private static void showMessage(JLabel line, Glyph glyph, String colorKey, String message) {
        Theme.fg(line, colorKey);
        line.setIcon(Icons.of(glyph, 16, colorKey));
        line.setText(Ui.wrap(message, FORM_WIDTH - 24)); // the icon and its gap take 24px
    }

    private static void clearMessage(JLabel line) {
        line.setIcon(null);
        line.setText(" ");
    }

    public static void main(String[] args) {
        com.greenpulse.App.main(args);
    }
}
