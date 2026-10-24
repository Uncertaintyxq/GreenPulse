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

    private final AnalyticsDataSource dataSource;
    private final AtomicInteger loadSequence = new AtomicInteger();
    private List<SmartBin> currentBins;
    private AnalyticsData history;
    private boolean updatingFilters = false;

    private final javax.swing.Timer autoRefresh;
    private final JComboBox<Period> periodBox = new JComboBox<>(PERIODS);
    private final JComboBox<String> typeBox = new JComboBox<>();
    private final JComboBox<String> locationBox = new JComboBox<>();
    private final JCheckBox chkSimulated = new JCheckBox("Include simulated readings", true);
    private final JLabel lblStatus = Ui.caption(" ");

    private final Metric mBins = new Metric(), mCritical = new Metric(), mAvgFill = new Metric(),
            mCollections = new Metric(), mWait = new Metric();
    private final AnalyticsCharts.LineChart trendChart = new AnalyticsCharts.LineChart();
    private final AnalyticsCharts.BarChart collectionChart = new AnalyticsCharts.BarChart(Theme.OK);
    private final JPanel comparisonGrid = new JPanel(new GridLayout(0, 3, 0, 0));
    private final JLabel lblInsights = Ui.label("", Theme.regular(14), Theme.INK2);
    private final DefaultTableModel forecastModel = new DefaultTableModel(
            new String[]{"Bin", "Type", "Location", "Fill", "Rate", "To " + (int) CollectionPolicy.CRITICAL_FILL + "%", "To 100%", "Trend"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable forecastTable;
    private final DefaultTableModel topModel = new DefaultTableModel(
            new String[]{"Bin", "Type", "Location", "Fill", "Priority", "What is driving it"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == 4 ? Integer.class : String.class; }
    };

    public AnalyticsPanel(List<SmartBin> bins, Runnable onBackAction) {
        this(bins, new AnalyticsDAO());
    }

    public AnalyticsPanel(List<SmartBin> bins, AnalyticsDataSource dataSource) {
        this.currentBins = bins == null ? new ArrayList<>() : new ArrayList<>(bins);
        this.dataSource = dataSource;
        setLayout(new BorderLayout(0, Theme.S5));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.S6, 40, Theme.S4, 40));
        buildUi();
        rebuildFilterChoices();
        render();

        // Keep the page current while it is on screen (the simulator and sensors keep adding readings).
        autoRefresh = new javax.swing.Timer(15000, e -> { if (isShowing()) reload(); });
        autoRefresh.setRepeats(true);
        autoRefresh.start();
        Theme.onThemeChange(this, this::render); // insight text and table colours are rebuilt in the new theme
    }

    /** Stops the refresh timer when the window closes (for example on sign-out). */
    @Override public void removeNotify() {
        autoRefresh.stop();
        super.removeNotify();
    }

    @Override public void addNotify() {
        super.addNotify();
        if (autoRefresh != null && !autoRefresh.isRunning()) autoRefresh.start();
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
        JButton refresh = Ui.secondary("Refresh", Icons.Glyph.REFRESH);
        refresh.addActionListener(e -> reload());
        chkSimulated.setOpaque(false);
        chkSimulated.setFont(Theme.regular(14));
        Theme.fg(chkSimulated, Theme.INK2);
        chkSimulated.setToolTipText("Simulator readings are demo data. Untick to see only real sensor, manual and collection readings.");
        periodBox.setSelectedIndex(1);
        typeBox.setPrototypeDisplayValue(ALL_TYPES);
        locationBox.setPrototypeDisplayValue("Central Canteen - East Wing");
        for (JComboBox<?> box : new JComboBox<?>[]{periodBox, typeBox, locationBox}) {
            box.setFont(Theme.regular(14));
            box.setPreferredSize(new Dimension(box.getPreferredSize().width + 8, 38));
        }
        periodBox.addActionListener(e -> { if (!updatingFilters) reload(); });
        chkSimulated.addActionListener(e -> reload());
        typeBox.addActionListener(e -> { if (!updatingFilters) render(); });
        locationBox.addActionListener(e -> { if (!updatingFilters) render(); });

        JPanel filters = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filters.add(periodBox); filters.add(Box.createHorizontalStrut(Theme.S2));
        filters.add(typeBox); filters.add(Box.createHorizontalStrut(Theme.S2));
        filters.add(locationBox); filters.add(Box.createHorizontalStrut(Theme.S4));
        filters.add(chkSimulated); filters.add(Box.createHorizontalStrut(Theme.S4));
        filters.add(refresh);

        JPanel top = Ui.transparent(new BorderLayout(0, Theme.S4));
        top.add(Ui.pageHeader("Fleet analytics", "Fill trends, recycling vs compost, collections, and the bins that need attention.", null), BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Figure strip: five headline numbers on one plate, divided by hairlines.
        JPanel figures = new Ui.Plate(new GridLayout(1, 5), 0);
        figures.add(figure("Bins in view", mBins, false));
        figures.add(figure("Critical now", mCritical, true));
        figures.add(figure("Average fill", mAvgFill, true));
        figures.add(figure("Collections", mCollections, true));
        figures.add(figure("Time at critical", mWait, true));

        JPanel comparisonCard = card("Recycling vs compost", "Current state of the bins in view", comparisonGrid);
        lblInsights.setVerticalAlignment(SwingConstants.TOP);
        JPanel insightsCard = card("What stands out", "Generated from the data on this page", lblInsights);

        JTable table = new JTable(topModel);
        table.setRowHeight(38);
        table.setPreferredScrollableViewportSize(new Dimension(520, 5 * 38));
        table.putClientProperty(Ui.RIGHT_ALIGNED_COLUMNS, new int[]{4});
        JScrollPane tableScroll = Ui.tableScroll(table);
        int[] widths = {130, 90, 160, 60, 80, 260};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(0).setCellRenderer(new Renderers.BinCode());
        table.getColumnModel().getColumn(4).setCellRenderer(new Renderers.Priority());
        table.getColumnModel().getColumn(5).setCellRenderer(new Renderers.Text(Theme.INK2, false));

        forecastTable = new JTable(forecastModel);
        forecastTable.setRowHeight(38);
        forecastTable.setPreferredScrollableViewportSize(new Dimension(520, 6 * 38));
        JScrollPane forecastScroll = Ui.tableScroll(forecastTable);
        int[] forecastWidths = {130, 80, 120, 50, 80, 76, 76, 90};
        for (int i = 0; i < forecastWidths.length; i++) forecastTable.getColumnModel().getColumn(i).setPreferredWidth(forecastWidths[i]);
        forecastTable.getColumnModel().getColumn(0).setCellRenderer(new Renderers.BinCode());
        javax.swing.table.DefaultTableCellRenderer urgencyRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                String text = String.valueOf(v);
                boolean now = "now".equals(text);
                boolean soon = text.startsWith("~") && (text.endsWith(" s") || text.endsWith(" min") || (text.endsWith(" h") && parseLeadingNumber(text) <= 2.0));
                setFont(now || soon ? Theme.monoBold(14) : Theme.mono(14));
                setForeground(Theme.color(now ? Theme.CRITICAL_TEXT : soon ? Theme.WARNING_TEXT : Theme.INK));
                setBackground(Renderers.cellBackground(t, sel));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        };
        forecastTable.getColumnModel().getColumn(5).setCellRenderer(urgencyRenderer);

        JPanel body = new Ui.ScrollBody(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int gap = Theme.S4;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, gap, 0);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        body.add(figures, c);
        c.gridwidth = 1; c.gridy = 1; c.weighty = 1;
        c.gridx = 0; c.weightx = 0.6; c.insets = new Insets(0, 0, gap, gap);
        body.add(card("Fill level over time", "Average fill of the bins in view, by bin type", trendChart), c);
        c.gridx = 1; c.weightx = 0.4; c.insets = new Insets(0, 0, gap, 0);
        body.add(comparisonCard, c);
        c.gridy = 2; c.gridx = 0; c.weightx = 0.4; c.insets = new Insets(0, 0, gap, gap);
        body.add(card("Collections", "Bins emptied in the selected period", collectionChart), c);
        c.gridx = 1; c.weightx = 0.6; c.insets = new Insets(0, 0, gap, 0);
        body.add(card("Needs attention", "Top 5 bins by priority score right now", tableScroll), c);
        c.gridy = 3; c.gridx = 0; c.weightx = 0.6; c.insets = new Insets(0, 0, 0, gap);
        body.add(card("Forecast: when will bins reach the critical level?",
                "Trend since each bin was last emptied, based on readings in the selected period", forecastScroll), c);
        c.gridx = 1; c.weightx = 0.4; c.insets = new Insets(0, 0, 0, 0);
        body.add(insightsCard, c);

        JScrollPane scroll = Ui.scroll(body);
        add(scroll, BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);
    }

    private static final class Metric {
        final JLabel value = Ui.label("—", Theme.heavy(30), Theme.INK);
        final JLabel caption = Ui.caption(" ");
    }

    private JPanel figure(String title, Metric metric, boolean dividerLeft) {
        JPanel cell = Ui.column(Ui.fieldLabel(title), Box.createVerticalStrut(Theme.S1), metric.value, Box.createVerticalStrut(2), metric.caption);
        cell.setBorder(BorderFactory.createCompoundBorder(
                dividerLeft ? Ui.hairline(Theme.LINE, 0, 1, 0, 0) : BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(Theme.S4, Theme.S5, Theme.S4, Theme.S4)));
        return cell;
    }

    private JPanel card(String title, String subtitle, JComponent content) {
        JPanel card = new Ui.Plate(new BorderLayout(0, Theme.S4), Theme.S5);
        card.add(Ui.plateHeader(title, subtitle), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------ data loading

    private void reload() {
        final int sequence = loadSequence.incrementAndGet();
        final Period period = (Period) periodBox.getSelectedItem();
        final boolean includeSimulated = chkSimulated.isSelected();
        Theme.fg(lblStatus, Theme.INK3);
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
                    Theme.fg(lblStatus, Theme.CRITICAL_TEXT);
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
        Theme.fg(mCritical.value, critical > 0 ? Theme.CRITICAL_TEXT : Theme.INK);
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
                    type == BinType.RECYCLING ? Theme.RECYCLING : Theme.COMPOST, values));
        }
        if (series.size() > 1) series.add(new AnalyticsCharts.LineChart.Series("Fleet average", Theme.FLEET, trend.fleet()));
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
        Theme.fg(lblStatus, Theme.INK3);
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
        String[] cells = {label, recycling, compost};
        for (int i = 0; i < cells.length; i++) {
            JLabel l;
            if (header) l = Ui.label(cells[i], Theme.bold(14), i == 1 ? Theme.RECYCLING : i == 2 ? Theme.COMPOST : Theme.INK2);
            else if (i == 0) l = Ui.label(cells[i], Theme.regular(14), Theme.INK2);
            else l = Ui.label(cells[i], Theme.mono(14), Theme.INK);
            if (i > 0) l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setBorder(BorderFactory.createCompoundBorder(Ui.hairline(header ? Theme.LINE : Theme.LINE2, 0, 0, 1, 0),
                    BorderFactory.createEmptyBorder(Theme.S2, i == 0 ? 0 : Theme.S3, Theme.S2, i == 0 ? 0 : 3)));
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
        for (String line : lines) sb.append("<p style='margin:0 0 10px 0'>").append(line).append("</p>");
        lblInsights.setText(html(sb.toString()));
    }

    // ------------------------------------------------------------------ helpers

    private static String html(String inner) {
        return "<html><body style='width:300px'>" + inner + "</body></html>";
    }

    private static String pct(Double v) { return v == null || v.isNaN() ? "—" : String.format(Locale.US, "%.0f%%", v); }

    private static String num(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.0f", v); }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
