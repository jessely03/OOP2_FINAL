import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * NaviGo colour palette (white + dark blue), fonts and a few small reusable
 * Swing components so every page looks the same.
 */
public final class Theme {
    private Theme() {}

    public static final Color NAVY       = new Color(0x0B2A5B);
    public static final Color NAVY_HOVER = new Color(0x163A75);
    public static final Color WHITE      = Color.WHITE;
    public static final Color BG         = new Color(0xF3F6FB); // page background
    public static final Color PALE       = new Color(0xE6EDF8); // chips, hover
    public static final Color BORDER     = new Color(0xD6DFEE);
    public static final Color TEXT_MUTED = new Color(0x6B7A99);
    public static final Color ERROR      = new Color(0xB3261E);
    public static final Color ONLINE     = new Color(0x2E9E6B);

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // ------------------------------------------------------------------
    // Rounded panel (used for chat bubbles, cards, chips)
    // ------------------------------------------------------------------
    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color line;
        private final int arc;

        public RoundedPanel(LayoutManager layout, Color fill, Color line, int arc) {
            super(layout);
            this.fill = fill;
            this.line = line;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------
    // Rounded button: primary (filled navy) or outline (white + navy border)
    // ------------------------------------------------------------------
    public static class RoundedButton extends JButton {
        private final boolean filled;
        private boolean hover;

        public RoundedButton(String text, boolean filled) {
            super(text);
            this.filled = filled;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(filled ? WHITE : NAVY);
            setFont(font(Font.BOLD, 13));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (filled) {
                g2.setColor(hover ? NAVY_HOVER : NAVY);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            } else {
                g2.setColor(hover ? PALE : WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.setColor(NAVY);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            }
            g2.dispose();
            super.paintComponent(g); // draws the text
        }
    }

    public static RoundedButton primaryButton(String text) { return new RoundedButton(text, true); }
    public static RoundedButton outlineButton(String text) { return new RoundedButton(text, false); }

    // ------------------------------------------------------------------
    // Round avatar with the member's first letter
    // ------------------------------------------------------------------
    public static class Avatar extends JComponent {
        private final String letter;
        private final int size;
        private final Color color;

        public Avatar(String name, int size) {
            this.letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            this.size = size;
            Color[] shades = { new Color(0x0B2A5B), new Color(0x1F4A8C), new Color(0x2F5DA8), new Color(0x3F6FB8) };
            this.color = shades[Math.abs(name.hashCode()) % shades.length];
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(WHITE);
            g2.setFont(font(Font.BOLD, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(letter, x, y);
            g2.dispose();
        }
    }
}
