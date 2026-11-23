import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MapPanel extends JPanel {

    private final GoogleMapsService mapsService = new GoogleMapsService();
    private final Set<String> savedPlaceIds = new HashSet<>(Set.of("elephant", "raohe"));
    private List<GoogleMapsService.Place> currentResults = new ArrayList<>();
    private String currentDestination = "Taipei, Taiwan";
    private String activeCategory = "all";

    private final JLabel destinationSubtitle = new JLabel("Taipei, Taiwan");
    private final JTextField searchField = new JTextField();
    private final JPanel cardsGrid = new JPanel(new GridLayout(0, 2, 12, 12));
    private final JLabel mapImageLabel = new JLabel("Loading Google Map...", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Ready · Search places via Google Maps");

    private Consumer<GoogleMapsService.Place> onPlanStop;

    public MapPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(14, 24, 28, 24));

        add(createTopSection(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        setDestination("Taipei, Taiwan");
    }

    /**
     * Called by NaviGo.java whenever the user switches trips or opens Explore
     */
    public void setDestination(String destination) {
        if (destination == null || destination.isBlank()) {
            destination = "Taipei, Taiwan";
        }
        this.currentDestination = destination;
        this.destinationSubtitle.setText(destination + " · Search places and add stops to your itinerary");
        this.activeCategory = "all";
        this.searchField.setText("");

        this.currentResults = new ArrayList<>(mapsService.getStarterPlacesFor(destination));
        renderCards();
        refreshMapPreview(currentResults);
    }

    public void setOnPlanStop(Consumer<GoogleMapsService.Place> callback) {
        this.onPlanStop = callback;
    }

    private JPanel createTopSection() {
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);

        // Matches NaviGo.heading(...) layout
        JPanel headerRow = new JPanel(new BorderLayout(12, 0));
        headerRow.setOpaque(false);

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
        titles.setOpaque(false);
        JLabel title = new JLabel("Explore");
        title.setFont(Theme.font(Font.BOLD, 32));
        title.setForeground(Theme.INK);

        destinationSubtitle.setFont(Theme.font(Font.PLAIN, 14));
        destinationSubtitle.setForeground(Theme.TEXT_MUTED);
        titles.add(title);
        titles.add(destinationSubtitle);

        JButton openExternalMap = Theme.primaryButton("Open in Google Maps ↗");
        openExternalMap.addActionListener(e -> {
            String encoded = java.net.URLEncoder.encode(currentDestination, java.nio.charset.StandardCharsets.UTF_8);
            openInBrowser("https://www.google.com/maps/search/?api=1&query=" + encoded);
        });

        headerRow.add(titles, BorderLayout.CENTER);
        headerRow.add(openExternalMap, BorderLayout.EAST);

        // Search & Filter Bar inside a Theme.RoundedPanel card
        JPanel searchBarCard = new Theme.RoundedPanel(new BorderLayout(12, 10), Theme.WHITE, Theme.BORDER, 18);
        searchBarCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        searchField.setFont(Theme.font(Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search night markets, temples, cafes, viewpoints...");

        JButton searchBtn = Theme.primaryButton("Search Places");
        searchBtn.addActionListener(e -> runGooglePlacesSearch(searchField.getText()));
        searchField.addActionListener(e -> runGooglePlacesSearch(searchField.getText()));

        inputRow.add(searchField, BorderLayout.CENTER);
        inputRow.add(searchBtn, BorderLayout.EAST);

        // Category Filter Chips
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setOpaque(false);
        String[][] filters = {
                {"all", "All spots"},
                {"food", "♨ Food & Night Markets"},
                {"culture", "✦ Culture"},
                {"nature", "▲ Nature"},
                {"city", "◈ City"}
        };
        for (String[] f : filters) {
            JButton chip = Theme.outlineButton(f[1]);
            chip.addActionListener(e -> {
                activeCategory = f[0];
                renderCards();
            });
            filterRow.add(chip);
        }

        searchBarCard.add(inputRow, BorderLayout.NORTH);
        searchBarCard.add(filterRow, BorderLayout.SOUTH);

        top.add(headerRow, BorderLayout.NORTH);
        top.add(searchBarCard, BorderLayout.SOUTH);
        return top;
    }

    private JComponent createMainContent() {
        JPanel split = new JPanel(new GridLayout(1, 2, 16, 0));
        split.setOpaque(false);

        // Left Column: Place Research Cards
        cardsGrid.setBackground(Theme.BG);
        JPanel cardsWrapper = new JPanel(new BorderLayout());
        cardsWrapper.setOpaque(false);
        cardsWrapper.add(cardsGrid, BorderLayout.NORTH);

        // Right Column: Google Static Map Card
        JPanel mapCard = new Theme.RoundedPanel(new BorderLayout(0, 10), Theme.WHITE, Theme.BORDER, 18);
        mapCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel mapTop = new JPanel(new BorderLayout());
        mapTop.setOpaque(false);
        JLabel mapTitle = new JLabel("Destination Map & Pins");
        mapTitle.setFont(Theme.font(Font.BOLD, 18));
        mapTitle.setForeground(Theme.INK);

        JButton resetPins = Theme.outlineButton("Show All Pins");
        resetPins.addActionListener(e -> refreshMapPreview(currentResults));
        mapTop.add(mapTitle, BorderLayout.WEST);
        mapTop.add(resetPins, BorderLayout.EAST);

        mapImageLabel.setOpaque(true);
        mapImageLabel.setBackground(Theme.PALE);
        mapImageLabel.setForeground(Theme.INK);
        mapImageLabel.setFont(Theme.font(Font.BOLD, 13));
        mapImageLabel.setPreferredSize(new Dimension(440, 380));
        mapImageLabel.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        statusLabel.setFont(Theme.font(Font.PLAIN, 12));
        statusLabel.setForeground(Theme.TEXT_MUTED);

        mapCard.add(mapTop, BorderLayout.NORTH);
        mapCard.add(mapImageLabel, BorderLayout.CENTER);
        mapCard.add(statusLabel, BorderLayout.SOUTH);

        split.add(cardsWrapper);
        split.add(mapCard);
        return split;
    }

    private void renderCards() {
        cardsGrid.removeAll();
        List<GoogleMapsService.Place> filtered = currentResults.stream()
                .filter(p -> activeCategory.equals("all") || p.type.equals(activeCategory))
                .toList();

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("No places match this category.");
            empty.setFont(Theme.font(Font.PLAIN, 13));
            empty.setForeground(Theme.TEXT_MUTED);
            cardsGrid.add(empty);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                cardsGrid.add(createPlaceCard(filtered.get(i), i + 1));
            }
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private JPanel createPlaceCard(GoogleMapsService.Place place, int pinIndex) {
        JPanel card = new Theme.RoundedPanel(new BorderLayout(0, 10), Theme.WHITE, Theme.BORDER, 16);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel tag = new JLabel("#" + pinIndex + " · " + place.getCategoryLabel());
        tag.setFont(Theme.font(Font.BOLD, 11));
        tag.setForeground(Theme.PRIMARY);

        JLabel name = new JLabel(place.name);
        name.setFont(Theme.font(Font.BOLD, 15));
        name.setForeground(Theme.INK);

        JLabel area = new JLabel(place.area);
        area.setFont(Theme.font(Font.PLAIN, 12));
        area.setForeground(Theme.TEXT_MUTED);

        info.add(tag);
        info.add(Box.createVerticalStrut(4));
        info.add(name);
        info.add(Box.createVerticalStrut(2));
        info.add(area);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setOpaque(false);

        boolean isSaved = savedPlaceIds.contains(place.id);
        JButton saveBtn = Theme.outlineButton(isSaved ? "★ Saved" : "☆ Save");
        saveBtn.setFont(Theme.font(Font.BOLD, 11));
        saveBtn.setBorder(new EmptyBorder(6, 9, 6, 9));
        saveBtn.addActionListener(e -> {
            if (savedPlaceIds.contains(place.id)) {
                savedPlaceIds.remove(place.id);
                statusLabel.setText("Removed " + place.name + " from saved places.");
            } else {
                savedPlaceIds.add(place.id);
                statusLabel.setText("Saved " + place.name + " to trip!");
            }
            renderCards();
        });

        JButton planBtn = Theme.primaryButton("+ Plan");
        planBtn.setFont(Theme.font(Font.BOLD, 11));
        planBtn.setBorder(new EmptyBorder(6, 9, 6, 9));
        planBtn.addActionListener(e -> {
            statusLabel.setText("Planned stop: " + place.name);
            if (onPlanStop != null) {
                onPlanStop.accept(place);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Added \"" + place.name + "\" to " + currentDestination + " itinerary!",
                        "Stop Planned", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton pinBtn = Theme.outlineButton("Pin");
        pinBtn.setFont(Theme.font(Font.BOLD, 11));
        pinBtn.setBorder(new EmptyBorder(6, 9, 6, 9));
        pinBtn.addActionListener(e -> {
            statusLabel.setText("Centered map on " + place.name);
            refreshMapPreview(List.of(place));
        });

        JButton routeBtn = Theme.outlineButton("Transit ↗");
        routeBtn.setFont(Theme.font(Font.BOLD, 11));
        routeBtn.setBorder(new EmptyBorder(6, 9, 6, 9));
        routeBtn.addActionListener(e -> openInBrowser(
                place.getGoogleDirectionsUrl(GoogleMapsService.LatLng.forDestination(currentDestination))
        ));

        actions.add(saveBtn);
        actions.add(planBtn);
        actions.add(pinBtn);
        actions.add(routeBtn);

        card.add(info, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void runGooglePlacesSearch(String query) {
        if (query == null || query.isBlank()) {
            currentResults = new ArrayList<>(mapsService.getStarterPlacesFor(currentDestination));
            renderCards();
            refreshMapPreview(currentResults);
            return;
        }

        statusLabel.setText("Searching Google Maps for \"" + query.trim() + "\"...");
        new SwingWorker<List<GoogleMapsService.Place>, Void>() {
            @Override
            protected List<GoogleMapsService.Place> doInBackground() throws Exception {
                return mapsService.searchPlaces(query.trim(), currentDestination);
            }

            @Override
            protected void done() {
                try {
                    List<GoogleMapsService.Place> found = get();
                    if (found.isEmpty()) {
                        statusLabel.setText("No results found. Check your API key or try another search.");
                    } else {
                        currentResults = found;
                        activeCategory = "all";
                        renderCards();
                        refreshMapPreview(found);
                        statusLabel.setText("Showing " + found.size() + " Google Maps results near " + currentDestination);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Search error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void refreshMapPreview(List<GoogleMapsService.Place> places) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return mapsService.fetchStaticMap(places, currentDestination, 460, 380);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        mapImageLabel.setText("");
                        mapImageLabel.setIcon(icon);
                    } else {
                        mapImageLabel.setText("Map preview unavailable — verify Maps Static API key.");
                    }
                } catch (Exception e) {
                    mapImageLabel.setText("Map load error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ex) {
            statusLabel.setText("Could not open browser: " + ex.getMessage());
        }
    }
}