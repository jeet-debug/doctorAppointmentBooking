package app;

import appointment.AppointmentsPanel;
import appointment.BookAppointmentPanel;
import doctor.DoctorPanel;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * MediBook dashboard (professional redesign).
 * Same class / package / behaviour:
 * - opened from LoginFrame with: new App().setVisible(true)
 * - Logout goes back to login.LoginFrame
 * - F11 = full screen, ESC = leave full screen
 */
public class App extends JFrame {

    private static final String FONT = "Segoe UI";

    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color DARK_BLUE = new Color(29, 78, 216);
    private static final Color SIDEBAR_TOP = new Color(15, 23, 42);
    private static final Color SIDEBAR_BOTTOM = new Color(17, 31, 61);
    private static final Color BG = new Color(243, 246, 251);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color RED = new Color(220, 70, 70);
    private static final Color RED_HOVER = new Color(239, 90, 90);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color ORANGE = new Color(234, 138, 20);

    /** Space around every card used to draw its soft shadow. */
    private static final int SH = 8;

    private JPanel contentPanel;
    private JPanel cardsPanel;
    private JPanel actionPanel;
    private JPanel sidebar;
    private boolean fullScreen;

    private CardLayout pageLayout;
    private JPanel pages;
    private BookAppointmentPanel bookPanel;
    private AppointmentsPanel appointmentsPanel;
    private DoctorPanel doctorPanel;

    private JLabel headerTitle;
    private JLabel headerSub;
    private final java.util.List<SideButton> menuButtons = new ArrayList<>();

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
        sidebar = new GradientPanel(SIDEBAR_TOP, SIDEBAR_BOTTOM);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(26, 14, 20, 14));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        brand.setBorder(new EmptyBorder(0, 6, 0, 0));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("MediBook");
        logo.setFont(new Font(FONT, Font.BOLD, 20));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Doctor Appointment System");
        subtitle.setFont(new Font(FONT, Font.PLAIN, 10));
        subtitle.setForeground(new Color(148, 163, 184));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandText.add(logo);
        brandText.add(subtitle);

        brand.add(new LogoMark(40));
        brand.add(Box.createHorizontalStrut(12));
        brand.add(brandText);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(28));

        sidebar.add(sectionLabel("MAIN MENU"));
        addMenuButton(sidebar, "Dashboard", "home", true);
        addMenuButton(sidebar, "Book Appointment", "calendar-plus", false);
        addMenuButton(sidebar, "Appointments", "calendar", false);

        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(sectionLabel("MANAGEMENT"));
        addMenuButton(sidebar, "Doctors", "doctor", false);
        addMenuButton(sidebar, "All Doctors", "doctors-grid", false);
        addMenuButton(sidebar, "Patients", "users", false);
        addMenuButton(sidebar, "Settings", "settings", false);

        sidebar.add(Box.createVerticalGlue());

        // user card
        RoundedPanel userCard = new RoundedPanel(16, null, 0, 0, 0, 0, new Color(255, 255, 255, 14), false);
        userCard.setLayout(new BorderLayout(10, 0));
        userCard.setBorder(new EmptyBorder(SH + 8, SH + 10, SH + 8, SH + 10));
        JPanel uText = new JPanel();
        uText.setOpaque(false);
        uText.setLayout(new BoxLayout(uText, BoxLayout.Y_AXIS));
        JLabel uName = new JLabel("Admin");
        uName.setFont(new Font(FONT, Font.BOLD, 13));
        uName.setForeground(Color.WHITE);
        JLabel uRole = new JLabel("Administrator");
        uRole.setFont(new Font(FONT, Font.PLAIN, 11));
        uRole.setForeground(new Color(148, 163, 184));
        uText.add(uName);
        uText.add(uRole);
        JPanel av = new JPanel(new GridBagLayout());
        av.setOpaque(false);
        av.add(new Avatar("A", 34));
        userCard.add(av, BorderLayout.WEST);
        userCard.add(uText, BorderLayout.CENTER);
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(userCard);
        sidebar.add(Box.createVerticalStrut(10));

        RoundedButton logoutButton = new RoundedButton("Logout", RED, RED_HOVER, 16);
        logoutButton.setFont(new Font(FONT, Font.BOLD, 13));
        logoutButton.setHorizontalAlignment(SwingConstants.CENTER);
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.setPreferredSize(new Dimension(100, 42));
        logoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        // =====================================================
        // HEADER
        // =====================================================
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 76));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(0, 32, 0, 24)));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        headerTitle = new JLabel("Dashboard");
        headerTitle.setFont(new Font(FONT, Font.BOLD, 20));
        headerTitle.setForeground(TEXT);
        headerTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerSub = new JLabel("Welcome to MediBook");
        headerSub.setFont(new Font(FONT, Font.PLAIN, 12));
        headerSub.setForeground(MUTED);
        headerSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(headerTitle);
        titleBox.add(headerSub);

        JPanel titleWrap = new JPanel(new GridBagLayout());
        titleWrap.setOpaque(false);
        titleWrap.add(titleBox);
        header.add(titleWrap, BorderLayout.WEST);

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
        contentPanel.setBorder(new EmptyBorder(22, 24, 24, 24));

        contentPanel.add(createBanner());
        contentPanel.add(Box.createVerticalStrut(18));

        contentPanel.add(heading("Overview", 18, TEXT));
        contentPanel.add(Box.createVerticalStrut(6));

        cardsPanel = new FlowGrid(4, 4);
        cardsPanel.add(createStatCard("Total Appointments", "24", BLUE, "calendar"));
        cardsPanel.add(createStatCard("Today's Appointments", "08", GREEN, "clock"));
        cardsPanel.add(createStatCard("Total Doctors", "12", PURPLE, "doctor"));
        cardsPanel.add(createStatCard("Total Patients", "156", ORANGE, "users"));
        contentPanel.add(cardsPanel);

        contentPanel.add(Box.createVerticalStrut(22));
        contentPanel.add(heading("Quick Actions", 18, TEXT));
        contentPanel.add(Box.createVerticalStrut(6));

        actionPanel = new FlowGrid(3, 4);
        actionPanel.add(createActionCard("Book Appointment", "Schedule a new doctor appointment", "calendar-plus", BLUE));
        actionPanel.add(createActionCard("View Appointments", "Check and manage appointments", "calendar", GREEN));
        actionPanel.add(createActionCard("Manage Doctors", "View available doctors", "doctor", PURPLE));
        contentPanel.add(actionPanel);

        contentPanel.add(Box.createVerticalStrut(22));
        contentPanel.add(heading("Recent Activity", 18, TEXT));
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(createActivityCard());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG);

        // ---- page holder ----
        pageLayout = new CardLayout();
        pages = new JPanel(pageLayout);
        pages.setOpaque(false);
        bookPanel = new BookAppointmentPanel(this::showAppointmentsPage);
        appointmentsPanel = new AppointmentsPanel(() -> bookPanel.refresh());
        doctorPanel = new DoctorPanel();
        pages.add(scrollPane, "dash");
        pages.add(bookPanel, "book");
        pages.add(appointmentsPanel, "appts");
        pages.add(doctorPanel, "doctors");

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

        int availableWidth = contentPanel.getWidth() - 48;
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

    private static JLabel heading(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, size >= 18 ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, SH, 0, 0));
        return l;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.BOLD, 10));
        l.setForeground(new Color(100, 116, 139));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 8, 8, 0));
        return l;
    }

    /** "Admin" pill in the header. */
    private JComponent createAdminChip() {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 242, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(0, 4, 0, 10));

        JLabel name = new JLabel("Admin");
        name.setFont(new Font(FONT, Font.BOLD, 13));
        name.setForeground(BLUE);

        chip.add(new Avatar("A", 30));
        chip.add(name);
        return chip;
    }

    /** Gradient welcome banner. */
    private JPanel createBanner() {
        int h = 150;
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 2 * SH;
                int hh = getHeight() - 2 * SH;
                for (int i = 1; i <= 6; i++) {
                    g2.setColor(new Color(37, 99, 235, 6));
                    g2.fillRoundRect(SH - i, SH - i + 4, w + 2 * i, hh + 2 * i, 28 + i, 28 + i);
                }
                Shape clip = new RoundRectangle2D.Float(SH, SH, w, hh, 26, 26);
                g2.setPaint(new GradientPaint(SH, SH, new Color(37, 99, 235),
                        SH + w, SH + hh, new Color(79, 70, 229)));
                g2.fill(clip);
                g2.clip(clip);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(SH + w - 230, SH - 70, 260, 260);
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillOval(SH + w - 360, SH + hh - 80, 200, 200);
                g2.dispose();
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        banner.setOpaque(false);
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(new EmptyBorder(SH + 26, SH + 30, SH + 22, SH + 30));
        banner.setPreferredSize(new Dimension(100, h + 2 * SH));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        int hour = LocalTime.now().getHour();
        String greet = hour < 12 ? "Good Morning" : hour < 17 ? "Good Afternoon" : "Good Evening";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));

        JLabel dateLabel = new JLabel(date.toUpperCase());
        dateLabel.setFont(new Font(FONT, Font.BOLD, 11));
        dateLabel.setForeground(new Color(255, 255, 255, 190));
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(greet + ", Admin");
        title.setFont(new Font(FONT, Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Manage your healthcare appointments and patients");
        sub.setFont(new Font(FONT, Font.PLAIN, 13));
        sub.setForeground(new Color(255, 255, 255, 215));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton book = new RoundedButton("Book Appointment", Color.WHITE, new Color(232, 240, 255), 36);
        book.setForeground(BLUE);
        book.setFont(new Font(FONT, Font.BOLD, 13));
        book.setHorizontalAlignment(SwingConstants.CENTER);
        book.setPreferredSize(new Dimension(170, 36));
        book.setMaximumSize(new Dimension(170, 36));
        book.setAlignmentX(Component.LEFT_ALIGNMENT);
        book.addActionListener(e -> showBookPage());

        banner.add(dateLabel);
        banner.add(Box.createVerticalStrut(4));
        banner.add(title);
        banner.add(Box.createVerticalStrut(4));
        banner.add(sub);
        banner.add(Box.createVerticalGlue());
        banner.add(book);
        return banner;
    }

    // =========================================================
    // SIDEBAR BUTTON & PAGE SWITCHING
    // =========================================================

    private void addMenuButton(JPanel sidebar, String text, String iconType, boolean active) {

        SideButton button = new SideButton(text, iconType, active);
        menuButtons.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));

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
                case "Doctors":
                case "All Doctors":
                    showDoctorPage();
                    break;
                case "Patients":
                case "Settings":
                    showComingSoon(text);
                    break;
                default:
                    showComingSoon(text);
            }
        });
    }

    private void markActive(String menuName, String title, String sub) {
        for (SideButton b : menuButtons) {
            b.setActive(b.getMenuName().equals(menuName));
        }
        headerTitle.setText(title);
        headerSub.setText(sub);
    }

    private void showDashboardPage() {
        markActive("Dashboard", "Dashboard", "Welcome to MediBook");
        pageLayout.show(pages, "dash");
    }

    private void showBookPage() {
        markActive("Book Appointment", "Book Appointment", "Schedule a new doctor appointment");
        bookPanel.refresh();
        pageLayout.show(pages, "book");
    }

    private void showAppointmentsPage() {
        markActive("Appointments", "Appointments", "Check and manage appointments");
        appointmentsPanel.refresh();
        pageLayout.show(pages, "appts");
    }

    private void showDoctorPage() {
        markActive("Doctors", "Doctors", "View available doctors");
        pageLayout.show(pages, "doctors");
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

    private JPanel createStatCard(String title, String value, Color color, String icon) {

        RoundedPanel card = new RoundedPanel(20, null, 18, 18, 18, 18, Color.WHITE, true);
        card.setLayout(new BorderLayout(14, 0));

        JPanel bubbleWrap = new JPanel(new GridBagLayout());
        bubbleWrap.setOpaque(false);
        bubbleWrap.add(new IconBubble(icon, color, 50));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        titleLabel.setForeground(MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(FONT, Font.BOLD, 30));
        valueLabel.setForeground(TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(valueLabel);

        card.add(bubbleWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // =========================================================
    // ACTION CARD
    // =========================================================

    private JPanel createActionCard(String title, String description, String icon, Color color) {

        RoundedPanel card = new RoundedPanel(20, null, 20, 22, 18, 22, Color.WHITE, true);
        card.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        IconBubble bubble = new IconBubble(icon, color, 44);
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT, Font.BOLD, 15));
        titleLabel.setForeground(TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html><div style='width:190px;'>" + description + "</div></html>");
        descriptionLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        descriptionLabel.setForeground(MUTED);
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(bubble);
        top.add(Box.createVerticalStrut(12));
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(descriptionLabel);

        RoundedButton button = new RoundedButton("Open", BLUE, DARK_BLUE, 34);
        button.setFont(new Font(FONT, Font.BOLD, 12));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(90, 34));
        button.addActionListener(e -> {
            if (title.equals("Book Appointment")) {
                showBookPage();
            } else if (title.equals("View Appointments")) {
                showAppointmentsPage();
            } else if (title.equals("Manage Doctors")) {
                showDoctorPage();
            } else {
                showComingSoon(title);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottom.add(button);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================
    // RECENT ACTIVITY
    // =========================================================

    private JPanel createActivityCard() {

        RoundedPanel card = new RoundedPanel(20, null, 20, 24, 20, 24, Color.WHITE, true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome to MediBook!");
        title.setFont(new Font(FONT, Font.BOLD, 15));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(activityRow("Your appointment management dashboard is ready.", BLUE));
        card.add(Box.createVerticalStrut(8));
        card.add(activityRow("Use the sidebar menu to quickly access doctor listings, patient records and settings.",
                GREEN));

        return card;
    }

    private JPanel activityRow(String text, Color dotColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.add(new Dot(dotColor));

        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.PLAIN, 13));
        label.setForeground(new Color(71, 85, 105));

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
    // ICON PAINTER
    // =========================================================

    private static void drawIcon(Graphics2D g2, String type, int x, int y, int s, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1.5f, s / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int a = s / 10, b = s - a; // padded bounds
        switch (type) {
            case "home":
                g.drawPolyline(new int[]{a, s / 2, b}, new int[]{s * 5 / 11, a, s * 5 / 11}, 3);
                g.drawPolyline(new int[]{s / 5, s / 5, s * 4 / 5, s * 4 / 5},
                        new int[]{s * 4 / 9, b, b, s * 4 / 9}, 4);
                g.drawLine(s * 2 / 5, b, s * 2 / 5, s * 5 / 9 + 1);
                g.drawLine(s * 3 / 5, b, s * 3 / 5, s * 5 / 9 + 1);
                g.drawLine(s * 2 / 5, s * 5 / 9 + 1, s * 3 / 5, s * 5 / 9 + 1);
                break;
            case "calendar-plus":
            case "calendar":
                g.drawRoundRect(a, s / 6, b - a, b - s / 6, 5, 5);
                g.drawLine(a, s * 2 / 5, b, s * 2 / 5);
                g.drawLine(s * 3 / 10, a / 2, s * 3 / 10, s / 4);
                g.drawLine(s * 7 / 10, a / 2, s * 7 / 10, s / 4);
                if (type.equals("calendar-plus")) {
                    g.drawLine(s / 2, s * 11 / 20, s / 2, s * 17 / 20);
                    g.drawLine(s * 7 / 20, s * 7 / 10, s * 13 / 20, s * 7 / 10);
                } else {
                    g.fillOval(s * 3 / 10 - 1, s * 11 / 20, 3, 3);
                    g.fillOval(s / 2 - 1, s * 11 / 20, 3, 3);
                    g.fillOval(s * 7 / 10 - 1, s * 11 / 20, 3, 3);
                    g.fillOval(s * 3 / 10 - 1, s * 4 / 5 - 1, 3, 3);
                    g.fillOval(s / 2 - 1, s * 4 / 5 - 1, 3, 3);
                }
                break;
            case "clock":
                g.drawOval(a, a, b - a, b - a);
                g.drawLine(s / 2, s / 4, s / 2, s / 2);
                g.drawLine(s / 2, s / 2, s * 7 / 10, s * 3 / 5);
                break;
            case "doctor":
                g.drawOval(s * 3 / 10, a, s * 2 / 5, s * 2 / 5);
                g.drawArc(a, s * 11 / 20, b - a, s * 7 / 10, 0, 180);
                g.drawLine(s / 2, s * 3 / 5, s / 2, s * 4 / 5);
                g.drawLine(s * 2 / 5, s * 7 / 10, s * 3 / 5, s * 7 / 10);
                break;
            case "doctors-grid":
                g.drawRoundRect(a, a, s * 2 / 5 - a + 1, s * 2 / 5 - a + 1, 3, 3);
                g.drawRoundRect(s * 11 / 20, a, s * 2 / 5 - a + 1, s * 2 / 5 - a + 1, 3, 3);
                g.drawRoundRect(a, s * 11 / 20, s * 2 / 5 - a + 1, s * 2 / 5 - a + 1, 3, 3);
                g.drawRoundRect(s * 11 / 20, s * 11 / 20, s * 2 / 5 - a + 1, s * 2 / 5 - a + 1, 3, 3);
                break;
            case "users":
                g.drawOval(s / 5, a, s * 3 / 10, s * 3 / 10);
                g.drawArc(a, s / 2, s * 11 / 20, s * 3 / 5, 0, 180);
                g.drawOval(s * 11 / 20, s / 5, s / 4, s / 4);
                g.drawArc(s * 11 / 20, s * 11 / 20, s * 2 / 5, s / 2, 0, 180);
                break;
            case "settings":
                g.drawLine(a, s / 4, b, s / 4);
                g.drawLine(a, s / 2, b, s / 2);
                g.drawLine(a, s * 3 / 4, b, s * 3 / 4);
                g.setColor(color);
                g.fillOval(s * 3 / 5, s / 4 - 3, 6, 6);
                g.fillOval(s / 4, s / 2 - 3, 6, 6);
                g.fillOval(s * 11 / 20, s * 3 / 4 - 3, 6, 6);
                break;
            default:
                g.fillOval(a, a, b - a, b - a);
        }
        g.dispose();
    }

    // =========================================================
    // CUSTOM COMPONENTS
    // =========================================================

    /** Sidebar menu item with icon, hover and active state. */
    private static class SideButton extends JButton {
        private final String label;
        private final String icon;
        private boolean active;
        private boolean hover;

        SideButton(String text, String icon, boolean active) {
            super(text);
            this.label = text;
            this.icon = icon;
            this.active = active;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(new Font(FONT, Font.PLAIN, 13));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
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

        String getMenuName() {
            return label;
        }

        void setActive(boolean a) {
            this.active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, BLUE, w, 0, new Color(59, 130, 246)));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }
            Color fg = active ? Color.WHITE : hover ? Color.WHITE : new Color(160, 174, 196);
            drawIcon(g2, icon, 16, (h - 18) / 2, 18, fg);
            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 13f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, 46, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

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

    /** Rounded card with optional soft shadow. */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final boolean shadow;

        RoundedPanel(int radius, Color accent, int top, int left, int bottom, int right,
                     Color fill, boolean shadow) {
            this.radius = radius;
            this.fill = fill;
            this.shadow = shadow;
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

            if (shadow) {
                for (int i = 1; i <= 6; i++) {
                    g2.setColor(new Color(15, 23, 42, 4));
                    g2.fillRoundRect(SH - i, SH - i + 2, w + 2 * i, h + 2 * i, radius + i, radius + i);
                }
            }
            g2.setColor(fill);
            g2.fillRoundRect(SH, SH, w, h, radius, radius);
            if (shadow) {
                g2.setColor(LINE);
                g2.drawRoundRect(SH, SH, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel with a vertical gradient background. */
    private static class GradientPanel extends JPanel {
        private final Color top, bottom;

        GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Rounded tinted square holding an icon. */
    private static class IconBubble extends JComponent {
        private final String icon;
        private final Color color;
        private final int size;

        IconBubble(String icon, Color color, int size) {
            this.icon = icon;
            this.color = color;
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
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
            g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            int is = size * 5 / 12;
            drawIcon(g2, icon, (size - is) / 2, (size - is) / 2, is, color);
            g2.dispose();
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

    /** Scroll content that always follows the window width. */
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

    /** Blue gradient tile with a white medical cross. */
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
            g2.setPaint(new GradientPaint(0, 0, new Color(59, 130, 246), size, size, new Color(79, 70, 229)));
            g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            g2.setColor(Color.WHITE);
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
            g2.setPaint(new GradientPaint(0, 0, new Color(59, 130, 246), size, size, new Color(79, 70, 229)));
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(FONT, Font.BOLD, size * 5 / 11));
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
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            g2.fillOval(0, 0, 10, 10);
            g2.setColor(color);
            g2.fillOval(2, 2, 6, 6);
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
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new App().setVisible(true);
        });
    }
}