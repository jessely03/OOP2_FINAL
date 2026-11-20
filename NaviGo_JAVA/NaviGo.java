import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class NaviGo extends JFrame {
    private static final Color INK = new Color(24, 49, 45);
    private static final Color GREEN = new Color(32, 89, 74);
    private static final Color LIME = new Color(226, 244, 146);
    private static final Color PAPER = new Color(246, 247, 239);
    private static final Color MINT = new Color(229, 239, 217);
    private static final Color MUTED = new Color(82, 102, 97);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final String[] SCREENS = {"Welcome", "Overview", "Trip Calendar", "Explore Map", "Expenses", "Group Chat"};

    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JPanel[] views = new JPanel[SCREENS.length];
    private final JButton[] links = new JButton[SCREENS.length];
    private final JComboBox<Trip> tripPicker = new JComboBox<>();
    private final JLabel breadcrumb = label("", 14, false);
    private final Image cover = new ImageIcon(new File("assets", "taipei.jpg").getPath()).getImage();
    private Trip trip;
    private LocalDate selected;
    private YearMonth month;
    private int current;

    public NaviGo() {
        super("NaviGo | Trip planner");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 620));
        setSize(1160, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(sidebar(), BorderLayout.WEST);
        add(workspace(), BorderLayout.CENTER);
        Trip demo = new Trip("Taiwan Trip", "Taipei, Taiwan",
                LocalDate.of(2025, 12, 20), LocalDate.of(2025, 12, 25), true);
        demo.activities.add(new Activity("Flight to Taipei", "Taoyuan International Airport", demo.start, LocalTime.of(9, 0)));
        demo.activities.add(new Activity("Visit Longshan Temple", "Wanhua District", demo.start, LocalTime.of(13, 0)));
        demo.activities.add(new Activity("Dinner at Taipei 101", "Xinyi District", demo.start, LocalTime.of(19, 0)));
        tripPicker.addItem(demo);
        selectTrip(demo);
        tripPicker.addActionListener(event -> selectTrip((Trip) tripPicker.getSelectedItem()));
        showPage(0);
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
        sidebar.setPreferredSize(new Dimension(186, 0));
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
        tripPicker.setPreferredSize(new Dimension(155, 32));
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
        selected = trip.start;
        month = YearMonth.from(selected);
        showPage(current);
    }

    private void showPage(int page) {
        current = page;
        breadcrumb.setText("Workspace / " + SCREENS[page]);
        for (int i = 0; i < links.length; i++) links[i].setBackground(i == page ? MINT : Color.WHITE);
        JPanel screen = switch (page) {
            case 0 -> welcome();
            case 1 -> overview();
            case 2 -> calendar();
            default -> preview(page);
        };
        views[page].removeAll();
        views[page].add(screen, BorderLayout.CENTER);
        pages.show(content, SCREENS[page]);
        views[page].revalidate();
        views[page].repaint();
    }

    private JPanel welcome() {
        JPanel screen = screen();
        screen.add(hero("NaviGo", "Plan together. Explore together.", "Get started", true,
                event -> showPage(1)));
        return screen;
    }

    private JPanel overview() {
        JPanel screen = screen();
        screen.add(heading("My Trips", "Pick a trip or start planning your next one.",
                button("+ New trip", GREEN, Color.WHITE, event -> createTrip())));
        screen.add(Box.createVerticalStrut(16));
        screen.add(hero(trip.name, trip.destination + "  •  " + range(), "Open itinerary", trip.demo,
                event -> showPage(2)));
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
        LocalDate first = trip.activities.stream().map(activity -> activity.date)
                .min(LocalDate::compareTo).orElse(trip.start);
        screen.add(columns(agenda(first), tools));
        return screen;
    }

    private JPanel calendar() {
        JPanel screen = screen();
        screen.add(heading("Trip Calendar", trip.name + "  •  " + range(),
                button("+ Add activity", GREEN, Color.WHITE, event -> editActivity(null, selected))));
        screen.add(Box.createVerticalStrut(16));

        JPanel card = card();
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(label(month.format(MONTH), 21, true), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.setOpaque(false);
        JButton previous = button("‹", MINT, INK, event -> moveMonth(-1));
        previous.getAccessibleContext().setAccessibleName("Previous month");
        JButton next = button("›", MINT, INK, event -> moveMonth(1));
        next.getAccessibleContext().setAccessibleName("Next month");
        controls.add(previous);
        controls.add(next);
        bar.add(controls, BorderLayout.EAST);
        card.add(bar);
        card.add(Box.createVerticalStrut(10));
        JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));
        grid.setOpaque(false);
        for (String day : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
            JLabel header = label(day, 12, true);
            header.setHorizontalAlignment(SwingConstants.CENTER);
            grid.add(header);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int slot = 0; slot < 42; slot++) {
            int number = slot - offset + 1;
            if (number < 1 || number > month.lengthOfMonth()) {
                grid.add(new JLabel());
                continue;
            }
            LocalDate date = month.atDay(number);
            boolean active = date.equals(selected);
            boolean planned = trip.activities.stream().anyMatch(activity -> activity.date.equals(date));
            JButton day = button(number + (planned ? " •" : ""),
                    active ? GREEN : inTrip(date) ? MINT : PAPER, active ? Color.WHITE : INK, event -> {
                        selected = date;
                        showPage(2);
                    });
            day.setMargin(new Insets(7, 2, 7, 2));
            day.getAccessibleContext().setAccessibleName(date.format(DAY) + (planned ? ", has activities" : ""));
            grid.add(day);
        }
        card.add(grid);
        card.add(Box.createVerticalStrut(8));
        card.add(label("Light green = trip dates    • = activity", 12, false));
        screen.add(columns(card, agenda(selected)));
        return screen;
    }

    private void moveMonth(int amount) {
        month = month.plusMonths(amount);
        selected = month.equals(YearMonth.from(trip.start)) ? trip.start : month.atDay(1);
        showPage(2);
    }

    private JPanel agenda(LocalDate date) {
        JPanel card = card();
        card.add(label(date.format(DAY), 21, true));
        ArrayList<Activity> activities = new ArrayList<>();
        for (Activity activity : trip.activities) if (activity.date.equals(date)) activities.add(activity);
        activities.sort(Comparator.comparing(activity -> activity.time));
        for (Activity activity : activities) {
            card.add(Box.createVerticalStrut(8));
            JButton row = button(activity.time + "   " + activity.name, PAPER, INK,
                    event -> editActivity(activity, date));
            row.setHorizontalAlignment(SwingConstants.LEFT);
            row.setToolTipText(activity.location);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(row);
            JLabel location = label(activity.location, 13, false);
            location.setForeground(MUTED);
            card.add(location);
        }
        if (activities.isEmpty()) card.add(label("No activities scheduled.", 14, false));
        return card;
    }

    private JPanel preview(int page) {
        JPanel screen = screen();
        screen.add(heading(SCREENS[page], trip.name, null));
        screen.add(Box.createVerticalStrut(16));
        JPanel card = card();
        card.add(label("Layout preview", 20, true));
        String[] rows = switch (page) {
            case 3 -> trip.demo ? new String[]{"Saved places", "1. Longshan Temple", "2. Taipei 101"} :
                    new String[]{"No saved places in this trip."};
            case 4 -> trip.demo ? new String[]{"Shared total: NT$240", "Alice   NT$60", "Daniel   NT$60", "You   NT$60", "Emma   NT$60"} :
                    new String[]{"No expenses in this trip."};
            default -> trip.demo ? new String[]{"Alice: Let’s meet at 9am!", "Daniel: I added a new cafe!"} :
                    new String[]{"No messages in this trip."};
        };
        for (String row : rows) {
            card.add(Box.createVerticalStrut(10));
            card.add(label(row, 16, false));
        }
        screen.add(card);
        return screen;
    }

    private void createTrip() {
        JTextField name = new JTextField(20);
        JTextField destination = new JTextField(20);
        JTextField start = new JTextField(LocalDate.now().toString());
        JTextField end = new JTextField(LocalDate.now().plusDays(2).toString());
        JPanel form = form("Trip name", name, "Destination", destination,
                "Start (YYYY-MM-DD)", start, "End (YYYY-MM-DD)", end);
        while (JOptionPane.showConfirmDialog(this, form, "Create a trip",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                LocalDate first = LocalDate.parse(start.getText().trim());
                LocalDate last = LocalDate.parse(end.getText().trim());
                if (name.getText().isBlank() || destination.getText().isBlank() || last.isBefore(first)) {
                    error("Enter a name and destination. The end date must follow or equal the start date.");
                    continue;
                }
                Trip created = new Trip(name.getText().trim(), destination.getText().trim(), first, last, false);
                tripPicker.addItem(created);
                tripPicker.setSelectedItem(created);
                return;
            } catch (DateTimeException invalid) {
                error("Use valid dates in YYYY-MM-DD format.");
            }
        }
    }

    private void editActivity(Activity existing, LocalDate date) {
        JTextField name = new JTextField(existing == null ? "" : existing.name, 20);
        JTextField place = new JTextField(existing == null ? "" : existing.location, 20);
        JTextField when = new JTextField((inTrip(date) ? date : trip.start).toString());
        JTextField time = new JTextField(existing == null ? "09:00" : existing.time.toString());
        JPanel form = form("Activity", name, "Location", place, "Date (YYYY-MM-DD)", when, "Time (HH:mm)", time);
        Object[] options = existing == null ? new String[]{"Save", "Cancel"} : new String[]{"Save", "Cancel", "Remove"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(this, form, existing == null ? "Add activity" : "Edit activity",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 2) {
                trip.activities.remove(existing);
                showPage(current);
                return;
            }
            if (choice != 0) return;
            try {
                LocalDate day = LocalDate.parse(when.getText().trim());
                LocalTime slot = LocalTime.parse(time.getText().trim());
                if (name.getText().isBlank() || place.getText().isBlank() || !inTrip(day)) {
                    error("Enter an activity and location. Choose a date within the trip.");
                    continue;
                }
                Activity replacement = new Activity(name.getText().trim(), place.getText().trim(), day, slot);
                if (existing == null) trip.activities.add(replacement);
                else trip.activities.set(trip.activities.indexOf(existing), replacement);
                selected = day;
                month = YearMonth.from(day);
                showPage(current);
                return;
            } catch (DateTimeException invalid) {
                error("Use a valid date and time, for example 2025-12-20 and 09:00.");
            }
        }
    }

    private boolean inTrip(LocalDate date) {
        return !date.isBefore(trip.start) && !date.isAfter(trip.end);
    }

    private String range() {
        return trip.start.format(RANGE) + " – " + trip.end.format(RANGE);
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private JPanel hero(String title, String subtitle, String action, boolean photo, ActionListener listener) {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
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
        copy.add(label(description, 14, false));
        panel.add(copy, BorderLayout.CENTER);
        if (action != null) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
            actions.setOpaque(false);
            actions.add(action);
            panel.add(actions, BorderLayout.EAST);
        }
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
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
            JTextField field = (JTextField) fields[i + 1];
            prompt.setLabelFor(field);
            panel.add(prompt);
            panel.add(field);
        }
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
        final ArrayList<Activity> activities = new ArrayList<>();

        Trip(String name, String destination, LocalDate start, LocalDate end, boolean demo) {
            this.name = name;
            this.destination = destination;
            this.start = start;
            this.end = end;
            this.demo = demo;
        }

        @Override public String toString() {
            return name;
        }
    }

    private static class Activity {
        final String name;
        final String location;
        final LocalDate date;
        final LocalTime time;

        Activity(String name, String location, LocalDate date, LocalTime time) {
            this.name = name;
            this.location = location;
            this.date = date;
            this.time = time;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NaviGo().setVisible(true));
    }
}
