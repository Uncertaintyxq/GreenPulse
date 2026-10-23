package com.greenpulse.service;

import com.greenpulse.model.CollectionRoute;
import com.greenpulse.model.SmartBin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Exports a planned route as CSV or a printable HTML manifest. All user-entered text is escaped. */
public class ReportExporter {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void exportRouteToCSV(CollectionRoute route, Path destFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(destFile, StandardCharsets.UTF_8))) {
            pw.println("STOP_SEQUENCE,BIN_ID,LOCATION,FILL_PERCENTAGE,PRIORITY_SCORE");
            int seq = 1;
            for (SmartBin bin : route.getBinQueue()) {
                pw.printf(Locale.US, "%d,%s,%s,%.1f,%d%n", seq++, csv(bin.getBinID()), csv(bin.getLocation()),
                        bin.getFillPercentage(), bin.calculatePriority());
            }
        }
    }

    public static void exportPrintableReportHTML(CollectionRoute route, Path destFile) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(destFile, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(w)) {
            pw.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
            pw.println("<title>GreenPulse Optimized Route Manifest</title>");
            pw.println("<style>");
            pw.println("body { font-family: Arial, sans-serif; margin: 30px; color: #1e293b; }");
            pw.println("h1 { color: #10b981; margin-bottom: 5px; }");
            pw.println("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            pw.println("th, td { border: 1px solid #cbd5e1; padding: 10px; text-align: left; }");
            pw.println("th { background-color: #0f172a; color: white; }");
            pw.println(".priority { font-weight: bold; color: #e11d48; }");
            pw.println("</style></head><body>");
            pw.println("<h1>GreenPulse Waste Collection Manifest</h1>");
            pw.println("<p><strong>Generated:</strong> " + LocalDateTime.now().format(STAMP)
                    + " | <strong>Route ID:</strong> " + html(route.getRouteID()) + "</p>");
            pw.println("<p><strong>Driver:</strong> " + html(route.getDriverName())
                    + " | <strong>Vehicle:</strong> " + html(route.getVehicleName()) + "</p>");
            pw.println(String.format(Locale.US, "<p><strong>Road distance:</strong> %.2f km | <strong>Est. drive time:</strong> %.0f min</p>",
                    route.getTotalDistanceKm(), route.getEstimatedMinutes()));
            pw.println("<table><tr><th>Stop #</th><th>Bin ID</th><th>Location</th><th>Fill Level</th><th>Priority Score</th></tr>");
            int seq = 1;
            for (SmartBin bin : route.getBinQueue()) {
                pw.println("<tr><td>#" + (seq++) + "</td><td>" + html(bin.getBinID()) + "</td><td>" + html(bin.getLocation())
                        + "</td><td>" + (int) bin.getFillPercentage() + "%</td><td class=\"priority\">"
                        + bin.calculatePriority() + "</td></tr>");
            }
            pw.println("</table></body></html>");
        }
    }

    /** Quotes a CSV field and neutralises spreadsheet formulas (a leading = + - @ would otherwise be executed by Excel). */
    static String csv(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    static String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
