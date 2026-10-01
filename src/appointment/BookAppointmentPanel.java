package appointment;

import appointment.AppointmentData.Doctor;
import database.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static appointment.UI.*;

/**
 * "Book Appointment" page:
 * patient form on the left,
 * live summary + fee on the right.
 *
 * Doctors, specializations and appointments
 * are loaded from MySQL database.
 */
public class BookAppointmentPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final Runnable onBooked;

    private JTextField tfName, tfAge, tfPhone;

    private JComboBox<String> cbGender;
    private JComboBox<String> cbSpec;
    private JComboBox<String> cbTime;

    private JComboBox<Doctor> cbDoctor;

    private JSpinner spDate;

    private JLabel sumDoctor;
    private JLabel sumSpec;
    private JLabel sumDate;
    private JLabel sumTime;
    private JLabel sumFee;

    private boolean updating;

    public BookAppointmentPanel(Runnable onBooked) {

        this.onBooked = onBooked;

        setLayout(new BorderLayout());
        setBackground(BG);

        add(
                header(
                        "Book Appointment",
                        "Fill the patient details and book a doctor"
                ),
                BorderLayout.NORTH
        );

        add(buildPage(), BorderLayout.CENTER);

        loadSpecializations();
    }

    /**
     * Call when this page is shown.
     */
    public void refresh() {

        loadSpecializations();

        refreshDoctors();

        refreshSlots();

        updateSummary();
    }

    // =========================================================
    // UI
    // =========================================================

    private JComponent buildPage() {

        JPanel page = new JPanel(new BorderLayout(4, 0));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(0, 28, 20, 28));

        // ---------- form card ----------

        Card form = new Card(22, 26, 22, 26);
        form.setLayout(new BorderLayout(0, 14));

        JLabel ft = new JLabel("Patient & Appointment Details");
        ft.setFont(new Font(FONT, Font.BOLD, 17));
        ft.setForeground(TEXT);

        form.add(ft, BorderLayout.NORTH);

        tfName = new JTextField();
        tfAge = new JTextField();
        tfPhone = new JTextField();

        cbGender = new JComboBox<>(
                new String[]{
                        "Male",
                        "Female",
                        "Other"
                }
        );

        cbSpec = new JComboBox<>();
        cbDoctor = new JComboBox<>();
        cbTime = new JComboBox<>();

        spDate = createDateSpinner();

        styleField(tfName);
        styleField(tfAge);
        styleField(tfPhone);

        styleCombo(cbGender);
        styleCombo(cbSpec);
        styleCombo(cbDoctor);
        styleCombo(cbTime);

        spDate.setFont(new Font(FONT, Font.PLAIN, 14));
        spDate.setPreferredSize(new Dimension(100, 40));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        addField(
                grid,
                0,
                0,
                "Patient Name *",
                tfName
        );

        addField(
                grid,
                1,
                0,
                "Gender",
                cbGender
        );

        addField(
                grid,
                0,
                1,
                "Age *",
                tfAge
        );

        addField(
                grid,
                1,
                1,
                "Phone Number *",
                tfPhone
        );

        addField(
                grid,
                0,
                2,
                "Doctor Type / Specialization *",
                cbSpec
        );

        addField(
                grid,
                1,
                2,
                "Select Doctor *",
                cbDoctor
        );

        addField(
                grid,
                0,
                3,
                "Appointment Date *",
                spDate
        );

        addField(
                grid,
                1,
                3,
                "Time Slot *",
                cbTime
        );

        form.add(grid, BorderLayout.CENTER);

        // ---------- buttons ----------

        RoundedButton book = new RoundedButton(
                "Book Appointment",
                BLUE,
                DARK_BLUE,
                40
        );

        book.setFont(new Font(FONT, Font.BOLD, 14));
        book.setPreferredSize(new Dimension(190, 44));

        book.addActionListener(
                e -> bookAppointment()
        );

        RoundedButton clear = new RoundedButton(
                "Clear",
                new Color(226, 232, 240),
                new Color(210, 219, 230),
                40
        );

        clear.setForeground(TEXT);
        clear.setFont(new Font(FONT, Font.BOLD, 14));
        clear.setPreferredSize(new Dimension(110, 44));

        clear.addActionListener(
                e -> clearForm()
        );

        JPanel btns = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        0,
                        0
                )
        );

        btns.setOpaque(false);

        btns.add(book);
        btns.add(Box.createHorizontalStrut(10));
        btns.add(clear);

        form.add(btns, BorderLayout.SOUTH);

        // ---------- summary card ----------

        Card sum = new Card(22, 26, 22, 26);

        sum.setLayout(
                new BoxLayout(
                        sum,
                        BoxLayout.Y_AXIS
                )
        );

        sum.setPreferredSize(
                new Dimension(
                        340 + 2 * SH,
                        0
                )
        );

        JLabel st = new JLabel("Booking Summary");

        st.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        17
                )
        );

        st.setForeground(TEXT);
        st.setAlignmentX(Component.LEFT_ALIGNMENT);

        sum.add(st);

        sum.add(
                Box.createVerticalStrut(16)
        );

        sumDoctor = valueLabel();
        sumSpec = valueLabel();
        sumDate = valueLabel();
        sumTime = valueLabel();

        sum.add(
                summaryRow(
                        "Doctor",
                        sumDoctor
                )
        );

        sum.add(
                summaryRow(
                        "Specialization",
                        sumSpec
                )
        );

        sum.add(
                summaryRow(
                        "Date",
                        sumDate
                )
        );

        sum.add(
                summaryRow(
                        "Time",
                        sumTime
                )
        );

        sum.add(
                Box.createVerticalStrut(10)
        );

        JSeparator sep = new JSeparator();

        sep.setForeground(LINE);
        sep.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        2
                )
        );

        sep.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        sum.add(sep);

        sum.add(
                Box.createVerticalStrut(14)
        );

        JLabel feeTitle =
                new JLabel("Consultation Fee");

        feeTitle.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        13
                )
        );

        feeTitle.setForeground(MUTED);
        feeTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        sumFee =
                new JLabel(
                        RUPEE + " 0"
                );

        sumFee.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        38
                )
        );

        sumFee.setForeground(GREEN);
        sumFee.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        sum.add(feeTitle);

        sum.add(
                Box.createVerticalStrut(4)
        );

        sum.add(sumFee);

        sum.add(
                Box.createVerticalGlue()
        );

        // =====================================================
        // LISTENERS
        // =====================================================

        cbSpec.addActionListener(e -> {

            if (!updating) {

                refreshDoctors();

                refreshSlots();

                updateSummary();
            }
        });

        cbDoctor.addActionListener(e -> {

            if (!updating) {

                refreshSlots();

                updateSummary();
            }
        });

        cbTime.addActionListener(e -> {

            if (!updating) {

                updateSummary();
            }
        });

        spDate.addChangeListener(e -> {

            if (!updating) {

                refreshSlots();

                updateSummary();
            }
        });

        page.add(
                form,
                BorderLayout.CENTER
        );

        page.add(
                sum,
                BorderLayout.EAST
        );

        return page;
    }

    // =========================================================
    // DATABASE
    // =========================================================

    /**
     * Load specializations dynamically from doctors table.
     */
    private void loadSpecializations() {

        if (cbSpec == null) {
            return;
        }

        String oldSpec =
                (String) cbSpec.getSelectedItem();

        updating = true;

        cbSpec.removeAllItems();

        String sql =
                "SELECT DISTINCT specialization " +
                "FROM doctors " +
                "WHERE status = 'Active' " +
                "ORDER BY specialization";

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql);
                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                cbSpec.addItem(
                        rs.getString("specialization")
                );
            }

            if (oldSpec != null) {

                cbSpec.setSelectedItem(oldSpec);
            }

            if (cbSpec.getSelectedIndex() == -1
                    && cbSpec.getItemCount() > 0) {

                cbSpec.setSelectedIndex(0);
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load specializations.\n"
                            + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        updating = false;
    }

    /**
     * Load doctors dynamically according to specialization.
     */
    private void refreshDoctors() {

        if (cbDoctor == null) {
            return;
        }

        String spec =
                (String) cbSpec.getSelectedItem();

        updating = true;

        Doctor oldDoctor =
                (Doctor) cbDoctor.getSelectedItem();

        cbDoctor.removeAllItems();

        if (spec == null || spec.isEmpty()) {

            updating = false;
            return;
        }

        String sql =
                "SELECT id, name, specialization, consultation_fee "
                        + "FROM doctors "
                        + "WHERE specialization = ? "
                        + "AND status = 'Active' "
                        + "ORDER BY name";

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(1, spec);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    int id =
                            rs.getInt("id");

                    String name =
                            rs.getString("name");

                    String specialization =
                            rs.getString(
                                    "specialization"
                            );

                    int fee =
                            rs.getBigDecimal(
                                    "consultation_fee"
                            ).intValue();

                    Doctor doctor =
                            new Doctor(
                                    id,
                                    name,
                                    specialization,
                                    fee
                            );

                    cbDoctor.addItem(doctor);
                }
            }

            // Try to keep previously selected doctor
            if (oldDoctor != null) {

                for (int i = 0;
                     i < cbDoctor.getItemCount();
                     i++) {

                    Doctor current =
                            cbDoctor.getItemAt(i);

                    if (current.id == oldDoctor.id) {

                        cbDoctor.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (cbDoctor.getSelectedIndex() == -1
                    && cbDoctor.getItemCount() > 0) {

                cbDoctor.setSelectedIndex(0);
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load doctors.\n"
                            + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        updating = false;
    }

    /**
     * Check whether a doctor slot is already booked.
     */
    private boolean isSlotTaken(
            Doctor doctor,
            LocalDate date,
            String time
    ) {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM appointments "
                        + "WHERE doctor_id = ? "
                        + "AND appointment_date = ? "
                        + "AND appointment_time = ? "
                        + "AND status = 'Booked'";

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    doctor.id
            );

            ps.setDate(
                    2,
                    java.sql.Date.valueOf(date)
            );

            ps.setString(
                    3,
                    time
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to check appointment slot.\n"
                            + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        return false;
    }

    // =========================================================
    // LOGIC
    // =========================================================

    /**
     * Fills time combo with free slots
     * for selected doctor + date.
     */
    private void refreshSlots() {

        if (cbTime == null) {
            return;
        }

        updating = true;

        Object old =
                cbTime.getSelectedItem();

        cbTime.removeAllItems();

        Doctor doctor =
                (Doctor) cbDoctor.getSelectedItem();

        LocalDate date =
                selectedDate();

        if (doctor != null) {

            LocalTime now =
                    LocalTime.now();

            for (int h = 9; h < 18; h++) {

                for (int m = 0; m < 60; m += 30) {

                    // Lunch break
                    if (h == 13) {
                        continue;
                    }

                    LocalTime time =
                            LocalTime.of(
                                    h,
                                    m
                            );

                    // Don't show past time today
                    if (
                            date.equals(LocalDate.now())
                                    && !time.isAfter(now)
                    ) {
                        continue;
                    }

                    String label =
                            time.format(TIME_FMT);

                    // Check DB
                    if (!isSlotTaken(
                            doctor,
                            date,
                            label
                    )) {

                        cbTime.addItem(label);
                    }
                }
            }

            // Keep previously selected slot
            if (old != null) {

                cbTime.setSelectedItem(old);
            }
        }

        updating = false;
    }

    private void updateSummary() {

        if (sumDoctor == null) {
            return;
        }

        Doctor doctor =
                (Doctor) cbDoctor.getSelectedItem();

        sumDoctor.setText(
                doctor == null
                        ? "-"
                        : doctor.name
        );

        sumSpec.setText(
                doctor == null
                        ? "-"
                        : doctor.spec
        );

        sumDate.setText(
                selectedDate()
                        .format(DATE_FMT)
        );

        sumTime.setText(
                cbTime.getSelectedItem() == null
                        ? "-"
                        : (String) cbTime.getSelectedItem()
        );

        sumFee.setText(
                RUPEE
                        + " "
                        + (
                        doctor == null
                                ? 0
                                : doctor.fee
                )
        );
    }

    private LocalDate selectedDate() {

        Date d =
                (Date) spDate.getValue();

        return d.toInstant()
                .atZone(
                        ZoneId.systemDefault()
                )
                .toLocalDate();
    }

    // =========================================================
    // BOOK APPOINTMENT
    // =========================================================

    private void bookAppointment() {

        String name =
                tfName.getText().trim();

        String ageText =
                tfAge.getText().trim();

        String phone =
                tfPhone.getText().trim();

        String gender =
                (String) cbGender.getSelectedItem();

        Doctor doctor =
                (Doctor) cbDoctor.getSelectedItem();

        String time =
                (String) cbTime.getSelectedItem();

        LocalDate date =
                selectedDate();

        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------

        if (
                name.isEmpty()
                        || !name.matches("[A-Za-z .]+")
        ) {

            warn(
                    "Please enter a valid patient name (letters only).",
                    tfName
            );

            return;
        }

        int age;

        try {

            age =
                    Integer.parseInt(ageText);

            if (age < 1 || age > 120) {

                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {

            warn(
                    "Please enter a valid age (1 - 120).",
                    tfAge
            );

            return;
        }

        if (!phone.matches("\\d{10}")) {

            warn(
                    "Phone number must be exactly 10 digits.",
                    tfPhone
            );

            return;
        }

        if (doctor == null) {

            warn(
                    "Please select a doctor.",
                    cbDoctor
            );

            return;
        }

        if (time == null) {

            warn(
                    "No free time slot for this doctor on the selected date.\n"
                            + "Please choose another date or doctor.",
                    spDate
            );

            return;
        }

        // -----------------------------------------------------
        // FINAL DB SLOT CHECK
        // -----------------------------------------------------

        if (
                isSlotTaken(
                        doctor,
                        date,
                        time
                )
        ) {

            warn(
                    "This slot was just booked. Please choose another time.",
                    cbTime
            );

            refreshSlots();

            return;
        }

        // -----------------------------------------------------
        // INSERT INTO DATABASE
        // -----------------------------------------------------

        String sql =
                "INSERT INTO appointments "
                        + "("
                        + "patient_name, "
                        + "age, "
                        + "phone, "
                        + "gender, "
                        + "doctor_id, "
                        + "appointment_date, "
                        + "appointment_time, "
                        + "fee, "
                        + "status"
                        + ") "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Booked')";

        int bookingId = -1;

        try (
                Connection con =
                        DB.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            ps.setString(
                    1,
                    name
            );

            ps.setInt(
                    2,
                    age
            );

            ps.setString(
                    3,
                    phone
            );

            ps.setString(
                    4,
                    gender
            );

            ps.setInt(
                    5,
                    doctor.id
            );

            ps.setDate(
                    6,
                    java.sql.Date.valueOf(date)
            );

            ps.setString(
                    7,
                    time
            );

            ps.setInt(
                    8,
                    doctor.fee
            );

            ps.executeUpdate();

            try (
                    ResultSet rs =
                            ps.getGeneratedKeys()
            ) {

                if (rs.next()) {

                    bookingId =
                            rs.getInt(1);
                }
            }

        } catch (SQLIntegrityConstraintViolationException ex) {

            // Another user may have booked the same slot
            warn(
                    "This appointment slot is already booked.\n"
                            + "Please select another time.",
                    cbTime
            );

            refreshSlots();

            return;

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to book appointment.\n\n"
                            + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // -----------------------------------------------------
        // SUCCESS MESSAGE
        // -----------------------------------------------------

        String msg =
                "<html><body style='width:300px;font-family:Segoe UI;'>"
                        + "<h2 style='color:#29A05F;margin:0 0 8px 0;'>"
                        + "Your appointment is booked!"
                        + "</h2>"

                        + "<b>Booking ID:</b> #"
                        + bookingId
                        + "<br>"

                        + "<b>Patient:</b> "
                        + name
                        + " ("
                        + age
                        + " yrs)<br>"

                        + "<b>Phone:</b> "
                        + phone
                        + "<br>"

                        + "<b>Doctor:</b> "
                        + doctor.name
                        + "<br>"

                        + "<b>Specialization:</b> "
                        + doctor.spec
                        + "<br>"

                        + "<b>Date:</b> "
                        + date.format(DATE_FMT)
                        + "<br>"

                        + "<b>Time:</b> "
                        + time
                        + "<br>"

                        + "<b>Fee:</b> "
                        + RUPEE
                        + " "
                        + doctor.fee

                        + "</body></html>";

        JOptionPane.showMessageDialog(
                this,
                msg,
                "MediBook - Booking Confirmed",
                JOptionPane.INFORMATION_MESSAGE
        );

        clearForm();

        if (onBooked != null) {
            onBooked.run();
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void warn(
            String msg,
            JComponent focus
    ) {

        JOptionPane.showMessageDialog(
                this,
                msg,
                "MediBook",
                JOptionPane.WARNING_MESSAGE
        );

        focus.requestFocusInWindow();
    }

    private void clearForm() {

        tfName.setText("");
        tfAge.setText("");
        tfPhone.setText("");

        cbGender.setSelectedIndex(0);

        updating = true;

        if (cbSpec.getItemCount() > 0) {
            cbSpec.setSelectedIndex(0);
        }

        spDate.setValue(new Date());

        updating = false;

        refreshDoctors();
        refreshSlots();
        updateSummary();
    }

    // =========================================================
    // DATE SPINNER
    // =========================================================

    /**
     * Date spinner starts from today
     * and cannot select past date.
     */
    private static JSpinner createDateSpinner() {

        java.util.Calendar c =
                java.util.Calendar.getInstance();

        c.set(
                java.util.Calendar.HOUR_OF_DAY,
                0
        );

        c.set(
                java.util.Calendar.MINUTE,
                0
        );

        c.set(
                java.util.Calendar.SECOND,
                0
        );

        c.set(
                java.util.Calendar.MILLISECOND,
                0
        );

        SpinnerDateModel m =
                new SpinnerDateModel(
                        new Date(),
                        c.getTime(),
                        null,
                        java.util.Calendar.DAY_OF_MONTH
                );

        JSpinner s =
                new JSpinner(m);

        s.setEditor(
                new JSpinner.DateEditor(
                        s,
                        "dd-MM-yyyy"
                )
        );

        return s;
    }

    // =========================================================
    // SMALL BUILDERS
    // =========================================================

    private void addField(
            JPanel grid,
            int col,
            int row,
            String label,
            JComponent comp
    ) {

        JPanel p = new JPanel();

        p.setOpaque(false);

        p.setLayout(
                new BoxLayout(
                        p,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel l =
                new JLabel(label);

        l.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        12
                )
        );

        l.setForeground(MUTED);

        l.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        comp.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        comp.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        40
                )
        );

        p.add(l);

        p.add(
                Box.createVerticalStrut(6)
        );

        p.add(comp);

        GridBagConstraints gc =
                new GridBagConstraints();

        gc.gridx = col;
        gc.gridy = row;

        gc.weightx = 1;

        gc.fill =
                GridBagConstraints.HORIZONTAL;

        gc.anchor =
                GridBagConstraints.NORTH;

        gc.insets =
                new Insets(
                        0,
                        col == 0 ? 0 : 10,
                        14,
                        col == 0 ? 10 : 0
                );

        grid.add(p, gc);
    }

    private JLabel valueLabel() {

        JLabel l =
                new JLabel("-");

        l.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        14
                )
        );

        l.setForeground(TEXT);

        return l;
    }

    private JPanel summaryRow(
            String label,
            JLabel value
    ) {

        JPanel row =
                new JPanel(
                        new BorderLayout(
                                10,
                                0
                        )
                );

        row.setOpaque(false);

        row.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        34
                )
        );

        JLabel l =
                new JLabel(label);

        l.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        13
                )
        );

        l.setForeground(MUTED);

        value.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        row.add(
                l,
                BorderLayout.WEST
        );

        row.add(
                value,
                BorderLayout.CENTER
        );

        return row;
    }
}