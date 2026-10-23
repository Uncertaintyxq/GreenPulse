package com.greenpulse.service;

import com.greenpulse.model.SmartBin;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Uses OSRM's public demo endpoint. Production deployments should configure their own OSRM server. */
public class RoadRoutingService {
    private static final String DEFAULT_BASE = "https://router.project-osrm.org";
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
    private static final Pattern PAIR = Pattern.compile("\\[\\s*(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)\\s*\\]");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final String baseUrl = System.getProperty("greenpulse.osrm.url", DEFAULT_BASE).replaceAll("/+$", "");

    public RouteResult calculateOptimizedRoute(double depotLat, double depotLon, List<SmartBin> bins)
            throws IOException, InterruptedException {
        if (bins == null || bins.isEmpty()) return new RouteResult(List.of(), List.of(), 0, 0);
        if (!validPhilippines(depotLat, depotLon)) throw new IllegalArgumentException("Set the depot inside the Philippines using the map picker.");

        List<SmartBin> eligible = new ArrayList<>();
        for (SmartBin bin : bins) if (bin.hasCoordinates()) {
            if (!validPhilippines(bin.getLatitude(), bin.getLongitude()))
                throw new IllegalArgumentException("Bin " + bin.getBinID() + " has coordinates outside the Philippines. Correct it from the dispatch map.");
            eligible.add(bin);
        }
        if (eligible.isEmpty()) return new RouteResult(List.of(), List.of(), 0, 0);
        if (eligible.size() > 50) throw new IllegalArgumentException("This demo route supports up to 50 GPS-enabled bins per dispatch. Use a managed/self-hosted routing service for larger fleets.");

        List<Coordinate> points = new ArrayList<>();
        points.add(new Coordinate(depotLat, depotLon));
        for (SmartBin bin : eligible) points.add(new Coordinate(bin.getLatitude(), bin.getLongitude()));

        double[][] distances = requestDistanceMatrix(points);
        List<Integer> order = optimizeStopOrder(distances, eligible.size());
        List<SmartBin> orderedBins = new ArrayList<>();
        List<Coordinate> orderedCoordinates = new ArrayList<>();
        orderedCoordinates.add(points.get(0));
        for (int idx : order) {
            orderedBins.add(eligible.get(idx - 1));
            orderedCoordinates.add(points.get(idx));
        }
        orderedCoordinates.add(points.get(0)); // return to depot
        RouteResult actual = requestRoute(orderedCoordinates, orderedBins);
        return new RouteResult(orderedBins, actual.geometry, actual.distanceMeters, actual.durationSeconds);
    }

    private double[][] requestDistanceMatrix(List<Coordinate> points) throws IOException, InterruptedException {
        String url = baseUrl + "/table/v1/driving/" + joinCoordinates(points) + "?annotations=distance";
        String json = get(url);
        int key = json.indexOf("\"distances\"");
        if (key < 0) throw new IOException("OSRM did not return a road-distance matrix.");
        int start = json.indexOf("[[", key);
        int end = json.indexOf("]]", start);
        if (start < 0 || end < 0) throw new IOException("OSRM returned an invalid distance matrix.");
        String matrixText = json.substring(start + 1, end + 1);
        Matcher rowMatcher = Pattern.compile("\\[([^\\[\\]]*)\\]").matcher(matrixText);
        List<double[]> rows = new ArrayList<>();
        while (rowMatcher.find()) {
            String rowText = rowMatcher.group(1);
            Matcher values = Pattern.compile("null|-?\\d+(?:\\.\\d+)?").matcher(rowText);
            List<Double> row = new ArrayList<>();
            while (values.find()) row.add("null".equals(values.group()) ? Double.POSITIVE_INFINITY : Double.parseDouble(values.group()));
            double[] arr = new double[row.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = row.get(i);
            rows.add(arr);
        }
        if (rows.size() != points.size()) throw new IOException("OSRM returned an incomplete distance matrix.");
        double[][] result = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).length != points.size()) throw new IOException("OSRM returned an incomplete distance matrix.");
            result[i] = rows.get(i);
        }
        return result;
    }

    private List<Integer> optimizeStopOrder(double[][] d, int binCount) {
        List<Integer> route = new ArrayList<>();
        boolean[] used = new boolean[binCount + 1];
        int current = 0;
        for (int step = 0; step < binCount; step++) {
            int best = -1;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int candidate = 1; candidate <= binCount; candidate++) {
                if (!used[candidate] && d[current][candidate] < bestDistance) {
                    bestDistance = d[current][candidate]; best = candidate;
                }
            }
            if (best < 0 || !Double.isFinite(bestDistance)) throw new IllegalArgumentException("One or more bins are not reachable by the driving network.");
            route.add(best); used[best] = true; current = best;
        }
        // 2-opt improves the nearest-neighbour order using OSRM road distances.
        boolean improved = true;
        int passes = 0;
        while (improved && passes++ < 30 && route.size() >= 3) {
            improved = false;
            for (int i = 0; i < route.size() - 1; i++) {
                for (int k = i + 1; k < route.size(); k++) {
                    double oldCost = routeCost(d, route);
                    Collections.reverse(route.subList(i, k + 1));
                    double newCost = routeCost(d, route);
                    if (Double.isFinite(newCost) && newCost + 0.5 < oldCost) {
                        improved = true;
                    } else {
                        Collections.reverse(route.subList(i, k + 1));
                    }
                }
            }
        }
        return route;
    }

    private double routeCost(double[][] distances, List<Integer> route) {
        if (route.isEmpty()) return 0;
        double total = distances[0][route.get(0)];
        for (int i = 1; i < route.size(); i++) total += distances[route.get(i - 1)][route.get(i)];
        total += distances[route.get(route.size() - 1)][0];
        return total;
    }

    private RouteResult requestRoute(List<Coordinate> points, List<SmartBin> orderedBins) throws IOException, InterruptedException {
        String url = baseUrl + "/route/v1/driving/" + joinCoordinates(points) + "?overview=full&geometries=geojson&steps=false";
        String json = get(url);
        if (!json.contains("\"code\":\"Ok\"")) throw new IOException("OSRM could not calculate a road route for these coordinates.");
        int routesAt = json.indexOf("\"routes\"");
        int routeStart = json.indexOf('{', routesAt);
        int geometryAt = json.indexOf("\"geometry\"", routeStart);
        int coordinatesAt = json.indexOf("\"coordinates\"", geometryAt);
        int coordStart = json.indexOf('[', coordinatesAt);
        int coordEnd = json.indexOf("\"type\"", coordStart);
        if (routesAt < 0 || geometryAt < 0 || coordStart < 0 || coordEnd < 0) throw new IOException("OSRM route geometry was missing.");
        String geometryJson = json.substring(coordStart, coordEnd);
        Matcher pairMatcher = PAIR.matcher(geometryJson);
        List<Coordinate> geometry = new ArrayList<>();
        while (pairMatcher.find()) {
            double lon = Double.parseDouble(pairMatcher.group(1));
            double lat = Double.parseDouble(pairMatcher.group(2));
            geometry.add(new Coordinate(lat, lon));
        }
        if (geometry.size() < 2) throw new IOException("OSRM returned an empty route geometry.");
        String routeJson = extractJsonObject(json, routeStart);
        Matcher distances = Pattern.compile("\"distance\"\\s*:\\s*(\\d+(?:\\.\\d+)?)").matcher(routeJson);
        Matcher durations = Pattern.compile("\"duration\"\\s*:\\s*(\\d+(?:\\.\\d+)?)").matcher(routeJson);
        double distance = lastNumber(distances);
        double duration = lastNumber(durations);
        if (distance < 0 || duration < 0) throw new IOException("OSRM route summary was missing.");
        return new RouteResult(orderedBins, geometry, distance, duration);
    }

    private String extractJsonObject(String json, int start) throws IOException {
        if (start < 0 || start >= json.length() || json.charAt(start) != '{')
            throw new IOException("OSRM route response was malformed.");
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') depth++;
                else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
            }
        }
        throw new IOException("OSRM route response was incomplete.");
    }

    private double lastNumber(Matcher matcher) {
        double value = -1;
        while (matcher.find()) value = Double.parseDouble(matcher.group(1));
        return value;
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "GreenPulse-SmartWaste/3.0 (educational dispatch demonstration)")
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Routing service returned HTTP " + response.statusCode() + ".");
        return response.body();
    }

    private String joinCoordinates(List<Coordinate> points) {
        StringBuilder out = new StringBuilder();
        for (Coordinate p : points) {
            if (out.length() > 0) out.append(';');
            out.append(String.format(Locale.US, "%.6f,%.6f", p.longitude, p.latitude));
        }
        return out.toString();
    }

    private boolean validPhilippines(double lat, double lon) {
        // Practical bounding box around the Philippine archipelago for this student project.
        return Double.isFinite(lat) && lat >= 4.5 && lat <= 21.5
                && Double.isFinite(lon) && lon >= 116.0 && lon <= 127.5;
    }

    public static final class Coordinate {
        public final double latitude, longitude;
        public Coordinate(double latitude, double longitude) { this.latitude = latitude; this.longitude = longitude; }
    }

    public static final class RouteResult {
        public final List<SmartBin> orderedBins;
        public final List<Coordinate> geometry;
        public final double distanceMeters;
        public final double durationSeconds;
        public RouteResult(List<SmartBin> orderedBins, List<Coordinate> geometry, double distanceMeters, double durationSeconds) {
            this.orderedBins = new ArrayList<>(orderedBins);
            this.geometry = new ArrayList<>(geometry);
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
        }
    }
}
