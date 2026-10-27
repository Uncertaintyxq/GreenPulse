package com.greenpulse.view;

import com.greenpulse.model.CollectionPolicy;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Every active bin as one dot on a 0–100% fill line, with the due and critical marks drawn on the track.
 * Bins at similar fill stack upward, so clusters read at a glance. A dot's colour is its status (a bin can be
 * critical below 85% through its priority score), its position is its fill. Hover names the bin; click picks it.
 */
final class FillLine extends JComponent {
    record Dot(String binId, double fill, String status) {}

    private static final int R = 6, STEP = 13, MAX_LANE = 2, SIDE = 10, TRACK_Y = 36, TRACK_H = 8;

    private final List<Dot> dots = new ArrayList<>();
    private final List<Point> centres = new ArrayList<>();
    private int hovered = -1;
    private Consumer<String> onPick = id -> {};

    FillLine() {
        setPreferredSize(new Dimension(600, 70));
        setToolTipText("");
        getAccessibleContext().setAccessibleName("Fill line");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHovered(dotAt(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHovered(-1); }
            @Override public void mouseClicked(MouseEvent e) {
                int i = dotAt(e.getPoint());
                if (i >= 0) onPick.accept(dots.get(i).binId());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void onPick(Consumer<String> listener) { this.onPick = listener; }

    // A bare JComponent has no accessible context until a subclass creates one.
    @Override public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {
            @Override public AccessibleRole getAccessibleRole() { return AccessibleRole.PANEL; }
        };
        return accessibleContext;
    }

    void setBins(List<Dot> bins) {
        dots.clear();
        dots.addAll(bins);
        dots.sort(Comparator.comparingDouble(Dot::fill));
        hovered = -1;
        long critical = dots.stream().filter(d -> MainDashboardFrame.STATUS_CRITICAL.equals(d.status())).count();
        long due = dots.stream().filter(d -> MainDashboardFrame.STATUS_DUE.equals(d.status())).count();
        getAccessibleContext().setAccessibleDescription(dots.size() + " bins by fill level: " + critical + " critical, " + due + " due for pickup");
        repaint();
    }

    private void setHovered(int index) {
        if (index == hovered) return;
        hovered = index;
        setCursor(Cursor.getPredefinedCursor(index >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private int x(double fill) { return SIDE + (int) Math.round(Math.max(0, Math.min(100, fill)) / 100.0 * (getWidth() - 2 * SIDE)); }

    /** Places each dot in the lowest lane where it does not overlap a neighbour, like a dot plot. */
    private void layoutDots() {
        centres.clear();
        List<List<Integer>> lanes = new ArrayList<>();
        for (int i = 0; i <= MAX_LANE; i++) lanes.add(new ArrayList<>());
        for (Dot d : dots) {
            int cx = x(d.fill()), lane = MAX_LANE;
            for (int l = 0; l <= MAX_LANE; l++) {
                List<Integer> taken = lanes.get(l);
                if (taken.isEmpty() || cx - taken.get(taken.size() - 1) >= 2 * R + 2) { lane = l; break; }
            }
            lanes.get(lane).add(cx);
            centres.add(new Point(cx, TRACK_Y + TRACK_H / 2 - lane * STEP));
        }
    }

    private int dotAt(Point p) {
        int best = -1;
        double bestDist = R + 4;
        for (int i = 0; i < centres.size(); i++) {
            double dist = p.distance(centres.get(i));
            if (dist <= bestDist) { bestDist = dist; best = i; }
        }
        return best;
    }

    @Override public String getToolTipText(MouseEvent e) {
        int i = dotAt(e.getPoint());
        if (i < 0) return null;
        Dot d = dots.get(i);
        String state = switch (d.status()) {
            case MainDashboardFrame.STATUS_CRITICAL -> "Critical";
            case MainDashboardFrame.STATUS_DUE -> "Due for pickup";
            default -> "Normal";
        };
        return String.format(Locale.US, "%s · %.0f%% full · %s", d.binId(), d.fill(), state);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        layoutDots();
        int due = x(CollectionPolicy.ROUTE_MIN_FILL), critical = x(CollectionPolicy.CRITICAL_FILL), end = x(100);

        // The track: neutral up to the due mark, then the due and critical zones.
        Shape track = new RoundRectangle2D.Double(SIDE, TRACK_Y, end - SIDE, TRACK_H, TRACK_H, TRACK_H);
        Shape clip = g.getClip();
        g.clip(track);
        g.setColor(Theme.color(Theme.LINE2));
        g.fillRect(SIDE, TRACK_Y, due - SIDE, TRACK_H);
        g.setColor(Theme.color(Theme.WARNING_SOFT));
        g.fillRect(due, TRACK_Y, critical - due, TRACK_H);
        g.setColor(Theme.color(Theme.CRITICAL_SOFT));
        g.fillRect(critical, TRACK_Y, end - critical + TRACK_H, TRACK_H);
        g.setClip(clip);
        g.setColor(Theme.color(Theme.LINE));
        g.draw(new RoundRectangle2D.Double(SIDE + 0.5, TRACK_Y + 0.5, end - SIDE - 1, TRACK_H - 1, TRACK_H, TRACK_H));

        // The two marks and the scale.
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Theme.color(Theme.WARNING_BAR));
        g.draw(new Line2D.Double(due, TRACK_Y - 6, due, TRACK_Y + TRACK_H + 6));
        g.setColor(Theme.color(Theme.CRITICAL));
        g.draw(new Line2D.Double(critical, TRACK_Y - 6, critical, TRACK_H + TRACK_Y + 6));
        g.setFont(Theme.mono(12));
        FontMetrics fm = g.getFontMetrics();
        int base = TRACK_Y + TRACK_H + 8 + fm.getAscent();
        g.setColor(Theme.color(Theme.INK3));
        g.drawString("0%", SIDE - 2, base);
        g.drawString("100%", end - fm.stringWidth("100%") + 2, base);
        String dueText = "Due " + (int) CollectionPolicy.ROUTE_MIN_FILL + "%", criticalText = "Critical " + (int) CollectionPolicy.CRITICAL_FILL + "%";
        g.setColor(Theme.color(Theme.WARNING_TEXT));
        g.drawString(dueText, due - fm.stringWidth(dueText) / 2, base);
        g.setColor(Theme.color(Theme.CRITICAL_TEXT));
        g.drawString(criticalText, critical - fm.stringWidth(criticalText) / 2, base);

        if (dots.isEmpty()) {
            g.setFont(Theme.regular(13));
            g.setColor(Theme.color(Theme.INK3));
            String empty = "Bins appear here by fill level once they are registered.";
            g.drawString(empty, SIDE, TRACK_Y - 18);
        }

        // The bins, a ring of sheet colour around each so stacked dots stay separate.
        Color ring = Theme.color(Theme.SURFACE);
        for (int i = 0; i < dots.size(); i++) {
            Point c = centres.get(i);
            String fillKey = switch (dots.get(i).status()) {
                case MainDashboardFrame.STATUS_CRITICAL -> Theme.CRITICAL;
                case MainDashboardFrame.STATUS_DUE -> Theme.WARNING_BAR;
                default -> Theme.OK;
            };
            int r = i == hovered ? R + 1 : R;
            g.setColor(ring);
            g.fill(new Ellipse2D.Double(c.x - r - 2, c.y - r - 2, 2 * r + 4, 2 * r + 4));
            g.setColor(Theme.color(fillKey));
            g.fill(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
            if (i == hovered) {
                g.setColor(Theme.color(Theme.INK));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(c.x - r - 2.5, c.y - r - 2.5, 2 * r + 5, 2 * r + 5));
            }
        }
        g.dispose();
    }
}
