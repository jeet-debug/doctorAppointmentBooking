package doctor;

import database.DB;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * All Doctors page: browse, search, filter, activate/deactivate and delete.
 * Stays in sync with the rest of the app through {@link DoctorEvents}.
 */
public class AllDoctorsPanel extends JPanel {

        private static final String FONT = "Segoe UI";

        private static final Color BLUE = new Color(31, 122, 224);
        private static final Color DARK_BLUE = new Color(20, 91, 174);
        private static final Color LIGHT_BLUE = new Color(232, 241, 253);
        private static final Color BG = new Color(243, 246, 251);
        private static final Color TEXT = new Color(35, 45, 60);
        private static final Color MUTED = new Color(110, 120, 135);
        private static final Color BORDER = new Color(214, 222, 233);
        private static final Color GREEN = new Color(22, 163, 74);
        private static final Color GREEN_BG = new Color(220, 246, 230);
        private static final Color RED = new Color(220, 53, 69);
        private static final Color DARK_RED = new Color(185, 38, 53);
        private static final Color RED_BG = new Color(252, 226, 229);
        private static final Color ORANGE = new Color(217, 119, 6);

        private static final Color[] AVATAR_COLORS = {
                        new Color(239, 68, 68), new Color(236, 72, 153), new Color(139, 92, 246),
                        new Color(99, 102, 241), new Color(14, 165, 233), new Color(13, 148, 136),
                        new Color(245, 158, 11), new Color(34, 197, 94)
        };

        private static final String ALL_SPEC = "All Specializations";
        private static final String ALL_STATUS = "All Status";

        private static final int CARD_W = 260;
        private static final int CARD_H = 392;
        private static final int PHOTO_W = 228; // CARD_W - 14 - 18
        private static final int PHOTO_H = 135;

        // ---------- state ----------
        private final List<DoctorData> all = new ArrayList<>();
        private boolean loading = false;

        // ---------- ui ----------
        private JPanel cardsContainer;
        private HintTextField searchField;
        private JComboBox<String> specBox, statusBox;
        private JLabel resultLabel;
        private JLabel totalNum, activeNum, inactiveNum;
        private StatCard totalCard, activeCard, inactiveCard;

        public AllDoctorsPanel() {
                setLayout(new BorderLayout());
                setBackground(BG);
                createUI();
                loadDoctors();

                // stay in sync with Add Doctor / other pages
                DoctorEvents.addListener(this::loadDoctors);
        }

        // =====================================================
        // UI HELPERS
        // =====================================================

        private static class CardPanel extends JPanel {
                CardPanel(LayoutManager layout) {
                        super(layout);
                        setOpaque(false);
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth() - 4, h = getHeight() - 4;
                        g2.setColor(new Color(20, 40, 80, 18));
                        g2.fillRoundRect(3, 4, w, h, 18, 18);
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(0, 0, w, h, 18, 18);
                        g2.dispose();
                        super.paintComponent(g);
                }
        }

        private static class RoundedBorder extends AbstractBorder {
                private final Color color;
                private final int radius;

                RoundedBorder(Color color, int radius) {
                        this.color = color;
                        this.radius = radius;
                }

                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(color);
                        g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
                        g2.dispose();
                }

                @Override
                public Insets getBorderInsets(Component c) {
                        return new Insets(6, 12, 6, 12);
                }
        }

        private static class PillLabel extends JLabel {
                private final Color pillColor;

                PillLabel(String text, Color bg, Color fg) {
                        super(text, SwingConstants.CENTER);
                        this.pillColor = bg;
                        setForeground(fg);
                        setFont(new Font(FONT, Font.BOLD, 12));
                        setBorder(new EmptyBorder(4, 12, 4, 12));
                        setOpaque(false);
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(pillColor);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                        g2.dispose();
                        super.paintComponent(g);
                }
        }

        /** Rounded button with hover effect; line may be null for no outline. */
        private static class ActionButton extends JButton {
                private final Color bg, hoverBg, line;
                private boolean hover = false;

                ActionButton(String text, Color fg, Color bg, Color hoverBg, Color line, Color hoverFg) {
                        super(text);
                        this.bg = bg;
                        this.hoverBg = hoverBg;
                        this.line = line;
                        setFont(new Font(FONT, Font.BOLD, 12));
                        setForeground(fg);
                        setFocusPainted(false);
                        setBorderPainted(false);
                        setContentAreaFilled(false);
                        setOpaque(false);
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        setPreferredSize(new Dimension(100, 34));
                        addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseEntered(MouseEvent e) {
                                        hover = true;
                                        setForeground(hoverFg);
                                        repaint();
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                        hover = false;
                                        setForeground(fg);
                                        repaint();
                                }
                        });
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(hover ? hoverBg : bg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        if (line != null) {
                                g2.setColor(line);
                                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                        }
                        g2.dispose();
                        super.paintComponent(g);
                }
        }

        private static class ModernButton extends JButton {
                private boolean hover = false;

                ModernButton(String text) {
                        super(text);
                        setFont(new Font(FONT, Font.BOLD, 13));
                        setForeground(BLUE);
                        setFocusPainted(false);
                        setBorderPainted(false);
                        setContentAreaFilled(false);
                        setOpaque(false);
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        setPreferredSize(new Dimension(110, 38));
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
                        g2.setColor(hover ? LIGHT_BLUE : Color.WHITE);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.setColor(BLUE);
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                        g2.dispose();
                        super.paintComponent(g);
                }
        }

        private static class HintTextField extends JTextField {
                private final String hint;

                HintTextField(String hint) {
                        this.hint = hint;
                }

                @Override
                protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (getText().isEmpty()) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                                g2.setColor(new Color(150, 160, 175));
                                g2.setFont(getFont());
                                Insets in = getInsets();
                                FontMetrics fm = g2.getFontMetrics();
                                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                                g2.dispose();
                        }
                }
        }

        private void styleInput(JComponent c) {
                c.setFont(new Font(FONT, Font.PLAIN, 13));
                c.setForeground(TEXT);
                c.setBackground(Color.WHITE);
                c.setBorder(new RoundedBorder(BORDER, 12));
                if (c instanceof JTextField) {
                        ((JTextField) c).setCaretColor(BLUE);
                }
                c.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                                c.setBorder(new RoundedBorder(BLUE, 12));
                        }

                        @Override
                        public void focusLost(FocusEvent e) {
                                c.setBorder(new RoundedBorder(BORDER, 12));
                        }
                });
        }

        private static class WrapLayout extends FlowLayout {
                WrapLayout(int align, int hgap, int vgap) {
                        super(align, hgap, vgap);
                }

                @Override
                public Dimension preferredLayoutSize(Container target) {
                        return layoutSize(target);
                }

                @Override
                public Dimension minimumLayoutSize(Container target) {
                        Dimension d = layoutSize(target);
                        d.width -= (getHgap() + 1);
                        return d;
                }

                private Dimension layoutSize(Container target) {
                        synchronized (target.getTreeLock()) {
                                Container c = target;
                                while (c.getSize().width == 0 && c.getParent() != null) {
                                        c = c.getParent();
                                }
                                int targetWidth = c.getSize().width;
                                if (targetWidth == 0) {
                                        targetWidth = Integer.MAX_VALUE;
                                }

                                int hgap = getHgap(), vgap = getVgap();
                                Insets in = target.getInsets();
                                int maxWidth = targetWidth - (in.left + in.right + hgap * 2);

                                Dimension dim = new Dimension(0, 0);
                                int rowW = 0, rowH = 0;

                                for (int i = 0; i < target.getComponentCount(); i++) {
                                        Component m = target.getComponent(i);
                                        if (!m.isVisible())
                                                continue;
                                        Dimension d = m.getPreferredSize();
                                        if (rowW + d.width > maxWidth && rowW > 0) {
                                                dim.width = Math.max(dim.width, rowW);
                                                dim.height += rowH + vgap;
                                                rowW = 0;
                                                rowH = 0;
                                        }
                                        if (rowW != 0)
                                                rowW += hgap;
                                        rowW += d.width;
                                        rowH = Math.max(rowH, d.height);
                                }
                                dim.width = Math.max(dim.width, rowW);
                                dim.height += rowH;
                                dim.width += in.left + in.right + hgap * 2;
                                dim.height += in.top + in.bottom + vgap * 2;
                                return dim;
                        }
                }
        }

        private static class ScrollablePanel extends JPanel implements Scrollable {
                ScrollablePanel(LayoutManager lm) {
                        super(lm);
                }

                @Override
                public Dimension getPreferredScrollableViewportSize() {
                        return getPreferredSize();
                }

                @Override
                public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
                        return 24;
                }

                @Override
                public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
                        return 120;
                }

                @Override
                public boolean getScrollableTracksViewportWidth() {
                        return true;
                }

                @Override
                public boolean getScrollableTracksViewportHeight() {
                        return false;
                }
        }

        // =====================================================
        // DATA
        // =====================================================

        private static class DoctorData {
                int id;
                String name, specialization, phone, email, gender;
                int experience;
                double fee;
                String days, time, imagePath, status;
                BufferedImage photo;

                boolean isActive() {
                        return "Active".equalsIgnoreCase(status);
                }

                String displayName() {
                        String n = name == null ? "" : name;
                        return n.toLowerCase().startsWith("dr") ? n : "Dr. " + n;
                }
        }

        private static String nz(String s) {
                return s == null ? "" : s;
        }

        private static String dash(String s) {
                return (s == null || s.isBlank()) ? "-" : s;
        }

        // =====================================================
        // PHOTO COMPONENT (rectangle + status pill on top)
        // =====================================================

        private static class PhotoComponent extends JComponent {
                private final BufferedImage photo;
                private final String initials, status;
                private final boolean active;
                private final Color color;

                PhotoComponent(BufferedImage photo, String name, int colorIndex, boolean active, String status) {
                        this.photo = photo;
                        this.initials = initialsOf(name);
                        this.active = active;
                        this.status = nz(status);
                        this.color = AVATAR_COLORS[Math.abs(colorIndex) % AVATAR_COLORS.length];
                        Dimension d = new Dimension(PHOTO_W, PHOTO_H);
                        setPreferredSize(d);
                        setMaximumSize(d);
                        setMinimumSize(d);
                }

                private static String initialsOf(String name) {
                        if (name == null)
                                return "?";
                        String n = name.replaceFirst("(?i)^dr\\.?\\s*", "").trim();
                        if (n.isEmpty())
                                return "?";
                        String[] p = n.split("\\s+");
                        String s = p[0].substring(0, 1);
                        if (p.length > 1)
                                s += p[p.length - 1].substring(0, 1);
                        return s.toUpperCase();
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                        int w = getWidth(), h = getHeight();
                        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12);

                        if (photo != null) {
                                g2.setClip(shape);
                                double scale = Math.max((double) w / photo.getWidth(),
                                                (double) h / photo.getHeight());
                                int sw = (int) Math.round(w / scale);
                                int sh = (int) Math.round(h / scale);
                                int sx = (photo.getWidth() - sw) / 2;
                                int sy = (photo.getHeight() - sh) / 2;
                                g2.drawImage(photo, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
                                g2.setClip(null);
                                g2.setColor(new Color(0, 0, 0, 25));
                                g2.draw(shape);
                        } else {
                                g2.setPaint(new GradientPaint(0, 0, color.brighter(), w, h, color.darker()));
                                g2.fill(shape);
                                g2.setColor(Color.WHITE);
                                g2.setFont(new Font(FONT, Font.BOLD, 40));
                                FontMetrics fm = g2.getFontMetrics();
                                g2.drawString(initials,
                                                (w - fm.stringWidth(initials)) / 2,
                                                (h - fm.getHeight()) / 2 + fm.getAscent());
                        }

                        // status pill (top-right)
                        if (!status.isEmpty()) {
                                g2.setFont(new Font(FONT, Font.BOLD, 10));
                                FontMetrics fm = g2.getFontMetrics();
                                int pw = fm.stringWidth(status) + 20, ph = 20;
                                int px = w - pw - 8, py = 8;
                                g2.setColor(active ? GREEN_BG : RED_BG);
                                g2.fillRoundRect(px, py, pw, ph, ph, ph);
                                g2.setColor(active ? GREEN : RED);
                                g2.fillOval(px + 7, py + ph / 2 - 3, 6, 6);
                                g2.drawString(status, px + 16, py + (ph - fm.getHeight()) / 2 + fm.getAscent());
                        }
                        g2.dispose();
                }
        }

        // =====================================================
        // DOCTOR CARD
        // =====================================================

        private class DoctorCard extends JPanel {
                private boolean hover = false;

                DoctorCard(DoctorData d) {
                        setOpaque(false);
                        setLayout(new BorderLayout());
                        setPreferredSize(new Dimension(CARD_W, CARD_H));
                        setBorder(new EmptyBorder(14, 14, 14, 18));

                        // ---------- head ----------
                        PhotoComponent photo = new PhotoComponent(d.photo, d.name, d.id, d.isActive(), d.status);
                        photo.setAlignmentX(CENTER_ALIGNMENT);

                        JLabel name = new JLabel(d.displayName(), SwingConstants.CENTER);
                        name.setFont(new Font(FONT, Font.BOLD, 15));
                        name.setForeground(TEXT);
                        name.setAlignmentX(CENTER_ALIGNMENT);
                        name.setMaximumSize(new Dimension(PHOTO_W, 22));
                        name.setToolTipText(d.displayName());

                        PillLabel spec = new PillLabel(nz(d.specialization), LIGHT_BLUE, BLUE);
                        spec.setFont(new Font(FONT, Font.BOLD, 11));
                        spec.setBorder(new EmptyBorder(3, 12, 3, 12));
                        spec.setAlignmentX(CENTER_ALIGNMENT);
                        spec.setMaximumSize(new Dimension(PHOTO_W, 22));
                        spec.setToolTipText(nz(d.specialization));

                        JPanel head = new JPanel();
                        head.setOpaque(false);
                        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
                        head.add(photo);
                        head.add(Box.createVerticalStrut(10));
                        head.add(name);
                        head.add(Box.createVerticalStrut(4));
                        head.add(spec);
                        head.add(Box.createVerticalStrut(10));

                        // ---------- info ----------
                        JPanel info = new JPanel();
                        info.setOpaque(false);
                        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                        info.setBorder(new javax.swing.border.MatteBorder(1, 0, 0, 0, new Color(232, 237, 245)));
                        info.add(Box.createVerticalStrut(6));
                        info.add(row("Experience", d.experience + " yrs"));
                        info.add(row("Fee", "\u20B9 " + (d.fee == Math.floor(d.fee)
                                        ? String.valueOf((long) d.fee)
                                        : String.format("%.2f", d.fee))));
                        info.add(row("Open", dash(d.days)));
                        info.add(row("Timing", dash(d.time)));
                        info.add(row("Phone", dash(d.phone)));
                        info.add(row("Email", dash(d.email)));

                        // ---------- actions ----------
                        ActionButton toggle = d.isActive()
                                        ? new ActionButton("Deactivate", ORANGE, Color.WHITE,
                                                        new Color(254, 243, 226), ORANGE, ORANGE)
                                        : new ActionButton("Activate", GREEN, Color.WHITE,
                                                        GREEN_BG, GREEN, GREEN);
                        ActionButton delete = new ActionButton("Delete", Color.WHITE, RED, DARK_RED, null,
                                        Color.WHITE);
                        toggle.addActionListener(e -> toggleStatus(d));
                        delete.addActionListener(e -> deleteDoctor(d));

                        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
                        actions.setOpaque(false);
                        actions.setBorder(new EmptyBorder(10, 0, 0, 0));
                        actions.add(toggle);
                        actions.add(delete);

                        add(head, BorderLayout.NORTH);
                        add(info, BorderLayout.CENTER);
                        add(actions, BorderLayout.SOUTH);

                        addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseEntered(MouseEvent e) {
                                        hover = true;
                                        repaint();
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                        Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
                                                        DoctorCard.this);
                                        if (!contains(p)) {
                                                hover = false;
                                                repaint();
                                        }
                                }
                        });
                }

                private JPanel row(String label, String value) {
                        JPanel p = new JPanel(new BorderLayout(8, 0));
                        p.setOpaque(false);
                        p.setBorder(new EmptyBorder(2, 0, 2, 0));

                        JLabel l = new JLabel(label);
                        l.setFont(new Font(FONT, Font.PLAIN, 11));
                        l.setForeground(MUTED);
                        l.setPreferredSize(new Dimension(64, 16));

                        JLabel v = new JLabel(value);
                        v.setFont(new Font(FONT, Font.BOLD, 11));
                        v.setForeground(TEXT);
                        v.setToolTipText(value);

                        p.add(l, BorderLayout.WEST);
                        p.add(v, BorderLayout.CENTER);
                        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                        return p;
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth() - 4, h = getHeight() - 4;

                        g2.setColor(new Color(20, 40, 80, hover ? 38 : 18));
                        g2.fillRoundRect(3, hover ? 7 : 4, w, h, 18, 18);

                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(0, 0, w, h, 18, 18);

                        g2.setColor(hover ? BLUE : new Color(226, 232, 242));
                        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

                        g2.dispose();
                        super.paintComponent(g);
                }
        }

        // =====================================================
        // UI
        // =====================================================

        private void createUI() {

                // ---------- STATS ----------
                totalNum = new JLabel("0");
                activeNum = new JLabel("0");
                inactiveNum = new JLabel("0");

                JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
                statsRow.setOpaque(false);
                totalCard = statCard(totalNum, "Total Doctors", BLUE, ALL_STATUS);
                activeCard = statCard(activeNum, "Active", GREEN, "Active");
                inactiveCard = statCard(inactiveNum, "Inactive", RED, "Inactive");
                statsRow.add(totalCard);
                statsRow.add(activeCard);
                statsRow.add(inactiveCard);

                // ---------- TOOLBAR ----------
                searchField = new HintTextField("Search by name, specialization, phone or email...");
                styleInput(searchField);
                searchField.setPreferredSize(new Dimension(200, 38));
                searchField.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                applyFilter();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                applyFilter();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                applyFilter();
                        }
                });

                specBox = new JComboBox<>(new String[] { ALL_SPEC });
                styleInput(specBox);
                specBox.setPreferredSize(new Dimension(200, 38));
                specBox.addActionListener(e -> {
                        if (!loading)
                                applyFilter();
                });

                statusBox = new JComboBox<>(new String[] { ALL_STATUS, "Active", "Inactive" });
                styleInput(statusBox);
                statusBox.setPreferredSize(new Dimension(130, 38));
                statusBox.addActionListener(e -> {
                        if (!loading)
                                applyFilter();
                });

                JButton refreshBtn = new ModernButton("Refresh");
                refreshBtn.addActionListener(e -> loadDoctors());

                JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                filters.setOpaque(false);
                filters.add(specBox);
                filters.add(statusBox);
                filters.add(refreshBtn);

                JPanel toolbar = new CardPanel(new BorderLayout(14, 0));
                toolbar.setBorder(new EmptyBorder(14, 16, 18, 20));
                toolbar.add(searchField, BorderLayout.CENTER);
                toolbar.add(filters, BorderLayout.EAST);

                JPanel top = new JPanel(new BorderLayout(0, 16));
                top.setOpaque(false);
                top.add(statsRow, BorderLayout.NORTH);
                top.add(toolbar, BorderLayout.CENTER);

                // ---------- CARDS ----------
                cardsContainer = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
                cardsContainer.setOpaque(false);
                cardsContainer.setBorder(new EmptyBorder(2, 2, 2, 2));

                JScrollPane cardsScroll = new JScrollPane(
                                cardsContainer,
                                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                cardsScroll.setBorder(new LineBorder(BORDER, 1, true));
                cardsScroll.getViewport().setBackground(new Color(250, 251, 254));
                cardsScroll.getVerticalScrollBar().setUnitIncrement(20);

                resultLabel = new JLabel(" ");
                resultLabel.setFont(new Font(FONT, Font.PLAIN, 12));
                resultLabel.setForeground(MUTED);

                JLabel listTitle = new JLabel("Doctor Directory");
                listTitle.setFont(new Font(FONT, Font.BOLD, 18));
                listTitle.setForeground(TEXT);

                JPanel listHead = new JPanel(new BorderLayout());
                listHead.setOpaque(false);
                listHead.setBorder(new EmptyBorder(0, 0, 14, 0));
                listHead.add(listTitle, BorderLayout.WEST);
                listHead.add(resultLabel, BorderLayout.EAST);

                JPanel listCard = new CardPanel(new BorderLayout());
                listCard.setBorder(new EmptyBorder(20, 20, 24, 24));
                listCard.add(listHead, BorderLayout.NORTH);
                listCard.add(cardsScroll, BorderLayout.CENTER);

                // ---------- MAIN ----------
                JPanel main = new JPanel(new BorderLayout(0, 18));
                main.setBackground(BG);
                main.setBorder(new EmptyBorder(20, 30, 25, 30));
                main.add(top, BorderLayout.NORTH);
                main.add(listCard, BorderLayout.CENTER);

                add(main, BorderLayout.CENTER);
        }

        /** Clickable stat card: clicking it filters the list by that status. */
        private class StatCard extends CardPanel {
                private final Color accent;
                private final String statusValue;
                private boolean selected = false, hover = false;

                StatCard(Color accent, String statusValue) {
                        super(new BorderLayout(0, 2));
                        this.accent = accent;
                        this.statusValue = statusValue;
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                        statusBox.setSelectedItem(StatCard.this.statusValue);
                                }

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

                void setSelected(boolean value) {
                        if (selected != value) {
                                selected = value;
                                repaint();
                        }
                }

                @Override
                protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (!selected && !hover)
                                return;
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth() - 4, h = getHeight() - 4;
                        if (selected) {
                                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                                g2.fillRoundRect(0, 0, w, h, 18, 18);
                                g2.setColor(accent);
                                g2.setStroke(new BasicStroke(2f));
                                g2.drawRoundRect(1, 1, w - 2, h - 2, 18, 18);
                        } else {
                                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
                                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                        }
                        g2.dispose();
                }
        }

        private StatCard statCard(JLabel number, String caption, Color accent, String statusValue) {
                number.setFont(new Font(FONT, Font.BOLD, 28));
                number.setForeground(accent);

                JLabel cap = new JLabel(caption);
                cap.setFont(new Font(FONT, Font.BOLD, 12));
                cap.setForeground(MUTED);

                StatCard p = new StatCard(accent, statusValue);
                p.setBorder(new EmptyBorder(14, 20, 18, 24));
                p.add(number, BorderLayout.CENTER);
                p.add(cap, BorderLayout.SOUTH);
                p.setToolTipText("Click to filter: " + caption);
                return p;
        }

        /** Highlights the stat card that matches the current status filter. */
        private void updateStatSelection() {
                if (totalCard == null || statusBox == null)
                        return;
                Object st = statusBox.getSelectedItem();
                totalCard.setSelected(ALL_STATUS.equals(st));
                activeCard.setSelected("Active".equals(st));
                inactiveCard.setSelected("Inactive".equals(st));
        }

        // =====================================================
        // LOAD + FILTER
        // =====================================================

        private void loadDoctors() {
                loading = true;
                all.clear();

                String sql = """
                                SELECT
                                    id,
                                    name,
                                    specialization,
                                    phone,
                                    email,
                                    gender,
                                    experience,
                                    consultation_fee,
                                    available_days,
                                    available_time,
                                    image_path,
                                    status
                                FROM doctors
                                ORDER BY name ASC
                                """;

                try (Connection con = DB.getConnection();
                                PreparedStatement ps = con.prepareStatement(sql);
                                ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                                DoctorData d = new DoctorData();
                                d.id = rs.getInt("id");
                                d.name = rs.getString("name");
                                d.specialization = rs.getString("specialization");
                                d.phone = rs.getString("phone");
                                d.email = rs.getString("email");
                                d.gender = rs.getString("gender");
                                d.experience = rs.getInt("experience");
                                d.fee = rs.getDouble("consultation_fee");
                                d.days = rs.getString("available_days");
                                d.time = rs.getString("available_time");
                                d.imagePath = rs.getString("image_path");
                                d.status = rs.getString("status");
                                d.photo = loadPhoto(d.imagePath);
                                all.add(d);
                        }

                } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                                        "Unable to load doctors:\n" + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                }

                // rebuild specialization filter, keeping the current choice
                Object previous = specBox.getSelectedItem();
                TreeSet<String> specs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (DoctorData d : all) {
                        if (d.specialization != null && !d.specialization.isBlank()) {
                                specs.add(d.specialization.trim());
                        }
                }
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                model.addElement(ALL_SPEC);
                for (String s : specs)
                        model.addElement(s);
                specBox.setModel(model);
                if (previous != null && model.getIndexOf(previous) >= 0) {
                        specBox.setSelectedItem(previous);
                }

                loading = false;

                int active = 0;
                for (DoctorData d : all)
                        if (d.isActive())
                                active++;
                totalNum.setText(String.valueOf(all.size()));
                activeNum.setText(String.valueOf(active));
                inactiveNum.setText(String.valueOf(all.size() - active));

                applyFilter();
        }

        private void applyFilter() {
                updateStatSelection();
                String q = searchField.getText().trim().toLowerCase();
                String spec = (String) specBox.getSelectedItem();
                String st = (String) statusBox.getSelectedItem();

                cardsContainer.removeAll();
                int shown = 0;

                for (DoctorData d : all) {
                        if (spec != null && !ALL_SPEC.equals(spec) && !spec.equalsIgnoreCase(nz(d.specialization).trim()))
                                continue;
                        if (st != null && !ALL_STATUS.equals(st) && !st.equalsIgnoreCase(nz(d.status)))
                                continue;
                        if (!q.isEmpty()) {
                                String hay = (nz(d.name) + " " + nz(d.specialization) + " " + nz(d.phone) + " "
                                                + nz(d.email) + " " + nz(d.days)).toLowerCase();
                                if (!hay.contains(q))
                                        continue;
                        }
                        cardsContainer.add(new DoctorCard(d));
                        shown++;
                }

                if (shown == 0) {
                        String msg = all.isEmpty()
                                        ? "No doctors found. Add doctors from the Doctors page."
                                        : "No doctors match your search or filters.";
                        JLabel empty = new JLabel(msg);
                        empty.setFont(new Font(FONT, Font.PLAIN, 13));
                        empty.setForeground(MUTED);
                        empty.setBorder(new EmptyBorder(30, 20, 0, 0));
                        cardsContainer.add(empty);
                }

                resultLabel.setText("Showing " + shown + " of " + all.size());
                cardsContainer.revalidate();
                cardsContainer.repaint();
        }

        // =====================================================
        // ACTIONS
        // =====================================================

        private void toggleStatus(DoctorData d) {
                setStatus(d, d.isActive() ? "Inactive" : "Active");
        }

        private void setStatus(DoctorData d, String newStatus) {
                try (Connection con = DB.getConnection();
                                PreparedStatement ps = con.prepareStatement(
                                                "UPDATE doctors SET status = ? WHERE id = ?")) {
                        ps.setString(1, newStatus);
                        ps.setInt(2, d.id);
                        ps.executeUpdate();
                } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                                        "Unable to update status:\n" + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                        return;
                }
                DoctorEvents.fireChanged();
        }

        private void deleteDoctor(DoctorData d) {
                int choice = JOptionPane.showConfirmDialog(this,
                                "Delete " + d.displayName() + " permanently?\nThis action cannot be undone.",
                                "Delete Doctor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION)
                        return;

                try (Connection con = DB.getConnection();
                                PreparedStatement ps = con.prepareStatement("DELETE FROM doctors WHERE id = ?")) {
                        ps.setInt(1, d.id);
                        ps.executeUpdate();

                } catch (Exception e) {
                        if (isForeignKeyError(e)) {
                                // doctor has appointments -> keep history, offer to deactivate instead
                                int c = JOptionPane.showConfirmDialog(this,
                                                d.displayName() + " has appointments linked to them, so deleting would "
                                                                + "break appointment history.\n\nMark this doctor as Inactive instead?",
                                                "Cannot Delete", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.INFORMATION_MESSAGE);
                                if (c == JOptionPane.YES_OPTION && d.isActive()) {
                                        setStatus(d, "Inactive");
                                }
                        } else {
                                JOptionPane.showMessageDialog(this,
                                                "Unable to delete doctor:\n" + e.getMessage(),
                                                "Database Error", JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                        }
                        return;
                }

                deleteImageFile(d.imagePath);
                DoctorEvents.fireChanged(); // every connected panel refreshes
        }

        private static boolean isForeignKeyError(Throwable t) {
                while (t != null) {
                        if (t instanceof SQLIntegrityConstraintViolationException)
                                return true;
                        if (t instanceof SQLException) {
                                String state = ((SQLException) t).getSQLState();
                                if (state != null && state.startsWith("23"))
                                        return true;
                        }
                        String m = t.getMessage();
                        if (m != null && m.toLowerCase().contains("foreign key"))
                                return true;
                        t = t.getCause();
                }
                return false;
        }

        private void deleteImageFile(String storedPath) {
                if (storedPath == null || storedPath.isBlank())
                        return;
                try {
                        File f = new File(storedPath);
                        if (!f.isAbsolute()) {
                                f = new File(System.getProperty("user.dir"), storedPath);
                        }
                        // only remove files that live in our own images folder
                        if (f.exists() && f.getPath().replace('\\', '/').contains("images/doctors/")) {
                                f.delete();
                        }
                } catch (Exception ignored) {
                }
        }

        private BufferedImage loadPhoto(String storedPath) {
                if (storedPath == null || storedPath.isBlank())
                        return null;
                try {
                        File f = new File(storedPath);
                        if (!f.isAbsolute()) {
                                f = new File(System.getProperty("user.dir"), storedPath);
                        }
                        if (!f.exists())
                                return null;
                        return ImageIO.read(f); // null for unsupported formats (e.g. webp) -> initials shown
                } catch (Exception e) {
                        return null;
                }
        }

        // =====================================================
        // REFRESH (call from the main frame when this tab opens)
        // =====================================================

        public void refresh() {
                loadDoctors();
        }
}