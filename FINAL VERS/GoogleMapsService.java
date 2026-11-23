import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMapsService {

    private static final String USER_AGENT = "NaviGo-Swing-Trip-Planner/1.0 (StudentProject)";
    private static final int TILE_SIZE = 256;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public record LatLng(double lat, double lng) {
        public static LatLng forDestination(String destination) {
            if (destination == null) return new LatLng(25.0375, 121.5637);
            String city = destination.split(",", 2)[0].strip().toLowerCase(Locale.ROOT);
            return switch (city) {
                case "taipei", "taipei city" -> new LatLng(25.0375, 121.5637);
                case "tainan", "tainan city" -> new LatLng(22.9997, 120.2270);
                case "kaohsiung", "kaohsiung city" -> new LatLng(22.6273, 120.3014);
                case "taichung", "taichung city" -> new LatLng(24.1477, 120.6736);
                default -> new LatLng(23.7, 120.95);
            };
        }
    }

    public static class Place {
        public final String id;
        public final String name;
        public final String area;
        public final String type; // "city", "culture", "food", "nature"
        public final double lat;
        public final double lng;

        public Place(String id, String name, String area, String type, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.area = area;
            this.type = type;
            this.lat = lat;
            this.lng = lng;
        }

        public String getCategoryLabel() {
            return switch (type) {
                case "culture" -> "✦ CULTURE";
                case "food" -> "♨ FOOD";
                case "nature" -> "▲ NATURE";
                default -> "◈ CITY";
            };
        }

        public String getGoogleMapsBrowserUrl() {
            return "https://www.google.com/maps/search/?api=1&query=" +
                    URLEncoder.encode(name + " " + lat + "," + lng, StandardCharsets.UTF_8);
        }

        public String getGoogleDirectionsUrl(LatLng origin) {
            return String.format(Locale.ROOT,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=transit",
                    origin.lat(), origin.lng(), lat, lng);
        }
    }

    /**
     * Curated starter spots matching NaviGo's Taiwan destinations
     */
    public List<Place> getStarterPlacesFor(String destination) {
        String city = destination == null ? "taipei" : destination.split(",", 2)[0].strip().toLowerCase(Locale.ROOT);
        return switch (city) {
            case "tainan", "tainan city" -> List.of(
                    new Place("anping", "Anping Old Fort", "Anping · Historic fort", "culture", 23.0015, 120.1606),
                    new Place("chihkan", "Chihkan Tower", "West Central · Heritage", "culture", 22.9975, 120.2026),
                    new Place("shennong", "Shennong Street", "West Central · Night lane", "food", 22.9979, 120.1966),
                    new Place("garden-nm", "Tainan Flower Night Market", "North District · Street food", "food", 23.0108, 120.2001)
            );
            case "kaohsiung", "kaohsiung city" -> List.of(
                    new Place("pier2", "Pier-2 Art Center", "Yancheng · Harbor art", "culture", 22.6200, 120.2816),
                    new Place("liuhe", "Liuhe Night Market", "Xinxing · Street food", "food", 22.6322, 120.2991),
                    new Place("lotus", "Lotus Pond", "Zuoying · Dragon & Tiger Pagodas", "nature", 22.6815, 120.2966),
                    new Place("sizihwan", "Sizihwan Sunset Viewpoint", "Gushan · Coastline", "nature", 22.6256, 120.2640)
            );
            case "taichung", "taichung city" -> List.of(
                    new Place("theater", "National Taichung Theater", "Xitun · Architecture", "city", 24.1627, 120.6403),
                    new Place("fengjia", "Fengjia Night Market", "Xitun · Street food", "food", 24.1788, 120.6469),
                    new Place("rainbow", "Rainbow Village", "Nantun · Folk art", "culture", 24.1384, 120.6098),
                    new Place("gaomei", "Gaomei Wetlands", "Qingshui · Sunset boardwalk", "nature", 24.3119, 120.5497)
            );
            default -> List.of(
                    new Place("101", "Taipei 101", "Xinyi · Skyline", "city", 25.0340, 121.5650),
                    new Place("elephant", "Elephant Mountain", "Xinyi · Sunset hike", "nature", 25.0270, 121.5770),
                    new Place("raohe", "Raohe Night Market", "Songshan · Street food", "food", 25.0510, 121.5770),
                    new Place("longshan", "Longshan Temple", "Wanhua · Heritage", "culture", 25.0370, 121.5000),
                    new Place("dihua", "Dihua Street", "Datong · Historic district", "culture", 25.0560, 121.5100),
                    new Place("daan", "Daan Forest Park", "Daan · Green space", "nature", 25.0300, 121.5360)
            );
        };
    }

    /**
     * Searches real Taiwan places via OpenStreetMap Nominatim API (100% free, no API key)
     */
    public List<Place> searchPlaces(String query, String destination) throws Exception {
        LatLng center = LatLng.forDestination(destination);
        String cityName = destination != null && !destination.isBlank()
                ? destination.split(",", 2)[0].strip() : "Taipei";

        String fullQuery = query.toLowerCase(Locale.ROOT).contains(cityName.toLowerCase(Locale.ROOT))
                ? query : query + " " + cityName;

        // Create a ~25km bounding box around the destination city to prioritize local results
        double dLon = 0.22;
        double dLat = 0.22;
        String viewbox = String.format(Locale.ROOT, "%f,%f,%f,%f",
                center.lng() - dLon, center.lat() + dLat,
                center.lng() + dLon, center.lat() - dLat);

        String url = "https://nominatim.openstreetmap.org/search?format=jsonv2" +
                "&q=" + URLEncoder.encode(fullQuery, StandardCharsets.UTF_8) +
                "&countrycodes=tw" +
                "&viewbox=" + viewbox +
                "&limit=10&addressdetails=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Place search HTTP error: " + response.statusCode());
        }
        return parseNominatimJson(response.body());
    }

    /**
     * Composites real OpenStreetMap tiles and draws numbered NaviGo forest-green pins
     */
    public ImageIcon fetchStaticMap(List<Place> places, String destination, int width, int height) throws Exception {
        LatLng center;
        int zoom;

        if (places.isEmpty()) {
            center = LatLng.forDestination(destination);
            zoom = 13;
        } else if (places.size() == 1) {
            center = new LatLng(places.get(0).lat, places.get(0).lng);
            zoom = 16;
        } else {
            double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
            for (Place p : places) {
                minLat = Math.min(minLat, p.lat);
                maxLat = Math.max(maxLat, p.lat);
                minLng = Math.min(minLng, p.lng);
                maxLng = Math.max(maxLng, p.lng);
            }
            center = new LatLng((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0);
            double span = Math.max(maxLat - minLat, maxLng - minLng);
            if (span > 0.12) zoom = 11;
            else if (span > 0.06) zoom = 12;
            else if (span > 0.025) zoom = 13;
            else zoom = 14;
        }

        double centerWorldX = lonToWorldX(center.lng(), zoom);
        double centerWorldY = latToWorldY(center.lat(), zoom);

        int topLeftWorldX = (int) Math.round(centerWorldX - width / 2.0);
        int topLeftWorldY = (int) Math.round(centerWorldY - height / 2.0);

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background fallback matching Theme.PALE (#E5EFD9)
        g2.setColor(new Color(0xE5, 0xEF, 0xD9));
        g2.fillRect(0, 0, width, height);

        int maxTile = (1 << zoom) - 1;
        int startTileX = Math.floorDiv(topLeftWorldX, TILE_SIZE);
        int startTileY = Math.floorDiv(topLeftWorldY, TILE_SIZE);
        int endTileX = Math.floorDiv(topLeftWorldX + width, TILE_SIZE);
        int endTileY = Math.floorDiv(topLeftWorldY + height, TILE_SIZE);

        for (int tx = startTileX; tx <= endTileX; tx++) {
            for (int ty = startTileY; ty <= endTileY; ty++) {
                if (tx < 0 || ty < 0 || tx > maxTile || ty > maxTile) continue;
                BufferedImage tile = downloadOsmTile(zoom, tx, ty);
                if (tile != null) {
                    int drawX = tx * TILE_SIZE - topLeftWorldX;
                    int drawY = ty * TILE_SIZE - topLeftWorldY;
                    g2.drawImage(tile, drawX, drawY, null);
                }
            }
        }

        // Draw numbered NaviGo pins (#05452D with #A8F62B highlight)
        for (int i = 0; i < Math.min(places.size(), 9); i++) {
            Place p = places.get(i);
            int px = (int) Math.round(lonToWorldX(p.lng, zoom) - topLeftWorldX);
            int py = (int) Math.round(latToWorldY(p.lat, zoom) - topLeftWorldY);
            drawNumberedPin(g2, px, py, String.valueOf(i + 1));
        }

        // Subtle OpenStreetMap attribution badge at bottom-right
        String credit = "© OpenStreetMap";
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int cw = g2.getFontMetrics().stringWidth(credit);
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillRoundRect(width - cw - 14, height - 22, cw + 10, 16, 6, 6);
        g2.setColor(new Color(0x18, 0x31, 0x2D));
        g2.drawString(credit, width - cw - 9, height - 10);

        g2.dispose();
        return new ImageIcon(canvas);
    }

    private BufferedImage downloadOsmTile(int zoom, int x, int y) {
        try {
            String url = String.format("https://tile.openstreetmap.org/%d/%d/%d.png", zoom, x, y);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<java.io.InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() == 200) {
                try (var in = res.body()) {
                    return ImageIO.read(in);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void drawNumberedPin(Graphics2D g2, int tipX, int tipY, String label) {
        int r = 13;
        int cy = tipY - 22;

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(tipX - 6, tipY - 3, 12, 6);

        // Pin pointer triangle
        Path2D pointer = new Path2D.Double();
        pointer.moveTo(tipX - 8, cy + 7);
        pointer.lineTo(tipX + 8, cy + 7);
        pointer.lineTo(tipX, tipY);
        pointer.closePath();
        g2.setColor(new Color(0x05, 0x45, 0x2D)); // Theme.PRIMARY
        g2.fill(pointer);

        // Pin head circle
        g2.fillOval(tipX - r, cy - r, r * 2, r * 2);
        g2.setColor(new Color(0xA8, 0xF6, 0x2B)); // Theme.LIME ring
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(tipX - r, cy - r, r * 2, r * 2);

        // Number label
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, tipX - fm.stringWidth(label) / 2, cy + fm.getAscent() / 2 - 2);
    }

    private static double lonToWorldX(double lon, int zoom) {
        return (lon + 180.0) / 360.0 * (TILE_SIZE << zoom);
    }

    private static double latToWorldY(double lat, int zoom) {
        double rad = Math.toRadians(lat);
        double mercator = Math.log(Math.tan(Math.PI / 4.0 + rad / 2.0));
        return (1.0 - mercator / Math.PI) / 2.0 * (TILE_SIZE << zoom);
    }

    private List<Place> parseNominatimJson(String json) {
        List<Place> results = new ArrayList<>();
        // Split each JSON object in the Nominatim array
        String[] objects = json.split("\\{\"place_id\":");

        Pattern idPat = Pattern.compile("^([0-9]+)");
        Pattern latPat = Pattern.compile("\"lat\"\\s*:\\s*\"([-0-9.]+)\"");
        Pattern lonPat = Pattern.compile("\"lon\"\\s*:\\s*\"([-0-9.]+)\"");
        Pattern namePat = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern displayPat = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern catPat = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");
        Pattern typePat = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");

        for (int i = 1; i < objects.length && results.size() < 10; i++) {
            String obj = objects[i];
            Matcher idM = idPat.matcher(obj);
            Matcher latM = latPat.matcher(obj);
            Matcher lonM = lonPat.matcher(obj);
            Matcher nameM = namePat.matcher(obj);
            Matcher dispM = displayPat.matcher(obj);
            Matcher catM = catPat.matcher(obj);
            Matcher typeM = typePat.matcher(obj);

            if (latM.find() && lonM.find()) {
                String id = idM.find() ? "osm_" + idM.group(1) : "osm_" + i;
                String displayName = dispM.find() ? dispM.group(1) : "Taiwan Location";
                String name = nameM.find() && !nameM.group(1).isBlank()
                        ? nameM.group(1)
                        : displayName.split(",")[0].trim();

                // Build a clean short area description from display_name
                String[] parts = displayName.split(",");
                String area = parts.length > 2
                        ? parts[1].trim() + " · " + parts[2].trim()
                        : (parts.length > 1 ? parts[1].trim() : "Taiwan");

                double lat = Double.parseDouble(latM.group(1));
                double lng = Double.parseDouble(lonM.group(1));
                String category = (catM.find() ? catM.group(1) : "") + " " + (typeM.find() ? typeM.group(1) : "");

                results.add(new Place(id, name, area, mapCategory(category), lat, lng));
            }
        }
        return results;
    }

    private String mapCategory(String raw) {
        String t = raw.toLowerCase(Locale.ROOT);
        if (t.contains("food") || t.contains("restaurant") || t.contains("cafe") || t.contains("bar")
                || t.contains("market") || t.contains("pub") || t.contains("bakery")) {
            return "food";
        }
        if (t.contains("park") || t.contains("natural") || t.contains("peak") || t.contains("garden")
                || t.contains("water") || t.contains("viewpoint") || t.contains("zoo")) {
            return "nature";
        }
        if (t.contains("museum") || t.contains("temple") || t.contains("worship") || t.contains("historic")
                || t.contains("heritage") || t.contains("gallery") || t.contains("monument")) {
            return "culture";
        }
        return "city";
    }
}
