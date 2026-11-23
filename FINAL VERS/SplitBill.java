import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * NaviGo - Expense Management panel (Java Swing).
 */
public class SplitBill extends JPanel {

    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final List<String> members = Arrays.asList("You", "Jessie", "Vivi", "Jason");
    private final Map<String, List<Expense>> tripExpensesMap = new LinkedHashMap<>();

    private String currentTripKey = "Taiwan Trip";
    private String currentTripName = "Taipei after hours";
    private String currentDestination = "TAIPEI, TAIWAN";
    private String currentDateRange = "Nov 20–22, 2026";

    private JLabel heroPlaceLabel;
    private JLabel heroTitleLabel;
    private JLabel heroMetaLabel;
    private JLabel heroStampLabel;

    private JLabel totalValue;
    private JLabel balanceValue;
    private JLabel shareValue;
    private JPanel memberListPanel;

    private JTable expenseTable;
    private DefaultTableModel expenseModel;
    private final List<Expense> displayedExpenses = new ArrayList<>();

    private DefaultTableModel settlementModel;

    public SplitBill() {
        seedSampleData();
        buildPanel();
        refreshDashboard();
    }

    public void setTripContext(String tripName, String destination, LocalDate start, LocalDate end, boolean isDemo) {
        this.currentTripKey = tripName;
        this.currentTripName = tripName;
        this.currentDestination = destination.toUpperCase(Locale.ENGLISH);
        this.currentDateRange = start.format(RANGE_FMT) + " – " + end.format(RANGE_FMT);

        if (!tripExpensesMap.containsKey(currentTripKey)) {
            tripExpensesMap.put(currentTripKey, isDemo ? createSampleExpenses() : new ArrayList<>());
        }

        heroPlaceLabel.setText("DESTINATION  •  " + currentDestination);
        heroTitleLabel.setText(currentTripName);
        heroMetaLabel.setText(currentDateRange + "   |   " + members.size() + " travelers in group");
        heroStampLabel.setText(currentDestination + "  /  NAVIGO");

        refreshDashboard();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel content = new JPanel();
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(20, 24, 28, 24));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel hero = buildTripHero();
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hero);
        content.add(Box.createVerticalStrut(22));

        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("TRIP FINANCES");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(Theme.PRIMARY);

        JLabel title = new JLabel("Expense overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Theme.INK);

        JLabel description = new JLabel("Split bills effortlessly and see who settles what with zero awkwardness.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(Theme.TEXT_MUTED);

        headingText.add(eyebrow);
        headingText.add(Box.createVerticalStrut(3));
        headingText.add(title);
        headingText.add(Box.createVerticalStrut(3));
        headingText.add(description);

        JButton addExpense = new JButton("+  Add expense");
        stylePrimaryButton(addExpense);
        addExpense.addActionListener(e -> showExpenseDialog(null));

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(addExpense);

        heading.add(headingText, BorderLayout.WEST);
        heading.add(buttonWrapper, BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, heading.getPreferredSize().height + 10));
        content.add(heading);
        content.add(Box.createVerticalStrut(18));

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalValue = new JLabel("NT$ 0");
        balanceValue = new JLabel("NT$ 0");
        shareValue = new JLabel("NT$ 0");
        stats.add(statCard("Total trip spend", totalValue, "Combined group expenses", "ALL"));
        stats.add(statCard("Your net balance", balanceValue, "Plus = owed to you  |  Minus = you owe", "NET"));
        stats.add(statCard("Your fair share", shareValue, "Your split portion so far", "YOU"));
        stats.setPreferredSize(new Dimension(600, 138));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 146));
        content.add(stats);
        content.add(Box.createVerticalStrut(18));

        JPanel upper = new JPanel(new GridLayout(1, 2, 16, 0));
        upper.setOpaque(false);
        upper.setAlignmentX(Component.LEFT_ALIGNMENT);
        upper.add(buildMembersCard());
        upper.add(buildExpensesCard());
        upper.setPreferredSize(new Dimension(600, 330));
        upper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 370));
        content.add(upper);
        content.add(Box.createVerticalStrut(18));

        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.setAlignmentX(Component.LEFT_ALIGNMENT);
        lower.add(buildSettlementCard());
        lower.add(buildSnapshotCard());
        lower.setPreferredSize(new Dimension(600, 225));
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 265));
        content.add(lower);

        add(content, BorderLayout.CENTER);
    }

    private JPanel buildTripHero() {
        JPanel hero = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int width = getWidth();
                int height = getHeight();

                // Clip sudut membulat dulu
                Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, 24, 24);
                g2.setClip(clip);

                g2.setColor(Theme.PRIMARY);
                g2.fillRect(0, 0, width, height);

                // Overlay gradasi gelap biar tulisan putih & lime tetap kontras
                GradientPaint overlay = new GradientPaint(
                        0, 0, Theme.withAlpha(Theme.INK, 210),
                        width, 0, Theme.withAlpha(Theme.PRIMARY, 110)
                );
                g2.setPaint(overlay);
                g2.fillRect(0, 0, width, height);

                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(24, 28, 24, 28));
        hero.setMinimumSize(new Dimension(400, 158));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 178));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        heroPlaceLabel = new JLabel("DESTINATION  •  " + currentDestination);
        heroPlaceLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heroPlaceLabel.setForeground(Theme.LIME);

        heroTitleLabel = new JLabel(currentTripName);
        heroTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        heroTitleLabel.setForeground(Color.WHITE);

        heroMetaLabel = new JLabel(currentDateRange + "   |   " + members.size() + " travelers in group");
        heroMetaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        heroMetaLabel.setForeground(Theme.PALE);

        details.add(heroPlaceLabel);
        details.add(Box.createVerticalStrut(6));
        details.add(heroTitleLabel);
        details.add(Box.createVerticalStrut(6));
        details.add(heroMetaLabel);

        heroStampLabel = new JLabel(currentDestination + "  /  NAVIGO", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.withAlpha(Theme.WHITE, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        heroStampLabel.setOpaque(false);
        heroStampLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heroStampLabel.setForeground(Color.WHITE);
        heroStampLabel.setBorder(new CompoundBorder(
                new LineBorder(Theme.withAlpha(Theme.WHITE, 75), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel stampHolder = new JPanel(new GridBagLayout());
        stampHolder.setOpaque(false);
        stampHolder.add(heroStampLabel);

        hero.add(details, BorderLayout.CENTER);
        hero.add(stampHolder, BorderLayout.EAST);
        return hero;
    }

    private JPanel statCard(String label, JLabel value, String note, String pillText) {
        RoundedCard card = new RoundedCard(18, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 6));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel name = new JLabel(label);
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(Theme.TEXT_MUTED);

        JLabel pill = new JLabel(pillText, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PALE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setForeground(Theme.PRIMARY);
        pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pill.setPreferredSize(new Dimension(44, 26));

        top.add(name, BorderLayout.WEST);
        top.add(pill, BorderLayout.EAST);

        value.setFont(new Font("Segoe UI", Font.BOLD, 27));
        value.setForeground(Theme.INK);

        JLabel noteLabel = new JLabel(note);
        noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteLabel.setForeground(Theme.TEXT_MUTED);

        card.add(top, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(noteLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMembersCard() {
        RoundedCard card = new RoundedCard(18, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.add(sectionTitle("Member balances"), BorderLayout.NORTH);

        memberListPanel = new JPanel();
        memberListPanel.setOpaque(false);
        memberListPanel.setLayout(new BoxLayout(memberListPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(memberListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildExpensesCard() {
        RoundedCard card = new RoundedCard(18, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 12));

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.add(sectionTitle("Recent expenses"), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        JButton editBtn = new JButton("Edit");
        styleMiniButton(editBtn, false);
        editBtn.addActionListener(e -> editSelectedExpense());

        JButton deleteBtn = new JButton("Delete");
        styleMiniButton(deleteBtn, true);
        deleteBtn.addActionListener(e -> deleteSelectedExpense());

        actions.add(editBtn);
        actions.add(deleteBtn);
        topBar.add(actions, BorderLayout.EAST);
        card.add(topBar, BorderLayout.NORTH);

        expenseModel = new DefaultTableModel(
                new Object[]{"Expense", "Date", "Category", "Paid by", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        expenseTable = new JTable(expenseModel);
        expenseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(expenseTable, 4);

        JScrollPane pane = new JScrollPane(expenseTable);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Color.WHITE);
        card.add(pane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSettlementCard() {
        RoundedCard card = new RoundedCard(18, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 10));
        card.add(sectionTitle("Settlement plan"), BorderLayout.NORTH);

        settlementModel = new DefaultTableModel(
                new Object[]{"From", "To", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(settlementModel);
        styleTable(table, 2);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(Color.WHITE);
        card.add(pane, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSnapshotCard() {
        RoundedCard card = new RoundedCard(18, Theme.BORDER);
        card.setLayout(new BorderLayout(0, 10));
        card.add(sectionTitle("How splitting works"), BorderLayout.NORTH);

        JPanel steps = new JPanel();
        steps.setOpaque(false);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));

        String[] items = {
                "01  Log a bill and pick who paid upfront.",
                "02  Check off everyone sharing that expense.",
                "03  Edit or delete any row anytime if needed.",
                "04  Follow the settlement plan to square up cleanly."
        };
        for (String item : items) {
            JLabel line = new JLabel(item);
            line.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            line.setForeground(Theme.TEXT_MUTED);
            line.setBorder(new EmptyBorder(3, 0, 3, 0));
            steps.add(line);
        }

        card.add(steps, BorderLayout.CENTER);
        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 17));
        label.setForeground(Theme.INK);
        return label;
    }

    private void styleTable(JTable table, int rightAlignCol) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setForeground(Theme.INK);
        table.setGridColor(Theme.BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(Theme.PALE);
        table.setSelectionForeground(Theme.INK);
        table.setIntercellSpacing(new Dimension(8, 0));

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setBackground(Theme.BG);
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        if (rightAlignCol >= 0 && rightAlignCol < table.getColumnCount()) {
            table.getColumnModel().getColumn(rightAlignCol).setCellRenderer(rightRenderer);
        }
    }

    private void styleMiniButton(JButton button, boolean danger) {
        Color fg = danger ? Theme.ERROR : Theme.PRIMARY;
        Color hoverBg = danger ? Theme.ERROR_BG : Theme.BG;
        button.setBackground(Color.WHITE);
        button.setForeground(fg);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(danger ? Theme.ERROR_BORDER : Theme.BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(Theme.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 18, 12, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Theme.PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Theme.PRIMARY);
            }
        });
    }

    private void styleOutlineButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(Theme.PRIMARY);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(10, 15, 10, 15)));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Theme.BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
    }

    private Expense getSelectedExpense() {
        int row = expenseTable.getSelectedRow();
        if (row < 0 || row >= displayedExpenses.size()) {
            showError(this, "Select an expense row first.");
            return null;
        }
        return displayedExpenses.get(row);
    }

    private void editSelectedExpense() {
        Expense target = getSelectedExpense();
        if (target != null) {
            showExpenseDialog(target);
        }
    }

    private void deleteSelectedExpense() {
        Expense target = getSelectedExpense();
        if (target == null) return;

        Window owner = SwingUtilities.getWindowAncestor(this);
        int confirm = JOptionPane.showConfirmDialog(
                owner,
                "Delete \"" + target.name + "\" (" + formatMoney(target.amount) + ")?",
                "Delete expense",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            currentExpenses().remove(target);
            refreshDashboard();
            JOptionPane.showMessageDialog(owner, "Expense deleted. Balances updated.",
                    "NaviGo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showExpenseDialog(Expense existing) {
        boolean isEdit = existing != null;
        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dialog = new JDialog(owner, isEdit ? "Edit expense" : "Add an expense",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(490, 620);
        dialog.setLocationRelativeTo(owner);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 24, 22, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(6, 4, 6, 4);

        JLabel title = new JLabel(isEdit ? "Edit expense" : "Add an expense");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Theme.INK);
        form.add(title, c);

        JTextField nameField = new JTextField(isEdit ? existing.name : "");
        JTextField amountField = new JTextField(isEdit ? String.valueOf(existing.amount) : "");
        JComboBox<String> payerBox = new JComboBox<>(members.toArray(new String[0]));
        if (isEdit && existing.payer != null) {
            payerBox.setSelectedItem(existing.payer);
        }

        LocalDate initialDate = (isEdit && existing.date != null) ? existing.date : LocalDate.now();
        DatePicker datePicker = new DatePicker(initialDate);

        JComboBox<String> categoryBox = new JComboBox<>(
                new String[]{"Food & drinks", "Transport", "Accommodation", "Tickets", "Shopping", "Other"});
        if (isEdit && existing.category != null) {
            categoryBox.setSelectedItem(existing.category);
        }

        c.gridy++;
        addFormRow(form, c, "Expense name", nameField);
        c.gridy++;
        addFormRow(form, c, "Amount (NT$)", amountField);
        c.gridy++;
        addFormRow(form, c, "Paid by", payerBox);
        c.gridy++;
        addFormRow(form, c, "Date (YYYY-MM-DD)", datePicker);
        c.gridy++;
        addFormRow(form, c, "Category", categoryBox);

        c.gridy++;
        JLabel participantsLabel = new JLabel("Split between");
        participantsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        participantsLabel.setForeground(Theme.INK);
        form.add(participantsLabel, c);

        JPanel checks = new JPanel(new GridLayout(2, 2, 8, 8));
        checks.setBackground(Theme.BG);
        checks.setBorder(new EmptyBorder(10, 12, 10, 12));
        Map<String, JCheckBox> participantChecks = new LinkedHashMap<>();
        for (String member : members) {
            boolean checked = !isEdit || existing.participants.contains(member);
            JCheckBox check = new JCheckBox(member, checked);
            check.setOpaque(false);
            check.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            check.setForeground(Theme.INK);
            participantChecks.put(member, check);
            checks.add(check);
        }
        c.gridy++;
        form.add(checks, c);

        JButton cancel = new JButton("Cancel");
        styleOutlineButton(cancel);
        cancel.addActionListener(e -> dialog.dispose());

        JButton save = new JButton(isEdit ? "Update expense" : "Save expense");
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
            List<String> selected = new ArrayList<>();
            for (String member : members) {
                if (participantChecks.get(member).isSelected()) selected.add(member);
            }
            if (selected.isEmpty()) {
                showError(dialog, "Please select at least one participant.");
                return;
            }
            LocalDate date;
            try {
                date = datePicker.getDate();
            } catch (Exception ex) {
                showError(dialog, "Please use the date format YYYY-MM-DD.");
                return;
            }

            String payer = (String) payerBox.getSelectedItem();
            String category = (String) categoryBox.getSelectedItem();

            if (isEdit) {
                existing.name = name;
                existing.amount = amount;
                existing.payer = payer;
                existing.date = date;
                existing.category = category;
                existing.participants = new ArrayList<>(selected);
            } else {
                Expense expense = new Expense(name, amount, payer, date, category, selected);
                tripExpensesMap.computeIfAbsent(currentTripKey, k -> new ArrayList<>()).add(expense);
            }

            refreshDashboard();
            dialog.dispose();
            JOptionPane.showMessageDialog(
                    owner,
                    isEdit ? "Expense updated." : "Expense saved. Balances and settlement plan updated.",
                    "NaviGo",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        c.gridy++;
        c.insets = new Insets(18, 4, 4, 4);
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
        text.setFont(new Font("Segoe UI", Font.BOLD, 13));
        text.setForeground(Theme.INK);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        if (field instanceof JTextField) {
            field.setBorder(new CompoundBorder(new LineBorder(Theme.BORDER, 1, true),
                    new EmptyBorder(9, 11, 9, 11)));
        }
        row.add(text, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        panel.add(row, c);
    }

    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Check your input", JOptionPane.WARNING_MESSAGE);
    }

    private List<Expense> currentExpenses() {
        return tripExpensesMap.computeIfAbsent(currentTripKey, k -> new ArrayList<>());
    }

    private void refreshDashboard() {
        List<Expense> expenses = currentExpenses();
        int total = 0;
        Map<String, Integer> paid = new LinkedHashMap<>();
        Map<String, Double> shares = new LinkedHashMap<>();
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

        Map<String, Double> balances = new LinkedHashMap<>();
        for (String member : members) {
            balances.put(member, paid.get(member) - shares.get(member));
        }

        totalValue.setText(formatMoney(total));
        boolean youPositive = balances.get("You") >= -0.009;
        balanceValue.setText((youPositive ? "+" : "–") + formatMoney(Math.abs(balances.get("You"))));
        balanceValue.setForeground(youPositive ? Theme.PRIMARY : Theme.ERROR);
        shareValue.setText(formatMoney(shares.get("You")));

        memberListPanel.removeAll();
        for (int i = 0; i < members.size(); i++) {
            String member = members.get(i);
            double bal = balances.get(member);
            boolean pos = bal >= -0.009;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(8, 2, 8, 2));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

            JLabel avatar = new JLabel(initials(member), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(avatarColor(member));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            avatar.setOpaque(false);
            avatar.setForeground(Theme.INK);
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            avatar.setPreferredSize(new Dimension(38, 38));

            JLabel details = new JLabel("<html><span style='font-size:13px;color:#18312d'><b>" + member +
                    "</b></span><br><span style='font-size:11px;color:#6c807d'>Paid " +
                    formatMoney(paid.get(member)) + "</span></html>");

            JLabel badge = new JLabel((pos ? "+" : "–") + formatMoney(Math.abs(bal)), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(pos ? Theme.PALE : Theme.ERROR_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            badge.setForeground(pos ? Theme.PRIMARY : Theme.ERROR);
            badge.setBorder(new EmptyBorder(5, 10, 5, 10));

            row.add(avatar, BorderLayout.WEST);
            row.add(details, BorderLayout.CENTER);
            row.add(badge, BorderLayout.EAST);
            memberListPanel.add(row);

            if (i < members.size() - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(Theme.BORDER);
                sep.setBackground(Color.WHITE);
                memberListPanel.add(sep);
            }
        }
        memberListPanel.revalidate();
        memberListPanel.repaint();

        expenseModel.setRowCount(0);
        displayedExpenses.clear();
        List<Expense> reversed = new ArrayList<>(expenses);
        Collections.reverse(reversed);
        if (reversed.isEmpty()) {
            expenseModel.addRow(new Object[]{"No expenses yet", "–", "Click + Add expense", "–", "NT$ 0"});
        } else {
            for (Expense expense : reversed) {
                displayedExpenses.add(expense);
                String dateStr = expense.date != null ? expense.date.format(SHORT_DATE) : "–";
                expenseModel.addRow(new Object[]{
                        expense.name,
                        dateStr,
                        expense.category,
                        expense.payer,
                        formatMoney(expense.amount)
                });
            }
        }

        settlementModel.setRowCount(0);
        List<Transfer> transfers = calculateSettlements(balances);
        for (Transfer transfer : transfers) {
            settlementModel.addRow(new Object[]{transfer.from, transfer.to, formatMoney(transfer.amount)});
        }
        if (transfers.isEmpty()) {
            settlementModel.addRow(new Object[]{"All settled up", "No balances due", formatMoney(0)});
        }
    }

    private List<Transfer> calculateSettlements(Map<String, Double> balances) {
        List<Transfer> result = new ArrayList<>();
        List<BalanceEntry> debtors = new ArrayList<>();
        List<BalanceEntry> creditors = new ArrayList<>();
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
        if (name.equals("You")) return "YOU";
        if (name.equals("Jessie")) return "JS";
        if (name.equals("Vivi")) return "VV";
        return "JN";
    }

    private Color avatarColor(String name) {
        if (name.equals("Jessie")) return Theme.PALE;
        if (name.equals("Vivi")) return Theme.BORDER;
        if (name.equals("Jason")) return Theme.BG;
        return Theme.LIME;
    }

    private void seedSampleData() {
        tripExpensesMap.put(currentTripKey, createSampleExpenses());
    }

    private List<Expense> createSampleExpenses() {
        List<Expense> list = new ArrayList<>();
        list.add(new Expense("Dinner at Ningxia", 1200, "You",
                LocalDate.of(2026, 11, 20), "Food & drinks", members));
        list.add(new Expense("Taxi to hotel", 400, "Jessie",
                LocalDate.of(2026, 11, 20), "Transport", members));
        list.add(new Expense("Drinks", 400, "Vivi",
                LocalDate.of(2026, 11, 20), "Food & drinks", members));
        list.add(new Expense("Museum tickets", 400, "Jason",
                LocalDate.of(2026, 11, 20), "Tickets", members));
        return list;
    }

    private static class RoundedCard extends JPanel {
        private final int radius;
        private final Color borderColor;

        RoundedCard(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
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
            this.participants = new ArrayList<>(participants);
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
}
