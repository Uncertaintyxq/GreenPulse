package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small chart components drawn with Java2D, so the analytics page needs no charting library.
 * Colours are theme tokens read at paint time, so the charts follow light and dark mode.
 */
final class AnalyticsCharts {
    private AnalyticsCharts() {}

    private static Font small() { return Theme.mono(12); }
    private static Font legend() { return Theme.semibold(13); }

    private static String timeLabel(long epoch, long spanSeconds) {
        DateTimeFormatter fmt = spanSeconds <= 36 * 3600L
                ? DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                : DateTimeFormatter.ofPattern("MMM d", Locale.US);
        return fmt.format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()));
    }

    private static void prepare(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void drawXAxis(Graphics2D g, Rectangle plot, long start, long end) {
        g.setFont(small());
        g.setColor(Theme.color(Theme.INK3));
        FontMetrics fm = g.getFontMetrics();
        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            long epoch = start + (end - start) * i / ticks;
            String label = timeLabel(epoch, end - start);
            int x = plot.x + plot.width * i / ticks - fm.stringWidth(label) / 2;
            x = Math.max(plot.x - 10, Math.min(x, plot.x + plot.width - fm.stringWidth(label) + 10));
            g.drawString(label, x, plot.y + plot.height + 20);
        }
    }

    private static void drawCentered(Graphics2D g, Rectangle plot, String message) {
        g.setFont(Theme.regular(14));
        g.setColor(Theme.color(Theme.INK3));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(message, plot.x + (plot.width - fm.stringWidth(message)) / 2, plot.y + plot.height / 2);
    }

    static final class LineChart extends JComponent {
        /** One line: its legend name, a colour token and one value per time bucket (NaN where there is no data). */
        record Series(String name, String colorKey, double[] values) {}

        private List<Series> series = List.of();
        private long start, end;
        private String emptyMessage = "";

        LineChart() { setPreferredSize(new Dimension(520, 270)); setOpaque(false); }

        void setData(List<Series> series, long startEpoch, long endEpoch, String emptyMessage) {
            this.series = series; this.start = startEpoch; this.end = endEpoch; this.emptyMessage = emptyMessage;
            repaint();
        }

        private static double lastValue(double[] values) {
            for (int i = values.length - 1; i >= 0; i--) if (!Double.isNaN(values[i])) return values[i];
            return Double.NaN;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            prepare(g);
            // Lay the legend out first: it wraps when the chart is too narrow, and the plot starts below it.
            g.setFont(legend());
            FontMetrics lfm = g.getFontMetrics();
            List<String> legendText = new ArrayList<>();
            List<Point> legendAt = new ArrayList<>();
            int lx = 44, ly = 15;
            for (Series s : series) {
                double last = lastValue(s.values());
                String text = s.name() + (Double.isNaN(last) ? "" : String.format(Locale.US, "  now %.0f%%", last));
                int w = 20 + lfm.stringWidth(text);
                if (lx > 44 && lx + w > getWidth() - 16) { lx = 44; ly += 20; }
                legendText.add(text);
                legendAt.add(new Point(lx, ly));
                lx += w + 24;
            }
            int top = ly + 21;
            Rectangle plot = new Rectangle(44, top, getWidth() - 44 - 16, getHeight() - top - 30);
            if (plot.width < 20 || plot.height < 20) { g.dispose(); return; }

            g.setFont(small());
            FontMetrics fm = g.getFontMetrics();
            for (int v = 0; v <= 100; v += 25) {
                int y = plot.y + plot.height - (int) Math.round(v / 100.0 * plot.height);
                g.setColor(Theme.color(v == 0 ? Theme.LINE : Theme.LINE2));
                g.drawLine(plot.x, y, plot.x + plot.width, y);
                g.setColor(Theme.color(Theme.INK3));
                String label = v + "%";
                g.drawString(label, plot.x - 8 - fm.stringWidth(label), y + 4);
            }
            int critY = plot.y + plot.height - (int) Math.round(CollectionPolicy.CRITICAL_FILL / 100.0 * plot.height);
            g.setColor(Theme.color(Theme.CRITICAL_TEXT));
            g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
            g.drawLine(plot.x, critY, plot.x + plot.width, critY);
            g.setStroke(new BasicStroke(1f));
            drawXAxis(g, plot, start, end);

            boolean any = false;
            for (int si = 0; si < series.size(); si++) {
                Series s = series.get(si);
                Color color = Theme.color(s.colorKey());
                double[] v = s.values();
                Path2D.Double path = new Path2D.Double();
                boolean pen = false;
                int lastX = 0, lastY = 0; double lastValue = Double.NaN;
                for (int i = 0; i < v.length; i++) {
                    if (Double.isNaN(v[i])) { pen = false; continue; }
                    double x = plot.x + (v.length == 1 ? 0 : plot.width * (double) i / (v.length - 1));
                    double y = plot.y + plot.height - Math.max(0, Math.min(100, v[i])) / 100.0 * plot.height;
                    if (!pen) { path.moveTo(x, y); pen = true; } else { path.lineTo(x, y); }
                    lastX = (int) Math.round(x); lastY = (int) Math.round(y); lastValue = v[i];
                    any = true;
                }
                if (!Double.isNaN(lastValue)) {
                    g.setColor(color);
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(path);
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(Theme.color(Theme.SURFACE));
                    g.fillOval(lastX - 6, lastY - 6, 12, 12);
                    g.setColor(color);
                    g.fillOval(lastX - 4, lastY - 4, 8, 8);
                }
                Point at = legendAt.get(si);
                g.setColor(color);
                g.fillRoundRect(at.x, at.y - 7, 14, 4, 2, 2);
                g.setFont(legend());
                g.setColor(Theme.color(Theme.INK2));
                g.drawString(legendText.get(si), at.x + 20, at.y);
            }
            g.setFont(Theme.semibold(12));
            g.setColor(Theme.color(Theme.CRITICAL_TEXT));
            String critLabel = (int) CollectionPolicy.CRITICAL_FILL + "% critical";
            g.drawString(critLabel, plot.x + plot.width - g.getFontMetrics().stringWidth(critLabel), critY - 6);
            if (!any) drawCentered(g, plot, emptyMessage);
            g.dispose();
        }
    }

    static final class BarChart extends JComponent {
        private int[] counts = new int[0];
        private long start, end;
        private String emptyMessage = "";
        private final String barColorKey;

        BarChart(String barColorKey) { this.barColorKey = barColorKey; setPreferredSize(new Dimension(380, 240)); setOpaque(false); }

        void setData(int[] counts, long startEpoch, long endEpoch, String emptyMessage) {
            this.counts = counts; this.start = startEpoch; this.end = endEpoch; this.emptyMessage = emptyMessage;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            prepare(g);
            Rectangle plot = new Rectangle(38, 16, getWidth() - 38 - 14, getHeight() - 16 - 30);
            if (plot.width < 20 || plot.height < 20) { g.dispose(); return; }

            int max = 0;
            for (int c : counts) max = Math.max(max, c);
            int step = Math.max(1, (int) Math.ceil(Math.max(max, 4) / 4.0));
            int top = step * 4;

            g.setFont(small());
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = plot.y + plot.height - (int) Math.round(i / 4.0 * plot.height);
                g.setColor(Theme.color(i == 0 ? Theme.LINE : Theme.LINE2));
                g.drawLine(plot.x, y, plot.x + plot.width, y);
                g.setColor(Theme.color(Theme.INK3));
                String label = String.valueOf(i * step);
                g.drawString(label, plot.x - 8 - fm.stringWidth(label), y + 4);
            }
            drawXAxis(g, plot, start, end);

            if (max == 0) { drawCentered(g, plot, emptyMessage); g.dispose(); return; }
            int n = counts.length;
            double slot = (double) plot.width / n;
            int barWidth = (int) Math.max(3, slot * 0.6);
            for (int i = 0; i < n; i++) {
                if (counts[i] == 0) continue;
                int h = (int) Math.round((double) counts[i] / top * plot.height);
                int x = plot.x + (int) Math.round(slot * i + (slot - barWidth) / 2);
                g.setColor(Theme.color(barColorKey));
                g.fill(new RoundRectangle2D.Double(x, plot.y + plot.height - h, barWidth, h + 3, 4, 4));
                g.setColor(Theme.color(Theme.INK2));
                g.setFont(Theme.monoBold(12));
                String label = String.valueOf(counts[i]);
                g.drawString(label, x + (barWidth - g.getFontMetrics().stringWidth(label)) / 2, plot.y + plot.height - h - 5);
                g.setFont(small());
            }
            g.setColor(Theme.color(Theme.LINE));
            g.drawLine(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height);
            g.dispose();
        }
    }
}
