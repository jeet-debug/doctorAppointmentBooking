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
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static appointment.UI.*;

/**
 * Appointments page.
 *
 * Features:
 * - Search + specialization + status filters
 * - Complete / Cancel / Re-Appointment / Print
 * - Total Fee + Paid + Due + Payment Status
 * - Collect remaining due payment (custom amount supported)
 * - Payment history is stored in the payments table
 * - Re-appointment uses doctor's saved available_days + available_time
 */
public class AppointmentsPanel extends JPanel {

    private static final int MAX_PER_SLOT = 2;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static final Color PARTIAL_ORANGE = new Color(234, 138, 20);
    private static final Color COMPLETED_BLUE = new Color(0, 123, 255);

    private DefaultTableModel model;
    private JTable table;
    private JTextField tfSearch;
    private JComboBox<String> cbFilterSpec;
    private JComboBox<String> cbFilterStatus;

    private JSpinner spFilterDate;
    private boolean dateFilterActive = false;

    private JLabel countLabel;

    /** Called after a change so Book Appointment page can refresh slots. */
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

    // =========================================================
    // REFRESH / LOAD DATA
    // =========================================================

    public void refresh() {
        if (model == null) {
            return;
        }

        String search = tfSearch.getText().trim().toLowerCase(Locale.ENGLISH);
        String spec = String.valueOf(cbFilterSpec.getSelectedItem());
        String status = String.valueOf(cbFilterStatus.getSelectedItem());
        LocalDate filterDate = dateFilterActive && spFilterDate != null
                ? getSpinnerLocalDate(spFilterDate)
                : null;

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
                    a.status,
                    COALESCE(SUM(p.amount), 0) AS paid_amount
                FROM appointments a
                INNER JOIN doctors d ON a.doctor_id = d.id
                LEFT JOIN payments p ON a.id = p.appointment_id
                WHERE 1 = 1
                """);

        if (!"All Specializations".equals(spec)) {
            sql.append(" AND d.specialization = ?");
        }
        if (!"All Status".equals(status)) {
            sql.append(" AND a.status = ?");
        }
        if (filterDate != null) {
            sql.append(" AND a.appointment_date = ?");
        }

        sql.append(" GROUP BY a.id, a.patient_name, a.age, a.phone, a.gender, "
                + "d.name, d.specialization, a.appointment_date, "
                + "a.appointment_time, a.fee, a.status ORDER BY a.id DESC");

        int shown = 0;
        int total = 0;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int index = 1;

            if (!"All Specializations".equals(spec)) {
                ps.setString(index++, spec);
            }
            if (!"All Status".equals(status)) {
                ps.setString(index++, status);
            }
            if (filterDate != null) {
                ps.setDate(index++, java.sql.Date.valueOf(filterDate));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String patient = rs.getString("patient_name");
                    int age = rs.getInt("age");
                    String phone = rs.getString("phone");
                    String doctor = rs.getString("doctor_name");
                    String specialization = rs.getString("specialization");

                    java.sql.Date sqlDate = rs.getDate("appointment_date");
                    String date = sqlDate == null
                            ? "-"
                            : sqlDate.toLocalDate().format(DATE_FMT);

                    String time = rs.getString("appointment_time");
                    java.math.BigDecimal feeBd = rs.getBigDecimal("fee");
                    java.math.BigDecimal paidBd = rs.getBigDecimal("paid_amount");

                    double fee = feeBd == null ? 0.0 : feeBd.doubleValue();
                    double paid = paidBd == null ? 0.0 : paidBd.doubleValue();
                    double due = Math.max(0.0, fee - paid);

                    String feeText = RUPEE + " " + money(fee);
                    String paidText = RUPEE + " " + money(paid);
                    String dueText = RUPEE + " " + money(due);
                    String paymentStatus = paymentStatus(fee, paid);
                    String appointmentStatus = rs.getString("status");

                    String searchableText = (
                            id + " " + patient + " " + phone + " "
                                    + doctor + " " + specialization + " " + date + " " + time
                    ).toLowerCase(Locale.ENGLISH);

                    if (!search.isEmpty() && !searchableText.contains(search)) {
                        continue;
                    }

                    model.addRow(new Object[]{
                            id,
                            patient,
                            age,
                            phone,
                            doctor,
                            specialization,
                            date,
                            time,
                            feeText,
                            paidText,
                            dueText,
                            paymentStatus,
                            appointmentStatus
                    });

                    shown++;
                }
            }

            StringBuilder countSql = new StringBuilder(
                    "SELECT COUNT(*) FROM appointments a "
                            + "INNER JOIN doctors d ON a.doctor_id = d.id WHERE 1=1"
            );

            if (!"All Specializations".equals(spec)) {
                countSql.append(" AND d.specialization = ?");
            }
            if (!"All Status".equals(status)) {
                countSql.append(" AND a.status = ?");
            }
            if (filterDate != null) {
                countSql.append(" AND a.appointment_date = ?");
            }

            try (PreparedStatement countPs = con.prepareStatement(countSql.toString())) {
                int i = 1;
                if (!"All Specializations".equals(spec)) {
                    countPs.setString(i++, spec);
                }
                if (!"All Status".equals(status)) {
                    countPs.setString(i++, status);
                }
                if (filterDate != null) {
                    countPs.setDate(i++, java.sql.Date.valueOf(filterDate));
                }

                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        total = countRs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to load appointments.", e);
        }

        countLabel.setText("Showing " + shown + " of " + total + " appointments");
    }

    // =========================================================
    // UI
    // =========================================================

    private JComponent buildPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(0, 28, 20, 28));

        Card card = new Card(18, 20, 18, 20);
        card.setLayout(new BorderLayout(0, 12));

        // ---------- toolbar ----------
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font(FONT, Font.BOLD, 13));
        searchLabel.setForeground(MUTED);

        tfSearch = new JTextField();
        styleField(tfSearch);
        tfSearch.setPreferredSize(new Dimension(255, 40));
        tfSearch.setToolTipText("Search by patient, phone, doctor, ID, date or time");

        cbFilterSpec = new JComboBox<>();
        cbFilterSpec.addItem("All Specializations");
        styleCombo(cbFilterSpec);
        cbFilterSpec.setPreferredSize(new Dimension(205, 40));

        cbFilterStatus = new JComboBox<>(new String[]{
                "All Status", "Booked", "Completed", "Cancelled"
        });
        styleCombo(cbFilterStatus);
        cbFilterStatus.setPreferredSize(new Dimension(145, 40));

        // =====================================================
        // DATE FILTER
        // =====================================================

        JLabel dateFilterLabel = new JLabel("Date:");
        dateFilterLabel.setFont(new Font(FONT, Font.BOLD, 13));
        dateFilterLabel.setForeground(MUTED);

        spFilterDate = createFilterDateSpinner();
        spFilterDate.setPreferredSize(new Dimension(120, 40));

        RoundedButton applyDate = new RoundedButton(
                "Apply Date",
                new Color(0, 121, 193),
                new Color(0, 102, 170),
                40
        );
        applyDate.setFont(new Font(FONT, Font.BOLD, 12));
        applyDate.setPreferredSize(new Dimension(105, 40));
        applyDate.addActionListener(e -> {
            dateFilterActive = true;
            refresh();
        });

        filterRow.add(searchLabel);
        filterRow.add(tfSearch);
        filterRow.add(cbFilterSpec);
        filterRow.add(cbFilterStatus);
        filterRow.add(dateFilterLabel);
        filterRow.add(spFilterDate);
        filterRow.add(applyDate);

        JPanel filterWrap = new JPanel(new BorderLayout());
        filterWrap.setOpaque(false);
        filterWrap.add(filterRow, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        RoundedButton complete = new RoundedButton(
                "Complete Appointment", GREEN, GREEN, 40);
        complete.setFont(new Font(FONT, Font.BOLD, 13));
        complete.setPreferredSize(new Dimension(180, 40));
        complete.addActionListener(e -> completeSelected());

        RoundedButton collect = new RoundedButton(
                "Collect Due", PARTIAL_ORANGE, new Color(210, 118, 12), 40);
        collect.setFont(new Font(FONT, Font.BOLD, 13));
        collect.setPreferredSize(new Dimension(115, 40));
        collect.addActionListener(e -> collectDuePayment());

        RoundedButton reAppointment = new RoundedButton(
                "Re-Appointment", new Color(0, 150, 136), new Color(0, 125, 113), 40);
        reAppointment.setFont(new Font(FONT, Font.BOLD, 13));
        reAppointment.setPreferredSize(new Dimension(145, 40));
        reAppointment.addActionListener(e -> scheduleReAppointment());

        RoundedButton print = new RoundedButton(
                "Print Appointment", new Color(0, 121, 193), new Color(0, 102, 170), 40);
        print.setFont(new Font(FONT, Font.BOLD, 13));
        print.setPreferredSize(new Dimension(160, 40));
        print.addActionListener(e -> printSelected());

        RoundedButton cancel = new RoundedButton(
                "Cancel Appointment", RED, RED_HOVER, 40);
        cancel.setFont(new Font(FONT, Font.BOLD, 13));
        cancel.setPreferredSize(new Dimension(175, 40));
        cancel.addActionListener(e -> cancelSelected());

        RoundedButton clearFilters = new RoundedButton(
                "Clear Filters",
                new Color(226, 232, 240),
                new Color(210, 219, 230),
                40);
        clearFilters.setForeground(TEXT);
        clearFilters.setFont(new Font(FONT, Font.BOLD, 13));
        clearFilters.setPreferredSize(new Dimension(115, 40));
        clearFilters.addActionListener(e -> clearFilters());

        actions.add(complete);
        actions.add(collect);
        actions.add(reAppointment);
        actions.add(print);
        actions.add(cancel);
        actions.add(clearFilters);

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        filterWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        toolbar.add(filterWrap);
        toolbar.add(Box.createVerticalStrut(8));
        toolbar.add(actions);

        card.add(toolbar, BorderLayout.NORTH);

        // ---------- table ----------
        String[] cols = {
                "ID", "Patient", "Age", "Phone", "Doctor", "Specialization",
                "Date", "Time", "Fee", "Paid", "Due", "Payment", "Status"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        table.setSelectionBackground(new Color(214, 232, 252));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);

        int[] widths = {55, 125, 50, 105, 125, 125, 90, 90, 90, 90, 90, 90, 95};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected, boolean focused,
                    int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        t, value, selected, false, row, column);

                label.setBorder(new EmptyBorder(0, 10, 0, 8));
                label.setFont(new Font(FONT, Font.PLAIN, 13));

                if (!selected) {
                    label.setBackground(row % 2 == 0
                            ? Color.WHITE
                            : new Color(249, 251, 254));
                    label.setForeground(TEXT);
                }

                // Payment column = 11
                if (column == 11 && !selected) {
                    String valueText = String.valueOf(value);
                    label.setFont(new Font(FONT, Font.BOLD, 12));
                    if ("Paid".equals(valueText)) {
                        label.setForeground(GREEN);
                    } else if ("Partial".equals(valueText)) {
                        label.setForeground(PARTIAL_ORANGE);
                    } else {
                        label.setForeground(RED);
                    }
                }

                // Appointment status column = 12
                if (column == 12 && !selected) {
                    String valueText = String.valueOf(value);
                    label.setFont(new Font(FONT, Font.BOLD, 12));
                    if ("Booked".equals(valueText)) {
                        label.setForeground(GREEN);
                    } else if ("Completed".equals(valueText)) {
                        label.setForeground(COMPLETED_BLUE);
                    } else {
                        label.setForeground(RED);
                    }
                }

                return label;
            }
        });

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected, boolean focused,
                    int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        t, value, false, false, row, column);
                label.setOpaque(true);
                label.setBackground(new Color(240, 245, 251));
                label.setForeground(MUTED);
                label.setFont(new Font(FONT, Font.BOLD, 12));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                        new EmptyBorder(0, 10, 0, 8)
                ));
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        card.add(scrollPane, BorderLayout.CENTER);

        countLabel = new JLabel(" ");
        countLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        countLabel.setForeground(MUTED);
        card.add(countLabel, BorderLayout.SOUTH);

        // ---------- listeners ----------
        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        cbFilterSpec.addActionListener(e -> refresh());
        cbFilterStatus.addActionListener(e -> refresh());

        // Date spinner only changes the selected date.
        // The database filter is applied only after clicking "Apply Date".

        page.add(card, BorderLayout.CENTER);

        loadSpecializations();
        return page;
    }

    // =========================================================
    // SPECIALIZATION FILTER
    // =========================================================

    private void loadSpecializations() {
        if (cbFilterSpec == null) {
            return;
        }

        String sql = """
                SELECT DISTINCT specialization
                FROM doctors
                WHERE status = 'Active'
                ORDER BY specialization
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cbFilterSpec.addItem(rs.getString("specialization"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to load specializations.", e);
        }
    }

    // =========================================================
    // PAYMENT HELPERS
    // =========================================================

    private static String money(double value) {
        return String.format(Locale.ENGLISH, "%.2f", Math.max(0.0, value));
    }

    private static double parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String paymentStatus(double fee, double paid) {
        if (fee <= 0.0) {
            return "No Fee";
        }
        if (paid <= 0.0) {
            return "Pending";
        }
        if (paid + 0.005 < fee) {
            return "Partial";
        }
        return "Paid";
    }

    /** Collect a later payment. Patient can pay any amount up to current due. */
    private void collectDuePayment() {
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

        int modelRow = table.convertRowIndexToModel(row);
        int appointmentId = Integer.parseInt(String.valueOf(model.getValueAt(modelRow, 0)));
        String patient = String.valueOf(model.getValueAt(modelRow, 1));
        String appointmentStatus = String.valueOf(model.getValueAt(modelRow, 12));

        if ("Cancelled".equals(appointmentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Payment cannot be collected for a cancelled appointment.",
                    "MediBook",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        double fee = 0.0;
        double paid = 0.0;

        String sql = """
                SELECT a.fee, COALESCE(SUM(p.amount), 0) AS paid_amount
                FROM appointments a
                LEFT JOIN payments p ON a.id = p.appointment_id
                WHERE a.id = ?
                GROUP BY a.id, a.fee
                """;

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
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

                java.math.BigDecimal feeBd = rs.getBigDecimal("fee");
                java.math.BigDecimal paidBd = rs.getBigDecimal("paid_amount");
                fee = feeBd == null ? 0.0 : feeBd.doubleValue();
                paid = paidBd == null ? 0.0 : paidBd.doubleValue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to load payment details.", e);
            return;
        }

        double due = Math.max(0.0, fee - paid);

        if (due <= 0.005) {
            JOptionPane.showMessageDialog(
                    this,
                    "This appointment is already fully paid.\n"
                            + "Total Fee: " + RUPEE + " " + money(fee),
                    "Payment Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Collect Due Payment",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setSize(470, 350);
        dialog.setLocationRelativeTo(this);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Collect Due Payment");
        title.setFont(new Font(FONT, Font.BOLD, 20));
        title.setForeground(TEXT);

        JLabel info = new JLabel(
                "Patient: " + patient + "  |  Appointment #" + appointmentId
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
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 5, 7, 5);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel totalLabel = new JLabel("Total Fee:");
        JLabel paidLabel = new JLabel("Already Paid:");
        JLabel dueLabel = new JLabel("Current Due:");
        JLabel amountLabel = new JLabel("Payment Amount:");
        JLabel methodLabel = new JLabel("Payment Method:");

        JLabel totalValue = new JLabel(RUPEE + " " + money(fee));
        JLabel paidValue = new JLabel(RUPEE + " " + money(paid));
        JLabel dueValue = new JLabel(RUPEE + " " + money(due));

        for (JLabel l : new JLabel[]{totalLabel, paidLabel, dueLabel, amountLabel, methodLabel}) {
            l.setFont(new Font(FONT, Font.BOLD, 13));
            l.setForeground(TEXT);
        }
        dueValue.setFont(new Font(FONT, Font.BOLD, 14));
        dueValue.setForeground(PARTIAL_ORANGE);

        JTextField amountField = new JTextField(money(due));
        styleField(amountField);
        amountField.selectAll();
        amountField.setPreferredSize(new Dimension(190, 38));
        installMoneyFilter(amountField, 10);

        JComboBox<String> methodCombo = new JComboBox<>(
                new String[]{"Cash", "UPI", "Card", "Online"}
        );
        styleCombo(methodCombo);
        methodCombo.setPreferredSize(new Dimension(190, 38));

        addDialogRow(form, g, 0, totalLabel, totalValue);
        addDialogRow(form, g, 1, paidLabel, paidValue);
        addDialogRow(form, g, 2, dueLabel, dueValue);
        addDialogRow(form, g, 3, amountLabel, amountField);
        addDialogRow(form, g, 4, methodLabel, methodCombo);

        main.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        RoundedButton close = new RoundedButton(
                "Close", new Color(120, 130, 145), new Color(100, 110, 125), 38);
        close.setPreferredSize(new Dimension(95, 38));
        close.setFont(new Font(FONT, Font.BOLD, 13));
        close.addActionListener(e -> dialog.dispose());

        RoundedButton save = new RoundedButton(
                "Save Payment", GREEN, GREEN, 38);
        save.setPreferredSize(new Dimension(135, 38));
        save.setFont(new Font(FONT, Font.BOLD, 13));

        save.addActionListener(e -> {
            double amount = parseMoney(amountField.getText());
            String method = String.valueOf(methodCombo.getSelectedItem());

            if (amount <= 0.0) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Please enter a payment amount greater than 0.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                amountField.requestFocus();
                return;
            }

            if (amount > due + 0.005) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Payment amount cannot be greater than current due.\n"
                                + "Current Due: " + RUPEE + " " + money(due),
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                amountField.requestFocus();
                return;
            }

            String insertSql = """
                    INSERT INTO payments
                    (appointment_id, amount, payment_method, payment_status, payment_note)
                    VALUES (?, ?, ?, 'Paid', ?)
                    """;

            try (Connection con = DB.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // Lock appointment row and re-check due before insert.
                    double freshDue;
                    try (PreparedStatement lock = con.prepareStatement(
                            "SELECT fee FROM appointments WHERE id = ? FOR UPDATE")) {
                        lock.setInt(1, appointmentId);
                        try (ResultSet rs = lock.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException("Appointment not found.");
                            }
                            java.math.BigDecimal feeBd = rs.getBigDecimal("fee");
                            double freshFee = feeBd == null ? 0.0 : feeBd.doubleValue();

                            try (PreparedStatement sumPs = con.prepareStatement(
                                    "SELECT COALESCE(SUM(amount),0) FROM payments WHERE appointment_id = ?")) {
                                sumPs.setInt(1, appointmentId);
                                try (ResultSet sumRs = sumPs.executeQuery()) {
                                    double freshPaid = 0.0;
                                    if (sumRs.next()) {
                                        java.math.BigDecimal p = sumRs.getBigDecimal(1);
                                        freshPaid = p == null ? 0.0 : p.doubleValue();
                                    }
                                    freshDue = Math.max(0.0, freshFee - freshPaid);
                                }
                            }
                        }
                    }

                    if (amount > freshDue + 0.005) {
                        con.rollback();
                        JOptionPane.showMessageDialog(
                                dialog,
                                "The due amount changed. Please enter an amount up to "
                                        + RUPEE + " " + money(freshDue),
                                "Payment Changed",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                        ps.setInt(1, appointmentId);
                        ps.setDouble(2, amount);
                        ps.setString(3, method);
                        ps.setString(
                                4,
                                amount + 0.005 >= freshDue
                                        ? "Final payment after consultation"
                                        : "Additional payment after booking"
                        );
                        ps.executeUpdate();
                    }

                    con.commit();

                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                } finally {
                    con.setAutoCommit(true);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                showDatabaseError("Unable to save payment.", ex);
                return;
            }

            JOptionPane.showMessageDialog(
                    dialog,
                    "Payment saved successfully.\n"
                            + "Amount: " + RUPEE + " " + money(amount)
                            + "\nMethod: " + method,
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dialog.dispose();
            refresh();
            notifyChanged();
        });

        buttons.add(close);
        buttons.add(save);
        main.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    private void addDialogRow(
            JPanel panel,
            GridBagConstraints g,
            int row,
            JLabel label,
            Component component) {

        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0;
        panel.add(label, g);

        g.gridx = 1;
        g.weightx = 1;
        panel.add(component, g);
    }

    // =========================================================
    // COMPLETE APPOINTMENT
    // =========================================================

    private void completeSelected() {
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

        int modelRow = table.convertRowIndexToModel(row);
        int id = getInt(modelRow, 0);
        String patient = String.valueOf(model.getValueAt(modelRow, 1));
        String currentStatus = String.valueOf(model.getValueAt(modelRow, 12));

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

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Mark appointment #" + id + " for " + patient + " as completed?",
                "Complete Appointment",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE appointments SET status = 'Completed' WHERE id = ?";

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Appointment completed successfully.",
                        "MediBook",
                        JOptionPane.INFORMATION_MESSAGE
                );
                refresh();
                notifyChanged();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to complete appointment.", e);
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

        int modelRow = table.convertRowIndexToModel(row);
        int appointmentId = getInt(modelRow, 0);
        String currentStatus = String.valueOf(model.getValueAt(modelRow, 12));

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

        String patient = String.valueOf(model.getValueAt(modelRow, 1));
        int ageFromTable = getInt(modelRow, 2);
        String phone = String.valueOf(model.getValueAt(modelRow, 3));
        String doctorName = String.valueOf(model.getValueAt(modelRow, 4));
        String specialization = String.valueOf(model.getValueAt(modelRow, 5));

        int doctorId;
        String gender;
        java.math.BigDecimal fee;

        String patientSql = """
                SELECT patient_name, age, phone, gender, doctor_id, fee
                FROM appointments
                WHERE id = ?
                """;

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(patientSql)) {
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
                if (ageFromTable <= 0) {
                    ageFromTable = rs.getInt("age");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to load appointment details.", e);
            return;
        }

        final int patientAge = ageFromTable;

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Schedule Re-Appointment",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(560, 590);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBorder(new EmptyBorder(20, 22, 20, 22));
        main.setBackground(Color.WHITE);

        JLabel title = new JLabel("Schedule Re-Appointment");
        title.setFont(new Font(FONT, Font.BOLD, 20));
        title.setForeground(TEXT);

        JLabel info = new JLabel(
                "Patient: " + patient + "  |  Doctor: " + doctorName + "  |  " + specialization
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
        styleDialogLabel(typeLabel);

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

        addDialogGridRow(form, gbc, 0, typeLabel, typePanel);

        JLabel daysLabel = new JLabel("Number of Days:");
        styleDialogLabel(daysLabel);
        SpinnerNumberModel daysModel = new SpinnerNumberModel(10, 1, 3650, 1);
        JSpinner daysSpinner = new JSpinner(daysModel);
        daysSpinner.setFont(new Font(FONT, Font.PLAIN, 13));
        daysSpinner.setPreferredSize(new Dimension(190, 36));
        addDialogGridRow(form, gbc, 1, daysLabel, daysSpinner);

        JLabel dateLabel = new JLabel("Follow-up Date:");
        styleDialogLabel(dateLabel);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        SpinnerDateModel dateModel = new SpinnerDateModel(
                tomorrow.getTime(), new Date(), null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd-MM-yyyy"));
        dateSpinner.setPreferredSize(new Dimension(190, 36));
        addDialogGridRow(form, gbc, 2, dateLabel, dateSpinner);

        JLabel timeLabel = new JLabel("Appointment Time:");
        styleDialogLabel(timeLabel);
        JComboBox<String> timeCombo = new JComboBox<>();
        styleCombo(timeCombo);
        timeCombo.setPreferredSize(new Dimension(190, 36));
        addDialogGridRow(form, gbc, 3, timeLabel, timeCombo);

        JLabel reasonLabel = new JLabel("Reason:");
        styleDialogLabel(reasonLabel);
        JTextField reasonField = new JTextField("Follow-up consultation");
        styleField(reasonField);
        reasonField.setPreferredSize(new Dimension(190, 36));
        addDialogGridRow(form, gbc, 4, reasonLabel, reasonField);

        JLabel note = new JLabel(
                "Doctor: " + doctorName + "  |  Fee: " + RUPEE + " "
                        + (fee == null ? "0.00" : fee.toPlainString())
                        + "  |  New appointment starts with due balance."
        );
        note.setFont(new Font(FONT, Font.PLAIN, 12));
        note.setForeground(MUTED);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(note, gbc);

        main.add(form, BorderLayout.CENTER);

        Runnable refreshTimes = () -> loadFollowUpTimeSlots(
                timeCombo,
                doctorId,
                getSpinnerLocalDate(dateSpinner)
        );

        // Initial mode: after days.
        dateSpinner.setEnabled(false);
        setDateAfterDays(dateSpinner, (Integer) daysSpinner.getValue());
        refreshTimes.run();

        daysSpinner.addChangeListener(e -> {
            if (afterDays.isSelected()) {
                setDateAfterDays(dateSpinner, (Integer) daysSpinner.getValue());
            }
        });

        dateSpinner.addChangeListener(e -> refreshTimes.run());

        afterDays.addActionListener(e -> {
            daysSpinner.setEnabled(true);
            dateSpinner.setEnabled(false);
            setDateAfterDays(dateSpinner, (Integer) daysSpinner.getValue());
        });

        specificDate.addActionListener(e -> {
            daysSpinner.setEnabled(false);
            dateSpinner.setEnabled(true);
            refreshTimes.run();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        RoundedButton close = new RoundedButton(
                "Close", new Color(120, 130, 145), new Color(100, 110, 125), 38);
        close.setPreferredSize(new Dimension(100, 38));
        close.setFont(new Font(FONT, Font.BOLD, 13));
        close.addActionListener(e -> dialog.dispose());

        RoundedButton schedule = new RoundedButton(
                "Schedule Follow-up", new Color(0, 121, 193), new Color(0, 102, 170), 38);
        schedule.setPreferredSize(new Dimension(180, 38));
        schedule.setFont(new Font(FONT, Font.BOLD, 13));

        schedule.addActionListener(e -> {
            LocalDate selectedDate = getSpinnerLocalDate(dateSpinner);
            String selectedTime = String.valueOf(timeCombo.getSelectedItem());
            String reason = reasonField.getText().trim();

            if (selectedDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Follow-up date cannot be in the past.",
                        "MediBook",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (selectedTime.trim().isEmpty() || "No available slots".equals(selectedTime)) {
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

            int followupDays = afterDays.isSelected()
                    ? (Integer) daysSpinner.getValue()
                    : 0;

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

            try (Connection con = DB.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // Lock doctor row so two follow-ups cannot overbook the same slot.
                    try (PreparedStatement lock = con.prepareStatement(
                            "SELECT id FROM doctors WHERE id = ? FOR UPDATE")) {
                        lock.setInt(1, doctorId);
                        try (ResultSet ignored = lock.executeQuery()) {
                            // lock acquired
                        }
                    }

                    if (bookedSlotCount(con, doctorId, selectedDate, selectedTime) >= MAX_PER_SLOT) {
                        con.rollback();
                        JOptionPane.showMessageDialog(
                                dialog,
                                "This time slot is now full. Please choose another time.",
                                "Slot Unavailable",
                                JOptionPane.WARNING_MESSAGE
                        );
                        refreshTimes.run();
                        return;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            insertSql, Statement.RETURN_GENERATED_KEYS)) {

                        ps.setInt(1, appointmentId);
                        if (afterDays.isSelected()) {
                            ps.setInt(2, followupDays);
                        } else {
                            ps.setNull(2, java.sql.Types.INTEGER);
                        }
                        ps.setString(3, reason);
                        ps.setString(4, patient);
                        ps.setInt(5, patientAge);
                        ps.setString(6, phone);
                        ps.setString(7, gender);
                        ps.setInt(8, doctorId);
                        ps.setDate(9, java.sql.Date.valueOf(selectedDate));
                        ps.setString(10, selectedTime);
                        ps.setBigDecimal(11, fee == null ? java.math.BigDecimal.ZERO : fee);
                        ps.executeUpdate();
                    }

                    con.commit();

                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                } finally {
                    con.setAutoCommit(true);
                }

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
                notifyChanged();

            } catch (SQLException ex) {
                ex.printStackTrace();
                showDatabaseErrorTo(dialog, "Unable to schedule re-appointment.", ex);
            }
        });

        buttons.add(close);
        buttons.add(schedule);
        main.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    private static void styleDialogLabel(JLabel label) {
        label.setFont(new Font(FONT, Font.BOLD, 13));
        label.setForeground(TEXT);
    }

    private static void addDialogGridRow(
            JPanel form,
            GridBagConstraints gbc,
            int row,
            Component left,
            Component right) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(left, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(right, gbc);
    }

    private static void setDateAfterDays(JSpinner dateSpinner, int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        dateSpinner.setValue(c.getTime());
    }

    private static JSpinner createFilterDateSpinner() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        SpinnerDateModel model = new SpinnerDateModel(
                c.getTime(),
                null,
                null,
                Calendar.DAY_OF_MONTH
        );

        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd-MM-yyyy"));
        spinner.setFont(new Font(FONT, Font.PLAIN, 13));
        return spinner;
    }

    private void clearFilters() {
        if (tfSearch != null) {
            tfSearch.setText("");
        }
        if (cbFilterSpec != null && cbFilterSpec.getItemCount() > 0) {
            cbFilterSpec.setSelectedIndex(0);
        }
        if (cbFilterStatus != null && cbFilterStatus.getItemCount() > 0) {
            cbFilterStatus.setSelectedIndex(0);
        }
        if (spFilterDate != null) {
            spFilterDate.setValue(new Date());
        }
        dateFilterActive = false;
        refresh();
    }

    private LocalDate getSpinnerLocalDate(JSpinner spinner) {
        Date value = (Date) spinner.getValue();
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // =========================================================
    // FOLLOW-UP AVAILABLE SLOTS
    // =========================================================

    private void loadFollowUpTimeSlots(
            JComboBox<String> combo,
            int doctorId,
            LocalDate date) {

        combo.removeAllItems();

        if (date == null) {
            combo.addItem("No available slots");
            return;
        }

        String[] availability = getDoctorAvailability(doctorId);
        String availableDays = availability[0];
        String availableTime = availability[1];

        if (!isDoctorAvailableOnDay(availableDays, date)) {
            combo.addItem("No available slots");
            return;
        }

        if (availableTime.isBlank()) {
            combo.addItem("No available slots");
            return;
        }

        String[] parts = availableTime.split("\\s+-\\s+", 2);
        if (parts.length != 2) {
            combo.addItem("No available slots");
            return;
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(parts[0].trim(), TIME_FMT);
            end = LocalTime.parse(parts[1].trim(), TIME_FMT);
        } catch (Exception ex) {
            combo.addItem("No available slots");
            return;
        }

        if (!start.isBefore(end)) {
            combo.addItem("No available slots");
            return;
        }

        Map<String, Integer> bookedCounts = loadBookedCounts(doctorId, date);
        LocalTime now = LocalTime.now();
        boolean today = date.equals(LocalDate.now());

        for (LocalTime slot = start; slot.isBefore(end); slot = slot.plusMinutes(30)) {
            if (today && !slot.isAfter(now)) {
                continue;
            }

            String label = slot.format(TIME_FMT);
            int booked = bookedCounts.getOrDefault(label, 0);

            if (booked < MAX_PER_SLOT) {
                combo.addItem(label);
            }
        }

        if (combo.getItemCount() == 0) {
            combo.addItem("No available slots");
        }
    }

    private Map<String, Integer> loadBookedCounts(int doctorId, LocalDate date) {
        Map<String, Integer> counts = new HashMap<>();

        String sql = """
                SELECT appointment_time, COUNT(*) AS cnt
                FROM appointments
                WHERE doctor_id = ?
                  AND appointment_date = ?
                  AND status = 'Booked'
                GROUP BY appointment_time
                """;

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("appointment_time"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return counts;
    }

    private int bookedSlotCount(
            Connection con,
            int doctorId,
            LocalDate date,
            String time) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM appointments
                WHERE doctor_id = ?
                  AND appointment_date = ?
                  AND appointment_time = ?
                  AND status = 'Booked'
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, time);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String[] getDoctorAvailability(int doctorId) {
        String sql = "SELECT available_days, available_time FROM doctors WHERE id = ?";

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, doctorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String days = rs.getString("available_days");
                    String time = rs.getString("available_time");
                    return new String[]{
                            days == null ? "" : days.trim(),
                            time == null ? "" : time.trim()
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new String[]{"", ""};
    }

    private boolean isDoctorAvailableOnDay(String availableDays, LocalDate date) {
        if (availableDays == null || availableDays.isBlank()) {
            return false;
        }

        String selectedDay = date.format(
                DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH));

        for (String day : availableDays.split(",")) {
            if (day.trim().equalsIgnoreCase(selectedDay)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // PRINT
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

        int modelRow = table.convertRowIndexToModel(row);

        String appointmentId = String.valueOf(model.getValueAt(modelRow, 0));
        String patient = String.valueOf(model.getValueAt(modelRow, 1));
        String age = String.valueOf(model.getValueAt(modelRow, 2));
        String phone = String.valueOf(model.getValueAt(modelRow, 3));
        String doctor = String.valueOf(model.getValueAt(modelRow, 4));
        String specialization = String.valueOf(model.getValueAt(modelRow, 5));
        String date = String.valueOf(model.getValueAt(modelRow, 6));
        String time = String.valueOf(model.getValueAt(modelRow, 7));
        String fee = String.valueOf(model.getValueAt(modelRow, 8));
        String paid = String.valueOf(model.getValueAt(modelRow, 9));
        String due = String.valueOf(model.getValueAt(modelRow, 10));
        String paymentStatus = String.valueOf(model.getValueAt(modelRow, 11));
        String status = String.valueOf(model.getValueAt(modelRow, 12));

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("MediBook Appointment - #" + appointmentId);
        job.setPrintable(new AppointmentPrintable(
                appointmentId, patient, age, phone, doctor, specialization,
                date, time, fee, paid, due, paymentStatus, status
        ));

        if (!job.printDialog()) {
            return;
        }

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
                    "Unable to print appointment.\n" + e.getMessage(),
                    "Print Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

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
        private final String paid;
        private final String due;
        private final String paymentStatus;
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
                String paid,
                String due,
                String paymentStatus,
                String status) {

            this.appointmentId = appointmentId;
            this.patient = patient;
            this.age = age;
            this.phone = phone;
            this.doctor = doctor;
            this.specialization = specialization;
            this.date = date;
            this.time = time;
            this.fee = fee;
            this.paid = paid;
            this.due = due;
            this.paymentStatus = paymentStatus;
            this.status = status;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                throws PrinterException {

            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            double printableWidth = pageFormat.getImageableWidth();
            int x = 20;
            int y = 25;

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            String title = "MEDIBOOK";
            FontMetrics tm = g2.getFontMetrics();
            int titleX = (int) ((printableWidth - tm.stringWidth(title)) / 2);
            g2.drawString(title, titleX, y);

            y += 22;
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            String subtitle = "Doctor Appointment Slip";
            FontMetrics sm = g2.getFontMetrics();
            int subtitleX = (int) ((printableWidth - sm.stringWidth(subtitle)) / 2);
            g2.drawString(subtitle, subtitleX, y);

            y += 18;
            g2.drawLine(x, y, (int) printableWidth - x, y);
            y += 28;

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString("Appointment ID: #" + appointmentId, x, y);
            y += 30;

            int labelWidth = 140;
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            y = drawPrintRow(g2, x, y, labelWidth, "Patient Name", patient);
            y = drawPrintRow(g2, x, y, labelWidth, "Age", age);
            y = drawPrintRow(g2, x, y, labelWidth, "Phone", phone);
            y = drawPrintRow(g2, x, y, labelWidth, "Doctor", doctor);
            y = drawPrintRow(g2, x, y, labelWidth, "Specialization", specialization);
            y = drawPrintRow(g2, x, y, labelWidth, "Date", date);
            y = drawPrintRow(g2, x, y, labelWidth, "Time", time);
            y = drawPrintRow(g2, x, y, labelWidth, "Consultation Fee", fee);
            y = drawPrintRow(g2, x, y, labelWidth, "Paid", paid);
            y = drawPrintRow(g2, x, y, labelWidth, "Due", due);
            y = drawPrintRow(g2, x, y, labelWidth, "Payment Status", paymentStatus);
            y = drawPrintRow(g2, x, y, labelWidth, "Appointment Status", status);

            y += 15;
            g2.drawLine(x, y, (int) printableWidth - x, y);
            y += 28;

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString(
                    "Please arrive 10 minutes before your appointment time.",
                    x,
                    y
            );
            y += 20;
            g2.drawString("Thank you for choosing MediBook.", x, y);

            return PAGE_EXISTS;
        }

        private int drawPrintRow(
                Graphics2D g2,
                int x,
                int y,
                int labelWidth,
                String label,
                String value) {

            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.drawString(label + ":", x, y);

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString(value, x + labelWidth, y);

            return y + 23;
        }
    }

    // =========================================================
    // CANCEL
    // =========================================================

    private void cancelSelected() {
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

        int modelRow = table.convertRowIndexToModel(row);
        int id = getInt(modelRow, 0);
        String patient = String.valueOf(model.getValueAt(modelRow, 1));
        String currentStatus = String.valueOf(model.getValueAt(modelRow, 12));

        if ("Cancelled".equals(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "This appointment is already cancelled.",
                    "MediBook",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if ("Completed".equals(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Completed appointment cannot be cancelled.",
                    "MediBook",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel appointment #" + id + " for " + patient + "?",
                "Cancel Appointment",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE appointments SET status = 'Cancelled' WHERE id = ?";

        try (Connection con = DB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Appointment cancelled successfully.",
                        "MediBook",
                        JOptionPane.INFORMATION_MESSAGE
                );
                refresh();
                notifyChanged();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDatabaseError("Unable to cancel appointment.", e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private int getInt(int row, int column) {
        return Integer.parseInt(String.valueOf(model.getValueAt(row, column)));
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void showDatabaseError(String prefix, SQLException e) {
        JOptionPane.showMessageDialog(
                this,
                prefix + "\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showDatabaseErrorTo(Component parent, String prefix, SQLException e) {
        JOptionPane.showMessageDialog(
                parent,
                prefix + "\n" + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /** Digits + one decimal point, max length. */
    private static void installMoneyFilter(JTextField field, int maxLength) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
                    @Override
                    public void insertString(
                            FilterBypass fb, int offset, String text,
                            javax.swing.text.AttributeSet attr)
                            throws javax.swing.text.BadLocationException {
                        replace(fb, offset, 0, text, attr);
                    }

                    @Override
                    public void replace(
                            FilterBypass fb, int offset, int length, String text,
                            javax.swing.text.AttributeSet attrs)
                            throws javax.swing.text.BadLocationException {

                        if (text == null || text.isEmpty()) {
                            super.replace(fb, offset, length, text, attrs);
                            return;
                        }

                        String current = fb.getDocument().getText(
                                0, fb.getDocument().getLength());
                        String next = current.substring(0, offset)
                                + text
                                + current.substring(offset + length);

                        if (!next.matches("\\d*(\\.\\d{0,2})?")) {
                            return;
                        }
                        if (next.length() > maxLength) {
                            return;
                        }

                        super.replace(fb, offset, length, text, attrs);
                    }
                }
        );
    }
}
