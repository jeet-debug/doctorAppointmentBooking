package appointment;

import database.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import static appointment.UI.*;

public class AppointmentsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private DefaultTableModel model;
    private JTable table;

    private JTextField tfSearch;

    private JComboBox<String> cbFilterSpec;
    private JComboBox<String> cbFilterStatus;

    private JLabel countLabel;

    /** Called after cancel so Book Appointment page can refresh slots. */
    private final Runnable onChanged;

    public AppointmentsPanel() {
        this(null);
    }

    public AppointmentsPanel(Runnable onChanged) {

        this.onChanged = onChanged;

        setLayout(new BorderLayout());
        setBackground(BG);

        add(
                header(
                        "Appointments",
                        "All booked appointments - search, filter and manage"
                ),
                BorderLayout.NORTH
        );

        add(buildPage(), BorderLayout.CENTER);

        refresh();
    }

    /**
     * Load latest appointments from database.
     */
    public void refresh() {

        if (model == null)
            return;

        String search = tfSearch.getText().trim().toLowerCase();

        String spec =
                (String) cbFilterSpec.getSelectedItem();

        String status =
                (String) cbFilterStatus.getSelectedItem();

        model.setRowCount(0);

        StringBuilder sql = new StringBuilder("""
            SELECT
                a.id,
                a.patient_name,
                a.age,
                a.phone,
                a.gender,
                d.name AS doctor_name,
                d.specialization,
                a.appointment_date,
                a.appointment_time,
                a.fee,
                a.status
            FROM appointments a
            INNER JOIN doctors d
                ON a.doctor_id = d.id
            WHERE 1 = 1
            """);

        if (!"All Specializations".equals(spec)) {
            sql.append(" AND d.specialization = ?");
        }

        if (!"All Status".equals(status)) {
            sql.append(" AND a.status = ?");
        }

        sql.append(" ORDER BY a.id DESC");

        int shown = 0;
        int total = 0;

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql.toString())
        ) {

            int index = 1;

            // Specialization filter
            if (!"All Specializations".equals(spec)) {

                ps.setString(index++, spec);
            }

            // Status filter
            if (!"All Status".equals(status)) {

                ps.setString(index++, status);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int id = rs.getInt("id");

                    String patient =
                            rs.getString("patient_name");

                    int age =
                            rs.getInt("age");

                    String phone =
                            rs.getString("phone");

                    String doctor =
                            rs.getString("doctor_name");

                    String specialization =
                            rs.getString("specialization");

                    String date =
                            rs.getDate("appointment_date")
                                    .toLocalDate()
                                    .format(DATE_FMT);

                    String time =
                            rs.getString("appointment_time");

                    String fee =
                            RUPEE + " " +
                            rs.getBigDecimal("fee");

                    String appointmentStatus =
                            rs.getString("status");

                    /*
                     * Search
                     */
                    String searchableText =
                            (
                                    id + " "
                                    + patient + " "
                                    + phone + " "
                                    + doctor + " "
                                    + specialization + " "
                                    + date
                            ).toLowerCase();

                    if (!search.isEmpty()
                            && !searchableText.contains(search)) {

                        continue;
                    }

                    model.addRow(new Object[] {

                            id,
                            patient,
                            age,
                            phone,
                            doctor,
                            specialization,
                            date,
                            time,
                            fee,
                            appointmentStatus

                    });

                    shown++;
                }
            }

            /*
             * Total appointments
             */
            String countSql =
                    "SELECT COUNT(*) FROM appointments";

            try (
                    PreparedStatement countPs =
                            con.prepareStatement(countSql);

                    ResultSet countRs =
                            countPs.executeQuery()
            ) {

                if (countRs.next()) {

                    total =
                            countRs.getInt(1);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load appointments.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        countLabel.setText(
                "Showing "
                        + shown
                        + " of "
                        + total
                        + " appointments"
        );
    }

    // =========================================================
    // UI
    // =========================================================

    private JComponent buildPage() {

        JPanel page =
                new JPanel(new BorderLayout());

        page.setOpaque(false);

        page.setBorder(
                new EmptyBorder(
                        0,
                        28,
                        20,
                        28
                )
        );

        Card card =
                new Card(
                        18,
                        20,
                        18,
                        20
                );

        card.setLayout(
                new BorderLayout(
                        0,
                        12
                )
        );

        // =====================================================
        // TOOLBAR
        // =====================================================

        JPanel bar =
                new JPanel(new BorderLayout());

        bar.setOpaque(false);

        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                0
                        )
                );

        left.setOpaque(false);

        JLabel sl =
                new JLabel("Search:");

        sl.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        sl.setForeground(MUTED);

        // Search field
        tfSearch =
                new JTextField();

        styleField(tfSearch);

        tfSearch.setPreferredSize(
                new Dimension(
                        260,
                        40
                )
        );

        tfSearch.setToolTipText(
                "Search by patient, phone, doctor, ID or date"
        );

        // =====================================================
        // SPECIALIZATION FILTER
        // =====================================================

        cbFilterSpec =
                new JComboBox<>();

        cbFilterSpec.addItem(
                "All Specializations"
        );

        loadSpecializations();

        styleCombo(cbFilterSpec);

        cbFilterSpec.setPreferredSize(
                new Dimension(
                        210,
                        40
                )
        );

        // =====================================================
        // STATUS FILTER
        // =====================================================

        cbFilterStatus =
                new JComboBox<>(
                        new String[] {
                                "All Status",
                                "Booked",
                                "Cancelled"
                        }
                );

        styleCombo(cbFilterStatus);

        cbFilterStatus.setPreferredSize(
                new Dimension(
                        140,
                        40
                )
        );

        left.add(sl);
        left.add(tfSearch);
        left.add(cbFilterSpec);
        left.add(cbFilterStatus);

        // =====================================================
        // CANCEL BUTTON
        // =====================================================

        RoundedButton cancel =
                new RoundedButton(
                        "Cancel Appointment",
                        RED,
                        RED_HOVER,
                        40
                );

        cancel.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        cancel.setPreferredSize(
                new Dimension(
                        190,
                        40
                )
        );

        cancel.addActionListener(
                e -> cancelSelected()
        );

        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );

        right.setOpaque(false);

        right.add(cancel);

        bar.add(
                left,
                BorderLayout.WEST
        );

        bar.add(
                right,
                BorderLayout.EAST
        );

        card.add(
                bar,
                BorderLayout.NORTH
        );

        // =====================================================
        // TABLE
        // =====================================================

        String[] cols = {

                "ID",
                "Patient",
                "Age",
                "Phone",
                "Doctor",
                "Specialization",
                "Date",
                "Time",
                "Fee",
                "Status"

        };

        model =
                new DefaultTableModel(
                        cols,
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {

                        return false;
                    }
                };

        table =
                new JTable(model);

        table.setRowHeight(38);

        table.setShowVerticalLines(false);

        table.setGridColor(LINE);

        table.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        13
                )
        );

        table.setSelectionBackground(
                new Color(
                        214,
                        232,
                        252
                )
        );

        table.setSelectionForeground(TEXT);

        table.setFillsViewportHeight(true);

        table.setAutoResizeMode(
                JTable.AUTO_RESIZE_ALL_COLUMNS
        );

        table.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(50);

        table.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(40);

        table.getColumnModel()
                .getColumn(8)
                .setPreferredWidth(60);

        // =====================================================
        // TABLE BODY RENDERER
        // =====================================================

        table.setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable t,
                            Object value,
                            boolean selected,
                            boolean focused,
                            int row,
                            int column
                    ) {

                        JLabel label =
                                (JLabel) super
                                        .getTableCellRendererComponent(
                                                t,
                                                value,
                                                selected,
                                                false,
                                                row,
                                                column
                                        );

                        label.setBorder(
                                new EmptyBorder(
                                        0,
                                        12,
                                        0,
                                        8
                                )
                        );

                        label.setFont(
                                new Font(
                                        FONT,
                                        Font.PLAIN,
                                        13
                                )
                        );

                        if (!selected) {

                            label.setBackground(
                                    row % 2 == 0
                                            ? Color.WHITE
                                            : new Color(
                                                    249,
                                                    251,
                                                    254
                                            )
                            );

                            label.setForeground(TEXT);
                        }

                        // Status column
                        if (column == 9) {

                            label.setFont(
                                    new Font(
                                            FONT,
                                            Font.BOLD,
                                            13
                                    )
                            );

                            if (!selected) {

                                label.setForeground(
                                        "Booked".equals(value)
                                                ? GREEN
                                                : RED
                                );
                            }
                        }

                        return label;
                    }
                }
        );

        // =====================================================
        // TABLE HEADER
        // =====================================================

        table.getTableHeader()
                .setReorderingAllowed(false);

        table.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                0,
                                40
                        )
                );

        table.getTableHeader()
                .setDefaultRenderer(
                        new DefaultTableCellRenderer() {

                            @Override
                            public Component getTableCellRendererComponent(
                                    JTable t,
                                    Object value,
                                    boolean selected,
                                    boolean focused,
                                    int row,
                                    int column
                            ) {

                                JLabel label =
                                        (JLabel) super
                                                .getTableCellRendererComponent(
                                                        t,
                                                        value,
                                                        false,
                                                        false,
                                                        row,
                                                        column
                                                );

                                label.setOpaque(true);

                                label.setBackground(
                                        new Color(
                                                240,
                                                245,
                                                251
                                        )
                                );

                                label.setForeground(MUTED);

                                label.setFont(
                                        new Font(
                                                FONT,
                                                Font.BOLD,
                                                12
                                        )
                                );

                                label.setBorder(
                                        BorderFactory
                                                .createCompoundBorder(
                                                        BorderFactory
                                                                .createMatteBorder(
                                                                        0,
                                                                        0,
                                                                        1,
                                                                        0,
                                                                        LINE
                                                                ),
                                                        new EmptyBorder(
                                                                0,
                                                                12,
                                                                0,
                                                                8
                                                        )
                                                )
                                );

                                label.setHorizontalAlignment(
                                        SwingConstants.LEFT
                                );

                                return label;
                            }
                        }
                );

        JScrollPane sp =
                new JScrollPane(table);

        sp.setBorder(
                BorderFactory
                        .createLineBorder(LINE)
        );

        sp.getViewport()
                .setBackground(Color.WHITE);

        card.add(
                sp,
                BorderLayout.CENTER
        );

        // =====================================================
        // COUNT LABEL
        // =====================================================

        countLabel =
                new JLabel(" ");

        countLabel.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        12
                )
        );

        countLabel.setForeground(MUTED);

        card.add(
                countLabel,
                BorderLayout.SOUTH
        );

        // =====================================================
        // SEARCH LISTENER
        // =====================================================

        tfSearch
                .getDocument()
                .addDocumentListener(
                        new DocumentListener() {

                            @Override
                            public void insertUpdate(
                                    DocumentEvent e
                            ) {

                                refresh();
                            }

                            @Override
                            public void removeUpdate(
                                    DocumentEvent e
                            ) {

                                refresh();
                            }

                            @Override
                            public void changedUpdate(
                                    DocumentEvent e
                            ) {

                                refresh();
                            }
                        }
                );

        // =====================================================
        // FILTER LISTENERS
        // =====================================================

        cbFilterSpec.addActionListener(
                e -> refresh()
        );

        cbFilterStatus.addActionListener(
                e -> refresh()
        );

        page.add(
                card,
                BorderLayout.CENTER
        );

        return page;
    }

    // =========================================================
    // LOAD SPECIALIZATIONS FROM DATABASE
    // =========================================================

    private void loadSpecializations() {

        String sql = """
            SELECT DISTINCT specialization
            FROM doctors
            WHERE status = 'Active'
            ORDER BY specialization
            """;

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql);
                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                cbFilterSpec.addItem(
                        rs.getString("specialization")
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load specializations.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =========================================================
    // CANCEL APPOINTMENT
    // =========================================================

    private void cancelSelected() {

        int row =
                table.getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select an appointment from the table first.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        int id =
                Integer.parseInt(
                        String.valueOf(
                                model.getValueAt(
                                        row,
                                        0
                                )
                        )
                );

        String patient =
                String.valueOf(
                        model.getValueAt(
                                row,
                                1
                        )
                );

        String currentStatus =
                String.valueOf(
                        model.getValueAt(
                                row,
                                9
                        )
                );

        if ("Cancelled".equals(currentStatus)) {

            JOptionPane.showMessageDialog(
                    this,
                    "This appointment is already cancelled."
            );

            return;
        }

        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        "Cancel appointment #"
                                + id
                                + " for "
                                + patient
                                + "?",
                        "Cancel Appointment",
                        JOptionPane.YES_NO_OPTION
                );

        if (confirm != JOptionPane.YES_OPTION)
            return;

        String sql = """
            UPDATE appointments
            SET status = 'Cancelled'
            WHERE id = ?
            """;

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            int updated =
                    ps.executeUpdate();

            if (updated > 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Appointment cancelled successfully."
                );

                refresh();

                /*
                 * Notify BookAppointmentPanel
                 * so cancelled slot can become available.
                 */
                if (onChanged != null) {

                    onChanged.run();
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,       
                    "Unable to cancel appointment.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}