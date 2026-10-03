package patient;

import database.DB;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * MediBook Patient Panel
 *
 * Features:
 * - Shows patients from appointments table
 * - Search by patient name / phone
 * - Filter patients according to doctor
 * - Shows appointment count and last appointment
 * - Refreshes data from database
 *
 * Note:
 * Patient details are currently stored in the appointments table,
 * so this panel reads patient information from appointments.
 */
public class PatientPanel extends JPanel {

    private static final String FONT = "Segoe UI";

    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color DARK_BLUE = new Color(29, 78, 216);
    private static final Color BG = new Color(243, 246, 251);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color RED = new Color(220, 70, 70);

    private final JTextField searchField;
    private final JComboBox<DoctorItem> doctorFilter;
    private final JTable patientTable;
    private final DefaultTableModel tableModel;
    private final JLabel countLabel;

    public PatientPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 24, 24, 24));

        // =====================================================
        // TOP TITLE
        // =====================================================

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 8, 16, 8));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Patients");
        title.setFont(new Font(FONT, Font.BOLD, 28));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("View and filter patient records according to doctor");
        subtitle.setFont(new Font(FONT, Font.PLAIN, 13));
        subtitle.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        top.add(titleBox, BorderLayout.WEST);

        // =====================================================
        // FILTER CARD
        // =====================================================

        RoundedPanel filterCard = new RoundedPanel(18, WHITE, true);
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font(FONT, Font.BOLD, 12));
        searchLabel.setForeground(MUTED);

        searchField = new JTextField();
        searchField.setFont(new Font(FONT, Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(230, 38));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(0, 10, 0, 10)));

        JLabel doctorLabel = new JLabel("Doctor:");
        doctorLabel.setFont(new Font(FONT, Font.BOLD, 12));
        doctorLabel.setForeground(MUTED);

        doctorFilter = new JComboBox<>();
        doctorFilter.setFont(new Font(FONT, Font.PLAIN, 13));
        doctorFilter.setPreferredSize(new Dimension(250, 38));

        RoundedButton refreshButton = new RoundedButton("Refresh", BLUE, DARK_BLUE, 12);
        refreshButton.setFont(new Font(FONT, Font.BOLD, 12));
        refreshButton.setPreferredSize(new Dimension(100, 38));

        gbc.gridx = 0;
        gbc.weightx = 0;
        filters.add(searchLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 0.35;
        filters.add(searchField, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        filters.add(doctorLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 0.35;
        filters.add(doctorFilter, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        filters.add(refreshButton, gbc);

        filterCard.add(filters, BorderLayout.CENTER);

        // =====================================================
        // TABLE
        // =====================================================

        String[] columns = {
                "ID",
                "Patient",
                "Age",
                "Phone",
                "Gender",
                "Doctor",
                "Specialization",
                "Appointments",
                "Last Appointment"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        patientTable = new JTable(tableModel);
        patientTable.setRowHeight(40);
        patientTable.setFont(new Font(FONT, Font.PLAIN, 12));
        patientTable.setForeground(TEXT);
        patientTable.setBackground(WHITE);
        patientTable.setGridColor(new Color(238, 242, 247));
        patientTable.setSelectionBackground(new Color(219, 234, 254));
        patientTable.setSelectionForeground(TEXT);
        patientTable.setShowVerticalLines(false);
        patientTable.setIntercellSpacing(new Dimension(0, 1));
        patientTable.setAutoCreateRowSorter(true);

        JTableHeader header = patientTable.getTableHeader();
        header.setFont(new Font(FONT, Font.BOLD, 12));
        header.setForeground(new Color(71, 85, 105));
        header.setBackground(new Color(248, 250, 252));
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        // Center selected columns.
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        patientTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        patientTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        patientTable.getColumnModel().getColumn(2).setPreferredWidth(55);
        patientTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        patientTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        patientTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        patientTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        patientTable.getColumnModel().getColumn(7).setPreferredWidth(90);
        patientTable.getColumnModel().getColumn(8).setPreferredWidth(130);

        patientTable.getColumnModel().getColumn(0).setCellRenderer(center);
        patientTable.getColumnModel().getColumn(2).setCellRenderer(center);
        patientTable.getColumnModel().getColumn(4).setCellRenderer(center);
        patientTable.getColumnModel().getColumn(7).setCellRenderer(center);

        JScrollPane tableScroll = new JScrollPane(patientTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(WHITE);

        RoundedPanel tableCard = new RoundedPanel(18, WHITE, true);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));
        tableCard.add(tableScroll, BorderLayout.CENTER);

        // =====================================================
        // BOTTOM
        // =====================================================

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 8, 0, 8));

        countLabel = new JLabel("Showing 0 patients");
        countLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        countLabel.setForeground(MUTED);

        bottom.add(countLabel, BorderLayout.WEST);

        // =====================================================
        // MAIN
        // =====================================================

        // Table wrapper keeps the table card separated from the filter card.
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(tableCard, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(filterCard, BorderLayout.NORTH);
        content.add(tableWrapper, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        // =====================================================
        // EVENTS
        // =====================================================

        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        refreshPatients();
                    }

                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        refreshPatients();
                    }

                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        refreshPatients();
                    }
                });

        doctorFilter.addActionListener(e -> refreshPatients());
        refreshButton.addActionListener(e -> refresh());

        patientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && patientTable.getSelectedRow() >= 0) {
                    showSelectedPatient();
                }
            }
        });

        refresh();
    }

    // =========================================================
    // REFRESH
    // =========================================================

    public void refresh() {
        loadDoctors();
        refreshPatients();
    }

    // =========================================================
    // LOAD DOCTORS
    // =========================================================

    private void loadDoctors() {

        DoctorItem selected = (DoctorItem) doctorFilter.getSelectedItem();
        int selectedId = selected == null ? -1 : selected.id;

        doctorFilter.removeAllItems();

        doctorFilter.addItem(new DoctorItem(0, "All Doctors"));

        String sql = """
                SELECT id, name, specialization
                FROM doctors
                WHERE status = 'Active'
                ORDER BY name
                """;

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                doctorFilter.addItem(
                        new DoctorItem(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("specialization")));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Unable to load doctors.\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        if (selectedId != -1) {
            for (int i = 0; i < doctorFilter.getItemCount(); i++) {
                DoctorItem item = doctorFilter.getItemAt(i);

                if (item.id == selectedId) {
                    doctorFilter.setSelectedIndex(i);
                    return;
                }
            }
        }

        doctorFilter.setSelectedIndex(0);
    }

    // =========================================================
    // LOAD PATIENTS
    // =========================================================

    private void refreshPatients() {

        tableModel.setRowCount(0);

        String search = searchField.getText().trim();
        DoctorItem doctor = (DoctorItem) doctorFilter.getSelectedItem();

        StringBuilder sql = new StringBuilder("""
                SELECT
                    MIN(a.id) AS patient_record_id,
                    a.patient_name,
                    a.age,
                    a.phone,
                    a.gender,
                    d.id AS doctor_id,
                    d.name AS doctor_name,
                    d.specialization,
                    COUNT(a.id) AS appointment_count,
                    MAX(a.appointment_date) AS last_date,
                    MAX(a.appointment_time) AS last_time
                FROM appointments a
                INNER JOIN doctors d ON a.doctor_id = d.id
                WHERE 1 = 1
                """);

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (!search.isEmpty()) {
            sql.append("""
                    AND (
                        a.patient_name LIKE ?
                        OR a.phone LIKE ?
                    )
                    """);

            String value = "%" + search + "%";
            params.add(value);
            params.add(value);
        }

        if (doctor != null && doctor.id != 0) {
            sql.append(" AND d.id = ? ");
            params.add(doctor.id);
        }

        /*
         * A patient can have appointments with different doctors.
         * Therefore we group by patient + doctor.
         */
        sql.append("""
                GROUP BY
                    a.patient_name,
                    a.age,
                    a.phone,
                    a.gender,
                    d.id,
                    d.name,
                    d.specialization
                ORDER BY a.patient_name ASC
                """);

        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String lastDate = rs.getDate("last_date") == null
                            ? "-"
                            : rs.getDate("last_date").toString();

                    String lastTime = rs.getString("last_time");

                    if (lastTime == null || lastTime.trim().isEmpty()) {
                        lastTime = "";
                    }

                    String lastAppointment = lastDate.equals("-")
                            ? "-"
                            : lastDate + (lastTime.isEmpty() ? "" : " " + lastTime);

                    tableModel.addRow(new Object[] {
                            rs.getInt("patient_record_id"),
                            rs.getString("patient_name"),
                            rs.getInt("age"),
                            rs.getString("phone"),
                            rs.getString("gender"),
                            rs.getString("doctor_name"),
                            rs.getString("specialization"),
                            rs.getInt("appointment_count"),
                            lastAppointment
                    });
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Unable to load patients.\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        countLabel.setText(
                "Showing " + tableModel.getRowCount() + " patient record"
                        + (tableModel.getRowCount() == 1 ? "" : "s"));
    }

    // =========================================================
    // PATIENT DETAILS
    // =========================================================

    private void showSelectedPatient() {

        int viewRow = patientTable.getSelectedRow();

        if (viewRow < 0) {
            return;
        }

        int row = patientTable.convertRowIndexToModel(viewRow);

        String patient = String.valueOf(tableModel.getValueAt(row, 1));
        String age = String.valueOf(tableModel.getValueAt(row, 2));
        String phone = String.valueOf(tableModel.getValueAt(row, 3));
        String gender = String.valueOf(tableModel.getValueAt(row, 4));
        String doctor = String.valueOf(tableModel.getValueAt(row, 5));
        String specialization = String.valueOf(tableModel.getValueAt(row, 6));
        String appointments = String.valueOf(tableModel.getValueAt(row, 7));
        String lastAppointment = String.valueOf(tableModel.getValueAt(row, 8));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        addDetail(panel, "Patient", patient);
        addDetail(panel, "Age", age);
        addDetail(panel, "Phone", phone);
        addDetail(panel, "Gender", gender);
        addDetail(panel, "Doctor", doctor);
        addDetail(panel, "Specialization", specialization);
        addDetail(panel, "Total Appointments", appointments);
        addDetail(panel, "Last Appointment", lastAppointment);

        JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                panel,
                "Patient Details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void addDetail(JPanel parent, String label, String value) {

        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(450, 30));

        JLabel l = new JLabel(label + ":");
        l.setFont(new Font(FONT, Font.BOLD, 12));
        l.setForeground(MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font(FONT, Font.PLAIN, 12));
        v.setForeground(TEXT);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);

        parent.add(row);
        parent.add(Box.createVerticalStrut(5));
    }

    // =========================================================
    // DOCTOR ITEM
    // =========================================================

    private static class DoctorItem {

        private final int id;
        private final String name;
        private final String specialization;

        DoctorItem(int id, String name) {
            this(id, name, "");
        }

        DoctorItem(int id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization == null ? "" : specialization;
        }

        @Override
        public String toString() {

            if (id == 0) {
                return name;
            }

            if (specialization.isEmpty()) {
                return name;
            }

            return name + " - " + specialization;
        }
    }

    // =========================================================
    // ROUNDED PANEL
    // =========================================================

    private static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color fill;
        private final boolean shadow;

        RoundedPanel(int radius, Color fill, boolean shadow) {
            this.radius = radius;
            this.fill = fill;
            this.shadow = shadow;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (shadow) {
                for (int i = 1; i <= 5; i++) {
                    g2.setColor(new Color(15, 23, 42, 5));

                    g2.fillRoundRect(
                            i - 2,
                            i,
                            w - (i * 2) + 2,
                            h - (i * 2),
                            radius + i,
                            radius + i);
                }
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // =========================================================
    // ROUNDED BUTTON
    // =========================================================

    private static class RoundedButton extends JButton {

        private final Color normal;
        private final Color hoverColor;
        private final int radius;
        private boolean hover;

        RoundedButton(
                String text,
                Color normal,
                Color hoverColor,
                int radius) {
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

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Color c = hover ? hoverColor : normal;

            if (getModel().isPressed()) {
                c = c.darker();
            }

            g2.setColor(c);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius);

            g2.dispose();

            super.paintComponent(g);
        }
    }
}
