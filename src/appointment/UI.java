package appointment;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Common look (colours, fonts, rounded button, card) used by both panels. */
public class UI {

  public static final String FONT = "Segoe UI";
  public static final Color BLUE = new Color(31, 122, 224);
  public static final Color DARK_BLUE = new Color(20, 91, 174);
  public static final Color BG = new Color(245, 248, 252);
  public static final Color TEXT = new Color(35, 45, 60);
  public static final Color MUTED = new Color(110, 120, 135);
  public static final Color LINE = new Color(226, 232, 240);
  public static final Color RED = new Color(200, 60, 60);
  public static final Color RED_HOVER = new Color(222, 76, 76);
  public static final Color GREEN = new Color(41, 160, 95);
  public static final int SH = 8;
  public static final String RUPEE = "\u20B9";

  private UI() {
  }

  /** Page title + subtitle used at the top of each panel. */
  public static JComponent header(String title, String subtitle) {
    JPanel box = new JPanel();
    box.setOpaque(false);
    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
    box.setBorder(new EmptyBorder(24, 28 + SH, 12, 28));

    JLabel t = new JLabel(title);
    t.setFont(new Font(FONT, Font.BOLD, 30));
    t.setForeground(TEXT);
    t.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel s = new JLabel(subtitle);
    s.setFont(new Font(FONT, Font.PLAIN, 14));
    s.setForeground(MUTED);
    s.setAlignmentX(Component.LEFT_ALIGNMENT);

    box.add(t);
    box.add(Box.createVerticalStrut(4));
    box.add(s);
    return box;
  }

  public static void styleField(JTextField f) {
    f.setFont(new Font(FONT, Font.PLAIN, 14));
    f.setForeground(TEXT);
    f.setPreferredSize(new Dimension(100, 40));
    f.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(LINE),
        new EmptyBorder(0, 12, 0, 12)));
  }

  public static void styleCombo(JComboBox<?> c) {
    c.setFont(new Font(FONT, Font.PLAIN, 14));
    c.setBackground(Color.WHITE);
    c.setForeground(TEXT);
    c.setPreferredSize(new Dimension(100, 40));
  }

  /** Button with rounded corners, hover and pressed colours. */
  public static class RoundedButton extends JButton {
    private final Color normal, hoverColor;
    private final int radius;
    private boolean hover;

    public RoundedButton(String text, Color normal, Color hoverColor, int radius) {
      super(text);
      this.normal = normal;
      this.hoverColor = hoverColor;
      this.radius = radius;
      setForeground(Color.WHITE);
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          hover = true;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          hover = false;
          repaint();
        }
      });
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color c = hover ? hoverColor : normal;
      if (getModel().isPressed())
        c = c.darker();
      g2.setColor(c);
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
      g2.dispose();
      super.paintComponent(g);
    }
  }

  /** White rounded card with soft shadow. */
  public static class Card extends JPanel {
    private static final int RADIUS = 26;

    public Card(int top, int left, int bottom, int right) {
      setOpaque(false);
      setBorder(new EmptyBorder(top + SH, left + SH, bottom + SH, right + SH));
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth() - 2 * SH;
      int h = getHeight() - 2 * SH;
      for (int i = 1; i <= 6; i++) {
        g2.setColor(new Color(15, 23, 42, 4));
        g2.fillRoundRect(SH - i, SH - i + 2, w + 2 * i, h + 2 * i, RADIUS + i, RADIUS + i);
      }
      g2.setColor(Color.WHITE);
      g2.fillRoundRect(SH, SH, w, h, RADIUS, RADIUS);
      g2.setColor(LINE);
      g2.drawRoundRect(SH, SH, w - 1, h - 1, RADIUS, RADIUS);
      g2.dispose();
      super.paintComponent(g);
    }
  }
}