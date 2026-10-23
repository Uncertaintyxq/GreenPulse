package com.greenpulse.view;

import com.greenpulse.dao.SmartBinDAO;
import com.greenpulse.event.BinEventBus;
import com.greenpulse.event.BinUpdateListener;
import com.greenpulse.event.LogNotificationChannel;
import com.greenpulse.event.ThresholdAlertListener;
import com.greenpulse.model.*;
import com.greenpulse.server.IoTTelemetryServer;
import com.greenpulse.service.BinService;
import com.greenpulse.service.TelemetrySimulatorEngine;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MainDashboardFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardContainer;

    private JTable binTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTextField txtSearch;
    private SmartBinDAO binDAO;
    private List<SmartBin> loadedBins;
    private User currentUser;
    private BinService binService;
    private BinUpdateListener alertListener;
    private String currentCard = "MONITOR";
    private boolean initialLoadDone = false;
    private final AtomicInteger loadSequence = new AtomicInteger();
    private JButton btnCollect, btnRemove;

    private JLabel lblTotalBins, lblCriticalCount, lblNormalCount, lblAvgFill;
    private JPanel toastPanel;
    private JLabel lblToastMessage;

    private AnalyticsPanel analyticsPanel;
    private SimulatePanel simulatePanel;
    private RouteMapPanel routeMapPanel;
    private AddBinPanel addBinPanel;
    private UserProfilePanel profilePanel;

    private List<JButton> sidebarButtons;
    private JButton btnNavMonitor, btnNavAnalytics, btnNavSimulate, btnNavDispatch, btnNavAddBin, btnNavProfile, btnLogout, btnToggleAutoSim;

    public MainDashboardFrame(User user) {
        this.currentUser = user;
        this.binDAO = new SmartBinDAO();
        this.binService = new BinService(user);
        this.loadedBins = new ArrayList<>();
        this.sidebarButtons = new ArrayList<>();

        initUI();
        setupKeyboardShortcuts();
        applyRolePermissions();
        registerAlertListener();
        startIotListenerIfConfigured();
        loadDataAsynchronously();
    }

    /** Shows a toast the first time a bin becomes critical (from the simulator, IoT sensors or a manual reading). */
    private void registerAlertListener() {
        LogNotificationChannel log = new LogNotificationChannel();
        alertListener = new ThresholdAlertListener((subject, message) -> {
            log.send(subject, message);
            SwingUtilities.invokeLater(() -> showToast("⚠ " + message));
        });
        BinEventBus.getInstance().subscribe(alertListener);
    }

    /** Starts the optional sensor endpoint only when an admin has configured an API key in the environment. */
    private void startIotListenerIfConfigured() {
        String key = System.getenv("GREENPULSE_IOT_API_KEY");
        if (key == null || key.isBlank() || currentUser.getRoleType() != Role.ADMIN) return;
        int port = 8090;
        try {
            String configured = System.getenv("GREENPULSE_IOT_PORT");
            if (configured != null && !configured.isBlank()) port = Integer.parseInt(configured.trim());
        } catch (NumberFormatException ignored) { /* keep default port */ }
        try {
            IoTTelemetryServer.startServer(port, key, this::loadDataAsynchronously);
            showToast("📡 IoT sensor listener running on port " + port);
        } catch (Exception ex) {
            showToast("⚠ IoT listener not started: " + ex.getMessage());
        }
    }

    private void initUI() {
        setTitle("GreenPulse Enterprise - Waste Management Console");
        setSize(1320, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));

        JPanel mainLayout = new JPanel(new BorderLayout());

        // Left Vertical Sidebar
        JPanel sidebar = createSidebar();

        // Card Container
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);

        JPanel monitorPanel = createMonitorPanel();
        analyticsPanel = new AnalyticsPanel(loadedBins, () -> switchToTab("MONITOR"));
        simulatePanel = new SimulatePanel(currentUser, loadedBins, () -> switchToTab("MONITOR"));
        routeMapPanel = new RouteMapPanel(currentUser, loadedBins, () -> switchToTab("MONITOR"));
        addBinPanel = new AddBinPanel(currentUser, () -> { txtSearch.setText(""); switchToTab("MONITOR"); });
        profilePanel = new UserProfilePanel(currentUser);

        cardContainer.add(monitorPanel, "MONITOR");
        cardContainer.add(analyticsPanel, "ANALYTICS");
        cardContainer.add(simulatePanel, "SIMULATE");
        cardContainer.add(routeMapPanel, "DISPATCH");
        cardContainer.add(addBinPanel, "ADD");
        cardContainer.add(profilePanel, "PROFILE");

        mainLayout.add(sidebar, BorderLayout.WEST);
        mainLayout.add(cardContainer, BorderLayout.CENTER);

        // Toast Overlay Panel
        toastPanel = createToastOverlay();
        toastPanel.setAlignmentX(1.0f);
        toastPanel.setAlignmentY(0.0f);

        layeredPane.add(toastPanel, Integer.valueOf(2));
        layeredPane.add(mainLayout, Integer.valueOf(1));

        add(layeredPane);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(15, 23, 42)); // Dark Slate
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(30, 41, 59)));

        // App Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblLogo = new JLabel("● GreenPulse");
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblLogo.setForeground(new Color(16, 185, 129));

        JLabel lblSub = new JLabel("ENTERPRISE CONSOLE");
        lblSub.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSub.setForeground(new Color(100, 116, 139));

        brandPanel.add(lblLogo);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(lblSub);

        // Navigation Group
        JPanel navPanel = new JPanel(new GridLayout(7, 1, 0, 6));
        navPanel.setOpaque(false);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        btnNavMonitor = createSidebarButton("📊 Real-Time Monitor", "MONITOR");
        btnNavAnalytics = createSidebarButton("📈 Trend Analytics", "ANALYTICS");
        btnNavSimulate = createSidebarButton("🎛 Sensor Simulation", "SIMULATE");
        btnNavDispatch = createSidebarButton("⚡ GIS Driver Dispatch", "DISPATCH");
        btnNavAddBin = createSidebarButton("➕ Register Smart Bin", "ADD");
        btnNavProfile = createSidebarButton("👤 User Profile", "PROFILE");

        btnToggleAutoSim = new JButton("▶ Start Auto-Sim");
        btnToggleAutoSim.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnToggleAutoSim.setBackground(new Color(30, 41, 59));
        btnToggleAutoSim.setForeground(new Color(226, 232, 240));
        btnToggleAutoSim.setFocusPainted(false);
        btnToggleAutoSim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleAutoSim.addActionListener(e -> toggleAutoSimulation());

        navPanel.add(btnNavMonitor);
        navPanel.add(btnNavAnalytics);
        navPanel.add(btnNavSimulate);
        navPanel.add(btnNavDispatch);
        navPanel.add(btnNavAddBin);
        navPanel.add(btnNavProfile);
        navPanel.add(btnToggleAutoSim);

        // User Footer Card
        JPanel userFooter = new JPanel(new BorderLayout(8, 0));
        userFooter.setBackground(new Color(30, 41, 59));
        userFooter.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblUserInfo = new JLabel("<html><b>" + currentUser.getFullName() + "</b><br><font color='#94a3b8'>" + currentUser.getRole() + "</font></html>");
        lblUserInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblUserInfo.setForeground(Color.WHITE);

        btnLogout = new JButton("🚪 Logout");
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnLogout.setBackground(new Color(225, 29, 72));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> performLogout());

        userFooter.add(lblUserInfo, BorderLayout.CENTER);
        userFooter.add(btnLogout, BorderLayout.EAST);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(userFooter, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createSidebarButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btn.addActionListener(e -> switchToTab(cardName));
        sidebarButtons.add(btn);
        return btn;
    }

    private JPanel createMonitorPanel() {
        JPanel monitorPanel = new JPanel(new BorderLayout(0, 10));
        monitorPanel.setBackground(new Color(241, 245, 249));

        // KPI Summary Header Row
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));

        lblTotalBins = createKPICard(kpiPanel, "TOTAL BINS", "0", new Color(14, 165, 233));
        lblCriticalCount = createKPICard(kpiPanel, "CRITICAL ALERT", "0", new Color(225, 29, 72));
        lblNormalCount = createKPICard(kpiPanel, "NORMAL STATUS", "0", new Color(16, 185, 129));
        lblAvgFill = createKPICard(kpiPanel, "AVG FILL LEVEL", "0%", new Color(245, 158, 11));

        // Filter Controls Bar
        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);
        filterBar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(280, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "🔍 Search bin ID or location...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        JPanel filterGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterGroup.setOpaque(false);

        JButton btnFilterAll = new JButton("All Bins");
        JButton btnFilterCrit = new JButton("Critical (≥" + (int) CollectionPolicy.CRITICAL_FILL + "%)");
        JButton btnFilterRecy = new JButton("Recycling");
        JButton btnFilterComp = new JButton("Compost");

        styleFilterPill(btnFilterAll, true);
        styleFilterPill(btnFilterCrit, false);
        styleFilterPill(btnFilterRecy, false);
        styleFilterPill(btnFilterComp, false);

        btnFilterAll.addActionListener(e -> { tableSorter.setRowFilter(null); highlightFilterPill(btnFilterAll, btnFilterCrit, btnFilterRecy, btnFilterComp); });
        btnFilterCrit.addActionListener(e -> { tableSorter.setRowFilter(RowFilter.regexFilter("CRITICAL", 5)); highlightFilterPill(btnFilterCrit, btnFilterAll, btnFilterRecy, btnFilterComp); });
        btnFilterRecy.addActionListener(e -> { tableSorter.setRowFilter(RowFilter.regexFilter("RecyclingBin", 1)); highlightFilterPill(btnFilterRecy, btnFilterAll, btnFilterCrit, btnFilterComp); });
        btnFilterComp.addActionListener(e -> { tableSorter.setRowFilter(RowFilter.regexFilter("CompostBin", 1)); highlightFilterPill(btnFilterComp, btnFilterAll, btnFilterCrit, btnFilterRecy); });

        btnCollect = new JButton("✔ Mark Collected");
        btnCollect.setToolTipText("Record the selected bin as emptied and reset its fill level to 0%.");
        btnCollect.addActionListener(e -> markSelectedCollected());
        btnRemove = new JButton("🗑 Remove Bin");
        btnRemove.setToolTipText("Remove the selected bin from the system. Its history is kept for reports.");
        btnRemove.addActionListener(e -> removeSelectedBin());
        filterGroup.add(btnCollect);
        filterGroup.add(btnRemove);
        filterGroup.add(Box.createHorizontalStrut(10));
        filterGroup.add(btnFilterAll);
        filterGroup.add(btnFilterCrit);
        filterGroup.add(btnFilterRecy);
        filterGroup.add(btnFilterComp);

        filterBar.add(txtSearch, BorderLayout.WEST);
        filterBar.add(filterGroup, BorderLayout.EAST);

        // Data Table
        String[] columnNames = {"BIN ID", "TYPE", "LOCATION", "FILL LEVEL (%)", "URGENCY PRIORITY", "STATUS", "ACTION"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        binTable = new JTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        binTable.setRowSorter(tableSorter);
        binTable.setRowHeight(38);
        binTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        binTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (binTable.rowAtPoint(e.getPoint()) >= 0 && binTable.columnAtPoint(e.getPoint()) == 6) switchToTab("DISPATCH");
            }
        });
        binTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        binTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        binTable.getColumnModel().getColumn(3).setCellRenderer(new FillBarRenderer());
        binTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        
        // Inline Dispatch Action Button
        binTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton btn = new JButton("⚡ Dispatch");
                btn.setFont(new Font("SansSerif", Font.BOLD, 11));
                btn.setBackground(new Color(16, 185, 129));
                btn.setForeground(Color.WHITE);
                return btn;
            }
        });

        JScrollPane scrollPane = new JScrollPane(binTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JPanel centerContainer = new JPanel(new BorderLayout(0, 8));
        centerContainer.setOpaque(false);
        centerContainer.add(filterBar, BorderLayout.NORTH);
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        monitorPanel.add(kpiPanel, BorderLayout.NORTH);
        monitorPanel.add(centerContainer, BorderLayout.CENTER);

        return monitorPanel;
    }

    private JPanel createToastOverlay() {
        JPanel toast = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        toast.setOpaque(false);

        JPanel innerCard = new JPanel(new BorderLayout(10, 0));
        innerCard.setBackground(new Color(15, 23, 42, 235));
        innerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(16, 185, 129), 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        lblToastMessage = new JLabel("Notification ready.");
        lblToastMessage.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblToastMessage.setForeground(Color.WHITE);

        innerCard.add(lblToastMessage, BorderLayout.CENTER);
        toast.add(innerCard);
        toast.setVisible(false);

        return toast;
    }

    public void showToast(String message) {
        lblToastMessage.setText(message);
        toastPanel.setVisible(true);
        Timer timer = new Timer(3500, e -> toastPanel.setVisible(false));
        timer.setRepeats(false);
        timer.start();
    }

    private void filterTable() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void setupKeyboardShortcuts() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK), "navMonitor");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK), "navAnalytics");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.CTRL_DOWN_MASK), "navSimulate");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.CTRL_DOWN_MASK), "navDispatch");

        root.getActionMap().put("navMonitor", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { switchToTab("MONITOR"); } });
        root.getActionMap().put("navAnalytics", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { switchToTab("ANALYTICS"); } });
        root.getActionMap().put("navSimulate", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { switchToTab("SIMULATE"); } });
        root.getActionMap().put("navDispatch", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { switchToTab("DISPATCH"); } });
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to log out of GreenPulse Console?",
            "Confirm Logout", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            TelemetrySimulatorEngine.stopSimulation();
            IoTTelemetryServer.stopServer();
            BinEventBus.getInstance().unsubscribe(alertListener);
            this.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    private void switchToTab(String cardName) {
        currentCard = cardName;
        cardLayout.show(cardContainer, cardName);

        styleSidebarButton(btnNavMonitor, "MONITOR".equals(cardName));
        styleSidebarButton(btnNavAnalytics, "ANALYTICS".equals(cardName));
        styleSidebarButton(btnNavSimulate, "SIMULATE".equals(cardName));
        styleSidebarButton(btnNavDispatch, "DISPATCH".equals(cardName));
        styleSidebarButton(btnNavAddBin, "ADD".equals(cardName));
        styleSidebarButton(btnNavProfile, "PROFILE".equals(cardName));

        if ("MONITOR".equals(cardName)) loadDataAsynchronously();
        else if ("ANALYTICS".equals(cardName)) analyticsPanel.refreshAnalytics(loadedBins);
        else if ("SIMULATE".equals(cardName)) simulatePanel.updateBinList(loadedBins);
        else if ("DISPATCH".equals(cardName)) routeMapPanel.refreshBins(loadedBins);
    }

    private void styleSidebarButton(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(16, 185, 129));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(15, 23, 42));
            btn.setForeground(new Color(203, 213, 225));
        }
    }

    private void styleFilterPill(JButton btn, boolean active) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        highlightFilterPill(btn, active);
    }

    private void highlightFilterPill(JButton selected, JButton... others) {
        highlightFilterPill(selected, true);
        for (JButton o : others) highlightFilterPill(o, false);
    }

    private void highlightFilterPill(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(16, 185, 129));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(226, 232, 240));
            btn.setForeground(new Color(51, 65, 85));
        }
    }

    private JLabel createKPICard(JPanel parent, String title, String initVal, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(new Color(100, 116, 139));

        JLabel lblVal = new JLabel(initVal);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblVal.setForeground(new Color(30, 41, 59));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        parent.add(card);

        return lblVal;
    }

    private void applyRolePermissions() {
        btnRemove.setVisible(currentUser.getRoleType().canArchiveBins());
        btnCollect.setVisible(currentUser.getRoleType().canRecordCollections());
        if (currentUser.getRoleType() == Role.DRIVER) {
            btnNavSimulate.setVisible(false);
            btnNavAddBin.setVisible(false);
            btnToggleAutoSim.setVisible(false);
            btnNavAnalytics.setVisible(false);
            switchToTab("DISPATCH");
        } else {
            switchToTab("MONITOR");
        }
    }

    private void toggleAutoSimulation() {
        if (TelemetrySimulatorEngine.isRunning()) {
            TelemetrySimulatorEngine.stopSimulation();
            btnToggleAutoSim.setText("▶ Start Auto-Sim");
            btnToggleAutoSim.setBackground(new Color(30, 41, 59));
            showToast("ℹ Telemetry simulation paused.");
        } else {
            TelemetrySimulatorEngine.startSimulation(this::loadDataAsynchronously);
            btnToggleAutoSim.setText("⏹ Stop Auto-Sim");
            btnToggleAutoSim.setBackground(new Color(225, 29, 72));
            showToast("⚡ Simulation active: Waste levels updating every 5s!");
        }
    }

    private void loadDataAsynchronously() {
        final int sequence = loadSequence.incrementAndGet();
        SwingWorker<List<SmartBin>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SmartBin> doInBackground() throws Exception {
                return binDAO.loadAll();
            }

            @Override
            protected void done() {
                if (sequence != loadSequence.get()) return; // a newer refresh is already running; ignore this stale result
                try {
                    loadedBins = get();
                    tableModel.setRowCount(0);
                    int total = loadedBins.size();
                    int critical = 0, normal = 0;
                    double sumFill = 0.0;

                    for (SmartBin bin : loadedBins) {
                        int priority = bin.calculatePriority();
                        double fill = bin.getFillPercentage();
                        sumFill += fill;

                        boolean isCrit = CollectionPolicy.isCritical(bin);
                        if (isCrit) critical++; else normal++;

                        tableModel.addRow(new Object[]{
                            bin.getBinID(),
                            bin.getBinType().getDbValue(),
                            bin.getLocation(),
                            (int) fill,
                            priority + " PRI",
                            isCrit ? "CRITICAL" : "NORMAL",
                            "⚡ Dispatch"
                        });
                    }

                    lblTotalBins.setText(String.valueOf(total));
                    lblCriticalCount.setText(String.valueOf(critical));
                    lblNormalCount.setText(String.valueOf(normal));
                    lblAvgFill.setText(total == 0 ? "—" : Math.round(sumFill / total) + "%");

                    // A driver starts on the dispatch screen before the first load finishes, so hand it the bins now.
                    if (!initialLoadDone) {
                        initialLoadDone = true;
                        if ("DISPATCH".equals(currentCard)) routeMapPanel.refreshBins(loadedBins);
                    }
                } catch (Exception ex) {
                    showToast("⚠ Could not load bins: " + rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private String selectedBinId() {
        int viewRow = binTable.getSelectedRow();
        if (viewRow < 0) {
            showToast("ℹ Select a bin in the table first.");
            return null;
        }
        return (String) tableModel.getValueAt(binTable.convertRowIndexToModel(viewRow), 0);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return (message == null || message.isBlank()) ? cause.getClass().getSimpleName() : message;
    }

    private void markSelectedCollected() {
        String binId = selectedBinId();
        if (binId == null) return;
        if (JOptionPane.showConfirmDialog(this, "Record " + binId + " as emptied?\nIts fill level will be reset to 0%.",
                "Mark as collected", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Optional<Double>, Void>() {
            @Override protected Optional<Double> doInBackground() throws Exception {
                return binService.markCollected(binId, null);
            }
            @Override protected void done() {
                try {
                    Optional<Double> before = get();
                    showToast(before.isPresent()
                            ? String.format(Locale.US, "✔ %s emptied (was %.0f%% full).", binId, before.get())
                            : "ℹ " + binId + " no longer exists.");
                    loadDataAsynchronously();
                } catch (Exception ex) {
                    showToast("⚠ " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private void removeSelectedBin() {
        String binId = selectedBinId();
        if (binId == null) return;
        if (JOptionPane.showConfirmDialog(this,
                "Remove " + binId + " from the system?\nIt will disappear from the dashboard and maps. Its history is kept for reports.",
                "Remove bin", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return binService.archiveBin(binId);
            }
            @Override protected void done() {
                try {
                    showToast(get() ? "🗑 " + binId + " removed." : "ℹ " + binId + " was already removed.");
                    loadDataAsynchronously();
                } catch (Exception ex) {
                    showToast("⚠ " + rootMessage(ex));
                }
            }
        }.execute();
    }
}
