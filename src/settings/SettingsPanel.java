package settings;

import database.DB;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Settings page for the MediBook desktop application. */
public class SettingsPanel extends JPanel {
    private static final String FONT = "Segoe UI";
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BLUE_HOVER = new Color(29, 78, 216);
    private static final Color BG = new Color(243, 246, 251);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);

    private final Runnable onSettingsChanged;

    public SettingsPanel(Runnable onSettingsChanged) {
        this.onSettingsChanged = onSettingsChanged;
        buildUI();
    }

    private void buildUI() {
        setBackground(BG);
        setLayout(new java.awt.BorderLayout());

        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Application Settings");
        title.setFont(new Font(FONT, Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);

        JLabel subtitle = new JLabel("Manage your administrator account and database connection.");
        subtitle.setFont(new Font(FONT, Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(6, 0, 20, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);

        JPanel accountCard = card();
        accountCard.setLayout(new BorderLayout(0, 16));
        accountCard.add(sectionTitle("Administrator Account"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField username = new JTextField("admin", 22);
        JPasswordField currentPassword = new JPasswordField(22);
        JPasswordField newPassword = new JPasswordField(22);
        JPasswordField confirmPassword = new JPasswordField(22);
        addField(fields, gbc, 0, "Username", username);
        addField(fields, gbc, 1, "Current password", currentPassword);
        addField(fields, gbc, 2, "New password", newPassword);
        addField(fields, gbc, 3, "Confirm new password", confirmPassword);
        accountCard.add(fields, BorderLayout.CENTER);

        JPanel accountButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        accountButtons.setOpaque(false);
        JButton saveButton = primaryButton("Save Password");
        saveButton.addActionListener(e -> updatePassword(
                username.getText().trim(),
                new String(currentPassword.getPassword()),
                new String(newPassword.getPassword()),
                new String(confirmPassword.getPassword()),
                currentPassword, newPassword, confirmPassword));
        accountButtons.add(saveButton);
        accountCard.add(accountButtons, BorderLayout.SOUTH);
        accountCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(accountCard);
        content.add(javax.swing.Box.createVerticalStrut(18));

        JPanel databaseCard = card();
        databaseCard.setLayout(new BorderLayout(0, 14));
        databaseCard.add(sectionTitle("Database Connection"), BorderLayout.NORTH);
        JLabel databaseInfo = new JLabel("Check whether MediBook can connect to your configured MySQL database.");
        databaseInfo.setFont(new Font(FONT, Font.PLAIN, 13));
        databaseInfo.setForeground(MUTED);
        databaseCard.add(databaseInfo, BorderLayout.CENTER);

        JPanel databaseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        databaseButtons.setOpaque(false);
        JButton testButton = primaryButton("Test Database");
        testButton.addActionListener(e -> testDatabase());
        JButton refreshButton = primaryButton("Refresh Dashboard Data");
        refreshButton.addActionListener(e -> {
            if (onSettingsChanged != null) onSettingsChanged.run();
            JOptionPane.showMessageDialog(this, "Dashboard data refreshed.", "MediBook", JOptionPane.INFORMATION_MESSAGE);
        });
        databaseButtons.add(testButton);
        databaseButtons.add(refreshButton);
        databaseCard.add(databaseButtons, BorderLayout.SOUTH);
        databaseCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(databaseCard);

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(22, 22, 22, 22)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 17));
        label.setForeground(TEXT);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT, Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(text.length() > 18 ? 190 : 150, 38));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(BLUE_HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(BLUE); }
        });
        return button;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel text = new JLabel(label);
        text.setFont(new Font(FONT, Font.BOLD, 12));
        text.setForeground(TEXT);
        panel.add(text, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        field.setFont(new Font(FONT, Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(300, 34));
        panel.add(field, gbc);
    }

    private void updatePassword(String username, String current, String next, String confirm,
                                JPasswordField currentField, JPasswordField nextField,
                                JPasswordField confirmField) {
        if (username.isEmpty() || current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showMessage("Please fill in all account fields.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (next.length() < 6) {
            showMessage("New password must contain at least 6 characters.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!next.equals(confirm)) {
            showMessage("New password and confirmation do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectSql = "SELECT password FROM users WHERE username = ?";
        String updateSql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setString(1, username);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    showMessage("Username was not found.", "Account", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!current.equals(rs.getString("password"))) {
                    showMessage("Current password is incorrect.", "Account", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, next);
                update.setString(2, username);
                if (update.executeUpdate() > 0) {
                    currentField.setText("");
                    nextField.setText("");
                    confirmField.setText("");
                    showMessage("Password updated successfully.", "MediBook", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showMessage("Password was not updated.", "MediBook", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            showMessage("Could not update password: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void testDatabase() {
        try (Connection connection = DB.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                showMessage("Database connection successful.", "Database Status", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showMessage("Database connection failed.", "Database Status", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            showMessage("Database connection failed: " + ex.getMessage(), "Database Status", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }
}
