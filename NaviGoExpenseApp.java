import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * NaviGo - Expense Management (Java Swing prototype)
 * Run: javac NaviGoExpenseApp.java
 *      java NaviGoExpenseApp
 *
 * This is a front-end prototype with sample data. It does not use a database yet.
 */
public class NaviGoExpenseApp extends JFrame {
    private static final Color PURPLE = new Color(43, 119, 112);
    private static final Color PURPLE_DARK = new Color(35, 58, 58);
    private static final Color LAVENDER = new Color(226, 241, 238);
    private static final Color BG = new Color(244, 248, 248);
    private static final Color LINE = new Color(224, 235, 233);
    private static final Color MUTED = new Color(119, 137, 135);
    private static final Color GREEN = new Color(25, 131, 108);
    private static final Color RED = new Color(207, 87, 103);

    private final List<String> members = Arrays.asList("You", "Jessie", "Vivi", "Jason");
    private final List<Expense> expenses = new ArrayList<Expense>();

    private JLabel totalValue;
    private JLabel balanceValue;
    private JLabel shareValue;
    private JPanel memberListPanel;
    private DefaultTableModel expenseModel;
    private DefaultTableModel settlementModel;

    public NaviGoExpenseApp() {
        super("NaviGo - Expense Management");
        seedSampleData();
        buildWindow();
        refreshDashboard();
    }

    private void buildWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 790);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, LINE));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("NaviGo");
        brand.setFont(new Font("Inter", Font.BOLD, 27));
        brand.setForeground(PURPLE_DARK);
        brand.setBorder(new EmptyBorder(25, 22, 2, 10));
        sidebar.add(brand);

        JLabel subtitle = new JLabel("GROUP TRAVEL PLANNER");
        subtitle.setFont(new Font("Inter", Font.BOLD, 9));
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(0, 24, 25, 10));
        sidebar.add(subtitle);

        JPanel trip = new JPanel(new BorderLayout(10, 0));
        trip.setBackground(LAVENDER);
        trip.setBorder(new EmptyBorder(12, 12, 12, 12));
        trip.setMaximumSize(new Dimension(190, 58));
        JLabel star = new JLabel("✦");
        star.setForeground(PURPLE);
        star.setFont(new Font("Inter", Font.BOLD, 20));
        trip.add(star, BorderLayout.WEST);
        JPanel tripText = new JPanel();
        tripText.setOpaque(false);
        tripText.setLayout(new BoxLayout(tripText, BoxLayout.Y_AXIS));
        JLabel tripName = new JLabel("Taipei after hours");
        tripName.setFont(new Font("Inter", Font.BOLD, 12));
        JLabel tripDate = new JLabel("Nov 20–22, 2026");
        tripDate.setFont(new Font("Inter", Font.PLAIN, 10));
        tripDate.setForeground(MUTED);
        tripText.add(tripName);
        tripText.add(tripDate);
        trip.add(tripText, BorderLayout.CENTER);
        JPanel tripWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tripWrap.setOpaque(false);
        tripWrap.add(trip);
        sidebar.add(tripWrap);
        sidebar.add(Box.createVerticalStrut(28));

        String[][] navItems = {
            {"◈", "Overview"}, {"≡", "Itinerary"}, {"▦", "Calendar"},
            {"◇", "Explore places"}, {"＄", "Split bill"}, {"▤", "Group chat"}
        };
        for (String[] item : navItems) {
            boolean active = item[1].equals("Split bill");
            JButton nav = new JButton(item[0] + "    " + item[1]);
            nav.setHorizontalAlignment(SwingConstants.LEFT);
            nav.setFont(new Font("Inter", Font.BOLD, 13));
            nav.setForeground(active ? PURPLE : new Color(112, 105, 125));
            nav.setBackground(active ? LAVENDER : Color.WHITE);
            nav.setFocusPainted(false);
            nav.setBorder(new EmptyBorder(12, 22, 12, 10));
            nav.setMaximumSize(new Dimension(210, 44));
            nav.setAlignmentX(Component.CENTER_ALIGNMENT);
            nav.addActionListener(e -> {
                if (!item[1].equals("Split bill")) {
                    JOptionPane.showMessageDialog(this,
                        item[1] + " page will be connected by the team later.",
                        "NaviGo", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            sidebar.add(nav);
        }

        sidebar.add(Box.createVerticalGlue());
        JLabel account = new JLabel("  ●  Your workspace");
        account.setFont(new Font("Inter", Font.BOLD, 12));
        account.setForeground(PURPLE_DARK);
        account.setBorder(new EmptyBorder(18, 12, 22, 8));
        sidebar.add(account);
        return sidebar;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);

        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(16, 30, 16, 30)));
        JLabel breadcrumb = new JLabel("Taipei after hours   /   <b>Split bill</b>");
        breadcrumb.setFont(new Font("Inter", Font.PLAIN, 13));
        breadcrumb.setForeground(MUTED);
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightActions.setOpaque(false);
        JLabel groupAvatars = new JLabel("●  ●  ●  ●");
        groupAvatars.setFont(new Font("Inter", Font.BOLD, 14));
        groupAvatars.setForeground(new Color(91, 151, 137));
        JButton share = new JButton("Share trip ↗");
        styleOutlineButton(share);
        share.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Trip sharing will be connected with the Group module later."));
        rightActions.add(groupAvatars);
        rightActions.add(share);
        topbar.add(breadcrumb, BorderLayout.WEST);
        topbar.add(rightActions, BorderLayout.EAST);
        main.add(topbar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(25, 30, 35, 30));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(buildTripHero());
        content.add(Box.createVerticalStrut(25));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("✦  TRIP FINANCES");
        eyebrow.setFont(new Font("Inter", Font.BOLD, 10));
        eyebrow.setForeground(PURPLE);
        JLabel title = new JLabel("Expense overview");
        title.setFont(new Font("Inter", Font.BOLD, 28));
        title.setForeground(PURPLE_DARK);
        JLabel description = new JLabel("Keep your group's spending clear and simple.");
        description.setFont(new Font("Inter", Font.PLAIN, 12));
        description.setForeground(MUTED);
        headingText.add(eyebrow);
        headingText.add(Box.createVerticalStrut(4));
        headingText.add(title);
        headingText.add(description);
        JButton addExpense = new JButton("+  Add expense");
        stylePrimaryButton(addExpense);
        addExpense.addActionListener(e -> showAddExpenseDialog());
        heading.add(headingText, BorderLayout.WEST);
        heading.add(addExpense, BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        content.add(heading);
        content.add(Box.createVerticalStrut(20));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        totalValue = new JLabel("NT$ 0");
        balanceValue = new JLabel("NT$ 0");
        shareValue = new JLabel("NT$ 0");
        stats.add(statCard("Total trip expenses", totalValue, "Across all recorded expenses", "＄"));
        stats.add(statCard("Your balance", balanceValue, "Positive means you are owed", "↗"));
        stats.add(statCard("Your share", shareValue, "Your part of the trip expenses", "♧"));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        content.add(stats);
        content.add(Box.createVerticalStrut(17));

        JPanel upper = new JPanel(new GridLayout(1, 2, 14, 0));
        upper.setOpaque(false);
        upper.add(buildMembersCard());
        upper.add(buildExpensesCard());
        upper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        content.add(upper);
        content.add(Box.createVerticalStrut(17));

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.add(buildSettlementCard());
        lower.add(buildSnapshotCard());
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        content.add(lower);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTripHero() {
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint paint = new GradientPaint(0, 0, new Color(37, 91, 88),
                        getWidth(), getHeight(), new Color(91, 145, 130));
                g2.setPaint(paint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(getWidth() - 185, -65, 220, 220);
                g2.fillOval(getWidth() - 110, 60, 130, 130);
                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(22, 25, 22, 25));
        hero.setPreferredSize(new Dimension(800, 145));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel place = new JLabel("✦   TAIPEI, TAIWAN");
        place.setFont(new Font("Inter", Font.BOLD, 10));
        place.setForeground(new Color(218, 239, 232));
        JLabel tripTitle = new JLabel("Taipei after hours.");
        tripTitle.setFont(new Font("Inter", Font.BOLD, 29));
        tripTitle.setForeground(Color.WHITE);
        JLabel meta = new JLabel("▦   Nov 20–22, 2026      ♧   4 travelers");
        meta.setFont(new Font("Inter", Font.PLAIN, 12));
        meta.setForeground(new Color(231, 243, 239));
        details.add(place);
        details.add(Box.createVerticalStrut(7));
        details.add(tripTitle);
        details.add(Box.createVerticalStrut(7));
        details.add(meta);
        JLabel stamp = new JLabel("TAIPEI · TW     ✳     25° N  121° E", SwingConstants.CENTER);
        stamp.setFont(new Font("Inter", Font.BOLD, 10));
        stamp.setForeground(new Color(235, 246, 242));
        stamp.setBorder(new CompoundBorder(new LineBorder(new Color(255,255,255,75), 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        hero.add(details, BorderLayout.WEST);
        hero.add(stamp, BorderLayout.EAST);
        return hero;
    }

    private JPanel statCard(String label, JLabel value, String note, String icon) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(new Font("Inter", Font.BOLD, 12));
        name.setForeground(MUTED);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(LAVENDER);
        iconLabel.setForeground(PURPLE);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(32, 32));
        top.add(name, BorderLayout.WEST);
        top.add(iconLabel, BorderLayout.EAST);
        value.setFont(new Font("Inter", Font.BOLD, 23));
        value.setForeground(PURPLE_DARK);
        JLabel noteLabel = new JLabel(note);
        noteLabel.setFont(new Font("Inter", Font.PLAIN, 10));
        noteLabel.setForeground(MUTED);
        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(noteLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMembersCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.add(sectionTitle("Member balances"), BorderLayout.NORTH);
        memberListPanel = new JPanel();
        memberListPanel.setOpaque(false);
        memberListPanel.setLayout(new BoxLayout(memberListPanel, BoxLayout.Y_AXIS));
        card.add(memberListPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildExpensesCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.add(sectionTitle("Recent expenses"), BorderLayout.NORTH);
        expenseModel = new DefaultTableModel(
            new Object[]{"Expense", "Paid by", "Amount", "People"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(expenseModel);
        styleTable(table);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());
        card.add(pane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSettlementCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.add(sectionTitle("Settlement plan"), BorderLayout.NORTH);
        settlementModel = new DefaultTableModel(
            new Object[]{"From", "To", "Amount"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(settlementModel);
        styleTable(table);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());
        card.add(pane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSnapshotCard() {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.add(sectionTitle("Expense workflow"), BorderLayout.NORTH);
        JTextArea info = new JTextArea(
            "1. Add an expense and choose who paid.\n" +
            "2. Select the members who shared it.\n" +
            "3. NaviGo calculates each person's share.\n" +
            "4. Check the settlement plan to see who owes whom.");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(new Font("Inter", Font.PLAIN, 12));
        info.setForeground(new Color(91, 83, 105));
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true),
                new EmptyBorder(17, 18, 17, 18)));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.BOLD, 15));
        label.setForeground(PURPLE_DARK);
        return label;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Inter", Font.PLAIN, 11));
        table.setRowHeight(28);
        table.setForeground(PURPLE_DARK);
        table.setGridColor(new Color(244, 240, 246));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(LAVENDER);
        table.getTableHeader().setFont(new Font("Inter", Font.BOLD, 11));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(250, 248, 252));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, LINE));
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(PURPLE);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Inter", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 17, 11, 17));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleOutlineButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(PURPLE);
        button.setFont(new Font("Inter", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
    }

    private void showAddExpenseDialog() {
        final JDialog dialog = new JDialog(this, "Add an expense", true);
        dialog.setSize(470, 570);
        dialog.setLocationRelativeTo(this);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.insets = new Insets(5, 4, 5, 4);

        JLabel title = new JLabel("Add an expense");
        title.setFont(new Font("Inter", Font.BOLD, 21));
        title.setForeground(PURPLE_DARK);
        form.add(title, c);

        JTextField nameField = new JTextField();
        JTextField amountField = new JTextField();
        JComboBox<String> payerBox = new JComboBox<String>(members.toArray(new String[0]));
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        JComboBox<String> categoryBox = new JComboBox<String>(
            new String[]{"Food & drinks", "Transport", "Accommodation", "Tickets", "Shopping", "Other"});

        c.gridy++; addFormRow(form, c, "Expense name", nameField);
        c.gridy++; addFormRow(form, c, "Amount (NT$)", amountField);
        c.gridy++; addFormRow(form, c, "Paid by", payerBox);
        c.gridy++; addFormRow(form, c, "Date (YYYY-MM-DD)", dateField);
        c.gridy++; addFormRow(form, c, "Category", categoryBox);

        c.gridy++;
        JLabel participantsLabel = new JLabel("Split between");
        participantsLabel.setFont(new Font("Inter", Font.BOLD, 12));
        participantsLabel.setForeground(PURPLE_DARK);
        form.add(participantsLabel, c);

        JPanel checks = new JPanel(new GridLayout(2, 2, 4, 4));
        checks.setOpaque(false);
        Map<String, JCheckBox> participantChecks = new LinkedHashMap<String, JCheckBox>();
        for (String member : members) {
            JCheckBox check = new JCheckBox(member, true);
            check.setOpaque(false);
            check.setFont(new Font("Inter", Font.PLAIN, 12));
            participantChecks.put(member, check);
            checks.add(check);
        }
        c.gridy++;
        form.add(checks, c);

        JButton cancel = new JButton("Cancel");
        styleOutlineButton(cancel);
        cancel.addActionListener(e -> dialog.dispose());
        JButton save = new JButton("Save expense");
        stylePrimaryButton(save);
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            String amountText = amountField.getText().trim();
            if (name.isEmpty() || amountText.isEmpty()) {
                showError(dialog, "Please enter the expense name and amount.");
                return;
            }
            int amount;
            try {
                amount = Integer.parseInt(amountText);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError(dialog, "Amount must be a positive whole number.");
                return;
            }
            List<String> selected = new ArrayList<String>();
            for (String member : members) {
                if (participantChecks.get(member).isSelected()) selected.add(member);
            }
            if (selected.isEmpty()) {
                showError(dialog, "Please select at least one participant.");
                return;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(dateField.getText().trim());
            } catch (Exception ex) {
                showError(dialog, "Please use the date format YYYY-MM-DD.");
                return;
            }
            Expense expense = new Expense(name, amount, (String) payerBox.getSelectedItem(),
                    date, (String) categoryBox.getSelectedItem(), selected);
            expenses.add(expense);
            refreshDashboard();
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Expense saved. Member balances have been updated.",
                    "NaviGo", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        c.gridy++; c.insets = new Insets(18, 4, 4, 4);
        form.add(buttons, c);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel panel, GridBagConstraints c, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 5));
        row.setOpaque(false);
        JLabel text = new JLabel(label);
        text.setFont(new Font("Inter", Font.BOLD, 12));
        text.setForeground(PURPLE_DARK);
        field.setFont(new Font("Inter", Font.PLAIN, 12));
        if (field instanceof JTextField) {
            field.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true),
                    new EmptyBorder(8, 9, 8, 9)));
        }
        row.add(text, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        panel.add(row, c);
    }

    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Input error", JOptionPane.WARNING_MESSAGE);
    }

    private void refreshDashboard() {
        int total = 0;
        Map<String, Integer> paid = new LinkedHashMap<String, Integer>();
        Map<String, Double> shares = new LinkedHashMap<String, Double>();
        for (String member : members) {
            paid.put(member, 0);
            shares.put(member, 0.0);
        }
        for (Expense expense : expenses) {
            total += expense.amount;
            paid.put(expense.payer, paid.get(expense.payer) + expense.amount);
            double eachShare = (double) expense.amount / expense.participants.size();
            for (String participant : expense.participants) {
                shares.put(participant, shares.get(participant) + eachShare);
            }
        }

        Map<String, Double> balances = new LinkedHashMap<String, Double>();
        for (String member : members) {
            balances.put(member, paid.get(member) - shares.get(member));
        }

        totalValue.setText(formatMoney(total));
        balanceValue.setText((balances.get("You") >= 0 ? "+" : "−") +
                formatMoney(Math.abs(balances.get("You"))));
        balanceValue.setForeground(balances.get("You") >= 0 ? GREEN : RED);
        shareValue.setText(formatMoney(shares.get("You")));

        memberListPanel.removeAll();
        for (String member : members) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(7, 0, 7, 0));
            JLabel avatar = new JLabel(initials(member), SwingConstants.CENTER);
            avatar.setOpaque(true);
            avatar.setBackground(avatarColor(member));
            avatar.setForeground(PURPLE_DARK);
            avatar.setFont(new Font("Inter", Font.BOLD, 10));
            avatar.setPreferredSize(new Dimension(34, 34));
            JLabel details = new JLabel("<html><b>" + member + "</b><br><font color='#817b8d' size='2'>Paid " +
                    formatMoney(paid.get(member)) + "</font></html>");
            JLabel balance = new JLabel((balances.get(member) >= 0 ? "+" : "−") +
                    formatMoney(Math.abs(balances.get(member))));
            balance.setFont(new Font("Inter", Font.BOLD, 12));
            balance.setForeground(balances.get(member) >= 0 ? GREEN : RED);
            row.add(avatar, BorderLayout.WEST);
            row.add(details, BorderLayout.CENTER);
            row.add(balance, BorderLayout.EAST);
            memberListPanel.add(row);
            memberListPanel.add(new JSeparator());
        }
        memberListPanel.revalidate();
        memberListPanel.repaint();

        expenseModel.setRowCount(0);
        List<Expense> reversed = new ArrayList<Expense>(expenses);
        Collections.reverse(reversed);
        for (Expense expense : reversed) {
            expenseModel.addRow(new Object[]{expense.name, expense.payer,
                    formatMoney(expense.amount), expense.participants.size()});
        }

        settlementModel.setRowCount(0);
        List<Transfer> transfers = calculateSettlements(balances);
        for (Transfer transfer : transfers) {
            settlementModel.addRow(new Object[]{transfer.from, transfer.to, formatMoney(transfer.amount)});
        }
        if (transfers.isEmpty()) {
            settlementModel.addRow(new Object[]{"Everyone", "settled", formatMoney(0)});
        }
    }

    private List<Transfer> calculateSettlements(Map<String, Double> balances) {
        List<Transfer> result = new ArrayList<Transfer>();
        List<BalanceEntry> debtors = new ArrayList<BalanceEntry>();
        List<BalanceEntry> creditors = new ArrayList<BalanceEntry>();
        for (String member : members) {
            double value = balances.get(member);
            if (value < -0.009) debtors.add(new BalanceEntry(member, -value));
            else if (value > 0.009) creditors.add(new BalanceEntry(member, value));
        }
        int d = 0, cr = 0;
        while (d < debtors.size() && cr < creditors.size()) {
            double amount = Math.min(debtors.get(d).amount, creditors.get(cr).amount);
            int rounded = (int) Math.round(amount);
            if (rounded > 0) result.add(new Transfer(debtors.get(d).name, creditors.get(cr).name, rounded));
            debtors.get(d).amount -= amount;
            creditors.get(cr).amount -= amount;
            if (debtors.get(d).amount < 0.01) d++;
            if (creditors.get(cr).amount < 0.01) cr++;
        }
        return result;
    }

    private String formatMoney(double amount) {
        return "NT$ " + String.format("%,.0f", amount);
    }

    private String initials(String name) {
        if (name.equals("You")) return "Y";
        if (name.equals("Jessie")) return "JS";
        if (name.equals("Vivi")) return "V";
        return "JN";
    }

    private Color avatarColor(String name) {
        if (name.equals("Jessie")) return new Color(220, 237, 228);
        if (name.equals("Vivi")) return new Color(250, 231, 201);
        if (name.equals("Jason")) return new Color(229, 222, 247);
        return new Color(241, 220, 235);
    }

    private void seedSampleData() {
        expenses.add(new Expense("Dinner at Ningxia", 1200, "You",
                LocalDate.of(2026, 11, 20), "Food & drinks", members));
        expenses.add(new Expense("Taxi to hotel", 400, "Jessie",
                LocalDate.of(2026, 11, 20), "Transport", members));
        expenses.add(new Expense("Drinks", 400, "Vivi",
                LocalDate.of(2026, 11, 20), "Food & drinks", members));
        expenses.add(new Expense("Museum tickets", 400, "Jason",
                LocalDate.of(2026, 11, 20), "Tickets", members));
    }

    private static class Expense {
        String name;
        int amount;
        String payer;
        LocalDate date;
        String category;
        List<String> participants;

        Expense(String name, int amount, String payer, LocalDate date,
                String category, List<String> participants) {
            this.name = name;
            this.amount = amount;
            this.payer = payer;
            this.date = date;
            this.category = category;
            this.participants = new ArrayList<String>(participants);
        }
    }

    private static class BalanceEntry {
        String name;
        double amount;
        BalanceEntry(String name, double amount) { this.name = name; this.amount = amount; }
    }

    private static class Transfer {
        String from;
        String to;
        int amount;
        Transfer(String from, String to, int amount) {
            this.from = from; this.to = to; this.amount = amount;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new NaviGoExpenseApp().setVisible(true);
        });
    }
}
