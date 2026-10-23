package com.greenpulse.server;

import com.greenpulse.dao.SmartBinDAO;
import com.greenpulse.event.BinEventBus;
import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional HTTP endpoint for real sensors: {@code POST /api/telemetry} with {@code {"bin_id":"BIN-001","fill":72.5}}
 * and the header {@code X-API-Key}. It only updates bins that are already registered; it never creates bins.
 * Start it by setting the {@code GREENPULSE_IOT_API_KEY} environment variable (the dashboard does this for admins).
 */
public class IoTTelemetryServer {
    private static final Logger LOG = Logger.getLogger(IoTTelemetryServer.class.getName());
    private static final int MAX_BODY_BYTES = 4096;
    private static final Pattern BIN_ID = Pattern.compile("\"bin_id\"\\s*:\\s*\"([A-Za-z0-9._-]{1,50})\"");
    private static final Pattern FILL = Pattern.compile("\"fill\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?");

    private static HttpServer server;
    private static boolean isRunning = false;

    public static synchronized void startServer(int port, String apiKey, Runnable onDataReceived) throws IOException {
        if (isRunning) return;
        if (apiKey == null || apiKey.length() < 8) {
            throw new IllegalArgumentException("Set GREENPULSE_IOT_API_KEY to a secret of at least 8 characters before starting the IoT listener.");
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/telemetry", new TelemetryHandler(apiKey, onDataReceived));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        isRunning = true;
        LOG.info("IoT listener started on port " + port);
    }

    public static synchronized void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
        }
    }

    public static boolean isRunning() { return isRunning; }

    private static class TelemetryHandler implements HttpHandler {
        private final byte[] apiKey;
        private final Runnable onDataReceived;

        TelemetryHandler(String apiKey, Runnable onDataReceived) {
            this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
            this.onDataReceived = onDataReceived;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    respond(exchange, 405, "{\"status\":\"ERROR\",\"message\":\"Use POST.\"}");
                    return;
                }
                String provided = exchange.getRequestHeaders().getFirst("X-API-Key");
                if (provided == null || !MessageDigest.isEqual(apiKey, provided.getBytes(StandardCharsets.UTF_8))) {
                    respond(exchange, 401, "{\"status\":\"ERROR\",\"message\":\"Missing or invalid API key.\"}");
                    return;
                }
                byte[] raw = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
                if (raw.length > MAX_BODY_BYTES) {
                    respond(exchange, 413, "{\"status\":\"ERROR\",\"message\":\"Request too large.\"}");
                    return;
                }
                String body = new String(raw, StandardCharsets.UTF_8);
                Matcher id = BIN_ID.matcher(body);
                Matcher fillMatch = FILL.matcher(body);
                if (!id.find() || !fillMatch.find()) {
                    respond(exchange, 400, "{\"status\":\"ERROR\",\"message\":\"Body must contain bin_id and fill.\"}");
                    return;
                }
                String binId = id.group(1);
                double fill = Double.parseDouble(fillMatch.group(1));

                SmartBinDAO dao = new SmartBinDAO();
                Optional<SmartBin> found = dao.findById(binId);
                if (found.isEmpty()) {
                    respond(exchange, 404, "{\"status\":\"ERROR\",\"message\":\"Unknown bin. Register it in GreenPulse first.\"}");
                    return;
                }
                SmartBin bin = found.get();
                bin.setFillLevel(fill); // rejects values outside 0-100
                if (dao.recordReading(bin, ReadingSource.SENSOR)) {
                    BinEventBus.getInstance().publish(bin, ReadingSource.SENSOR);
                    if (onDataReceived != null) onDataReceived.run();
                }
                respond(exchange, 200, "{\"status\":\"SUCCESS\"}");
            } catch (com.greenpulse.exception.InvalidFillLevelException | NumberFormatException bad) {
                respond(exchange, 400, "{\"status\":\"ERROR\",\"message\":\"fill must be a number from 0 to 100.\"}");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "IoT request failed", e);
                respond(exchange, 500, "{\"status\":\"ERROR\",\"message\":\"Server error.\"}");
            }
        }

        private void respond(HttpExchange exchange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }
}
