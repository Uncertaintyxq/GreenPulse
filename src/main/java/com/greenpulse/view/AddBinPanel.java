package com.greenpulse.view;

import com.greenpulse.model.SmartBin;
import com.greenpulse.model.SmartBinFactory;
import com.greenpulse.model.User;
import com.greenpulse.service.BinService;

import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Guided bin registration form. */
public class AddBinPanel extends JPanel {
    private JTextField txtBinID, txtCapacity, txtLocation, txtLatitude, txtLongitude, txtParam1, txtParam2;
    private JComboBox<String> comboType;
    private JLabel lblParam1, lblParam2, lblParam2Hint, lblCoordinateHelp;
    private Runnable onBackAction;
    private final BinService binService;
    private JButton btnRegister;

    public AddBinPanel(User user, Runnable onBackAction) {
        this.onBackAction = onBackAction;
        this.binService = new BinService(user);
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        JPanel page = Ui.transparent(new BorderLayout(0, Theme.S5));
        page.setBorder(new EmptyBorder(Theme.S6, 40, Theme.S4, 40));

        JButton back = Ui.secondary("Back to monitor", Glyph.ARROW_LEFT);
        back.addActionListener(e -> { if (onBackAction != null) onBackAction.run(); });
        page.add(Ui.pageHeader("Register a smart bin", "Add a bin in a few steps. You can set its map location now or later.", back), BorderLayout.NORTH);

        comboType = Ui.field(new JComboBox<>(new String[]{"Recycling bin", "Compost bin"}), null);
        comboType.setToolTipText("Recycling bins track contamination; compost bins track moisture and temperature.");
        txtBinID = Ui.field(new JTextField(), null); txtBinID.setEditable(false); txtBinID.setFont(Theme.mono(14));
        txtBinID.setToolTipText("Generated automatically from the bin type.");
        txtCapacity = Ui.field(new JTextField("120"), null);
        txtLocation = Ui.field(new JTextField(), "e.g. Near Barangay Hall, Poblacion");
        JPanel details = section("Bin details", "Choose the bin type and enter the basic information.",
                grid(Ui.labelled("Bin type", comboType), Ui.labelled("Generated bin ID", txtBinID),
                     Ui.labelled("Capacity (litres)", txtCapacity), Ui.labelled("Location or landmark", txtLocation)), null);

        lblParam1 = Ui.fieldLabel("Contamination level (%)"); txtParam1 = Ui.field(new JTextField("12"), null);
        lblParam2 = Ui.fieldLabel("Temperature (°C)"); txtParam2 = Ui.field(new JTextField("25"), null);
        lblParam2Hint = Ui.caption("Compost bins only.");
        JPanel sensors = section("Sensor settings", "Use the current sensor values, or adjust them for your bin.",
                grid(labelled(lblParam1, txtParam1), Ui.column(labelled(lblParam2, txtParam2), Box.createVerticalStrut(Theme.S1), lblParam2Hint)),
                Ui.caption("These are starting readings. You can update readings later from Sensor simulation."));

        txtLatitude = Ui.field(new JTextField(), "4.5 to 21.5"); txtLongitude = Ui.field(new JTextField(), "116.0 to 127.5");
        txtLatitude.setToolTipText("Latitude between approximately 4.5 and 21.5");
        txtLongitude.setToolTipText("Longitude between approximately 116.0 and 127.5");
        lblCoordinateHelp = Ui.caption("You can leave both blank and place the bin by clicking its location on the Dispatch map.");
        JPanel location = section("Map location (Philippines)", "Coordinates help the dispatch planner calculate a road route.",
                grid(Ui.labelled("Latitude", txtLatitude), Ui.labelled("Longitude", txtLongitude)), lblCoordinateHelp);

        JButton clear = Ui.secondary("Clear form", null);
        clear.addActionListener(e -> resetForm());
        btnRegister = Ui.primary("Register bin", Glyph.CHECK);
        btnRegister.addActionListener(e -> registerBin());
        JPanel actions = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(clear); actions.add(Box.createHorizontalStrut(Theme.S3)); actions.add(btnRegister);
        JPanel footer = Ui.transparent(new BorderLayout(Theme.S3, 0));
        footer.add(Ui.caption("Required: bin type, capacity and location or landmark. GPS is optional."), BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);

        JPanel content = Ui.column(details, Box.createVerticalStrut(Theme.S4), sensors, Box.createVerticalStrut(Theme.S4), location,
                Box.createVerticalStrut(Theme.S4));
        JPanel body = new Ui.ScrollBody(new BorderLayout());
        body.add(new SimulatePanel.MaxWidth(content, 900), BorderLayout.NORTH);
        page.add(Ui.scroll(body), BorderLayout.CENTER);
        // The register action stays in view however long the form is.
        footer.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(Theme.LINE, 1, 0, 0, 0), new EmptyBorder(Theme.S4, 0, 0, 0)));
        page.add(new SimulatePanel.MaxWidth(footer, 900), BorderLayout.SOUTH);
        add(page, BorderLayout.CENTER);

        comboType.addActionListener(e -> updateTypeFields(true));
        updateTypeFields(false);
    }

    private JPanel section(String title, String description, JComponent fields, JComponent note) {
        JPanel plate = new Ui.Plate(new BorderLayout(0, Theme.S4), Theme.S5);
        plate.add(Ui.plateHeader(title, description), BorderLayout.NORTH);
        plate.add(fields, BorderLayout.CENTER);
        if (note != null) plate.add(note, BorderLayout.SOUTH);
        plate.setAlignmentX(Component.LEFT_ALIGNMENT);
        return plate;
    }

    /** Two equal columns of labelled fields. */
    private JPanel grid(JComponent... groups) {
        JPanel grid = Ui.transparent(new GridLayout(0, 2, Theme.S5, Theme.S4));
        for (JComponent g : groups) grid.add(g);
        return grid;
    }

    /** Like {@link Ui#labelled}, but keeps the given label so its text can change with the bin type. */
    private JPanel labelled(JLabel label, JComponent field) {
        JPanel group = Ui.column(label, Box.createVerticalStrut(6), field);
        return group;
    }

    private String newBinId() {
        return "BIN-" + ("Recycling bin".equals(comboType.getSelectedItem()) ? "RB-" : "CB-") + (int)(Math.random() * 9000 + 1000);
    }

    private void updateTypeFields(boolean regenerateId) {
        boolean recycling = "Recycling bin".equals(comboType.getSelectedItem());
        if (regenerateId || txtBinID.getText().isEmpty()) txtBinID.setText(newBinId());
        lblParam1.setText(recycling ? "Contamination level (%)" : "Moisture level (%)");
        lblParam2Hint.setText(recycling ? "Compost bins only. Not used for recycling bins." : "Starting compost temperature.");
        txtParam2.setEnabled(!recycling);
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
                    (Double.isNaN(latitude) ? "\n\nNext step: open Dispatch, choose this bin under ‘Place a bin on the map’, then click its location." : "\n\nIts coordinates have been saved."),
                    "Registration complete", JOptionPane.INFORMATION_MESSAGE);
            if (onBackAction != null) onBackAction.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Check the number fields. Capacity, sensor values and coordinates must be numeric.", "Check your entries", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Could not register the bin." : ex.getMessage(), "Could not register bin", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnRegister.setEnabled(true); btnRegister.setText("Register bin");
        }
    }
}
