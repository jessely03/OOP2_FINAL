import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Group page (a JPanel, shown inside MainFrame's CardLayout - no new window).
 *
 * +---------------------------------------------------------------+
 * | <  Taiwan Trip  Dec 20 - Dec 25                  4 members    |  header (dark blue)
 * +---------------------------------------------------------------+
 * | [Calendar] [Itinerary] [Map] [Split Bill]                     |  shortcuts
 * +----------------+----------------------------------------------+
 * | Members        |  chat messages ...                           |
 * |  Alice  Host   |                                              |
 * |  ...           |                                              |
 * | Invite code    |                                              |
 * | Join a trip    |  [+] [ type a message ........ ] [Send]      |
 * +----------------+----------------------------------------------+
 */
public class GroupPage extends JPanel {

    private final Navigator navigator;
    private final ChatPanel chatPanel;

    private final JLabel tripName = new JLabel();
    private final JLabel tripDates = new JLabel();
    private final JLabel memberCount = new JLabel();

    private final JPanel memberList = new JPanel();
    private final JLabel membersTitle = new JLabel();
    private final JLabel inviteCodeLabel = new JLabel();
    private final JTextField joinField = new JTextField();
    private final JLabel joinMessage = new JLabel(" ");

    private TripGroup trip;

    public GroupPage(Navigator navigator) {
        this.navigator = navigator;
        this.chatPanel = new ChatPanel(navigator);

        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildShortcutBar(), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);

        setTrip(MockData.defaultTrip());
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------
    /** Show another trip (called at start, and after joining with a code). */
    public void setTrip(TripGroup trip) {
        this.trip = trip;
        tripName.setText(trip.getName());
        tripDates.setText(trip.getDates());
        inviteCodeLabel.setText(trip.getInviteCode());
        refreshMembers();
        chatPanel.setTrip(trip);
    }

    // ------------------------------------------------------------------
    // Header + shortcut bar
    // ------------------------------------------------------------------
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(Theme.NAVY);
        header.setBorder(new EmptyBorder(14, 16, 14, 20));

        JButton back = new JButton("←");
        back.setFont(Theme.font(Font.BOLD, 20));
        back.setForeground(Theme.WHITE);
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.setToolTipText("Back to home");
        back.addActionListener(e -> navigator.navigateTo(MainFrame.HOME));

        tripName.setFont(Theme.font(Font.BOLD, 20));
        tripName.setForeground(Theme.WHITE);
        tripDates.setFont(Theme.font(Font.PLAIN, 13));
        tripDates.setForeground(new Color(0xC5D3EC));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(tripName);
        titles.add(tripDates);

        memberCount.setFont(Theme.font(Font.BOLD, 12));
        memberCount.setForeground(Theme.NAVY);
        Theme.RoundedPanel chip = new Theme.RoundedPanel(new BorderLayout(), Theme.WHITE, null, 16);
        chip.setBorder(new EmptyBorder(5, 12, 5, 12));
        chip.add(memberCount);
        JPanel chipWrap = new JPanel(new GridBagLayout());
        chipWrap.setOpaque(false);
        chipWrap.add(chip);

        header.add(back, BorderLayout.WEST);
        header.add(titles, BorderLayout.CENTER);
        header.add(chipWrap, BorderLayout.EAST);
        return header;
    }

    private JComponent buildShortcutBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        bar.setBackground(Theme.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        bar.add(shortcut("Calendar", MainFrame.CALENDAR));
        bar.add(shortcut("Itinerary", MainFrame.ITINERARY));
        bar.add(shortcut("Map", MainFrame.MAP));
        bar.add(shortcut("Split Bill", MainFrame.SPLIT_BILL));
        return bar;
    }

    private JComponent shortcut(String label, String page) {
        Theme.RoundedButton b = Theme.outlineButton(label);
        b.addActionListener(e -> navigator.navigateTo(page));
        return b;
    }

    // ------------------------------------------------------------------
    // Sidebar: members, invite code, join
    // ------------------------------------------------------------------
    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.WHITE);
        side.setPreferredSize(new Dimension(260, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        membersTitle.setFont(Theme.font(Font.BOLD, 14));
        membersTitle.setForeground(Theme.NAVY);
        membersTitle.setBorder(new EmptyBorder(16, 16, 8, 16));

        memberList.setLayout(new BoxLayout(memberList, BoxLayout.Y_AXIS));
        memberList.setBackground(Theme.WHITE);
        JPanel listWrap = new JPanel(new BorderLayout());
        listWrap.setBackground(Theme.WHITE);
        listWrap.add(memberList, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(listWrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.WHITE);

        side.add(membersTitle, BorderLayout.NORTH);
        side.add(scroll, BorderLayout.CENTER);
        side.add(buildJoinSection(), BorderLayout.SOUTH);
        return side;
    }

    private JComponent buildJoinSection() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Theme.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(14, 16, 16, 16)));

        // --- invite code to share ---
        JLabel inviteTitle = smallTitle("Invite code");
        inviteCodeLabel.setFont(Theme.font(Font.BOLD, 15));
        inviteCodeLabel.setForeground(Theme.NAVY);
        Theme.RoundedButton copy = Theme.outlineButton("Copy");
        copy.setBorder(new EmptyBorder(5, 12, 5, 12));
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(inviteCodeLabel.getText()), null);
            copy.setText("Copied!");
            Timer t = new Timer(1200, ev -> copy.setText("Copy"));
            t.setRepeats(false);
            t.start();
        });
        Theme.RoundedPanel codeChip = new Theme.RoundedPanel(new BorderLayout(10, 0), Theme.PALE, null, 14);
        codeChip.setBorder(new EmptyBorder(6, 12, 6, 8));
        codeChip.add(inviteCodeLabel, BorderLayout.CENTER);
        codeChip.add(copy, BorderLayout.EAST);

        // --- join another trip ---
        JLabel joinTitle = smallTitle("Join a trip");
        Theme.RoundedPanel field = new Theme.RoundedPanel(new BorderLayout(), Theme.BG, Theme.BORDER, 14);
        field.setBorder(new EmptyBorder(4, 10, 4, 10));
        joinField.setBorder(null);
        joinField.setOpaque(false);
        joinField.setFont(Theme.font(Font.PLAIN, 14));
        joinField.setForeground(Theme.NAVY);
        joinField.setCaretColor(Theme.NAVY);
        joinField.addActionListener(e -> joinTrip());
        field.add(joinField, BorderLayout.CENTER);

        Theme.RoundedButton join = Theme.primaryButton("Join Trip");
        join.addActionListener(e -> joinTrip());

        joinMessage.setFont(Theme.font(Font.PLAIN, 12));
        joinMessage.setForeground(Theme.ERROR);

        for (JComponent c : new JComponent[] { inviteTitle, codeChip, joinTitle, field, join, joinMessage }) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        // keep each control as wide as the sidebar
        codeChip.setMaximumSize(new Dimension(Integer.MAX_VALUE, codeChip.getPreferredSize().height));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 6));
        join.setMaximumSize(new Dimension(Integer.MAX_VALUE, join.getPreferredSize().height));

        box.add(inviteTitle);
        box.add(Box.createVerticalStrut(6));
        box.add(codeChip);
        box.add(Box.createVerticalStrut(16));
        box.add(joinTitle);
        box.add(Box.createVerticalStrut(6));
        box.add(field);
        box.add(Box.createVerticalStrut(8));
        box.add(join);
        box.add(Box.createVerticalStrut(4));
        box.add(joinMessage);
        return box;
    }

    private JLabel smallTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private void joinTrip() {
        String code = joinField.getText().trim();
        if (code.isEmpty()) {
            joinMessage.setText("Please enter an invite code");
            return;
        }
        TripGroup found = MockData.findByCode(code);
        if (found == null) {
            joinMessage.setText("Code not found");
        } else if (found == trip) {
            joinMessage.setText("You are already in this trip");
        } else {
            joinField.setText("");
            joinMessage.setText(" ");
            setTrip(found);
            chatPanel.addMessage(ChatMessage.system("You joined " + found.getName()));
        }
    }

    // ------------------------------------------------------------------
    // Member list
    // ------------------------------------------------------------------
    private void refreshMembers() {
        memberList.removeAll();
        for (Member m : trip.getMembers()) {
            memberList.add(buildMemberRow(m));
        }
        int n = trip.getMembers().size();
        membersTitle.setText("Members (" + n + ")");
        memberCount.setText(n + (n == 1 ? " member" : " members"));
        memberList.revalidate();
        memberList.repaint();
    }

    private JComponent buildMemberRow(Member m) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setBackground(Theme.WHITE);
        row.setBorder(new EmptyBorder(6, 16, 6, 16));

        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(new Theme.Avatar(m.getName(), 34));

        JLabel name = new JLabel(m.getName());
        name.setFont(Theme.font(Font.BOLD, 14));
        name.setForeground(Theme.NAVY);
        JLabel role = new JLabel(m.getRole());
        role.setFont(Theme.font(Font.PLAIN, 12));
        role.setForeground(Theme.TEXT_MUTED);
        JPanel texts = new JPanel();
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.setOpaque(false);
        texts.add(name);
        texts.add(role);

        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(m.isOnline() ? Theme.ONLINE : Theme.BORDER);
                g2.fillOval(0, 0, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 10));
        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.add(dot);

        row.add(avatarWrap, BorderLayout.WEST);
        row.add(texts, BorderLayout.CENTER);
        row.add(dotWrap, BorderLayout.EAST);
        return row;
    }
}
