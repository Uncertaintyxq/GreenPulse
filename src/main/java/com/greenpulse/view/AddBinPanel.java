package com.greenpulse.view;

import com.greenpulse.model.SmartBin;
import com.greenpulse.model.SmartBinFactory;
import com.greenpulse.model.User;
import com.greenpulse.service.BinService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Friendly, guided bin registration form. */
public class AddBinPanel extends JPanel {
    private JTextField txtBinID, txtCapacity, txtLocation, txtLatitude, txtLongitude, txtParam1, txtParam2;
    private JComboBox<String> comboType;
    private JLabel lblParam1, lblParam2, lblCoordinateHelp;
    private Runnable onBackAction;
    private final BinService binService;
    private JButton btnRegister;

    private static final Color BG = new Color(245, 248, 250);
    private static final Color INK = new Color(24, 39, 55);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color BORDER = new Color(225, 232, 238);

    public AddBinPanel(User user, Runnable onBackAction) {
        this.onBackAction = onBackAction;
        this.binService = new BinService(user);
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        initUI();
    }

    private void initUI() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout(12, 6));
        top.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("BIN MANAGEMENT  /  NEW BIN");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrow.setForeground(GREEN);
        JLabel title = new JLabel("Register a smart bin");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Add a bin in a few steps. You can set its map location now or later.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        heading.add(eyebrow); heading.add(Box.createVerticalStrut(5)); heading.add(title);
        heading.add(Box.createVerticalStrut(5)); heading.add(subtitle);
        JButton back = secondaryButton("←  Back to monitor");
        back.addActionListener(e -> { if (onBackAction != null) onBackAction.run(); });
        top.add(heading, BorderLayout.CENTER); top.add(back, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel detailsCard = cardPanel();
        detailsCard.setLayout(new BorderLayout(0, 14));
        detailsCard.add(sectionHeading("1", "Bin details", "Choose the bin type and enter the basic information."), BorderLayout.NORTH);
        JPanel detailsGrid = new JPanel(new GridBagLayout());
        detailsGrid.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 8, 7, 14); g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.anchor = GridBagConstraints.NORTHWEST;

        comboType = new JComboBox<>(new String[]{"Recycling bin", "Compost bin"});
        comboType.setToolTipText("Recycling bins track contamination; compost bins track moisture and temperature.");
        txtBinID = new JTextField(); txtBinID.setEditable(false); txtBinID.setBackground(new Color(246, 248, 250));
        txtCapacity = new JTextField("120");
        txtLocation = new JTextField(); txtLocation.setToolTipText("Example: Near Barangay Hall, Poblacion");
        addField(detailsGrid, g, 0, 0, "Bin type", comboType);
        addField(detailsGrid, g, 0, 1, "Generated bin ID", txtBinID);
        addField(detailsGrid, g, 1, 0, "Capacity (litres)", txtCapacity);
        addField(detailsGrid, g, 1, 1, "Location or landmark", txtLocation);
        detailsCard.add(detailsGrid, BorderLayout.CENTER);

        JPanel sensorCard = cardPanel();
        sensorCard.setLayout(new BorderLayout(0, 14));
        sensorCard.add(sectionHeading("2", "Sensor settings", "Use the current sensor values, or adjust them for your bin."), BorderLayout.NORTH);
        JPanel sensorGrid = new JPanel(new GridBagLayout()); sensorGrid.setOpaque(false);
        GridBagConstraints sg = new GridBagConstraints(); sg.insets = new Insets(7, 8, 7, 14); sg.fill = GridBagConstraints.HORIZONTAL; sg.weightx = 1; sg.anchor = GridBagConstraints.NORTHWEST;
        lblParam1 = new JLabel("Contamination level (%)"); txtParam1 = new JTextField("12");
        lblParam2 = new JLabel("Not used for recycling"); txtParam2 = new JTextField("25");
        addField(sensorGrid, sg, 0, 0, lblParam1, txtParam1);
        addField(sensorGrid, sg, 0, 1, lblParam2, txtParam2);
        JLabel sensorHint = new JLabel("Tip: these are starting readings. You can update readings from Sensor Simulation.");
        sensorHint.setFont(new Font("SansSerif", Font.PLAIN, 11)); sensorHint.setForeground(MUTED);
        JPanel sensorBody = new JPanel(new BorderLayout(0, 8)); sensorBody.setOpaque(false); sensorBody.add(sensorGrid, BorderLayout.CENTER); sensorBody.add(sensorHint, BorderLayout.SOUTH);
        sensorCard.add(sensorBody, BorderLayout.CENTER);

        JPanel locationCard = cardPanel();
        locationCard.setLayout(new BorderLayout(0, 14));
        locationCard.add(sectionHeading("3", "Map location (Philippines)", "Coordinates help the dispatch planner calculate a road route."), BorderLayout.NORTH);
        JPanel locationGrid = new JPanel(new GridBagLayout()); locationGrid.setOpaque(false);
        GridBagConstraints lg = new GridBagConstraints(); lg.insets = new Insets(7, 8, 7, 14); lg.fill = GridBagConstraints.HORIZONTAL; lg.weightx = 1; lg.anchor = GridBagConstraints.NORTHWEST;
        txtLatitude = new JTextField(); txtLongitude = new JTextField();
        txtLatitude.setToolTipText("Latitude between approximately 4.5 and 21.5");
        txtLongitude.setToolTipText("Longitude between approximately 116.0 and 127.5");
        addField(locationGrid, lg, 0, 0, "Latitude", txtLatitude);
        addField(locationGrid, lg, 0, 1, "Longitude", txtLongitude);
        lblCoordinateHelp = new JLabel("You can leave both blank and place the bin by clicking its location on the Dispatch map.");
        lblCoordinateHelp.setFont(new Font("SansSerif", Font.PLAIN, 11)); lblCoordinateHelp.setForeground(MUTED);
        JPanel locationBody = new JPanel(new BorderLayout(0, 8)); locationBody.setOpaque(false);
        locationBody.add(locationGrid, BorderLayout.CENTER); locationBody.add(lblCoordinateHelp, BorderLayout.SOUTH);
        locationCard.add(locationBody, BorderLayout.CENTER);

        content.add(detailsCard); content.add(Box.createVerticalStrut(14));
        content.add(sensorCard); content.add(Box.createVerticalStrut(14)); content.add(locationCard);

        JPanel footer = new JPanel(new BorderLayout(12, 8)); footer.setOpaque(false); footer.setBorder(new EmptyBorder(6, 0, 0, 0));
        JLabel required = new JLabel("Required: bin type, capacity and location/landmark. GPS is optional.");
        required.setFont(new Font("SansSerif", Font.PLAIN, 11)); required.setForeground(MUTED);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); actions.setOpaque(false);
        JButton clear = secondaryButton("Clear form");
        clear.addActionListener(e -> resetForm());
        btnRegister = new JButton("✓  Register bin");
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 13)); btnRegister.setForeground(Color.WHITE); btnRegister.setBackground(GREEN);
        btnRegister.setFocusPainted(false); btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegister.setPreferredSize(new Dimension(165, 42)); btnRegister.addActionListener(e -> registerBin());
        actions.add(clear); actions.add(btnRegister);
        footer.add(required, BorderLayout.CENTER); footer.add(actions, BorderLayout.EAST);

        JPanel scrollContent = new JPanel(new BorderLayout(0, 16)); scrollContent.setOpaque(false);
        scrollContent.add(content, BorderLayout.CENTER); scrollContent.add(footer, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

        comboType.addActionListener(e -> updateTypeFields(true));
        updateTypeFields(false);
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel(); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 20, 18, 20)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT); return p;
    }

    private JPanel sectionHeading(String number, String title, String description) {
        JPanel wrap = new JPanel(new BorderLayout(12, 0)); wrap.setOpaque(false);
        JLabel badge = new JLabel(number, SwingConstants.CENTER); badge.setOpaque(true); badge.setBackground(new Color(220, 252, 231)); badge.setForeground(new Color(6, 95, 70));
        badge.setFont(new Font("SansSerif", Font.BOLD, 13)); badge.setPreferredSize(new Dimension(34, 34));
        JPanel words = new JPanel(); words.setOpaque(false); words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(new Font("SansSerif", Font.BOLD, 15)); t.setForeground(INK);
        JLabel d = new JLabel(description); d.setFont(new Font("SansSerif", Font.PLAIN, 11)); d.setForeground(MUTED);
        words.add(t); words.add(Box.createVerticalStrut(4)); words.add(d); wrap.add(badge, BorderLayout.WEST); wrap.add(words, BorderLayout.CENTER); return wrap;
    }

    private void addField(JPanel panel, GridBagConstraints base, int row, int col, String label, JComponent field) {
        JPanel group = new JPanel(); group.setOpaque(false); group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label); l.setFont(new Font("SansSerif", Font.BOLD, 11)); l.setForeground(new Color(51, 65, 85));
        l.setAlignmentX(Component.LEFT_ALIGNMENT); field.setFont(new Font("SansSerif", Font.PLAIN, 13)); field.setPreferredSize(new Dimension(220, 36)); field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT); group.add(l); group.add(Box.createVerticalStrut(6)); group.add(field);
        GridBagConstraints c = (GridBagConstraints) base.clone(); c.gridx = col; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; panel.add(group, c);
    }

    private void addField(JPanel panel, GridBagConstraints base, int row, int col, JLabel label, JComponent field) {
        addField(panel, base, row, col, label.getText(), field);
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text); b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setPreferredSize(new Dimension(150, 38)); return b;
    }

    private String newBinId() {
        return "BIN-" + ("Recycling bin".equals(comboType.getSelectedItem()) ? "RB-" : "CB-") + (int)(Math.random() * 9000 + 1000);
    }

    private void updateTypeFields(boolean regenerateId) {
        boolean recycling = "Recycling bin".equals(comboType.getSelectedItem());
        if (regenerateId || txtBinID.getText().isEmpty()) txtBinID.setText(newBinId());
        lblParam1.setText(recycling ? "Contamination level (%)" : "Moisture level (%)");
        lblParam2.setText(recycling ? "Not used for recycling" : "Temperature (°C)");
        txtParam2.setEnabled(!recycling);
        txtParam2.setBackground(recycling ? new Color(246, 248, 250) : Color.WHITE);
        txtParam1.setToolTipText(recycling ? "Typical range: 0–100 percent" : "Typical range: 0–100 percent");
    }

    private boolean isPhilippinesCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && latitude >= 4.5 && latitude <= 21.5
                && Double.isFinite(longitude) && longitude >= 116.0 && longitude <= 127.5;
    }

    private void resetForm() {
        txtCapacity.setText("120"); txtLocation.setText(""); txtLatitude.setText(""); txtLongitude.setText("");
        txtParam1.setText("Recycling bin".equals(comboType.getSelectedItem()) ? "12" : "45"); txtParam2.setText("25");
        txtLocation.requestFocusInWindow();
    }

    private void registerBin() {
        try {
            String type = "Recycling bin".equals(comboType.getSelectedItem()) ? "RecyclingBin" : "CompostBin";
            String id = txtBinID.getText().trim();
            double cap = Double.parseDouble(txtCapacity.getText().trim());
            String loc = txtLocation.getText().trim();
            if (loc.isEmpty()) throw new IllegalArgumentException("Enter a recognizable location or landmark.");
            if (!Double.isFinite(cap) || cap <= 0 || cap > 100000) throw new IllegalArgumentException("Capacity must be a number greater than 0 litres.");
            double latitude = Double.NaN, longitude = Double.NaN;
            boolean hasLat = !txtLatitude.getText().trim().isEmpty(), hasLon = !txtLongitude.getText().trim().isEmpty();
            if (hasLat != hasLon) throw new IllegalArgumentException("Enter both latitude and longitude, or leave both blank and place the bin on the Dispatch map.");
            if (hasLat) {
                latitude = Double.parseDouble(txtLatitude.getText().trim()); longitude = Double.parseDouble(txtLongitude.getText().trim());
                if (!isPhilippinesCoordinate(latitude, longitude)) throw new IllegalArgumentException("Coordinates must be within the approximate Philippines region (latitude 4.5–21.5, longitude 116.0–127.5). You can use the Dispatch map instead.");
            }
            double p1 = Double.parseDouble(txtParam1.getText().trim());
            double p2 = txtParam2.isEnabled() ? Double.parseDouble(txtParam2.getText().trim()) : 0.0;
            if (!Double.isFinite(p1) || p1 < 0 || p1 > 100) throw new IllegalArgumentException((type.equals("RecyclingBin") ? "Contamination" : "Moisture") + " must be between 0 and 100%.");
            if (txtParam2.isEnabled() && (!Double.isFinite(p2) || p2 < -50 || p2 > 100)) throw new IllegalArgumentException("Temperature must be between -50°C and 100°C.");

            btnRegister.setEnabled(false); btnRegister.setText("Registering…");
            SmartBin newBin = SmartBinFactory.createBin(type, id, cap, 0.0, loc, p1, p2, latitude, longitude);
            binService.registerBin(newBin);
            JOptionPane.showMessageDialog(this,
                    "Bin registered successfully!\n\n" + id + " — " + loc +
                    (Double.isNaN(latitude) ? "\n\nNext step: open GIS Driver Dispatch, select this bin, then click ‘Place Selected Bin on Map’." : "\n\nIts coordinates have been saved."),
                    "Registration complete", JOptionPane.INFORMATION_MESSAGE);
            if (onBackAction != null) onBackAction.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Check the number fields. Capacity, sensor values and coordinates must be numeric.", "Check your entries", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Could not register the bin." : ex.getMessage(), "Could not register bin", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnRegister.setEnabled(true); btnRegister.setText("✓  Register bin");
        }
    }
}
