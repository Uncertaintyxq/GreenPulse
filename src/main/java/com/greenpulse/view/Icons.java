package com.greenpulse.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * The GreenPulse icon set, drawn with Java2D on an 18-unit grid with one 1.75-unit round stroke, so every
 * icon matches in weight. An icon paints in its component's text colour unless it is given a colour token.
 */
public final class Icons implements Icon {
    public enum Glyph {
        MONITOR, ANALYTICS, SIMULATE, DISPATCH, REGISTER, PROFILE, PLAY, STOP, LOGOUT, SEARCH, CHECK, TRASH,
        REFRESH, ARROW_RIGHT, ARROW_LEFT, PIN, FIT, SAVE, SKIP, INFO, WARNING, SUN, MOON, DOT
    }

    private final Glyph glyph;
    private final int size;
    private final String colorKey;

    private Icons(Glyph glyph, int size, String colorKey) { this.glyph = glyph; this.size = size; this.colorKey = colorKey; }

    public static Icons of(Glyph glyph) { return new Icons(glyph, 18, null); }
    public static Icons of(Glyph glyph, int size) { return new Icons(glyph, size, null); }
    public static Icons of(Glyph glyph, int size, String colorKey) { return new Icons(glyph, size, colorKey); }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color color = colorKey != null ? Theme.color(colorKey) : (c != null ? c.getForeground() : Theme.color(Theme.INK));
        if (c != null && !c.isEnabled()) color = Theme.color(Theme.INK3);
        g.setColor(color);
        g.translate(x, y);
        double s = size / 18.0;
        g.scale(s, s);
        g.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        draw(g);
        g.dispose();
    }

    private void draw(Graphics2D g) {
        switch (glyph) {
            case MONITOR -> { // gauge
                g.draw(new Arc2D.Double(2, 3.5, 14, 14, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(9, 10.5, 12.6, 6.6));
                g.fill(new Ellipse2D.Double(7.6, 9.1, 2.8, 2.8));
                g.draw(new Line2D.Double(2.5, 14.5, 15.5, 14.5));
            }
            case ANALYTICS -> {
                g.draw(polyline(2.5, 2.5, 2.5, 15.5, 15.5, 15.5));
                g.draw(polyline(5.5, 12, 8.5, 8.5, 11, 10.5, 15, 5));
            }
            case SIMULATE -> { // three sliders
                for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(3, 4.5 + i * 4.5, 15, 4.5 + i * 4.5));
                g.fill(new Ellipse2D.Double(10, 3, 3, 3));
                g.fill(new Ellipse2D.Double(5, 7.5, 3, 3));
                g.fill(new Ellipse2D.Double(11.5, 12, 3, 3));
            }
            case DISPATCH -> { // a route from a start dot to a pin
                g.draw(new Ellipse2D.Double(2, 12, 4, 4));
                Path2D p = new Path2D.Double();
                p.moveTo(6, 14); p.lineTo(10, 14); p.curveTo(13, 14, 13, 9.5, 10, 9.5); p.lineTo(8, 9.5);
                p.curveTo(5, 9.5, 5, 5, 8, 5); p.lineTo(11, 5);
                g.draw(p);
                g.draw(pin(14.2, 2.2, 3.2));
            }
            case REGISTER -> { // bin with a plus
                g.draw(polyline(3.5, 5.5, 4.8, 16, 13.2, 16, 14.5, 5.5));
                g.draw(new Line2D.Double(2.5, 5.5, 15.5, 5.5));
                g.draw(new Line2D.Double(7, 3, 11, 3));
                g.draw(new Line2D.Double(9, 8.5, 9, 13));
                g.draw(new Line2D.Double(6.8, 10.75, 11.2, 10.75));
            }
            case PROFILE -> {
                g.draw(new Ellipse2D.Double(5.5, 2.5, 7, 7));
                g.draw(new Arc2D.Double(2.5, 11, 13, 10, 20, 140, Arc2D.OPEN));
            }
            case PLAY -> g.draw(polygon(5.5, 3.5, 14.5, 9, 5.5, 14.5));
            case STOP -> g.draw(new RoundRectangle2D.Double(4, 4, 10, 10, 2.5, 2.5));
            case LOGOUT -> {
                g.draw(polyline(10, 3, 3.5, 3, 3.5, 15, 10, 15));
                g.draw(new Line2D.Double(7.5, 9, 15.5, 9));
                g.draw(polyline(12.5, 6, 15.5, 9, 12.5, 12));
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(2.5, 2.5, 10, 10));
                g.draw(new Line2D.Double(11, 11, 15.5, 15.5));
            }
            case CHECK -> g.draw(polyline(3, 9.5, 7, 13.5, 15, 4.5));
            case TRASH -> {
                g.draw(new Line2D.Double(2.5, 4.5, 15.5, 4.5));
                g.draw(polyline(6.5, 4.5, 6.5, 2.5, 11.5, 2.5, 11.5, 4.5));
                g.draw(polyline(4, 4.5, 5, 15.5, 13, 15.5, 14, 4.5));
                g.draw(new Line2D.Double(7.5, 7.5, 7.5, 12.5));
                g.draw(new Line2D.Double(10.5, 7.5, 10.5, 12.5));
            }
            case REFRESH -> {
                g.draw(new Arc2D.Double(3, 3, 12, 12, 70, 290, Arc2D.OPEN));
                g.draw(polyline(10.5, 1.8, 13.2, 3.6, 11, 6.2));
            }
            case ARROW_RIGHT -> { g.draw(new Line2D.Double(3, 9, 15, 9)); g.draw(polyline(10.5, 4.5, 15, 9, 10.5, 13.5)); }
            case ARROW_LEFT -> { g.draw(new Line2D.Double(3, 9, 15, 9)); g.draw(polyline(7.5, 4.5, 3, 9, 7.5, 13.5)); }
            case PIN -> { g.draw(pin(9, 1.5, 5)); g.draw(new Ellipse2D.Double(7.4, 4.9, 3.2, 3.2)); }
            case FIT -> {
                g.draw(polyline(2.5, 6.5, 2.5, 2.5, 6.5, 2.5));
                g.draw(polyline(11.5, 2.5, 15.5, 2.5, 15.5, 6.5));
                g.draw(polyline(15.5, 11.5, 15.5, 15.5, 11.5, 15.5));
                g.draw(polyline(6.5, 15.5, 2.5, 15.5, 2.5, 11.5));
            }
            case SAVE -> {
                g.draw(polyline(2.5, 11, 2.5, 15.5, 15.5, 15.5, 15.5, 11));
                g.draw(new Line2D.Double(9, 2.5, 9, 11.5));
                g.draw(polyline(5.5, 8, 9, 11.5, 12.5, 8));
            }
            case SKIP -> { g.draw(polygon(3.5, 4, 11, 9, 3.5, 14)); g.draw(new Line2D.Double(14, 4, 14, 14)); }
            case INFO -> {
                g.draw(new Ellipse2D.Double(2, 2, 14, 14));
                g.draw(new Line2D.Double(9, 8.2, 9, 12.8));
                g.fill(new Ellipse2D.Double(8, 4.6, 2, 2));
            }
            case WARNING -> {
                g.draw(polygon(9, 2.5, 16, 15, 2, 15));
                g.draw(new Line2D.Double(9, 7, 9, 10.5));
                g.fill(new Ellipse2D.Double(8.1, 12, 1.8, 1.8));
            }
            case SUN -> {
                g.draw(new Ellipse2D.Double(5.5, 5.5, 7, 7));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    g.draw(new Line2D.Double(9 + Math.cos(a) * 6, 9 + Math.sin(a) * 6, 9 + Math.cos(a) * 7.6, 9 + Math.sin(a) * 7.6));
                }
            }
            case MOON -> {
                Area moon = new Area(new Ellipse2D.Double(2.5, 2.5, 13, 13));
                moon.subtract(new Area(new Ellipse2D.Double(6.5, 0.5, 12, 12)));
                g.draw(moon);
            }
            case DOT -> g.fill(new Ellipse2D.Double(5, 5, 8, 8));
        }
    }

    private static Path2D pin(double cx, double top, double r) {
        Path2D p = new Path2D.Double();
        double cy = top + r;
        p.moveTo(cx, cy + r * 2.1);
        p.curveTo(cx - r * 0.6, cy + r * 1.3, cx - r, cy + r * 0.6, cx - r, cy);
        p.curveTo(cx - r, cy - r * 0.6, cx - r * 0.55, cy - r, cx, cy - r);
        p.curveTo(cx + r * 0.55, cy - r, cx + r, cy - r * 0.6, cx + r, cy);
        p.curveTo(cx + r, cy + r * 0.6, cx + r * 0.6, cy + r * 1.3, cx, cy + r * 2.1);
        p.closePath();
        return p;
    }

    private static Path2D polyline(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        return p;
    }

    private static Path2D polygon(double... xy) {
        Path2D p = polyline(xy);
        p.closePath();
        return p;
    }
}
