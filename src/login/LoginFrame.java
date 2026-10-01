package login;
import app.App;

import database.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.prefs.Preferences;

/**
 * MediBook login screen - full screen, responsive, modern design.
 *  - Wide window  : brand panel on the left + login card on the right
 *  - Narrow window: brand panel hides, compact logo shows above the card
 *  - F11 toggles true full screen, ESC leaves it
 */
public class LoginFrame extends JFrame {

    private static final Color PRIMARY = new Color(25, 118, 210);
    private static final Color PRIMARY_DARK = new Color(13, 71, 161);
    private static final Color TEXT = new Color(15, 23, 42);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color ERROR = new Color(220, 38, 38);
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final String FONT = "Segoe UI";
    private static final int BREAKPOINT = 900;

    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox rememberBox;
    private JLabel errorLabel;
    private GradientButton loginButton; 
    private JComponent compactLogo;
    private boolean fullScreen = false;
    private boolean busy = false;
    private int row = 0;

    public LoginFrame() {
        setTitle("MediBook - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(380, 660));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel brand = buildBrandPanel();
        JPanel form = buildFormPanel();

        JPanel root = new JPanel();
        root.add(brand);
        root.add(form);
        root.setLayout(new LayoutManager() {
            public void addLayoutComponent(String name, Component comp) { }
            public void removeLayoutComponent(Component comp) { }
            public Dimension preferredLayoutSize(Container parent) { return new Dimension(1000, 700); }
            public Dimension minimumLayoutSize(Container parent) { return new Dimension(360, 600); }
            public void layoutContainer(Container c) {
                int w = c.getWidth(), h = c.getHeight();
                int leftWidth = w >= BREAKPOINT ? (int) (w * 0.55) : 0;
                brand.setBounds(0, 0, leftWidth, h);
                form.setBounds(leftWidth, 0, w - leftWidth, h);
            }
        });
        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                compactLogo.setVisible(root.getWidth() < BREAKPOINT);
            }
        });
        setContentPane(root);
        compactLogo.setVisible(false);

        // keyboard shortcuts: F11 = full screen, ESC = leave full screen
        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fs");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        rp.getActionMap().put("fs", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleFullScreen(); }
        });
        rp.getActionMap().put("esc", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (fullScreen) toggleFullScreen(); }
        });

        // remembered username
        String saved = prefs.get("username", "");
        usernameField.setText(saved);
        rememberBox.setSelected(!saved.isEmpty());
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                (usernameField.getText().isEmpty() ? usernameField : passwordField).requestFocusInWindow();
            }
        });
    }

    // =====================================================
    // LEFT: BRAND PANEL
    // =====================================================
    private JPanel buildBrandPanel() {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, w, h, new Color(38, 166, 224)));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(-w / 6, -w / 6, w / 2, w / 2);
                g2.fillOval((int) (w * 0.50), (int) (h * 0.62), (int) (w * 0.65), (int) (w * 0.65));
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval((int) (w * 0.70), (int) (h * 0.08), w / 4, w / 4);
                g2.drawOval((int) (w * 0.05), (int) (h * 0.78), w / 6, w / 6);
                g2.dispose();
            }
        };

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(40, 60, 40, 60));

        LogoMark logo = new LogoMark();
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = label("MediBook", 46, Font.BOLD, Color.WHITE);
        JLabel tag = label("<html><div style='width:320px'>Smart doctor appointment management for modern clinics.</div></html>",
                20, Font.PLAIN, new Color(224, 242, 254));
        JLabel desc = label("<html><div style='width:320px'>Book, track and manage patient visits from one simple place.</div></html>",
                15, Font.PLAIN, new Color(186, 220, 250));

        content.add(logo);
        content.add(Box.createVerticalStrut(22));
        content.add(name);
        content.add(Box.createVerticalStrut(12));
        content.add(tag);
        content.add(Box.createVerticalStrut(10));
        content.add(desc);
        content.add(Box.createVerticalStrut(34));
        content.add(featureRow("Book appointments in seconds"));
        content.add(Box.createVerticalStrut(14));
        content.add(featureRow("Manage doctors, patients and schedules"));
        content.add(Box.createVerticalStrut(14));
        content.add(featureRow("Secure, reliable patient records"));

        p.add(content);
        return p;
    }

    private JPanel featureRow(String text) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        r.setOpaque(false);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(new CheckDot());
        JLabel l = label(text, 16, Font.PLAIN, Color.WHITE);
        r.add(l);
        return r;
    }

    // =====================================================
    // RIGHT: LOGIN FORM
    // =====================================================
    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PAGE_BG);

        RoundedCard card = new RoundedCard(28);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(44, 50, 48, 50));
        row = 0;

        compactLogo = label("+ MediBook", 28, Font.BOLD, PRIMARY);
        ((JLabel) compactLogo).setHorizontalAlignment(SwingConstants.CENTER);
        put(card, compactLogo, 0, 18);

        put(card, label("Welcome back", 30, Font.BOLD, TEXT), 0, 4);
        put(card, label("Sign in to continue to your dashboard", 14, Font.PLAIN, MUTED), 0, 26);

        // username
        put(card, label("Username", 13, Font.BOLD, TEXT), 0, 6);
        usernameField = new JTextField();
        put(card, new InputBox(true, usernameField, null), 0, 16);

        // password
        put(card, label("Password", 13, Font.BOLD, TEXT), 0, 6);
        passwordField = new JPasswordField();
        passwordField.setEchoChar('\u2022');
        JButton eye = new JButton("Show");
        eye.setFont(new Font(FONT, Font.BOLD, 12));
        eye.setForeground(PRIMARY);
        eye.setContentAreaFilled(false);
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addActionListener(e -> {
            if (passwordField.getEchoChar() == (char) 0) {
                passwordField.setEchoChar('\u2022');
                eye.setText("Show");
            } else {
                passwordField.setEchoChar((char) 0);
                eye.setText("Hide");
            }
        });
        put(card, new InputBox(false, passwordField, eye), 0, 12);

        // remember + forgot
        rememberBox = new JCheckBox("Remember me");
        rememberBox.setOpaque(false);
        rememberBox.setFont(new Font(FONT, Font.PLAIN, 13));
        rememberBox.setForeground(MUTED);
        rememberBox.setFocusPainted(false);
        JLabel forgot = label("Forgot password?", 13, Font.BOLD, PRIMARY);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                        "Please contact your administrator to reset your password.",
                        "Forgot Password", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.add(rememberBox, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        put(card, options, 0, 8);

        // error message
        errorLabel = label(" ", 13, Font.PLAIN, ERROR);
        put(card, errorLabel, 0, 10);

        // login button
        loginButton = new GradientButton("Sign In");
        loginButton.addActionListener(e -> login());
        put(card, loginButton, 0, 0);

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> login());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        form.add(card, c);

        JLabel foot = label("\u00A9 2026 MediBook  \u2022  Press F11 for full screen", 12, Font.PLAIN, MUTED);
        c.gridy = 1;
        c.insets = new Insets(14, 0, 0, 0);
        form.add(foot, c);
        return form;
    }

    private void put(JPanel p, Component comp, int top, int bottom) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, bottom, 0);
        p.add(comp, c);
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // =====================================================
    // LOGIN LOGIC (same table/columns as before, now off the UI thread)
    // =====================================================
    private void login() {
        if (busy) return;
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }
        showError(" ");
        setBusy(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
                try (Connection con = DB.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (get()) {
                        if (rememberBox.isSelected()) prefs.put("username", username);
                        else prefs.remove("username");
                        onLoginSuccess(username);
                    } else {
                        showError("Invalid username or password.");
                        passwordField.setText("");
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    showError("Cannot connect to database. Is MySQL running?");
                    errorLabel.setToolTipText(t.getMessage());
                }
            }
        }.execute();
    }

    /** Called after a successful login. Open your main window here. */
   private void onLoginSuccess(String username) {

    dispose();

    SwingUtilities.invokeLater(() -> {
        new App().setVisible(true);
    });
}

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setToolTipText(null);
    }

    private void setBusy(boolean b) {
        busy = b;
        loginButton.setText(b ? "Signing in..." : "Sign In");
        usernameField.setEnabled(!b);
        passwordField.setEnabled(!b);
        setCursor(Cursor.getPredefinedCursor(b ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void toggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!fullScreen) {
            dispose();
            setUndecorated(true);
            setVisible(true);
            gd.setFullScreenWindow(this);
        } else {
            gd.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setVisible(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        fullScreen = !fullScreen;
    }

    // =====================================================
    // CUSTOM COMPONENTS
    // =====================================================

    /** White rounded card with a soft shadow. */
    private static class RoundedCard extends JPanel {
        private final int arc;

        RoundedCard(int arc) {
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 480), d.height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(300, super.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            for (int i = 0; i < 9; i++) {
                g2.setColor(new Color(15, 23, 42, 7));
                g2.fillRoundRect(i, i + 3, w - 2 * i, h - 2 * i - 2, arc + 8, arc + 8);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(9, 7, w - 18, h - 18, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded input with an icon on the left and an optional component on the right. */
    private static class InputBox extends JPanel {
        private boolean focused;

        InputBox(boolean userIcon, JTextComponent field, JComponent trailing) {
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(0, 14, 0, 10));
            setPreferredSize(new Dimension(100, 50));

            field.setOpaque(false);
            field.setBorder(new EmptyBorder(12, 0, 12, 0));
            field.setFont(new Font(FONT, Font.PLAIN, 15));
            field.setForeground(TEXT);
            field.setCaretColor(PRIMARY);
            field.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { focused = true; repaint(); }
                public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });

            add(new IconView(userIcon), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            if (trailing != null) add(trailing, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(focused ? Color.WHITE : new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.setColor(focused ? PRIMARY : BORDER);
            g2.setStroke(new BasicStroke(focused ? 2f : 1f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
            g2.dispose();
        }
    }

    /** Vector user / lock icon (no font or image files needed). */
    private static class IconView extends JComponent {
        private final boolean user;

        IconView(boolean user) {
            this.user = user;
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(MUTED);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate((getWidth() - 24) / 2, (getHeight() - 24) / 2);
            if (user) {
                g2.drawOval(8, 3, 8, 8);
                g2.drawArc(4, 14, 16, 14, 0, 180);
            } else {
                g2.drawRoundRect(5, 11, 14, 10, 4, 4);
                g2.drawArc(7, 3, 10, 16, 0, 180);
                g2.fillOval(11, 15, 3, 3);
            }
            g2.dispose();
        }
    }

    /** Gradient rounded button with hover effect. */
    private static class GradientButton extends JButton {
        private boolean hover;

        GradientButton(String text) {
            super(text);
            setFont(new Font(FONT, Font.BOLD, 16));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 52));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color a = hover ? PRIMARY.brighter() : PRIMARY;
            Color b = hover ? PRIMARY_DARK.brighter() : PRIMARY_DARK;
            g2.setPaint(new GradientPaint(0, 0, a, getWidth(), 0, b));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** White rounded logo tile with a medical cross. */
    private static class LogoMark extends JComponent {
        LogoMark() {
            setPreferredSize(new Dimension(68, 68));
            setMaximumSize(new Dimension(68, 68));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, 64, 64, 22, 22);
            g2.setColor(PRIMARY);
            g2.fillRoundRect(26, 14, 12, 36, 5, 5);
            g2.fillRoundRect(14, 26, 36, 12, 5, 5);
            g2.dispose();
        }
    }

    /** Small circle with a check mark for the feature list. */
    private static class CheckDot extends JComponent {
        CheckDot() {
            setPreferredSize(new Dimension(28, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillOval(0, 0, 26, 26);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{7, 11, 19}, new int[]{13, 17, 9}, 3);
            g2.dispose();
        }
    } 

    // =====================================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}