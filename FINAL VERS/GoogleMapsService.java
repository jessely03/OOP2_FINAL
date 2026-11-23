import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMapsService {

    // Replace with your Google Cloud API Key (enable Places API & Maps Static API)
    private static final String API_KEY = System.getenv().getOrDefault(
            "GOOGLE_MAPS_API_KEY", "AIzaSyAMOGdy_sbG6N16dX7ra3uk5fP1-iKP_8w"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

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
     * Searches Google Maps Places API biased around the selected trip's destination
     */
    public List<Place> searchPlaces(String query, String destination) throws Exception {
        LatLng center = LatLng.forDestination(destination);
        String cityPrefix = destination != null && !destination.isBlank() ? destination + " " : "Taiwan ";
        String fullQuery = query.toLowerCase(Locale.ROOT).contains(cityPrefix.trim().toLowerCase(Locale.ROOT))
                ? query : cityPrefix + query;

        String encodedQuery = URLEncoder.encode(fullQuery, StandardCharsets.UTF_8);
        String url = String.format(Locale.ROOT,
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&location=%f,%f&radius=12000&key=%s",
                encodedQuery, center.lat(), center.lng(), API_KEY
        );

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Google Places API HTTP " + response.statusCode());
        }
        return parsePlacesJson(response.body());
    }

    /**
     * Renders a Google Static Map PNG styled with Theme.PRIMARY (0x05452D) pins
     */
    public ImageIcon fetchStaticMap(List<Place> places, String destination, int width, int height) throws Exception {
        LatLng center = LatLng.forDestination(destination);
        StringBuilder mapUrl = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap?");
        mapUrl.append("size=").append(width).append("x").append(height).append("&scale=2&maptype=roadmap");

        if (places.isEmpty()) {
            mapUrl.append(String.format(Locale.ROOT, "&center=%f,%f&zoom=13", center.lat(), center.lng()));
        } else if (places.size() == 1) {
            Place p = places.get(0);
            mapUrl.append(String.format(Locale.ROOT, "&center=%f,%f&zoom=16", p.lat, p.lng));
        }

        // Pin color matches Theme.PRIMARY (#05452D)
        for (int i = 0; i < Math.min(places.size(), 9); i++) {
            Place p = places.get(i);
            String marker = String.format(Locale.ROOT, "color:0x05452D|label:%d|%f,%f", i + 1, p.lat, p.lng);
            mapUrl.append("&markers=").append(URLEncoder.encode(marker, StandardCharsets.UTF_8));
        }

        mapUrl.append("&key=").append(API_KEY);

        BufferedImage img = ImageIO.read(new URL(mapUrl.toString()));
        if (img == null) return null;
        return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private List<Place> parsePlacesJson(String json) {
        List<Place> results = new ArrayList<>();
        String[] blocks = json.split("\"formatted_address\"\\s*:");

        Pattern namePat = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern idPat = Pattern.compile("\"place_id\"\\s*:\\s*\"([^\"]+)\"");
        Pattern locPat = Pattern.compile("\"location\"\\s*:\\s*\\{\\s*\"lat\"\\s*:\\s*([-0-9.]+),\\s*\"lng\"\\s*:\\s*([-0-9.]+)");
        Pattern typesPat = Pattern.compile("\"types\"\\s*:\\s*\\[([^\\]]+)\\]");
        Pattern ratePat = Pattern.compile("\"rating\"\\s*:\\s*([0-9.]+)");

        for (int i = 1; i < blocks.length && results.size() < 10; i++) {
            String block = blocks[i];
            String address = block.split("\"")[1].replace(", Taiwan", "");

            Matcher nameM = namePat.matcher(block);
            Matcher idM = idPat.matcher(block);
            Matcher locM = locPat.matcher(block);
            Matcher typesM = typesPat.matcher(block);
            Matcher rateM = ratePat.matcher(block);

            if (nameM.find() && locM.find()) {
                String id = idM.find() ? idM.group(1) : "gp_" + i;
                String name = nameM.group(1);
                double lat = Double.parseDouble(locM.group(1));
                double lng = Double.parseDouble(locM.group(2));
                String rawTypes = typesM.find() ? typesM.group(1) : "";
                String rating = rateM.find() ? "★ " + rateM.group(1) + " · " : "";

                results.add(new Place(id, name, rating + address, mapCategory(rawTypes), lat, lng));
            }
        }
        return results;
    }

    private String mapCategory(String types) {
        String t = types.toLowerCase(Locale.ROOT);
        if (t.contains("restaurant") || t.contains("food") || t.contains("cafe") || t.contains("bar")) return "food";
        if (t.contains("park") || t.contains("natural_feature") || t.contains("zoo")) return "nature";
        if (t.contains("museum") || t.contains("place_of_worship") || t.contains("art_gallery")) return "culture";
        return "city";
    }

}
