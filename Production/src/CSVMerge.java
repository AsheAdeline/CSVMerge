/*
CSVMerge.java
"A simple program with GUI to merge, clean, and shuffle two or more .CSV files."

V. 2.3.1.2
31 July 2025
Ashe Daphne Johnson Bones
Synventive Engineering Inc.

Created with JDK 22.0.2
Compatible and tested with JRE 8u461
*/

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class CSVMergeGUI extends JFrame {

    // GUI Elements
    private JTextField entriesField, categoryField, outputNameField, inputDirField, outputDirField;
    private JTextPane consoleArea;
    private JProgressBar progressBar;

    // Directories
    private final Path defaultInputDir, defaultOutputDir, logsDir;

    // Logging
    private BufferedWriter logWriter;

    // Styles
    private SimpleAttributeSet normalStyle, errorStyle;

    public CSVMergeGUI() {
        setTitle("CSV Merger v2.3.0.1");
        setSize(850, 550);
        setMinimumSize(new Dimension(700, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Initialize directories
        Path currentDir = Paths.get("").toAbsolutePath();
        defaultInputDir = currentDir.resolve("Input Folder");
        defaultOutputDir = currentDir.resolve("Output Folder");
        logsDir = currentDir.resolve("Logs");
        createDirectories(defaultInputDir, defaultOutputDir, logsDir);

        initLogFile();

        // GUI Construction
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildConsolePanel(), BorderLayout.SOUTH);
    }

    // Create required directories if missing
    private void createDirectories(Path... dirs) {
        for (Path dir : dirs) {
            try { if (!Files.exists(dir)) Files.createDirectory(dir); }
            catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error creating directories: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Header panel
    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 120, 255),
                        getWidth(), getHeight(), new Color(175, 200, 255)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setPreferredSize(new Dimension(800, 60));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
        JLabel title = new JLabel("CSV Merger");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        panel.add(title);
        return panel;
    }

    // Main panel, includes main input fields.
    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(200,200,200), 1, true),
                new EmptyBorder(15, 15, 15, 15)));
        panel.setBackground(new Color(250, 250, 250));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        int row = 0;
        panelAddRow(panel, gbc, row++, "Input Directory:",
                inputDirField = new JTextField(defaultInputDir.toString()),
                e -> browseDirectory(inputDirField));
        panelAddRow(panel, gbc, row++, "Output Directory:",
                outputDirField = new JTextField(defaultOutputDir.toString()),
                e -> browseDirectory(outputDirField));
        panelAddRow(panel, gbc, row++, "Max Entries per CSV:",
                entriesField = new JTextField("500"), null);
        panelAddRow(panel, gbc, row++, "Category Base (Optional):",
                categoryField = new JTextField(""), null);
        panelAddRow(panel, gbc, row, "Output Base Filename:",
                outputNameField = new JTextField("ShuffledCSV"), null);

        return panel;
    }

    // Console panel and progress bar panel
    private JPanel buildConsolePanel() {
        consoleArea = new JTextPane();
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(245, 245, 245));
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, Color.BLACK);

        errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setBold(errorStyle, true);

        JScrollPane scrollPane = new JScrollPane(consoleArea);
        scrollPane.setPreferredSize(new Dimension(800, 120));
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(new EmptyBorder(5, 15, 10, 15));

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 25));
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(66, 135, 245));
        progressBar.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));
        bottomPanel.add(progressBar, BorderLayout.CENTER);

        JButton runButton = new JButton("▶ Run");
        runButton.setBackground(new Color(66, 135, 245));
        runButton.setForeground(Color.BLACK);
        runButton.setPreferredSize(new Dimension(100, 35));
        runButton.addActionListener(this::runMergeAction);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(runButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel southContainer = new JPanel();
        southContainer.setLayout(new BoxLayout(southContainer, BoxLayout.Y_AXIS));
        southContainer.add(scrollPane);
        southContainer.add(bottomPanel);

        return southContainer;
    }

    // Method to add rows to the main panel
    private void panelAddRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField field, ActionListener browseAction) {
        gbc.gridy = row; gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(label, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        field.setPreferredSize(new Dimension(200, 28));
        panel.add(field, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        if (browseAction != null) {
            JButton browse = new JButton("...");
            browse.setPreferredSize(new Dimension(40, 28));
            browse.addActionListener(browseAction);
            panel.add(browse, gbc);
        } else panel.add(Box.createHorizontalStrut(1), gbc);
    }

    // Directory pop-up
    private void browseDirectory(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Log initialization
    private void initLogFile() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Path logFile = logsDir.resolve("CSVMergeLog_" + timestamp + ".txt");
            logWriter = Files.newBufferedWriter(logFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not create log file: " + e.getMessage(),
                    "Log Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Writes console text to the log
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> appendToConsole(msg, normalStyle));
        writeToLogFile(msg);
    }

    // Error display and writing
    private void showError(String msg) {
        SwingUtilities.invokeLater(() -> appendToConsole("ERROR: " + msg, errorStyle));
        writeToLogFile("ERROR: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        progressBar.setForeground(Color.RED);
        progressBar.setString("FAILED");
    }

    // Method to display text to console
    private void appendToConsole(String msg, AttributeSet style) {
        try {
            StyledDocument doc = consoleArea.getStyledDocument();
            doc.insertString(doc.getLength(), msg + "\n", style);
            consoleArea.setCaretPosition(doc.getLength());
        } catch (Exception ignored) {}
    }

    // Method to write to the log file
    private void writeToLogFile(String msg) {
        try { if (logWriter != null) { logWriter.write(msg + System.lineSeparator()); logWriter.flush(); } }
        catch (IOException ignored) {}
    }

    // Merge and clean statistics
    static class MergeResult {
        int finalCount, duplicatesRemoved;
        List<String> uniqueRows;
        String header;
        MergeResult(int count, int dup, List<String> rows, String header) {
            this.finalCount = count; this.duplicatesRemoved = dup;
            this.uniqueRows = rows; this.header = header;
        }
    }

    // Primary logic method for the statistic counters.
    public static MergeResult mergeCSVFiles(List<String> inputFiles) throws IOException {
        Set<String> uniqueRows = new LinkedHashSet<>();
        Set<String> allRows = new HashSet<>();
        String header = null;
        int duplicates = 0, expectedCols = -1;

        for (String file : inputFiles) {
            List<String> lines = Files.readAllLines(Paths.get(file));
            if (lines.isEmpty()) continue;

            String currentHeader = lines.get(0).replace("\uFEFF", "").trim();
            String[] currentCols = currentHeader.split(",", -1);

            if (header == null) {
                header = currentHeader;
                expectedCols = currentCols.length;
            } else {
                String[] baseCols = header.split(",", -1);
                for (int i = 0; i < Math.min(5, Math.min(baseCols.length, currentCols.length)); i++) {
                    if (!baseCols[i].trim().equalsIgnoreCase(currentCols[i].trim()))
                        throw new IllegalArgumentException("CSV headers do not match in file: " + file);
                }
            }

            for (int i = 1; i < lines.size(); i++) {
                String row = lines.get(i).trim();
                if (row.isEmpty() || row.split(",", -1).length < expectedCols) continue;
                if (!allRows.add(row)) duplicates++;
                uniqueRows.add(row);
            }
        }

        List<String> shuffled = new ArrayList<>(uniqueRows);
        Collections.shuffle(shuffled);
        return new MergeResult(shuffled.size(), duplicates, shuffled, header);
    }

    // Method to split .CSV files into multiple files.
    public static int splitIntoFiles(String header, List<String> rows, Path outputDir,
                                     int entriesPerFile, String categoryLetters, int startNumber,
                                     String outputFileBase, boolean addCategory) throws IOException {
        int fileCount = 0;
        for (int start = 0; start < rows.size(); start += entriesPerFile) {
            fileCount++;
            int end = Math.min(start + entriesPerFile, rows.size());
            Path file = outputDir.resolve(outputFileBase + fileCount + ".csv");
            String categoryName = categoryLetters + (startNumber + fileCount - 1);

            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write(addCategory ? header + ",Category" : header);
                writer.newLine();
                for (int i = start; i < end; i++) {
                    writer.write(addCategory ? rows.get(i) + "," + categoryName : rows.get(i));
                    writer.newLine();
                }
            }
        }
        return fileCount;
    }

    // Method to hande most of the logic for merging .CSV files.
    private void runMergeAction(ActionEvent e) {
        consoleArea.setText("");
        progressBar.setValue(0);
        progressBar.setString("");

        int entriesPerFile;
        try {
            entriesPerFile = Integer.parseInt(entriesField.getText().trim());
            if (entriesPerFile <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Invalid number for entries per file."); return;
        }

        String categoryBase = categoryField.getText().trim();
        boolean addCategory = !categoryBase.isEmpty();

        String outputFileBase = outputNameField.getText().trim();
        if (outputFileBase.isEmpty()) { showError("Output base filename cannot be empty."); return; }

        Path inputDir = inputDirField.getText().trim().isEmpty() ? defaultInputDir : Paths.get(inputDirField.getText().trim());
        Path outputDir = outputDirField.getText().trim().isEmpty() ? defaultOutputDir : Paths.get(outputDirField.getText().trim());
        createDirectories(inputDir, outputDir);

        // Parse category base (letters + number suffix)
        int startNumber; String baseLetters;
        if (addCategory) {
            int i = categoryBase.length() - 1;
            while (i >= 0 && Character.isDigit(categoryBase.charAt(i))) i--;
            if (i < categoryBase.length() - 1) {
                baseLetters = categoryBase.substring(0, i + 1);
                startNumber = Integer.parseInt(categoryBase.substring(i + 1));
            } else {
                startNumber = 1;
                baseLetters = categoryBase;
            }
        } else {
            startNumber = 1;
            baseLetters = categoryBase;
        }

        new Thread(() -> {
            try {
                progressBar.setIndeterminate(true);
                log("Scanning input directory...");
                List<String> inputFiles = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.csv")) {
                    for (Path file : stream) inputFiles.add(file.toString());
                }

                if (inputFiles.isEmpty()) {
                    log("No CSV files found in input directory.");
                    progressBar.setIndeterminate(false); return;
                }

                log("Found " + inputFiles.size() + " CSV files. Starting merge...");
                MergeResult result = mergeCSVFiles(inputFiles);
                progressBar.setIndeterminate(false);
                progressBar.setValue(25);

                // Step 2: Create full outputs
                log("Creating full CSV outputs...");
                List<String> sortedRows = new ArrayList<>(result.uniqueRows);
                sortedRows.sort(Comparator.comparing(row -> row.split(",", -1)[0].toLowerCase()));
                writeFile(outputDir.resolve("0_FULL_UNSHUFFLED.csv"), result.header, sortedRows);
                log("Created: 0_FULL_UNSHUFFLED.csv");

                writeFile(outputDir.resolve("1_FULL_SHUFFLED.csv"), result.header, result.uniqueRows);
                log("Created: 1_FULL_SHUFFLED.csv");

                progressBar.setValue(60);

                // Step 3: Split files
                log("Splitting into smaller CSV files...");
                int fileCount = splitIntoFiles(result.header, result.uniqueRows, outputDir,
                        entriesPerFile, baseLetters, startNumber, outputFileBase, addCategory);

                progressBar.setValue(100);
                progressBar.setForeground(new Color(40, 120, 25));
                progressBar.setString("FINISHED");

                log("\nSummary:");
                log("Total entries: " + result.finalCount);
                log("Duplicates removed: " + result.duplicatesRemoved);
                log("Output CSV files created: " + fileCount);
                log("All tasks completed successfully!");

            } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
        }).start();
    }

    // Writes rows to a .CSV file
    private void writeFile(Path file, String header, List<String> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(header); writer.newLine();
            for (String row : rows) { writer.write(row); writer.newLine(); }
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CSVMergeGUI().setVisible(true));
    }
}