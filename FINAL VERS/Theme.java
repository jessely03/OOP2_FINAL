import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * NaviGo colour palette (forest green, lime and warm whites), fonts and a few small reusable
 * Swing components so every page looks the same.
 */
public final class Theme {
    private Theme() {}

    public static final Color PRIMARY = new Color(0x05452D);
    public static final Color PRIMARY_HOVER = new Color(0x20594A);
    public static final Color INK = new Color(0x18312D);
    public static final Color LIME = new Color(0xA8F62B);
    public static final Color WHITE = Color.WHITE;
    public static final Color BG = new Color(0xF6F7EF);
    public static final Color PALE = new Color(0xE5EFD9);
    public static final Color BORDER = new Color(0xD7E4D3);
    public static final Color TEXT_MUTED = new Color(0x526661);
    public static final Color ERROR = new Color(0xB3261E);
    public static final Color ERROR_BG = new Color(0xFDE8EC);
    public static final Color ERROR_BORDER = new Color(0xF4CED4);
    public static final Color ONLINE = new Color(0x2E9E6B);

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static void applyDefaults() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.foreground", PRIMARY);
        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.select", PALE);
        UIManager.put("TextField.selectionBackground", PALE);
        UIManager.put("TextArea.selectionBackground", PALE);
        UIManager.put("ComboBox.selectionBackground", PALE);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("List.selectionBackground", PALE);
        UIManager.put("List.selectionForeground", INK);
    }

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
    // Rounded button: primary (filled green) or outline (white + green border)
    // ------------------------------------------------------------------
    public static class RoundedButton extends JButton {
        private final boolean outline;

        public RoundedButton(String text, boolean filled) {
            this(text, filled ? PRIMARY : WHITE, filled ? WHITE : PRIMARY, !filled);
        }

        public RoundedButton(String text, Color fill, Color textColor, boolean outline) {
            super(text);
            this.outline = outline;
            putClientProperty("html.disable", Boolean.TRUE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBackground(fill);
            setForeground(textColor);
            setFont(font(Font.BOLD, 13));
            setBorder(new EmptyBorder(9, 13, 9, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getBackground();
            if (getModel().isArmed() || getModel().isRollover()) {
                fill = fill.equals(PRIMARY) ? PRIMARY_HOVER
                        : fill.equals(WHITE) || fill.equals(BG) ? PALE : fill.darker();
            }
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width, height, 12, 12);
            if (outline) {
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, width, height, 12, 12);
            }
            if (isFocusOwner()) {
                g2.setColor(getForeground());
                g2.drawRoundRect(2, 2, width - 4, height - 4, 9, 9);
            }
            g2.dispose();
            super.paintComponent(g);
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
            Color[] shades = { PRIMARY, PRIMARY_HOVER, INK };
            this.color = shades[Math.floorMod(name.hashCode(), shades.length)];
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
