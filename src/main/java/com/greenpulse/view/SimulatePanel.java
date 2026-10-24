package com.greenpulse.view;

import com.greenpulse.model.CompostBin;
import com.greenpulse.model.RecyclingBin;
import com.greenpulse.model.SmartBin;
import com.greenpulse.model.User;
import com.greenpulse.service.BinService;
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SimulatePanel extends JPanel {
    private JComboBox<String> comboBins;
    private JSlider fillSlider, secondarySlider;
    private JLabel lblSecondary, lblFillValue, lblSecondaryValue, lblSensorHint, lblSelectedBin;
    private String secondaryUnit = "%";
    private List<SmartBin> loadedBins;
    private final Runnable onBackAction;
    private final BinService binService;

    public SimulatePanel(User user, List<SmartBin> bins, Runnable onBackAction) {
        this.loadedBins = bins; this.onBackAction = onBackAction; this.binService = new BinService(user);
        setLayout(new BorderLayout()); setOpaque(false); initUI();
    }

    public void updateBinList(List<SmartBin> bins) {
        this.loadedBins = bins; if (comboBins == null) return;
        String previous = (String) comboBins.getSelectedItem(); comboBins.removeAllItems();
        for (SmartBin b : loadedBins) comboBins.addItem(b.getBinID() + " — " + b.getLocation());
        if (previous != null) comboBins.setSelectedItem(previous);
        onBinSelectionChange();
    }

    private void initUI() {
        JPanel page = Ui.transparent(new BorderLayout(0, Theme.S5)); page.setBorder(new EmptyBorder(Theme.S6, 40, Theme.S6, 40));
        page.add(Ui.pageHeader("Sensor simulation", "Send a test reading to a bin and see how GreenPulse responds.", null), BorderLayout.NORTH);

        comboBins = Ui.field(new JComboBox<>(), null);
        lblSelectedBin = Ui.caption("Choose a bin to see its current readings.");
        JPanel binGroup = Ui.column(Ui.labelled("Smart bin", comboBins), Box.createVerticalStrut(Theme.S2), lblSelectedBin);

        lblFillValue = Ui.label("50%", Theme.monoBold(18), Theme.INK);
        fillSlider = slider(50);
        fillSlider.addChangeListener(e -> lblFillValue.setText(fillSlider.getValue() + "%"));
        JPanel fillGroup = Ui.column(sliderHead(Ui.fieldLabel("Fill level"), lblFillValue), Box.createVerticalStrut(Theme.S2), fillSlider);

        lblSecondary = Ui.fieldLabel("Secondary sensor");
        lblSecondaryValue = Ui.label("20%", Theme.monoBold(18), Theme.INK);
        secondarySlider = slider(20);
        secondarySlider.addChangeListener(e -> lblSecondaryValue.setText(secondarySlider.getValue() + secondaryUnit));
        lblSensorHint = Ui.caption("Adjust the secondary reading for the selected bin type.");
        JPanel secondaryGroup = Ui.column(sliderHead(lblSecondary, lblSecondaryValue), Box.createVerticalStrut(Theme.S2), secondarySlider,
                Box.createVerticalStrut(Theme.S1), lblSensorHint);

        JLabel note = Ui.label(Ui.wrap("Simulation writes a new reading to the database. Use it for testing, not as a real sensor measurement.", 520),
                Theme.regular(14), Theme.INK2);
        note.setIcon(Icons.of(Glyph.INFO, 18, Theme.OK_TEXT));
        note.setIconTextGap(Theme.S3);
        note.setVerticalTextPosition(SwingConstants.TOP);

        JButton send = Ui.primary("Send test reading", Glyph.ARROW_RIGHT);
        send.setHorizontalTextPosition(SwingConstants.LEFT);
        send.addActionListener(e -> pushTelemetry());
        JPanel footer = Ui.transparent(new BorderLayout(Theme.S5, 0));
        footer.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(Theme.LINE2, 1, 0, 0, 0), new EmptyBorder(Theme.S5, 0, 0, 0)));
        footer.add(note, BorderLayout.CENTER);
        JPanel actions = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0)); actions.add(send);
        footer.add(actions, BorderLayout.EAST);

        JPanel card = new Ui.Plate(new BorderLayout(0, Theme.S5), Theme.S6);
        card.add(Ui.column(binGroup, Box.createVerticalStrut(Theme.S6), fillGroup, Box.createVerticalStrut(Theme.S5), secondaryGroup), BorderLayout.NORTH);
        card.add(footer, BorderLayout.SOUTH);

        // Keep the form at a readable width instead of stretching across a wide window.
        JPanel holder = Ui.transparent(new BorderLayout());
        holder.add(new MaxWidth(card, 820), BorderLayout.NORTH);
        page.add(holder, BorderLayout.CENTER); add(page, BorderLayout.CENTER);
        comboBins.addActionListener(e -> onBinSelectionChange()); updateBinList(loadedBins);
    }

    /** Wraps a component so it never grows wider than {@code max} pixels. */
    static final class MaxWidth extends JPanel {
        private final int max;
        MaxWidth(JComponent child, int max) { super(new BorderLayout()); this.max = max; setOpaque(false); add(child); }
        @Override public Dimension getPreferredSize() { Dimension d = super.getPreferredSize(); return new Dimension(Math.min(d.width, max), d.height); }
        @Override public void doLayout() { Component child = getComponent(0); child.setBounds(0, 0, Math.min(getWidth(), max), getHeight()); }
    }

    private JSlider slider(int value) {
        JSlider s = new JSlider(0, 100, value);
        s.setMajorTickSpacing(25); s.setPaintTicks(true); s.setPaintLabels(true); s.setOpaque(false);
        s.setFont(Theme.mono(12));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    private JPanel sliderHead(JLabel label, JLabel value) {
        JPanel head = Ui.transparent(new BorderLayout()); head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(label, BorderLayout.WEST); head.add(value, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, value.getPreferredSize().height));
        return head;
    }

    private void onBinSelectionChange() {
        if (comboBins == null || fillSlider == null) return;
        int idx = comboBins.getSelectedIndex(); if (idx < 0 || idx >= loadedBins.size()) { lblSelectedBin.setText("No bins available. Register a bin first."); return; }
        SmartBin bin = loadedBins.get(idx); fillSlider.setValue((int) bin.getFillPercentage()); lblFillValue.setText(fillSlider.getValue() + "%");
        lblSelectedBin.setText("Current fill " + String.format(java.util.Locale.US, "%.1f%%", bin.getFillPercentage()) + "  ·  " + bin.getBinType().getLabel() + " bin");
        if (bin instanceof RecyclingBin rBin) { secondaryUnit = "%"; lblSecondary.setText("Contamination level"); lblSensorHint.setText("Share of material that may be contaminated (0–100%)."); secondarySlider.setMinimum(0); secondarySlider.setMaximum(100); secondarySlider.setValue((int) rBin.getContaminationPercentage()); }
        else if (bin instanceof CompostBin cBin) { secondaryUnit = " °C"; lblSecondary.setText("Temperature"); lblSensorHint.setText("A simulated compost temperature, limited to 0–100 °C."); secondarySlider.setMinimum(0); secondarySlider.setMaximum(100); secondarySlider.setValue((int) cBin.getTempCelsius()); }
        lblSecondaryValue.setText(secondarySlider.getValue() + secondaryUnit);
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
