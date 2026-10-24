package com.greenpulse.view;

import com.greenpulse.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class UserProfilePanel extends JPanel {
    private final User user;

    public UserProfilePanel(User user) {
        this.user = user;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(Theme.S6, 40, Theme.S6, 40));

        JPanel page = Ui.transparent(new BorderLayout(0, Theme.S5));
        page.add(Ui.pageHeader("Profile", "Your account and display preferences.", null), BorderLayout.NORTH);

        // Account
        JPanel identity = Ui.transparent(new BorderLayout(Theme.S4, 0));
        identity.add(new Avatar(initials(user.getFullName())), BorderLayout.WEST);
        identity.add(Ui.column(Ui.label(orDash(user.getFullName()), Theme.heavy(22), Theme.INK), Box.createVerticalStrut(2),
                Ui.label(roleLabel(), Theme.semibold(14), Theme.OK_TEXT)), BorderLayout.CENTER);

        JPanel details = Ui.column(
                detailRow("Username", user.getUsername(), true),
                detailRow("Full name", user.getFullName(), false),
                detailRow("Access role", roleLabel(), false));

        JPanel userCard = new Ui.Plate(new BorderLayout(0, Theme.S5), Theme.S6);
        userCard.add(identity, BorderLayout.NORTH);
        userCard.add(details, BorderLayout.CENTER);

        // Appearance
        Ui.Segmented themeChoice = new Ui.Segmented("Light", "Dark");
        themeChoice.select(Theme.isDark() ? 1 : 0);
        themeChoice.onSelect(i -> SwingUtilities.invokeLater(() -> Theme.setDark(i == 1)));
        JPanel choiceRow = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        choiceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        choiceRow.add(themeChoice);

        JPanel themeCard = new Ui.Plate(new BorderLayout(), Theme.S6);
        themeCard.add(Ui.column(
                Ui.sectionTitle("Theme"),
                Box.createVerticalStrut(Theme.S1),
                Ui.body(Ui.wrap("Light suits bright offices and projectors. Dark is easier on the eyes at night.", 330)),
                Box.createVerticalStrut(Theme.S5),
                choiceRow,
                Box.createVerticalStrut(Theme.S4),
                Ui.caption(Ui.wrap("Applies to every screen straight away and is remembered next time you sign in.", 330))), BorderLayout.NORTH);

        JPanel cards = Ui.transparent(new GridLayout(1, 2, Theme.S4, 0));
        cards.add(userCard);
        cards.add(themeCard);
        JPanel holder = Ui.transparent(new BorderLayout());
        holder.add(new SimulatePanel.MaxWidth(cards, 1000), BorderLayout.NORTH);

        page.add(holder, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    private String roleLabel() {
        return switch (user.getRoleType()) {
            case ADMIN -> "Administrator";
            case STAFF -> "Staff";
            case DRIVER -> "Driver";
        };
    }

    private static String orDash(String value) { return value == null || value.isBlank() ? "—" : value; }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        return Arrays.stream(name.trim().split("\\s+")).limit(2)
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT)).collect(Collectors.joining());
    }

    private JPanel detailRow(String label, String value, boolean code) {
        JPanel row = Ui.column(Ui.fieldLabel(label), Box.createVerticalStrut(2),
                Ui.label(orDash(value), code ? Theme.mono(15) : Theme.regular(15), Theme.INK));
        row.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(Theme.LINE2, 1, 0, 0, 0), new EmptyBorder(Theme.S3, 0, Theme.S3, 0)));
        return row;
    }

    /** The user's initials on a sign-green disc. */
    private static final class Avatar extends JComponent {
        private final String text;
        Avatar(String text) { this.text = text; setPreferredSize(new Dimension(56, 56)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g.setColor(Theme.color(Theme.SIGN));
            g.fillOval(0, (getHeight() - d) / 2, d, d);
            g.setColor(Theme.color(Theme.SIGN_INK));
            g.setFont(Theme.heavy(20));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (d - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
