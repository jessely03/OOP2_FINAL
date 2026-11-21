import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class NaviGo extends JFrame {
    private static final Color INK = new Color(24, 49, 45);
    private static final Color GREEN = new Color(32, 89, 74);
    private static final Color LIME = new Color(226, 244, 146);
    private static final Color PAPER = new Color(246, 247, 239);
    private static final Color MINT = new Color(229, 239, 217);
    private static final Color MUTED = new Color(82, 102, 97);
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private static final String[] SCREENS = {"Welcome", "Overview", "Trip Calendar", "Expenses", "Explore", "Group Chat"};

    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JPanel[] views = new JPanel[SCREENS.length];
    private final JButton[] links = new JButton[SCREENS.length];
    private final JComboBox<Trip> tripPicker = new JComboBox<>();
    private final JLabel breadcrumb = label("", 14, false);
    private final Image cover = new ImageIcon(new File("assets", "taipei.jpg").getPath()).getImage();

    // =========================================================================
    // [OPSIONAL / KOMENTAR] Aktifkan jika file panel terpisah sudah tersedia:
    private final CalendarPanel calendarPanel = new CalendarPanel();
    private final splitbill expensesPanel = new splitbill();
    // private final ExplorePanel explorePanel = new ExplorePanel();
    // private final GroupChatPanel groupChatPanel = new GroupChatPanel();
    // =========================================================================

    private final ArrayList<Trip> trips = new ArrayList<>();
    private Trip trip;
    private int current = 0;

    public NaviGo() {
        super("NaviGo");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setSize(1160, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

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
        panel.add(label("✦  NaviGo.", 27, true));

        for (int i = 0; i < SCREENS.length; i++) {
            final int page = i;
            links[i] = button(SCREENS[i], Color.WHITE, INK, event -> showPage(page));
            links[i].setHorizontalAlignment(SwingConstants.LEFT);
            panel.add(links[i]);
        }

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.add(panel, BorderLayout.NORTH);
        return sidebar;
    }

    private JPanel workspace() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(PAPER);
        top.setBorder(new EmptyBorder(15, 24, 10, 24));
        top.add(breadcrumb, BorderLayout.WEST);

        tripPicker.setPreferredSize(new Dimension(170, 32));
        tripPicker.getAccessibleContext().setAccessibleName("Active trip");
        top.add(tripPicker, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        for (int i = 0; i < SCREENS.length; i++) {
            views[i] = new JPanel(new BorderLayout());
            JScrollPane scroll = new JScrollPane(views[i]);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            content.add(scroll, SCREENS[i]);
        }

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void selectTrip(Trip item) {
        if (item == null) return;
        trip = item;
        showPage(current);
    }

    private void showPage(int page) {
        current = page;
        breadcrumb.setText("Workspace / " + SCREENS[page]);

        for (int i = 0; i < links.length; i++) {
            links[i].setBackground(i == page ? MINT : Color.WHITE);
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
                // explorePanel.refresh(...);
                // yield explorePanel;
                yield placeholder(SCREENS[page]);
            }
            case 5 -> {
                // groupChatPanel.refresh(...);
                // yield groupChatPanel;
                yield placeholder(SCREENS[page]);
            }
            default -> placeholder(SCREENS[page]);
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
                true,
                event -> showPage(1)
        ));
        return screen;
    }

    private JPanel overview() {
        JPanel screen = screen();
        screen.add(heading(
                "My Trips",
                "Pick a trip or start planning your next one.",
                button("+ New trip", GREEN, Color.WHITE, event -> createTrip())
        ));
        screen.add(Box.createVerticalStrut(16));

        screen.add(hero(
                trip.name,
                trip.destination + "  •  " + range(),
                "Open expenses",
                trip.demo,
                event -> showPage(3)
        ));
        screen.add(Box.createVerticalStrut(16));

        JPanel tools = card();
        tools.add(label("Trip tools", 21, true));
        for (int i = 2; i < SCREENS.length; i++) {
            final int page = i;
            tools.add(Box.createVerticalStrut(8));
            JButton link = button(SCREENS[i], MINT, INK, event -> showPage(page));
            link.setAlignmentX(Component.LEFT_ALIGNMENT);
            tools.add(link);
        }

        JPanel summary = card();
        summary.add(label("Active trip details", 21, true));
        summary.add(Box.createVerticalStrut(10));
        summary.add(label("Trip: " + trip.name, 15, true));
        summary.add(Box.createVerticalStrut(6));
        summary.add(label("Destination: " + trip.destination, 14, false));
        summary.add(Box.createVerticalStrut(6));
        summary.add(label("Dates: " + range(), 14, false));

        screen.add(columns(summary, tools));
        return screen;
    }

    private void createTrip() {
        JTextField name = new JTextField(20);
        JTextField destination = new JTextField(20);
        DatePicker start = new DatePicker(LocalDate.now());
        DatePicker end = new DatePicker(LocalDate.now().plusDays(2));
        JPanel form = form("Trip name", name, "Destination", destination,
                "Start (YYYY-MM-DD)", start, "End (YYYY-MM-DD)", end);

        while (JOptionPane.showConfirmDialog(this, form, "Create a trip",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                LocalDate first = LocalDate.parse(start.getText());
                LocalDate last = LocalDate.parse(end.getText());
                if (name.getText().isBlank() || destination.getText().isBlank() || last.isBefore(first)) {
                    error("Enter a name and destination. The end date must follow or equal the start date.");
                    continue;
                }
                Trip created = new Trip(name.getText().trim(), destination.getText().trim(), first, last, false);
                trips.add(created);
                tripPicker.addItem(created);
                tripPicker.setSelectedItem(created);
                return;
            } catch (DateTimeException invalid) {
                error("Use valid dates in YYYY-MM-DD format.");
            }
        }
    }

    private String range() {
        return trip.start.format(RANGE) + " – " + trip.end.format(RANGE);
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private JPanel hero(String title, String subtitle, String action, boolean photo, ActionListener listener) {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                int width = getWidth(), height = getHeight();
                g.setColor(GREEN);
                g.fillRect(0, 0, width, height);
                int imageWidth = cover.getWidth(null), imageHeight = cover.getHeight(null);
                if (photo && imageWidth > 0 && imageHeight > 0) {
                    double scale = Math.max((double) width / imageWidth, (double) height / imageHeight);
                    int w = (int) Math.ceil(imageWidth * scale), h = (int) Math.ceil(imageHeight * scale);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(cover, (width - w) / 2, (height - h) / 2, w, h, null);
                }
                g.setPaint(new GradientPaint(0, 0, new Color(13, 35, 30, 245),
                        Math.max(1, width), 0, new Color(13, 35, 30, 24)));
                g.fillRect(0, 0, width, height);
                g.dispose();
            }
        };
        banner.setPreferredSize(new Dimension(650, 300));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setBorder(new EmptyBorder(24, 28, 26, 28));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel heading = label(title, 44, true);
        heading.setForeground(Color.WHITE);
        JLabel detail = label(subtitle, 16, false);
        detail.setForeground(Color.WHITE);

        text.add(heading);
        text.add(Box.createVerticalStrut(6));
        text.add(detail);
        text.add(Box.createVerticalStrut(14));

        JButton cta = button(action, LIME, INK, listener);
        cta.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(cta);

        banner.add(text, BorderLayout.SOUTH);
        return banner;
    }

    private static JPanel heading(String title, String description, JButton action) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel copy = new JPanel(new GridLayout(0, 1));
        copy.setOpaque(false);
        copy.add(label(title, 32, true));
        JLabel desc = label(description, 14, false);
        desc.setForeground(MUTED);
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

    private static JPanel columns(JPanel left, JPanel right) {
        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0));
        columns.setAlignmentX(Component.LEFT_ALIGNMENT);
        columns.setOpaque(false);
        for (JPanel card : new JPanel[]{left, right}) {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(card, BorderLayout.NORTH);
            columns.add(wrapper);
        }
        return columns;
    }

    private static JPanel screen() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PAPER);
        panel.setBorder(new EmptyBorder(14, 24, 28, 24));
        return panel;
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    private static JPanel form(Object... fields) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 5));
        for (int i = 0; i < fields.length; i += 2) {
            JLabel prompt = new JLabel((String) fields[i]);
            JComponent field = (JComponent) fields[i + 1];
            prompt.setLabelFor(field);
            panel.add(prompt);
            panel.add(field);
        }
        return panel;
    }

    private static JPanel placeholder(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PAPER);
        panel.setBorder(new EmptyBorder(20, 24, 28, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(label(title, 22, true));
        card.add(Box.createVerticalStrut(6));
        card.add(label("Panel file for " + title + " is commented out / not attached yet.", 14, false));

        panel.add(card);
        return panel;
    }

    private static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(INK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton button(String text, Color fill, Color color, ActionListener listener) {
        JButton button = new JButton(text);
        button.putClientProperty("html.disable", Boolean.TRUE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(color);
        button.setBackground(fill);
        button.setMargin(new Insets(8, 12, 8, 12));
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        SwingUtilities.invokeLater(() -> new NaviGo().setVisible(true));
    }
}
