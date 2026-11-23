import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Reusable date input + pop-up calendar component (format: YYYY-MM-DD).
 *
 * Usage:
 *   private final DatePicker datePicker = new DatePicker();
 *   // atau dengan tanggal awal tertentu:
 *   private final DatePicker startPicker = new DatePicker(LocalDate.now());
 *
 * Ambil tanggalnya:
 *   LocalDate d = datePicker.getDate(); // atau datePicker.getText()
 */
public class DatePicker extends JPanel {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final String[] DAYS = {"M", "T", "W", "T", "F", "S", "S"};

    private final JTextField dateField;
    private final JButton pickDateBtn;

    public DatePicker() {
        this(LocalDate.now());
    }

    public DatePicker(LocalDate initialDate) {
        super(new BorderLayout(8, 0));
        setOpaque(false);

        LocalDate start = initialDate != null ? initialDate : LocalDate.now();
        dateField = new JTextField(start.format(DateTimeFormatter.ISO_LOCAL_DATE));
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateField.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(9, 11, 9, 11)
        ));

        pickDateBtn = Theme.outlineButton("Calendar");
        pickDateBtn.addActionListener(e -> showPopup());

        add(dateField, BorderLayout.CENTER);
        add(pickDateBtn, BorderLayout.EAST);
    }

    public String getText() {
        return dateField.getText().trim();
    }

    public void setDate(LocalDate date) {
        if (date != null) {
            dateField.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }

    public LocalDate getDate() {
        return LocalDate.parse(getText());
    }

    public JTextField getField() {
        return dateField;
    }

    private void showPopup() {
        LocalDate initial = parseOrToday(dateField.getText());
        final LocalDate[] monthCursor = {initial.withDayOfMonth(1)};

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Color.WHITE);
        root.setPreferredSize(new Dimension(268, 238));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JButton prevMonth = navButton("<");
        JButton nextMonth = navButton(">");
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        monthLabel.setForeground(Theme.INK);

        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        grid.setOpaque(false);

        Runnable rebuild = () -> {
            grid.removeAll();
            LocalDate firstOfMonth = monthCursor[0];
            monthLabel.setText(firstOfMonth.format(MONTH_FMT));

            for (String dayName : DAYS) {
                JLabel lbl = new JLabel(dayName, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setForeground(Theme.TEXT_MUTED);
                grid.add(lbl);
            }

            int leadingEmpty = firstOfMonth.getDayOfWeek().getValue() - 1;
            for (int i = 0; i < leadingEmpty; i++) {
                grid.add(new JLabel(""));
            }

            LocalDate selectedDate = parseOrToday(dateField.getText());
            int length = firstOfMonth.lengthOfMonth();

            for (int day = 1; day <= length; day++) {
                LocalDate date = firstOfMonth.withDayOfMonth(day);
                JButton dayBtn = new JButton(String.valueOf(day));
                dayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dayBtn.setMargin(new Insets(0, 0, 0, 0));
                dayBtn.setFocusPainted(false);
                dayBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                if (date.equals(selectedDate)) {
                    dayBtn.setBackground(Theme.PRIMARY);
                    dayBtn.setForeground(Color.WHITE);
                    dayBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else if (date.equals(LocalDate.now())) {
                    dayBtn.setBackground(Theme.PALE);
                    dayBtn.setForeground(Theme.PRIMARY);
                    dayBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else {
                    dayBtn.setBackground(Color.WHITE);
                    dayBtn.setForeground(Theme.INK);
                }

                dayBtn.addActionListener(e -> {
                    dateField.setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    popup.setVisible(false);
                });
                grid.add(dayBtn);
            }

            grid.revalidate();
            grid.repaint();
        };

        prevMonth.addActionListener(e -> {
            monthCursor[0] = monthCursor[0].minusMonths(1);
            rebuild.run();
        });
        nextMonth.addActionListener(e -> {
            monthCursor[0] = monthCursor[0].plusMonths(1);
            rebuild.run();
        });

        rebuild.run();

        header.add(prevMonth, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextMonth, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);

        popup.add(root);
        popup.show(dateField, 0, dateField.getHeight());
    }

    private static JButton navButton(String symbol) {
        JButton btn = new JButton(symbol);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(Theme.BG);
        btn.setForeground(Theme.INK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(4, 9, 4, 9));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static LocalDate parseOrToday(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }
}
