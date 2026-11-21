import javax.swing.*;
import java.awt.*;

/**
 * Demo main window: one JFrame, pages are JPanels switched by a CardLayout.
 * In the real project, keep your team's MainFrame and just add:
 *     cards.add(new GroupPage(this), GROUP);
 * (and make MainFrame implement Navigator).
 */
public class MainFrame extends JFrame implements Navigator {

    // page names used by CardLayout
    public static final String HOME       = "home";
    public static final String GROUP      = "group";
    public static final String CALENDAR   = "calendar";
    public static final String ITINERARY  = "itinerary";
    public static final String MAP        = "map";
    public static final String SPLIT_BILL = "splitBill";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public MainFrame() {
        super("NaviGo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        cards.add(new GroupPage(this), GROUP);
        // Placeholders - teammates replace these with their real pages.
        cards.add(placeholder("Home", "Open group page", GROUP), HOME);
        cards.add(placeholder("Calendar", "Back to group", GROUP), CALENDAR);
        cards.add(placeholder("Itinerary", "Back to group", GROUP), ITINERARY);
        cards.add(placeholder("Map", "Back to group", GROUP), MAP);
        cards.add(placeholder("Split Bill", "Back to group", GROUP), SPLIT_BILL);

        setContentPane(cards);
        navigateTo(GROUP);
    }

    @Override
    public void navigateTo(String pageName) {
        cardLayout.show(cards, pageName);
    }

    private JPanel placeholder(String title, String buttonText, String target) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        JLabel l = new JLabel(title + " page (placeholder)");
        l.setFont(Theme.font(Font.BOLD, 22));
        l.setForeground(Theme.NAVY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        Theme.RoundedButton b = Theme.primaryButton(buttonText);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addActionListener(e -> navigateTo(target));
        box.add(l);
        box.add(Box.createVerticalStrut(14));
        box.add(b);
        p.add(box);
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
