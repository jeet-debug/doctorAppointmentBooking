package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class App extends JFrame {

    private JPanel contentPanel;
        private JPanel cardsPanel;
        private JPanel actionPanel;
        private JPanel sidebar;
        private boolean fullScreen;

    private final Color BLUE = new Color(31, 122, 224);
    private final Color DARK_BLUE = new Color(20, 91, 174);
    private final Color SIDEBAR = new Color(20, 31, 48);
    private final Color BG = new Color(245, 248, 252);

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
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Logo
        JLabel logo = new JLabel("MediBook");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(Color.WHITE);
        logo.setBorder(new EmptyBorder(30, 25, 25, 10));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);

        JLabel subtitle = new JLabel("Doctor Appointment System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(170, 185, 205));
        subtitle.setBorder(new EmptyBorder(0, 25, 30, 10));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(subtitle);

        // Menu buttons
        addMenuButton(sidebar, "Dashboard", true);
        addMenuButton(sidebar, "Book Appointment", false);
        addMenuButton(sidebar, "Appointments", false);
        addMenuButton(sidebar, "Doctors", false);
        addMenuButton(sidebar, "Patients", false);

        sidebar.add(Box.createVerticalGlue());

        JButton logoutButton = new JButton("Logout");
        logoutButton.setMaximumSize(new Dimension(210, 45));
        logoutButton.setPreferredSize(new Dimension(210, 45));

        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(190, 55, 55));
        logoutButton.setBorderPainted(false);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoutButton.addActionListener(e -> logout());

        sidebar.add(logoutButton);
        sidebar.add(Box.createVerticalStrut(30));

        // =====================================================
        // RIGHT SIDE
        // =====================================================

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BG);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 76));
        header.setBackground(Color.WHITE);
        header.setBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0,
                        new Color(225, 230, 237)
                )
        );

        JLabel welcome = new JLabel(
                "  Welcome to MediBook"
        );

        welcome.setFont(
                new Font("Segoe UI", Font.BOLD, 22)
        );

        welcome.setForeground(
                new Color(35, 45, 60)
        );

        header.add(
                welcome,
                BorderLayout.WEST
        );

        JLabel admin = new JLabel(
                "Admin   "
        );

        admin.setFont(
                new Font("Segoe UI", Font.BOLD, 14)
        );

        admin.setForeground(BLUE);

        header.add(
                admin,
                BorderLayout.EAST
        );

        rightPanel.add(
                header,
                BorderLayout.NORTH
        );

        // =====================================================
        // DASHBOARD CONTENT
        // =====================================================

        contentPanel = new JPanel();
        contentPanel.setBackground(BG);
        contentPanel.setLayout(
                new BoxLayout(
                        contentPanel,
                        BoxLayout.Y_AXIS
                )
        );

        contentPanel.setBorder(
                new EmptyBorder(30, 35, 30, 35)
        );

        // Heading
        JLabel dashboardTitle = new JLabel(
                "Dashboard"
        );

        dashboardTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 30)
        );

        dashboardTitle.setForeground(
                new Color(35, 45, 60)
        );

        dashboardTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        contentPanel.add(dashboardTitle);

        JLabel dashboardSubtitle = new JLabel(
                "Manage your healthcare appointments and patients"
        );

        dashboardSubtitle.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        dashboardSubtitle.setForeground(
                new Color(110, 120, 135)
        );

        dashboardSubtitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        contentPanel.add(
                Box.createVerticalStrut(5)
        );

        contentPanel.add(
                dashboardSubtitle
        );

        // =====================================================
        // STAT CARDS
        // =====================================================

        cardsPanel = new JPanel(new GridLayout(0, 4, 18, 18));

        cardsPanel.setBackground(BG);
        cardsPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        cardsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        290
                )
        );

        cardsPanel.setBorder(
                new EmptyBorder(
                        25, 0, 25, 0
                )
        );

        cardsPanel.add(
                createStatCard(
                        "Total Appointments",
                        "24",
                        new Color(31, 122, 224)
                )
        );

        cardsPanel.add(
                createStatCard(
                        "Today's Appointments",
                        "08",
                        new Color(41, 160, 95)
                )
        );

        cardsPanel.add(
                createStatCard(
                        "Total Doctors",
                        "12",
                        new Color(150, 90, 210)
                )
        );

        cardsPanel.add(
                createStatCard(
                        "Total Patients",
                        "156",
                        new Color(230, 145, 45)
                )
        );

        contentPanel.add(cardsPanel);

        // =====================================================
        // QUICK ACTIONS
        // =====================================================

        JLabel quickTitle = new JLabel(
                "Quick Actions"
        );

        quickTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 20)
        );

        quickTitle.setForeground(
                new Color(35, 45, 60)
        );

        quickTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        contentPanel.add(quickTitle);

        actionPanel = new JPanel(new GridLayout(0, 3, 20, 20));

        actionPanel.setBackground(BG);
        actionPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        actionPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        330
                )
        );

        actionPanel.setBorder(
                new EmptyBorder(
                        18, 0, 25, 0
                )
        );

        actionPanel.add(
                createActionCard(
                        "Book Appointment",
                        "Schedule a new doctor appointment"
                )
        );

        actionPanel.add(
                createActionCard(
                        "View Appointments",
                        "Check and manage appointments"
                )
        );

        actionPanel.add(
                createActionCard(
                        "Manage Doctors",
                        "View available doctors"
                )
        );

        contentPanel.add(actionPanel);

        // =====================================================
        // RECENT ACTIVITY
        // =====================================================

        JLabel recentTitle = new JLabel(
                "Recent Activity"
        );

        recentTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 20)
        );

        recentTitle.setForeground(
                new Color(35, 45, 60)
        );

        recentTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        contentPanel.add(recentTitle);

        JPanel activity = new JPanel(
                new BorderLayout()
        );

        activity.setBackground(Color.WHITE);

        activity.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 237)
                        ),
                        new EmptyBorder(
                                20, 20, 20, 20
                        )
                )
        );

        activity.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        activity.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        120
                )
        );

        JLabel activityText = new JLabel(
                "<html>"
                        + "<b>Welcome to MediBook!</b><br><br>"
                        + "Your appointment management dashboard is ready."
                        + "<br>"
                        + "Use the menu to manage doctors, patients and appointments."
                        + "</html>"
        );

        activityText.setFont(
                new Font("Arial", Font.PLAIN, 13)
        );

        activity.add(
                activityText,
                BorderLayout.CENTER
        );

        contentPanel.add(
                Box.createVerticalStrut(15)
        );

        contentPanel.add(activity);

        // Scroll
        JScrollPane scrollPane =
                new JScrollPane(contentPanel);

        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);

        rightPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        mainPanel.add(
                sidebar,
                BorderLayout.WEST
        );

        mainPanel.add(
                rightPanel,
                BorderLayout.CENTER
        );

                mainPanel.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized(ComponentEvent e) {
                                boolean compact = mainPanel.getWidth() < 1000;
                                sidebar.setPreferredSize(new Dimension(compact ? 190 : 230, 0));
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

                int availableWidth = contentPanel.getWidth() - 70;
                int statColumns = Math.max(1, Math.min(4, (availableWidth + 18) / 228));
                int actionColumns = Math.max(1, Math.min(3, (availableWidth + 20) / 280));

                ((GridLayout) cardsPanel.getLayout()).setColumns(statColumns);
                ((GridLayout) actionPanel.getLayout()).setColumns(actionColumns);
                cardsPanel.revalidate();
                actionPanel.revalidate();
        }

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
    // SIDEBAR BUTTON
    // =========================================================

    private void addMenuButton(
            JPanel sidebar,
            String text,
            boolean active
    ) {

        JButton button = new JButton(
                "   " + text
        );

        button.setMaximumSize(
                new Dimension(220, 48)
        );

        button.setPreferredSize(
                new Dimension(220, 48)
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );

        button.setForeground(Color.WHITE);

        button.setBackground(
                active
                        ? BLUE
                        : SIDEBAR
        );

        button.setBorderPainted(false);
        button.setFocusPainted(false);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        button.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));

        button.addActionListener(e -> {

            if (text.equals("Logout")) {
                logout();
            }

            // Pages baad me connect karenge
            else if (!text.equals("Dashboard")) {

                JOptionPane.showMessageDialog(
                        this,
                        text + " page will be connected next.",
                        "MediBook",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }

    // =========================================================
    // STAT CARD
    // =========================================================

    private JPanel createStatCard(
            String title,
            String value,
            Color color
    ) {

        JPanel card = new JPanel();
        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 237)
                        ),
                        new EmptyBorder(
                                15, 18, 15, 18
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        titleLabel.setForeground(
                new Color(110, 120, 135)
        );

        JLabel valueLabel =
                new JLabel(value);

        valueLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        30
                )
        );

        valueLabel.setForeground(color);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);

        return card;
    }

    // =========================================================
    // ACTION CARD
    // =========================================================

    private JPanel createActionCard(
            String title,
            String description
    ) {

        JPanel card = new JPanel();

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 230, 237)
                        ),
                        new EmptyBorder(
                                20, 20, 20, 20
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        16
                )
        );

        titleLabel.setForeground(
                new Color(35, 45, 60)
        );

        JLabel descriptionLabel =
                new JLabel(
                        "<html><div style='width:180px;'>"
                                + description
                                + "</div></html>"
                );

        descriptionLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        descriptionLabel.setForeground(
                new Color(110, 120, 135)
        );

        JButton button =
                new JButton("Open");

        button.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        button.setForeground(Color.WHITE);
        button.setBackground(BLUE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(descriptionLabel);
        card.add(Box.createVerticalGlue());
        card.add(button);

        return card;
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {

            dispose();

            SwingUtilities.invokeLater(() -> {
                new login.LoginFrame().setVisible(true);
            });
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
                                .getSystemLookAndFeelClassName()
                );

            } catch (Exception ignored) {
            }

            new App().setVisible(true);
        });
    }
}