package appointment;

import appointment.AppointmentData.Doctor;
import database.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static appointment.UI.*;

/**
 * "Book Appointment" page:
 * patient form on the left,
 * live summary + fee on the right.
 *
 * Doctors, specializations and appointments
 * are loaded from MySQL database.
 *
 * Ek time slot me max MAX_PER_SLOT (2) patients book ho sakte hain.
 * Slot full hone par wo muted (grey) ho jata hai aur select nahi hota.
 */
public class BookAppointmentPanel extends JPanel {

    /** Ek slot me kitne patients allowed hain. */
    private static final int MAX_PER_SLOT = 2;

    private static final Color MUTED_SLOT = new Color(160, 170, 185);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    /**
     * Time combo ka item: label + kitni booking ho chuki hai.
     */
    private static class Slot {

        final String label;
        final int booked;
        final boolean full;

        Slot(String label, int booked) {
            this.label = label;
            this.booked = booked;
            this.full = booked >= MAX_PER_SLOT;
        }

        @Override
        public String toString() {

            if (full) {
                return label + "   (Full)";
            }

            if (booked > 0) {
                return label + "   (" + (MAX_PER_SLOT - booked) + " left)";
            }

            return label;
        }
    }

    private final Runnable onBooked;

    private JTextField tfName, tfAge, tfPhone;

    private JComboBox<String> cbGender;
    private JComboBox<String> cbSpec;
    private JComboBox<Slot> cbTime;

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

        // Full slot select na ho sake, isliye custom model
        cbTime = new JComboBox<>(new DefaultComboBoxModel<Slot>() {

            @Override
            public void setSelectedItem(Object item) {

                if (item instanceof Slot && ((Slot) item).full) {
                    return; // full slot ko ignore karo
                }

                super.setSelectedItem(item);
            }
        });

        spDate = createDateSpinner();

        styleField(tfName);
        styleField(tfAge);
        styleField(tfPhone);

        // Age: only digits, max 3 | Phone: only digits, max 10
        digitsOnly(tfAge, 3);
        digitsOnly(tfPhone, 10);

        styleCombo(cbGender);
        styleCombo(cbSpec);
        styleCombo(cbDoctor);
        styleCombo(cbTime);

        // Full slots ko grey (muted) dikhane ke liye renderer
        final ListCellRenderer<? super Slot> baseRenderer =
                cbTime.getRenderer();

        cbTime.setRenderer((list, value, index, isSelected, hasFocus) -> {

            Component c = baseRenderer.getListCellRendererComponent(
                    list, value, index, isSelected, hasFocus
            );

            if (value != null && value.full) {
                c.setForeground(MUTED_SLOT);
            }

            return c;
        });

        spDate.setFont(new Font(FONT, Font.PLAIN, 14));
        spDate.setPreferredSize(new Dimension(100, 40));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        addField(grid, 0, 0, "Patient Name *", tfName);
        addField(grid, 1, 0, "Gender", cbGender);

        addField(grid, 0, 1, "Age *", tfAge);
        addField(grid, 1, 1, "Phone Number *", tfPhone);

        addField(grid, 0, 2, "Doctor Type / Specialization *", cbSpec);
        addField(grid, 1, 2, "Select Doctor *", cbDoctor);

        addField(grid, 0, 3, "Appointment Date *", spDate);
        addField(grid, 1, 3, "Time Slot *", cbTime);

        // Grid ko upar chipkane ke liye wrapper
        // (CENTER mein direct rakhne se bada gap aa raha tha)
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setOpaque(false);
        gridWrap.add(grid, BorderLayout.NORTH);

        form.add(gridWrap, BorderLayout.CENTER);

        // ---------- buttons ----------

        RoundedButton book = new RoundedButton(
                "Book Appointment",
                BLUE,
                DARK_BLUE,
                40
        );

        book.setFont(new Font(FONT, Font.BOLD, 14));
        book.setPreferredSize(new Dimension(190, 44));

        book.addActionListener(e -> bookAppointment());

        RoundedButton clear = new RoundedButton(
                "Clear",
                new Color(226, 232, 240),
                new Color(210, 219, 230),
                40
        );

        clear.setForeground(TEXT);
        clear.setFont(new Font(FONT, Font.BOLD, 14));
        clear.setPreferredSize(new Dimension(110, 44));

        clear.addActionListener(e -> clearForm());

        JPanel btns = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );

        btns.setOpaque(false);

        btns.add(book);
        btns.add(Box.createHorizontalStrut(10));
        btns.add(clear);

        form.add(btns, BorderLayout.SOUTH);

        // ---------- summary card ----------

        Card sum = new Card(22, 26, 22, 26);

        sum.setLayout(new BoxLayout(sum, BoxLayout.Y_AXIS));

        sum.setPreferredSize(new Dimension(340 + 2 * SH, 0));

        JLabel st = new JLabel("Booking Summary");
        st.setFont(new Font(FONT, Font.BOLD, 17));
        st.setForeground(TEXT);
        st.setAlignmentX(Component.LEFT_ALIGNMENT);

        sum.add(st);
        sum.add(Box.createVerticalStrut(16));

        sumDoctor = valueLabel();
        sumSpec = valueLabel();
        sumDate = valueLabel();
        sumTime = valueLabel();

        sum.add(summaryRow("Doctor", sumDoctor));
        sum.add(summaryRow("Specialization", sumSpec));
        sum.add(summaryRow("Date", sumDate));
        sum.add(summaryRow("Time", sumTime));

        sum.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(LINE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        sum.add(sep);
        sum.add(Box.createVerticalStrut(14));

        JLabel feeTitle = new JLabel("Consultation Fee");
        feeTitle.setFont(new Font(FONT, Font.PLAIN, 13));
        feeTitle.setForeground(MUTED);
        feeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sumFee = new JLabel(RUPEE + " 0");
        sumFee.setFont(new Font(FONT, Font.BOLD, 38));
        sumFee.setForeground(GREEN);
        sumFee.setAlignmentX(Component.LEFT_ALIGNMENT);

        sum.add(feeTitle);
        sum.add(Box.createVerticalStrut(4));
        sum.add(sumFee);
        sum.add(Box.createVerticalGlue());

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

        page.add(form, BorderLayout.CENTER);
        page.add(sum, BorderLayout.EAST);

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

        String oldSpec = (String) cbSpec.getSelectedItem();

        updating = true;

        cbSpec.removeAllItems();

        String sql =
                "SELECT DISTINCT specialization " +
                "FROM doctors " +
                "WHERE status = 'Active' " +
                "ORDER BY specialization";

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                cbSpec.addItem(rs.getString("specialization"));
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

        String spec = (String) cbSpec.getSelectedItem();

        updating = true;

        Doctor oldDoctor = (Doctor) cbDoctor.getSelectedItem();

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
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, spec);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String specialization = rs.getString("specialization");
                    int fee = rs.getBigDecimal("consultation_fee").intValue();

                    cbDoctor.addItem(
                            new Doctor(id, name, specialization, fee)
                    );
                }
            }

            // Try to keep previously selected doctor
            if (oldDoctor != null) {

                for (int i = 0; i < cbDoctor.getItemCount(); i++) {

                    Doctor current = cbDoctor.getItemAt(i);

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
     * Ek doctor + date ke har slot ki booking count (ek hi query me).
     * Key = time label (e.g. "02:00 PM"), Value = kitni 'Booked'.
     */
    private Map<String, Integer> loadBookedCounts(
            Doctor doctor,
            LocalDate date
    ) {

        Map<String, Integer> counts = new HashMap<>();

        String sql =
                "SELECT appointment_time, COUNT(*) AS cnt "
                        + "FROM appointments "
                        + "WHERE doctor_id = ? "
                        + "AND appointment_date = ? "
                        + "AND status = 'Booked' "
                        + "GROUP BY appointment_time";

        try (
                Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, doctor.id);
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    counts.put(
                            rs.getString("appointment_time"),
                            rs.getInt("cnt")
                    );
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to check appointment slots.\n"
                            + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        return counts;
    }

    /**
     * Ek slot par abhi kitni 'Booked' appointments hain.
     * Same connection use hota hai taaki transaction ke andar chale.
     */
    private int bookedCount(
            Connection con,
            Doctor doctor,
            LocalDate date,
            String time
    ) throws SQLException {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM appointments "
                        + "WHERE doctor_id = ? "
                        + "AND appointment_date = ? "
                        + "AND appointment_time = ? "
                        + "AND status = 'Booked'";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, doctor.id);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, time);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    // =========================================================
    // LOGIC
    // =========================================================

    /**
     * Fills time combo for selected doctor + date.
     * Full slots (2 bookings) dikhte hain par muted aur un-selectable.
     */
    private void refreshSlots() {

        if (cbTime == null) {
            return;
        }

        updating = true;

        Slot oldSlot = (Slot) cbTime.getSelectedItem();
        String oldLabel = oldSlot == null ? null : oldSlot.label;

        cbTime.removeAllItems();

        Doctor doctor = (Doctor) cbDoctor.getSelectedItem();

        LocalDate date = selectedDate();

        if (doctor != null) {

            Map<String, Integer> counts = loadBookedCounts(doctor, date);

            LocalTime now = LocalTime.now();

            for (int h = 9; h < 18; h++) {

                for (int m = 0; m < 60; m += 30) {

                    // Lunch break
                    if (h == 13) {
                        continue;
                    }

                    LocalTime time = LocalTime.of(h, m);

                    // Don't show past time today
                    if (date.equals(LocalDate.now())
                            && !time.isAfter(now)) {
                        continue;
                    }

                    String label = time.format(TIME_FMT);

                    cbTime.addItem(
                            new Slot(label, counts.getOrDefault(label, 0))
                    );
                }
            }

            // Pehle wala slot wapas select karo (agar full nahi hai),
            // warna pehla available slot.
            Slot toSelect = null;
            Slot firstFree = null;

            for (int i = 0; i < cbTime.getItemCount(); i++) {

                Slot s = cbTime.getItemAt(i);

                if (s.full) {
                    continue;
                }

                if (firstFree == null) {
                    firstFree = s;
                }

                if (s.label.equals(oldLabel)) {
                    toSelect = s;
                    break;
                }
            }

            if (toSelect == null) {
                toSelect = firstFree;
            }

            // null => koi free slot nahi
            cbTime.setSelectedItem(toSelect);
        }

        updating = false;
    }

    private void updateSummary() {

        if (sumDoctor == null) {
            return;
        }

        Doctor doctor = (Doctor) cbDoctor.getSelectedItem();

        sumDoctor.setText(doctor == null ? "-" : doctor.name);

        sumSpec.setText(doctor == null ? "-" : doctor.spec);

        sumDate.setText(selectedDate().format(DATE_FMT));

        Slot slot = (Slot) cbTime.getSelectedItem();

        sumTime.setText(slot == null ? "-" : slot.label);

        sumFee.setText(
                RUPEE + " " + (doctor == null ? 0 : doctor.fee)
        );
    }

    private LocalDate selectedDate() {

        Date d = (Date) spDate.getValue();

        return d.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    // =========================================================
    // BOOK APPOINTMENT
    // =========================================================

    private void bookAppointment() {

        String name = tfName.getText().trim();
        String ageText = tfAge.getText().trim();
        String phone = tfPhone.getText().trim();

        String gender = (String) cbGender.getSelectedItem();

        Doctor doctor = (Doctor) cbDoctor.getSelectedItem();

        Slot slot = (Slot) cbTime.getSelectedItem();

        String time = slot == null ? null : slot.label;

        LocalDate date = selectedDate();

        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------

        if (name.isEmpty() || !name.matches("[A-Za-z .]+")) {

            warn(
                    "Please enter a valid patient name (letters only).",
                    tfName
            );

            return;
        }

        int age;

        try {

            age = Integer.parseInt(ageText);

            if (age < 1 || age > 120) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {

            warn("Please enter a valid age (1 - 120).", tfAge);

            return;
        }

        if (!phone.matches("\\d{10}")) {

            warn("Phone number must be exactly 10 digits.", tfPhone);

            return;
        }

        if (doctor == null) {

            warn("Please select a doctor.", cbDoctor);

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
        // CHECK + INSERT (ek transaction me)
        //
        // Doctor row ko lock karte hain taaki 2 users ek saath
        // book karein to bhi 2 se zyada booking na ho paye.
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
        boolean slotFull = false;

        try (Connection con = DB.getConnection()) {

            con.setAutoCommit(false);

            try {

                // Lock doctor row
                try (
                        PreparedStatement lock = con.prepareStatement(
                                "SELECT id FROM doctors WHERE id = ? FOR UPDATE"
                        )
                ) {

                    lock.setInt(1, doctor.id);

                    try (ResultSet ignored = lock.executeQuery()) {
                        // sirf lock lena tha
                    }
                }

                if (bookedCount(con, doctor, date, time) >= MAX_PER_SLOT) {

                    slotFull = true;
                    con.rollback();

                } else {

                    try (
                            PreparedStatement ps = con.prepareStatement(
                                    sql,
                                    Statement.RETURN_GENERATED_KEYS
                            )
                    ) {

                        ps.setString(1, name);
                        ps.setInt(2, age);
                        ps.setString(3, phone);
                        ps.setString(4, gender);
                        ps.setInt(5, doctor.id);
                        ps.setDate(6, java.sql.Date.valueOf(date));
                        ps.setString(7, time);
                        ps.setInt(8, doctor.fee);

                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {

                            if (rs.next()) {
                                bookingId = rs.getInt(1);
                            }
                        }
                    }

                    con.commit();
                }

            } catch (SQLException ex) {

                con.rollback();
                throw ex;

            } finally {

                con.setAutoCommit(true);
            }

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

        if (slotFull) {

            warn(
                    "This slot is now full (" + MAX_PER_SLOT
                            + " patients booked).\n"
                            + "Please choose another time.",
                    cbTime
            );

            refreshSlots();
            updateSummary();

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

                        + "<b>Booking ID:</b> #" + bookingId + "<br>"

                        + "<b>Patient:</b> " + name
                        + " (" + age + " yrs)<br>"

                        + "<b>Phone:</b> " + phone + "<br>"

                        + "<b>Doctor:</b> " + doctor.name + "<br>"

                        + "<b>Specialization:</b> " + doctor.spec + "<br>"

                        + "<b>Date:</b> " + date.format(DATE_FMT) + "<br>"

                        + "<b>Time:</b> " + time + "<br>"

                        + "<b>Fee:</b> " + RUPEE + " " + doctor.fee

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

    private void warn(String msg, JComponent focus) {

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

    /**
     * Field mein sirf digits allow karta hai, maxLength tak.
     * Typing aur paste dono pe kaam karta hai.
     * Letters / special characters bilkul type nahi hote.
     */
    private static void digitsOnly(JTextField field, int maxLength) {

        ((AbstractDocument) field.getDocument()).setDocumentFilter(
                new DocumentFilter() {

                    @Override
                    public void insertString(FilterBypass fb, int offset,
                                             String text, AttributeSet attr)
                            throws BadLocationException {

                        replace(fb, offset, 0, text, attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length,
                                        String text, AttributeSet attrs)
                            throws BadLocationException {

                        // Delete / clear (setText("")) hamesha allow
                        if (text == null || text.isEmpty()) {
                            super.replace(fb, offset, length, text, attrs);
                            return;
                        }

                        // Sirf digits rakho
                        String digits = text.replaceAll("\\D", "");

                        // Kitni jagah bachi hai
                        int current = fb.getDocument().getLength();
                        int room = maxLength - (current - length);

                        if (room <= 0 || digits.isEmpty()) {
                            return;
                        }

                        if (digits.length() > room) {
                            digits = digits.substring(0, room);
                        }

                        super.replace(fb, offset, length, digits, attrs);
                    }
                }
        );
    }

    // =========================================================
    // DATE SPINNER
    // =========================================================

    /**
     * Date spinner starts from today
     * and cannot select past date.
     */
    private static JSpinner createDateSpinner() {

        java.util.Calendar c = java.util.Calendar.getInstance();

        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);

        SpinnerDateModel m =
                new SpinnerDateModel(
                        new Date(),
                        c.getTime(),
                        null,
                        java.util.Calendar.DAY_OF_MONTH
                );

        JSpinner s = new JSpinner(m);

        s.setEditor(new JSpinner.DateEditor(s, "dd-MM-yyyy"));

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
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);

        l.setFont(new Font(FONT, Font.BOLD, 12));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(comp);

        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = col;
        gc.gridy = row;

        gc.weightx = 1;

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;

        gc.insets = new Insets(
                0,
                col == 0 ? 0 : 10,
                14,
                col == 0 ? 10 : 0
        );

        grid.add(p, gc);
    }

    private JLabel valueLabel() {

        JLabel l = new JLabel("-");

        l.setFont(new Font(FONT, Font.BOLD, 14));
        l.setForeground(TEXT);

        return l;
    }

    private JPanel summaryRow(String label, JLabel value) {

        JPanel row = new JPanel(new BorderLayout(10, 0));

        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel l = new JLabel(label);

        l.setFont(new Font(FONT, Font.PLAIN, 13));
        l.setForeground(MUTED);

        value.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);

        return row;
    }
}