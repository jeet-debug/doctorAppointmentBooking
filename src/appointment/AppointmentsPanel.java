package appointment;

import database.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
                                "Completed",
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
        // ACTION BUTTONS
        // =====================================================

        JPanel actions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                0
                        )
                );

        actions.setOpaque(false);

        RoundedButton complete =
                new RoundedButton(
                        "Complete Appointment",
                        GREEN,
                        GREEN,
                        40
                );

        complete.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        complete.setPreferredSize(
                new Dimension(
                        180,
                        40
                )
        );

        complete.addActionListener(
                e -> completeSelected()
        );

        RoundedButton reAppointment =
                new RoundedButton(
                        "Re-Appointment",
                        new Color(0, 150, 136),
                        new Color(0, 125, 113),
                        40
                );

        reAppointment.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        reAppointment.setPreferredSize(
                new Dimension(
                        145,
                        40
                )
        );

        reAppointment.addActionListener(
                e -> scheduleReAppointment()
        );

        RoundedButton print =
                new RoundedButton(
                        "Print Appointment",
                        new Color(0, 121, 193),
                        new Color(0, 102, 170),
                        40
                );

        print.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        print.setPreferredSize(
                new Dimension(
                        160,
                        40
                )
        );

        print.addActionListener(
                e -> printSelected()
        );

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
                        175,
                        40
                )
        );

        cancel.addActionListener(
                e -> cancelSelected()
        );

        actions.add(complete);
        actions.add(reAppointment);
        actions.add(print);
        actions.add(cancel);

        // Keep filters and action buttons on separate rows so
        // the existing card design remains clean at different widths.
        JPanel toolbar =
                new JPanel();

        toolbar.setOpaque(false);
        toolbar.setLayout(
                new BoxLayout(
                        toolbar,
                        BoxLayout.Y_AXIS
                )
        );

        bar.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        44
                )
        );

        actions.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        44
                )
        );

        toolbar.add(bar);
        toolbar.add(Box.createVerticalStrut(8));
        toolbar.add(actions);

        card.add(
                toolbar,
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

                                if ("Booked".equals(value)) {
                                    label.setForeground(GREEN);
                                } else if ("Completed".equals(value)) {
                                    label.setForeground(new Color(0, 123, 255));
                                } else {
                                    label.setForeground(RED);
                                }
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
    // COMPLETE APPOINTMENT
    // =========================================================

    private void completeSelected() {

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
                    "Cancelled appointment cannot be completed.",
                    "MediBook",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if ("Completed".equals(currentStatus)) {

            JOptionPane.showMessageDialog(
                    this,
                    "This appointment is already completed.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        "Mark appointment #"
                                + id
                                + " for "
                                + patient
                                + " as completed?",
                        "Complete Appointment",
                        JOptionPane.YES_NO_OPTION
                );

        if (confirm != JOptionPane.YES_OPTION)
            return;

        String sql = """
            UPDATE appointments
            SET status = 'Completed'
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
                        "Appointment completed successfully.",
                        "MediBook",
                        JOptionPane.INFORMATION_MESSAGE
                );

                refresh();

                if (onChanged != null) {
                    onChanged.run();
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to complete appointment.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =========================================================
    // RE-APPOINTMENT / FOLLOW-UP
    // =========================================================

    private void scheduleReAppointment() {

        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a completed appointment first.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int appointmentId = Integer.parseInt(
                String.valueOf(model.getValueAt(row, 0))
        );

        String currentStatus = String.valueOf(
                model.getValueAt(row, 9)
        );

        if ("Cancelled".equals(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cancelled appointment cannot be used for re-appointment.",
                    "MediBook",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!"Completed".equals(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please complete this appointment first, then schedule a re-appointment.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String patient = String.valueOf(model.getValueAt(row, 1));
        String age = String.valueOf(model.getValueAt(row, 2));
        String phone = String.valueOf(model.getValueAt(row, 3));
        String doctorName = String.valueOf(model.getValueAt(row, 4));
        String specialization = String.valueOf(model.getValueAt(row, 5));

        int doctorId;
        String gender;
        java.math.BigDecimal fee;

        String patientSql = """
            SELECT
                a.patient_name,
                a.age,
                a.phone,
                a.gender,
                a.doctor_id,
                a.fee
            FROM appointments a
            WHERE a.id = ?
            """;

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(patientSql)
        ) {

            ps.setInt(1, appointmentId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Appointment details could not be found.",
                            "MediBook",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                doctorId = rs.getInt("doctor_id");
                gender = rs.getString("gender");
                fee = rs.getBigDecimal("fee");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load appointment details.\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Schedule Re-Appointment",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setSize(540, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBorder(new EmptyBorder(20, 22, 20, 22));
        main.setBackground(Color.WHITE);

        JLabel title = new JLabel("Schedule Re-Appointment");
        title.setFont(new Font(FONT, Font.BOLD, 20));
        title.setForeground(TEXT);

        JLabel info = new JLabel(
                "Patient: " + patient
                        + "  |  Doctor: " + doctorName
                        + "  |  " + specialization
        );
        info.setFont(new Font(FONT, Font.PLAIN, 12));
        info.setForeground(MUTED);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(info);

        main.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 5, 7, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel typeLabel = new JLabel("Follow-up Type:");
        typeLabel.setFont(new Font(FONT, Font.BOLD, 13));
        typeLabel.setForeground(TEXT);

        JRadioButton afterDays = new JRadioButton("After Days");
        JRadioButton specificDate = new JRadioButton("Specific Date");
        afterDays.setFont(new Font(FONT, Font.PLAIN, 13));
        specificDate.setFont(new Font(FONT, Font.PLAIN, 13));
        afterDays.setOpaque(false);
        specificDate.setOpaque(false);
        afterDays.setSelected(true);

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(afterDays);
        typeGroup.add(specificDate);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typePanel.setOpaque(false);
        typePanel.add(afterDays);
        typePanel.add(Box.createHorizontalStrut(15));
        typePanel.add(specificDate);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        form.add(typeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(typePanel, gbc);

        JLabel daysLabel = new JLabel("Number of Days:");
        daysLabel.setFont(new Font(FONT, Font.BOLD, 13));
        daysLabel.setForeground(TEXT);

        SpinnerNumberModel daysModel =
                new SpinnerNumberModel(10, 1, 3650, 1);

        JSpinner daysSpinner = new JSpinner(daysModel);
        daysSpinner.setFont(new Font(FONT, Font.PLAIN, 13));
        daysSpinner.setPreferredSize(new Dimension(180, 36));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(daysLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(daysSpinner, gbc);

        JLabel dateLabel = new JLabel("Follow-up Date:");
        dateLabel.setFont(new Font(FONT, Font.BOLD, 13));
        dateLabel.setForeground(TEXT);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        SpinnerDateModel dateModel = new SpinnerDateModel(
                tomorrow.getTime(),
                new Date(),
                null,
                Calendar.DAY_OF_MONTH
        );

        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor =
                new JSpinner.DateEditor(dateSpinner, "dd-MM-yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setPreferredSize(new Dimension(180, 36));

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(dateLabel, gbc);

        gbc.gridx = 1;
        form.add(dateSpinner, gbc);

        JLabel timeLabel = new JLabel("Appointment Time:");
        timeLabel.setFont(new Font(FONT, Font.BOLD, 13));
        timeLabel.setForeground(TEXT);

        JComboBox<String> timeCombo = new JComboBox<>();
        styleCombo(timeCombo);
        timeCombo.setPreferredSize(new Dimension(180, 36));
        loadFollowUpTimeSlots(
                timeCombo,
                doctorId,
                getSpinnerLocalDate(dateSpinner)
        );

        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(timeLabel, gbc);

        gbc.gridx = 1;
        form.add(timeCombo, gbc);

        JLabel reasonLabel = new JLabel("Reason:");
        reasonLabel.setFont(new Font(FONT, Font.BOLD, 13));
        reasonLabel.setForeground(TEXT);

        JTextField reasonField = new JTextField("Follow-up consultation");
        styleField(reasonField);
        reasonField.setPreferredSize(new Dimension(180, 36));

        gbc.gridx = 0;
        gbc.gridy = 4;
        form.add(reasonLabel, gbc);

        gbc.gridx = 1;
        form.add(reasonField, gbc);

        JLabel note = new JLabel(
                "Doctor: " + doctorName
                        + "  |  Fee: " + RUPEE + " " + fee
        );
        note.setFont(new Font(FONT, Font.PLAIN, 12));
        note.setForeground(MUTED);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(note, gbc);

        main.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        buttons.setOpaque(false);

        RoundedButton close = new RoundedButton(
                "Close",
                new Color(120, 130, 145),
                new Color(100, 110, 125),
                38
        );
        close.setPreferredSize(new Dimension(100, 38));
        close.setFont(new Font(FONT, Font.BOLD, 13));
        close.addActionListener(e -> dialog.dispose());

        RoundedButton schedule = new RoundedButton(
                "Schedule Follow-up",
                new Color(0, 121, 193),
                new Color(0, 102, 170),
                38
        );
        schedule.setPreferredSize(new Dimension(180, 38));
        schedule.setFont(new Font(FONT, Font.BOLD, 13));

        // When the user changes the follow-up type/date, refresh available times.
        Runnable refreshTimes = () -> {
            LocalDate selectedDate = getSpinnerLocalDate(dateSpinner);
            loadFollowUpTimeSlots(timeCombo, doctorId, selectedDate);
        };

        daysSpinner.addChangeListener(e -> {
            if (afterDays.isSelected()) {
                int days = (Integer) daysSpinner.getValue();
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_MONTH, days);
                dateSpinner.setValue(c.getTime());
            }
        });

        dateSpinner.addChangeListener(e -> {
            refreshTimes.run();
        });

        afterDays.addActionListener(e -> {
            daysSpinner.setEnabled(true);
            dateSpinner.setEnabled(false);
            int days = (Integer) daysSpinner.getValue();
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, days);
            dateSpinner.setValue(c.getTime());
        });

        specificDate.addActionListener(e -> {
            daysSpinner.setEnabled(false);
            dateSpinner.setEnabled(true);
            refreshTimes.run();
        });

        // Initial mode: after days.
        dateSpinner.setEnabled(false);
        int initialDays = (Integer) daysSpinner.getValue();
        Calendar initialDate = Calendar.getInstance();
        initialDate.add(Calendar.DAY_OF_MONTH, initialDays);
        dateSpinner.setValue(initialDate.getTime());
        refreshTimes.run();

        schedule.addActionListener(e -> {

            LocalDate selectedDate = getSpinnerLocalDate(dateSpinner);
            String selectedTime = (String) timeCombo.getSelectedItem();
            String reason = reasonField.getText().trim();

            if (selectedDate == null) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Please select a valid follow-up date.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (selectedDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Follow-up date cannot be in the past.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (selectedTime == null
                    || selectedTime.trim().isEmpty()
                    || "No available slots".equals(selectedTime)) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "No available time slot is selected.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter the follow-up reason.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                reasonField.requestFocus();
                return;
            }

            // Final slot check before inserting.
            if (isFollowUpSlotTaken(
                    doctorId,
                    selectedDate,
                    selectedTime
            )) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "This time slot has just been booked. Please select another time.",
                        "Slot Unavailable",
                        JOptionPane.WARNING_MESSAGE
                );
                refreshTimes.run();
                return;
            }

            int followupDays = 0;

            if (afterDays.isSelected()) {
                followupDays = (Integer) daysSpinner.getValue();
            }

            String insertSql = """
                INSERT INTO appointments
                (
                    parent_appointment_id,
                    appointment_type,
                    followup_days,
                    followup_reason,
                    patient_name,
                    age,
                    phone,
                    gender,
                    doctor_id,
                    appointment_date,
                    appointment_time,
                    fee,
                    status
                )
                VALUES (?, 'FOLLOW_UP', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Booked')
                """;

            try (
                    Connection con = DB.getConnection();
                    PreparedStatement ps = con.prepareStatement(insertSql)
            ) {

                ps.setInt(1, appointmentId);

                if (afterDays.isSelected()) {
                    ps.setInt(2, followupDays);
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }

                ps.setString(3, reason);
                ps.setString(4, patient);
                ps.setInt(5, Integer.parseInt(age));
                ps.setString(6, phone);
                ps.setString(7, gender);
                ps.setInt(8, doctorId);
                ps.setDate(9, java.sql.Date.valueOf(selectedDate));
                ps.setString(10, selectedTime);
                ps.setBigDecimal(11, fee);

                int inserted = ps.executeUpdate();

                if (inserted > 0) {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Re-appointment scheduled successfully.\n"
                                    + "Patient: " + patient + "\n"
                                    + "Date: " + selectedDate.format(DATE_FMT) + "\n"
                                    + "Time: " + selectedTime,
                            "MediBook",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dialog.dispose();
                    refresh();

                    if (onChanged != null) {
                        onChanged.run();
                    }
                }

            } catch (SQLException | NumberFormatException ex) {

                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        dialog,
                        "Unable to schedule re-appointment.\n"
                                + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        buttons.add(close);
        buttons.add(schedule);
        main.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    private LocalDate getSpinnerLocalDate(JSpinner spinner) {

        Date value = (Date) spinner.getValue();

        return value.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    private void loadFollowUpTimeSlots(
            JComboBox<String> combo,
            int doctorId,
            LocalDate date
    ) {

        combo.removeAllItems();

        LocalTime now = LocalTime.now();
        boolean today = date.equals(LocalDate.now());

        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("hh:mm a");

        for (int hour = 9; hour <= 17; hour++) {

            for (int minute : new int[]{0, 30}) {

                if (hour == 17 && minute > 30) {
                    continue;
                }

                if (hour == 13) {
                    continue;
                }

                LocalTime slot = LocalTime.of(hour, minute);

                if (today && !slot.isAfter(now)) {
                    continue;
                }

                String time = slot.format(timeFormatter);

                if (!isFollowUpSlotTaken(doctorId, date, time)) {
                    combo.addItem(time);
                }
            }
        }

        if (combo.getItemCount() == 0) {
            combo.addItem("No available slots");
        }
    }

    private boolean isFollowUpSlotTaken(
            int doctorId,
            LocalDate date,
            String time
    ) {

        String sql = """
            SELECT COUNT(*)
            FROM appointments
            WHERE doctor_id = ?
              AND appointment_date = ?
              AND appointment_time = ?
              AND status = 'Booked'
            """;

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, doctorId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, time);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // PRINT APPOINTMENT
    // =========================================================

    private void printSelected() {

        int row = table.getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select an appointment from the table first.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        // Read the currently selected appointment from the table.
        String appointmentId =
                String.valueOf(model.getValueAt(row, 0));

        String patient =
                String.valueOf(model.getValueAt(row, 1));

        String age =
                String.valueOf(model.getValueAt(row, 2));

        String phone =
                String.valueOf(model.getValueAt(row, 3));

        String doctor =
                String.valueOf(model.getValueAt(row, 4));

        String specialization =
                String.valueOf(model.getValueAt(row, 5));

        String date =
                String.valueOf(model.getValueAt(row, 6));

        String time =
                String.valueOf(model.getValueAt(row, 7));

        String fee =
                String.valueOf(model.getValueAt(row, 8));

        String status =
                String.valueOf(model.getValueAt(row, 9));

        PrinterJob job = PrinterJob.getPrinterJob();

        job.setJobName("MediBook Appointment - #" + appointmentId);

        job.setPrintable(
                new AppointmentPrintable(
                        appointmentId,
                        patient,
                        age,
                        phone,
                        doctor,
                        specialization,
                        date,
                        time,
                        fee,
                        status
                )
        );

        boolean proceed = job.printDialog();

        if (!proceed)
            return;

        try {

            job.print();

            JOptionPane.showMessageDialog(
                    this,
                    "Appointment slip printed successfully.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (PrinterException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to print appointment.\n"
                            + e.getMessage(),
                    "Print Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =========================================================
    // APPOINTMENT PRINTABLE
    // =========================================================

    private static class AppointmentPrintable implements Printable {

        private final String appointmentId;
        private final String patient;
        private final String age;
        private final String phone;
        private final String doctor;
        private final String specialization;
        private final String date;
        private final String time;
        private final String fee;
        private final String status;

        AppointmentPrintable(
                String appointmentId,
                String patient,
                String age,
                String phone,
                String doctor,
                String specialization,
                String date,
                String time,
                String fee,
                String status
        ) {

            this.appointmentId = appointmentId;
            this.patient = patient;
            this.age = age;
            this.phone = phone;
            this.doctor = doctor;
            this.specialization = specialization;
            this.date = date;
            this.time = time;
            this.fee = fee;
            this.status = status;
        }

        @Override
        public int print(
                Graphics graphics,
                PageFormat pageFormat,
                int pageIndex
        ) throws PrinterException {

            if (pageIndex > 0)
                return NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;

            g2.translate(
                    pageFormat.getImageableX(),
                    pageFormat.getImageableY()
            );

            double printableWidth =
                    pageFormat.getImageableWidth();

            int x = 20;
            int y = 25;

            // Header
            g2.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            24
                    )
            );

            String title = "MEDIBOOK";

            FontMetrics titleMetrics = g2.getFontMetrics();

            int titleX =
                    (int) ((printableWidth
                            - titleMetrics.stringWidth(title)) / 2);

            g2.drawString(title, titleX, y);

            y += 22;

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            12
                    )
            );

            String subtitle =
                    "Doctor Appointment Slip";

            FontMetrics subtitleMetrics = g2.getFontMetrics();

            int subtitleX =
                    (int) ((printableWidth
                            - subtitleMetrics.stringWidth(subtitle)) / 2);

            g2.drawString(subtitle, subtitleX, y);

            y += 18;

            g2.drawLine(
                    x,
                    y,
                    (int) printableWidth - x,
                    y
            );

            y += 28;

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            13
                    )
            );

            g2.drawString(
                    "Appointment ID: #" + appointmentId,
                    x,
                    y
            );

            y += 30;

            int labelWidth = 125;

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            11
                    )
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Patient Name",
                    patient
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Age",
                    age
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Phone",
                    phone
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Doctor",
                    doctor
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Specialization",
                    specialization
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Date",
                    date
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Time",
                    time
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Consultation Fee",
                    fee
            );

            y = drawPrintRow(
                    g2,
                    x,
                    y,
                    labelWidth,
                    "Status",
                    status
            );

            y += 15;

            g2.drawLine(
                    x,
                    y,
                    (int) printableWidth - x,
                    y
            );

            y += 28;

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            11
                    )
            );

            g2.drawString(
                    "Please arrive 10 minutes before your appointment time.",
                    x,
                    y
            );

            y += 20;

            g2.drawString(
                    "Thank you for choosing MediBook.",
                    x,
                    y
            );

            return PAGE_EXISTS;
        }

        private int drawPrintRow(
                Graphics2D g2,
                int x,
                int y,
                int labelWidth,
                String label,
                String value
        ) {

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            11
                    )
            );

            g2.drawString(
                    label + ":",
                    x,
                    y
            );

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            11
                    )
            );

            g2.drawString(
                    value,
                    x + labelWidth,
                    y
            );

            return y + 23;
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