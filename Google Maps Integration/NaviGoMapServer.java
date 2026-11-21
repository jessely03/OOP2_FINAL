import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaviGoMapServer {

    // Replace with your Google Cloud API Key (enable Places API & Maps Static API)
    private static final String GOOGLE_API_KEY = System.getenv().getOrDefault(
            "GOOGLE_MAPS_API_KEY", "YOUR_GOOGLE_MAPS_API_KEY"
    );

    private static final int PORT = 8080;
    private static final double TAIPEI_LAT = 25.0418;
    private static final double TAIPEI_LNG = 121.5432;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Endpoint 1: Real-time Google Places search formatted for NaviGo's frontend
        server.createContext("/api/places/search", NaviGoMapServer::handlePlaceSearch);

        // Endpoint 2: Real Google Map image generator for the itinerary route panel
        server.createContext("/api/map/static", NaviGoMapServer::handleStaticMap);

        server.setExecutor(null);
        server.start();
        System.out.println("NaviGo Java Map Server running at http://localhost:" + PORT);
        System.out.println("Test search: http://localhost:" + PORT + "/api/places/search?query=night+market");
    }

    /**
     * GET /api/places/search?query=ramen
     * Returns JSON array matching friend's schema:
     * [{"id":"...", "name":"...", "area":"...", "type":"food", "lat":25.05, "lng":121.57, "rating":"4.5"}]
     */
    private static void handlePlaceSearch(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String query = params.getOrDefault("query", "Taipei night market").trim();
        if (query.isEmpty()) {
            query = "Taipei attractions";
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String googleUrl = String.format(
                    "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&location=%f,%f&radius=10000&key=%s",
                    encodedQuery, TAIPEI_LAT, TAIPEI_LNG, GOOGLE_API_KEY
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(googleUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String formattedJson = convertGooglePlacesToNaviGoJson(response.body());

            sendJsonResponse(exchange, 200, formattedJson);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * GET /api/map/static?stops=25.034,121.565|25.027,121.577
     * Fetches a real Google Static Map PNG with NaviGo blue (#305e85) numbered pins & route path
     */
    private static void handleStaticMap(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String stopsParam = params.getOrDefault("stops", "25.034,121.565");
        String[] coordsList = stopsParam.split("\\|");

        try {
            StringBuilder mapUrl = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap?");
            mapUrl.append("size=640x400&scale=2&maptype=roadmap");

            if (coordsList.length == 1) {
                mapUrl.append("&center=").append(URLEncoder.encode(coordsList[0], StandardCharsets.UTF_8));
                mapUrl.append("&zoom=15");
            }

            // Draw dashed-style route polyline in NaviGo's #547e9f color
            if (coordsList.length > 1) {
                StringBuilder pathBuilder = new StringBuilder("color:0x547e9fff|weight:4");
                for (String c : coordsList) {
                    pathBuilder.append("|").append(c.trim());
                }
                mapUrl.append("&path=").append(URLEncoder.encode(pathBuilder.toString(), StandardCharsets.UTF_8));
            }

            // Add numbered markers in NaviGo's #305e85 color
            for (int i = 0; i < Math.min(coordsList.length, 9); i++) {
                String marker = String.format("color:0x305e85|label:%d|%s", i + 1, coordsList[i].trim());
                mapUrl.append("&markers=").append(URLEncoder.encode(marker, StandardCharsets.UTF_8));
            }

            mapUrl.append("&key=").append(GOOGLE_API_KEY);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(mapUrl.toString())).GET().build();
            HttpResponse<byte[]> imgResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, imgResponse.body().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(imgResponse.body());
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"Failed to load static map\"}");
        }
    }

    /**
     * Parses Google Places API response into the exact object format used by NaviGo's map.js:
     * { id, name, area, type, lat, lng, rating }
     */
    private static String convertGooglePlacesToNaviGoJson(String googleJson) {
        List<String> jsonObjects = new ArrayList<>();
        String[] entries = googleJson.split("\"formatted_address\"\\s*:");

        Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern placeIdPattern = Pattern.compile("\"place_id\"\\s*:\\s*\"([^\"]+)\"");
        Pattern latLngPattern = Pattern.compile("\"location\"\\s*:\\s*\\{\\s*\"lat\"\\s*:\\s*([-0-9.]+),\\s*\"lng\"\\s*:\\s*([-0-9.]+)");
        Pattern typesPattern = Pattern.compile("\"types\"\\s*:\\s*\\[([^\\]]+)\\]");
        Pattern ratingPattern = Pattern.compile("\"rating\"\\s*:\\s*([0-9.]+)");

        for (int i = 1; i < entries.length && jsonObjects.size() < 12; i++) {
            String block = entries[i];
            String address = block.split("\"")[1];
            // Extract concise neighborhood/area from address
            String shortArea = address.replace(", Taiwan", "").replace("Taipei City, ", "");

            Matcher nameM = namePattern.matcher(block);
            Matcher idM = placeIdPattern.matcher(block);
            Matcher locM = latLngPattern.matcher(block);
            Matcher typesM = typesPattern.matcher(block);
            Matcher rateM = ratingPattern.matcher(block);

            if (nameM.find() && locM.find()) {
                String id = idM.find() ? idM.group(1) : "gp_" + i;
                String name = nameM.group(1);
                double lat = Double.parseDouble(locM.group(1));
                double lng = Double.parseDouble(locM.group(2));
                String rawTypes = typesM.find() ? typesM.group(1) : "";
                String rating = rateM.find() ? rateM.group(1) : "";

                String naviGoCategory = mapToNaviGoCategory(rawTypes);
                String areaDisplay = rating.isEmpty() ? shortArea : "★ " + rating + " · " + shortArea;

                String obj = String.format(
                        "{\"id\":\"%s\",\"name\":\"%s\",\"area\":\"%s\",\"type\":\"%s\",\"lat\":%f,\"lng\":%f}",
                        escapeJson(id),
                        escapeJson(name),
                        escapeJson(areaDisplay),
                        naviGoCategory,
                        lat,
                        lng
                );
                jsonObjects.add(obj);
            }
        }
        return "[" + String.join(",", jsonObjects) + "]";
    }

    /**
     * Maps Google Place types to NaviGo's 4 CSS categories: city | culture | food | nature
     */
    private static String mapToNaviGoCategory(String googleTypes) {
        String t = googleTypes.toLowerCase();
        if (t.contains("restaurant") || t.contains("food") || t.contains("cafe") || t.contains("bakery") || t.contains("bar")) {
            return "food";
        }
        if (t.contains("park") || t.contains("natural_feature") || t.contains("campground") || t.contains("zoo")) {
            return "nature";
        }
        if (t.contains("museum") || t.contains("place_of_worship") || t.contains("art_gallery") || t.contains("hindu_temple") || t.contains("buddhist_temple")) {
            return "culture";
        }
        return "city";
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
        return map;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
