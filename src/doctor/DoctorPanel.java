package doctor;

import database.DB;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class DoctorPanel extends JPanel {

    private JTextField nameField;
    private JTextField specializationField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField experienceField;
    private JTextField feeField;
    private JTextField daysField;
    private JTextField timeField;

    private JComboBox<String> genderBox;
    private JComboBox<String> statusBox;

    private JTable doctorTable;
    private DefaultTableModel tableModel;

    private JLabel countBadge;

    // Doctor image
    private JLabel imagePreview;
    private String selectedImagePath;

    private static final String FONT = "Segoe UI";

    private static final Color BLUE = new Color(31, 122, 224);
    private static final Color DARK_BLUE = new Color(20, 91, 174);
    private static final Color LIGHT_BLUE = new Color(232, 241, 253);
    private static final Color BG = new Color(243, 246, 251);
    private static final Color TEXT = new Color(35, 45, 60);
    private static final Color MUTED = new Color(110, 120, 135);
    private static final Color BORDER = new Color(214, 222, 233);
    private static final Color ROW_ALT = new Color(248, 250, 254);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color GREEN_BG = new Color(220, 246, 230);
    private static final Color RED = new Color(220, 53, 69);
    private static final Color RED_BG = new Color(252, 226, 229);

    public DoctorPanel() {

        setLayout(new BorderLayout());
        setBackground(BG);

        createUI();
        loadDoctors();
    }

    // ============================
    // CUSTOM UI HELPERS
    // ============================

    /**
     * White rounded card with a soft shadow.
     */
    private static class CardPanel extends JPanel {

        CardPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = getWidth() - 4;
            int h = getHeight() - 4;

            g2.setColor(new Color(20, 40, 80, 18));
            g2.fillRoundRect(3, 4, w, h, 18, 18);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 18, 18);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Blue gradient banner.
     */
    private static class GradientPanel extends JPanel {

        GradientPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setPaint(
                    new GradientPaint(
                            0,
                            0,
                            BLUE,
                            getWidth(),
                            0,
                            DARK_BLUE
                    )
            );

            g2.fillRect(
                    0,
                    0,
                    getWidth(),
                    getHeight()
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Rounded outline border for inputs.
     */
    private static class RoundedBorder extends AbstractBorder {

        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {

            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(
                Component c,
                Graphics g,
                int x,
                int y,
                int w,
                int h
        ) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(color);

            g2.drawRoundRect(
                    x,
                    y,
                    w - 1,
                    h - 1,
                    radius,
                    radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {

            return new Insets(
                    6,
                    12,
                    6,
                    12
            );
        }
    }

    /**
     * Small rounded pill label.
     */
    private static class PillLabel extends JLabel {

        private Color pillColor;

        PillLabel(
                String text,
                Color bg,
                Color fg
        ) {

            super(
                    text,
                    SwingConstants.CENTER
            );

            this.pillColor = bg;

            setForeground(fg);

            setFont(
                    new Font(
                            FONT,
                            Font.BOLD,
                            12
                    )
            );

            setBorder(
                    new EmptyBorder(
                            4,
                            12,
                            4,
                            12
                    )
            );

            setOpaque(false);
        }

        void setPillColor(Color c) {
            this.pillColor = c;
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(pillColor);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    getHeight(),
                    getHeight()
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Rounded button with hover effect.
     */
    private static class ModernButton extends JButton {

        private final boolean filled;
        private boolean hover = false;

        ModernButton(
                String text,
                boolean filled
        ) {

            super(text);

            this.filled = filled;

            setFont(
                    new Font(
                            FONT,
                            Font.BOLD,
                            14
                    )
            );

            setForeground(
                    filled
                            ? Color.WHITE
                            : BLUE
            );

            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            setPreferredSize(
                    new Dimension(
                            120,
                            42
                    )
            );

            addMouseListener(
                    new MouseAdapter() {

                        @Override
                        public void mouseEntered(
                                MouseEvent e
                        ) {

                            hover = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(
                                MouseEvent e
                        ) {

                            hover = false;
                            repaint();
                        }
                    }
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (filled) {

                g2.setColor(
                        hover
                                ? DARK_BLUE
                                : BLUE
                );

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        12,
                        12
                );

            } else {

                g2.setColor(
                        hover
                                ? LIGHT_BLUE
                                : Color.WHITE
                );

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        12,
                        12
                );

                g2.setColor(BLUE);

                g2.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        12,
                        12
                );
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private void styleInput(JComponent c) {

        c.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        13
                )
        );

        c.setForeground(TEXT);
        c.setBackground(Color.WHITE);

        c.setBorder(
                new RoundedBorder(
                        BORDER,
                        12
                )
        );

        if (c instanceof JTextField) {

            ((JTextField) c)
                    .setCaretColor(BLUE);
        }

        c.addFocusListener(
                new FocusAdapter() {

                    @Override
                    public void focusGained(
                            FocusEvent e
                    ) {

                        c.setBorder(
                                new RoundedBorder(
                                        BLUE,
                                        12
                                )
                        );
                    }

                    @Override
                    public void focusLost(
                            FocusEvent e
                    ) {

                        c.setBorder(
                                new RoundedBorder(
                                        BORDER,
                                        12
                                )
                        );
                    }
                }
        );
    }

    // ============================
    // UI
    // ============================

    private void createUI() {

        // ============================
        // HEADER
        // ============================

        JPanel header =
                new GradientPanel(
                        new BorderLayout()
                );

        header.setBorder(
                new EmptyBorder(
                        26,
                        30,
                        26,
                        30
                )
        );

        JLabel title =
                new JLabel(
                        "Doctor Management"
                );

        title.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        28
                )
        );

        title.setForeground(
                Color.WHITE
        );

        JLabel subtitle =
                new JLabel(
                        "Add and manage doctors"
                );

        subtitle.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        14
                )
        );

        subtitle.setForeground(
                new Color(
                        214,
                        230,
                        250
                )
        );

        JPanel titlePanel =
                new JPanel();

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        titlePanel.setOpaque(false);

        titlePanel.add(title);

        titlePanel.add(
                Box.createVerticalStrut(5)
        );

        titlePanel.add(subtitle);

        header.add(
                titlePanel,
                BorderLayout.WEST
        );

        PillLabel badge =
                new PillLabel(
                        "0 Doctors",
                        Color.WHITE,
                        DARK_BLUE
                );

        badge.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        13
                )
        );

        countBadge = badge;

        JPanel badgeWrap =
                new JPanel(
                        new GridBagLayout()
                );

        badgeWrap.setOpaque(false);

        badgeWrap.add(badge);

        header.add(
                badgeWrap,
                BorderLayout.EAST
        );

        add(
                header,
                BorderLayout.NORTH
        );

        // ============================
        // MAIN
        // ============================

        JPanel main =
                new JPanel(
                        new BorderLayout(
                                22,
                                22
                        )
                );

        main.setBackground(BG);

        main.setBorder(
                new EmptyBorder(
                        25,
                        30,
                        25,
                        30
                )
        );

        // ============================
        // FORM
        // ============================

        JPanel formCard =
                new CardPanel(
                        new GridBagLayout()
                );

        formCard.setBorder(
                new EmptyBorder(
                        22,
                        22,
                        26,
                        26
                )
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        4,
                        4,
                        4,
                        4
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        nameField =
                new JTextField();

        specializationField =
                new JTextField();

        phoneField =
                new JTextField();

        emailField =
                new JTextField();

        experienceField =
                new JTextField();

        feeField =
                new JTextField();

        daysField =
                new JTextField();

        timeField =
                new JTextField();

        genderBox =
                new JComboBox<>(
                        new String[]{
                                "Male",
                                "Female",
                                "Other"
                        }
                );

        statusBox =
                new JComboBox<>(
                        new String[]{
                                "Active",
                                "Inactive"
                        }
                );

        // ============================
        // IMAGE PREVIEW
        // ============================

        imagePreview =
                new JLabel(
                        "No Image",
                        SwingConstants.CENTER
                );

        imagePreview.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        12
                )
        );

        imagePreview.setForeground(
                MUTED
        );

        imagePreview.setBackground(
                new Color(
                        248,
                        250,
                        253
                )
        );

        imagePreview.setOpaque(true);

        imagePreview.setPreferredSize(
                new Dimension(
                        120,
                        120
                )
        );

        imagePreview.setBorder(
                new LineBorder(
                        BORDER,
                        1,
                        true
                )
        );

        JButton chooseImageButton =
                new ModernButton(
                        "Choose Image",
                        false
                );

        chooseImageButton.setPreferredSize(
                new Dimension(
                        120,
                        38
                )
        );

        chooseImageButton.addActionListener(
                e -> chooseDoctorImage()
        );

        JPanel imagePanel =
                new JPanel();

        imagePanel.setOpaque(false);

        imagePanel.setLayout(
                new BoxLayout(
                        imagePanel,
                        BoxLayout.Y_AXIS
                )
        );

        imagePreview.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        chooseImageButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        imagePanel.add(imagePreview);

        imagePanel.add(
                Box.createVerticalStrut(8)
        );

        imagePanel.add(
                chooseImageButton
        );

        JLabel formTitle =
                new JLabel(
                        "Doctor Details"
                );

        formTitle.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        18
                )
        );

        formTitle.setForeground(TEXT);

        formTitle.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        8,
                        0
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;

        formCard.add(
                formTitle,
                gbc
        );

        int row = 0;

        addField(
                formCard,
                gbc,
                row++,
                "Doctor Name",
                nameField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Specialization",
                specializationField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Phone",
                phoneField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Email",
                emailField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Gender",
                genderBox
        );

        addField(
                formCard,
                gbc,
                row++,
                "Experience (Years)",
                experienceField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Consultation Fee",
                feeField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Available Days",
                daysField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Available Time",
                timeField
        );

        addField(
                formCard,
                gbc,
                row++,
                "Status",
                statusBox
        );

        // ============================
        // IMAGE FIELD
        // ============================

        gbc.gridx = 0;
        gbc.gridy = 1 + row * 2;
        gbc.gridwidth = 1;

        gbc.insets =
                new Insets(
                        8,
                        4,
                        4,
                        4
                );

        JLabel imageLabel =
                new JLabel(
                        "Doctor Image"
                );

        imageLabel.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        12
                )
        );

        imageLabel.setForeground(MUTED);

        formCard.add(
                imageLabel,
                gbc
        );

        gbc.gridy =
                2 + row * 2;

        gbc.insets =
                new Insets(
                        0,
                        4,
                        4,
                        4
                );

        formCard.add(
                imagePanel,
                gbc
        );

        row++;

        // ============================
        // BUTTONS
        // ============================

        JButton addButton =
                new ModernButton(
                        "Add Doctor",
                        true
                );

        JButton clearButton =
                new ModernButton(
                        "Clear",
                        false
                );

        JPanel buttonRow =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                10,
                                0
                        )
                );

        buttonRow.setOpaque(false);

        buttonRow.add(addButton);
        buttonRow.add(clearButton);

        gbc.gridx = 0;

        gbc.gridy =
                1 + row * 2;

        gbc.gridwidth = 1;

        gbc.insets =
                new Insets(
                        18,
                        4,
                        4,
                        4
                );

        formCard.add(
                buttonRow,
                gbc
        );

        addButton.addActionListener(
                e -> addDoctor()
        );

        clearButton.addActionListener(
                e -> clearForm()
        );

        // Push everything to the top

        gbc.gridy =
                2 + row * 2;

        gbc.weighty = 1;

        gbc.fill =
                GridBagConstraints.BOTH;

        formCard.add(
                Box.createGlue(),
                gbc
        );

        JScrollPane formScroll =
                new JScrollPane(
                        formCard,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                );

        formScroll.setBorder(null);

        formScroll.setOpaque(false);

        formScroll.getViewport()
                .setOpaque(false);

        formScroll
                .getVerticalScrollBar()
                .setUnitIncrement(16);

        formScroll.setPreferredSize(
                new Dimension(
                        390,
                        100
                )
        );

        // ============================
        // TABLE
        // ============================

        String[] columns = {

                "ID",
                "Name",
                "Specialization",
                "Phone",
                "Email",
                "Gender",
                "Experience",
                "Fee",
                "Days",
                "Time",
                "Image",
                "Status"
        };

        tableModel =
                new DefaultTableModel(
                        columns,
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

        tableModel.addTableModelListener(
                e -> {

                    int n =
                            tableModel
                                    .getRowCount();

                    countBadge.setText(
                            n
                                    + (
                                    n == 1
                                            ? " Doctor"
                                            : " Doctors"
                            )
                    );
                }
        );

        doctorTable =
                new JTable(
                        tableModel
                );

        doctorTable.setRowHeight(40);

        doctorTable.setFont(
                new Font(
                        FONT,
                        Font.PLAIN,
                        13
                )
        );

        doctorTable.setForeground(TEXT);

        doctorTable.setShowGrid(false);

        doctorTable.setIntercellSpacing(
                new Dimension(
                        0,
                        0
                )
        );

        doctorTable.setFillsViewportHeight(true);

        doctorTable.setSelectionBackground(
                LIGHT_BLUE
        );

        doctorTable.setSelectionForeground(
                DARK_BLUE
        );

        doctorTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        );

        doctorTable.getTableHeader()
                .setFont(
                        new Font(
                                FONT,
                                Font.BOLD,
                                13
                        )
                );

        doctorTable.getTableHeader()
                .setReorderingAllowed(false);

        doctorTable.getTableHeader()
                .setPreferredSize(
                        new Dimension(
                                100,
                                42
                        )
                );

        doctorTable.getTableHeader()
                .setDefaultRenderer(
                        new HeaderRenderer()
                );

        doctorTable.setDefaultRenderer(
                Object.class,
                new RowRenderer()
        );

        doctorTable.getColumnModel()
                .getColumn(11)
                .setCellRenderer(
                        new StatusRenderer()
                );

        int[] widths = {

                45,
                140,
                130,
                100,
                170,
                70,
                90,
                80,
                150,
                180,
                150,
                90
        };

        for (
                int i = 0;
                i < widths.length;
                i++
        ) {

            doctorTable
                    .getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(
                            widths[i]
                    );
        }

        JScrollPane scrollPane =
                new JScrollPane(
                        doctorTable
                );

        scrollPane.setBorder(
                new LineBorder(
                        BORDER,
                        1,
                        true
                )
        );

        scrollPane.getViewport()
                .setBackground(
                        Color.WHITE
                );

        scrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(16);

        JPanel tableCard =
                new CardPanel(
                        new BorderLayout()
                );

        tableCard.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        24,
                        24
                )
        );

        JLabel tableTitle =
                new JLabel(
                        "Doctors List"
                );

        tableTitle.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        18
                )
        );

        tableTitle.setForeground(TEXT);

        tableTitle.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        14,
                        0
                )
        );

        tableCard.add(
                tableTitle,
                BorderLayout.NORTH
        );

        tableCard.add(
                scrollPane,
                BorderLayout.CENTER
        );

        // ============================
        // LAYOUT
        // ============================

        main.add(
                formScroll,
                BorderLayout.WEST
        );

        main.add(
                tableCard,
                BorderLayout.CENTER
        );

        add(
                main,
                BorderLayout.CENTER
        );
    }

    // ============================
    // CHOOSE IMAGE
    // ============================

    private void chooseDoctorImage() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select Doctor Image"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Image Files",
                        "jpg",
                        "jpeg",
                        "png",
                        "webp"
                );

        chooser.setFileFilter(filter);

        int result =
                chooser.showOpenDialog(
                        this
                );

        if (
                result
                        != JFileChooser.APPROVE_OPTION
        ) {

            return;
        }

        File file =
                chooser.getSelectedFile();

        selectedImagePath =
                file.getAbsolutePath();

        showImagePreview(
                selectedImagePath
        );
    }

    // ============================
    // IMAGE PREVIEW
    // ============================

    private void showImagePreview(
            String imagePath
    ) {

        if (
                imagePath == null
                        || imagePath.isEmpty()
        ) {

            imagePreview.setIcon(null);

            imagePreview.setText(
                    "No Image"
            );

            return;
        }

        ImageIcon original =
                new ImageIcon(
                        imagePath
                );

        if (
                original.getIconWidth()
                        <= 0
        ) {

            imagePreview.setIcon(null);

            imagePreview.setText(
                    "Invalid Image"
            );

            return;
        }

        Image image =
                original.getImage()
                        .getScaledInstance(
                                110,
                                110,
                                Image.SCALE_SMOOTH
                        );

        imagePreview.setText("");

        imagePreview.setIcon(
                new ImageIcon(image)
        );
    }

    // ============================
    // COPY IMAGE TO PROJECT
    // ============================

    private String saveDoctorImage(
            String sourcePath
    ) throws Exception {

        if (
                sourcePath == null
                        || sourcePath.isEmpty()
        ) {

            return null;
        }

        File sourceFile =
                new File(sourcePath);

        if (!sourceFile.exists()) {

            return null;
        }

        // Project root
        String projectPath =
                System.getProperty(
                        "user.dir"
                );

        Path imageDirectory =
                Path.of(
                        projectPath,
                        "images",
                        "doctors"
                );

        Files.createDirectories(
                imageDirectory
        );

        String originalName =
                sourceFile.getName();

        String extension = "";

        int dot =
                originalName.lastIndexOf('.');

        if (dot >= 0) {

            extension =
                    originalName.substring(
                            dot
                    );
        }

        String safeName =
                "doctor_"
                        + System.currentTimeMillis()
                        + extension;

        Path destination =
                imageDirectory.resolve(
                        safeName
                );

        Files.copy(
                sourceFile.toPath(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        /*
         * Store relative path in DB.
         *
         * Example:
         * images/doctors/doctor_123456.jpg
         */

        return "images/doctors/"
                + safeName;
    }

    // ============================
    // TABLE RENDERERS
    // ============================

    private static class HeaderRenderer
            extends DefaultTableCellRenderer {

        HeaderRenderer() {

            setOpaque(true);

            setBackground(BLUE);

            setForeground(
                    Color.WHITE
            );

            setFont(
                    new Font(
                            FONT,
                            Font.BOLD,
                            13
                    )
            );

            setBorder(
                    new EmptyBorder(
                            0,
                            12,
                            0,
                            12
                    )
            );

            setHorizontalAlignment(
                    SwingConstants.LEFT
            );
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            super.getTableCellRendererComponent(
                    table,
                    value,
                    false,
                    false,
                    row,
                    column
            );

            setBackground(BLUE);

            setForeground(
                    Color.WHITE
            );

            setBorder(
                    new EmptyBorder(
                            0,
                            12,
                            0,
                            12
                    )
            );

            return this;
        }
    }

    private static class RowRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            String text =
                    value == null
                            ? ""
                            : value.toString();

            if (
                    column == 7
                            && value instanceof Double
            ) {

                text =
                        String.format(
                                "\u20B9 %.2f",
                                (Double) value
                        );

            } else if (
                    column == 6
                            && value instanceof Integer
            ) {

                text =
                        value
                                + " yrs";
            }

            super.getTableCellRendererComponent(
                    table,
                    text,
                    isSelected,
                    false,
                    row,
                    column
            );

            setBorder(
                    new EmptyBorder(
                            0,
                            12,
                            0,
                            12
                    )
            );

            if (!isSelected) {

                setBackground(
                        row % 2 == 0
                                ? Color.WHITE
                                : ROW_ALT
                );

                setForeground(TEXT);
            }

            if (column == 0) {

                setForeground(
                        isSelected
                                ? DARK_BLUE
                                : MUTED
                );
            }

            setFont(
                    new Font(
                            FONT,
                            column == 1
                                    ? Font.BOLD
                                    : Font.PLAIN,
                            13
                    )
            );

            return this;
        }
    }

    private static class StatusRenderer
            implements TableCellRenderer {

        private final JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                12,
                                7
                        )
                );

        private final PillLabel pill =
                new PillLabel(
                        "",
                        GREEN_BG,
                        GREEN
                );

        StatusRenderer() {

            panel.add(pill);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {

            String status =
                    value == null
                            ? ""
                            : value.toString();

            boolean active =
                    "Active".equalsIgnoreCase(
                            status
                    );

            pill.setText(status);

            pill.setForeground(
                    active
                            ? GREEN
                            : RED
            );

            pill.setPillColor(
                    active
                            ? GREEN_BG
                            : RED_BG
            );

            panel.setBackground(
                    isSelected
                            ? LIGHT_BLUE
                            : (
                            row % 2 == 0
                                    ? Color.WHITE
                                    : ROW_ALT
                    )
            );

            return panel;
        }
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JComponent component
    ) {

        gbc.gridx = 0;

        gbc.gridwidth = 1;

        gbc.weightx = 1;

        gbc.weighty = 0;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        // label

        gbc.gridy =
                1 + row * 2;

        gbc.insets =
                new Insets(
                        8,
                        4,
                        3,
                        4
                );

        JLabel label =
                new JLabel(
                        labelText
                );

        label.setFont(
                new Font(
                        FONT,
                        Font.BOLD,
                        12
                )
        );

        label.setForeground(MUTED);

        panel.add(
                label,
                gbc
        );

        // input

        gbc.gridy =
                2 + row * 2;

        gbc.insets =
                new Insets(
                        0,
                        4,
                        4,
                        4
                );

        styleInput(component);

        component.setPreferredSize(
                new Dimension(
                        200,
                        38
                )
        );

        panel.add(
                component,
                gbc
        );
    }

    // ============================
    // ADD DOCTOR
    // ============================

    private void addDoctor() {

        String name =
                nameField
                        .getText()
                        .trim();

        String specialization =
                specializationField
                        .getText()
                        .trim();

        String phone =
                phoneField
                        .getText()
                        .trim();

        String email =
                emailField
                        .getText()
                        .trim();

        String gender =
                genderBox
                        .getSelectedItem()
                        .toString();

        String experienceText =
                experienceField
                        .getText()
                        .trim();

        String feeText =
                feeField
                        .getText()
                        .trim();

        String days =
                daysField
                        .getText()
                        .trim();

        String time =
                timeField
                        .getText()
                        .trim();

        String status =
                statusBox
                        .getSelectedItem()
                        .toString();

        // ============================
        // VALIDATION
        // ============================

        if (
                name.isEmpty()
                        || specialization.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "Doctor name and specialization are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int experience = 0;

        if (!experienceText.isEmpty()) {

            try {

                experience =
                        Integer.parseInt(
                                experienceText
                        );

                if (experience < 0) {

                    throw new NumberFormatException();
                }

            } catch (
                    NumberFormatException e
            ) {

                JOptionPane.showMessageDialog(
                        this,
                        "Experience must be a valid number.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }
        }

        double fee = 0;

        if (!feeText.isEmpty()) {

            try {

                fee =
                        Double.parseDouble(
                                feeText
                        );

                if (fee < 0) {

                    throw new NumberFormatException();
                }

            } catch (
                    NumberFormatException e
            ) {

                JOptionPane.showMessageDialog(
                        this,
                        "Consultation fee must be a valid number.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }
        }

        // ============================
        // IMAGE
        // ============================

        String imagePath = null;

        try {

            if (
                    selectedImagePath != null
                            && !selectedImagePath.isEmpty()
            ) {

                imagePath =
                        saveDoctorImage(
                                selectedImagePath
                        );
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to save doctor image.\n"
                            + e.getMessage(),
                    "Image Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // ============================
        // INSERT
        // ============================

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

        try (
                Connection con =
                        DB.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    name
            );

            ps.setString(
                    2,
                    specialization
            );

            ps.setString(
                    3,
                    phone
            );

            ps.setString(
                    4,
                    email
            );

            ps.setString(
                    5,
                    imagePath
            );

            ps.setString(
                    6,
                    gender
            );

            ps.setInt(
                    7,
                    experience
            );

            ps.setDouble(
                    8,
                    fee
            );

            ps.setString(
                    9,
                    days
            );

            ps.setString(
                    10,
                    time
            );

            ps.setString(
                    11,
                    status
            );

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                    this,
                    "Doctor added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearForm();

            loadDoctors();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Database Error:\n"
                            + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ============================
    // LOAD DOCTORS
    // ============================

    private void loadDoctors() {

        tableModel.setRowCount(0);

        String sql =
                """
                SELECT
                    id,
                    name,
                    specialization,
                    phone,
                    email,
                    gender,
                    experience,
                    consultation_fee,
                    available_days,
                    available_time,
                    image_path,
                    status
                FROM doctors
                ORDER BY id DESC
                """;

        try (
                Connection con =
                        DB.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                tableModel.addRow(
                        new Object[]{

                                rs.getInt(
                                        "id"
                                ),

                                rs.getString(
                                        "name"
                                ),

                                rs.getString(
                                        "specialization"
                                ),

                                rs.getString(
                                        "phone"
                                ),

                                rs.getString(
                                        "email"
                                ),

                                rs.getString(
                                        "gender"
                                ),

                                rs.getInt(
                                        "experience"
                                ),

                                rs.getDouble(
                                        "consultation_fee"
                                ),

                                rs.getString(
                                        "available_days"
                                ),

                                rs.getString(
                                        "available_time"
                                ),

                                rs.getString(
                                        "image_path"
                                ),

                                rs.getString(
                                        "status"
                                )
                        }
                );
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load doctors:\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );

            e.printStackTrace();
        }
    }

    // ============================
    // CLEAR FORM
    // ============================

    private void clearForm() {

        nameField.setText("");

        specializationField.setText("");

        phoneField.setText("");

        emailField.setText("");

        experienceField.setText("");

        feeField.setText("");

        daysField.setText("");

        timeField.setText("");

        genderBox.setSelectedIndex(0);

        statusBox.setSelectedIndex(0);

        selectedImagePath = null;

        imagePreview.setIcon(null);

        imagePreview.setText(
                "No Image"
        );
    }

    // ============================
    // REFRESH
    // ============================

    public void refresh() {

        loadDoctors();
    }
}