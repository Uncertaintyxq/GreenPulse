package com.greenpulse.view;

import com.greenpulse.model.CompostBin;
import com.greenpulse.model.RecyclingBin;
import com.greenpulse.model.SmartBin;
import com.greenpulse.model.User;
import com.greenpulse.service.BinService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SimulatePanel extends JPanel {
    private JComboBox<String> comboBins;
    private JSlider fillSlider, secondarySlider;
    private JLabel lblSecondary, lblFillValue, lblSensorHint, lblSelectedBin;
    private List<SmartBin> loadedBins;
    private final Runnable onBackAction;
    private final BinService binService;
    private static final Color BG = new Color(245, 248, 250), INK = new Color(24, 39, 55), MUTED = new Color(100, 116, 139), GREEN = new Color(5, 150, 105), BORDER = new Color(225, 232, 238);

    public SimulatePanel(User user, List<SmartBin> bins, Runnable onBackAction) {
        this.loadedBins = bins; this.onBackAction = onBackAction; this.binService = new BinService(user);
        setLayout(new BorderLayout()); setBackground(BG); initUI();
    }

    public void updateBinList(List<SmartBin> bins) {
        this.loadedBins = bins; if (comboBins == null) return;
        String previous = (String) comboBins.getSelectedItem(); comboBins.removeAllItems();
        for (SmartBin b : loadedBins) comboBins.addItem(b.getBinID() + " — " + b.getLocation());
        if (previous != null) comboBins.setSelectedItem(previous);
        onBinSelectionChange();
    }

    private void initUI() {
        JPanel page = new JPanel(new BorderLayout(0, 22)); page.setOpaque(false); page.setBorder(new EmptyBorder(26, 30, 26, 30));
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("TESTING TOOLS"); eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10)); eyebrow.setForeground(GREEN);
        JLabel title = new JLabel("Sensor simulation"); title.setFont(new Font("SansSerif", Font.BOLD, 25)); title.setForeground(INK);
        JLabel subtitle = new JLabel("Send a test reading to a bin and check how GreenPulse responds."); subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13)); subtitle.setForeground(MUTED);
        heading.add(eyebrow); heading.add(Box.createVerticalStrut(5)); heading.add(title); heading.add(Box.createVerticalStrut(5)); heading.add(subtitle); page.add(heading, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(0, 18)); card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(22, 24, 22, 24)));
        JPanel selected = new JPanel(); selected.setOpaque(false); selected.setLayout(new BoxLayout(selected, BoxLayout.Y_AXIS));
        selected.add(label("1  CHOOSE A SMART BIN", true)); selected.add(Box.createVerticalStrut(8));
        comboBins = new JComboBox<>(); comboBins.setFont(new Font("SansSerif", Font.PLAIN, 13)); comboBins.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        selected.add(comboBins); selected.add(Box.createVerticalStrut(8));
        lblSelectedBin = new JLabel("Choose a bin to see its current readings."); lblSelectedBin.setFont(new Font("SansSerif", Font.PLAIN, 11)); lblSelectedBin.setForeground(MUTED); selected.add(lblSelectedBin);

        JPanel reading = new JPanel(); reading.setOpaque(false); reading.setLayout(new BoxLayout(reading, BoxLayout.Y_AXIS));
        JPanel fillHead = new JPanel(new BorderLayout()); fillHead.setOpaque(false); fillHead.add(label("2  FILL LEVEL", true), BorderLayout.WEST);
        lblFillValue = new JLabel("50%"); lblFillValue.setFont(new Font("SansSerif", Font.BOLD, 17)); lblFillValue.setForeground(GREEN); fillHead.add(lblFillValue, BorderLayout.EAST);
        fillSlider = new JSlider(0, 100, 50); fillSlider.setMajorTickSpacing(25); fillSlider.setPaintTicks(true); fillSlider.setPaintLabels(true); fillSlider.setOpaque(false);
        fillSlider.addChangeListener(e -> lblFillValue.setText(fillSlider.getValue() + "%"));
        reading.add(fillHead); reading.add(Box.createVerticalStrut(8)); reading.add(fillSlider); reading.add(Box.createVerticalStrut(20));
        lblSecondary = label("3  SECONDARY SENSOR", true); secondarySlider = new JSlider(0, 100, 20); secondarySlider.setMajorTickSpacing(25); secondarySlider.setPaintTicks(true); secondarySlider.setPaintLabels(true); secondarySlider.setOpaque(false);
        lblSensorHint = new JLabel("Adjust the secondary reading for the selected bin type."); lblSensorHint.setFont(new Font("SansSerif", Font.PLAIN, 11)); lblSensorHint.setForeground(MUTED);
        reading.add(lblSecondary); reading.add(Box.createVerticalStrut(8)); reading.add(secondarySlider); reading.add(Box.createVerticalStrut(6)); reading.add(lblSensorHint);

        JPanel note = new JPanel(new BorderLayout(10, 0)); note.setBackground(new Color(236, 253, 245)); note.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel noteIcon = new JLabel("ℹ"); noteIcon.setForeground(GREEN); noteIcon.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel noteText = new JLabel("Simulation writes a new reading to the database. Use it for testing, not as a real sensor measurement."); noteText.setForeground(new Color(6, 95, 70)); noteText.setFont(new Font("SansSerif", Font.PLAIN, 11)); note.add(noteIcon, BorderLayout.WEST); note.add(noteText, BorderLayout.CENTER);
        JButton send = new JButton("Send test reading  →"); send.setFont(new Font("SansSerif", Font.BOLD, 13)); send.setForeground(Color.WHITE); send.setBackground(GREEN); send.setFocusPainted(false); send.setPreferredSize(new Dimension(205, 42)); send.addActionListener(e -> pushTelemetry());
        JPanel bottom = new JPanel(new BorderLayout(0, 14)); bottom.setOpaque(false); bottom.add(note, BorderLayout.NORTH); JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); actions.setOpaque(false); actions.add(send); bottom.add(actions, BorderLayout.SOUTH);
        JPanel body = new JPanel(new BorderLayout(0, 20)); body.setOpaque(false); body.add(selected, BorderLayout.NORTH); body.add(reading, BorderLayout.CENTER); card.add(body, BorderLayout.CENTER); card.add(bottom, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER); add(page, BorderLayout.CENTER);
        comboBins.addActionListener(e -> onBinSelectionChange()); updateBinList(loadedBins);
    }

    private JLabel label(String text, boolean bold) { JLabel l = new JLabel(text); l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 11)); l.setForeground(bold ? INK : MUTED); return l; }

    private void onBinSelectionChange() {
        if (comboBins == null || fillSlider == null) return;
        int idx = comboBins.getSelectedIndex(); if (idx < 0 || idx >= loadedBins.size()) { lblSelectedBin.setText("No bins available. Register a bin first."); return; }
        SmartBin bin = loadedBins.get(idx); fillSlider.setValue((int) bin.getFillPercentage()); lblFillValue.setText(fillSlider.getValue() + "%");
        lblSelectedBin.setText("Current fill: " + String.format(java.util.Locale.US, "%.1f%%", bin.getFillPercentage()) + "  ·  " + bin.getClass().getSimpleName());
        if (bin instanceof RecyclingBin rBin) { lblSecondary.setText("3  CONTAMINATION LEVEL"); lblSensorHint.setText("Percentage of material that may be contaminated (0–100%)."); secondarySlider.setMinimum(0); secondarySlider.setMaximum(100); secondarySlider.setValue((int) rBin.getContaminationPercentage()); }
        else if (bin instanceof CompostBin cBin) { lblSecondary.setText("3  TEMPERATURE (°C)"); lblSensorHint.setText("Set a simulated compost temperature. The slider is limited to 0–100°C."); secondarySlider.setMinimum(0); secondarySlider.setMaximum(100); secondarySlider.setValue((int) cBin.getTempCelsius()); }
    }

    private void pushTelemetry() {
        int idx = comboBins.getSelectedIndex(); if (idx < 0 || idx >= loadedBins.size()) { JOptionPane.showMessageDialog(this, "Register or select a bin before sending a reading.", "No bin selected", JOptionPane.INFORMATION_MESSAGE); return; }
        SmartBin bin = loadedBins.get(idx);
        try {
            bin.setFillLevel(fillSlider.getValue());
            if (bin instanceof RecyclingBin rBin) rBin.setContaminationPercentage(secondarySlider.getValue());
            else if (bin instanceof CompostBin cBin) cBin.setTempCelsius(secondarySlider.getValue());
            if (!binService.recordManualReading(bin)) { JOptionPane.showMessageDialog(this, "This bin no longer exists. Refresh the monitor and try again.", "Bin not found", JOptionPane.WARNING_MESSAGE); return; }
            JOptionPane.showMessageDialog(this, "Test reading saved for " + bin.getBinID() + ".", "Reading saved", JOptionPane.INFORMATION_MESSAGE);
            if (onBackAction != null) onBackAction.run();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Could not save the reading: " + ex.getMessage(), "Simulation error", JOptionPane.ERROR_MESSAGE); }
    }
}
