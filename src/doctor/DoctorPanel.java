package doctor;
 
import database.DB;
 
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
 
public class DoctorPanel extends JPanel {
 
    private JTextField nameField, specializationField, phoneField, emailField;
    private JTextField experienceField, feeField;
    private JComboBox<String> genderBox, statusBox;
 
    // Available days (checkboxes) and time (from / to dropdowns)
    private static final String[] DAYS = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
    private final JCheckBox[] dayChecks = new JCheckBox[DAYS.length];
    private JComboBox<String> fromTimeBox, toTimeBox;
 
    // Specialization suggestions
    private static final String[] DEFAULT_SPECIALIZATIONS = {
            "General Physician", "Cardiologist", "Dermatologist", "Pediatrician",
            "Gynecologist", "Orthopedic", "Neurologist", "Psychiatrist",
            "ENT Specialist", "Ophthalmologist", "Dentist", "Urologist",
            "Gastroenterologist", "Pulmonologist", "Endocrinologist",
            "Diabetologist", "Nephrologist", "Oncologist", "Radiologist",
            "Physiotherapist", "Dietitian", "General Surgeon"
    };
    private final List<String> allSpecializations = new ArrayList<>();
    private JPopupMenu suggestionPopup;
    private JList<String> suggestionList;
    private JScrollPane suggestionScroll;
    private boolean suppressSuggest = false;
 
    // Doctor image
    private JLabel imagePreview;
    private String selectedImagePath;
 
    private static final String FONT = "Segoe UI";
 
    // width of every input box -> controls the width of the whole form (increase / decrease as you like)
    private static final int FIELD_WIDTH = 700;
 
    // Validation patterns
    private static final String PHONE_REGEX = "\\d{10}";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
 
    private static final Color BLUE = new Color(31, 122, 224);
    private static final Color DARK_BLUE = new Color(20, 91, 174);
    private static final Color LIGHT_BLUE = new Color(232, 241, 253);
    private static final Color BG = new Color(243, 246, 251);
    private static final Color TEXT = new Color(35, 45, 60);
    private static final Color MUTED = new Color(110, 120, 135);
    private static final Color BORDER = new Color(214, 222, 233);
 
    public DoctorPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        createUI();
    }
 
    // =====================================================
    // CUSTOM UI HELPERS
    // =====================================================
 
    private static class CardPanel extends JPanel {
        CardPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 4, h = getHeight() - 4;
            g2.setColor(new Color(20, 40, 80, 18));
            g2.fillRoundRect(3, 4, w, h, 18, 18);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
 
        RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }
 
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
 
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 12, 6, 12);
        }
    }
 
    private static class ModernButton extends JButton {
        private final boolean filled;
        private boolean hover = false;
 
        ModernButton(String text, boolean filled) {
            super(text);
            this.filled = filled;
            setFont(new Font(FONT, Font.BOLD, 14));
            setForeground(filled ? Color.WHITE : BLUE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(120, 42));
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (filled) {
                g2.setColor(hover ? DARK_BLUE : BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else {
                g2.setColor(hover ? LIGHT_BLUE : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BLUE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    private void styleInput(JComponent c) {
        c.setFont(new Font(FONT, Font.PLAIN, 13));
        c.setForeground(TEXT);
        c.setBackground(Color.WHITE);
        c.setBorder(new RoundedBorder(BORDER, 12));
        if (c instanceof JTextField) {
            ((JTextField) c).setCaretColor(BLUE);
        }
        c.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                c.setBorder(new RoundedBorder(BLUE, 12));
            }
 
            @Override
            public void focusLost(FocusEvent e) {
                c.setBorder(new RoundedBorder(BORDER, 12));
            }
        });
    }
 
    /** Allows only digits, with a maximum length (used for the phone field). */
    private static class DigitsOnlyFilter extends DocumentFilter {
        private final int maxLength;
 
        DigitsOnlyFilter(int maxLength) {
            this.maxLength = maxLength;
        }
 
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }
 
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
                throws BadLocationException {
            if (text == null)
                text = "";
            if (!text.matches("\\d*"))
                return; // reject letters / symbols
            int newLength = fb.getDocument().getLength() - length + text.length();
            if (newLength <= maxLength) {
                super.replace(fb, offset, length, text, attr);
            }
        }
    }
 
    /** Panel that follows the scroll viewport's width (so the form stays centred). */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel(LayoutManager lm) {
            super(lm);
        }
 
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
 
        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 24;
        }
 
        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return 120;
        }
 
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
 
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
 
    // =====================================================
    // UI
    // =====================================================
 
    private void createUI() {
 
        // ---------- FORM CARD ----------
        JPanel formCard = new CardPanel(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(22, 26, 26, 30));
 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
 
        nameField = new JTextField();
        specializationField = new JTextField();
        phoneField = new JTextField();
        emailField = new JTextField();
        experienceField = new JTextField();
        feeField = new JTextField();
 
        // phone: digits only, max 10
        ((AbstractDocument) phoneField.getDocument()).setDocumentFilter(new DigitsOnlyFilter(10));
        phoneField.setToolTipText("10 digit mobile number");
 
        genderBox = new JComboBox<>(new String[] { "Male", "Female", "Other" });
        statusBox = new JComboBox<>(new String[] { "Active", "Inactive" });
 
        // specialization suggestions (e.g. "General Physician")
        loadSpecializations();
        setupSpecializationSuggestions();
 
        // available days: one checkbox per day
        JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        daysPanel.setOpaque(false);
        for (int i = 0; i < DAYS.length; i++) {
            JCheckBox cb = new JCheckBox(DAYS[i]);
            cb.setFont(new Font(FONT, Font.PLAIN, 13));
            cb.setForeground(TEXT);
            cb.setOpaque(false);
            cb.setFocusPainted(false);
            cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dayChecks[i] = cb;
            daysPanel.add(cb);
        }
        // sensible default: Mon - Sat
        for (int i = 0; i < 6; i++)
            dayChecks[i].setSelected(true);
 
        // available time: From / To dropdowns (30 minute steps)
        String[] slots = buildTimeSlots();
        fromTimeBox = new JComboBox<>(slots);
        toTimeBox = new JComboBox<>(slots);
        styleInput(fromTimeBox);
        styleInput(toTimeBox);
        fromTimeBox.setSelectedItem("10:00 AM");
        toTimeBox.setSelectedItem("05:00 PM");
 
        JPanel timePanel = new JPanel(new GridLayout(1, 2, 12, 0));
        timePanel.setOpaque(false);
        timePanel.add(fromTimeBox);
        timePanel.add(toTimeBox);
 
        // image preview
        imagePreview = new JLabel("No Image", SwingConstants.CENTER);
        imagePreview.setFont(new Font(FONT, Font.BOLD, 12));
        imagePreview.setForeground(MUTED);
        imagePreview.setBackground(new Color(248, 250, 253));
        imagePreview.setOpaque(true);
        imagePreview.setPreferredSize(new Dimension(120, 120));
        imagePreview.setMaximumSize(new Dimension(120, 120));
        imagePreview.setBorder(new LineBorder(BORDER, 1, true));
 
        JButton chooseImageButton = new ModernButton("Choose Image", false);
        chooseImageButton.setPreferredSize(new Dimension(120, 38));
        chooseImageButton.setMaximumSize(new Dimension(120, 38));
        chooseImageButton.addActionListener(e -> chooseDoctorImage());
 
        JPanel imagePanel = new JPanel();
        imagePanel.setOpaque(false);
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
        imagePreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooseImageButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        imagePanel.add(imagePreview);
        imagePanel.add(Box.createVerticalStrut(8));
        imagePanel.add(chooseImageButton);
 
        JLabel formTitle = new JLabel("Add New Doctor");
        formTitle.setFont(new Font(FONT, Font.BOLD, 20));
        formTitle.setForeground(TEXT);
        formTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
 
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        formCard.add(formTitle, gbc);
 
        int row = 0;
        addField(formCard, gbc, row++, "Doctor Name", nameField);
        addField(formCard, gbc, row++, "Specialization (type to see suggestions)", specializationField);
        addField(formCard, gbc, row++, "Phone (10 digits)", phoneField);
        addField(formCard, gbc, row++, "Email", emailField);
        addField(formCard, gbc, row++, "Gender", genderBox);
        addField(formCard, gbc, row++, "Experience (Years)", experienceField);
        addField(formCard, gbc, row++, "Consultation Fee", feeField);
        addCustomField(formCard, gbc, row++, "Available Days (select the days doctor is available)", daysPanel);
        addCustomField(formCard, gbc, row++, "Available Time (From  -  To)", timePanel);
        addField(formCard, gbc, row++, "Status", statusBox);
 
        // image field
        gbc.gridx = 0;
        gbc.gridy = 1 + row * 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 4, 4, 4);
 
        JLabel imageLabel = new JLabel("Doctor Image");
        imageLabel.setFont(new Font(FONT, Font.BOLD, 12));
        imageLabel.setForeground(MUTED);
        formCard.add(imageLabel, gbc);
 
        gbc.gridy = 2 + row * 2;
        gbc.insets = new Insets(0, 4, 4, 4);
        formCard.add(imagePanel, gbc);
 
        row++;
 
        // buttons
        JButton addButton = new ModernButton("Add Doctor", true);
        JButton clearButton = new ModernButton("Clear", false);
 
        addButton.setPreferredSize(new Dimension(160, 42));
        clearButton.setPreferredSize(new Dimension(160, 42));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(addButton);
        buttonRow.add(Box.createHorizontalStrut(10));
        buttonRow.add(clearButton);
 
        gbc.gridx = 0;
        gbc.gridy = 1 + row * 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(18, 4, 4, 4);
        formCard.add(buttonRow, gbc);
 
        addButton.addActionListener(e -> addDoctor());
        clearButton.addActionListener(e -> clearForm());
 
        // ---------- CENTER THE FORM CARD ----------
        JPanel wrapper = new ScrollablePanel(new GridBagLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(25, 30, 25, 30));
 
        GridBagConstraints wg = new GridBagConstraints();
        wg.gridx = 0;
        wg.gridy = 0;
        wg.anchor = GridBagConstraints.NORTH;
        wg.weightx = 1;
        wg.weighty = 1;
        // no fill: the form card keeps its own width (set by FIELD_WIDTH) and stays centred
        wrapper.add(formCard, wg);
 
        JScrollPane scroll = new JScrollPane(
                wrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
 
        add(scroll, BorderLayout.CENTER);
    }
 
    // =====================================================
    // TIME SLOTS + DAYS HELPERS
    // =====================================================
 
    /** 06:00 AM ... 11:30 PM in 30 minute steps. */
    private String[] buildTimeSlots() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        List<String> list = new ArrayList<>();
        LocalTime t = LocalTime.of(6, 0);
        while (t.isBefore(LocalTime.of(23, 31)) && !t.equals(LocalTime.MIDNIGHT.minusMinutes(1))) {
            list.add(t.format(fmt));
            LocalTime next = t.plusMinutes(30);
            if (next.isBefore(t))
                break; // wrapped past midnight
            t = next;
        }
        return list.toArray(new String[0]);
    }
 
    /** Returns e.g. "Tue, Sun" in Mon..Sun order, or "" when nothing is selected. */
    private String getSelectedDays() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < DAYS.length; i++) {
            if (dayChecks[i].isSelected())
                selected.add(DAYS[i]);
        }
        return String.join(", ", selected);
    }
 
    // =====================================================
    // SPECIALIZATION SUGGESTIONS
    // =====================================================
 
    /** Default list + specializations already saved in the doctors table. */
    private void loadSpecializations() {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String s : DEFAULT_SPECIALIZATIONS)
            set.add(s);
 
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT DISTINCT specialization FROM doctors WHERE specialization IS NOT NULL");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String s = rs.getString(1);
                if (s != null && !s.trim().isEmpty())
                    set.add(s.trim());
            }
        } catch (Exception ignored) {
            // DB not reachable -> default list is still enough
        }
 
        allSpecializations.clear();
        allSpecializations.addAll(set);
    }
 
    private void setupSpecializationSuggestions() {
        suggestionList = new JList<>();
        suggestionList.setFont(new Font(FONT, Font.PLAIN, 13));
        suggestionList.setForeground(TEXT);
        suggestionList.setSelectionBackground(LIGHT_BLUE);
        suggestionList.setSelectionForeground(DARK_BLUE);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFixedCellHeight(28);
        suggestionList.setFocusable(false);
        suggestionList.setBorder(new EmptyBorder(2, 6, 2, 6));
        suggestionList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
 
        suggestionList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = suggestionList.locationToIndex(e.getPoint());
                if (idx >= 0)
                    suggestionList.setSelectedIndex(idx);
            }
        });
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String value = suggestionList.getSelectedValue();
                if (value != null)
                    pickSuggestion(value);
            }
        });
 
        suggestionScroll = new JScrollPane(suggestionList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionScroll.setBorder(null);
        suggestionScroll.setFocusable(false);
 
        suggestionPopup = new JPopupMenu();
        suggestionPopup.setFocusable(false);
        suggestionPopup.setBorder(new LineBorder(BORDER, 1));
        suggestionPopup.add(suggestionScroll);
 
        specializationField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSuggestions());
            }
 
            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSuggestions());
            }
 
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
 
        specializationField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionPopup.isVisible())
                    return;
                int size = suggestionList.getModel().getSize();
                int idx = suggestionList.getSelectedIndex();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        suggestionList.setSelectedIndex(Math.min(idx + 1, size - 1));
                        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        suggestionList.setSelectedIndex(Math.max(idx - 1, 0));
                        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                        if (idx >= 0) {
                            pickSuggestion(suggestionList.getSelectedValue());
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestionPopup.setVisible(false);
                        e.consume();
                        break;
                    default:
                        break;
                }
            }
        });
 
        specializationField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                suggestionPopup.setVisible(false);
            }
        });
    }
 
    private void updateSuggestions() {
        if (suppressSuggest || !specializationField.isShowing() || !specializationField.hasFocus()) {
            return;
        }
 
        String typed = specializationField.getText().trim().toLowerCase();
        if (typed.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }
 
        List<String> matches = new ArrayList<>();
        for (String s : allSpecializations) {
            if (s.toLowerCase().contains(typed))
                matches.add(s);
        }
 
        // nothing to suggest, or the user already typed the exact value
        if (matches.isEmpty() || (matches.size() == 1 && matches.get(0).equalsIgnoreCase(typed))) {
            suggestionPopup.setVisible(false);
            return;
        }
 
        suggestionList.setListData(matches.toArray(new String[0]));
        suggestionList.setSelectedIndex(-1);
 
        int rows = Math.min(matches.size(), 6);
        suggestionScroll.setPreferredSize(new Dimension(specializationField.getWidth() - 2, rows * 28 + 6));
        suggestionPopup.pack();
        suggestionPopup.show(specializationField, 0, specializationField.getHeight());
    }
 
    private void pickSuggestion(String value) {
        suppressSuggest = true;
        specializationField.setText(value);
        suppressSuggest = false;
        suggestionPopup.setVisible(false);
        specializationField.requestFocusInWindow();
    }
 
    // =====================================================
    // CHOOSE IMAGE
    // =====================================================
 
    private void chooseDoctorImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Doctor Image");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "webp"));
 
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
 
        File file = chooser.getSelectedFile();
        selectedImagePath = file.getAbsolutePath();
        showImagePreview(selectedImagePath);
    }
 
    private void showImagePreview(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            imagePreview.setIcon(null);
            imagePreview.setText("No Image");
            return;
        }
 
        ImageIcon original = new ImageIcon(imagePath);
        if (original.getIconWidth() <= 0) {
            imagePreview.setIcon(null);
            imagePreview.setText("Invalid Image");
            return;
        }
 
        Image image = original.getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH);
        imagePreview.setText("");
        imagePreview.setIcon(new ImageIcon(image));
    }
 
    // =====================================================
    // COPY IMAGE TO PROJECT
    // =====================================================
 
    private String saveDoctorImage(String sourcePath) throws Exception {
        if (sourcePath == null || sourcePath.isEmpty())
            return null;
 
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists())
            return null;
 
        String projectPath = System.getProperty("user.dir");
        Path imageDirectory = Path.of(projectPath, "images", "doctors");
        Files.createDirectories(imageDirectory);
 
        String originalName = sourceFile.getName();
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0)
            extension = originalName.substring(dot);
 
        String safeName = "doctor_" + System.currentTimeMillis() + extension;
        Path destination = imageDirectory.resolve(safeName);
 
        Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
 
        // Stored in DB as relative path: images/doctors/doctor_123456.jpg
        return "images/doctors/" + safeName;
    }
 
    // =====================================================
    // FORM FIELD HELPERS
    // =====================================================
 
    private void addField(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, JComponent component) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
 
        gbc.gridy = 1 + row * 2;
        gbc.insets = new Insets(8, 4, 3, 4);
 
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(FONT, Font.BOLD, 12));
        label.setForeground(MUTED);
        panel.add(label, gbc);
 
        gbc.gridy = 2 + row * 2;
        gbc.insets = new Insets(0, 4, 4, 4);
 
        styleInput(component);
        component.setPreferredSize(new Dimension(FIELD_WIDTH, 38));
        panel.add(component, gbc);
    }
 
    /** Same as addField, but the component is already styled (panels with checkboxes / dropdowns). */
    private void addCustomField(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, JComponent component) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
 
        gbc.gridy = 1 + row * 2;
        gbc.insets = new Insets(8, 4, 3, 4);
 
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(FONT, Font.BOLD, 12));
        label.setForeground(MUTED);
        panel.add(label, gbc);
 
        gbc.gridy = 2 + row * 2;
        gbc.insets = new Insets(0, 4, 4, 4);
 
        component.setPreferredSize(new Dimension(FIELD_WIDTH, 38));
        panel.add(component, gbc);
    }
 
    // =====================================================
    // ADD DOCTOR
    // =====================================================
 
    private void addDoctor() {
 
        String name = nameField.getText().trim();
        String specialization = specializationField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String gender = genderBox.getSelectedItem().toString();
        String experienceText = experienceField.getText().trim();
        String feeText = feeField.getText().trim();
        String days = getSelectedDays();
        String time = fromTimeBox.getSelectedItem() + " - " + toTimeBox.getSelectedItem();
        String status = statusBox.getSelectedItem().toString();
 
        // ---------- VALIDATION ----------
        if (name.isEmpty() || specialization.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Doctor name and specialization are required.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        if (!phone.matches(PHONE_REGEX)) {
            JOptionPane.showMessageDialog(this,
                    "Phone number must be exactly 10 digits (numbers only).",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            phoneField.requestFocus();
            return;
        }
 
        if (!email.matches(EMAIL_REGEX)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address (e.g. name@example.com).",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            emailField.requestFocus();
            return;
        }
 
        int experience = 0;
        if (!experienceText.isEmpty()) {
            try {
                experience = Integer.parseInt(experienceText);
                if (experience < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Experience must be a valid number.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
 
        double fee = 0;
        if (!feeText.isEmpty()) {
            try {
                fee = Double.parseDouble(feeText);
                if (fee < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Consultation fee must be a valid number.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
 
        if (days.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one available day.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        if (fromTimeBox.getSelectedIndex() >= toTimeBox.getSelectedIndex()) {
            JOptionPane.showMessageDialog(this,
                    "Available 'To' time must be later than the 'From' time.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        // ---------- IMAGE ----------
        String imagePath = null;
        try {
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                imagePath = saveDoctorImage(selectedImagePath);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save doctor image.\n" + e.getMessage(),
                    "Image Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
 
        // ---------- INSERT ----------
        String sql = """
                INSERT INTO doctors
                (
                    name,
                    specialization,
                    phone,
                    email,
                    image_path,
                    gender,
                    experience,
                    consultation_fee,
                    available_days,
                    available_time,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
 
        try (Connection con = DB.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
 
            ps.setString(1, name);
            ps.setString(2, specialization);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setString(5, imagePath);
            ps.setString(6, gender);
            ps.setInt(7, experience);
            ps.setDouble(8, fee);
            ps.setString(9, days);
            ps.setString(10, time);
            ps.setString(11, status);
 
            ps.executeUpdate();
 
            JOptionPane.showMessageDialog(this,
                    "Doctor added successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
 
            clearForm();
            loadSpecializations(); // new specialization shows up in suggestions next time
            DoctorEvents.fireChanged(); // All Doctors + other connected panels refresh
 
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database Error:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
 
    // =====================================================
    // CLEAR FORM
    // =====================================================
 
    private void clearForm() {
        nameField.setText("");
        suppressSuggest = true;
        specializationField.setText("");
        suppressSuggest = false;
        suggestionPopup.setVisible(false);
        phoneField.setText("");
        emailField.setText("");
        experienceField.setText("");
        feeField.setText("");
        genderBox.setSelectedIndex(0);
        statusBox.setSelectedIndex(0);
 
        // days back to default Mon - Sat, time back to 10:00 AM - 05:00 PM
        for (int i = 0; i < DAYS.length; i++)
            dayChecks[i].setSelected(i < 6);
        fromTimeBox.setSelectedItem("10:00 AM");
        toTimeBox.setSelectedItem("05:00 PM");
 
        selectedImagePath = null;
        imagePreview.setIcon(null);
        imagePreview.setText("No Image");
    }
 
    // =====================================================
    // REFRESH (kept so other panels calling it still compile)
    // =====================================================
 
    public void refresh() {
        loadSpecializations();
    }
}
 


