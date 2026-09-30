package app;

import appointment.AppointmentsPanel;
import appointment.BookAppointmentPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * MediBook dashboard (redesigned).
 * Same class name / package / behaviour as before:
 * - opened from LoginFrame with: new App().setVisible(true)
 * - Logout goes back to login.LoginFrame
 * - F11 = full screen, ESC = leave full screen
 * Sidebar / quick action buttons now open BookAppointmentPanel and
 * AppointmentsPanel.
 */
public class App extends JFrame {

    // ---------- fonts / colours (same family as the login screen) ----------
    private static final String FONT = "Segoe UI";

    private static final Color BLUE = new Color(31, 122, 224);
    private static final Color DARK_BLUE = new Color(20, 91, 174);
    private static final Color SIDEBAR = new Color(20, 31, 48);
    private static final Color BG = new Color(245, 248, 252);
    private static final Color TEXT = new Color(35, 45, 60);
    private static final Color MUTED = new Color(110, 120, 135);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color RED = new Color(200, 60, 60);
    private static final Color RED_HOVER = new Color(222, 76, 76);

    /** Space around every card that is used to draw its soft shadow. */
    private static final int SH = 8;

    private JPanel contentPanel;
    private JPanel cardsPanel;
    private JPanel actionPanel;
    private JPanel sidebar;
    private boolean fullScreen;

    // page switching (Dashboard <-> Appointment panel)
    private CardLayout pageLayout;
    private JPanel pages;
    private BookAppointmentPanel bookPanel;
    private AppointmentsPanel appointmentsPanel;

    public App() {
        setTitle("MediBook - Dashboard");
        setSize(1100, 700);
        setMinimumSize(new Dimension(760, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        createDashboard();
        installFullScreenShortcuts();
    }

    private void createDashboard() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG);

        // =====================================================
        // SIDEBAR
        // =====================================================
        sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(28, 16, 28, 16));

        // brand: logo tile + name + 2-line subtitle (no more "MediB..." cut)
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        brand.setBorder(new EmptyBorder(0, 6, 0, 0));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("MediBook");
        logo.setFont(new Font(FONT, Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html>Doctor Appointment<br>System</html>");
        subtitle.setFont(new Font(FONT, Font.PLAIN, 11));
        subtitle.setForeground(new Color(170, 185, 205));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandText.add(logo);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(subtitle);

        brand.add(new LogoMark(42));
        brand.add(Box.createHorizontalStrut(12));
        brand.add(brandText);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(34));

        // menu
        addMenuButton(sidebar, "Dashboard", true);
        addMenuButton(sidebar, "Book Appointment", false);
        addMenuButton(sidebar, "Appointments", false);
        addMenuButton(sidebar, "Doctors", false);
        addMenuButton(sidebar, "Patients", false);

        sidebar.add(Box.createVerticalGlue());

        RoundedButton logoutButton = new RoundedButton("Logout", RED, RED_HOVER, 22);
        logoutButton.setFont(new Font(FONT, Font.BOLD, 14));
        logoutButton.setHorizontalAlignment(SwingConstants.CENTER);
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.setPreferredSize(new Dimension(100, 46));
        logoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        // =====================================================
        // RIGHT SIDE : HEADER
        // =====================================================
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 76));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(0, 36, 0, 28)));

        JLabel welcome = new JLabel("Welcome to MediBook");
        welcome.setFont(new Font(FONT, Font.BOLD, 22));
        welcome.setForeground(TEXT);
        header.add(welcome, BorderLayout.WEST);

        JPanel adminWrap = new JPanel(new GridBagLayout());
        adminWrap.setOpaque(false);
        adminWrap.add(createAdminChip());
        header.add(adminWrap, BorderLayout.EAST);

        rightPanel.add(header, BorderLayout.NORTH);

        // =====================================================
        // DASHBOARD CONTENT
        // =====================================================
        contentPanel = new ScrollPanel();
        contentPanel.setBackground(BG);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(28, 28, 28, 28));

        contentPanel.add(heading("Dashboard", 30, TEXT));
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(heading("Manage your healthcare appointments and patients", 14, MUTED));
        contentPanel.add(Box.createVerticalStrut(22));

        // ---- stat cards ----
        cardsPanel = new FlowGrid(4, 4);
        cardsPanel.add(createStatCard("Total Appointments", "24", new Color(31, 122, 224)));
        cardsPanel.add(createStatCard("Today's Appointments", "08", new Color(41, 160, 95)));
        cardsPanel.add(createStatCard("Total Doctors", "12", new Color(150, 90, 210)));
        cardsPanel.add(createStatCard("Total Patients", "156", new Color(230, 145, 45)));
        contentPanel.add(cardsPanel);

        // ---- quick actions ----
        contentPanel.add(Box.createVerticalStrut(26));
        contentPanel.add(heading("Quick Actions", 20, TEXT));
        contentPanel.add(Box.createVerticalStrut(10));

        actionPanel = new FlowGrid(3, 4);
        actionPanel.add(createActionCard("Book Appointment", "Schedule a new doctor appointment"));
        actionPanel.add(createActionCard("View Appointments", "Check and manage appointments"));
        actionPanel.add(createActionCard("Manage Doctors", "View available doctors"));
        contentPanel.add(actionPanel);

        // ---- recent activity ----
        contentPanel.add(Box.createVerticalStrut(26));
        contentPanel.add(heading("Recent Activity", 20, TEXT));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createActivityCard());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG);

        // ---- page holder: dashboard + appointment panel ----
        pageLayout = new CardLayout();
        pages = new JPanel(pageLayout);
        pages.setOpaque(false);
        bookPanel = new BookAppointmentPanel(this::showAppointmentsPage);
        appointmentsPanel = new AppointmentsPanel(() -> bookPanel.refresh());
        pages.add(scrollPane, "dash");
        pages.add(bookPanel, "book");
        pages.add(appointmentsPanel, "appts");

        rightPanel.add(pages, BorderLayout.CENTER);

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean compact = mainPanel.getWidth() < 1000;
                sidebar.setPreferredSize(new Dimension(compact ? 220 : 260, 0));
                updateResponsiveColumns();
                mainPanel.revalidate();
            }
        });

        contentPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveColumns();
            }
        });

        add(mainPanel);
    }

    private void updateResponsiveColumns() {
        if (contentPanel == null || cardsPanel == null || actionPanel == null) {
            return;
        }

        int availableWidth = contentPanel.getWidth() - 56;
        int statColumns = Math.max(1, Math.min(4, (availableWidth + 4) / 236));
        int actionColumns = Math.max(1, Math.min(3, (availableWidth + 4) / 290));

        ((GridLayout) cardsPanel.getLayout()).setColumns(statColumns);
        ((GridLayout) actionPanel.getLayout()).setColumns(actionColumns);
        cardsPanel.revalidate();
        actionPanel.revalidate();
    }

    // =========================================================
    // FULL SCREEN (unchanged behaviour)
    // =========================================================

    private void installFullScreenShortcuts() {
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleFullScreen");
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "exitFullScreen");
        rootPane.getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullScreen();
            }
        });
        rootPane.getActionMap().put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (fullScreen) {
                    toggleFullScreen();
                }
            }
        });
    }

    private void toggleFullScreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        if (!fullScreen) {
            dispose();
            setUndecorated(true);
            setVisible(true);
            device.setFullScreenWindow(this);
        } else {
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setVisible(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        fullScreen = !fullScreen;
    }

    // =========================================================
    // SMALL BUILDERS
    // =========================================================

    /** Left-aligned text line (indented by SH so it lines up with the cards). */
    private static JLabel heading(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, size >= 20 ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, SH, 0, 0));
        return l;
    }

    /** "A Admin" pill in the header. */
    private JComponent createAdminChip() {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(232, 241, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(0, 4, 0, 10));

        JLabel name = new JLabel("Admin");
        name.setFont(new Font(FONT, Font.BOLD, 14));
        name.setForeground(BLUE);

        chip.add(new Avatar("A", 30));
        chip.add(name);
        return chip;
    }

    // =========================================================
    // SIDEBAR BUTTON
    // =========================================================

    private void addMenuButton(JPanel sidebar, String text, boolean active) {

        RoundedButton button = new RoundedButton(
                text,
                active ? BLUE : new Color(0, 0, 0, 0),
                active ? BLUE : new Color(255, 255, 255, 26),
                22);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(0, 20, 0, 12));
        button.setFont(new Font(FONT, active ? Font.BOLD : Font.PLAIN, 14));
        button.setForeground(active ? Color.WHITE : new Color(214, 224, 238));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(100, 46));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(6));

        button.addActionListener(e -> {
            switch (text) {
                case "Dashboard":
                    showDashboardPage();
                    break;
                case "Book Appointment":
                    showBookPage();
                    break;
                case "Appointments":
                    showAppointmentsPage();
                    break;
                default:
                    // Doctors / Patients pages baad me connect karenge
                    showComingSoon(text);
            }
        });
    }

    private void showDashboardPage() {
        pageLayout.show(pages, "dash");
    }

    private void showBookPage() {
        bookPanel.refresh();
        pageLayout.show(pages, "book");
    }

    private void showAppointmentsPage() {
        appointmentsPanel.refresh();
        pageLayout.show(pages, "appts");
    }

    private void showComingSoon(String name) {
        JOptionPane.showMessageDialog(
                this,
                name + " page will be connected next.",
                "MediBook",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    // STAT CARD
    // =========================================================

    private JPanel createStatCard(String title, String value, Color color) {

        RoundedPanel card = new RoundedPanel(26, color, 20, 24, 20, 18);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT, Font.PLAIN, 13));
        titleLabel.setForeground(MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(FONT, Font.BOLD, 36));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(valueLabel);

        return card;
    }

    // =========================================================
    // ACTION CARD
    // =========================================================

    private JPanel createActionCard(String title, String description) {

        RoundedPanel card = new RoundedPanel(26, null, 22, 24, 22, 24);
        card.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT, Font.BOLD, 17));
        titleLabel.setForeground(TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html><div style='width:200px;'>" + description + "</div></html>");
        descriptionLabel.setFont(new Font(FONT, Font.PLAIN, 13));
        descriptionLabel.setForeground(MUTED);
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(titleLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(descriptionLabel);

        RoundedButton button = new RoundedButton("Open", BLUE, DARK_BLUE, 38);
        button.setFont(new Font(FONT, Font.BOLD, 13));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(100, 38));
        button.addActionListener(e -> {
            if (title.equals("Book Appointment")) {
                showBookPage();
            } else if (title.equals("View Appointments")) {
                showAppointmentsPage();
            } else {
                showComingSoon(title);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(20, 0, 0, 0));
        bottom.add(button);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================
    // RECENT ACTIVITY
    // =========================================================

    private JPanel createActivityCard() {

        RoundedPanel card = new RoundedPanel(26, null, 22, 26, 22, 26);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome to MediBook!");
        title.setFont(new Font(FONT, Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(activityRow("Your appointment management dashboard is ready.", BLUE));
        card.add(Box.createVerticalStrut(8));
        card.add(activityRow("Use the menu to manage doctors, patients and appointments.",
                new Color(41, 160, 95)));

        return card;
    }

    private JPanel activityRow(String text, Color dotColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.add(new Dot(dotColor));

        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.PLAIN, 14));
        label.setForeground(new Color(70, 82, 99));

        row.add(dotWrap, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    // =========================================================
    // LOGOUT (unchanged)
    // =========================================================

    private void logout() {

        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {

            dispose();

            SwingUtilities.invokeLater(() -> {
                new login.LoginFrame().setVisible(true);
            });
        }
    }

    // =========================================================
    // CUSTOM COMPONENTS (all inside this file - nothing else to add)
    // =========================================================

    /** Button with rounded corners, hover and pressed colours. */
    private static class RoundedButton extends JButton {
        private final Color normal;
        private final Color hoverColor;
        private final int radius;
        private boolean hover;

        RoundedButton(String text, Color normal, Color hoverColor, int radius) {
            super(text);
            this.normal = normal;
            this.hoverColor = hoverColor;
            this.radius = radius;
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
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
            if (getModel().isPressed()) {
                c = c.darker();
            }
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * White rounded card with soft shadow and optional coloured strip on the left.
     */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color accent;

        RoundedPanel(int radius, Color accent, int top, int left, int bottom, int right) {
            this.radius = radius;
            this.accent = accent;
            setOpaque(false);
            setBorder(new EmptyBorder(top + SH, left + SH, bottom + SH, right + SH));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 2 * SH;
            int h = getHeight() - 2 * SH;

            // soft shadow
            for (int i = 1; i <= 6; i++) {
                g2.setColor(new Color(15, 23, 42, 4));
                g2.fillRoundRect(SH - i, SH - i + 2, w + 2 * i, h + 2 * i, radius + i, radius + i);
            }

            // card body
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(SH, SH, w, h, radius, radius);
            g2.setColor(LINE);
            g2.drawRoundRect(SH, SH, w - 1, h - 1, radius, radius);

            // coloured strip (stat cards)
            if (accent != null) {
                Shape oldClip = g2.getClip();
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(SH, SH, w, h, radius, radius));
                g2.setColor(accent);
                g2.fillRect(SH, SH, 6, h);
                g2.setClip(oldClip);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Grid of cards whose height never stretches beyond what the cards need. */
    private static class FlowGrid extends JPanel {
        FlowGrid(int columns, int gap) {
            super(new GridLayout(0, columns, gap, gap));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * Scroll content that always follows the window width (no sideways scrolling).
     */
    private static class ScrollPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(16, r.height - 40);
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

    /** White rounded tile with a blue medical cross. */
    private static class LogoMark extends JComponent {
        private final int size;

        LogoMark(int size) {
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            g2.setColor(BLUE);
            int bar = size / 5;
            int len = size * 5 / 9;
            int mid = (size - bar) / 2;
            int start = (size - len) / 2;
            g2.fillRoundRect(mid, start, bar, len, 4, 4);
            g2.fillRoundRect(start, mid, len, bar, 4, 4);
            g2.dispose();
        }
    }

    /** Round avatar with one letter. */
    private static class Avatar extends JComponent {
        private final String letter;
        private final int size;

        Avatar(String letter, int size) {
            this.letter = letter;
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(BLUE);
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(FONT, Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(letter, x, y);
            g2.dispose();
        }
    }

    /** Small coloured dot for the activity list. */
    private static class Dot extends JComponent {
        private final Color color;

        Dot(Color color) {
            this.color = color;
            Dimension d = new Dimension(10, 10);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, 10, 10);
            g2.dispose();
        }
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager
                                .getSystemLookAndFeelClassName());

            } catch (Exception ignored) {
            }

            new App().setVisible(true);
        });
    }
}