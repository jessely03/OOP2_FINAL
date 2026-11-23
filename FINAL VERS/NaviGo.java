import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class NaviGo extends JFrame implements Navigator {
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private static final String[] SCREENS = {"Welcome", "Overview", "Trip Calendar", "Expenses", "Explore", "Group Chat"};

    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JPanel[] views = new JPanel[SCREENS.length];
    private final JButton[] links = new JButton[SCREENS.length];
    private final JComboBox<Trip> tripPicker = new JComboBox<>();
    private final JLabel pageTitle = label("", 24, true);

    private final CalendarPanel calendarPanel = new CalendarPanel();
    private final SplitBill expensesPanel = new SplitBill();
    private final GroupPage groupPage = new GroupPage(this);
    private final MapPanel mapPanel = new MapPanel();

    private final ArrayList<Trip> trips = new ArrayList<>();
    private Trip trip;
    private int current = 0;

    public NaviGo() {
        super("NaviGo");
        setIconImages(java.util.List.of(logoMark(16), logoMark(32), logoMark(64)));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 650));
        setSize(1160, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        configureTripPicker();
        add(sidebar(), BorderLayout.WEST);
        add(workspace(), BorderLayout.CENTER);

        Trip demo = new Trip("Taiwan Trip", "Taipei, Taiwan",
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), true);
        trips.add(demo);
        tripPicker.addItem(demo);
        selectTrip(demo);

        tripPicker.addActionListener(event -> selectTrip((Trip) tripPicker.getSelectedItem()));
        showPage(current);
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(24, 16, 0, 16));
        JLabel brand = label("NaviGo", 27, true);
        brand.setForeground(Theme.PRIMARY);
        brand.setIcon(new ImageIcon(logoMark(32)));
        brand.setIconTextGap(8);
        panel.add(brand);

        for (int i = 0; i < SCREENS.length; i++) {
            final int page = i;
            links[i] = button(SCREENS[i], Color.WHITE, Theme.INK, event -> showPage(page));
            links[i].setHorizontalAlignment(SwingConstants.LEFT);
            panel.add(links[i]);
        }

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBackground(Theme.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        sidebar.add(panel, BorderLayout.NORTH);
        return sidebar;
    }

    private static BufferedImage logoMark(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.scale(size / 32.0, size / 32.0);

        Path2D pin = new Path2D.Double();
        pin.moveTo(16, 2);
        pin.curveTo(23, 2, 29, 8, 29, 15);
        pin.curveTo(29, 21, 24, 25, 17, 30);
        pin.quadTo(16, 31, 15, 30);
        pin.curveTo(8, 25, 3, 21, 3, 15);
        pin.curveTo(3, 8, 9, 2, 16, 2);
        pin.closePath();
        graphics.setColor(Theme.PRIMARY);
        graphics.fill(pin);

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(9, 15);
        arrow.lineTo(22, 9);
        arrow.quadTo(24, 8, 23, 10);
        arrow.lineTo(17, 23);
        arrow.quadTo(16, 25, 15.5, 23);
        arrow.lineTo(14.5, 18);
        arrow.lineTo(9, 17);
        arrow.quadTo(7, 16.5, 9, 15);
        arrow.closePath();
        graphics.setColor(Theme.LIME);
        graphics.fill(arrow);

        graphics.dispose();
        return image;
    }

    private JPanel workspace() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(workspaceHeader(), BorderLayout.NORTH);

        for (int i = 0; i < SCREENS.length; i++) {
            views[i] = new JPanel(new BorderLayout());
            if (SCREENS[i].equals("Group Chat")) {
                content.add(views[i], SCREENS[i]);
                continue;
            }
            JScrollPane scroll = new JScrollPane(views[i]);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            content.add(scroll, SCREENS[i]);
        }

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel workspaceHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Theme.PRIMARY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.PRIMARY_HOVER),
                new EmptyBorder(12, 24, 12, 24)));

        JPanel identity = new JPanel(new BorderLayout(12, 0));
        identity.setOpaque(false);
        JPanel mark = new Theme.RoundedPanel(new GridBagLayout(), Theme.PALE, null, 12);
        mark.setPreferredSize(new Dimension(42, 42));
        mark.add(new JLabel(new ImageIcon(logoMark(28))));
        pageTitle.setForeground(Theme.WHITE);
        identity.add(mark, BorderLayout.WEST);
        identity.add(pageTitle, BorderLayout.CENTER);
        header.add(identity, BorderLayout.CENTER);
        return header;
    }

    private void configureTripPicker() {
        tripPicker.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new Theme.RoundedButton("", Theme.PALE, Theme.PRIMARY, false);
                arrow.setIcon(new Icon() {
                    @Override public int getIconWidth() { return 14; }
                    @Override public int getIconHeight() { return 14; }

                    @Override
                    public void paintIcon(Component component, Graphics graphics, int x, int y) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(Theme.PRIMARY);
                        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.drawPolyline(new int[]{x + 3, x + 7, x + 11},
                                new int[]{y + 5, y + 9, y + 5}, 3);
                        g.dispose();
                    }
                });
                arrow.setBorder(new EmptyBorder(4, 10, 6, 10));
                arrow.setFocusable(false);
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(hasFocus ? Theme.PALE : Theme.WHITE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        tripPicker.setPreferredSize(new Dimension(226, 42));
        tripPicker.setBackground(Theme.WHITE);
        tripPicker.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        tripPicker.getAccessibleContext().setAccessibleName("Choose trip");
        tripPicker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, selected, cellHasFocus);
                putClientProperty("html.disable", Boolean.TRUE);
                setText(value instanceof Trip option ? option.name : "");
                setFont(Theme.font(Font.BOLD, 13));
                setBorder(new EmptyBorder(7, 10, 7, 10));
                setBackground(selected ? Theme.PALE : Theme.WHITE);
                setOpaque(index >= 0);
                setForeground(Theme.INK);
                if (value instanceof Trip option) {
                    setToolTipText(option.name + " · " + option.destination);
                } else {
                    setToolTipText(null);
                }
                return this;
            }
        });
    }

    private void selectTrip(Trip item) {
        if (item == null) return;
        trip = item;
        showPage(current);
    }

    private void showPage(int page) {
        current = page;
        pageTitle.setText(SCREENS[page]);

        for (int i = 0; i < links.length; i++) {
            links[i].setBackground(i == page ? Theme.PRIMARY : Theme.WHITE);
            links[i].setForeground(i == page ? Theme.WHITE : Theme.INK);
        }

        JPanel screen = switch (page) {
            case 0 -> welcome();
            case 1 -> overview();
            case 2 -> {
                calendarPanel.setTripContext(trip, trip.name, trip.destination, trip.start, trip.end, trip.demo);
                yield calendarPanel;
            }
            case 3 -> {
                expensesPanel.setTripContext(
                        trip.name,
                        trip.destination,
                        trip.start,
                        trip.end,
                        trip.demo
                );
                yield expensesPanel;
            }
            case 4 -> {
                mapPanel.setDestination(trip.destination);
                yield mapPanel;
            }
            case 5 -> groupPage;
            default -> throw new IllegalArgumentException("Unknown page: " + page);
        };

        views[page].removeAll();
        views[page].add(screen, BorderLayout.CENTER);
        pages.show(content, SCREENS[page]);
        views[page].revalidate();
        views[page].repaint();
    }

    private JPanel welcome() {
        JPanel screen = screen();
        screen.add(hero(
                "NaviGo",
                "Plan together. Explore together.",
                "Get started",
                event -> showPage(1)
        ));
        screen.add(Box.createVerticalStrut(20));
        screen.add(heading("Recommended trips", "Find ideas for your next trip in Taiwan.", null));
        screen.add(Box.createVerticalStrut(14));

        JPanel suggestions = new JPanel(new GridLayout(1, 3, 12, 0));
        suggestions.setOpaque(false);
        suggestions.setAlignmentX(Component.LEFT_ALIGNMENT);
        suggestions.add(tripSuggestion("Tainan", "History & local food", "Anping Old Fort"));
        suggestions.add(tripSuggestion("Kaohsiung", "Harbor & public art", "Pier-2 Art Center"));
        suggestions.add(tripSuggestion("Taichung", "Art & architecture", "National Taichung Theater"));
        suggestions.setMaximumSize(new Dimension(Integer.MAX_VALUE, suggestions.getPreferredSize().height));
        screen.add(suggestions);
        return screen;
    }

    private JPanel tripSuggestion(String city, String theme, String highlight) {
        JPanel suggestion = card();
        suggestion.add(label(city, 20, true));
        suggestion.add(Box.createVerticalStrut(6));

        JLabel themeLabel = label(theme, 13, false);
        themeLabel.setForeground(Theme.TEXT_MUTED);
        suggestion.add(themeLabel);
        suggestion.add(Box.createVerticalStrut(6));
        suggestion.add(label(highlight, 13, false));
        suggestion.add(Box.createVerticalStrut(14));

        JButton plan = Theme.outlineButton("Plan trip");
        plan.getAccessibleContext().setAccessibleName("Plan a trip to " + city);
        plan.setAlignmentX(Component.LEFT_ALIGNMENT);
        plan.addActionListener(event -> createTrip(city));
        suggestion.add(plan);
        return suggestion;
    }
    private JPanel overview() {
        JPanel tripActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tripActions.setOpaque(false);
        tripActions.add(tripPicker);
        tripActions.add(button("+ New trip", Theme.PRIMARY, Theme.WHITE, event -> createTrip("")));
        JPanel screen = screen();
        screen.add(heading(
                "My Trips",
                "Choose a trip to plan your days, split costs, and coordinate with your group.",
                tripActions
        ));
        screen.add(Box.createVerticalStrut(16));

        screen.add(hero(
                trip.name,
                trip.destination + "  •  " + range(),
                "Open calendar",
                event -> showPage(2)
        ));
        screen.add(Box.createVerticalStrut(16));

        JPanel tools = card();
        tools.add(label("Trip tools", 19, true));
        tools.add(Box.createVerticalStrut(12));
        JPanel shortcuts = new JPanel(new GridLayout(2, 2, 8, 8));
        shortcuts.setOpaque(false);
        shortcuts.setAlignmentX(Component.LEFT_ALIGNMENT);
        shortcuts.add(tripShortcut(2, "Add activities to each day."));
        shortcuts.add(tripShortcut(3, "Record costs and see who owes what."));
        shortcuts.add(tripShortcut(4, "Find places near your destination."));
        shortcuts.add(tripShortcut(5, "Message your travel group."));
        tools.add(shortcuts);
        tools.setMaximumSize(new Dimension(Integer.MAX_VALUE, tools.getPreferredSize().height));
        screen.add(tools);
        return screen;
    }

    private JPanel tripShortcut(int page, String description) {
        JPanel shortcut = new Theme.RoundedPanel(new BorderLayout(0, 4), Theme.BG, Theme.BORDER, 14);
        shortcut.setBorder(new EmptyBorder(6, 8, 10, 8));
        JButton open = button(SCREENS[page], Theme.BG, Theme.PRIMARY, event -> showPage(page));
        open.setHorizontalAlignment(SwingConstants.LEFT);
        open.getAccessibleContext().setAccessibleDescription(description);
        JLabel detail = label(description, 12, false);
        detail.setForeground(Theme.TEXT_MUTED);
        detail.setBorder(new EmptyBorder(0, 13, 0, 8));
        shortcut.add(open, BorderLayout.NORTH);
        shortcut.add(detail, BorderLayout.CENTER);
        return shortcut;
    }

    private void createTrip(String city) {
        JDialog dialog = new JDialog(this, "Plan a trip", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JTextField name = tripField(city.isEmpty() ? "" : city + " Trip");
        JTextField destination = tripField(city.isEmpty() ? "" : city + ", Taiwan");
        LocalDate today = LocalDate.now();
        DatePicker start = new DatePicker(today);
        DatePicker end = new DatePicker(today.plusDays(2));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(22, 24, 22, 24));
        JLabel title = label(city.isEmpty() ? "Plan a trip" : "Plan a trip to " + city, 25, true);
        title.setForeground(Theme.WHITE);
        JLabel subtitle = label("Choose where and when you want to go.", 13, false);
        subtitle.setForeground(Theme.PALE);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JPanel details = new JPanel(new GridLayout(0, 1, 0, 13));
        details.setOpaque(false);
        details.add(tripInput("Trip name", name, name));
        details.add(tripInput("Destination", destination, destination));
        JPanel dates = new JPanel(new GridLayout(1, 2, 12, 0));
        dates.setOpaque(false);
        dates.add(tripInput("Start date", start, start.getField()));
        dates.add(tripInput("End date", end, end.getField()));
        details.add(dates);

        JLabel warning = label(" ", 13, false);
        warning.setForeground(Theme.ERROR);
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(warning, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = Theme.outlineButton("Cancel");
        JButton save = Theme.primaryButton("Create trip");
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            if (name.getText().isBlank() || destination.getText().isBlank()) {
                warning.setText("Enter a trip name and destination.");
                (name.getText().isBlank() ? name : destination).requestFocusInWindow();
                return;
            }
            try {
                LocalDate first = start.getDate();
                LocalDate last = end.getDate();
                if (last.isBefore(first)) {
                    warning.setText("End date must be on or after the start date.");
                    end.getField().requestFocusInWindow();
                    return;
                }
                Trip created = new Trip(name.getText().trim(), destination.getText().trim(), first, last, false);
                trips.add(created);
                tripPicker.addItem(created);
                tripPicker.setSelectedItem(created);
                if (current != 1) showPage(1);
                dialog.dispose();
            } catch (DateTimeException invalid) {
                warning.setText("Enter dates in YYYY-MM-DD format.");
            }
        });
        actions.add(cancel);
        actions.add(save);
        footer.add(actions, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));
        body.add(details, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(body, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static JTextField tripField(String value) {
        JTextField field = new JTextField(value, 26);
        field.setFont(Theme.font(Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(9, 11, 9, 11)));
        return field;
    }

    private static JPanel tripInput(String title, JComponent input, JComponent focusTarget) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel caption = label(title, 13, true);
        caption.setLabelFor(focusTarget);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }
    private String range() {
        return trip.start.format(RANGE) + " – " + trip.end.format(RANGE);
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private JPanel hero(String title, String subtitle, String action, ActionListener listener) {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                int width = getWidth(), height = getHeight();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, Math.max(1, width), height, Theme.PRIMARY_HOVER));
                g.fillRoundRect(0, 0, width, height, 24, 24);
                g.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(590, 224));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 224));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setBorder(new EmptyBorder(24, 28, 26, 28));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel heading = label(title, 44, true);
        heading.setForeground(Color.WHITE);
        JLabel detail = label(subtitle, 16, false);
        detail.setForeground(Color.WHITE);

        text.add(Box.createVerticalGlue());
        text.add(heading);
        text.add(Box.createVerticalStrut(6));
        text.add(detail);
        text.add(Box.createVerticalStrut(14));

        JButton cta = button(action, Theme.LIME, Theme.INK, listener);
        cta.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(cta);
        text.add(Box.createVerticalGlue());

        banner.add(text, BorderLayout.CENTER);
        return banner;
    }

    private static JPanel heading(String title, String description, JComponent action) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel copy = new JPanel(new GridLayout(0, 1));
        copy.setOpaque(false);
        copy.add(label(title, 32, true));
        JLabel desc = label(description, 14, false);
        desc.setForeground(Theme.TEXT_MUTED);
        copy.add(desc);
        panel.add(copy, BorderLayout.CENTER);

        if (action != null) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
            actions.setOpaque(false);
            actions.add(action);
            panel.add(actions, BorderLayout.EAST);
        }

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 8));
        return panel;
    }

    private static JPanel screen() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG);
        panel.setBorder(new EmptyBorder(14, 24, 28, 24));
        return panel;
    }

    private static JPanel card() {
        JPanel panel = new Theme.RoundedPanel(null, Theme.WHITE, Theme.BORDER, 18);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    @Override
    public void navigateTo(String pageName) {
        switch (pageName) {
            case HOME -> showPage(1);
            case CALENDAR, ITINERARY -> showPage(2);
            case SPLIT_BILL -> showPage(3);
            case MAP -> showPage(4);
            default -> throw new IllegalArgumentException("Unknown page: " + pageName);
        }
    }

    private JPanel explore() {
        JPanel panel = screen();
        panel.add(heading("Explore", trip.destination,
                googleMapButton(trip.destination)));
        panel.add(Box.createVerticalStrut(16));

        MapLocation location = MapLocation.forDestination(trip.destination);
        ExploreMap map = new ExploreMap(location);
        JPanel mapCard = card();
        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel instructions = label(location == null
                ? "Browse Taiwan or open your destination in Google Maps."
                : "Drag to move the map. Scroll or use the buttons to zoom.", 13, false);
        instructions.setForeground(Theme.TEXT_MUTED);
        controls.add(instructions, BorderLayout.CENTER);

        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        JButton zoomOut = Theme.outlineButton("−");
        JButton zoomIn = Theme.outlineButton("+");
        JButton reset = Theme.outlineButton("Reset view");
        zoomOut.getAccessibleContext().setAccessibleName("Zoom out");
        zoomIn.getAccessibleContext().setAccessibleName("Zoom in");
        zoomOut.addActionListener(event -> map.zoomBy(-1));
        zoomIn.addActionListener(event -> map.zoomBy(1));
        reset.addActionListener(event -> map.resetView());
        zoom.add(zoomOut);
        zoom.add(zoomIn);
        zoom.add(reset);
        controls.add(zoom, BorderLayout.EAST);
        mapCard.add(controls);
        mapCard.add(Box.createVerticalStrut(10));
        map.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapCard.add(map);
        JPanel credits = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        credits.setOpaque(false);
        credits.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton attribution = Theme.outlineButton("© OpenStreetMap contributors");
        attribution.addActionListener(event -> openInBrowser(URI.create("https://www.openstreetmap.org/copyright")));
        credits.add(attribution);
        mapCard.add(Box.createVerticalStrut(8));
        mapCard.add(credits);
        mapCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, mapCard.getPreferredSize().height));
        panel.add(mapCard);
        return panel;
    }

    private JButton googleMapButton(String destination) {
        JButton open = Theme.primaryButton("Open map");
        open.setToolTipText("Open " + destination + " in Google Maps");
        open.addActionListener(event -> openMap(destination));
        return open;
    }

    private record MapLocation(double latitude, double longitude) {
        private static MapLocation forDestination(String destination) {
            String city = destination.split(",", 2)[0].strip().toLowerCase(Locale.ROOT);
            return switch (city) {
                case "taipei", "taipei city" -> new MapLocation(25.0375, 121.5637);
                case "tainan", "tainan city" -> new MapLocation(22.9997, 120.2270);
                case "kaohsiung", "kaohsiung city" -> new MapLocation(22.6273, 120.3014);
                case "taichung", "taichung city" -> new MapLocation(24.1477, 120.6736);
                default -> null;
            };
        }
    }

    private static final class ExploreMap extends JPanel {
        private static final int TILE_SIZE = 256;
        private static final int MIN_ZOOM = 3;
        private static final int MAX_ZOOM = 17;
        private static final long CACHE_AGE_MS = 7L * 24 * 60 * 60 * 1000;
        private static final String TILE_URL = System.getProperty("navigo.tiles.url",
                "https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        private static final Path CACHE = Path.of(System.getProperty("user.home"), ".navigo", "map-cache",
                Integer.toHexString(TILE_URL.hashCode()));
        private static final MapLocation TAIWAN = new MapLocation(23.7, 120.95);

        private final MapLocation destination;
        private final BufferedImage pin = logoMark(32);
        private final Map<Tile, BufferedImage> images = new LinkedHashMap<>(32, 0.75f, true);
        private final Timer refreshTimer = new Timer(150, event -> loadVisibleTiles());
        private SwingWorker<Void, TileImage> loader;
        private boolean failed;
        private Point drag;
        private int zoom;
        private double x;
        private double y;

        private record Tile(int zoom, int x, int y) {}
        private record TileImage(Tile tile, BufferedImage image) {}

        ExploreMap(MapLocation destination) {
            this.destination = destination;
            setBackground(Theme.PALE);
            setPreferredSize(new Dimension(460, 365));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 365));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Interactive destination map");
            refreshTimer.setRepeats(false);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) { drag = event.getPoint(); }

                @Override public void mouseDragged(MouseEvent event) {
                    if (drag == null) return;
                    x = clampWorld(x + drag.x - event.getX());
                    y = clampWorld(y + drag.y - event.getY());
                    drag = event.getPoint();
                    repaint();
                }

                @Override public void mouseReleased(MouseEvent event) {
                    drag = null;
                    scheduleRefresh();
                }

                @Override public void mouseWheelMoved(MouseWheelEvent event) {
                    zoomBy(-event.getWheelRotation());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) { scheduleRefresh(); }
            });
            addHierarchyListener(event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                    scheduleRefresh();
                }
            });
            resetView();
        }

        void resetView() {
            MapLocation center = destination == null ? TAIWAN : destination;
            zoom = destination == null ? 7 : 12;
            x = mapX(center, zoom);
            y = mapY(center, zoom);
            scheduleRefresh();
        }

        void zoomBy(int amount) {
            int next = clamp(zoom + amount, MIN_ZOOM, MAX_ZOOM);
            if (next == zoom) return;
            double scale = Math.scalb(1.0, next - zoom);
            zoom = next;
            x = clampWorld(x * scale);
            y = clampWorld(y * scale);
            scheduleRefresh();
        }

        private double clampWorld(double coordinate) {
            return Math.max(0, Math.min(worldSize(zoom) - 1, coordinate));
        }

        private static int worldSize(int zoom) { return TILE_SIZE << zoom; }
        private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

        private static double mapX(MapLocation location, int zoom) {
            return (location.longitude() + 180) / 360 * worldSize(zoom);
        }

        private static double mapY(MapLocation location, int zoom) {
            double latitude = Math.toRadians(location.latitude());
            double mercator = Math.log(Math.tan(Math.PI / 4 + latitude / 2));
            return (1 - mercator / Math.PI) * worldSize(zoom) / 2;
        }

        private int left() { return (int) x - getWidth() / 2; }
        private int top() { return (int) y - getHeight() / 2; }

        private List<Tile> visibleTiles() {
            List<Tile> tiles = new ArrayList<>();
            int max = (1 << zoom) - 1;
            int right = Math.min(max, Math.floorDiv(left() + getWidth() - 1, TILE_SIZE));
            int bottom = Math.min(max, Math.floorDiv(top() + getHeight() - 1, TILE_SIZE));
            for (int row = Math.max(0, Math.floorDiv(top(), TILE_SIZE)); row <= bottom; row++) {
                for (int column = Math.max(0, Math.floorDiv(left(), TILE_SIZE)); column <= right; column++) {
                    tiles.add(new Tile(zoom, column, row));
                }
            }
            return tiles;
        }

        private void scheduleRefresh() {
            failed = false;
            repaint();
            refreshTimer.restart();
        }

        private void loadVisibleTiles() {
            if (!isShowing() || getWidth() < 1 || getHeight() < 1 || drag != null) return;
            List<Tile> missing = visibleTiles().stream().filter(tile -> !images.containsKey(tile)).toList();
            if (missing.isEmpty()) return;
            if (loader != null) loader.cancel(true);
            loader = new SwingWorker<>() {
                @Override protected Void doInBackground() {
                    for (Tile tile : missing) {
                        if (isCancelled()) break;
                        try {
                            publish(new TileImage(tile, readTile(tile)));
                        } catch (IOException | IllegalArgumentException unavailable) {
                            break;
                        }
                    }
                    return null;
                }

                @Override protected void process(List<TileImage> loaded) {
                    if (isCancelled()) return;
                    for (TileImage result : loaded) {
                        images.put(result.tile(), result.image());
                        if (images.size() > 96) images.remove(images.keySet().iterator().next());
                    }
                    repaint();
                }

                @Override protected void done() {
                    if (loader != this) return;
                    loader = null;
                    failed = !isCancelled() && missing.stream().anyMatch(tile -> !images.containsKey(tile));
                    repaint();
                }
            };
            loader.execute();
        }

        @Override public void removeNotify() {
            refreshTimer.stop();
            if (loader != null) loader.cancel(true);
            loader = null;
            super.removeNotify();
        }

        private static BufferedImage readTile(Tile tile) throws IOException {
            Path file = CACHE.resolve(tile.zoom() + "-" + tile.x() + "-" + tile.y() + ".png");
            Files.createDirectories(CACHE);
            if (Files.isRegularFile(file) && System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis()
                    < CACHE_AGE_MS) {
                BufferedImage cached = ImageIO.read(file.toFile());
                if (cached != null) return cached;
            }

            String address = TILE_URL.replace("{z}", Integer.toString(tile.zoom()))
                    .replace("{x}", Integer.toString(tile.x())).replace("{y}", Integer.toString(tile.y()));
            URLConnection request = URI.create(address).toURL().openConnection();
            request.setRequestProperty("User-Agent", "NaviGo-Swing-Trip-Planner/1.0");
            request.setConnectTimeout(4000);
            request.setReadTimeout(4000);
            BufferedImage image;
            try (var stream = request.getInputStream()) {
                image = ImageIO.read(stream);
            }
            if (image == null) throw new IOException("Invalid map tile");
            Path temporary = Files.createTempFile(CACHE, "tile-", ".png");
            try {
                ImageIO.write(image, "png", temporary.toFile());
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return image;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            boolean hasTile = false;
            for (Tile tile : visibleTiles()) {
                BufferedImage image = images.get(tile);
                if (image != null) {
                    graphics.drawImage(image, tile.x() * TILE_SIZE - left(),
                            tile.y() * TILE_SIZE - top(), this);
                    hasTile = true;
                }
            }
            if (hasTile && destination != null) {
                graphics.drawImage(pin, (int) mapX(destination, zoom) - left() - 16,
                        (int) mapY(destination, zoom) - top() - 30, this);
            }
            if (!hasTile || failed) {
                String message = failed ? "Map tiles unavailable. Try Reset view or Open map." : "Loading map…";
                graphics.setFont(Theme.font(Font.BOLD, 13));
                int width = graphics.getFontMetrics().stringWidth(message);
                graphics.setColor(Theme.WHITE);
                graphics.fillRoundRect(12, 12, width + 20, 30, 12, 12);
                graphics.setColor(Theme.INK);
                graphics.drawString(message, 22, 32);
            }
        }
    }
    private void openMap(String query) {
        String destination = URLEncoder.encode(query, StandardCharsets.UTF_8);
        openInBrowser(URI.create("https://www.google.com/maps/search/?api=1&query=" + destination));
    }

    private void openInBrowser(URI link) {
        try {
            Desktop.getDesktop().browse(link);
        } catch (IOException | UnsupportedOperationException failure) {
            error("Could not open the link.\n" + link);
        }
    }
    private static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(Theme.INK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton button(String text, Color fill, Color color, ActionListener listener) {
        JButton button = new Theme.RoundedButton(text, fill, color, false);
        button.addActionListener(listener);
        return button;
    }

    private static class Trip {
        final String name;
        final String destination;
        final LocalDate start;
        final LocalDate end;
        final boolean demo;

        Trip(String name, String destination, LocalDate start, LocalDate end, boolean demo) {
            this.name = name;
            this.destination = destination;
            this.start = start;
            this.end = end;
            this.demo = demo;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.applyDefaults();
            new NaviGo().setVisible(true);
        });
    }
}

