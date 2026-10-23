package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.model.CollectionRoute;
import com.greenpulse.model.Driver;
import com.greenpulse.model.RouteRun;
import com.greenpulse.model.RouteRun.StopStatus;
import com.greenpulse.model.SmartBin;
import com.greenpulse.model.User;
import com.greenpulse.dao.DriverDAO;
import com.greenpulse.dao.SmartBinDAO;
import com.greenpulse.service.RouteService;
import com.greenpulse.service.ReportExporter;
import com.greenpulse.service.RoadRoutingService;
import com.greenpulse.service.StopLocator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** OpenStreetMap base layer with OSRM street routing & Speed Control. */
public class RouteMapPanel extends JPanel {
    private static final double DEFAULT_DEPOT_LAT = 7.0731; // Davao City demo depot
    private static final double DEFAULT_DEPOT_LON = 125.6128;
    private static final int TILE_SIZE = 256;
    private static final int TILE_ZOOM = 15;
    private static final double PH_MIN_LAT = 4.5, PH_MAX_LAT = 21.5;
    private static final double PH_MIN_LON = 116.0, PH_MAX_LON = 127.5;

    private List<SmartBin> binList = new ArrayList<>();
    private final Runnable onNavBack;
    private final RoadRoutingService routingService = new RoadRoutingService();
    private static final double SERVICE_MINUTES_PER_STOP = 4.0;
    private final SmartBinDAO binDAO = new SmartBinDAO();
    private final User user;
    private final RouteService routeService;
    private RouteRun run;
    private int[] stopGeometryIndex;
    private boolean stopActionBusy = false;
    private int hintedStop = -1;
    private final JPanel stopsPanel = new JPanel();
    /**
     * A label that wraps long text onto several lines instead of cutting it off with "...".
     * Text is HTML-escaped, so bin names and error messages are shown exactly as written.
     */
    private static final class WrappingLabel extends JLabel {
        private static final int WRAP_WIDTH_PX = 330;

        WrappingLabel(String text) { setText(text); }

        @Override public void setText(String text) {
            super.setText(text == null ? null : "<html><div style='width:" + WRAP_WIDTH_PX + "px'>"
                    + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div></html>");
        }
    }

    private final JLabel lblRunSummary = new WrappingLabel("No route yet.");
    private final JCheckBox chkAutoCollect = new JCheckBox("Auto-collect as truck passes", true);
    private String savedRouteId;
    private final JTextField txtDepotLat = new JTextField(String.valueOf(DEFAULT_DEPOT_LAT), 8);
    private final JTextField txtDepotLon = new JTextField(String.valueOf(DEFAULT_DEPOT_LON), 8);
    private final JComboBox<BinChoice> comboPlaceBin = new JComboBox<>();
    private final JComboBox<String> comboCity = new JComboBox<>(new String[]{
            "Davao City", "Metro Manila", "Cebu City", "Cagayan de Oro", "Iloilo City", "Baguio City", "Zamboanga City", "General Santos City"});
    private final JLabel lblMapPickMode = new WrappingLabel("Tip: choose a map action, then click a street location.");
    private PickMode pickMode = PickMode.NONE;
    private final JComboBox<Driver> comboDrivers = new JComboBox<>();
    private final JLabel lblDriverStatus = new WrappingLabel("● Status: READY");
    private final JLabel lblTotalDistance = new WrappingLabel("Route distance: waiting for route");
    private final JLabel lblEstTime = new WrappingLabel("Estimated total time: —");
    private final JLabel lblFuel = new WrappingLabel("Fuel / energy estimate: —");
    private final JLabel lblRouteNote = new WrappingLabel("Enter GPS coordinates for bins to plan a route.");
    private final JButton btnDispatch = new JButton("▶ Calculate & Dispatch Route");
    private final MapCanvas mapCanvas = new MapCanvas();
    private RoadRoutingService.RouteResult routeResult;
    private boolean routeLoading = false;
    private boolean reroutePending = false;
    private Timer animTimer;
    private double animationProgress = 0;
    private int animationSegment = 0;

    // Dispatch Animation Speed Multiplier (1x, 2x, 5x, 10x)
    private double speedMultiplier = 1.0;
    private JProgressBar dispatchProgressBar;
    private JLabel lblSpeedIndicator;

    public RouteMapPanel(User user, List<SmartBin> bins, Runnable onNavBack) {
        this.binList = bins == null ? new ArrayList<>() : new ArrayList<>(bins);
        this.onNavBack = onNavBack;
        this.user = user;
        this.routeService = new RouteService(user);
        initUI();
        loadDriversAsync();
        refreshBinChoices();
        updateRouteAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(new Color(245, 248, 250));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("⚡ GIS Route Optimization & Fleet Dispatch");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(24, 39, 55));
        JLabel disclaimer = new JLabel("Place depot & bins on the map, calculate OSRM routes, and simulate animated driver dispatch with speed control.");
        disclaimer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        disclaimer.setForeground(new Color(100, 116, 139));
        JPanel titles = new JPanel(new BorderLayout(8, 4));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(disclaimer, BorderLayout.SOUTH);
        header.add(titles, BorderLayout.CENTER);

        // Always visible at the top right (the left control panel scrolls, so a checkbox there is easy to miss).
        chkAutoCollect.setFont(new Font("SansSerif", Font.BOLD, 12));
        chkAutoCollect.setForeground(new Color(6, 95, 70));
        chkAutoCollect.setOpaque(false);
        chkAutoCollect.setToolTipText("When ticked, each bin is recorded as collected (fill level reset to 0%) as the animated truck reaches it, whether the bin is critical or not. Untick it to confirm stops yourself with the ✔ buttons.");
        JPanel autoPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        autoPill.setBackground(new Color(236, 253, 245));
        autoPill.setBorder(BorderFactory.createLineBorder(new Color(167, 243, 208)));
        autoPill.add(chkAutoCollect);
        JPanel autoWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        autoWrap.setOpaque(false);
        autoWrap.add(autoPill);
        header.add(autoWrap, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        // Let BoxLayout calculate the full content height. A fixed preferred height here
        // can prevent the scroll pane from recognizing that the controls exceed the viewport.
        controls.setMinimumSize(new Dimension(310, 0));

        JLabel depotTitle = new JLabel("1. Set Collection Depot");
        depotTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        controls.add(depotTitle);
        controls.add(Box.createVerticalStrut(5));
        JPanel coords = new JPanel(new GridLayout(2, 2, 5, 4));
        coords.setOpaque(false);
        coords.add(new JLabel("Lat")); coords.add(txtDepotLat);
        coords.add(new JLabel("Lon")); coords.add(txtDepotLon);
        controls.add(coords);
        JButton pickDepot = new JButton("📍 Place Depot on Map");
        pickDepot.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickDepot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pickDepot.addActionListener(e -> {
            pickMode = PickMode.DEPOT;
            lblMapPickMode.setText("Click the map where your depot should be.");
            mapCanvas.repaint();
        });
        controls.add(Box.createVerticalStrut(5)); controls.add(pickDepot);
        JButton applyDepot = new JButton("Use Coordinates / Recalculate");
        applyDepot.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyDepot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        applyDepot.addActionListener(e -> updateRouteAsync());
        controls.add(Box.createVerticalStrut(4)); controls.add(applyDepot);
        controls.add(Box.createVerticalStrut(12));

        JLabel binTitle = new JLabel("2. Place Smart Bin Location");
        binTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        controls.add(binTitle);
        controls.add(Box.createVerticalStrut(5));
        comboPlaceBin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        controls.add(comboPlaceBin);
        JButton pickBin = new JButton("📍 Place Selected Bin on Map");
        pickBin.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickBin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pickBin.addActionListener(e -> {
            if (comboPlaceBin.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Register a smart bin first.", "No Bins", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            pickMode = PickMode.BIN;
            lblMapPickMode.setText("Click street location for " + comboPlaceBin.getSelectedItem() + ".");
            mapCanvas.repaint();
        });
        controls.add(Box.createVerticalStrut(4)); controls.add(pickBin);
        lblMapPickMode.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblMapPickMode.setForeground(new Color(5, 150, 105));
        lblMapPickMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMapPickMode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        controls.add(Box.createVerticalStrut(5)); controls.add(lblMapPickMode);
        
        JButton fitMap = new JButton("🔍 Fit Depot & Bins in View");
        fitMap.setAlignmentX(Component.LEFT_ALIGNMENT);
        fitMap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        fitMap.addActionListener(e -> { mapCanvas.manualCenter = false; mapCanvas.repaint(); });
        controls.add(Box.createVerticalStrut(4)); controls.add(fitMap);
        controls.add(Box.createVerticalStrut(12));

        // Speed Control Section
        JLabel speedTitle = new JLabel("3. Dispatch Speed Multiplier");
        speedTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        controls.add(speedTitle);
        controls.add(Box.createVerticalStrut(6));

        JPanel speedBtnGroup = new JPanel(new GridLayout(1, 4, 4, 0));
        speedBtnGroup.setOpaque(false);
        speedBtnGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JButton btn1x = new JButton("1x");
        JButton btn2x = new JButton("2x");
        JButton btn5x = new JButton("5x");
        JButton btn10x = new JButton("10x");

        styleSpeedButton(btn1x, true);
        styleSpeedButton(btn2x, false);
        styleSpeedButton(btn5x, false);
        styleSpeedButton(btn10x, false);

        btn1x.addActionListener(e -> setSpeedMultiplier(1.0, btn1x, btn2x, btn5x, btn10x));
        btn2x.addActionListener(e -> setSpeedMultiplier(2.0, btn2x, btn1x, btn5x, btn10x));
        btn5x.addActionListener(e -> setSpeedMultiplier(5.0, btn5x, btn1x, btn2x, btn10x));
        btn10x.addActionListener(e -> setSpeedMultiplier(10.0, btn10x, btn1x, btn2x, btn5x));

        speedBtnGroup.add(btn1x);
        speedBtnGroup.add(btn2x);
        speedBtnGroup.add(btn5x);
        speedBtnGroup.add(btn10x);
        controls.add(speedBtnGroup);
        controls.add(Box.createVerticalStrut(12));

        JLabel vehicleTitle = new JLabel("4. Assign Vehicle");
        vehicleTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        controls.add(vehicleTitle);
        controls.add(Box.createVerticalStrut(4));
        comboDrivers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboDrivers.addActionListener(e -> updateMetrics());
        controls.add(comboDrivers);
        controls.add(Box.createVerticalStrut(14));

        for (JLabel label : new JLabel[]{lblTotalDistance, lblEstTime, lblFuel}) {
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setForeground(new Color(30, 41, 59));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            controls.add(label);
            controls.add(Box.createVerticalStrut(6));
        }

        lblDriverStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDriverStatus.setForeground(new Color(5, 150, 105));
        controls.add(Box.createVerticalStrut(4));
        controls.add(lblDriverStatus);
        lblRouteNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblRouteNote.setForeground(new Color(71, 85, 105));
        lblRouteNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(Box.createVerticalStrut(4));
        controls.add(lblRouteNote);
        controls.add(Box.createVerticalStrut(8));

        // Dispatch Route Progress Bar
        dispatchProgressBar = new JProgressBar(0, 100);
        dispatchProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        dispatchProgressBar.setStringPainted(true);
        dispatchProgressBar.setFont(new Font("SansSerif", Font.BOLD, 10));
        dispatchProgressBar.setForeground(new Color(16, 185, 129));
        controls.add(dispatchProgressBar);
        controls.add(Box.createVerticalStrut(8));

        btnDispatch.setBackground(new Color(5, 150, 105));
        btnDispatch.setForeground(Color.WHITE);
        btnDispatch.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnDispatch.setFocusPainted(false);
        btnDispatch.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDispatch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnDispatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDispatch.addActionListener(e -> {
            if (routeLoading) return;
            if (routeResult == null || routeResult.geometry.size() < 2) {
                updateRouteAsync();
                return;
            }
            animationProgress = 0;
            animationSegment = 0;
            dispatchProgressBar.setValue(0);
            lblDriverStatus.setText("● Status: EN ROUTE 🚚 (" + speedMultiplier + "x speed)");
            if (!animTimer.isRunning()) animTimer.start();
        });
        controls.add(btnDispatch);

        JButton reset = new JButton("⏹ Reset Truck Animation");
        reset.setFont(new Font("SansSerif", Font.BOLD, 11));
        reset.setAlignmentX(Component.LEFT_ALIGNMENT);
        reset.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        reset.addActionListener(e -> {
            if (animTimer != null) animTimer.stop();
            animationProgress = 0; animationSegment = 0;
            dispatchProgressBar.setValue(0);
            lblDriverStatus.setText("● Status: READY");
            mapCanvas.repaint();
        });
        controls.add(Box.createVerticalStrut(6));
        controls.add(reset);

        JLabel stopsTitle = new JLabel("5. Route stops");
        stopsTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        stopsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(Box.createVerticalStrut(14));
        controls.add(stopsTitle);
        controls.add(Box.createVerticalStrut(4));
        lblRunSummary.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblRunSummary.setForeground(new Color(71, 85, 105));
        lblRunSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(lblRunSummary);
        controls.add(Box.createVerticalStrut(4));
        stopsPanel.setLayout(new BoxLayout(stopsPanel, BoxLayout.Y_AXIS));
        stopsPanel.setOpaque(false);
        stopsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(stopsPanel);

        JButton export = new JButton("💾 Save Route & Export…");
        export.setFont(new Font("SansSerif", Font.BOLD, 11));
        export.setAlignmentX(Component.LEFT_ALIGNMENT);
        export.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        export.addActionListener(e -> saveAndExportRoute());
        controls.add(Box.createVerticalStrut(6));
        controls.add(export);

        JButton collected = new JButton("✔ Collect All Remaining Stops");
        collected.setFont(new Font("SansSerif", Font.BOLD, 11));
        collected.setAlignmentX(Component.LEFT_ALIGNMENT);
        collected.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        collected.addActionListener(e -> collectAllRemaining());
        controls.add(Box.createVerticalStrut(6));
        controls.add(collected);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        // Use one left edge for everything and let buttons and drop-downs fill the panel, so their text is not cut off.
        for (Component comp : controls.getComponents()) {
            if (!(comp instanceof JComponent jc) || jc instanceof Box.Filler) continue;
            jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (jc == stopsPanel) continue; // its height grows and shrinks with the number of stops
            if (jc instanceof JPanel) {
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height)); // do not stretch tall
            } else if (jc instanceof JButton || jc instanceof JComboBox || jc instanceof JProgressBar) {
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getMaximumSize().height));
            }
        }
        JScrollPane controlsScroll = new JScrollPane(controls);
        controlsScroll.setBorder(BorderFactory.createEmptyBorder());
        controlsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        controlsScroll.setPreferredSize(new Dimension(390, 480));
        controlsScroll.setMinimumSize(new Dimension(300, 180));
        controlsScroll.getVerticalScrollBar().setUnitIncrement(18);
        controlsScroll.getViewport().setBackground(Color.WHITE);
        controlsScroll.setViewportBorder(BorderFactory.createEmptyBorder());

        // High-Visibility Map Border with Accent Frame
        mapCanvas.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(16, 185, 129), 3), // Emerald Accent Border
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1)
        ));

        // A draggable split pane gives the controls enough room for labels and buttons
        // without permanently squeezing the map. The controls remain independently scrollable.
        JSplitPane dispatchSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlsScroll, mapCanvas);
        dispatchSplit.setContinuousLayout(true);
        dispatchSplit.setOneTouchExpandable(true);
        dispatchSplit.setResizeWeight(0.38);
        dispatchSplit.setDividerSize(8);
        dispatchSplit.setBorder(BorderFactory.createEmptyBorder());
        dispatchSplit.setBackground(new Color(245, 248, 250));
        dispatchSplit.setPreferredSize(new Dimension(1000, 560));
        dispatchSplit.setMinimumSize(new Dimension(600, 300));
        dispatchSplit.setDividerLocation(390);
        add(dispatchSplit, BorderLayout.CENTER);

        animTimer = new Timer(40, e -> {
            if (routeResult == null || routeResult.geometry.size() < 2) { animTimer.stop(); return; }
            animationProgress += 0.035 * speedMultiplier;
            if (animationProgress >= 1.0) {
                animationProgress = 0.0;
                animationSegment++;
                if (animationSegment >= routeResult.geometry.size() - 1) {
                    animTimer.stop();
                    dispatchProgressBar.setValue(100);
                    lblDriverStatus.setText(driveFinishedText());
                }
            }
            checkTruckReachedStops();
            int totalSegs = routeResult.geometry.size() - 1;
            if (totalSegs > 0) {
                double totalProg = ((double) animationSegment + animationProgress) / totalSegs * 100.0;
                dispatchProgressBar.setValue((int) Math.min(100, totalProg));
            }
            mapCanvas.repaint();
        });
    }

    private void styleSpeedButton(JButton btn, boolean active) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(2, 4, 2, 4));
        if (active) {
            btn.setBackground(new Color(16, 185, 129));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(226, 232, 240));
            btn.setForeground(new Color(51, 65, 85));
        }
    }

    private void setSpeedMultiplier(double mult, JButton activeBtn, JButton... otherBtns) {
        this.speedMultiplier = mult;
        styleSpeedButton(activeBtn, true);
        for (JButton b : otherBtns) styleSpeedButton(b, false);
        if (animTimer != null && animTimer.isRunning()) {
            lblDriverStatus.setText("● Status: EN ROUTE 🚚 (" + speedMultiplier + "x speed)");
        }
    }

    private double depotLat() { return Double.parseDouble(txtDepotLat.getText().trim()); }
    private double depotLon() { return Double.parseDouble(txtDepotLon.getText().trim()); }

    private void updateRouteAsync() {
        if (routeLoading) { reroutePending = true; return; }
        final double lat, lon;
        try {
            lat = depotLat(); lon = depotLon();
            if (!isPhilippinesCoordinate(lat, lon))
                throw new IllegalArgumentException("Choose a location inside the Philippines.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter valid depot coordinates. " + ex.getMessage(), "Invalid Depot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        routeLoading = true;
        btnDispatch.setEnabled(false);
        btnDispatch.setText("Calculating road route…");
        lblRouteNote.setText("Requesting road distances and route geometry from OSRM…");
        new SwingWorker<RoadRoutingService.RouteResult, Void>() {
            @Override protected RoadRoutingService.RouteResult doInBackground() throws Exception {
                return routingService.calculateOptimizedRoute(lat, lon, binList);
            }
            @Override protected void done() {
                routeLoading = false;
                btnDispatch.setEnabled(true);
                btnDispatch.setText("▶ Calculate & Dispatch Route");
                if (reroutePending) {
                    reroutePending = false;
                    updateRouteAsync();
                    return;
                }
                try {
                    routeResult = get();
                    savedRouteId = null;
                    rebuildRun();
                    if (routeResult.geometry.isEmpty()) {
                        lblRouteNote.setText("No bins with GPS coordinates are available to route.");
                        lblTotalDistance.setText("Road distance: —");
                        lblEstTime.setText("Drive + service: —");
                        lblFuel.setText("Fuel / energy estimate: —");
                    } else {
                        int missing = 0;
                        for (SmartBin bin : binList) if (!bin.hasCoordinates()) missing++;
                        lblRouteNote.setText("Optimized " + routeResult.orderedBins.size() + " stop(s) by road distance. " +
                                (missing > 0 ? missing + " bin(s) missing GPS." : "All active bins have GPS coordinates."));
                        lblDriverStatus.setText("● Status: ROUTE READY");
                    }
                    updateMetrics();
                    mapCanvas.repaint();
                } catch (Exception ex) {
                    routeResult = null;
                    rebuildRun();
                    lblRouteNote.setText("Routing unavailable: " + safeMessage(ex) + " Check internet or coordinates.");
                    lblTotalDistance.setText("Route distance: unavailable");
                    lblEstTime.setText("Estimated total time: unavailable");
                    lblFuel.setText("Fuel / energy estimate: unavailable");
                    mapCanvas.repaint();
                }
            }
        }.execute();
    }

    private String safeMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) return cause.getClass().getSimpleName();
        return message.length() > 110 ? message.substring(0, 107) + "..." : message;
    }

    private void updateMetrics() {
        if (routeResult == null || routeResult.geometry.isEmpty()) return;
        double km = routeResult.distanceMeters / 1000.0;
        double driveMinutes = routeResult.durationSeconds / 60.0;
        double serviceMinutes = routeResult.orderedBins.size() * SERVICE_MINUTES_PER_STOP;
        lblTotalDistance.setText(String.format(Locale.US, "Road distance: %.2f km", km));
        lblEstTime.setText(String.format(Locale.US, "Drive + service: %.0f min", driveMinutes + serviceMinutes));
        Driver driver = (Driver) comboDrivers.getSelectedItem();
        lblFuel.setText(driver == null ? "Fuel / energy estimate: —" : driver.describeFuelUse(km));
    }

    private void loadDriversAsync() {
        new SwingWorker<List<Driver>, Void>() {
            @Override protected List<Driver> doInBackground() throws Exception { return new DriverDAO().loadAllDrivers(); }
            @Override protected void done() {
                try {
                    DefaultComboBoxModel<Driver> model = new DefaultComboBoxModel<>();
                    for (Driver d : get()) model.addElement(d);
                    comboDrivers.setModel(model);
                    updateMetrics();
                } catch (Exception ex) {
                    lblRouteNote.setText("Could not load drivers: " + safeMessage(ex));
                }
            }
        }.execute();
    }

    /** Saves the current route to the database (once) and writes a CSV or HTML manifest for the driver. */
    private void saveAndExportRoute() {
        if (routeResult == null || routeResult.orderedBins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Calculate a route with at least one bin first.", "No route to export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final double dLat, dLon;
        try { dLat = depotLat(); dLon = depotLon(); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter valid depot coordinates first.", "Invalid depot", JOptionPane.WARNING_MESSAGE); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export route manifest (.html or .csv)");
        chooser.setSelectedFile(new java.io.File("greenpulse-route-manifest.html"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final Path target = chooser.getSelectedFile().toPath();
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this, target.getFileName() + " already exists. Replace it?",
                "Replace file", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        final RoadRoutingService.RouteResult result = routeResult;
        final Driver driver = (Driver) comboDrivers.getSelectedItem();
        final String existingId = savedRouteId;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                String id = saveRouteIfNeeded(existingId, driver, dLat, dLon, result);
                double minutes = result.durationSeconds / 60.0 + result.orderedBins.size() * SERVICE_MINUTES_PER_STOP;
                CollectionRoute route = new CollectionRoute(id, result.orderedBins, result.distanceMeters / 1000.0, minutes,
                        driver == null ? "Unassigned" : driver.getDriverName(), driver == null ? "—" : driver.getVehicleName());
                if (target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) ReportExporter.exportRouteToCSV(route, target);
                else ReportExporter.exportPrintableReportHTML(route, target);
                return id;
            }
            @Override protected void done() {
                try {
                    savedRouteId = get();
                    JOptionPane.showMessageDialog(RouteMapPanel.this, "Route " + savedRouteId + " saved.\nManifest written to " + target,
                            "Route exported", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RouteMapPanel.this, "Could not export the route: " + safeMessage(ex), "Export failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- route run: collecting and skipping stops

    private void rebuildRun() {
        if (routeResult == null || routeResult.orderedBins.isEmpty()) {
            run = null;
            stopGeometryIndex = null;
        } else {
            run = new RouteRun(routeResult.orderedBins);
            hintedStop = -1;
            stopGeometryIndex = StopLocator.locate(routeResult.geometry, routeResult.orderedBins);
        }
        rebuildStopsPanel();
    }

    private void rebuildStopsPanel() {
        stopsPanel.removeAll();
        if (run == null) {
            lblRunSummary.setText("No route yet. Calculate a route to see its stops.");
        } else {
            lblRunSummary.setText(run.count(StopStatus.COLLECTED) + " collected • " + run.count(StopStatus.SKIPPED)
                    + " skipped • " + run.count(StopStatus.PENDING) + " pending");
            for (int i = 0; i < run.size(); i++) stopsPanel.add(stopRow(i));
        }
        stopsPanel.revalidate();
        stopsPanel.repaint();
    }

    private JPanel stopRow(int index) {
        SmartBin bin = run.bin(index);
        StopStatus status = run.status(index);
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        String mark = status == StopStatus.COLLECTED ? "✓ " : status == StopStatus.SKIPPED ? "× " : "";
        JLabel label = new JLabel(String.format(Locale.US, "%s%d. %s  %d%%", mark, index + 1, bin.getBinID(), Math.round(bin.getFillPercentage())));
        label.setFont(new Font("SansSerif", status == StopStatus.PENDING ? Font.PLAIN : Font.BOLD, 11));
        label.setForeground(status == StopStatus.COLLECTED ? new Color(22, 101, 52)
                : status == StopStatus.SKIPPED ? new Color(100, 116, 139) : new Color(30, 41, 59));
        if (status == StopStatus.SKIPPED) label.setToolTipText("Skipped: " + run.skipReason(index));
        row.add(label, BorderLayout.CENTER);
        if (status == StopStatus.PENDING) {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
            buttons.setOpaque(false);
            JButton collect = new JButton("✔");
            collect.setToolTipText("Collect this stop: the bin is emptied and its fill level reset");
            collect.addActionListener(e -> resolveStopAsync(index, true, null, false));
            JButton skip = new JButton("⏭");
            skip.setToolTipText("Skip this stop and record why");
            skip.addActionListener(e -> skipStopWithReason(index));
            for (JButton b : new JButton[]{collect, skip}) {
                b.setMargin(new Insets(1, 6, 1, 6));
                b.setFocusPainted(false);
                b.setEnabled(!stopActionBusy);
                buttons.add(b);
            }
            row.add(buttons, BorderLayout.EAST);
        }
        return row;
    }

    private void skipStopWithReason(int index) {
        if (run == null || run.status(index) != StopStatus.PENDING) return;
        Object choice = JOptionPane.showInputDialog(this, "Why is " + run.bin(index).getBinID() + " being skipped?",
                "Skip stop", JOptionPane.QUESTION_MESSAGE, null, RouteRun.SKIP_REASONS.toArray(), RouteRun.SKIP_REASONS.get(0));
        if (choice != null) resolveStopAsync(index, false, choice.toString(), false);
    }

    /**
     * Called on every animation step. Once the truck has reached the next pending stop it is collected
     * automatically (if auto-collect is ticked) or the driver is told to confirm it with the tick button.
     */
    private void checkTruckReachedStops() {
        if (run == null || stopGeometryIndex == null || stopActionBusy) return;
        int next = run.nextPending();
        if (next < 0 || stopGeometryIndex[next] > animationSegment) return;
        String id = run.bin(next).getBinID();
        if (chkAutoCollect.isSelected()) {
            lblRouteNote.setText("Truck reached " + id + " — recording the collection…");
            resolveStopAsync(next, true, null, true);
        } else if (hintedStop != next) {
            hintedStop = next;
            lblRouteNote.setText("Truck reached " + id + ". Auto-collect is off, so press ✔ in Route stops to record it.");
        }
    }

    private String driveFinishedText() {
        if (run == null || run.isFinished()) return "● Status: ROUTE COMPLETED ✓";
        return "● Status: BACK AT DEPOT – " + run.count(StopStatus.PENDING) + " stop(s) still open";
    }

    /** Saves the route the first time it is needed so stops can be recorded against it. Runs off the Swing thread. */
    private String saveRouteIfNeeded(String existingId, Driver driver, double dLat, double dLon,
                                     RoadRoutingService.RouteResult result) throws Exception {
        if (existingId != null) return existingId;
        return routeService.saveRoute(driver == null ? null : driver.getDriverId(), dLat, dLon,
                result.distanceMeters, result.durationSeconds, result.orderedBins);
    }

    private void onRunProgress() {
        rebuildStopsPanel();
        mapCanvas.repaint();
        if (run != null && run.isFinished()) {
            lblDriverStatus.setText("● Status: ROUTE COMPLETED ✓ (" + run.count(StopStatus.COLLECTED) + " collected, "
                    + run.count(StopStatus.SKIPPED) + " skipped)");
            lblRouteNote.setText("Route complete. Open the Monitor or Analytics to see the updated fill levels and collection stats.");
        }
    }

    private void resolveStopAsync(int index, boolean collect, String reason, boolean automatic) {
        if (run == null || stopActionBusy || run.status(index) != StopStatus.PENDING) return;
        final double dLat, dLon;
        try { dLat = depotLat(); dLon = depotLon(); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid depot coordinates first.", "Invalid depot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final RouteRun currentRun = run;
        final RoadRoutingService.RouteResult result = routeResult;
        final Driver driver = (Driver) comboDrivers.getSelectedItem();
        final String existingId = savedRouteId;
        final SmartBin bin = run.bin(index);
        stopActionBusy = true;
        rebuildStopsPanel();
        new SwingWorker<String, Void>() {
            private Optional<Double> fillBefore = Optional.empty();

            @Override protected String doInBackground() throws Exception {
                String id = saveRouteIfNeeded(existingId, driver, dLat, dLon, result);
                if (collect) fillBefore = routeService.collectStop(id, bin);
                else routeService.skipStop(id, bin.getBinID(), reason);
                return id;
            }

            @Override protected void done() {
                stopActionBusy = false;
                try {
                    String id = get();
                    if (currentRun == run) { // ignore if the route was recalculated while this was saving
                        savedRouteId = id;
                        if (!collect) currentRun.markSkipped(index, reason);
                        else if (fillBefore.isPresent()) {
                            currentRun.markCollected(index);
                            lblRouteNote.setText(String.format(Locale.US, "Collected %s (%.0f%% → 0%%).", bin.getBinID(), fillBefore.get()));
                        }
                        else currentRun.markSkipped(index, "Bin no longer in service");
                    }
                } catch (Exception ex) {
                    if (automatic) chkAutoCollect.setSelected(false);
                    JOptionPane.showMessageDialog(RouteMapPanel.this, "Could not update the stop: " + safeMessage(ex),
                            "Stop not recorded", JOptionPane.ERROR_MESSAGE);
                }
                onRunProgress();
            }
        }.execute();
    }

    /** Collects every stop that is still pending, one after another, then reports how many were done. */
    private void collectAllRemaining() {
        if (run == null || run.isFinished()) {
            JOptionPane.showMessageDialog(this, run == null ? "Calculate a route with at least one bin first."
                    : "Every stop on this route has already been collected or skipped.", "Nothing to collect", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (stopActionBusy) return;
        final double dLat, dLon;
        try { dLat = depotLat(); dLon = depotLon(); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid depot coordinates first.", "Invalid depot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final List<Integer> pending = new ArrayList<>();
        for (int i = 0; i < run.size(); i++) if (run.status(i) == StopStatus.PENDING) pending.add(i);
        if (JOptionPane.showConfirmDialog(this, "Collect the " + pending.size() + " remaining stop(s)?\nTheir fill levels will be reset to 0%.",
                "Collect all remaining stops", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        final RouteRun currentRun = run;
        final RoadRoutingService.RouteResult result = routeResult;
        final Driver driver = (Driver) comboDrivers.getSelectedItem();
        final String existingId = savedRouteId;
        stopActionBusy = true;
        rebuildStopsPanel();
        new SwingWorker<Void, Void>() {
            private volatile String routeId;
            private final Map<Integer, Boolean> outcome = new java.util.concurrent.ConcurrentHashMap<>();

            @Override protected Void doInBackground() throws Exception {
                routeId = saveRouteIfNeeded(existingId, driver, dLat, dLon, result);
                for (int index : pending) {
                    outcome.put(index, routeService.collectStop(routeId, currentRun.bin(index)).isPresent());
                }
                return null;
            }

            @Override protected void done() {
                stopActionBusy = false;
                Exception failure = null;
                try { get(); } catch (Exception ex) { failure = ex; }
                if (currentRun == run) {
                    if (routeId != null) savedRouteId = routeId;
                    for (int index : pending) {
                        Boolean collected = outcome.get(index);
                        if (collected == null) continue;
                        if (collected) currentRun.markCollected(index);
                        else currentRun.markSkipped(index, "Bin no longer in service");
                    }
                }
                onRunProgress();
                if (failure != null) {
                    JOptionPane.showMessageDialog(RouteMapPanel.this, "Stopped after " + outcome.size() + " stop(s): " + safeMessage(failure),
                            "Collection failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void refreshBins(List<SmartBin> bins) {
        this.binList = bins == null ? new ArrayList<>() : new ArrayList<>(bins);
        mapCanvas.manualCenter = false;
        refreshBinChoices();
        routeResult = null;
        savedRouteId = null;
        rebuildRun();
        if (animTimer != null) animTimer.stop();
        animationProgress = 0; animationSegment = 0;
        dispatchProgressBar.setValue(0);
        lblDriverStatus.setText("● Status: READY");
        mapCanvas.repaint();
        updateRouteAsync();
    }

    private boolean isPhilippinesCoordinate(double lat, double lon) {
        return Double.isFinite(lat) && lat >= PH_MIN_LAT && lat <= PH_MAX_LAT
                && Double.isFinite(lon) && lon >= PH_MIN_LON && lon <= PH_MAX_LON;
    }

    private void refreshBinChoices() {
        Object old = comboPlaceBin.getSelectedItem();
        comboPlaceBin.removeAllItems();
        for (SmartBin bin : binList) comboPlaceBin.addItem(new BinChoice(bin));
        if (old instanceof BinChoice oldChoice) {
            for (int i = 0; i < comboPlaceBin.getItemCount(); i++) {
                if (comboPlaceBin.getItemAt(i).bin.getBinID().equals(oldChoice.bin.getBinID())) {
                    comboPlaceBin.setSelectedIndex(i); break;
                }
            }
        }
    }

    private void handleMapPick(double lat, double lon) {
        if (!isPhilippinesCoordinate(lat, lon)) {
            lblMapPickMode.setText("Point outside Philippines. Click inside the region.");
            return;
        }
        if (pickMode == PickMode.DEPOT) {
            txtDepotLat.setText(String.format(Locale.US, "%.6f", lat));
            txtDepotLon.setText(String.format(Locale.US, "%.6f", lon));
            pickMode = PickMode.NONE;
            lblMapPickMode.setText("Depot set. Calculating road route…");
            mapCanvas.repaint();
            updateRouteAsync();
            return;
        }
        if (pickMode == PickMode.BIN) {
            BinChoice choice = (BinChoice) comboPlaceBin.getSelectedItem();
            if (choice == null) { pickMode = PickMode.NONE; return; }
            SmartBin bin = choice.bin;
            lblMapPickMode.setText("Saving location for " + bin.getBinID() + "…");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return binDAO.updateCoordinates(bin.getBinID(), lat, lon);
                }
                @Override protected void done() {
                    try {
                        if (get()) {
                            bin.setCoordinates(lat, lon);
                            pickMode = PickMode.NONE;
                            lblMapPickMode.setText(String.format(Locale.US, "%s placed at %.5f, %.5f", bin.getBinID(), lat, lon));
                            updateRouteAsync();
                            mapCanvas.repaint();
                        } else {
                            lblMapPickMode.setText("Bin not found in database.");
                        }
                    } catch (Exception ex) {
                        lblMapPickMode.setText("Could not save location: " + safeMessage(ex));
                    }
                }
            }.execute();
        }
    }

    private enum PickMode { NONE, DEPOT, BIN }

    private static final class BinChoice {
        private final SmartBin bin;
        private BinChoice(SmartBin bin) { this.bin = bin; }
        @Override public String toString() {
            return bin.getBinID() + (bin.hasCoordinates() ? " • placed" : " • needs location");
        }
    }

    private class MapCanvas extends JPanel {
        private final ConcurrentHashMap<String, BufferedImage> memoryTiles = new ConcurrentHashMap<>();
        private final Set<String> loadingTiles = ConcurrentHashMap.newKeySet();
        private final ExecutorService tileExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "greenpulse-map-tiles"); t.setDaemon(true); return t;
        });
        private final HttpClient tileClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        private final Path tileRoot = Path.of(System.getProperty("user.home"), ".greenpulse", "map-tiles");
        private double zoom = TILE_ZOOM;
        private double centerLat = DEFAULT_DEPOT_LAT;
        private double centerLon = DEFAULT_DEPOT_LON;
        private boolean manualCenter = false;
        private Point dragStart;
        private Point2D.Double dragCenterWorld;

        MapCanvas() {
            setBackground(new Color(230, 237, 242));
            addMouseWheelListener(e -> {
                zoom = Math.max(11, Math.min(18, zoom + (e.getWheelRotation() < 0 ? 1 : -1)));
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (pickMode == PickMode.NONE && SwingUtilities.isLeftMouseButton(e)) {
                        dragStart = e.getPoint();
                        dragCenterWorld = worldPixel(centerLat, centerLon, Math.pow(2, zoom));
                    }
                }
                @Override public void mouseReleased(MouseEvent e) { dragStart = null; dragCenterWorld = null; }
                @Override public void mouseClicked(MouseEvent e) {
                    if (pickMode != PickMode.NONE) {
                        double scale = Math.pow(2, zoom);
                        Point2D.Double center = worldPixel(centerLat, centerLon, scale);
                        double left = center.x - getWidth() / 2.0;
                        double top = center.y - getHeight() / 2.0;
                        Point2D.Double world = new Point2D.Double(left + e.getX(), top + e.getY());
                        double lon = world.x / (scale * TILE_SIZE) * 360.0 - 180.0;
                        double mercator = Math.PI * (1.0 - 2.0 * world.y / (scale * TILE_SIZE));
                        double lat = Math.toDegrees(Math.atan(Math.sinh(mercator)));
                        handleMapPick(lat, lon);
                    } else if (e.getClickCount() == 2) {
                        zoom = Math.min(18, zoom + 1); repaint();
                    }
                }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (pickMode != PickMode.NONE || dragStart == null || dragCenterWorld == null) return;
                    double scale = Math.pow(2, zoom);
                    double wx = dragCenterWorld.x - (e.getX() - dragStart.x);
                    double wy = dragCenterWorld.y - (e.getY() - dragStart.y);
                    centerLon = wx / (scale * TILE_SIZE) * 360.0 - 180.0;
                    double mercator = Math.PI * (1.0 - 2.0 * wy / (scale * TILE_SIZE));
                    centerLat = Math.toDegrees(Math.atan(Math.sinh(mercator)));
                    manualCenter = true;
                    repaint();
                }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            updateCenter();
            int w = getWidth(), h = getHeight();
            double scale = Math.pow(2, zoom);
            Point2D.Double center = worldPixel(centerLat, centerLon, scale);
            double left = center.x - w / 2.0, top = center.y - h / 2.0;
            int minTileX = (int) Math.floor(left / TILE_SIZE), maxTileX = (int) Math.floor((left + w) / TILE_SIZE);
            int minTileY = (int) Math.floor(top / TILE_SIZE), maxTileY = (int) Math.floor((top + h) / TILE_SIZE);
            int worldTiles = (int) scale;
            for (int ty = minTileY; ty <= maxTileY; ty++) {
                if (ty < 0 || ty >= worldTiles) continue;
                for (int tx = minTileX; tx <= maxTileX; tx++) {
                    int wrappedX = ((tx % worldTiles) + worldTiles) % worldTiles;
                    int sx = (int) Math.round(tx * TILE_SIZE - left);
                    int sy = (int) Math.round(ty * TILE_SIZE - top);
                    BufferedImage tile = getTile((int) zoom, wrappedX, ty);
                    if (tile != null) g.drawImage(tile, sx, sy, TILE_SIZE, TILE_SIZE, null);
                    else {
                        g.setColor(new Color(235, 240, 244)); g.fillRect(sx, sy, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
            drawRouteAndPins(g, center, left, top, scale);

            // Speed Indicator Overlay
            g.setColor(new Color(15, 23, 42, 210));
            g.fillRoundRect(w - 120, 10, 110, 26, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("⚡ Speed: " + speedMultiplier + "x", w - 110, 27);

            // Attribution Footer
            g.setColor(new Color(255, 255, 255, 235));
            g.fillRoundRect(8, h - 31, 250, 22, 8, 8);
            g.setColor(new Color(30, 41, 59));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString("© OpenStreetMap contributors | Wheel: zoom", 14, h - 16);

            if (pickMode != PickMode.NONE) {
                g.setColor(new Color(5, 150, 105, 230));
                g.fillRoundRect(8, 8, Math.min(w - 140, 340), 28, 10, 10);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString(pickMode == PickMode.DEPOT ? "CLICK MAP TO PLACE DEPOT" : "CLICK MAP TO PLACE SELECTED BIN", 18, 26);
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
            g.dispose();
        }

        private void updateCenter() {
            if (manualCenter) return;
            double sumLat = depotLatSafe(), sumLon = depotLonSafe();
            int n = 1;
            for (SmartBin b : binList) if (b.hasCoordinates()) { sumLat += b.getLatitude(); sumLon += b.getLongitude(); n++; }
            centerLat = sumLat / n; centerLon = sumLon / n;
        }

        private double depotLatSafe() { try { return depotLat(); } catch (Exception e) { return DEFAULT_DEPOT_LAT; } }
        private double depotLonSafe() { try { return depotLon(); } catch (Exception e) { return DEFAULT_DEPOT_LON; } }

        private void drawRouteAndPins(Graphics2D g, Point2D.Double center, double left, double top, double scale) {
            Point2D.Double depot = screenPoint(depotLatSafe(), depotLonSafe(), left, top, scale);
            if (routeResult != null && routeResult.geometry.size() > 1) {
                Path2D path = new Path2D.Double();
                for (int i = 0; i < routeResult.geometry.size(); i++) {
                    RoadRoutingService.Coordinate c = routeResult.geometry.get(i);
                    Point2D.Double p = screenPoint(c.latitude, c.longitude, left, top, scale);
                    if (i == 0) path.moveTo(p.x, p.y); else path.lineTo(p.x, p.y);
                }
                g.setColor(new Color(37, 99, 235, 75));
                g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
                g.setColor(new Color(37, 99, 235));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);

                // Animated Truck Icon
                if (animTimer != null && animTimer.isRunning() && animationSegment < routeResult.geometry.size() - 1) {
                    RoadRoutingService.Coordinate a = routeResult.geometry.get(animationSegment);
                    RoadRoutingService.Coordinate b = routeResult.geometry.get(animationSegment + 1);
                    Point2D.Double pa = screenPoint(a.latitude, a.longitude, left, top, scale);
                    Point2D.Double pb = screenPoint(b.latitude, b.longitude, left, top, scale);
                    int x = (int) (pa.x + (pb.x - pa.x) * animationProgress);
                    int y = (int) (pa.y + (pb.y - pa.y) * animationProgress);

                    g.setColor(new Color(245, 158, 11, 120));
                    g.fillOval(x - 14, y - 14, 28, 28);

                    g.setColor(new Color(245, 158, 11));
                    g.fillOval(x - 10, y - 10, 20, 20);
                    g.setColor(Color.WHITE);
                    g.drawOval(x - 10, y - 10, 20, 20);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g.drawString("🚚", x - 6, y + 4);
                }

                int index = 1;
                for (SmartBin bin : routeResult.orderedBins) {
                    Point2D.Double p = screenPoint(bin.getLatitude(), bin.getLongitude(), left, top, scale);
                    StopStatus status = (run != null && index - 1 < run.size()) ? run.status(index - 1) : StopStatus.PENDING;
                    drawPin(g, (int) p.x, (int) p.y, String.valueOf(index++), bin, status);
                }
            } else {
                int index = 1;
                for (SmartBin bin : binList) if (bin.hasCoordinates()) {
                    Point2D.Double p = screenPoint(bin.getLatitude(), bin.getLongitude(), left, top, scale);
                    drawPin(g, (int) p.x, (int) p.y, String.valueOf(index++), bin);
                }
            }
            g.setColor(new Color(29, 78, 216)); g.fillOval((int) depot.x - 9, (int) depot.y - 9, 18, 18);
            g.setColor(Color.WHITE); g.drawOval((int) depot.x - 6, (int) depot.y - 6, 12, 12);
            g.setColor(new Color(15, 23, 42)); g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("DEPOT", (int) depot.x + 10, (int) depot.y - 8);
        }

        private void drawPin(Graphics2D g, int x, int y, String order, SmartBin bin) {
            drawPin(g, x, y, order, bin, StopStatus.PENDING);
        }

        /** Collected stops show a dark-green tick, skipped stops a grey cross; pending stops show their stop number. */
        private void drawPin(Graphics2D g, int x, int y, String order, SmartBin bin, StopStatus status) {
            Color color = status == StopStatus.COLLECTED ? new Color(22, 101, 52)
                    : status == StopStatus.SKIPPED ? new Color(100, 116, 139)
                    : CollectionPolicy.isCritical(bin) ? new Color(220, 38, 38) : new Color(5, 150, 105);
            if (status == StopStatus.COLLECTED) order = "✓";
            else if (status == StopStatus.SKIPPED) order = "×";
            g.setColor(new Color(15, 23, 42, 70)); g.fillOval(x - 9, y - 8, 18, 18);
            g.setColor(color); g.fillOval(x - 9, y - 9, 18, 18);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(order, x - fm.stringWidth(order) / 2, y + 4);
            g.setColor(new Color(15, 23, 42)); g.setFont(new Font("SansSerif", Font.BOLD, 10));
            String caption = bin.getBinID() + " (" + Math.round(bin.getFillPercentage()) + "%)";
            int captionWidth = g.getFontMetrics().stringWidth(caption);
            // Put the label on the left of the pin when there is no room on the right, so it is never cut off by the map edge.
            int captionX = (x + 11 + captionWidth > getWidth() - 4) ? x - 11 - captionWidth : x + 11;
            g.drawString(caption, captionX, y - 8);
        }

        private BufferedImage getTile(int z, int x, int y) {
            String key = z + "/" + x + "/" + y;
            BufferedImage cached = memoryTiles.get(key);
            if (cached != null) return cached;
            Path file = tileRoot.resolve(z + "/" + x + "/" + y + ".png");
            try {
                if (Files.exists(file) && System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() < Duration.ofDays(7).toMillis()) {
                    BufferedImage image = ImageIO.read(file.toFile());
                    if (image != null) { memoryTiles.put(key, image); return image; }
                }
            } catch (IOException ignored) { }
            if (loadingTiles.add(key)) {
                tileExecutor.submit(() -> {
                    try {
                        String url = "https://tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png";
                        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                                .header("User-Agent", "GreenPulse-SmartWaste/3.0 (educational dispatch demonstration)")
                                .GET().build();
                        HttpResponse<byte[]> response = tileClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        if (response.statusCode() == 200) {
                            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
                            if (image != null) {
                                memoryTiles.put(key, image);
                                Files.createDirectories(file.getParent());
                                Files.write(file, response.body());
                            }
                        }
                    } catch (Exception ignored) { }
                    finally {
                        loadingTiles.remove(key);
                        SwingUtilities.invokeLater(this::repaint);
                    }
                });
            }
            return null;
        }

        private Point2D.Double screenPoint(double lat, double lon, double left, double top, double scale) {
            Point2D.Double p = worldPixel(lat, lon, scale);
            return new Point2D.Double(p.x - left, p.y - top);
        }

        private Point2D.Double worldPixel(double lat, double lon, double scale) {
            double x = (lon + 180.0) / 360.0 * scale * TILE_SIZE;
            double latRad = Math.toRadians(Math.max(-85.0511, Math.min(85.0511, lat)));
            double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * scale * TILE_SIZE;
            return new Point2D.Double(x, y);
        }
    }
}
