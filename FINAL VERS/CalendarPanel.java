import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class CalendarPanel extends JPanel {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final String[] WEEK = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private record Activity(LocalDate date, LocalTime start, LocalTime end, String name, String location) {}

    private final Map<Object, List<Activity>> schedules = new HashMap<>();
    private final JLabel context = label("", 14, false);
    private final JLabel monthTitle = label("", 20, true);
    private final JLabel dayTitle = label("", 18, true);
    private final JPanel days = new JPanel(new GridLayout(7, 7, 5, 5));
    private final JPanel agenda = new JPanel();
    private LocalDate selected;
    private YearMonth month;
    private List<Activity> activities;

    public CalendarPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 24, 24, 24));

        JPanel header = card(new BorderLayout(10, 5));
        header.add(label("Trip Calendar", 26, true), BorderLayout.WEST);
        header.add(button("+ Add activity", () -> edit(null)), BorderLayout.EAST);
        header.add(context, BorderLayout.SOUTH);
        add(header);
        add(Box.createVerticalStrut(12));

        JPanel calendar = card(new BorderLayout(0, 10));
        JPanel navigation = new JPanel(new BorderLayout());
        navigation.setOpaque(false);
        monthTitle.setHorizontalAlignment(SwingConstants.CENTER);
        JButton previous = outlineButton("‹", () -> changeMonth(-1));
        previous.getAccessibleContext().setAccessibleName("Previous month");
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(monthTitle, BorderLayout.CENTER);
        JButton next = outlineButton("›", () -> changeMonth(1));
        next.getAccessibleContext().setAccessibleName("Next month");
        navigation.add(next, BorderLayout.EAST);
        days.setOpaque(false);
        calendar.add(navigation, BorderLayout.NORTH);
        calendar.add(days, BorderLayout.CENTER);
        add(calendar);
        add(Box.createVerticalStrut(12));

        JPanel daily = card(new BorderLayout(0, 10));
        agenda.setOpaque(false);
        agenda.setLayout(new BoxLayout(agenda, BoxLayout.Y_AXIS));
        daily.add(dayTitle, BorderLayout.NORTH);
        JScrollPane agendaScroll = new JScrollPane(agenda);
        agendaScroll.setBorder(null);
        agendaScroll.getViewport().setBackground(Theme.WHITE);
        agendaScroll.getVerticalScrollBar().setUnitIncrement(18);
        agendaScroll.setPreferredSize(new Dimension(0, 160));
        daily.add(agendaScroll, BorderLayout.CENTER);
        int dailyHeight = daily.getPreferredSize().height;
        daily.setMinimumSize(new Dimension(0, dailyHeight));
        daily.setMaximumSize(new Dimension(Integer.MAX_VALUE, dailyHeight));
        add(daily);

        setTripContext("Taiwan Trip", "Taiwan Trip", "Taipei, Taiwan",
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22), true);
    }

    public void setTripContext(Object tripId, String name, String destination,
                               LocalDate start, LocalDate end, boolean demo) {
        if (end.isBefore(start)) throw new IllegalArgumentException("Invalid trip dates");
        context.setText(name + " | " + destination + " | " + start + " to " + end);
        List<Activity> tripActivities = schedules.computeIfAbsent(tripId, id -> {
            List<Activity> entries = new ArrayList<>();
            if (demo) {
                entries.add(new Activity(start, LocalTime.of(9, 0), LocalTime.of(11, 0),
                        "Flight to Taipei", "Taoyuan International Airport"));
                entries.add(new Activity(start, LocalTime.of(13, 0), LocalTime.of(14, 30),
                        "Visit Longshan Temple", "Longshan Temple"));
                entries.add(new Activity(start, LocalTime.of(19, 0), LocalTime.of(20, 30),
                        "Dinner at Taipei 101", "Taipei 101"));
            }
            return entries;
        });
        if (activities != tripActivities) {
            selected = start;
            month = YearMonth.from(start);
            activities = tripActivities;
        }
        refresh();
    }

    private void changeMonth(int offset) {
        month = month.plusMonths(offset);
        selected = month.atDay(Math.min(selected.getDayOfMonth(), month.lengthOfMonth()));
        refresh();
    }

    private void refresh() {
        monthTitle.setText(month.format(MONTH));
        days.removeAll();
        for (String weekday : WEEK) {
            JLabel heading = label(weekday, 13, true);
            heading.setHorizontalAlignment(SwingConstants.CENTER);
            days.add(heading);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        for (int index = 0; index < 42; index++) {
            int number = index - offset + 1;
            if (number < 1 || number > month.lengthOfMonth()) {
                days.add(new JLabel());
                continue;
            }
            LocalDate date = month.atDay(number);
            boolean scheduled = activities.stream().anyMatch(activity -> activity.date().equals(date));
            JButton cell = button(number + (scheduled ? " •" : ""), () -> {
                selected = date;
                refresh();
            });
            cell.setBackground(date.equals(selected) ? Theme.PRIMARY : Theme.BG);
            cell.setForeground(date.equals(selected) ? Color.WHITE : Theme.INK);
            String dateLabel = date.format(DAY) + (scheduled ? ", activities scheduled" : "");
            cell.getAccessibleContext().setAccessibleName(dateLabel);
            cell.setToolTipText(dateLabel);
            days.add(cell);
        }

        dayTitle.setText((selected.equals(LocalDate.now()) ? "Today | " : "") + selected.format(DAY));
        agenda.removeAll();
        List<Activity> daily = activities.stream()
                .filter(activity -> activity.date().equals(selected))
                .sorted(Comparator.comparing(Activity::start))
                .toList();
        if (daily.isEmpty()) {
            JLabel empty = label("No activities planned for this day.", 14, false);
            empty.setBorder(new EmptyBorder(10, 0, 0, 0));
            agenda.add(empty);
        }
        for (Activity activity : daily) {
            JPanel row = new JPanel(new BorderLayout(12, 4));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(6, 0, 6, 0));
            JPanel details = new JPanel(new GridLayout(0, 1, 0, 4));
            details.setOpaque(false);
            details.add(label(activity.name(), 16, true));
            details.add(label(activity.start().format(TIME) + " - " + activity.end().format(TIME)
                    + " | " + activity.location(), 14, false));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            actions.setOpaque(false);
            actions.add(outlineButton("Edit", () -> edit(activity)));
            JButton remove = outlineButton("Delete", () -> delete(activity));
            remove.setForeground(Theme.ERROR);
            actions.add(remove);
            row.add(details, BorderLayout.CENTER);
            row.add(actions, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            agenda.add(row);
        }
        agenda.add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    private void edit(Activity existing) {
        DatePicker date = new DatePicker(existing == null ? selected : existing.date());
        JTextField name = new JTextField(existing == null ? "" : existing.name(), 22);
        JTextField location = new JTextField(existing == null ? "" : existing.location(), 22);
        JTextField start = new JTextField(existing == null ? "09:00" : existing.start().toString());
        JTextField end = new JTextField(existing == null ? "10:00" : existing.end().toString());
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Activity"));
        form.add(name);
        form.add(new JLabel("Location"));
        form.add(location);
        form.add(new JLabel("Date (YYYY-MM-DD)"));
        form.add(date);
        form.add(new JLabel("Start time (HH:mm)"));
        form.add(start);
        form.add(new JLabel("End time (HH:mm)"));
        form.add(end);

        String title = existing == null ? "Add activity" : "Edit activity";
        while (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Activity candidate = new Activity(date.getDate(), parseTime(start.getText()),
                        parseTime(end.getText()), name.getText().trim(), location.getText().trim());
                String error = validationError(candidate, existing);
                if (error != null) {
                    showError(error);
                    continue;
                }
                if (existing != null) activities.remove(existing);
                activities.add(candidate);
                selected = candidate.date();
                month = YearMonth.from(selected);
                refresh();
                return;
            } catch (DateTimeException invalid) {
                showError("Enter a valid date and 24-hour times in HH:mm format.");
            }
        }
    }

    private static LocalTime parseTime(String value) {
        String time = value.trim();
        if (!time.matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeException("Invalid time");
        return LocalTime.parse(time);
    }

    private String validationError(Activity candidate, Activity existing) {
        if (candidate.name().isBlank() || candidate.location().isBlank()) {
            return "Enter an activity name and location.";
        }
        if (!candidate.end().isAfter(candidate.start())) return "End time must follow start time.";
        for (Activity other : activities) {
            if (other != existing && other.date().equals(candidate.date())
                    && candidate.start().isBefore(other.end())
                    && candidate.end().isAfter(other.start())) {
                return "This time overlaps with " + other.name() + ".";
            }
        }
        return null;
    }

    private void delete(Activity activity) {
        if (JOptionPane.showConfirmDialog(this, "Delete " + activity.name() + "?",
                "Delete activity", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            activities.remove(activity);
            refresh();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Trip Calendar", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel card(LayoutManager layout) {
        JPanel card = new Theme.RoundedPanel(layout, Theme.WHITE, Theme.BORDER, 18);
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(bold ? Theme.INK : Theme.TEXT_MUTED);
        return label;
    }

    private static JButton button(String text, Runnable action) {
        JButton button = Theme.primaryButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static JButton outlineButton(String text, Runnable action) {
        JButton button = Theme.outlineButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }
}
