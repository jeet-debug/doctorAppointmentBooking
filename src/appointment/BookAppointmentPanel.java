package appointment;

import appointment.AppointmentData.Booking;
import appointment.AppointmentData.Doctor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static appointment.UI.*;

/**
 * "Book Appointment" page: patient form on the left, live summary + fee on the
 * right.
 * Booking hone ke baad onBooked chalta hai (App isse Appointments page khol
 * deta hai).
 */
public class BookAppointmentPanel extends JPanel {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

  private final Runnable onBooked;

  private JTextField tfName, tfAge, tfPhone;
  private JComboBox<String> cbGender, cbSpec, cbTime;
  private JComboBox<Doctor> cbDoctor;
  private JSpinner spDate;
  private JLabel sumDoctor, sumSpec, sumDate, sumTime, sumFee;
  private boolean updating;

  public BookAppointmentPanel(Runnable onBooked) {
    this.onBooked = onBooked;
    setLayout(new BorderLayout());
    setBackground(BG);

    add(header("Book Appointment", "Fill the patient details and book a doctor"), BorderLayout.NORTH);
    add(buildPage(), BorderLayout.CENTER);

    onSpecChanged();
  }

  /** Call when this page is shown (slots may have changed after a cancel). */
  public void refresh() {
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
    cbGender = new JComboBox<>(new String[] { "Male", "Female", "Other" });
    cbSpec = new JComboBox<>(AppointmentData.SPECS);
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
    addField(grid, 0, 0, "Patient Name *", tfName);
    addField(grid, 1, 0, "Gender", cbGender);
    addField(grid, 0, 1, "Age *", tfAge);
    addField(grid, 1, 1, "Phone Number *", tfPhone);
    addField(grid, 0, 2, "Doctor Type / Specialization *", cbSpec);
    addField(grid, 1, 2, "Select Doctor *", cbDoctor);
    addField(grid, 0, 3, "Appointment Date *", spDate);
    addField(grid, 1, 3, "Time Slot *", cbTime);
    form.add(grid, BorderLayout.CENTER);

    RoundedButton book = new RoundedButton("Book Appointment", BLUE, DARK_BLUE, 40);
    book.setFont(new Font(FONT, Font.BOLD, 14));
    book.setPreferredSize(new Dimension(190, 44));
    book.addActionListener(e -> bookAppointment());

    RoundedButton clear = new RoundedButton("Clear", new Color(226, 232, 240), new Color(210, 219, 230), 40);
    clear.setForeground(TEXT);
    clear.setFont(new Font(FONT, Font.BOLD, 14));
    clear.setPreferredSize(new Dimension(110, 44));
    clear.addActionListener(e -> clearForm());

    JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
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

    // listeners
    cbSpec.addActionListener(e -> {
      if (!updating)
        onSpecChanged();
    });
    cbDoctor.addActionListener(e -> {
      if (!updating) {
        refreshSlots();
        updateSummary();
      }
    });
    cbTime.addActionListener(e -> {
      if (!updating)
        updateSummary();
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
  // LOGIC
  // =========================================================
  private void onSpecChanged() {
    updating = true;
    String spec = (String) cbSpec.getSelectedItem();
    cbDoctor.removeAllItems();
    for (Doctor d : AppointmentData.DOCTORS) {
      if (d.spec.equals(spec))
        cbDoctor.addItem(d);
    }
    updating = false;
    refreshSlots();
    updateSummary();
  }

  /** Fills the time combo with free slots for the chosen doctor + date. */
  private void refreshSlots() {
    if (cbTime == null)
      return;
    updating = true;
    Object old = cbTime.getSelectedItem();
    cbTime.removeAllItems();

    Doctor d = (Doctor) cbDoctor.getSelectedItem();
    LocalDate date = selectedDate();
    if (d != null) {
      LocalTime now = LocalTime.now();
      for (int h = 9; h < 18; h++) {
        for (int m = 0; m < 60; m += 30) {
          if (h == 13)
            continue; // lunch break
          LocalTime t = LocalTime.of(h, m);
          if (date.equals(LocalDate.now()) && !t.isAfter(now))
            continue;
          String label = t.format(TIME_FMT);
          if (!AppointmentData.isTaken(d, date, label))
            cbTime.addItem(label);
        }
      }
      if (old != null)
        cbTime.setSelectedItem(old);
    }
    updating = false;
  }

  private void updateSummary() {
    Doctor d = (Doctor) cbDoctor.getSelectedItem();
    sumDoctor.setText(d == null ? "-" : d.name);
    sumSpec.setText(d == null ? "-" : d.spec);
    sumDate.setText(selectedDate().format(DATE_FMT));
    sumTime.setText(cbTime.getSelectedItem() == null ? "-" : (String) cbTime.getSelectedItem());
    sumFee.setText(RUPEE + " " + (d == null ? 0 : d.fee));
  }

  private LocalDate selectedDate() {
    Date d = (Date) spDate.getValue();
    return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void bookAppointment() {
    String name = tfName.getText().trim();
    String ageText = tfAge.getText().trim();
    String phone = tfPhone.getText().trim();
    Doctor d = (Doctor) cbDoctor.getSelectedItem();
    String time = (String) cbTime.getSelectedItem();

    if (name.isEmpty() || !name.matches("[A-Za-z .]+")) {
      warn("Please enter a valid patient name (letters only).", tfName);
      return;
    }
    int age;
    try {
      age = Integer.parseInt(ageText);
      if (age < 1 || age > 120)
        throw new NumberFormatException();
    } catch (NumberFormatException ex) {
      warn("Please enter a valid age (1 - 120).", tfAge);
      return;
    }
    if (!phone.matches("\\d{10}")) {
      warn("Phone number must be exactly 10 digits.", tfPhone);
      return;
    }
    if (d == null) {
      warn("Please select a doctor.", cbDoctor);
      return;
    }
    if (time == null) {
      warn("No free time slot for this doctor on the selected date.\nPlease choose another date or doctor.", spDate);
      return;
    }
    if (AppointmentData.isTaken(d, selectedDate(), time)) {
      warn("This slot was just booked. Please choose another time.", cbTime);
      refreshSlots();
      return;
    }

    Booking b = new Booking();
    b.id = AppointmentData.nextId();
    b.patient = name;
    b.age = age;
    b.phone = phone;
    b.gender = (String) cbGender.getSelectedItem();
    b.doctor = d;
    b.date = selectedDate();
    b.time = time;
    AppointmentData.BOOKINGS.add(b);

    String msg = "<html><body style='width:300px;font-family:Segoe UI;'>"
        + "<h2 style='color:#29A05F;margin:0 0 8px 0;'>Your appointment is booked!</h2>"
        + "<b>Booking ID:</b> #" + b.id + "<br>"
        + "<b>Patient:</b> " + b.patient + " (" + b.age + " yrs)<br>"
        + "<b>Phone:</b> " + b.phone + "<br>"
        + "<b>Doctor:</b> " + d.name + "<br>"
        + "<b>Specialization:</b> " + d.spec + "<br>"
        + "<b>Date:</b> " + b.date.format(DATE_FMT) + "<br>"
        + "<b>Time:</b> " + b.time + "<br>"
        + "<b>Fee:</b> " + RUPEE + " " + d.fee
        + "</body></html>";
    JOptionPane.showMessageDialog(this, msg, "MediBook - Booking Confirmed",
        JOptionPane.INFORMATION_MESSAGE);

    clearForm();
    if (onBooked != null)
      onBooked.run();
  }

  private void warn(String msg, JComponent focus) {
    JOptionPane.showMessageDialog(this, msg, "MediBook", JOptionPane.WARNING_MESSAGE);
    focus.requestFocusInWindow();
  }

  private void clearForm() {
    tfName.setText("");
    tfAge.setText("");
    tfPhone.setText("");
    cbGender.setSelectedIndex(0);
    updating = true;
    cbSpec.setSelectedIndex(0);
    spDate.setValue(new Date());
    updating = false;
    onSpecChanged();
  }

  // =========================================================
  // SMALL BUILDERS
  // =========================================================
  /** Date spinner that starts today and can't go to the past. */
  private static JSpinner createDateSpinner() {
    java.util.Calendar c = java.util.Calendar.getInstance();
    c.set(java.util.Calendar.HOUR_OF_DAY, 0);
    c.set(java.util.Calendar.MINUTE, 0);
    c.set(java.util.Calendar.SECOND, 0);
    c.set(java.util.Calendar.MILLISECOND, 0);
    SpinnerDateModel m = new SpinnerDateModel(new Date(), c.getTime(), null,
        java.util.Calendar.DAY_OF_MONTH);
    JSpinner s = new JSpinner(m);
    s.setEditor(new JSpinner.DateEditor(s, "dd-MM-yyyy"));
    return s;
  }

  private void addField(JPanel grid, int col, int row, String label, JComponent comp) {
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
    gc.insets = new Insets(0, col == 0 ? 0 : 10, 14, col == 0 ? 10 : 0);
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