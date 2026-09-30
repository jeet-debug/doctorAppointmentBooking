package appointment;

import appointment.AppointmentData.Booking;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

import static appointment.UI.*;

/**
 * "Appointments" page: table of all booked appointments with search,
 * specialization / status filter and Cancel option.
 */
public class AppointmentsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private DefaultTableModel model;
    private JTable table;
    private JTextField tfSearch;
    private JComboBox<String> cbFilterSpec, cbFilterStatus;
    private JLabel countLabel;

    /** Called after a cancel so the Book page can free the slot (optional). */
    private final Runnable onChanged;

    public AppointmentsPanel() {
        this(null);
    }

    public AppointmentsPanel(Runnable onChanged) {
        this.onChanged = onChanged;
        setLayout(new BorderLayout());
        setBackground(BG);

        add(header("Appointments", "All booked appointments - search, filter and manage"), BorderLayout.NORTH);
        add(buildPage(), BorderLayout.CENTER);

        refresh();
    }

    /** Call when this page is shown so the newest bookings appear. */
    public void refresh() {
        if (model == null)
            return;
        String q = tfSearch.getText().trim().toLowerCase();
        String spec = (String) cbFilterSpec.getSelectedItem();
        String status = (String) cbFilterStatus.getSelectedItem();

        model.setRowCount(0);
        int shown = 0;
        for (Booking b : AppointmentData.BOOKINGS) {
            if (!"All Specializations".equals(spec) && !b.doctor.spec.equals(spec))
                continue;
            if (!"All Status".equals(status) && !b.status.equals(status))
                continue;
            String hay = (b.id + " " + b.patient + " " + b.phone + " " + b.doctor.name + " "
                    + b.date.format(DATE_FMT)).toLowerCase();
            if (!q.isEmpty() && !hay.contains(q))
                continue;

            model.addRow(new Object[] {
                    b.id, b.patient, b.age, b.phone, b.doctor.name, b.doctor.spec,
                    b.date.format(DATE_FMT), b.time, RUPEE + " " + b.doctor.fee, b.status });
            shown++;
        }
        countLabel.setText("Showing " + shown + " of " + AppointmentData.BOOKINGS.size() + " appointments");
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

        // ---- toolbar ----
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel sl = new JLabel("Search:");
        sl.setFont(new Font(FONT, Font.BOLD, 13));
        sl.setForeground(MUTED);

        tfSearch = new JTextField();
        styleField(tfSearch);
        tfSearch.setPreferredSize(new Dimension(260, 40));
        tfSearch.setToolTipText("Search by patient, phone, doctor, ID or date");

        cbFilterSpec = new JComboBox<>();
        cbFilterSpec.addItem("All Specializations");
        for (String s : AppointmentData.SPECS)
            cbFilterSpec.addItem(s);
        styleCombo(cbFilterSpec);
        cbFilterSpec.setPreferredSize(new Dimension(210, 40));

        cbFilterStatus = new JComboBox<>(new String[] { "All Status", "Booked", "Cancelled" });
        styleCombo(cbFilterStatus);
        cbFilterStatus.setPreferredSize(new Dimension(140, 40));

        left.add(sl);
        left.add(tfSearch);
        left.add(cbFilterSpec);
        left.add(cbFilterStatus);

        RoundedButton cancel = new RoundedButton("Cancel Appointment", RED, RED_HOVER, 40);
        cancel.setFont(new Font(FONT, Font.BOLD, 13));
        cancel.setPreferredSize(new Dimension(190, 40));
        cancel.addActionListener(e -> cancelSelected());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(cancel);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        card.add(bar, BorderLayout.NORTH);

        // ---- table ----
        String[] cols = { "ID", "Patient", "Age", "Phone", "Doctor", "Specialization",
                "Date", "Time", "Fee", "Status" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
        table.getColumnModel().getColumn(8).setPreferredWidth(60);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                    boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, row, col);
                l.setBorder(new EmptyBorder(0, 12, 0, 8));
                l.setFont(new Font(FONT, Font.PLAIN, 13));
                if (!sel) {
                    l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 251, 254));
                    l.setForeground(TEXT);
                }
                if (col == 9) {
                    l.setFont(new Font(FONT, Font.BOLD, 13));
                    if (!sel)
                        l.setForeground("Booked".equals(v) ? GREEN : RED);
                }
                return l;
            }
        });

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                    boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(240, 245, 251));
                l.setForeground(MUTED);
                l.setFont(new Font(FONT, Font.BOLD, 12));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                        new EmptyBorder(0, 12, 0, 8)));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);

        countLabel = new JLabel(" ");
        countLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        countLabel.setForeground(MUTED);
        card.add(countLabel, BorderLayout.SOUTH);

        // filters
        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        cbFilterSpec.addActionListener(e -> refresh());
        cbFilterStatus.addActionListener(e -> refresh());

        page.add(card, BorderLayout.CENTER);
        return page;
    }

    // =========================================================
    // LOGIC
    // =========================================================
    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an appointment from the table first.",
                    "MediBook", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        for (Booking b : AppointmentData.BOOKINGS) {
            if (b.id == id) {
                if ("Cancelled".equals(b.status)) {
                    JOptionPane.showMessageDialog(this, "This appointment is already cancelled.");
                    return;
                }
                int r = JOptionPane.showConfirmDialog(this,
                        "Cancel appointment #" + id + " for " + b.patient + "?",
                        "Cancel Appointment", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    b.status = "Cancelled";
                    refresh();
                    if (onChanged != null)
                        onChanged.run();
                }
                return;
            }
        }
    }
}