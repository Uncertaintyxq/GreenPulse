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
import com.greenpulse.view.Icons.Glyph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MainDashboardFrame extends JFrame {
    private static final int COL_ID = 0, COL_TYPE = 1, COL_LOCATION = 2, COL_FILL = 3, COL_PRIORITY = 4, COL_STATUS = 5, COL_ROUTE = 6;
    static final String STATUS_CRITICAL = "CRITICAL", STATUS_DUE = "DUE", STATUS_NORMAL = "NORMAL";

    private CardLayout cardLayout;
    private JPanel cardContainer;

    private JTable binTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTextField txtSearch;
    private Ui.Segmented filterControl;
    private SmartBinDAO binDAO;
    private List<SmartBin> loadedBins;
    private User currentUser;
    private BinService binService;
    private BinUpdateListener alertListener;
    private String currentCard = "MONITOR";
    private boolean initialLoadDone = false;
    private final AtomicInteger loadSequence = new AtomicInteger();
    private JButton btnCollect, btnRemove;
    private Component removeGap;

    private JLabel lblTotalBins, lblCriticalCount, lblDueCount, lblAvgFill, lblUpdated, lblNextNote;
    private JButton btnNextAction;
    private FillLine fillLine;
    private JPanel toastPanel;
    private JLabel lblToastMessage;
    private Timer toastTimer;

    private AnalyticsPanel analyticsPanel;
    private SimulatePanel simulatePanel;
    private RouteMapPanel routeMapPanel;
    private AddBinPanel addBinPanel;
    private UserProfilePanel profilePanel;

    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private JButton btnLogout, btnToggleAutoSim;

    public MainDashboardFrame(User user) {
        this.currentUser = user;
        this.binDAO = new SmartBinDAO();
        this.binService = new BinService(user);
        this.loadedBins = new ArrayList<>();

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
            SwingUtilities.invokeLater(() -> showToast(Glyph.WARNING, message));
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
            showToast(Glyph.INFO, "IoT sensor listener running on port " + port + ".");
        } catch (Exception ex) {
            showToast(Glyph.WARNING, "IoT listener not started: " + ex.getMessage());
        }
    }

    private void initUI() {
        setTitle("GreenPulse");
        setSize(1360, 840);
        setMinimumSize(new Dimension(1240, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.setOpaque(false);

        cardLayout = new CardLayout();
        cardContainer = new Ui.Ground(cardLayout);

        JPanel monitorPanel = createMonitorPanel();
        // Administrators get the detailed operator view; staff and drivers get the plain-language summary.
        analyticsPanel = new AnalyticsPanel(loadedBins, () -> switchToTab("DISPATCH"), currentUser.getRoleType() != Role.ADMIN);
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

        mainLayout.add(createSidebar(), BorderLayout.WEST);
        mainLayout.add(cardContainer, BorderLayout.CENTER);

        toastPanel = createToastOverlay();
        toastPanel.setAlignmentX(1.0f);
        toastPanel.setAlignmentY(0.0f);

        layeredPane.add(toastPanel, Integer.valueOf(2));
        layeredPane.add(mainLayout, Integer.valueOf(1));

        add(layeredPane);
    }

    // ------------------------------------------------------------------ sidebar

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, Theme.S5));
        Theme.bg(sidebar, Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(Theme.LINE, 0, 0, 0, 1), new EmptyBorder(Theme.S5, Theme.S4, Theme.S4, Theme.S4)));

        JPanel brand = Ui.transparent(null);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setBorder(new EmptyBorder(0, Theme.S1, Theme.S2, 0));
        brand.add(new Ui.LogoMark(30));
        brand.add(Box.createHorizontalStrut(10));
        brand.add(Ui.label("GreenPulse", Theme.display(18), Theme.INK));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        addNav(nav, "MONITOR", "Monitor", Glyph.MONITOR, 1);
        addNav(nav, "ANALYTICS", "Analytics", Glyph.ANALYTICS, 2);
        addNav(nav, "SIMULATE", "Sensor simulation", Glyph.SIMULATE, 3);
        addNav(nav, "DISPATCH", "Dispatch", Glyph.DISPATCH, 4);
        addNav(nav, "ADD", "Register bin", Glyph.REGISTER, 5);
        addNav(nav, "PROFILE", "Profile", Glyph.PROFILE, 6);

        btnToggleAutoSim = new JButton();
        btnToggleAutoSim.setFont(Theme.medium(14));
        btnToggleAutoSim.setHorizontalAlignment(SwingConstants.LEFT);
        btnToggleAutoSim.setIconTextGap(Theme.S3);
        btnToggleAutoSim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleAutoSim.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnToggleAutoSim.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnToggleAutoSim.setToolTipText("Simulated sensors add a reading for every bin every 5 seconds.");
        btnToggleAutoSim.addActionListener(e -> toggleAutoSimulation());
        Theme.onThemeChange(btnToggleAutoSim, () -> styleAutoSimButton(TelemetrySimulatorEngine.isRunning()));
        nav.add(Box.createVerticalStrut(Theme.S5));
        nav.add(btnToggleAutoSim);

        JPanel userFooter = new JPanel(new BorderLayout(Theme.S3, 0));
        userFooter.setOpaque(false);
        userFooter.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(Theme.LINE, 1, 0, 0, 0), new EmptyBorder(Theme.S4, Theme.S1, 0, 0)));
        JPanel who = Ui.column(
                Ui.label(currentUser.getFullName(), Theme.medium(14), Theme.INK),
                Box.createVerticalStrut(1),
                Ui.label(roleName(currentUser.getRoleType()), Theme.regular(13), Theme.INK3));
        JPanel whoRow = Ui.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        whoRow.add(who, c);
        btnLogout = new JButton(Icons.of(Glyph.LOGOUT, 18));
        btnLogout.setToolTipText("Sign out");
        btnLogout.getAccessibleContext().setAccessibleName("Sign out");
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.putClientProperty("JButton.buttonType", "borderless");
        Theme.onThemeChange(btnLogout, () -> btnLogout.putClientProperty("FlatLaf.style", "foreground: " + Ui.hex(Theme.color(Theme.INK3))
                + "; hoverBackground: " + Ui.hex(Theme.color(Theme.SURFACE2)) + "; pressedBackground: " + Ui.hex(Theme.color(Theme.LINE2))));
        btnLogout.addActionListener(e -> performLogout());
        userFooter.add(new Ui.Avatar(currentUser.getFullName(), 34), BorderLayout.WEST);
        userFooter.add(whoRow, BorderLayout.CENTER);
        userFooter.add(btnLogout, BorderLayout.EAST);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(userFooter, BorderLayout.SOUTH);
        return sidebar;
    }

    private static String roleName(Role role) {
        return switch (role) {
            case ADMIN -> "Administrator";
            case STAFF -> "Staff";
            case DRIVER -> "Driver";
        };
    }

    private void addNav(JPanel nav, String card, String text, Glyph glyph, int shortcut) {
        NavButton btn = new NavButton(text, glyph);
        String key = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac") ? "⌘" : "Ctrl+";
        btn.setToolTipText(text + "  (" + key + shortcut + ")");
        btn.addActionListener(e -> switchToTab(card));
        navButtons.put(card, btn);
        nav.add(btn);
        nav.add(Box.createVerticalStrut(2));
    }

    /** A sidebar link in quiet ink; the current page sits on a soft green chip (the reserved marking). */
    private static final class NavButton extends JButton {
        private boolean active;

        NavButton(String text, Glyph glyph) {
            super(text, Icons.of(glyph, 18));
            setFont(Theme.medium(14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(Theme.S3);
            setBorder(new EmptyBorder(9, 12, 9, 12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            Theme.onThemeChange(this, this::applyColors);
        }

        void setActive(boolean active) {
            this.active = active;
            getAccessibleContext().setAccessibleDescription(active ? "Current page" : null);
            applyColors();
            repaint();
        }

        private void applyColors() { setForeground(Theme.color(active ? Theme.ACCENT_TEXT : Theme.INK2)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || getModel().isRollover()) {
                g.setColor(Theme.color(active ? Theme.ACCENT_SOFT : Theme.SURFACE2));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
            }
            if (isFocusOwner()) {
                g.setColor(Theme.color(Theme.ACCENT));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(0.75, 0.75, getWidth() - 1.5, getHeight() - 1.5, 10, 10));
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private void styleAutoSimButton(boolean running) {
        btnToggleAutoSim.setText(running ? "Stop auto-simulation" : "Start auto-simulation");
        btnToggleAutoSim.setIcon(Icons.of(running ? Glyph.STOP : Glyph.PLAY, 16));
        btnToggleAutoSim.putClientProperty("FlatLaf.style", running
                ? "background: " + Ui.hex(Theme.color(Theme.ACCENT_SOFT)) + "; foreground: " + Ui.hex(Theme.color(Theme.ACCENT_TEXT))
                  + "; borderColor: " + Ui.hex(Theme.color(Theme.ACCENT_TEXT), 0x55) + "; hoverBackground: " + Ui.hex(Theme.color(Theme.ACCENT_SOFT))
                  + "; hoverBorderColor: " + Ui.hex(Theme.color(Theme.ACCENT_TEXT)) + "; margin: 9,12,9,12"
                : "foreground: " + Ui.hex(Theme.color(Theme.INK)) + "; margin: 9,12,9,12");
    }

    // ------------------------------------------------------------------ monitor page

    private JPanel createMonitorPanel() {
        JPanel page = Ui.transparent(new BorderLayout(0, Theme.S5));
        page.setBorder(new EmptyBorder(Theme.S6, 40, Theme.S6 - Ui.SHADOW, 40));

        lblUpdated = Ui.subtitle("Live fill levels for every active bin.");
        JPanel header = Ui.column(Ui.title("Monitor"), Box.createVerticalStrut(Theme.S1), lblUpdated);

        JPanel top = Ui.transparent(new BorderLayout(0, Theme.S5));
        top.add(header, BorderLayout.NORTH);
        top.add(createFleetSheet(), BorderLayout.CENTER);
        top.add(createToolbar(), BorderLayout.SOUTH);

        page.add(top, BorderLayout.NORTH);
        page.add(createBinTablePlate(), BorderLayout.CENTER);
        return page;
    }

    /** The fleet sheet: four counts and the next move, over every bin on one fill line. */
    private JComponent createFleetSheet() {
        JPanel sheet = new Ui.Plate(new BorderLayout(0, Theme.S4), Theme.S5);

        JPanel counts = Ui.transparent(new GridLayout(1, 4, Theme.S6, 0));
        lblTotalBins = fleetCount(counts, "Active bins");
        lblCriticalCount = fleetCount(counts, "Critical");
        lblDueCount = fleetCount(counts, "Due for pickup");
        lblAvgFill = fleetCount(counts, "Average fill");

        btnNextAction = Ui.primary("Loading bins…", Glyph.ARROW_RIGHT);
        btnNextAction.setFont(Theme.semibold(14));
        btnNextAction.setHorizontalTextPosition(SwingConstants.LEFT);
        btnNextAction.setIconTextGap(Theme.S3);
        btnNextAction.setToolTipText("Open Dispatch");
        btnNextAction.addActionListener(e -> switchToTab("DISPATCH"));
        lblNextNote = Ui.label("No bins need collection", Theme.medium(14), Theme.OK_TEXT);
        lblNextNote.setIconTextGap(Theme.S2);
        lblNextNote.setVisible(false);
        JPanel next = Ui.transparent(new GridBagLayout());
        next.add(btnNextAction);
        next.add(lblNextNote);

        JPanel top = Ui.transparent(new BorderLayout(Theme.S5, 0));
        top.add(counts, BorderLayout.WEST);
        top.add(next, BorderLayout.EAST);

        fillLine = new FillLine();
        fillLine.onPick(this::revealBin);

        sheet.add(top, BorderLayout.NORTH);
        sheet.add(fillLine, BorderLayout.CENTER);
        return sheet;
    }

    private JLabel fleetCount(JPanel parent, String caption) {
        JLabel value = Ui.label("—", Theme.display(30), Theme.INK);
        parent.add(Ui.column(Ui.label(caption, Theme.medium(13), Theme.INK3), Box.createVerticalStrut(2), value));
        return value;
    }

    /** The sheet's next move: a green button when bins need collecting, otherwise one calm line. */
    private void showNextAction(String action) {
        btnNextAction.setText(action);
        btnNextAction.getAccessibleContext().setAccessibleName(action);
        btnNextAction.setVisible(true);
        lblNextNote.setVisible(false);
    }

    private void showNextNote(String note, boolean problem) {
        lblNextNote.setText(note);
        Theme.fg(lblNextNote, problem ? Theme.CRITICAL_TEXT : Theme.OK_TEXT);
        lblNextNote.setIcon(Icons.of(problem ? Glyph.WARNING : Glyph.CHECK, 18));
        lblNextNote.setVisible(true);
        btnNextAction.setVisible(false);
    }

    /** Selects a bin picked on the fill line, clearing the search and filter first if they hide it. */
    private void revealBin(String binId) {
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            if (!binId.equals(tableModel.getValueAt(r, COL_ID))) continue;
            if (binTable.convertRowIndexToView(r) < 0) { txtSearch.setText(""); filterControl.select(0); filterTable(); }
            int view = binTable.convertRowIndexToView(r);
            if (view >= 0) {
                binTable.setRowSelectionInterval(view, view);
                binTable.scrollRectToVisible(binTable.getCellRect(view, 0, true));
                binTable.requestFocusInWindow();
            }
            return;
        }
    }

    private JComponent createToolbar() {
        txtSearch = Ui.field(new JTextField(), "Search bin ID, type or location");
        txtSearch.putClientProperty("JTextField.leadingIcon", Icons.of(Glyph.SEARCH, 16, Theme.INK3));
        txtSearch.putClientProperty("JTextField.showClearButton", true);
        txtSearch.setMinimumSize(new Dimension(170, 40));
        txtSearch.setPreferredSize(new Dimension(280, 40));
        txtSearch.setMaximumSize(new Dimension(340, 40));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        filterControl = new Ui.Segmented("All", "Critical", "Due", "Recycling", "Compost");
        filterControl.button(1).setToolTipText("At or above " + (int) CollectionPolicy.CRITICAL_FILL + "% full, or priority above " + CollectionPolicy.CRITICAL_PRIORITY);
        filterControl.button(2).setToolTipText("At or above " + (int) CollectionPolicy.ROUTE_MIN_FILL + "% full or priority " + CollectionPolicy.ROUTE_MIN_PRIORITY + ": included in the next route");
        filterControl.onSelect(i -> filterTable());

        btnCollect = Ui.secondary("Mark collected", Glyph.CHECK);
        btnCollect.setToolTipText("Record the selected bin as emptied and reset its fill level to 0%.");
        btnCollect.addActionListener(e -> markSelectedCollected());
        btnRemove = Ui.danger("Remove bin", Glyph.TRASH);
        btnRemove.setToolTipText("Remove the selected bin from the system. Its history is kept for reports.");
        btnRemove.addActionListener(e -> removeSelectedBin());

        JPanel left = Ui.transparent(null);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(txtSearch);
        left.add(Box.createHorizontalStrut(Theme.S3));
        left.add(filterControl);
        left.add(Box.createHorizontalGlue());
        JPanel right = Ui.transparent(null);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btnCollect);
        removeGap = Box.createHorizontalStrut(Theme.S5); // keep the destructive action away from the everyday one
        right.add(removeGap);
        right.add(btnRemove);

        JPanel bar = Ui.transparent(new BorderLayout(Theme.S3, 0));
        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent createBinTablePlate() {
        tableModel = new DefaultTableModel(new String[]{"Bin", "Type", "Location", "Fill", "Priority", "Status", ""}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == COL_FILL || c == COL_PRIORITY ? Integer.class : String.class; }
        };

        binTable = new EmptyStateTable(tableModel);
        tableSorter = new TableRowSorter<>(tableModel);
        binTable.setRowSorter(tableSorter);
        binTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        binTable.setRowHeight(48);
        binTable.getSelectionModel().addListSelectionListener(e -> updateSelectionActions());
        binTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (binTable.rowAtPoint(e.getPoint()) >= 0 && binTable.columnAtPoint(e.getPoint()) == COL_ROUTE) switchToTab("DISPATCH");
            }
        });
        binTable.putClientProperty(Ui.RIGHT_ALIGNED_COLUMNS, new int[]{COL_PRIORITY});
        JScrollPane scroll = Ui.tableScroll(binTable);
        binTable.getColumnModel().getColumn(COL_ID).setCellRenderer(new Renderers.BinCode());
        binTable.getColumnModel().getColumn(COL_FILL).setCellRenderer(new FillBarRenderer());
        binTable.getColumnModel().getColumn(COL_PRIORITY).setCellRenderer(new Renderers.Priority());
        binTable.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusBadgeRenderer());
        binTable.getColumnModel().getColumn(COL_ROUTE).setCellRenderer(new Renderers.Link("Open Dispatch to plan a route"));
        int[] widths = {150, 110, 320, 200, 90, 120, 56};
        for (int i = 0; i < widths.length; i++) binTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        binTable.getColumnModel().getColumn(COL_ROUTE).setMaxWidth(64);

        JPanel plate = new Ui.Plate(new BorderLayout(), 1);
        plate.add(scroll);
        return plate;
    }

    /** A table that explains itself when it has no rows instead of showing an empty white box. */
    private final class EmptyStateTable extends JTable {
        EmptyStateTable(DefaultTableModel model) { super(model); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (getRowCount() > 0) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String head, sub;
            if (!initialLoadDone) { head = "Loading bins…"; sub = "Reading the latest fill levels from the database."; }
            else if (tableModel.getRowCount() == 0) {
                head = "No bins registered yet";
                sub = currentUser.getRoleType().canRegisterBins() ? "Open Register bin to add the first one." : "An administrator or staff member can register bins.";
            } else { head = "No bins match this filter"; sub = "Clear the search or choose All."; }
            g.setFont(Theme.semibold(16));
            g.setColor(Theme.color(Theme.INK));
            FontMetrics fm = g.getFontMetrics();
            int y = Math.max(90, getVisibleRect().height / 2 - 10);
            g.drawString(head, (getWidth() - fm.stringWidth(head)) / 2, y);
            g.setFont(Theme.regular(14));
            g.setColor(Theme.color(Theme.INK3));
            fm = g.getFontMetrics();
            g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, y + 26);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ toast

    private JPanel createToastOverlay() {
        JPanel toast = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S5, Theme.S5));
        toast.setOpaque(false);

        JPanel innerCard = new JPanel(new BorderLayout(Theme.S3, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UIManager.getColor("ToolTip.background"));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g.dispose();
            }
        };
        innerCard.setOpaque(false);
        innerCard.setBorder(new EmptyBorder(12, 16, 12, 18));

        lblToastMessage = new JLabel("Notification ready.");
        lblToastMessage.setFont(Theme.medium(14));
        lblToastMessage.setIconTextGap(Theme.S3);
        Theme.onThemeChange(lblToastMessage, () -> lblToastMessage.setForeground(UIManager.getColor("ToolTip.foreground")));

        innerCard.add(lblToastMessage, BorderLayout.CENTER);
        toast.add(innerCard);
        toast.setVisible(false);

        toastTimer = new Timer(4000, e -> toastPanel.setVisible(false));
        toastTimer.setRepeats(false);
        return toast;
    }

    public void showToast(String message) { showToast(Glyph.INFO, message); }

    private void showToast(Glyph glyph, String message) {
        lblToastMessage.setForeground(UIManager.getColor("ToolTip.foreground"));
        lblToastMessage.setIcon(Icons.of(glyph, 18));
        lblToastMessage.setText(message);
        toastPanel.setVisible(true);
        toastPanel.revalidate();
        toastTimer.restart(); // a new message gets its full time on screen
    }

    // ------------------------------------------------------------------ behaviour

    /** Search (literal text, any case) and the filter control narrow the table together. */
    private void filterTable() {
        final String query = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        final int filter = filterControl.selectedIndex();
        tableSorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                String status = e.getStringValue(COL_STATUS), type = e.getStringValue(COL_TYPE);
                boolean passesFilter = switch (filter) {
                    case 1 -> STATUS_CRITICAL.equals(status);
                    case 2 -> STATUS_DUE.equals(status);
                    case 3 -> BinType.RECYCLING.getLabel().equals(type);
                    case 4 -> BinType.COMPOST.getLabel().equals(type);
                    default -> true;
                };
                if (!passesFilter) return false;
                if (query.isEmpty()) return true;
                return e.getStringValue(COL_ID).toLowerCase(Locale.ROOT).contains(query)
                        || type.toLowerCase(Locale.ROOT).contains(query)
                        || e.getStringValue(COL_LOCATION).toLowerCase(Locale.ROOT).contains(query);
            }
        });
        binTable.repaint();
    }

    private void setupKeyboardShortcuts() {
        JRootPane root = getRootPane();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        String[] cards = navButtons.keySet().toArray(new String[0]);
        for (int i = 0; i < cards.length; i++) {
            final String card = cards[i];
            String action = "nav" + card;
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, mask), action);
            root.getActionMap().put(action, new AbstractAction() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (navButtons.get(card).isVisible()) switchToTab(card); // pages hidden for this role stay closed
                }
            });
        }
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Sign out of GreenPulse?",
            "Sign out", JOptionPane.YES_NO_OPTION);

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
        navButtons.forEach((card, btn) -> btn.setActive(card.equals(cardName)));

        if ("MONITOR".equals(cardName)) loadDataAsynchronously();
        else if ("ANALYTICS".equals(cardName)) analyticsPanel.refreshAnalytics(loadedBins);
        else if ("SIMULATE".equals(cardName)) simulatePanel.updateBinList(loadedBins);
        else if ("DISPATCH".equals(cardName)) routeMapPanel.refreshBins(loadedBins);
    }

    private void applyRolePermissions() {
        btnRemove.setVisible(currentUser.getRoleType().canArchiveBins());
        removeGap.setVisible(btnRemove.isVisible());
        updateSelectionActions();
        btnCollect.setVisible(currentUser.getRoleType().canRecordCollections());
        if (currentUser.getRoleType() == Role.DRIVER) {
            navButtons.get("SIMULATE").setVisible(false);
            navButtons.get("ADD").setVisible(false);
            btnToggleAutoSim.setVisible(false);
            switchToTab("DISPATCH");
        } else {
            switchToTab("MONITOR");
        }
    }

    private void toggleAutoSimulation() {
        if (TelemetrySimulatorEngine.isRunning()) {
            TelemetrySimulatorEngine.stopSimulation();
            styleAutoSimButton(false);
            showToast(Glyph.STOP, "Telemetry simulation paused.");
        } else {
            TelemetrySimulatorEngine.startSimulation(this::loadDataAsynchronously);
            styleAutoSimButton(true);
            showToast(Glyph.PLAY, "Simulation running: fill levels update every 5 seconds.");
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
                    applyBins(get());
                } catch (Exception ex) {
                    initialLoadDone = true;
                    showNextNote("Bins could not be loaded", true);
                    showToast(Glyph.WARNING, "Could not load bins: " + rootMessage(ex));
                }
                binTable.repaint();
            }
        };
        worker.execute();
    }

    /** Fills the table, the fleet counts and the fill line from a fresh list of bins. */
    void applyBins(List<SmartBin> bins) {
        loadedBins = bins;
        String selected = selectedBinIdQuietly();
        tableModel.setRowCount(0);
        int total = loadedBins.size();
        int critical = 0, due = 0;
        double sumFill = 0.0;
        List<FillLine.Dot> dots = new ArrayList<>();

        for (SmartBin bin : loadedBins) {
            int priority = bin.calculatePriority();
            double fill = bin.getFillPercentage();
            sumFill += fill;

            String status = CollectionPolicy.isCritical(bin) ? STATUS_CRITICAL
                    : CollectionPolicy.needsRoute(bin) ? STATUS_DUE : STATUS_NORMAL;
            if (STATUS_CRITICAL.equals(status)) critical++;
            else if (STATUS_DUE.equals(status)) due++;

            tableModel.addRow(new Object[]{
                bin.getBinID(),
                bin.getBinType().getLabel(),
                bin.getLocation(),
                (int) fill,
                priority,
                status,
                "Dispatch"
            });
            dots.add(new FillLine.Dot(bin.getBinID(), fill, status));
        }
        reselect(selected);

        lblTotalBins.setText(String.valueOf(total));
        lblCriticalCount.setText(String.valueOf(critical));
        lblDueCount.setText(String.valueOf(due));
        Theme.fg(lblCriticalCount, critical > 0 ? Theme.CRITICAL_TEXT : Theme.INK);
        Theme.fg(lblDueCount, due > 0 ? Theme.WARNING_TEXT : Theme.INK);
        lblAvgFill.setText(total == 0 ? "—" : Math.round(sumFill / total) + "%");
        lblUpdated.setText("Live fill levels for every active bin · updated " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        fillLine.setBins(dots);
        if (critical > 0) showNextAction("Dispatch " + critical + " critical bin" + (critical == 1 ? "" : "s"));
        else if (due > 0) showNextAction("Route " + due + " bin" + (due == 1 ? "" : "s") + " due for pickup");
        else showNextNote(total == 0 ? "No bins registered yet" : "No bins need collection", false);

        // A driver starts on the dispatch screen before the first load finishes, so hand it the bins now.
        if (!initialLoadDone) {
            initialLoadDone = true;
            if ("DISPATCH".equals(currentCard)) routeMapPanel.refreshBins(loadedBins);
        }
        binTable.repaint();
    }

    /** Keeps the user's selected row selected when the simulator refreshes the table every few seconds. */
    private String selectedBinIdQuietly() {
        int viewRow = binTable.getSelectedRow();
        return viewRow < 0 ? null : (String) tableModel.getValueAt(binTable.convertRowIndexToModel(viewRow), COL_ID);
    }

    private void reselect(String binId) {
        if (binId == null) return;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            if (binId.equals(tableModel.getValueAt(r, COL_ID))) {
                int view = binTable.convertRowIndexToView(r);
                if (view >= 0) binTable.setRowSelectionInterval(view, view);
                return;
            }
        }
    }

    /** Mark collected and Remove bin act on the selected row, so they are only available while one is selected. */
    private void updateSelectionActions() {
        boolean selected = binTable.getSelectedRow() >= 0;
        btnCollect.setEnabled(selected);
        btnRemove.setEnabled(selected);
        String hint = selected ? null : "Select a bin in the table first.";
        btnCollect.setToolTipText(selected ? "Record the selected bin as emptied and reset its fill level to 0%." : hint);
        btnRemove.setToolTipText(selected ? "Remove the selected bin from the system. Its history is kept for reports." : hint);
    }

    private String selectedBinId() {
        String id = selectedBinIdQuietly();
        if (id == null) showToast(Glyph.INFO, "Select a bin in the table first.");
        return id;
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
                    if (before.isPresent()) showToast(Glyph.CHECK, String.format(Locale.US, "%s emptied (was %.0f%% full).", binId, before.get()));
                    else showToast(Glyph.INFO, binId + " no longer exists.");
                    loadDataAsynchronously();
                } catch (Exception ex) {
                    showToast(Glyph.WARNING, rootMessage(ex));
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
                    if (get()) showToast(Glyph.TRASH, binId + " removed.");
                    else showToast(Glyph.INFO, binId + " was already removed.");
                    loadDataAsynchronously();
                } catch (Exception ex) {
                    showToast(Glyph.WARNING, rootMessage(ex));
                }
            }
        }.execute();
    }
}
