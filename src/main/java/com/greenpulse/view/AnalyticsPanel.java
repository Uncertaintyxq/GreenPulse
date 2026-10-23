package com.greenpulse.view;

import com.greenpulse.analytics.AnalyticsCalculator;
import com.greenpulse.analytics.AnalyticsCalculator.CollectionStats;
import com.greenpulse.analytics.AnalyticsCalculator.TrendSeries;
import com.greenpulse.analytics.AnalyticsCalculator.TypeSummary;
import com.greenpulse.analytics.AnalyticsCalculator.WaitStats;
import com.greenpulse.analytics.AnalyticsData;
import com.greenpulse.analytics.AnalyticsDataSource;
import com.greenpulse.analytics.FillForecaster;
import com.greenpulse.dao.AnalyticsDAO;
import com.greenpulse.model.BinType;
import com.greenpulse.model.CollectionPolicy;
import com.greenpulse.model.SmartBin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Fleet analytics: how fill levels have changed over time, how recycling and compost bins compare, how
 * collections are going, and which bins need attention now. History comes from the telemetry log and the
 * collection events; everything can be filtered by period, bin type and location.
 */
public class AnalyticsPanel extends JPanel {
    private record Period(String label, long seconds) {
        @Override public String toString() { return label; }
    }

    private static final Period[] PERIODS = {
            new Period("Last hour", 3600), new Period("Last 24 hours", 86400),
            new Period("Last 7 days", 7 * 86400L), new Period("Last 30 days", 30 * 86400L)};
    private static final String ALL_TYPES = "All bin types";
    private static final String ALL_LOCATIONS = "All locations";
    private static final int TREND_BUCKETS = 48;
    private static final int COLLECTION_BUCKETS = 12;
    private static final Color RECYCLING_COLOR = new Color(14, 165, 233);
    private static final Color COMPOST_COLOR = new Color(217, 119, 6);
    private static final Color FLEET_COLOR = new Color(71, 85, 105);
    private static final Color GREEN = new Color(5, 150, 105);

    private final AnalyticsDataSource dataSource;
    private final AtomicInteger loadSequence = new AtomicInteger();
    private List<SmartBin> currentBins;
    private AnalyticsData history;
    private boolean updatingFilters = false;

    private final JComboBox<Period> periodBox = new JComboBox<>(PERIODS);
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JComboBox<String> locationBox = new JComboBox<>();
    private final JCheckBox chkSimulated = new JCheckBox("Include simulated readings", true);
    private final JLabel lblStatus = new JLabel(" ");

    private final Metric mBins = new Metric(), mCritical = new Metric(), mAvgFill = new Metric(),
            mCollections = new Metric(), mWait = new Metric();
    private final AnalyticsCharts.LineChart trendChart = new AnalyticsCharts.LineChart();
    private final AnalyticsCharts.BarChart collectionChart = new AnalyticsCharts.BarChart(GREEN);
    private final JPanel comparisonGrid = new JPanel(new GridLayout(0, 3, 6, 6));
    private final JLabel lblInsights = new JLabel();
    private final DefaultTableModel forecastModel = new DefaultTableModel(
            new String[]{"BIN", "TYPE", "LOCATION", "FILL", "RATE", "TO " + (int) CollectionPolicy.CRITICAL_FILL + "%", "TO 100%", "TREND"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable forecastTable;
    private final DefaultTableModel topModel = new DefaultTableModel(
            new String[]{"BIN", "TYPE", "LOCATION", "FILL", "PRIORITY", "WHAT IS DRIVING IT"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public AnalyticsPanel(List<SmartBin> bins, Runnable onBackAction) {
        this(bins, new AnalyticsDAO());
    }

    public AnalyticsPanel(List<SmartBin> bins, AnalyticsDataSource dataSource) {
        this.currentBins = bins == null ? new ArrayList<>() : new ArrayList<>(bins);
        this.dataSource = dataSource;
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(241, 245, 249));
        setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));
        buildUi();
        rebuildFilterChoices();
        render();

        // Keep the page current while it is on screen (the simulator and sensors keep adding readings).
        javax.swing.Timer autoRefresh = new javax.swing.Timer(15000, e -> { if (isShowing()) reload(); });
        autoRefresh.setRepeats(true);
        autoRefresh.start();
    }

    /** Called when the page is opened: shows the bins already loaded, then fetches fresh history. */
    public void refreshAnalytics(List<SmartBin> freshBins) {
        if (freshBins != null) this.currentBins = new ArrayList<>(freshBins);
        rebuildFilterChoices();
        render();
        reload();
    }

    // ------------------------------------------------------------------ layout

    private void buildUi() {
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("REPORTING");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        eyebrow.setForeground(GREEN);
        JLabel title = new JLabel("Fleet analytics");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(new Color(24, 39, 55));
        JLabel sub = new JLabel("Fill trends, recycling vs compost, collections, and the bins that need attention.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(100, 116, 139));
        heading.add(eyebrow); heading.add(Box.createVerticalStrut(4)); heading.add(title);
        heading.add(Box.createVerticalStrut(4)); heading.add(sub);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JButton refresh = new JButton("↻ Refresh");
        refresh.addActionListener(e -> reload());
        chkSimulated.setOpaque(false);
        chkSimulated.setToolTipText("Simulator readings are demo data. Untick to see only real sensor, manual and collection readings.");
        periodBox.setSelectedIndex(1);
        typeBox.setPrototypeDisplayValue(ALL_TYPES);
        locationBox.setPrototypeDisplayValue("Central Canteen - East Wing");
        periodBox.addActionListener(e -> { if (!updatingFilters) reload(); });
        chkSimulated.addActionListener(e -> reload());
        typeBox.addActionListener(e -> { if (!updatingFilters) render(); });
        locationBox.addActionListener(e -> { if (!updatingFilters) render(); });
        filters.add(periodBox); filters.add(typeBox); filters.add(locationBox);
        filters.add(chkSimulated); filters.add(refresh);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // KPI row
        JPanel kpis = new JPanel(new GridLayout(1, 5, 12, 0));
        kpis.setOpaque(false);
        kpis.add(metricCard("BINS IN VIEW", mBins, new Color(14, 165, 233)));
        kpis.add(metricCard("CRITICAL NOW", mCritical, new Color(225, 29, 72)));
        kpis.add(metricCard("AVERAGE FILL", mAvgFill, new Color(245, 158, 11)));
        kpis.add(metricCard("COLLECTIONS", mCollections, GREEN));
        kpis.add(metricCard("TIME AT CRITICAL", mWait, new Color(124, 58, 237)));

        // Row 2: trend chart | comparison + insights
        JPanel comparisonCard = card("Recycling vs compost", "Current state of the bins in view", comparisonGrid);
        lblInsights.setVerticalAlignment(SwingConstants.TOP);
        JPanel insightsCard = card("What stands out", "Generated from the data on this page", lblInsights);

        // Row 3: collections | top bins table
        JTable table = new JTable(topModel);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(520, 5 * 26));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        int[] widths = {80, 90, 170, 55, 70, 260};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        DefaultTableCellRenderer priorityRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                boolean critical = v instanceof Integer p && p > CollectionPolicy.CRITICAL_PRIORITY;
                comp.setFont(comp.getFont().deriveFont(critical ? Font.BOLD : Font.PLAIN));
                if (!sel) comp.setForeground(critical ? new Color(190, 18, 60) : new Color(30, 41, 59));
                setHorizontalAlignment(SwingConstants.CENTER);
                return comp;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(priorityRenderer);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.getViewport().setBackground(Color.WHITE);

        forecastTable = new JTable(forecastModel);
        forecastTable.setRowHeight(26);
        forecastTable.setShowGrid(false);
        forecastTable.setIntercellSpacing(new Dimension(0, 0));
        forecastTable.setFillsViewportHeight(true);
        forecastTable.setPreferredScrollableViewportSize(new Dimension(520, 6 * 26));
        forecastTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        int[] forecastWidths = {70, 70, 110, 42, 68, 66, 66, 76};
        for (int i = 0; i < forecastWidths.length; i++) forecastTable.getColumnModel().getColumn(i).setPreferredWidth(forecastWidths[i]);
        DefaultTableCellRenderer urgencyRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String text = String.valueOf(v);
                boolean now = "now".equals(text);
                boolean soon = text.startsWith("~") && (text.endsWith(" s") || text.endsWith(" min") || (text.endsWith(" h") && parseLeadingNumber(text) <= 2.0));
                comp.setFont(comp.getFont().deriveFont(now || soon ? Font.BOLD : Font.PLAIN));
                if (!sel) comp.setForeground(now ? new Color(190, 18, 60) : soon ? new Color(180, 83, 9) : new Color(30, 41, 59));
                return comp;
            }
        };
        forecastTable.getColumnModel().getColumn(5).setCellRenderer(urgencyRenderer);
        JScrollPane forecastScroll = new JScrollPane(forecastTable);
        forecastScroll.setBorder(BorderFactory.createEmptyBorder());
        forecastScroll.getViewport().setBackground(Color.WHITE);
        forecastScroll.setColumnHeaderView(forecastTable.getTableHeader());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 12, 12);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        body.add(kpis, c);
        c.gridwidth = 1; c.gridy = 1; c.weighty = 1;
        c.gridx = 0; c.weightx = 0.6;
        body.add(card("Fill level over time", "Average fill of the bins in view, by bin type", trendChart), c);
        c.gridx = 1; c.weightx = 0.4; c.insets = new Insets(0, 0, 12, 0);
        body.add(comparisonCard, c);
        c.gridy = 2; c.gridx = 0; c.weightx = 0.4; c.insets = new Insets(0, 0, 0, 12);
        body.add(card("Collections", "Bins emptied in the selected period", collectionChart), c);
        c.gridx = 1; c.weightx = 0.6; c.insets = new Insets(0, 0, 0, 0);
        body.add(card("Needs attention", "Top 5 bins by priority score right now", tableScroll), c);
        c.gridy = 3; c.gridx = 0; c.weightx = 0.6; c.insets = new Insets(12, 0, 0, 12);
        body.add(card("Forecast: when will bins hit the critical level?",
                "Trend since each bin was last emptied, based on readings in the selected period", forecastScroll), c);
        c.gridx = 1; c.weightx = 0.4; c.insets = new Insets(12, 0, 0, 0);
        body.add(insightsCard, c);

        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(100, 116, 139));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private static final class Metric {
        final JLabel value = new JLabel("—");
        final JLabel caption = new JLabel(" ");
    }

    private JPanel metricCard(String title, Metric metric, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 14, 12, 12)));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(new Color(100, 116, 139));
        metric.value.setFont(new Font("SansSerif", Font.BOLD, 21));
        metric.value.setForeground(new Color(30, 41, 59));
        metric.caption.setFont(new Font("SansSerif", Font.PLAIN, 10));
        metric.caption.setForeground(new Color(100, 116, 139));
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(metric.value, BorderLayout.CENTER);
        card.add(metric.caption, BorderLayout.SOUTH);
        return card;
    }

    private JPanel card(String title, String subtitle, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 14));
        t.setForeground(new Color(15, 23, 42));
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 11));
        s.setForeground(new Color(100, 116, 139));
        header.add(t);
        header.add(s);
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------ data loading

    private void reload() {
        final int sequence = loadSequence.incrementAndGet();
        final Period period = (Period) periodBox.getSelectedItem();
        final boolean includeSimulated = chkSimulated.isSelected();
        lblStatus.setForeground(new Color(100, 116, 139));
        lblStatus.setText("Loading " + period.label.toLowerCase(Locale.ROOT) + "…");
        new SwingWorker<AnalyticsData, Void>() {
            @Override protected AnalyticsData doInBackground() throws Exception {
                return dataSource.load(period.seconds, includeSimulated);
            }
            @Override protected void done() {
                if (sequence != loadSequence.get()) return; // a newer request is already running
                try {
                    history = get();
                    currentBins = new ArrayList<>(history.bins());
                    rebuildFilterChoices();
                    render();
                } catch (Exception ex) {
                    Throwable cause = ex;
                    while (cause.getCause() != null) cause = cause.getCause();
                    history = null;
                    render();
                    lblStatus.setForeground(new Color(190, 18, 60));
                    lblStatus.setText("Could not load history: " + (cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()));
                }
            }
        }.execute();
    }

    private void rebuildFilterChoices() {
        updatingFilters = true;
        try {
            Object keepType = typeBox.getSelectedItem();
            typeBox.removeAllItems();
            typeBox.addItem(ALL_TYPES);
            for (BinType t : BinType.values()) typeBox.addItem(t.getLabel());
            if (keepType != null) typeBox.setSelectedItem(keepType);

            Object keepLocation = locationBox.getSelectedItem();
            locationBox.removeAllItems();
            locationBox.addItem(ALL_LOCATIONS);
            currentBins.stream().map(SmartBin::getLocation).distinct().sorted().forEach(locationBox::addItem);
            if (keepLocation != null) locationBox.setSelectedItem(keepLocation);
            if (locationBox.getSelectedItem() == null) locationBox.setSelectedIndex(0);
        } finally {
            updatingFilters = false;
        }
    }

    // ------------------------------------------------------------------ rendering

    private List<SmartBin> selectedBins() {
        Object type = typeBox.getSelectedItem();
        Object location = locationBox.getSelectedItem();
        return currentBins.stream()
                .filter(b -> type == null || ALL_TYPES.equals(type) || b.getBinType() == BinType.fromString((String) type))
                .filter(b -> location == null || ALL_LOCATIONS.equals(location) || b.getLocation().equals(location))
                .collect(Collectors.toList());
    }

    private void render() {
        List<SmartBin> selected = selectedBins();
        Map<String, BinType> selectedTypes = new LinkedHashMap<>();
        for (SmartBin b : selected) selectedTypes.put(b.getBinID(), b.getBinType());
        Set<String> ids = selectedTypes.keySet();

        // Current state
        int critical = (int) selected.stream().filter(CollectionPolicy::isCritical).count();
        double avgFill = selected.stream().mapToDouble(SmartBin::getFillPercentage).average().orElse(Double.NaN);
        mBins.value.setText(String.valueOf(selected.size()));
        mBins.caption.setText(selected.size() == currentBins.size() ? "all active bins" : "of " + currentBins.size() + " active bins");
        mCritical.value.setText(String.valueOf(critical));
        mCritical.caption.setText("≥ " + (int) CollectionPolicy.CRITICAL_FILL + "% full or priority > " + CollectionPolicy.CRITICAL_PRIORITY);
        mAvgFill.value.setText(Double.isNaN(avgFill) ? "—" : String.format(Locale.US, "%.1f%%", avgFill));
        mAvgFill.caption.setText("right now");

        renderComparison(selected);
        renderTopBins(selected);

        // History
        if (history == null) {
            mCollections.value.setText("—"); mCollections.caption.setText("history not loaded");
            mWait.value.setText("—"); mWait.caption.setText("history not loaded");
            trendChart.setData(List.of(), 0, 1, "History is not available yet.");
            collectionChart.setData(new int[COLLECTION_BUCKETS], 0, 1, "History is not available yet.");
            forecastModel.setRowCount(0);
            lblInsights.setText(html("<p>History is not available yet. Current-state figures are still shown.</p>"));
            return;
        }

        TrendSeries trend = AnalyticsCalculator.trend(history, selectedTypes, TREND_BUCKETS);
        List<AnalyticsCharts.LineChart.Series> series = new ArrayList<>();
        for (BinType type : BinType.values()) {
            double[] values = trend.byType().get(type);
            if (values != null) series.add(new AnalyticsCharts.LineChart.Series(type.getLabel(),
                    type == BinType.RECYCLING ? RECYCLING_COLOR : COMPOST_COLOR, values));
        }
        if (series.size() > 1) series.add(new AnalyticsCharts.LineChart.Series("Fleet average", FLEET_COLOR, trend.fleet()));
        trendChart.setData(series, history.sinceEpoch(), history.nowEpoch(),
                "No readings in this period yet. Start the simulator or wait for sensor data.");

        CollectionStats collections = AnalyticsCalculator.collectionStats(history, ids);
        int[] perBucket = AnalyticsCalculator.collectionsPerBucket(history, ids, COLLECTION_BUCKETS);
        collectionChart.setData(perBucket, history.sinceEpoch(), history.nowEpoch(),
                "No collections recorded in this period.");
        mCollections.value.setText(String.valueOf(collections.count()));
        mCollections.caption.setText(collections.count() == 0 ? "none in this period"
                : String.format(Locale.US, "avg %.0f%% full at pickup", collections.avgFillBefore()));

        WaitStats waits = AnalyticsCalculator.criticalWaits(history, ids);
        mWait.value.setText(waits.completed() == 0 ? "—" : AnalyticsCalculator.formatDuration(waits.avgCompletedSeconds()));
        mWait.caption.setText(waits.stillWaiting() == 0
                ? (waits.completed() == 0 ? "no critical episodes" : "avg until collected")
                : waits.stillWaiting() + " waiting, longest " + AnalyticsCalculator.formatDuration(waits.longestWaitingSeconds()));

        List<FillForecaster.Entry> forecasts = FillForecaster.rank(history, selected);
        renderForecast(forecasts);
        renderInsights(selected, collections, waits, forecasts);

        long readings = history.points().stream().filter(p -> ids.contains(p.binId())).count();
        lblStatus.setForeground(new Color(100, 116, 139));
        lblStatus.setText(String.format(Locale.US,
                "%d readings analysed (%s) • updated %s • updates automatically while this page is open",
                readings, chkSimulated.isSelected() ? "includes simulated data" : "simulated data excluded",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
    }

    private void renderComparison(List<SmartBin> selected) {
        Map<BinType, TypeSummary> s = AnalyticsCalculator.summarise(selected);
        TypeSummary rec = s.get(BinType.RECYCLING), com = s.get(BinType.COMPOST);
        comparisonGrid.removeAll();
        comparisonGrid.setOpaque(false);
        addGridRow("", "Recycling", "Compost", true);
        addGridRow("Bins", String.valueOf(rec.bins()), String.valueOf(com.bins()), false);
        addGridRow("Critical now", String.valueOf(rec.critical()), String.valueOf(com.critical()), false);
        addGridRow("Average fill", pct(rec.avgFill()), pct(com.avgFill()), false);
        addGridRow("Average priority", num(rec.avgPriority()), num(com.avgPriority()), false);
        addGridRow("Contamination", pct(rec.avgContamination()), "n/a", false);
        addGridRow("Temperature", "n/a", com.avgTemperature() == null ? "—" : String.format(Locale.US, "%.1f °C", com.avgTemperature()), false);
        addGridRow("Moisture", "n/a", pct(com.avgMoisture()), false);
        comparisonGrid.revalidate();
        comparisonGrid.repaint();
    }

    private void addGridRow(String label, String recycling, String compost, boolean header) {
        for (String text : new String[]{label, recycling, compost}) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("SansSerif", header ? Font.BOLD : Font.PLAIN, 12));
            l.setForeground(header ? (text.equals("Recycling") ? RECYCLING_COLOR : COMPOST_COLOR) : new Color(51, 65, 85));
            if (text.equals(label) && !header) l.setForeground(new Color(100, 116, 139));
            if (text != label) l.setHorizontalAlignment(SwingConstants.RIGHT);
            comparisonGrid.add(l);
        }
    }

    private void renderTopBins(List<SmartBin> selected) {
        topModel.setRowCount(0);
        for (SmartBin bin : AnalyticsCalculator.topByPriority(selected, 5)) {
            topModel.addRow(new Object[]{bin.getBinID(), bin.getBinType().getLabel(), bin.getLocation(),
                    Math.round(bin.getFillPercentage()) + "%", bin.calculatePriority(),
                    AnalyticsCalculator.describeDrivers(bin)});
        }
    }

    private void renderForecast(List<FillForecaster.Entry> forecasts) {
        forecastModel.setRowCount(0);
        int shown = 0;
        for (FillForecaster.Entry e : forecasts) {
            if (shown++ == 6) break;
            FillForecaster.Forecast f = e.forecast();
            forecastModel.addRow(new Object[]{e.bin().getBinID(), e.bin().getBinType().getLabel(), e.bin().getLocation(),
                    Math.round(e.bin().getFillPercentage()) + "%", FillForecaster.describeRate(f),
                    FillForecaster.describeTimeToCritical(f), FillForecaster.describeTimeToFull(f),
                    FillForecaster.describeConfidence(f)});
        }
    }

    private static double parseLeadingNumber(String text) {
        try {
            return Double.parseDouble(text.substring(1).split(" ")[0]);
        } catch (RuntimeException ex) {
            return Double.MAX_VALUE;
        }
    }

    private void renderInsights(List<SmartBin> selected, CollectionStats collections, WaitStats waits,
                                List<FillForecaster.Entry> forecasts) {
        List<String> lines = new ArrayList<>();
        if (selected.isEmpty()) {
            lines.add("No bins match the current filters.");
        } else {
            Map<BinType, TypeSummary> s = AnalyticsCalculator.summarise(selected);
            TypeSummary rec = s.get(BinType.RECYCLING), com = s.get(BinType.COMPOST);
            if (rec.bins() > 0 && com.bins() > 0) {
                boolean compostHigher = com.avgFill() > rec.avgFill();
                lines.add(String.format(Locale.US, "%s bins are fuller on average (%.0f%% vs %.0f%%).",
                        compostHigher ? "Compost" : "Recycling",
                        Math.max(com.avgFill(), rec.avgFill()), Math.min(com.avgFill(), rec.avgFill())));
            }
            SmartBin worst = AnalyticsCalculator.topByPriority(selected, 1).get(0);
            lines.add("Highest priority: <b>" + escape(worst.getBinID()) + "</b> (" + worst.calculatePriority() + "/100) — "
                    + escape(AnalyticsCalculator.describeDrivers(worst)) + ".");
            if (com.bins() > 0 && com.avgTemperature() != null && com.avgTemperature() >= 45) {
                lines.add(String.format(Locale.US, "Compost bins are running warm (avg %.0f °C), which raises their priority.", com.avgTemperature()));
            }
            if (rec.bins() > 0 && rec.avgContamination() != null && rec.avgContamination() >= 25) {
                lines.add(String.format(Locale.US, "Recycling contamination is high (avg %.0f%%); consider signage or a clean-up.", rec.avgContamination()));
            }
        }
        if (collections.count() > 0) {
            if (collections.avgFillBefore() >= 95) {
                lines.add(String.format(Locale.US, "Bins are collected almost full (avg %.0f%%), so overflow is likely; dispatch earlier.", collections.avgFillBefore()));
            } else if (collections.avgFillBefore() < 50) {
                lines.add(String.format(Locale.US, "Bins are collected at only %.0f%% full on average; some trips may be avoidable.", collections.avgFillBefore()));
            }
        } else {
            lines.add("No collections have been recorded in this period, so waiting times cannot be measured.");
        }
        List<FillForecaster.Entry> rising = forecasts.stream()
                .filter(e -> e.forecast().status() == FillForecaster.Status.RISING).toList();
        if (!rising.isEmpty()) {
            FillForecaster.Entry next = rising.get(0);
            long within2h = rising.stream().filter(e -> e.forecast().secondsToCritical() <= 2 * 3600).count();
            lines.add("Next to reach " + (int) CollectionPolicy.CRITICAL_FILL + "%: <b>" + escape(next.bin().getBinID()) + "</b> in "
                    + FillForecaster.describeTimeToCritical(next.forecast()) + " ("
                    + FillForecaster.describeRate(next.forecast()) + "). " + within2h + " bin(s) are forecast to reach it within 2 hours.");
        } else if (!forecasts.isEmpty() && forecasts.stream().noneMatch(e -> e.forecast().status() == FillForecaster.Status.CRITICAL_NOW)) {
            lines.add("No bin is on course to become critical in the near term (or there are too few readings to tell).");
        }
        if (waits.stillWaiting() > 0) {
            lines.add(waits.stillWaiting() + " bin(s) are still waiting at critical level (longest "
                    + AnalyticsCalculator.formatDuration(waits.longestWaitingSeconds()) + ").");
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) sb.append("<p style='margin:0 0 6px 0'>• ").append(line).append("</p>");
        lblInsights.setText(html(sb.toString()));
    }

    // ------------------------------------------------------------------ helpers

    private static String html(String inner) {
        return "<html><body style='width:230px;color:#475569;font-size:11px'>" + inner + "</body></html>";
    }

    private static String pct(Double v) { return v == null || v.isNaN() ? "—" : String.format(Locale.US, "%.0f%%", v); }

    private static String num(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.0f", v); }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
