import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Group chat inside a JPanel.
 *
 * Workflow:
 *  1. setTrip(trip)           -> loads that trip's history
 *  2. user types + Enter/Send -> sendText()    -> bubble appears -> mock teammate replies
 *  3. user clicks "+"         -> shareCard()   -> itinerary / bill info card appears
 *  4. user clicks card button -> Navigator opens the matching page
 */
public class ChatPanel extends JPanel {

    private static final int MAX_TEXT_WIDTH = 340;

    private final Navigator navigator;
    private final JPanel messageColumn = new ScrollableColumn();
    private final JScrollPane scrollPane;
    private final JTextField input = new JTextField();
    private final Random random = new Random();

    private TripGroup trip;

    public ChatPanel(Navigator navigator) {
        this.navigator = navigator;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        messageColumn.setLayout(new BoxLayout(messageColumn, BoxLayout.Y_AXIS));
        messageColumn.setOpaque(false);
        messageColumn.setBorder(new EmptyBorder(12, 0, 12, 0));

        JPanel wrapper = new ScrollableColumn();
        wrapper.setLayout(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.add(messageColumn, BorderLayout.NORTH);

        scrollPane = new JScrollPane(wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Theme.BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(buildInputBar(), BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------
    public void setTrip(TripGroup trip) {
        this.trip = trip;
        messageColumn.removeAll();
        for (ChatMessage m : trip.getMessages()) {
            messageColumn.add(buildRow(m));
        }
        refresh();
    }

    public void addMessage(ChatMessage m) {
        trip.getMessages().add(m);
        messageColumn.add(buildRow(m));
        refresh();
    }

    // ------------------------------------------------------------------
    // Input bar
    // ------------------------------------------------------------------
    private JComponent buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(Theme.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(10, 16, 10, 16)));

        Theme.RoundedButton plus = Theme.outlineButton("+");
        plus.setBorder(new EmptyBorder(8, 14, 8, 14));
        plus.setToolTipText("Share an update card");
        plus.addActionListener(e -> showShareMenu(plus));

        Theme.RoundedPanel field = new Theme.RoundedPanel(new BorderLayout(), Theme.BG, Theme.BORDER, 18);
        field.setBorder(new EmptyBorder(2, 12, 2, 12));
        input.setBorder(null);
        input.setOpaque(false);
        input.setFont(Theme.font(Font.PLAIN, 14));
        input.setForeground(Theme.PRIMARY);
        input.setCaretColor(Theme.PRIMARY);
        input.addActionListener(e -> sendText());   // Enter key
        field.add(input, BorderLayout.CENTER);

        Theme.RoundedButton send = Theme.primaryButton("Send");
        send.addActionListener(e -> sendText());

        bar.add(plus, BorderLayout.WEST);
        bar.add(field, BorderLayout.CENTER);
        bar.add(send, BorderLayout.EAST);
        return bar;
    }

    private void showShareMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itinerary = new JMenuItem("Share itinerary update");
        JMenuItem bill = new JMenuItem("Share bill update");
        itinerary.addActionListener(e -> shareCard(ChatMessage.Kind.ITINERARY_CARD));
        bill.addActionListener(e -> shareCard(ChatMessage.Kind.BILL_CARD));
        menu.add(itinerary);
        menu.add(bill);
        menu.show(anchor, 0, -menu.getPreferredSize().height);
    }

    // ------------------------------------------------------------------
    // Workflow actions
    // ------------------------------------------------------------------
    private void sendText() {
        String text = input.getText().trim();
        if (text.isEmpty() || trip == null) return;
        input.setText("");
        addMessage(ChatMessage.text(MockData.CURRENT_USER, true, text));
        scheduleMockReply();
    }

    private void shareCard(ChatMessage.Kind kind) {
        if (trip == null) return;
        if (kind == ChatMessage.Kind.ITINERARY_CARD) {
            // TODO: replace with the real latest itinerary change from the calendar page
            addMessage(ChatMessage.card(kind, MockData.CURRENT_USER, true,
                    "Itinerary updated", "Dec 21: Dinner at Shilin Night Market, 7:00 PM", Navigator.CALENDAR));
        } else {
            // TODO: replace with the real latest expense from the split-bill page
            addMessage(ChatMessage.card(kind, MockData.CURRENT_USER, true,
                    "New expense added", "Taxi to hotel - $60 split " + trip.getMembers().size() + " ways",
                    Navigator.SPLIT_BILL));
        }
    }

    /** Fake a teammate answering after a moment (remove once a real backend exists). */
    private void scheduleMockReply() {
        final TripGroup replyTrip = trip;
        List<Member> others = new ArrayList<>();
        for (Member m : trip.getMembers()) {
            if (!m.getName().equals(MockData.CURRENT_USER)) others.add(m);
        }
        if (others.isEmpty()) return;
        final Member who = others.get(random.nextInt(others.size()));
        final String[] replies = { "Got it!", "Sounds great", "Haha, agreed", "Let me check the itinerary", "OK see you there" };
        Timer t = new Timer(1200, e -> {
            if (replyTrip == trip) { // user may have switched trips meanwhile
                addMessage(ChatMessage.text(who.getName(), false, replies[random.nextInt(replies.length)]));
            }
        });
        t.setRepeats(false);
        t.start();
    }

    // ------------------------------------------------------------------
    // Building message rows
    // ------------------------------------------------------------------
    private JComponent buildRow(ChatMessage m) {
        if (m.getKind() == ChatMessage.Kind.SYSTEM) return buildSystemRow(m);

        boolean mine = m.isMine();
        JComponent content = (m.getKind() == ChatMessage.Kind.TEXT) ? buildBubble(m) : buildCard(m);
        float align = mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT;
        content.setAlignmentX(align);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        if (!mine) {
            JLabel name = new JLabel(m.getSender());
            name.setFont(Theme.font(Font.BOLD, 11));
            name.setForeground(Theme.TEXT_MUTED);
            name.setAlignmentX(align);
            name.setBorder(new EmptyBorder(0, 4, 3, 0));
            stack.add(name);
        }
        stack.add(content);
        JLabel time = new JLabel(m.getTime());
        time.setFont(Theme.font(Font.PLAIN, 10));
        time.setForeground(Theme.TEXT_MUTED);
        time.setAlignmentX(align);
        time.setBorder(new EmptyBorder(3, 4, 0, 4));
        stack.add(time);

        JPanel row = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 16, 5, 16));

        if (mine) {
            row.add(stack, BorderLayout.EAST);
        } else {
            JPanel avatarTop = new JPanel(new BorderLayout());
            avatarTop.setOpaque(false);
            avatarTop.add(new Theme.Avatar(m.getSender(), 32), BorderLayout.NORTH);

            JPanel side = new JPanel(new BorderLayout(8, 0));
            side.setOpaque(false);
            side.add(avatarTop, BorderLayout.WEST);
            side.add(stack, BorderLayout.CENTER);
            row.add(side, BorderLayout.WEST);
        }
        return row;
    }

    private JComponent buildSystemRow(ChatMessage m) {
        JLabel label = new JLabel(m.getText());
        label.setFont(Theme.font(Font.PLAIN, 12));
        label.setForeground(Theme.PRIMARY);
        Theme.RoundedPanel chip = new Theme.RoundedPanel(new BorderLayout(), Theme.PALE, null, 14);
        chip.setBorder(new EmptyBorder(4, 12, 4, 12));
        chip.add(label);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.add(chip);
        return row;
    }

    private JComponent buildBubble(ChatMessage m) {
        boolean mine = m.isMine();
        JTextArea area = new JTextArea(m.getText());
        area.setFont(Theme.font(Font.PLAIN, 14));
        area.setForeground(mine ? Theme.WHITE : Theme.PRIMARY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setBorder(null);

        // Size the text to its content, but wrap once it reaches MAX_TEXT_WIDTH.
        FontMetrics fm = area.getFontMetrics(area.getFont());
        int w = 0;
        for (String line : m.getText().split("\n")) w = Math.max(w, fm.stringWidth(line));
        w = Math.min(MAX_TEXT_WIDTH, Math.max(20, w + 8));
        area.setSize(w, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(w, area.getPreferredSize().height));

        Theme.RoundedPanel bubble = new Theme.RoundedPanel(new BorderLayout(),
                mine ? Theme.PRIMARY : Theme.WHITE, mine ? null : Theme.BORDER, 18);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));
        bubble.add(area, BorderLayout.CENTER);
        bubble.setMaximumSize(bubble.getPreferredSize());
        return bubble;
    }

    private JComponent buildCard(ChatMessage m) {
        boolean itinerary = m.getKind() == ChatMessage.Kind.ITINERARY_CARD;

        JLabel tag = new JLabel(itinerary ? "ITINERARY" : "SPLIT BILL");
        tag.setFont(Theme.font(Font.BOLD, 10));
        tag.setForeground(Theme.WHITE);
        Theme.RoundedPanel tagChip = new Theme.RoundedPanel(new BorderLayout(), Theme.PRIMARY, null, 10);
        tagChip.setBorder(new EmptyBorder(2, 8, 2, 8));
        tagChip.add(tag);
        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tagRow.setOpaque(false);
        tagRow.add(tagChip);

        JLabel title = new JLabel(m.getText());
        title.setFont(Theme.font(Font.BOLD, 15));
        title.setForeground(Theme.PRIMARY);

        JLabel detail = new JLabel("<html><body style='width:240px'>" + escape(m.getDetail()) + "</body></html>");
        detail.setFont(Theme.font(Font.PLAIN, 13));
        detail.setForeground(Theme.TEXT_MUTED);

        Theme.RoundedButton open = Theme.outlineButton(itinerary ? "View itinerary" : "View bill");
        open.setBorder(new EmptyBorder(6, 14, 6, 14));
        open.addActionListener(e -> navigator.navigateTo(m.getTargetPage()));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(open);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(tagRow);
        body.add(Box.createVerticalStrut(8));
        body.add(title);
        body.add(Box.createVerticalStrut(4));
        body.add(detail);
        body.add(Box.createVerticalStrut(10));
        body.add(btnRow);

        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(), Theme.WHITE, Theme.PRIMARY, 16);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.add(body, BorderLayout.CENTER);
        Dimension d = new Dimension(290, card.getPreferredSize().height);
        card.setPreferredSize(d);
        card.setMaximumSize(d);
        return card;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private void refresh() {
        messageColumn.revalidate();
        messageColumn.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    /** A panel that always matches the scroll pane's width, so rows never scroll sideways. */
    private static class ScrollableColumn extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
