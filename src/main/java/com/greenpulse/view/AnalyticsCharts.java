package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Small chart components drawn with Java2D, so the analytics page needs no charting library. */
final class AnalyticsCharts {
    private AnalyticsCharts() {}

    private static final Color GRID = new Color(226, 232, 240);
    private static final Color AXIS_TEXT = new Color(100, 116, 139);
    private static final Color CRITICAL = new Color(225, 29, 72);
    private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font LEGEND = new Font("SansSerif", Font.BOLD, 11);

    private static String timeLabel(long epoch, long spanSeconds) {
        DateTimeFormatter fmt = spanSeconds <= 36 * 3600L
                ? DateTimeFormatter.ofPattern("HH:mm", Locale.US)
                : DateTimeFormatter.ofPattern("MMM d", Locale.US);
        return fmt.format(Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()));
    }

    private static void drawXAxis(Graphics2D g, Rectangle plot, long start, long end) {
        g.setFont(SMALL);
        g.setColor(AXIS_TEXT);
        FontMetrics fm = g.getFontMetrics();
        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            long epoch = start + (end - start) * i / ticks;
            String label = timeLabel(epoch, end - start);
            int x = plot.x + plot.width * i / ticks - fm.stringWidth(label) / 2;
            x = Math.max(plot.x - 10, Math.min(x, plot.x + plot.width - fm.stringWidth(label) + 10));
            g.drawString(label, x, plot.y + plot.height + 16);
        }
    }

    private static void drawCentered(Graphics2D g, Rectangle plot, String message) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(AXIS_TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(message, plot.x + (plot.width - fm.stringWidth(message)) / 2, plot.y + plot.height / 2);
    }

    static final class LineChart extends JComponent {
        record Series(String name, Color color, double[] values) {}

        private List<Series> series = List.of();
        private long start, end;
        private String emptyMessage = "";

        LineChart() { setPreferredSize(new Dimension(520, 250)); setOpaque(false); }

        void setData(List<Series> series, long startEpoch, long endEpoch, String emptyMessage) {
            this.series = series; this.start = startEpoch; this.end = endEpoch; this.emptyMessage = emptyMessage;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle plot = new Rectangle(38, 30, getWidth() - 38 - 14, getHeight() - 30 - 26);
            if (plot.width < 20 || plot.height < 20) { g.dispose(); return; }

            g.setFont(SMALL);
            FontMetrics fm = g.getFontMetrics();
            for (int v = 0; v <= 100; v += 25) {
                int y = plot.y + plot.height - (int) Math.round(v / 100.0 * plot.height);
                g.setColor(GRID);
                g.drawLine(plot.x, y, plot.x + plot.width, y);
                g.setColor(AXIS_TEXT);
                String label = v + "%";
                g.drawString(label, plot.x - 6 - fm.stringWidth(label), y + 4);
            }
            int critY = plot.y + plot.height - (int) Math.round(CollectionPolicy.CRITICAL_FILL / 100.0 * plot.height);
            g.setColor(CRITICAL);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f));
            g.drawLine(plot.x, critY, plot.x + plot.width, critY);
            g.setStroke(new BasicStroke(1f));
            drawXAxis(g, plot, start, end);

            boolean any = false;
            int legendX = plot.x;
            for (Series s : series) {
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
                    g.setColor(s.color());
                    g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(path);
                    g.setStroke(new BasicStroke(1f));
                    g.fillOval(lastX - 4, lastY - 4, 8, 8);
                }
                g.setColor(s.color());
                g.fillOval(legendX, 11, 9, 9);
                g.setFont(LEGEND);
                g.setColor(new Color(51, 65, 85));
                String text = s.name() + (Double.isNaN(lastValue) ? "" : String.format(Locale.US, "  now %.0f%%", lastValue));
                g.drawString(text, legendX + 14, 20);
                legendX += 14 + g.getFontMetrics().stringWidth(text) + 18;
            }
            g.setFont(SMALL);
            g.setColor(CRITICAL);
            String critLabel = (int) CollectionPolicy.CRITICAL_FILL + "% critical";
            g.drawString(critLabel, plot.x + plot.width - g.getFontMetrics().stringWidth(critLabel), critY - 4);
            if (!any) drawCentered(g, plot, emptyMessage);
            g.dispose();
        }
    }

    static final class BarChart extends JComponent {
        private int[] counts = new int[0];
        private long start, end;
        private String emptyMessage = "";
        private final Color barColor;

        BarChart(Color barColor) { this.barColor = barColor; setPreferredSize(new Dimension(380, 220)); setOpaque(false); }

        void setData(int[] counts, long startEpoch, long endEpoch, String emptyMessage) {
            this.counts = counts; this.start = startEpoch; this.end = endEpoch; this.emptyMessage = emptyMessage;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle plot = new Rectangle(34, 14, getWidth() - 34 - 12, getHeight() - 14 - 26);
            if (plot.width < 20 || plot.height < 20) { g.dispose(); return; }

            int max = 0;
            for (int c : counts) max = Math.max(max, c);
            int step = Math.max(1, (int) Math.ceil(Math.max(max, 4) / 4.0));
            int top = step * 4;

            g.setFont(SMALL);
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = plot.y + plot.height - (int) Math.round(i / 4.0 * plot.height);
                g.setColor(GRID);
                g.drawLine(plot.x, y, plot.x + plot.width, y);
                g.setColor(AXIS_TEXT);
                String label = String.valueOf(i * step);
                g.drawString(label, plot.x - 6 - fm.stringWidth(label), y + 4);
            }
            drawXAxis(g, plot, start, end);

            if (max == 0) { drawCentered(g, plot, emptyMessage); g.dispose(); return; }
            int n = counts.length;
            double slot = (double) plot.width / n;
            int barWidth = (int) Math.max(3, slot * 0.62);
            for (int i = 0; i < n; i++) {
                if (counts[i] == 0) continue;
                int h = (int) Math.round((double) counts[i] / top * plot.height);
                int x = plot.x + (int) Math.round(slot * i + (slot - barWidth) / 2);
                g.setColor(barColor);
                g.fillRoundRect(x, plot.y + plot.height - h, barWidth, h, 4, 4);
                g.setColor(new Color(51, 65, 85));
                String label = String.valueOf(counts[i]);
                g.drawString(label, x + (barWidth - fm.stringWidth(label)) / 2, plot.y + plot.height - h - 3);
            }
            g.dispose();
        }
    }
}
