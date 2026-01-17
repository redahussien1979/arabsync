import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.text.AttributedString;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class arabicSync {
    private int currentFrame = 0;
    private int totalFrames = 0;
    private static final double FRAME_RATE = 10.0; // Change this value to control frame rate globally

    private WordTiming[] currentWordTimings;
    private FormattedTextDataArabicSync currentFormattedData;
    // Image cache to avoid repeated disk reads - speeds up video generation
    private java.util.Map<Integer, java.util.List<BufferedImage>> lineImageCache = new java.util.HashMap<>();
    private java.util.Map<String, BufferedImage> generalImageCache = new java.util.HashMap<>();
    private static final int USE_CHANGING_BACKGROUNDS = 1; //
    private VideoConfig config = new VideoConfig();

    public void setConfig(VideoConfig config) {
        this.config = config;
    }

    // ADD THESE TWO METHODS HERE:
    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    private static final String FONT_PATH = "C:\\Users\\hp\\AppData\\Local\\Microsoft\\Windows\\Fonts\\IMFellEnglish-Regular.ttf";

    private static final String FONT_PATH2 = "C:\\Users\\hp\\AppData\\Local\\Microsoft\\Windows\\Fonts\\amiriquran-regular.ttf";

    // ADD THIS:
    private volatile boolean stopRequested = false;

    public void requestStop() {
        stopRequested = true;
        System.out.println("Stop requested by user");
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void resetStop() {
        stopRequested = false;

    }


    private static class WordTiming {
        String word;
        double startTime;
        double endTime;

        WordTiming(String word, double startTime, double endTime) {
            this.word = word;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public String toString() {
            return String.format("%s [%.2f-%.2f]", word, startTime, endTime);
        }
    }



    ////////////////////////////Add batch mode flag to VideoConfig////
    ///
    /////
    //////

    private static class VideoConfig {
        String textFilePath = "qqq.txt";
        String audioFilePath = "";
        String arabicFilePath = "quoteAR.txt";
        String fontPath1 = "C:\\Users\\hp\\AppData\\Local\\Microsoft\\Windows\\Fonts\\IMFellEnglish-Regular.ttf";
        String fontPath2 = "C:\\Users\\hp\\AppData\\Local\\Microsoft\\Windows\\Fonts\\amiriquran-regular.ttf";
        String imageFolder1 = "imgchng";
        String imageFolder2 = "imgchng2";
        int backgroundMode = 0; // 0=random, 1=jigsaw, 2=slideshow, 3=single
        String outputVideoName = "";
        int rerunCount = 1; // ADD THIS LINE
        String specificBackgroundImage = ""; // ADD THIS LINE
        boolean removeTextAndBackground = false; // ADD THIS LINE
        int highlightWordCount = 1; // ADD THIS LINE - default to 1 word
        Color backgroundColor = Color.BLACK; // ADD THIS LINE

        double frameRate = 10.0; // ADD THIS LINE

        String imagesPerLineFolder = "images_per_line"; // ADD THIS

        // Images per line mode settings
        int imagesPerLineFirstLineY = 100; // Y position for first line text (0 = top, higher = lower)

        // First line text effects settings
        int firstLineFontSize = 65; // Font size for first line (20-120)
        Color firstLineTextColor = new Color(255, 165, 0); // Default orange color
        int firstLineAnimationType = 0; // 0=none, 1=fade-in, 2=typewriter, 3=wave, 4=bounce, 5=glow-pulse
        boolean firstLineShakeEnabled = false; // Enable shake/vibrate effect
        int firstLineShakeIntensity = 5; // Shake intensity (1-20 pixels)
        int firstLineTiltAngle = 0; // Tilt angle in degrees (-30 to 30)
        boolean firstLineShadowEnabled = true; // Enable text shadow
        int firstLineShadowOffset = 3; // Shadow offset pixels
        boolean firstLineOutlineEnabled = true; // Enable text outline
        Color firstLineOutlineColor = Color.BLACK; // Outline color
        boolean firstLineGlowEnabled = false; // Enable glow effect
        Color firstLineGlowColor = new Color(255, 200, 100); // Glow color

        // ADD THESE EFFECT FLAGS:
        boolean enableColorEnhancement = true;
        boolean enableWaterEffect = false;
        boolean enableDynamicLighting = true;
        boolean enableLensDistortion = true;
        boolean enableVignette = true;
        boolean enableParticles = true;
        boolean enableLightRays = true;
        boolean enableSparkles = true;

        // ADD THESE 4 NEW FLAGS:
        boolean enableZoom = true;
        boolean enableFlip = true;
        boolean enableGrayscale = true;
        boolean enableFallingPieces = true;

        boolean enableBatchMode = false;  // ADD THIS
        String batchInputFolder = "batch_input";  // ADD THIS
    }


    public static void main(String[] args) {


        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ArabicVideoGUI().setVisible(true);
        });
    }

    private static class ArabicVideoGUI extends JFrame {
        private VideoConfig config = new VideoConfig();
        private JTextField textFileField, audioFileField, arabicFileField;
        private JTextField font1Field, font2Field, imageFolder1Field, imageFolder2Field;
        private JTextField outputNameField;
        private JTextField rerunCountField; // ADD THIS LINE
        private JTextField specificBackgroundField; // ADD THIS LINE
        private JTextField frameRateField; // ADD THIS LINE
        private JComboBox<String> backgroundModeCombo;
        private JButton generateButton;
        private JButton stopButton;  // ADD THIS

        private JProgressBar progressBar;
        private JTextArea logArea;

        private arabicSync generator;


        private JCheckBox batchModeCheck;
        private JTextField batchFolderField;
        //  private JTextField frameRateField;
        // ADD THESE CHECKBOX FIELDS:
        private JCheckBox colorEnhancementCheck;
        private JCheckBox waterEffectCheck;
        private JCheckBox dynamicLightingCheck;
        private JCheckBox lensDistortionCheck;
        private JCheckBox vignetteCheck;
        private JCheckBox particlesCheck;
        private JCheckBox lightRaysCheck;
        private JCheckBox sparklesCheck;


        // ADD THESE 4 DECLARATIONS:
        private JCheckBox zoomCheck;
        private JCheckBox flipCheck;
        private JCheckBox grayscaleCheck;
        private JCheckBox fallingPiecesCheck;

        public ArabicVideoGUI() {
            setTitle("Arabic Video Generator");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(800, 700);
            setLocationRelativeTo(null);

            initializeGUI();
            loadDefaultValues();
        }

        private void initializeGUI() {
            setLayout(new BorderLayout());

            // Main panel with tabs
            JTabbedPane tabbedPane = new JTabbedPane();

            // Files tab
            JPanel filesPanel = createFilesPanel();
            tabbedPane.addTab("Files & Fonts", filesPanel);

            // Folders tab
            JPanel foldersPanel = createFoldersPanel();
            tabbedPane.addTab("Image Folders", foldersPanel);

            // Settings tab
            JPanel settingsPanel = createSettingsPanel();
            tabbedPane.addTab("Settings", settingsPanel);

            add(tabbedPane, BorderLayout.CENTER);

            // Bottom panel
            JPanel bottomPanel = new JPanel(new BorderLayout());

            // Generate button
            generateButton = new JButton("Generate Video");
            generateButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            generateButton.addActionListener(this::generateVideo);

            // ADD STOP BUTTON:
            stopButton = new JButton("Stop");
            stopButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            stopButton.setEnabled(false);
            stopButton.addActionListener(e -> stopGeneration());

            // Progress bar
            progressBar = new JProgressBar();
            progressBar.setStringPainted(true);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(generateButton);
            buttonPanel.add(stopButton);  // ADD THIS

            bottomPanel.add(buttonPanel, BorderLayout.NORTH);
            bottomPanel.add(progressBar, BorderLayout.CENTER);

            // Log area
            logArea = new JTextArea(8, 50);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            logArea.setEditable(false);
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
            bottomPanel.add(logScroll, BorderLayout.SOUTH);

            add(bottomPanel, BorderLayout.SOUTH);
        }


        private JPanel createFilesPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Text file
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Text File (qqq.txt):"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            textFileField = new JTextField(30);
            panel.add(textFileField, gbc);
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseText = new JButton("Browse");
            browseText.addActionListener(e -> browseFile(textFileField, "Select Text File", "txt"));
            panel.add(browseText, gbc);




// After Arabic File field (around line 250-260 in your GUI code)
            gbc.gridx = 0;
            gbc.gridy++; // Next row
            gbc.gridwidth = 3;
            JButton editTextButton = new JButton("✏️ Edit Arabic Text");
            editTextButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            editTextButton.addActionListener(e -> openTextEditorDialog());
            panel.add(editTextButton, gbc);
            gbc.gridwidth = 1; // Reset
















            // Audio file
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("Audio File:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            audioFileField = new JTextField(30);
            panel.add(audioFileField, gbc);
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseAudio = new JButton("Browse");
            browseAudio.addActionListener(e -> browseFile(audioFileField, "Select Audio File", "mp3", "wav", "m4a"));
            panel.add(browseAudio, gbc);

            // Arabic file
            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(new JLabel("Arabic File (quoteAR.txt):"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            arabicFileField = new JTextField(30);
            panel.add(arabicFileField, gbc);
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseArabic = new JButton("Browse");
            browseArabic.addActionListener(e -> browseFile(arabicFileField, "Select Arabic File", "txt"));
            panel.add(browseArabic, gbc);

            // Store font file references for later use
            final java.util.Map<String, File> fontFileMap1 = new java.util.HashMap<>();
            final java.util.Map<String, File> fontFileMap2 = new java.util.HashMap<>();

// English Font Dropdown
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(new JLabel("English Font:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;

            String[] fonts1 = scanFontsInDirectoryWithMap(fontFileMap1);
            JComboBox<String> font1Combo = new JComboBox<>(fonts1);
            font1Combo.addActionListener(e -> {
                String selectedFont = (String) font1Combo.getSelectedItem();
                if (selectedFont != null && !selectedFont.equals("No fonts found")) {
                    File fontFile = fontFileMap1.get(selectedFont);
                    if (fontFile != null) {
                        config.fontPath1 = fontFile.getAbsolutePath();
                        System.out.println("English font selected: " + config.fontPath1);
                    }
                }
            });
            panel.add(font1Combo, gbc);
            gbc.gridwidth = 1;

// Arabic Font Dropdown
            gbc.gridx = 0;
            gbc.gridy = 4;
            panel.add(new JLabel("Arabic Font:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;

            String[] fonts2 = scanFontsInDirectoryWithMap(fontFileMap2);
            JComboBox<String> font2Combo = new JComboBox<>(fonts2);
            font2Combo.addActionListener(e -> {
                String selectedFont = (String) font2Combo.getSelectedItem();
                if (selectedFont != null && !selectedFont.equals("No fonts found")) {
                    File fontFile = fontFileMap2.get(selectedFont);
                    if (fontFile != null) {
                        config.fontPath2 = fontFile.getAbsolutePath();
                        System.out.println("Arabic font selected: " + config.fontPath2);
                    }
                }
            });
            panel.add(font2Combo, gbc);
            gbc.gridwidth = 1;


            return panel;
        }

        private JPanel createFoldersPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Primary Image Folder
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Primary Image Folder:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            imageFolder1Field = new JTextField(30);
            panel.add(imageFolder1Field, gbc);
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseFolder1 = new JButton("Browse");
            browseFolder1.addActionListener(e -> browseFolder(imageFolder1Field, "Select Primary Image Folder"));
            panel.add(browseFolder1, gbc);

            // Slideshow Image Folder
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("Slideshow Image Folder:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            imageFolder2Field = new JTextField(30);
            panel.add(imageFolder2Field, gbc);
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseFolder2 = new JButton("Browse");
            browseFolder2.addActionListener(e -> browseFolder(imageFolder2Field, "Select Slideshow Image Folder"));
            panel.add(browseFolder2, gbc);







            // Images per Line Folder
            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Images per Line Folder:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            JTextField imagesPerLineField = new JTextField("images_per_line", 30);
            panel.add(imagesPerLineField, gbc);
            gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            JButton browseFolder3 = new JButton("Browse");
            browseFolder3.addActionListener(e -> {
                browseFolder(imagesPerLineField, "Select Images per Line Folder");
                config.imagesPerLineFolder = imagesPerLineField.getText();
            });
            panel.add(browseFolder3, gbc);

// ADD THIS NEW BUTTON:
            gbc.gridx = 3;
            JButton manageImagesBtn = new JButton("Manage Images");
            manageImagesBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            manageImagesBtn.addActionListener(e -> {
                config.imagesPerLineFolder = imagesPerLineField.getText();
                ImagesPerLineManagerGUI manager = new ImagesPerLineManagerGUI(ArabicVideoGUI.this, config);
                manager.setVisible(true);
            });
            panel.add(manageImagesBtn, gbc);

            // After the "Manage Images" button:
            gbc.gridx = 4;  // Or next available column
            JButton effectsEditorBtn = new JButton("🎨 Effects Editor");
            effectsEditorBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            effectsEditorBtn.addActionListener(e -> {
                ImageEffectsEditorGUI editor = new ImageEffectsEditorGUI(config);
                editor.setVisible(true);
            });
            panel.add(effectsEditorBtn, gbc);

            // First Line Y Position (for images per line mode)
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(new JLabel("First Line Y Position:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            JSlider firstLineYSlider = new JSlider(JSlider.HORIZONTAL, 50, 800, config.imagesPerLineFirstLineY);
            firstLineYSlider.setMajorTickSpacing(150);
            firstLineYSlider.setMinorTickSpacing(50);
            firstLineYSlider.setPaintTicks(true);
            firstLineYSlider.setPaintLabels(true);
            JLabel firstLineYValueLabel = new JLabel(String.valueOf(config.imagesPerLineFirstLineY) + " px");
            firstLineYSlider.addChangeListener(e -> {
                config.imagesPerLineFirstLineY = firstLineYSlider.getValue();
                firstLineYValueLabel.setText(config.imagesPerLineFirstLineY + " px");
            });
            panel.add(firstLineYSlider, gbc);
            gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            panel.add(firstLineYValueLabel, gbc);
            gbc.gridx = 3;
            JLabel firstLineYHint = new JLabel("(Top=50, Middle=400, Bottom=800)");
            firstLineYHint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
            firstLineYHint.setForeground(Color.GRAY);
            panel.add(firstLineYHint, gbc);

            // First Line Effects Button
            gbc.gridx = 4;
            JButton firstLineEffectsBtn = new JButton("✨ First Line Effects");
            firstLineEffectsBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            firstLineEffectsBtn.addActionListener(e -> openFirstLineEffectsDialog());
            panel.add(firstLineEffectsBtn, gbc);

            return panel;
        }
        private void openTextEditorDialog() {
            JDialog dialog = new JDialog(this, "Arabic Text Editor", true);
            dialog.setSize(900, 700);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            // Top panel - controls
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

            JLabel wordsPerLineLabel = new JLabel("Words per line:");
            JSpinner wordsPerLineSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 20, 1));

            JButton splitButton = new JButton("Split Lines");
            JButton convertNumbersButton = new JButton("Convert Numbers to Words");

            // ADD THIS NEW BUTTON:
            JLabel wordsPerCommaLabel = new JLabel("Words per comma:");
            JSpinner wordsPerCommaSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
            JButton commaButton = new JButton("Make Single Line with Commas");
            JButton saveButton = new JButton("💾 Save to File");

            controlPanel.add(wordsPerLineLabel);
            controlPanel.add(wordsPerLineSpinner);
            controlPanel.add(splitButton);
            controlPanel.add(convertNumbersButton);

            // ADD THESE:
            controlPanel.add(wordsPerCommaLabel);
            controlPanel.add(wordsPerCommaSpinner);
            controlPanel.add(commaButton);


            controlPanel.add(saveButton);

            // Text area
            JTextArea textArea = new JTextArea();
            textArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Arabic Text"));

            // Status label
            JLabel statusLabel = new JLabel("Ready");
            statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            // Load existing text
            try {
                String content = new String(java.nio.file.Files.readAllBytes(
                        java.nio.file.Paths.get(config.arabicFilePath)),
                        java.nio.charset.StandardCharsets.UTF_8);
                textArea.setText(content);
            } catch (Exception ex) {
                textArea.setText("# Error loading file: " + ex.getMessage());
            }

            // Split lines button action
            splitButton.addActionListener(e -> {
                int wordsPerLine = (int) wordsPerLineSpinner.getValue();
                String text = textArea.getText();
                String splitText = splitTextIntoLines(text, wordsPerLine);
                textArea.setText(splitText);
                statusLabel.setText("✓ Text split into lines with " + wordsPerLine + " words each");
            });

            // Convert numbers button action
            convertNumbersButton.addActionListener(e -> {
                String text = textArea.getText();
                String convertedText = convertNumbersToArabicWords(text);
                textArea.setText(convertedText);
                statusLabel.setText("✓ Numbers converted to Arabic words");
            });
            // ADD THIS COMMA BUTTON ACTION:
            commaButton.addActionListener(e -> {
                int wordsPerComma = (int) wordsPerCommaSpinner.getValue();
                String text = textArea.getText();
                String commaText = makeSingleLineWithCommas(text, wordsPerComma);
                textArea.setText(commaText);
                statusLabel.setText("✓ Text converted to single line with commas every " + wordsPerComma + " words");
            });

            // Save button action
            saveButton.addActionListener(e -> {
                try {
                    java.nio.file.Files.write(
                            java.nio.file.Paths.get(config.arabicFilePath),
                            textArea.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    );
                    statusLabel.setText("✓ Saved to " + config.arabicFilePath);
                    JOptionPane.showMessageDialog(dialog, "File saved successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusLabel.setText("✗ Error saving file");
                    JOptionPane.showMessageDialog(dialog, "Error saving: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            dialog.add(controlPanel, BorderLayout.NORTH);
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(statusLabel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        }




        private String splitTextIntoLines(String text, int wordsPerLine) {
            // Remove all commas before processing
            text = text.replace("،", "");

            StringBuilder result = new StringBuilder();
            String[] lines = text.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    result.append(line).append("\n");
                    continue;
                }

                // Split by -- to separate Arabic and English parts
                String[] parts = line.split("--", 2);
                String arabicPart = parts[0].trim();
                String englishPart = parts.length > 1 ? parts[1].trim() : "";

                // Split Arabic text into words
                String[] words = arabicPart.split("\\s+");

                // Group words
                for (int i = 0; i < words.length; i += wordsPerLine) {
                    int end = Math.min(i + wordsPerLine, words.length);
                    String[] lineWords = java.util.Arrays.copyOfRange(words, i, end);
                    result.append(String.join(" ", lineWords));

                    // Add English part only to first line
                    if (i == 0 && !englishPart.isEmpty()) {
                        result.append(" -- ").append(englishPart);
                    }

                    result.append("\n");
                }
            }

            return result.toString();
        }







        private String makeSingleLineWithCommas(String text, int wordsPerComma) {
            StringBuilder result = new StringBuilder();
            String[] lines = text.split("\n");

            // Collect all words from all lines
            List<String> allWords = new ArrayList<>();
            String englishPart = "";

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                // Split by -- to separate Arabic and English parts
                String[] parts = line.split("--", 2);
                String arabicPart = parts[0].trim();

                // Remove existing commas (both Arabic and English commas)
                arabicPart = arabicPart.replace("،", "").replace(",", "");

                // Keep the English part from first line only
                if (englishPart.isEmpty() && parts.length > 1) {
                    englishPart = parts[1].trim();
                }

                // Collect all Arabic words
                String[] words = arabicPart.split("\\s+");
                for (String word : words) {
                    word = word.trim();
                    if (!word.isEmpty()) {
                        allWords.add(word);
                    }
                }
            }

            // Build single line with commas
            for (int i = 0; i < allWords.size(); i++) {
                result.append(allWords.get(i));

                // Add comma after every X words (except last word)
                if ((i + 1) % wordsPerComma == 0 && i < allWords.size() - 1) {
                    result.append("، ");
                }
                // Add space (except after comma or last word)
                else if (i < allWords.size() - 1) {
                    result.append(" ");
                }
            }

            // Add English part if exists
            if (!englishPart.isEmpty()) {
                result.append(" -- ").append(englishPart);
            }

            return result.toString();
        }











        private String convertNumbersToArabicWords(String text) {
            // Find all numbers in the text
            Pattern pattern = Pattern.compile("\\b\\d+\\b");
            Matcher matcher = pattern.matcher(text);

            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String number = matcher.group();
                String arabicWords = convertSingleNumberToArabic(Integer.parseInt(number));
                matcher.appendReplacement(result, arabicWords);
            }

            matcher.appendTail(result);
            return result.toString();
        }

        private String convertSingleNumberToArabic(int number) {
            if (number == 0) return "صفر";

            String[] ones = {"", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"};
            String[] tens = {"", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"};
            String[] teens = {"عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
                    "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"};
            String[] hundreds = {"", "مئة", "مئتان", "ثلاثمئة", "أربعمئة", "خمسمئة", "ستمئة", "سبعمئة", "ثمانمئة", "تسعمئة"};

            if (number < 0) return "سالب " + convertSingleNumberToArabic(-number);

            StringBuilder result = new StringBuilder();

            // Thousands
            if (number >= 1000) {
                int thousands = number / 1000;
                if (thousands == 1) {
                    result.append("ألف");
                } else if (thousands == 2) {
                    result.append("ألفان");
                } else if (thousands <= 10) {
                    result.append(convertSingleNumberToArabic(thousands)).append(" آلاف");
                } else {
                    result.append(convertSingleNumberToArabic(thousands)).append(" ألف");
                }
                number %= 1000;
                if (number > 0) result.append(" و");
            }

            // Hundreds
            if (number >= 100) {
                int hundredsDigit = number / 100;
                result.append(hundreds[hundredsDigit]);
                number %= 100;
                if (number > 0) result.append(" و");
            }

            // Tens and ones
            // Tens and ones
            if (number >= 20) {
                int onesDigit = number % 10;
                int tensDigit = number / 10;

                // Ones come BEFORE tens in Arabic
                if (onesDigit > 0) {
                    result.append(ones[onesDigit]).append(" و");
                }
                result.append(tens[tensDigit]);
            } else if (number >= 10 && number < 20) {
                result.append(teens[number - 10]);
            } else if (number > 0) {
                result.append(ones[number]);
            }

            return result.toString();
        }


        private String[] scanFontsInDirectoryWithMap(java.util.Map<String, File> fontFileMap) {
            File currentDir = new File(".");
            String[] fontExtensions = {".ttf", ".otf"};
            java.util.List<String> fontNames = new java.util.ArrayList<>();

            // Recursive scan
            scanFontsRecursivelyWithMap(currentDir, fontExtensions, fontNames, fontFileMap);

            if (fontNames.isEmpty()) {
                return new String[]{"No fonts found"};
            }

            // Sort alphabetically
            java.util.Collections.sort(fontNames);

            return fontNames.toArray(new String[0]);
        }

        private void scanFontsRecursivelyWithMap(File directory, String[] extensions,
                                                 java.util.List<String> fontNames,
                                                 java.util.Map<String, File> fontFileMap) {
            File[] allFiles = directory.listFiles();
            if (allFiles == null) return;

            for (File file : allFiles) {
                if (file.isDirectory()) {
                    scanFontsRecursivelyWithMap(file, extensions, fontNames, fontFileMap);
                } else if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            String fontName = file.getName();
                            fontNames.add(fontName);
                            fontFileMap.put(fontName, file); // Store actual file reference
                            break;
                        }
                    }
                }
            }
        }

        private JPanel createSettingsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Background Mode
            // Background Mode
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Background Mode:"), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridwidth = 2;







            ////////////honnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn

            backgroundModeCombo = new JComboBox<>(new String[]{
                    "Random Selection", "Jigsaw Puzzle", "Slideshow", "Single Image with Effects", "Solid Color with Effects", "Image + Text (Comma-separated)", "Quiz Mode", "Images per Line"
            });
            panel.add(backgroundModeCombo, gbc);

// Color picker right next to background mode
            gbc.gridx = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            JPanel colorPanel = new JPanel();
            colorPanel.setPreferredSize(new Dimension(40, 25));
            colorPanel.setBackground(Color.BLACK);
            colorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            panel.add(colorPanel, gbc);

            gbc.gridx = 4;
            JButton colorPickerButton = new JButton("Color");
            colorPickerButton.addActionListener(e -> {
                Color selectedColor = JColorChooser.showDialog(this, "Choose Background Color", colorPanel.getBackground());
                if (selectedColor != null) {
                    colorPanel.setBackground(selectedColor);
                    config.backgroundColor = selectedColor;
                }
            });
            panel.add(colorPickerButton, gbc);
            gbc.gridwidth = 1;
//ok


// Output Video Name (move this down)
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
            // Output Video Name
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Output Video Name:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 4;
            outputNameField = new JTextField(30);
            panel.add(outputNameField, gbc);
            gbc.gridwidth = 1;

            // Number of Reruns
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            panel.add(new JLabel("Number of Reruns:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 4;
            rerunCountField = new JTextField("1", 30);
            panel.add(rerunCountField, gbc);
            gbc.gridwidth = 1;

            // Specific Background Image
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weightx = 0;
            panel.add(new JLabel("Specific Background Image:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 3;
            specificBackgroundField = new JTextField(30);
            panel.add(specificBackgroundField, gbc);
            gbc.gridx = 4;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JButton browseSpecificBg = new JButton("Browse");
            browseSpecificBg.addActionListener(e -> browseFile(specificBackgroundField, "Select Specific Background Image", "jpg","jfif", "jpeg", "png", "bmp", "gif"));
            panel.add(browseSpecificBg, gbc);

            // Remove Text and Background Option
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0;
            gbc.gridwidth = 5;
            JCheckBox removeTextBgCheckbox = new JCheckBox("Remove txt and bg");
            removeTextBgCheckbox.addActionListener(e -> config.removeTextAndBackground = removeTextBgCheckbox.isSelected());
            panel.add(removeTextBgCheckbox, gbc);
            gbc.gridwidth = 1;


            ///honaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            // ADD THIS NEW SECTION:
// Number of highlighted words
            //gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
            // Number of highlighted words OR comma-separated mode
            gbc.gridx = 3;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Highlighted Text Mode (when text removed):"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 4;

            JComboBox<String> highlightWordCountCombo = new JComboBox<>(new String[]{"1 word", "2 words", "3 words", "4 words", "Comma-separated", "Stripes (comma-separated)"});

            highlightWordCountCombo.addActionListener(e -> {
                int selectedIndex = highlightWordCountCombo.getSelectedIndex();
                if (selectedIndex == 4) {
                    config.highlightWordCount = -1; // Special flag for comma mode
                } else if (selectedIndex == 5) {
                    config.highlightWordCount = -2; // Special flag for stripes mode
                } else {
                    config.highlightWordCount = selectedIndex + 1;
                }
            });
            panel.add(highlightWordCountCombo, gbc);
            gbc.gridwidth = 1;


            // Frame Rate
            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.weightx = 0;
            panel.add(new JLabel("Frame Rate (FPS):"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 4;
            frameRateField = new JTextField("10.0", 30);
            panel.add(frameRateField, gbc);
            gbc.gridwidth = 1;

            // === SINGLE IMAGE EFFECTS SECTION (FOUR COLUMNS) ===
            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.gridwidth = 5;
            JLabel effectsLabel = new JLabel("Single Image Effects:");
            effectsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            panel.add(effectsLabel, gbc);
            gbc.gridwidth = 1;

            // Row 7 - 4 checkboxes
            gbc.gridx = 0;
            gbc.gridy = 7;
            colorEnhancementCheck = new JCheckBox("Color Enhancement", true);
            panel.add(colorEnhancementCheck, gbc);

            gbc.gridx = 1;
            waterEffectCheck = new JCheckBox("Water Effect", false);
            panel.add(waterEffectCheck, gbc);

            gbc.gridx = 2;
            dynamicLightingCheck = new JCheckBox("Dynamic Lighting", true);
            panel.add(dynamicLightingCheck, gbc);

            gbc.gridx = 3;
            lensDistortionCheck = new JCheckBox("Lens Distortion", true);
            panel.add(lensDistortionCheck, gbc);

            // Row 8 - 4 checkboxes
            gbc.gridx = 0;
            gbc.gridy = 8;
            vignetteCheck = new JCheckBox("Vignette", true);
            panel.add(vignetteCheck, gbc);

            gbc.gridx = 1;
            particlesCheck = new JCheckBox("Particles", true);
            panel.add(particlesCheck, gbc);

            gbc.gridx = 2;
            lightRaysCheck = new JCheckBox("Light Rays", true);
            panel.add(lightRaysCheck, gbc);

            gbc.gridx = 3;
            sparklesCheck = new JCheckBox("Sparkles", true);
            panel.add(sparklesCheck, gbc);

            // Row 9 - 4 checkboxes
            gbc.gridx = 0;
            gbc.gridy = 9;
            zoomCheck = new JCheckBox("Zoom Effect", true);
            panel.add(zoomCheck, gbc);

            gbc.gridx = 1;
            flipCheck = new JCheckBox("Flip Effect", true);
            panel.add(flipCheck, gbc);

            gbc.gridx = 2;
            grayscaleCheck = new JCheckBox("Grayscale Effect", true);
            panel.add(grayscaleCheck, gbc);

            gbc.gridx = 3;
            fallingPiecesCheck = new JCheckBox("Falling Pieces Effect", true);
            panel.add(fallingPiecesCheck, gbc);

            // === BATCH MODE SECTION ===
            gbc.gridx = 0;
            gbc.gridy = 10;
            gbc.gridwidth = 5;
            batchModeCheck = new JCheckBox("Enable Batch Mode (Process Multiple Quotes)", false);
            panel.add(batchModeCheck, gbc);
            gbc.gridwidth = 1;

            // Batch Input Folder
            gbc.gridx = 0;
            gbc.gridy = 11;
            gbc.weightx = 0;
            panel.add(new JLabel("Batch Input Folder:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = 3;
            batchFolderField = new JTextField("batch_input", 30);
            panel.add(batchFolderField, gbc);
            gbc.gridx = 4;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JButton browseBatchFolder = new JButton("Browse");
            browseBatchFolder.addActionListener(e -> browseFolder(batchFolderField, "Select Batch Input Folder"));
            panel.add(browseBatchFolder, gbc);

            return panel;
        }


        private File findAudioFile(File folder, String number) {
            String[] audioExtensions = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"};

            for (String ext : audioExtensions) {
                File audioFile = new File(folder, "audio" + number + ext);
                if (audioFile.exists()) {
                    return audioFile;
                }
            }
            return null;
        }


        private void loadDefaultValues() {
            textFileField.setText(config.textFilePath);

            // Find and set the latest audio file automatically
            String latestAudio = findLatestAudioFile();
            if (latestAudio != null) {
                audioFileField.setText(latestAudio);
                config.audioFilePath = latestAudio;
            } else {
                audioFileField.setText(config.audioFilePath);
            }

            arabicFileField.setText(config.arabicFilePath);
//            font1Field.setText(config.fontPath1);
//            font2Field.setText(config.fontPath2);
            imageFolder1Field.setText(config.imageFolder1);
            imageFolder2Field.setText(config.imageFolder2);
            backgroundModeCombo.setSelectedIndex(0);
        }

        private String findLatestAudioFile() {
            String[] extensions = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"};

            // Get current directory
            File currentDir = new File(".");
            File[] allFiles = currentDir.listFiles();

            if (allFiles == null) {
                return null;
            }

            File latestAudioFile = null;
            long latestModifiedTime = 0;

            // Find all audio files and track the latest one
            for (File file : allFiles) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();

                    // Check if it's an audio file
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            long modifiedTime = file.lastModified();

                            if (modifiedTime > latestModifiedTime) {
                                latestModifiedTime = modifiedTime;
                                latestAudioFile = file;
                            }
                            break; // Break inner loop once we find it's an audio file
                        }
                    }
                }
            }

            if (latestAudioFile != null) {
                System.out.println("✓ Latest audio file found: " + latestAudioFile.getName());
                return latestAudioFile.getAbsolutePath();
            } else {
                System.out.println("✗ No audio files found in directory");
                return null;
            }
        }

        private void browseFile(JTextField field, String title, String... extensions) {
            ThumbnailFileChooser chooser = new ThumbnailFileChooser();
            chooser.setCurrentDirectory(new File(".")); // Start in current app directory
            chooser.setDialogTitle(title);

            if (extensions.length > 0) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        String.join(", ", extensions).toUpperCase() + " files", extensions);
                chooser.setFileFilter(filter);
            }

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }

        private void openFirstLineEffectsDialog() {
            JDialog dialog = new JDialog(this, "First Line Text Effects", true);
            dialog.setSize(600, 700);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // === FONT SIZE SECTION ===
            JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            sizePanel.setBorder(BorderFactory.createTitledBorder("Font Size"));
            JSlider fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 20, 120, config.firstLineFontSize);
            fontSizeSlider.setMajorTickSpacing(20);
            fontSizeSlider.setMinorTickSpacing(5);
            fontSizeSlider.setPaintTicks(true);
            fontSizeSlider.setPaintLabels(true);
            fontSizeSlider.setPreferredSize(new Dimension(400, 50));
            JLabel fontSizeLabel = new JLabel(config.firstLineFontSize + " pt");
            fontSizeSlider.addChangeListener(e -> {
                config.firstLineFontSize = fontSizeSlider.getValue();
                fontSizeLabel.setText(config.firstLineFontSize + " pt");
            });
            sizePanel.add(fontSizeSlider);
            sizePanel.add(fontSizeLabel);
            mainPanel.add(sizePanel);

            // === COLOR SECTION ===
            JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorPanel.setBorder(BorderFactory.createTitledBorder("Text Color"));
            JButton textColorBtn = new JButton("Choose Text Color");
            JPanel textColorPreview = new JPanel();
            textColorPreview.setBackground(config.firstLineTextColor);
            textColorPreview.setPreferredSize(new Dimension(60, 30));
            textColorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            textColorBtn.addActionListener(e -> {
                Color chosen = JColorChooser.showDialog(dialog, "Choose Text Color", config.firstLineTextColor);
                if (chosen != null) {
                    config.firstLineTextColor = chosen;
                    textColorPreview.setBackground(chosen);
                }
            });
            colorPanel.add(textColorBtn);
            colorPanel.add(textColorPreview);

            // Outline color
            JCheckBox outlineCheck = new JCheckBox("Enable Outline", config.firstLineOutlineEnabled);
            JButton outlineColorBtn = new JButton("Outline Color");
            JPanel outlineColorPreview = new JPanel();
            outlineColorPreview.setBackground(config.firstLineOutlineColor);
            outlineColorPreview.setPreferredSize(new Dimension(40, 25));
            outlineColorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            outlineCheck.addActionListener(e -> config.firstLineOutlineEnabled = outlineCheck.isSelected());
            outlineColorBtn.addActionListener(e -> {
                Color chosen = JColorChooser.showDialog(dialog, "Choose Outline Color", config.firstLineOutlineColor);
                if (chosen != null) {
                    config.firstLineOutlineColor = chosen;
                    outlineColorPreview.setBackground(chosen);
                }
            });
            colorPanel.add(outlineCheck);
            colorPanel.add(outlineColorBtn);
            colorPanel.add(outlineColorPreview);
            mainPanel.add(colorPanel);

            // === ANIMATION SECTION ===
            JPanel animPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            animPanel.setBorder(BorderFactory.createTitledBorder("Animation Effect"));
            String[] animOptions = {"None", "Fade In", "Typewriter", "Wave", "Bounce", "Glow Pulse", "Scale In", "Slide In"};
            JComboBox<String> animCombo = new JComboBox<>(animOptions);
            animCombo.setSelectedIndex(config.firstLineAnimationType);
            animCombo.addActionListener(e -> config.firstLineAnimationType = animCombo.getSelectedIndex());
            animPanel.add(new JLabel("Animation:"));
            animPanel.add(animCombo);
            mainPanel.add(animPanel);

            // === SHAKE EFFECT SECTION ===
            JPanel shakePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            shakePanel.setBorder(BorderFactory.createTitledBorder("Shake Effect"));
            JCheckBox shakeCheck = new JCheckBox("Enable Shake", config.firstLineShakeEnabled);
            shakeCheck.addActionListener(e -> config.firstLineShakeEnabled = shakeCheck.isSelected());
            JSlider shakeSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, config.firstLineShakeIntensity);
            shakeSlider.setPreferredSize(new Dimension(200, 40));
            shakeSlider.setMajorTickSpacing(5);
            shakeSlider.setPaintTicks(true);
            shakeSlider.setPaintLabels(true);
            JLabel shakeLabel = new JLabel(config.firstLineShakeIntensity + " px");
            shakeSlider.addChangeListener(e -> {
                config.firstLineShakeIntensity = shakeSlider.getValue();
                shakeLabel.setText(config.firstLineShakeIntensity + " px");
            });
            shakePanel.add(shakeCheck);
            shakePanel.add(new JLabel("Intensity:"));
            shakePanel.add(shakeSlider);
            shakePanel.add(shakeLabel);
            mainPanel.add(shakePanel);

            // === TILT EFFECT SECTION ===
            JPanel tiltPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            tiltPanel.setBorder(BorderFactory.createTitledBorder("Tilt / Rotation"));
            JSlider tiltSlider = new JSlider(JSlider.HORIZONTAL, -30, 30, config.firstLineTiltAngle);
            tiltSlider.setPreferredSize(new Dimension(300, 50));
            tiltSlider.setMajorTickSpacing(10);
            tiltSlider.setMinorTickSpacing(5);
            tiltSlider.setPaintTicks(true);
            tiltSlider.setPaintLabels(true);
            JLabel tiltLabel = new JLabel(config.firstLineTiltAngle + "°");
            tiltSlider.addChangeListener(e -> {
                config.firstLineTiltAngle = tiltSlider.getValue();
                tiltLabel.setText(config.firstLineTiltAngle + "°");
            });
            tiltPanel.add(new JLabel("Tilt Angle:"));
            tiltPanel.add(tiltSlider);
            tiltPanel.add(tiltLabel);
            mainPanel.add(tiltPanel);

            // === SHADOW & GLOW SECTION ===
            JPanel effectsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            effectsPanel.setBorder(BorderFactory.createTitledBorder("Shadow & Glow"));

            JCheckBox shadowCheck = new JCheckBox("Enable Shadow", config.firstLineShadowEnabled);
            shadowCheck.addActionListener(e -> config.firstLineShadowEnabled = shadowCheck.isSelected());
            JSpinner shadowOffsetSpinner = new JSpinner(new SpinnerNumberModel(config.firstLineShadowOffset, 1, 10, 1));
            shadowOffsetSpinner.addChangeListener(e -> config.firstLineShadowOffset = (int) shadowOffsetSpinner.getValue());

            JCheckBox glowCheck = new JCheckBox("Enable Glow", config.firstLineGlowEnabled);
            glowCheck.addActionListener(e -> config.firstLineGlowEnabled = glowCheck.isSelected());
            JButton glowColorBtn = new JButton("Glow Color");
            JPanel glowColorPreview = new JPanel();
            glowColorPreview.setBackground(config.firstLineGlowColor);
            glowColorPreview.setPreferredSize(new Dimension(40, 25));
            glowColorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            glowColorBtn.addActionListener(e -> {
                Color chosen = JColorChooser.showDialog(dialog, "Choose Glow Color", config.firstLineGlowColor);
                if (chosen != null) {
                    config.firstLineGlowColor = chosen;
                    glowColorPreview.setBackground(chosen);
                }
            });

            effectsPanel.add(shadowCheck);
            effectsPanel.add(new JLabel("Offset:"));
            effectsPanel.add(shadowOffsetSpinner);
            effectsPanel.add(Box.createHorizontalStrut(20));
            effectsPanel.add(glowCheck);
            effectsPanel.add(glowColorBtn);
            effectsPanel.add(glowColorPreview);
            mainPanel.add(effectsPanel);

            // === PREVIEW SECTION ===
            JPanel previewPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    String sampleText = "نص تجريبي";
                    Font previewFont = new Font("Arial", Font.BOLD, Math.max(20, config.firstLineFontSize / 2));
                    g2d.setFont(previewFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    int textWidth = fm.stringWidth(sampleText);
                    int x = (getWidth() - textWidth) / 2;
                    int y = getHeight() / 2 + fm.getAscent() / 2;

                    // Apply tilt
                    if (config.firstLineTiltAngle != 0) {
                        Graphics2D g2dRotated = (Graphics2D) g2d.create();
                        g2dRotated.rotate(Math.toRadians(config.firstLineTiltAngle), getWidth() / 2, getHeight() / 2);

                        // Draw with effects
                        drawPreviewText(g2dRotated, sampleText, x, y, fm);
                        g2dRotated.dispose();
                    } else {
                        drawPreviewText(g2d, sampleText, x, y, fm);
                    }
                }

                private void drawPreviewText(Graphics2D g2d, String text, int x, int y, FontMetrics fm) {
                    // Glow effect
                    if (config.firstLineGlowEnabled) {
                        g2d.setColor(new Color(config.firstLineGlowColor.getRed(),
                                config.firstLineGlowColor.getGreen(),
                                config.firstLineGlowColor.getBlue(), 100));
                        for (int i = 8; i > 0; i -= 2) {
                            g2d.drawString(text, x - i, y);
                            g2d.drawString(text, x + i, y);
                            g2d.drawString(text, x, y - i);
                            g2d.drawString(text, x, y + i);
                        }
                    }

                    // Shadow
                    if (config.firstLineShadowEnabled) {
                        g2d.setColor(new Color(0, 0, 0, 150));
                        g2d.drawString(text, x + config.firstLineShadowOffset, y + config.firstLineShadowOffset);
                    }

                    // Outline
                    if (config.firstLineOutlineEnabled) {
                        g2d.setColor(config.firstLineOutlineColor);
                        for (int ox = -2; ox <= 2; ox++) {
                            for (int oy = -2; oy <= 2; oy++) {
                                if (ox != 0 || oy != 0) {
                                    g2d.drawString(text, x + ox, y + oy);
                                }
                            }
                        }
                    }

                    // Main text
                    g2d.setColor(config.firstLineTextColor);
                    g2d.drawString(text, x, y);
                }
            };
            previewPanel.setPreferredSize(new Dimension(550, 100));
            previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

            // Refresh preview when settings change
            javax.swing.Timer previewTimer = new javax.swing.Timer(100, e -> previewPanel.repaint());
            previewTimer.start();

            mainPanel.add(previewPanel);

            // === BUTTONS ===
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton resetBtn = new JButton("Reset to Defaults");
            resetBtn.addActionListener(e -> {
                config.firstLineFontSize = 65;
                config.firstLineTextColor = new Color(255, 165, 0);
                config.firstLineAnimationType = 0;
                config.firstLineShakeEnabled = false;
                config.firstLineShakeIntensity = 5;
                config.firstLineTiltAngle = 0;
                config.firstLineShadowEnabled = true;
                config.firstLineShadowOffset = 3;
                config.firstLineOutlineEnabled = true;
                config.firstLineOutlineColor = Color.BLACK;
                config.firstLineGlowEnabled = false;
                config.firstLineGlowColor = new Color(255, 200, 100);

                // Update UI
                fontSizeSlider.setValue(65);
                textColorPreview.setBackground(config.firstLineTextColor);
                animCombo.setSelectedIndex(0);
                shakeCheck.setSelected(false);
                shakeSlider.setValue(5);
                tiltSlider.setValue(0);
                shadowCheck.setSelected(true);
                shadowOffsetSpinner.setValue(3);
                outlineCheck.setSelected(true);
                outlineColorPreview.setBackground(Color.BLACK);
                glowCheck.setSelected(false);
                glowColorPreview.setBackground(new Color(255, 200, 100));
            });

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> {
                previewTimer.stop();
                dialog.dispose();
            });

            buttonPanel.add(resetBtn);
            buttonPanel.add(closeBtn);

            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setBorder(null);

            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    previewTimer.stop();
                }
            });

            dialog.setVisible(true);
        }

        private void browseFolder(JTextField field, String title) {
            ThumbnailFileChooser chooser = new ThumbnailFileChooser();
            chooser.setCurrentDirectory(new File(".")); // Start in current app directory
            chooser.setDialogTitle(title);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }


        private void updateConfig() {
            config.textFilePath = textFileField.getText();
            config.audioFilePath = audioFileField.getText();
            config.arabicFilePath = arabicFileField.getText();
//            config.fontPath1 = font1Field.getText();
//            config.fontPath2 = font2Field.getText();
            config.imageFolder1 = imageFolder1Field.getText();
            config.imageFolder2 = imageFolder2Field.getText();
            config.backgroundMode = backgroundModeCombo.getSelectedIndex();
            config.specificBackgroundImage = specificBackgroundField.getText().trim(); // ADD THIS LINE
// ADD THESE LINES to read effect checkboxes:
            config.enableColorEnhancement = colorEnhancementCheck.isSelected();
            config.enableWaterEffect = waterEffectCheck.isSelected();
            config.enableDynamicLighting = dynamicLightingCheck.isSelected();
            config.enableLensDistortion = lensDistortionCheck.isSelected();
            config.enableVignette = vignetteCheck.isSelected();
            config.enableParticles = particlesCheck.isSelected();
            config.enableLightRays = lightRaysCheck.isSelected();
            config.enableSparkles = sparklesCheck.isSelected();

            // ADD THESE 4 LINES:
            config.enableZoom = zoomCheck.isSelected();
            config.enableFlip = flipCheck.isSelected();
            config.enableGrayscale = grayscaleCheck.isSelected();
            config.enableFallingPieces = fallingPiecesCheck.isSelected();

            config.enableBatchMode = batchModeCheck.isSelected();
            config.batchInputFolder = batchFolderField.getText().trim();

            config.outputVideoName = outputNameField.getText().trim();

            try {
                config.rerunCount = Integer.parseInt(rerunCountField.getText().trim());
                if (config.rerunCount < 1) config.rerunCount = 1;
            } catch (NumberFormatException e) {
                config.rerunCount = 1;
            }

            if (config.outputVideoName.isEmpty()) {
                config.outputVideoName = getCleanArabicVideoName(config.arabicFilePath);
            }

            // ADD THESE LINES FOR FRAME RATE:
            try {
                config.frameRate = Double.parseDouble(frameRateField.getText().trim());
                if (config.frameRate < 1.0) config.frameRate = 1.0;
                if (config.frameRate > 60.0) config.frameRate = 60.0; // Reasonable limits
            } catch (NumberFormatException e) {
                config.frameRate = 10.0; // Default fallback
            }

            if (config.outputVideoName.isEmpty()) {
                config.outputVideoName = getCleanArabicVideoName(config.arabicFilePath);
            }
        }


        private void generateVideo(ActionEvent e) {
            updateConfig();

            if (!validateInputs() && !config.enableBatchMode) return;

            generateButton.setEnabled(false);
            stopButton.setEnabled(true);  // ENABLE IT HERE

            progressBar.setValue(0);
            logArea.setText("");

            SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                //    private arabicSync generator;
                private javax.swing.Timer progressTimer;


                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        generator = new arabicSync();
                        generator.setConfig(config);
                        generator.resetStop();  // ADD THIS

                        //  SwingUtilities.invokeLater(() -> stopButton.setEnabled(true));

                        // Start progress monitoring timer
                        progressTimer = new javax.swing.Timer(500, ae -> {
                            if (generator.getTotalFrames() > 0) {
                                int current = generator.getCurrentFrame();
                                int total = generator.getTotalFrames();
                                int percentage = (int) ((double) current / total * 100);

                                SwingUtilities.invokeLater(() -> {
                                    progressBar.setValue(percentage);
                                    progressBar.setString(current + "/" + total + " frames (" + percentage + "%)");
                                });

                                publish("Frame " + current + "/" + total);
                            }
                        });
                        progressTimer.start();

                        if (config.enableBatchMode) {
                            // BATCH MODE
                            publish("=== BATCH MODE ENABLED ===");
                            publish("Batch folder: " + config.batchInputFolder);
                            publish("Rerun count per quote: " + config.rerunCount);
                            publish("Background mode: " + getBackgroundModeName());
                            publish("Frame rate: " + config.frameRate + " fps");
                            publish("Remove text & background: " + config.removeTextAndBackground);
                            if (config.removeTextAndBackground) {
                                publish("Highlighted words: " + config.highlightWordCount);
                            }
                            publish("");

                            generator.processBatchMode(config.batchInputFolder);

                        } else {
                            // SINGLE MODE
                            publish("=== SINGLE VIDEO MODE ===");
                            publish("English text file: " + config.textFilePath);
                            publish("Arabic text file: " + config.arabicFilePath);
                            publish("Audio file: " + config.audioFilePath);
                            publish("Output name: " + config.outputVideoName);
                            publish("Number of videos: " + config.rerunCount);
                            publish("Background mode: " + getBackgroundModeName());
                            publish("Frame rate: " + config.frameRate + " fps");
                            publish("Remove text & background: " + config.removeTextAndBackground);
                            if (config.removeTextAndBackground) {
                                publish("Highlighted words: " + config.highlightWordCount);
                            }
                            publish("");

                            String arabicQuote = readTextFromFile(config.textFilePath);
                            if (arabicQuote == null || arabicQuote.trim().isEmpty()) {
                                publish("❌ ERROR: Text file is empty or not found!");
                                return null;
                            }

                            publish("Text loaded successfully (" + arabicQuote.length() + " characters)");
                            publish("");

                            for (int run = 1; run <= config.rerunCount; run++) {
                                publish("=== GENERATING VIDEO " + run + " of " + config.rerunCount + " ===");

                                String videoName;
                                if (config.rerunCount == 1) {
                                    videoName = config.outputVideoName + ".mp4";
                                } else {
                                    videoName = config.outputVideoName + "_" + run + ".mp4";
                                }

                                publish("Output: " + videoName);

                                long startTime = System.currentTimeMillis();

                                generator.createVideoWithExactFormattingJigsawArabicAudioSync(
                                        arabicQuote, config.audioFilePath, videoName);

                                long endTime = System.currentTimeMillis();
                                long duration = (endTime - startTime) / 1000;

                                publish("✅ Video " + run + " completed: " + videoName);
                                publish("   Processing time: " + duration + " seconds");
                                publish("");

                                if (run < config.rerunCount) {
                                    Thread.sleep(1000);
                                    publish("Preparing next video...");
                                }
                            }

                            publish("🎉 All " + config.rerunCount + " video(s) generated successfully!");
                        }

                        progressTimer.stop();

                    } catch (Exception ex) {
                        if (progressTimer != null) progressTimer.stop();
                        publish("❌ ERROR: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    return null;
                }


                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String message : chunks) {
                        logArea.append(message + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    }
                }

                @Override
                protected void done() {
                    if (progressTimer != null) progressTimer.stop();
                    generateButton.setEnabled(true);
                    stopButton.setEnabled(false);  // ADD THIS

                    progressBar.setValue(100);
                    progressBar.setString("Complete");
                }
            };

            worker.execute();
        }

        private void stopGeneration() {
            if (generator != null) {
                generator.requestStop();
                logArea.append("⚠ Stop requested - finishing current frame...\n");
                stopButton.setEnabled(false);
            }
        }

        private String getBackgroundModeName() {
            switch (config.backgroundMode) {
                case 0:
                    return "Random Selection";
                case 1:
                    return "Jigsaw Puzzle";
                case 2:
                    return "Slideshow";
                case 3:
                    return "Single Image with Effects";
                default:
                    return "Unknown";
            }
        }


        private File findAudioFileInBatch(File folder, String number) {
            String[] audioExtensions = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"};

            for (String ext : audioExtensions) {
                File audioFile = new File(folder, "audio" + number + ext);
                if (audioFile.exists()) {
                    return audioFile;
                }
            }
            return null;
        }


        private boolean validateInputs() {
            if (!new File(textFileField.getText()).exists()) {
                JOptionPane.showMessageDialog(this, "Text file not found!", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (!new File(audioFileField.getText()).exists()) {
                JOptionPane.showMessageDialog(this, "Audio file not found!", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (!new File(arabicFileField.getText()).exists()) {
                JOptionPane.showMessageDialog(this, "Arabic file not found!", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }


    }


    /**
     * Read text content from a file
     */
    private static String readTextFromFile(String fileName) {
        try {
            File fontFile = new File(fileName);
            if (!fontFile.exists()) {
                System.out.println("File " + fileName + " not found in the current directory.");
                System.out.println("Current directory: " + System.getProperty("user.dir"));
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fontFile), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            // Remove the last newline if present
            if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                content.setLength(content.length() - 1);
            }

            return content.toString();

        } catch (IOException e) {
            System.out.println("Error reading file " + fileName + ": " + e.getMessage());
            return null;
        }
    }


    /**
     * Find the latest added audio file in the directory
     */
    private File findAudioFile(String baseName) {
        String[] extensions = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"};

        // Get current directory
        File currentDir = new File(".");
        File[] allFiles = currentDir.listFiles();

        if (allFiles == null) {
            System.out.println("✗ Could not read current directory");
            return null;
        }

        File latestAudioFile = null;
        long latestModifiedTime = 0;

        // Find all audio files and track the latest one
        for (File file : allFiles) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();

                // Check if it's an audio file
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        long modifiedTime = file.lastModified();

                        if (modifiedTime > latestModifiedTime) {
                            latestModifiedTime = modifiedTime;
                            latestAudioFile = file;
                        }
                        break; // Break inner loop once we find it's an audio file
                    }
                }
            }
        }

        if (latestAudioFile != null) {
            System.out.println("✓ Latest audio file found: " + latestAudioFile.getName());

            // Show the modification date for confirmation
            java.util.Date modDate = new java.util.Date(latestModifiedTime);
            System.out.println("  Modified: " + modDate.toString());

            return latestAudioFile;
        } else {
            System.out.println("✗ No audio files found in directory");
            System.out.println("Supported formats: " + String.join(", ", extensions));
            return null;
        }
    }

    private double getAudioDuration(String audioPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "quiet", "-show_entries", "format=duration",
                    "-of", "csv=p=0", audioPath
            );

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String durationStr = reader.readLine();
                if (durationStr != null && !durationStr.trim().isEmpty()) {
                    return Double.parseDouble(durationStr.trim());
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.out.println("⚠ Could not get audio duration: " + e.getMessage());
        }

        return 0;
    }

    public void createVideoWithExactFormattingJigsawArabicAudioSync(String originalText, String audioFileName, String outputVideo) {
        try {
            File audioFile;

// Check if batch mode is enabled - use exact path
// Otherwise use the auto-find behavior for single mode
            if (config.enableBatchMode) {
                audioFile = new File(audioFileName);
                if (!audioFile.exists() || !audioFile.isFile()) {
                    System.out.println("✗ Audio file not found: " + audioFileName);
                    return;
                }
            } else {
                // Single mode - use existing auto-find behavior
                audioFile = findAudioFile(audioFileName);
                if (audioFile == null) {
                    System.out.println("✗ Audio file not found for Arabic audio sync!");
                    return;
                }
            }

            System.out.println("✓ Audio file found: " + audioFile.getName());
            System.out.println("✓ MODE 8: Arabic audio sync with shake effect for full Arabic text");

            // Get audio duration
            double audioDuration = getAudioDuration(audioFile.getPath());
            if (audioDuration <= 0) {
                audioDuration = 10.0;
            }

            // Parse BOTH English text and Arabic translations
            FormattedTextDataArabicSync formattedData = parseExactFormattingArabicSync(originalText);
            if (formattedData == null) {
                System.out.println("✗ Failed to parse Arabic sync data!");
                return;
            }

            // Generate word timings based on ARABIC text for Arabic audio
            // Generate word timings based on ARABIC text for Arabic audio
            WordTiming[] wordTimings = generateArabicAudioWordTimings(audioFile.getPath(), formattedData);
            if (wordTimings == null || wordTimings.length == 0) {
                System.out.println("✗ Could not generate Arabic word timings. Using estimated timing.");
                wordTimings = generateEstimatedArabicTimings(formattedData, audioDuration);
            }

// Store for use in highlighting methods
            this.currentWordTimings = wordTimings;
            this.currentFormattedData = formattedData;
            // Generate precisely timed sequence with Arabic audio sync
            String tempFolder = generateArabicAudioSyncSequence(formattedData, wordTimings, audioDuration);
            if (tempFolder == null) {
                System.out.println("✗ Failed to generate Arabic audio sync images!");
                return;
            }

            // Create video
            createVideoFromPreciseSequence(tempFolder, audioFile.getPath(), outputVideo, audioDuration);

            // Cleanup
            cleanupTempFolder(tempFolder);

        } catch (Exception e) {
            System.out.println("❌ Error creating Arabic audio sync video: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static String stripPunctuation(String word) {
        if (word == null) return "";

        // Remove ALL punctuation (more comprehensive)
        word = word.replaceAll("[\\p{P}\\p{S}]", "").trim();

        // Remove ALL Arabic diacritics (tashkeel)
        word = word.replaceAll("[\\u064B-\\u0652\\u0670]", "");

        // Normalize Arabic characters
        word = word.replace('أ', 'ا');
        word = word.replace('إ', 'ا');
        word = word.replace('آ', 'ا');
        word = word.replace('ٱ', 'ا');
        word = word.replace('ة', 'ه');
        word = word.replace('ى', 'ي');

        return word.trim();
    }
    private void cleanupTempFolder(String tempFolder) {
        try {
            File tempDir = new File(tempFolder);
            if (tempDir.exists()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                tempDir.delete();
                System.out.println("✓ Cleaned up temporary files");
            }
        } catch (Exception e) {
            System.out.println("⚠ Warning: Could not cleanup temp folder: " + e.getMessage());
        }
    }















    private void createVideoFromPreciseSequence(String tempFolder, String audioPath,
                                                String outputVideo, double audioDuration) {
        try {
// Set waveform Y position based on mode
            // Set waveform Y position based on mode
            int waveformY;
            if (config.removeTextAndBackground) {
                if (config.highlightWordCount == -2) {
                    waveformY = 150; // Stripes mode
                } else {
                    waveformY = 350; // Other remove text modes
                }
            } else if (config.backgroundMode == 5) {
                waveformY = 150; // Image + Text mode - higher position
            } else if (config.backgroundMode == 6) {
                waveformY = -1000; // Image + Text mode - higher position
            } else if (config.backgroundMode == 7) {
                waveformY = -1000; // Image + Text mode - higher position

            } else {
                waveformY = 250; // Normal mode with background
            }
            // Complex filter with waveform and border
            String filterComplex =
                    // Scale and pad base video
                    "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2[padded];" +

                            // Create waveform background
                            "color=black@0.1:s=410x70[bg];" +

                            // Generate audio waveform
                            "[1:a]showwaves=s=400x60:mode=cline:colors=gold|white:scale=sqrt:draw=full[waves];" +

                            // Overlay waveform on background
                            "[bg][waves]overlay=5:5[wave];" +

                            // Combine padded video with waveform
                            //  "[padded][wave]overlay=x=(W-w)/2:y=H*0.6[video_with_wave];" +
                            // "[padded][wave]overlay=x=(W-w)/2:y=250[video_with_wave];" +
                            "[padded][wave]overlay=x=(W-w)/2:y=" + waveformY + "[video_with_wave];" +

                            // Add yellow border
                            "[video_with_wave]drawbox=x=70:y=70:w=iw-140:h=ih-140:color=yellow:t=1[v]";

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-framerate", String.valueOf((int)config.frameRate),
                    "-i", tempFolder + "/frame_%04d.jpg",
                    "-i", audioPath,
                    "-filter_complex", filterComplex,
                    "-map", "[v]",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-crf", "28",
                    "-c:a", "aac",
                    "-b:a", "320k",
                    "-pix_fmt", "yuv420p",
                    "-shortest",
                    outputVideo
            );

            pb.redirectErrorStream(true);
            System.out.println("\n🎬 Creating word-by-word synced video with waveform and border...");

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("time=") || line.contains("error")) {
                        System.out.println("FFmpeg: " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Word-by-word synced video created: " + outputVideo);
            } else {
                System.out.println("❌ FFmpeg failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.out.println("❌ Error creating word-by-word video: " + e.getMessage());
        }
    }













    /**
     * Data structure for Arabic audio sync
     */
    private static class FormattedTextDataArabicSync {
        java.util.List<FormattedLineArabicSync> lines;
        String[] arabicSpeakableWords; // Arabic words for audio sync
        String[] englishLines; // English text for display

        FormattedTextDataArabicSync() {
            this.lines = new java.util.ArrayList<>();
        }
    }

    private static class FormattedLineArabicSync {
        String englishContent;
        String arabicContent;          // For display (with digits)
        String arabicAudioText;        // For ElevenLabs audio (with words)
        int wordStartIndex;
        int wordCount;
        double startTime;
        double endTime;

        FormattedLineArabicSync(String english, String arabic, int startIndex, int count) {
            this.englishContent = english;

            // Parse format: 1932{ألف وتسعمئة...}
            String[] parsed = parseNumberFormatStatic(arabic);
            this.arabicContent = parsed[0];      // Display: "1932"
            this.arabicAudioText = parsed[1];    // Audio: "ألف وتسعمئة..."

            this.wordStartIndex = startIndex;
            this.wordCount = count;
        }

        // Static method to parse number format
        private static String[] parseNumberFormatStatic(String text) {
            Pattern pattern = Pattern.compile("(\\d+)\\{([^}]+)\\}");
            Matcher displayMatcher = pattern.matcher(text);
            Matcher audioMatcher = pattern.matcher(text);

            StringBuffer displayBuffer = new StringBuffer();
            StringBuffer audioBuffer = new StringBuffer();

            while (displayMatcher.find()) {
                String digits = displayMatcher.group(1);
                displayMatcher.appendReplacement(displayBuffer, digits);
            }
            displayMatcher.appendTail(displayBuffer);

            while (audioMatcher.find()) {
                String words = audioMatcher.group(2);
                audioMatcher.appendReplacement(audioBuffer, words);
            }
            audioMatcher.appendTail(audioBuffer);

            return new String[]{displayBuffer.toString(), audioBuffer.toString()};
        }
    }
    /**
     * Parse both English text and Arabic translations for audio sync
     */
    private FormattedTextDataArabicSync parseExactFormattingArabicSync(String originalEnglishText) {
        FormattedTextDataArabicSync data = new FormattedTextDataArabicSync();

        // Parse English lines
        String[] englishLines = originalEnglishText.split("\n");

        // Load Arabic translations
        String[] arabicTranslations = loadArabicTranslationsFromFile();
        if (arabicTranslations == null) {
            System.out.println("✗ Could not load Arabic translations from quoteAR.txt!");
            return null;
        }

        java.util.List<String> arabicSpeakableWordsList = new java.util.ArrayList<>();
        java.util.List<FormattedLineArabicSync> validLines = new java.util.ArrayList<>();

        System.out.println("🔤 Parsing for Arabic audio sync...");

        int globalArabicWordIndex = 0;
        int lineIndex = 0;

        // Process each non-empty English line with its Arabic translation
        for (String englishLine : englishLines) {
            String trimmedEnglishLine = englishLine.trim();
            if (!trimmedEnglishLine.isEmpty() && lineIndex < arabicTranslations.length) {

                String arabicTranslation = arabicTranslations[lineIndex].trim();
                if (arabicTranslation.isEmpty()) {
                    arabicTranslation = "ترجمة: " + trimmedEnglishLine; // Fallback
                }

// Parse to get audio version (with words for ElevenLabs)
                String[] parsed = parseNumberFormat(arabicTranslation);
                String arabicAudioText = parsed[1];  // Use audio version with words

                // Split Arabic translation into words for audio sync
                String[] arabicWordsInLine = arabicAudioText.split("\\s+");
                int arabicWordCount = 0;

                for (String arabicWord : arabicWordsInLine) {
                    String cleanArabicWord = stripPunctuation(arabicWord.trim());  // Strip punctuation here too!
                    if (!cleanArabicWord.isEmpty()) {
                        arabicSpeakableWordsList.add(cleanArabicWord);
                        arabicWordCount++;
                    }
                }

                // Store this line pair
                validLines.add(new FormattedLineArabicSync(trimmedEnglishLine, arabicTranslation,
                        globalArabicWordIndex, arabicWordCount));

                System.out.println("Arabic Sync Quote " + (lineIndex + 1) + ":");
                System.out.println("  English: '" + trimmedEnglishLine + "'");
                System.out.println("  Arabic: '" + arabicTranslation + "'");
                System.out.println("  Arabic words " + globalArabicWordIndex + " to " + (globalArabicWordIndex + arabicWordCount - 1));

                globalArabicWordIndex += arabicWordCount;
                lineIndex++;
            }
        }

        data.lines = validLines;
        data.arabicSpeakableWords = arabicSpeakableWordsList.toArray(new String[0]);
        data.englishLines = englishLines;

        System.out.println("✓ Parsed " + validLines.size() + " quote pairs with " + data.arabicSpeakableWords.length + " total Arabic words");

        return data;
    }

    /**
     * Generate word timings for Arabic audio
     */


    private WordTiming[] generateArabicAudioWordTimings(String audioPath, FormattedTextDataArabicSync formattedData) {
        try {
            // Create temporary text file with Arabic words
            File textFile = File.createTempFile("arabic_audio_transcript", ".txt");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(textFile, "UTF-8")) {
                writer.println(String.join(" ", formattedData.arabicSpeakableWords));
            }

            // Use ElevenLabs speech-to-text instead of Whisper
            String nodeScript = createElevenLabsSTTScript();
            File scriptFile = new File("elevenlabs_stt_arabic.js");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(scriptFile, "UTF-8")) {
                writer.println(nodeScript);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "node", "elevenlabs_stt_arabic.js",
                    audioPath, textFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            System.out.println("🎯 Running ElevenLabs Arabic audio alignment...");

            Process process = pb.start();

            java.util.List<WordTiming> rawTimings = new java.util.ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("TIMING:")) {
                        String[] parts = line.split(":");
                        if (parts.length == 4) {
                            String word = stripPunctuation(parts[1]);  // Strip punctuation here!
                            double startTime = Double.parseDouble(parts[2]);
                            double endTime = Double.parseDouble(parts[3]);
                            rawTimings.add(new WordTiming(word, startTime, endTime));
                        }
                    }
                    System.out.println("ElevenLabs Arabic Alignment: " + line);
                }
            }

            int exitCode = process.waitFor();

            // Cleanup
            textFile.delete();
            scriptFile.delete();

            if (exitCode == 0 && !rawTimings.isEmpty()) {
                System.out.println("✅ Generated " + rawTimings.size() + " ElevenLabs Arabic audio word timings");
                // Return raw ElevenLabs timings directly - NO fuzzy matching needed
                // ElevenLabs gives us accurate timings from the actual audio
                return rawTimings.toArray(new WordTiming[0]);
            } else {
                return null;
            }

        } catch (Exception e) {
            System.out.println("❌ Error in ElevenLabs Arabic audio alignment: " + e.getMessage());
            return null;
        }
    }

    private String createElevenLabsSTTScript() {
        return """
                const { ElevenLabsClient } = require('elevenlabs');
                const fs = require('fs');
                const axios = require('axios');
                const FormData = require('form-data');

                // Your ElevenLabs API key
                const API_KEY = 'sk_085b309952bcc3227379faa49e8f49d40478fda3985840e7';

                async function elevenLabsSTT(audioPath, textPath) {
                    try {
                        // Read expected Arabic text
                        const expectedText = fs.readFileSync(textPath, 'utf8').trim();
                        
                        console.log('🎧 Transcribing Arabic audio with ElevenLabs Scribe...');
                        console.log('Expected text: ' + expectedText.substring(0, 100) + '...');
                        
                        // Create form data for ElevenLabs API
                        const formData = new FormData();
                        formData.append('file', fs.createReadStream(audioPath));
                        formData.append('model_id', 'scribe_v1');
                        formData.append('language', 'ar'); // Arabic language
                        formData.append('word_timestamps', 'true'); // Request word-level timestamps
                        
                        // Make request to ElevenLabs speech-to-text API
                        const response = await axios.post('https://api.elevenlabs.io/v1/speech-to-text', formData, {
                            headers: {
                                'xi-api-key': API_KEY,
                                ...formData.getHeaders()
                            }
                        });
                        
                        const result = response.data;
                        console.log('ElevenLabs transcription result:', JSON.stringify(result, null, 2));
                        
                        // Extract word timings from ElevenLabs response
                        if (result.words && result.words.length > 0) {
                            console.log('✅ Found ' + result.words.length + ' words with timestamps');
                            
                            // Output word timings in the expected format
                          // Output word timings in the expected format
                          
//                             for (const wordData of result.words) {
//                                 const word = wordData.word || wordData.text || '';
//                                 const startTime = wordData.start || wordData.start_time || 0;
//                                 const endTime = wordData.end || wordData.end_time || startTime + 0.5;
//                             // Strip punctuation and normalize the word before outputting
//                                 const cleanWord = word.trim().replace(/[\\p{P}\\p{S}]/gu, '').replace(/[\\u064B-\\u0652\\u0670]/g, '');
//                                 if (cleanWord) {
//                                     console.log(`TIMING:${cleanWord}:${startTime.toFixed(3)}:${endTime.toFixed(3)}`);
//                                 }
//                             }
                            // Output word timings in the expected format
                                            for (const wordData of result.words) {
                                                const rawWord = wordData.word || wordData.text || '';
                                                const startTime = wordData.start || wordData.start_time || 0;
                                                const endTime = wordData.end || wordData.end_time || startTime + 0.5;
                                               // Clean the word: remove ALL punctuation, symbols, and diacritics
                                                let cleanWord = rawWord.trim();
                                                // Remove ALL punctuation and symbols (Unicode categories P and S)
                                                cleanWord = cleanWord.replace(/[\\p{P}\\p{S}]/gu, '');
                                              // Remove Arabic diacritics (tashkeel)
                                                cleanWord = cleanWord.replace(/[\\u064B-\\u0652\\u0670]/g, '');
                                               // Remove any remaining special characters (keep only letters and numbers)
                                                cleanWord = cleanWord.replace(/[^\\p{L}\\p{N}]/gu, '');
                                                if (cleanWord) {
                                                    console.log(`TIMING:${cleanWord}:${startTime.toFixed(3)}:${endTime.toFixed(3)}`);
                                                }
                                            }
                            console.log('SUCCESS: Generated ElevenLabs word timings for ' + result.words.length + ' words');
                        } else if (result.transcript || result.text) {
                            // Fallback if no word-level timestamps available
                            console.log('WARNING: No word timestamps from ElevenLabs, using fallback');
                            fallbackTiming(expectedText, 10.0); // Assume 10 second duration
                        } else {
                            console.log('ERROR: Unexpected ElevenLabs response format');
                            fallbackTiming(expectedText, 10.0);
                        }
                        
                    } catch (error) {
                        console.log('ERROR: ElevenLabs STT failed: ' + error.message);
                        
                        // Read expected text for fallback
                        const expectedText = fs.readFileSync(textPath, 'utf8').trim();
                        fallbackTiming(expectedText, 10.0);
                    }
                }

                function fallbackTiming(text, duration) {
                    const words = text.split(/\\s+/);
                    const timePerWord = duration / words.length;
                    
                    console.log('FALLBACK: Using estimated timings for ' + words.length + ' words');
                    
                    for (let i = 0; i < words.length; i++) {
                        const startTime = i * timePerWord;
                        const endTime = (i + 1) * timePerWord;
                        const cleanWord = words[i].replace(/[^\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF\\p{L}\\p{N}]/gu, '').trim();
                        
                        if (cleanWord) {
                            console.log(`TIMING:${cleanWord}:${startTime.toFixed(3)}:${endTime.toFixed(3)}`);
                        }
                    }
                }

                // Main execution
                if (process.argv.length !== 4) {
                    console.log('Usage: node elevenlabs_stt_arabic.js <audio_file> <text_file>');
                    process.exit(1);
                }

                const audioPath = process.argv[2];
                const textPath = process.argv[3];

                elevenLabsSTT(audioPath, textPath);
                """;
    }


    /**
     * Align Arabic transcript to our text
     */
    /**
     * Align Arabic transcript from speech-to-text to our text using fuzzy matching.
     * This ensures 100% accurate sync by properly matching words even with minor differences.
     */
    private WordTiming[] alignArabicTranscriptToText(java.util.List<WordTiming> rawTimings, FormattedTextDataArabicSync formattedData) {
        System.out.println("🔄 Aligning Arabic audio transcript with fuzzy matching...");

        String[] ourArabicWords = formattedData.arabicSpeakableWords;
        WordTiming[] alignedTimings = new WordTiming[ourArabicWords.length];

        if (rawTimings == null || rawTimings.isEmpty()) {
            System.out.println("⚠ No raw timings provided, cannot align");
            return null;
        }

        // Use dynamic programming approach for better alignment
        int ourIndex = 0;
        int rawIndex = 0;

        while (ourIndex < ourArabicWords.length && rawIndex < rawTimings.size()) {
            WordTiming rawTiming = rawTimings.get(rawIndex);
            String ourWord = stripPunctuation(ourArabicWords[ourIndex]);
            String rawWord = stripPunctuation(rawTiming.word);

            // Calculate similarity score
            double similarity = calculateWordSimilarity(ourWord, rawWord);

            if (similarity >= 0.7) {
                // Good match - use exact timing (NO advance offset for accurate sync)
                alignedTimings[ourIndex] = new WordTiming(ourArabicWords[ourIndex],
                        rawTiming.startTime, rawTiming.endTime);
                ourIndex++;
                rawIndex++;
            } else if (rawIndex + 1 < rawTimings.size()) {
                // Check if next raw word matches better
                String nextRawWord = stripPunctuation(rawTimings.get(rawIndex + 1).word);
                double nextSimilarity = calculateWordSimilarity(ourWord, nextRawWord);

                if (nextSimilarity > similarity) {
                    // Skip current raw word (probably extra word in speech)
                    rawIndex++;
                } else {
                    // Use current timing even with low similarity
                    alignedTimings[ourIndex] = new WordTiming(ourArabicWords[ourIndex],
                            rawTiming.startTime, rawTiming.endTime);
                    ourIndex++;
                    rawIndex++;
                }
            } else {
                // Use current timing
                alignedTimings[ourIndex] = new WordTiming(ourArabicWords[ourIndex],
                        rawTiming.startTime, rawTiming.endTime);
                ourIndex++;
                rawIndex++;
            }
        }

        // Handle remaining our words with interpolated timing
        if (ourIndex < ourArabicWords.length) {
            double lastEndTime = rawTimings.get(rawTimings.size() - 1).endTime;
            int remainingWords = ourArabicWords.length - ourIndex;
            double avgWordDuration = calculateAverageWordDuration(rawTimings);

            for (int i = ourIndex; i < ourArabicWords.length; i++) {
                double wordDuration = avgWordDuration * getWordLengthFactor(ourArabicWords[i]);
                double startTime = (i == ourIndex) ? lastEndTime : alignedTimings[i - 1].endTime;
                double endTime = startTime + wordDuration;
                alignedTimings[i] = new WordTiming(ourArabicWords[i], startTime, endTime);
            }
        }

        // Validate and fix any timing gaps or overlaps
        alignedTimings = validateAndFixTimings(alignedTimings);

        System.out.println("✅ Arabic audio alignment complete: " + ourArabicWords.length + " words aligned");
        return alignedTimings;
    }

    /**
     * Calculate similarity between two words (0.0 to 1.0)
     */
    private double calculateWordSimilarity(String word1, String word2) {
        if (word1 == null || word2 == null) return 0.0;
        if (word1.isEmpty() || word2.isEmpty()) return 0.0;
        if (word1.equals(word2)) return 1.0;

        // Normalize both words
        word1 = normalizeArabicWord(word1);
        word2 = normalizeArabicWord(word2);

        if (word1.equals(word2)) return 1.0;

        // Calculate Levenshtein distance ratio
        int maxLen = Math.max(word1.length(), word2.length());
        if (maxLen == 0) return 1.0;

        int distance = levenshteinDistance(word1, word2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Normalize Arabic word for comparison
     */
    private String normalizeArabicWord(String word) {
        if (word == null) return "";
        // Remove diacritics
        word = word.replaceAll("[\\u064B-\\u0652\\u0670]", "");
        // Normalize alef variants
        word = word.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا');
        // Normalize taa marbuta
        word = word.replace('ة', 'ه');
        // Normalize yaa
        word = word.replace('ى', 'ي');
        return word.trim();
    }

    /**
     * Calculate Levenshtein edit distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,      // deletion
                                dp[i][j - 1] + 1),     // insertion
                        dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculate average word duration from raw timings
     */
    private double calculateAverageWordDuration(java.util.List<WordTiming> timings) {
        if (timings == null || timings.isEmpty()) return 0.5;
        double totalDuration = 0;
        for (WordTiming t : timings) {
            totalDuration += (t.endTime - t.startTime);
        }
        return totalDuration / timings.size();
    }

    /**
     * Get word length factor for duration estimation (longer words take more time)
     */
    private double getWordLengthFactor(String word) {
        if (word == null || word.isEmpty()) return 1.0;
        int len = word.length();
        if (len <= 2) return 0.7;
        if (len <= 4) return 1.0;
        if (len <= 6) return 1.3;
        return 1.5;
    }

    /**
     * Validate and fix timing gaps/overlaps to ensure seamless sync
     */
    private WordTiming[] validateAndFixTimings(WordTiming[] timings) {
        if (timings == null || timings.length == 0) return timings;

        for (int i = 1; i < timings.length; i++) {
            if (timings[i] == null) continue;
            if (timings[i - 1] == null) continue;

            // Fix gap: if there's a gap between words, extend previous word's end time
            if (timings[i].startTime > timings[i - 1].endTime) {
                double gap = timings[i].startTime - timings[i - 1].endTime;
                // Split the gap: extend previous word and advance current word
                timings[i - 1] = new WordTiming(timings[i - 1].word,
                        timings[i - 1].startTime, timings[i - 1].endTime + gap / 2);
                timings[i] = new WordTiming(timings[i].word,
                        timings[i].startTime - gap / 2, timings[i].endTime);
            }

            // Fix overlap: if words overlap, adjust boundaries
            if (timings[i].startTime < timings[i - 1].endTime) {
                double midpoint = (timings[i - 1].endTime + timings[i].startTime) / 2;
                timings[i - 1] = new WordTiming(timings[i - 1].word,
                        timings[i - 1].startTime, midpoint);
                timings[i] = new WordTiming(timings[i].word,
                        midpoint, timings[i].endTime);
            }
        }

        return timings;
    }

    /**
     * Generate estimated Arabic timings based on word length (longer words = more time)
     * This provides better sync than equal distribution.
     */
    private WordTiming[] generateEstimatedArabicTimings(FormattedTextDataArabicSync formattedData, double audioDuration) {
        String[] words = formattedData.arabicSpeakableWords;
        WordTiming[] timings = new WordTiming[words.length];

        if (words.length == 0) return timings;

        // Calculate total weighted length
        double totalWeight = 0;
        double[] weights = new double[words.length];
        for (int i = 0; i < words.length; i++) {
            weights[i] = getWordLengthFactor(words[i]);
            totalWeight += weights[i];
        }

        // Distribute time proportionally based on word weight
        double currentTime = 0;
        for (int i = 0; i < words.length; i++) {
            double wordDuration = (weights[i] / totalWeight) * audioDuration;
            double startTime = currentTime;
            double endTime = currentTime + wordDuration;
            timings[i] = new WordTiming(words[i], startTime, endTime);
            currentTime = endTime;
        }

        // Ensure last word ends exactly at audio duration
        if (words.length > 0) {
            timings[words.length - 1] = new WordTiming(
                    timings[words.length - 1].word,
                    timings[words.length - 1].startTime,
                    audioDuration
            );
        }

        System.out.println("⚠ Using weighted estimated Arabic timings (word-length based)");
        return timings;
    }

    /**
     * Generate image sequence with Arabic audio sync
     */


    private String generateArabicAudioSyncSequence(FormattedTextDataArabicSync formattedData, WordTiming[] wordTimings, double audioDuration) {
        try {
            //llllllllllllllllllllllllllllllllllllllllll

            double frameRate = config.frameRate;

            int totalFrames = (int) Math.ceil(audioDuration * frameRate);

            this.totalFrames = totalFrames;
            this.currentFrame = 0;

            String tempFolder = "temp_arabic_audio_sync_" + System.currentTimeMillis();
            File tempDir = new File(tempFolder);
            tempDir.mkdirs();

            System.out.println("🎬 Generating " + totalFrames + " frames with Arabic audio sync...");

            Font preloadedEnglishFont = loadCustomFont();
            Font preloadedArabicFont = loadArabicFont();

            // Calculate quote timings based on Arabic audio
            QuoteTimingInfoArabicSync[] quoteTimings = calculateArabicQuoteTimings(formattedData, wordTimings, audioDuration);


            //lllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll
            // RANDOMLY CHOOSE BACKGROUND STYLE (same as Mode 7)
            // RANDOMLY CHOOSE BACKGROUND STYLE - 3 options
            java.util.Random backgroundChoice = new java.util.Random();
            boolean useJigsawPuzzle = false;
            boolean useRandomSlideshow = false;

// Use config.backgroundMode instead of random selection
            switch (config.backgroundMode) {
                case 0: // Random Selection
                    double choice = backgroundChoice.nextDouble();
                    useJigsawPuzzle = choice < 0.33;
                    useRandomSlideshow = choice >= 0.33 && choice < 0.66;
                    break;
                case 1: // Jigsaw Puzzle
                    useJigsawPuzzle = true;
                    break;
                case 2: // Slideshow
                    useRandomSlideshow = true;
                    break;
                case 3: // Single Image with Effects
                    // Both false = single image mode
                    break;
            }


            JigsawPiece[] jigsawPieces = null;
            BufferedImage singleEffectImage = null;


            if (useJigsawPuzzle) {
                System.out.println("🧩 Selected: JIGSAW PUZZLE background for Arabic audio sync");
                BufferedImage jigsawBackground = loadJigsawBackgroundImage();
                if (jigsawBackground != null) {
                    BufferedImage scaledBackground = scaleImageToVideoDimensions(jigsawBackground, 1080, 1920);
                    int puzzleGridSize = calculateOptimalPuzzleSize(audioDuration);
                    jigsawPieces = generateJigsawPieces(scaledBackground, puzzleGridSize);
                } else {
                    useJigsawPuzzle = false;
                    useRandomSlideshow = true;
                }
            } else if (useRandomSlideshow) {
                System.out.println("🎬 Selected: RANDOM SLIDESHOW from imgchng2 folder for Arabic audio sync");
            } else if (config.backgroundMode == 4) {
                System.out.println("🎨 Selected: SOLID COLOR with effects for Arabic audio sync");
                singleEffectImage = createSolidColorImage(config.backgroundColor, 1080, 1920);
            } else if (config.backgroundMode == 5) {
                System.out.println("🖼️ Selected: IMAGE + TEXT mode for Arabic audio sync");
                // No pre-loading needed for this mode - handled per frame
            } else if (config.backgroundMode == 6) {
                System.out.println("❓ Selected: QUIZ MODE for Arabic audio sync");
                // No pre-loading needed for quiz mode - handled per frame
            } else if (config.backgroundMode == 7) {
                System.out.println("🖼️ Selected: IMAGES PER LINE mode for Arabic audio sync");
                // No pre-loading needed - images loaded per line

            } else {
                System.out.println("🎨 Selected: SINGLE IMAGE with effects for Arabic audio sync");
                singleEffectImage = loadSingleRandomImageForEffects();
                if (singleEffectImage != null) {
                    singleEffectImage = scaleImageToVideoDimensions(singleEffectImage, 1080, 1920);
                }
            }

            // Generate frames
            for (int frame = 0; frame < totalFrames; frame++) {

                // ADD THIS CHECK:
                if (stopRequested) {
                    System.out.println("Generation stopped at frame " + frame);
                    return tempFolder;
                }
                this.currentFrame = frame + 1; // Update current frame counter

                double currentTime = (double) frame / frameRate;

                // Determine current quote being spoken
                QuoteDisplayInfoArabicSync displayInfo = getCurrentArabicQuoteDisplayInfo(currentTime, quoteTimings);
// ADD THIS DEBUG (only print every 50 frames to avoid spam)
                if (frame % 50 == 0) {
                    System.out.println("DEBUG Frame " + frame + ": time=" + String.format("%.2f", currentTime) +
                            "s, currentQuote=" + displayInfo.currentQuote +
                            ", isActive=" + displayInfo.isActive);
                }
                // Generate frame
                String frameName = String.format("%s/frame_%04d.jpg", tempFolder, frame);

                if (useJigsawPuzzle) {
                    double completionPercentage = currentTime / audioDuration;
                    updateJigsawPieces(jigsawPieces, completionPercentage);
                    generateArabicSyncJigsawFrame(formattedData, displayInfo, frameName, jigsawPieces, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                } else if (useRandomSlideshow) {
                    generateArabicSyncSlideshowFrame(formattedData, displayInfo, frameName, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                } else if (config.backgroundMode == 5) {
                    generateImagePlusTextFrame(formattedData, displayInfo, frameName, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                } else if (config.backgroundMode == 6) {
                    generateQuizFrame(formattedData, displayInfo, frameName, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                } else if (config.backgroundMode == 7) {
                    generateImagesPerLineFrame(formattedData, displayInfo, frameName, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                } else {
                    generateArabicSyncSingleImageFrame(formattedData, displayInfo, frameName, singleEffectImage, currentTime,
                            preloadedEnglishFont, preloadedArabicFont, audioDuration);
                }
            }


            System.out.println("✅ Arabic audio sync sequence generated!");
            return tempFolder;

        } catch (Exception e) {
            System.out.println("❌ Error generating Arabic audio sync sequence: " + e.getMessage());
            return null;
        }
    }

    private BufferedImage createSolidColorImage(Color color, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }
    private BufferedImage scaleImageToVideoDimensions(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return scaled;
    }


//    private void generateQuizFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
//                                   String outputPath, double currentTime,
//                                   Font englishFont, Font arabicFont, double totalDuration) throws Exception {
//
//        int width = 1080;
//        int height = 1920;
//
//        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = image.createGraphics();
//
//        // High-quality rendering
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//
//        // Draw gradient background
//        GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 60),
//                0, height, new Color(60, 30, 80));
//        g2d.setPaint(gradient);
//        g2d.fillRect(0, 0, width, height);
//
//        // Font sizes
//        Font questionFont = arabicFont.deriveFont(Font.BOLD, 55f);
//        Font answerFont = arabicFont.deriveFont(Font.PLAIN, 50f);
//        FontMetrics qfm = g2d.getFontMetrics(questionFont);
//        FontMetrics afm = g2d.getFontMetrics(answerFont);
//
//        int sidePadding = 130;  // THIS NOW CONTROLS SIDE PADDING
//        int textWidth = width - (2 * sidePadding);  // PROPERLY APPLIED
//        int qaSpacing = 1; // Very close spacing between question and answer
//
//        // First pass: calculate total content height
//        int totalContentHeight = 0;
//        java.util.List<java.util.List<String>> allQuestionLines = new java.util.ArrayList<>();
//        java.util.List<java.util.List<String>> allAnswerLines = new java.util.ArrayList<>();
//
//        for (int i = 0; i < formattedData.lines.size(); i++) {
//            FormattedLineArabicSync line = formattedData.lines.get(i);
//            String fullText = line.arabicContent;
//
//            // Split by question mark
//            String[] parts = fullText.split("\\?|؟", 2);
//            String question = parts.length > 0 ? parts[0].trim() + "؟" : "";
//            String answer = parts.length > 1 ? parts[1].trim() : "";
//
//            // Wrap text
//            java.util.List<String> questionLines = wrapTextToLines(question, qfm, textWidth);
//            java.util.List<String> answerLines = wrapTextToLines(answer, afm, textWidth);
//
//            allQuestionLines.add(questionLines);
//            allAnswerLines.add(answerLines);
//
//            // Calculate height for this Q&A pair
//            totalContentHeight += questionLines.size() * (int)(qfm.getHeight() * 1.1);
//            totalContentHeight += qaSpacing;
//            totalContentHeight += answerLines.size() * (int)(afm.getHeight() * 1.1);
//        }
//
//        // Calculate spacing between Q&A pairs to distribute evenly
//        int topMargin = 150;
//        int bottomMargin = 100;
//        int availableHeight = height - topMargin - bottomMargin - totalContentHeight;
//        int spaceBetweenPairs = formattedData.lines.size() > 1 ?
//                availableHeight / (formattedData.lines.size() - 1) : 0;
//
//        // Ensure minimum spacing
//        spaceBetweenPairs = Math.max(spaceBetweenPairs, 30);
//
//        // Start drawing from top with margin
//        int currentY = topMargin;
//
//        // Draw all questions and answers
//        for (int i = 0; i < formattedData.lines.size(); i++) {
//            FormattedLineArabicSync line = formattedData.lines.get(i);
//            String fullText = line.arabicContent;
//
//            // Split by question mark
//            String[] parts = fullText.split("\\?|؟", 2);
//            String question = parts.length > 0 ? parts[0].trim() + "؟" : "";
//            String answer = parts.length > 1 ? parts[1].trim() : "";
//
//            // Calculate timing for this question's answer reveal
//            int questionWordCount = question.split("\\s+").length;
//            int startIdx = line.wordStartIndex;
//            int questionEndIdx = Math.min(startIdx + questionWordCount - 1,
//                    currentWordTimings != null ? currentWordTimings.length - 1 : startIdx);
//
//            double answerRevealTime = 0;
//            if (currentWordTimings != null && questionEndIdx < currentWordTimings.length) {
//                answerRevealTime = currentWordTimings[questionEndIdx].endTime;
//            }
//
//            boolean showAnswer = currentTime >= answerRevealTime;
//
//            // Get wrapped lines
//            java.util.List<String> questionLines = allQuestionLines.get(i);
//            java.util.List<String> answerLines = allAnswerLines.get(i);
//
//            // Draw question
//            g2d.setFont(questionFont);
//            for (int lineIdx = 0; lineIdx < questionLines.size(); lineIdx++) {
//                String qLine = questionLines.get(lineIdx);
//                int lineWidth = qfm.stringWidth(qLine);
//                int centerX = (width - lineWidth) / 2;
//
//                // Shadow
//                g2d.setColor(new Color(0, 0, 0, 180));
//                g2d.drawString(qLine, centerX + 2, currentY + 2);
//
//                // Main text - gold color for questions
//                g2d.setColor(new Color(255, 215, 0));
//                g2d.drawString(qLine, centerX, currentY);
//
//                currentY += (int)(qfm.getHeight() * 1.1);
//            }
//
//            currentY += qaSpacing;
//
//            // Draw answer (if it's time to reveal)
//            if (showAnswer && !answer.isEmpty()) {
//                g2d.setFont(answerFont);
//
//                // Fade-in effect for answer
//                double timeSinceReveal = currentTime - answerRevealTime;
//                float fadeAlpha = (float)Math.min(1.0, timeSinceReveal / 0.5); // 0.5 second fade
//
//                for (int lineIdx = 0; lineIdx < answerLines.size(); lineIdx++) {
//                    String aLine = answerLines.get(lineIdx);
//                    int lineWidth = afm.stringWidth(aLine);
//                    int centerX = (width - lineWidth) / 2;
//
//                    // Apply shake effect when answer first appears
//                    int[] shakeOffset = new int[]{0, 0};
//                    if (timeSinceReveal < 1.0) { // Shake for first second
//                        shakeOffset = calculateShakeOffset(currentTime, 3);
//                    }
//                    int shakeX = centerX + shakeOffset[0];
//                    int shakeY = currentY + shakeOffset[1];
//
//                    // Shadow
//                    g2d.setColor(new Color(0, 0, 0, (int)(150 * fadeAlpha)));
//                    g2d.drawString(aLine, shakeX + 2, shakeY + 2);
//
//                    // Glow effect
//                    g2d.setColor(new Color(100, 255, 100, (int)(100 * fadeAlpha)));
//                    for (int offset = 1; offset <= 2; offset++) {
//                        g2d.drawString(aLine, shakeX - offset, shakeY);
//                        g2d.drawString(aLine, shakeX + offset, shakeY);
//                        g2d.drawString(aLine, shakeX, shakeY - offset);
//                        g2d.drawString(aLine, shakeX, shakeY + offset);
//                    }
//
//                    // Main text - white color for answers
//                    g2d.setColor(new Color(255, 255, 255, (int)(255 * fadeAlpha)));
//                    g2d.drawString(aLine, shakeX, shakeY);
//
//                    currentY += (int)(afm.getHeight() * 1.1);
//                }
//            } else if (!showAnswer) {
//                // Reserve space for answer (invisible placeholder)
//                currentY += answerLines.size() * (int)(afm.getHeight() * 1.1);
//            }
//
//            // Add spacing between Q&A pairs (except after last one)
//            if (i < formattedData.lines.size() - 1) {
//                currentY += spaceBetweenPairs;
//            }
//        }
//
//        g2d.dispose();
//        ImageIO.write(image, "JPEG", new File(outputPath));
//    }

    private void generateQuizFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                   String outputPath, double currentTime,
                                   Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Load and draw background image
        BufferedImage bgImage = loadQuizBackgroundImage();
        if (bgImage != null) {
            // Scale to fit screen
            bgImage = scaleImageToVideoDimensions(bgImage, width, height);
            g2d.drawImage(bgImage, 0, 0, null);

            // Optional: Add semi-transparent overlay for better text readability
            g2d.setColor(new Color(0, 0, 0, 100)); // Adjust opacity (0-255)
            g2d.fillRect(0, 0, width, height);
        } else {
            // Fallback: gradient background if no image
            GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 60),
                    0, height, new Color(60, 30, 80));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);
        }

        // Add animated particles in background
        drawAnimatedParticles(g2d, width, height, currentTime);

        // Add subtle light rays
        drawSubtleLightRays(g2d, width, height, currentTime);

        // Font sizes
        Font questionFont = arabicFont.deriveFont(Font.BOLD, 65f);
        Font answerFont = arabicFont.deriveFont(Font.PLAIN, 55f);
        FontMetrics qfm = g2d.getFontMetrics(questionFont);
        FontMetrics afm = g2d.getFontMetrics(answerFont);

        int sidePadding = 130;
        int textWidth = width - (2 * sidePadding);
        int qaSpacing = 1;

        // First pass: calculate total content height
        int totalContentHeight = 0;
        java.util.List<java.util.List<String>> allQuestionLines = new java.util.ArrayList<>();
        java.util.List<java.util.List<String>> allAnswerLines = new java.util.ArrayList<>();

        for (int i = 0; i < formattedData.lines.size(); i++) {
            FormattedLineArabicSync line = formattedData.lines.get(i);
            String fullText = line.arabicContent;

            String[] parts = fullText.split("\\?|؟", 2);
            String question = parts.length > 0 ? parts[0].trim() + "؟" : "";
            String answer = parts.length > 1 ? parts[1].trim() : "";

            java.util.List<String> questionLines = wrapTextToLines(question, qfm, textWidth);
            java.util.List<String> answerLines = wrapTextToLines(answer, afm, textWidth);

            allQuestionLines.add(questionLines);
            allAnswerLines.add(answerLines);

            totalContentHeight += questionLines.size() * (int)(qfm.getHeight() * 1.1);
            totalContentHeight += qaSpacing;
            totalContentHeight += answerLines.size() * (int)(afm.getHeight() * 1.1);
        }

        int topMargin = 150;
        int bottomMargin = 120;
        int availableHeight = height - topMargin - bottomMargin - totalContentHeight;
        int spaceBetweenPairs = formattedData.lines.size() > 1 ?
                availableHeight / (formattedData.lines.size() - 1) : 0;
        spaceBetweenPairs = Math.max(spaceBetweenPairs, 30);

        int currentY = topMargin;

        // Draw all questions and answers
        for (int i = 0; i < formattedData.lines.size(); i++) {
            FormattedLineArabicSync line = formattedData.lines.get(i);
            String fullText = line.arabicContent;

            String[] parts = fullText.split("\\?|؟", 2);
            String question = parts.length > 0 ? parts[0].trim() + "؟" : "";
            String answer = parts.length > 1 ? parts[1].trim() : "";

            int questionWordCount = question.split("\\s+").length;
            int startIdx = line.wordStartIndex;
            int questionEndIdx = Math.min(startIdx + questionWordCount - 1,
                    currentWordTimings != null ? currentWordTimings.length - 1 : startIdx);

            double answerRevealTime = 0;
            if (currentWordTimings != null && questionEndIdx < currentWordTimings.length) {
                answerRevealTime = currentWordTimings[questionEndIdx].endTime;
            }

            boolean showAnswer = currentTime >= answerRevealTime;

            java.util.List<String> questionLines = allQuestionLines.get(i);
            java.util.List<String> answerLines = allAnswerLines.get(i);

            // Calculate question block bounds for decoration
            int questionStartY = currentY;
            int questionHeight = questionLines.size() * (int)(qfm.getHeight() * 1.1);

            // Draw decorative frame around question
            drawDecorativeQuestionFrame(g2d, width, questionStartY, questionHeight, currentTime, i);

            // Draw question with enhanced effects
            g2d.setFont(questionFont);
            for (int lineIdx = 0; lineIdx < questionLines.size(); lineIdx++) {
                String qLine = questionLines.get(lineIdx);
                int lineWidth = qfm.stringWidth(qLine);
                // int centerX = (width - lineWidth) / 2;
                int centerX = width - sidePadding - lineWidth;  // RIGHT ALIGNED


                // Pulsing glow effect
                double pulsePhase = (currentTime * 2.0 + i * 0.5) % (Math.PI * 2);
                float glowIntensity = (float)((Math.sin(pulsePhase) + 1) / 2 * 0.3 + 0.7);

                // Multiple shadow layers for depth
                g2d.setColor(new Color(0, 0, 0, 200));
                for (int offset = 4; offset <= 6; offset++) {
                    g2d.drawString(qLine, centerX + offset, currentY + offset);
                }

                // Animated glow layers
                Color[] glowColors = {
                        new Color(255, 200, 0, (int)(200 * glowIntensity)),
                        new Color(255, 215, 0, (int)(160 * glowIntensity)),
                        new Color(255, 230, 0, (int)(120 * glowIntensity))
                };

                for (int layer = 0; layer < glowColors.length; layer++) {
                    g2d.setColor(glowColors[layer]);
                    int glowSize = 5 - layer;
                    for (int offset = 1; offset <= glowSize; offset++) {
                        g2d.drawString(qLine, centerX - offset, currentY);
                        g2d.drawString(qLine, centerX + offset, currentY);
                        g2d.drawString(qLine, centerX, currentY - offset);
                        g2d.drawString(qLine, centerX, currentY + offset);
                    }
                }

                // Main text with gradient
                GradientPaint textGradient = new GradientPaint(
                        centerX, currentY - qfm.getHeight(), new Color(94, 79, 2),
                        centerX, currentY, new Color(18, 15, 9)
                );
                g2d.setPaint(textGradient);
                g2d.drawString(qLine, centerX, currentY);

                currentY += (int)(qfm.getHeight() * 1.1);
            }

            currentY += qaSpacing;

            // Draw answer with reveal animation
            if (showAnswer && !answer.isEmpty()) {
                double timeSinceReveal = currentTime - answerRevealTime;

                // Slide-in and fade-in effect
                float fadeAlpha = (float)Math.min(1.0, timeSinceReveal / 0.5);
                int slideOffset = (int)Math.max(0, (1.0 - timeSinceReveal / 0.5) * 30);

                // Draw decorative answer indicator
                drawAnswerIndicator(g2d, width, currentY - 20, fadeAlpha, currentTime);

                g2d.setFont(answerFont);

                for (int lineIdx = 0; lineIdx < answerLines.size(); lineIdx++) {
                    String aLine = answerLines.get(lineIdx);
                    int lineWidth = afm.stringWidth(aLine);
                    //   int centerX = (width - lineWidth) / 2;
                    int centerX = width - sidePadding - lineWidth;  // RIGHT ALIGNED

                    // Shake effect when answer first appears
                    int[] shakeOffset = new int[]{0, 0};
                    if (timeSinceReveal < 1.0) {
                        shakeOffset = calculateShakeOffset(currentTime, (int)(5 * (1.0 - timeSinceReveal)));
                    }
                    int shakeX = centerX + shakeOffset[0];
                    int shakeY = currentY + shakeOffset[1] + slideOffset;

                    // Shadow with fade
                    g2d.setColor(new Color(0, 0, 0, (int)(180 * fadeAlpha)));
                    for (int offset = 3; offset <= 5; offset++) {
                        g2d.drawString(aLine, shakeX + offset, shakeY + offset);
                    }

                    // Animated green glow
                    double answerGlowPhase = (currentTime * 3.0) % (Math.PI * 2);
                    float answerGlowIntensity = (float)((Math.sin(answerGlowPhase) + 1) / 2 * 0.4 + 0.6);

                    g2d.setColor(new Color(100, 255, 100, (int)(150 * fadeAlpha * answerGlowIntensity)));
                    for (int offset = 1; offset <= 3; offset++) {
                        g2d.drawString(aLine, shakeX - offset, shakeY);
                        g2d.drawString(aLine, shakeX + offset, shakeY);
                        g2d.drawString(aLine, shakeX, shakeY - offset);
                        g2d.drawString(aLine, shakeX, shakeY + offset);
                    }

                    // Main text with gradient
                    GradientPaint answerGradient = new GradientPaint(
                            shakeX, shakeY - afm.getHeight(), new Color(255, 255, 255, (int)(255 * fadeAlpha)),
                            shakeX, shakeY, new Color(200, 255, 200, (int)(255 * fadeAlpha))
                    );
                    g2d.setPaint(answerGradient);
                    g2d.drawString(aLine, shakeX, shakeY);

                    // Sparkle effect around newly revealed answer
                    if (timeSinceReveal < 2.0) {
                        drawAnswerSparkles(g2d, shakeX, shakeY, lineWidth, afm.getHeight(),
                                currentTime, timeSinceReveal);
                    }

                    currentY += (int)(afm.getHeight() * 1.1);
                }
            } else if (!showAnswer) {
                currentY += answerLines.size() * (int)(afm.getHeight() * 1.1);
            }

            if (i < formattedData.lines.size() - 1) {
                currentY += spaceBetweenPairs;

                // Draw decorative separator
                drawDecorativeSeparator(g2d, width, currentY - spaceBetweenPairs/2, currentTime, i);
            }
        }

        // Add screen border decoration
        drawScreenBorder(g2d, width, height, currentTime);
        drawCornerRibbon(g2d, width, height, currentTime);

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }

    // Add this new method to load background image for quiz mode
    private BufferedImage loadQuizBackgroundImage() {
        // CHECK FOR SPECIFIC IMAGE FIRST
        if (!config.specificBackgroundImage.isEmpty() && new File(config.specificBackgroundImage).exists()) {
            try {
                BufferedImage specificImage = ImageIO.read(new File(config.specificBackgroundImage));
                System.out.println("✓ Using specific background image for quiz mode: " + config.specificBackgroundImage);
                return specificImage;
            } catch (IOException e) {
                System.out.println("✗ Error reading specific background image for quiz mode");
            }
        }

        // Fallback: Load from imgchng folder
        File imgchngDir = new File(config.imageFolder1);
        if (!imgchngDir.exists() || !imgchngDir.isDirectory()) {
            System.out.println("✗ imgchng folder not found for quiz background!");
            return null;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
        File[] allFiles = imgchngDir.listFiles();

        if (allFiles == null) {
            System.out.println("✗ Could not read imgchng directory");
            return null;
        }

        java.util.List<File> imageFiles = new java.util.ArrayList<>();

        // Find all image files in imgchng folder
        for (File file : allFiles) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        imageFiles.add(file);
                        break;
                    }
                }
            }
        }

        if (imageFiles.isEmpty()) {
            System.out.println("✗ No image files found in imgchng folder for quiz background");
            return null;
        }

        // Check for preferred image name first
        File selectedImage = null;
        String preferredImageName = "quiz.jpg"; // Change this to your preferred quiz background

        // First, try to find the preferred image
        for (File imageFile : imageFiles) {
            if (imageFile.getName().equalsIgnoreCase(preferredImageName)) {
                selectedImage = imageFile;
                System.out.println("✓ Found preferred quiz background image: " + preferredImageName);
                break;
            }
        }

        // If preferred image not found, select randomly
        if (selectedImage == null) {
            java.util.Random random = new java.util.Random();
            selectedImage = imageFiles.get(random.nextInt(imageFiles.size()));
            System.out.println("✓ Preferred image not found, using random quiz background: " + selectedImage.getName());
        }

        try {
            BufferedImage image = ImageIO.read(selectedImage);
            System.out.println("✓ Quiz background image loaded: " + selectedImage.getName());
            return image;
        } catch (IOException e) {
            System.out.println("✗ Error reading quiz background image: " + e.getMessage());
            return null;
        }
    }



    // Animated particles in background
    private void drawAnimatedParticles(Graphics2D g2d, int width, int height, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int particleCount = 50;
        for (int i = 0; i < particleCount; i++) {
            double phase = currentTime * 0.5 + i * 0.1;
            double x = (Math.sin(phase + i * 0.5) * 0.4 + 0.5) * width;
            double y = ((currentTime * 0.2 + i * 0.02) % 1.0) * height;

            double opacity = (Math.sin(phase * 2) + 1) / 2 * 0.3;
            int size = 2 + (i % 3);

            g2d.setColor(new Color(200, 200, 255, (int)(opacity * 255)));
            g2d.fillOval((int)x, (int)y, size, size);
        }
    }

    // Subtle light rays
    private void drawSubtleLightRays(Graphics2D g2d, int width, int height, double currentTime) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));

        double rayPhase = currentTime * 0.1;
        int rayCount = 5;

        for (int i = 0; i < rayCount; i++) {
            double angle = (i * Math.PI * 2 / rayCount) + rayPhase;
            int centerX = width / 2;
            int centerY = height / 2;
            int endX = (int)(centerX + Math.cos(angle) * width * 1.5);
            int endY = (int)(centerY + Math.sin(angle) * height * 1.5);

            LinearGradientPaint rayGradient = new LinearGradientPaint(
                    centerX, centerY, endX, endY,
                    new float[]{0.0f, 0.8f, 1.0f},
                    new Color[]{
                            new Color(255, 255, 255, 30),
                            new Color(255, 255, 255, 10),
                            new Color(255, 255, 255, 0)
                    }
            );

            g2d.setPaint(rayGradient);
            g2d.setStroke(new BasicStroke(40, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(centerX, centerY, endX, endY);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // Decorative frame around questions
    private void drawDecorativeQuestionFrame(Graphics2D g2d, int width, int y, int height,
                                             double currentTime, int index) {
        int padding = 40;
        int frameWidth = width - (2 * padding);

        // Pulsing corner decorations
        double pulse = (Math.sin(currentTime * 2 + index) + 1) / 2;
        int cornerSize = (int)(15 + pulse * 5);

        g2d.setColor(new Color(255, 200, 0, 80));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Top corners
        g2d.drawLine(padding, y - 20, padding + cornerSize, y - 20);
        g2d.drawLine(padding, y - 20, padding, y - 20 + cornerSize);

        g2d.drawLine(width - padding, y - 20, width - padding - cornerSize, y - 20);
        g2d.drawLine(width - padding, y - 20, width - padding, y - 20 + cornerSize);
    }

    // Answer indicator decoration
    private void drawAnswerIndicator(Graphics2D g2d, int width, int y, float alpha, double currentTime) {
        int indicatorWidth = 60;
        int centerX = width / 2;

        // Animated arrow/chevron pointing to answer
        double bounce = Math.sin(currentTime * 4) * 3;

        g2d.setColor(new Color(100, 255, 100, (int)(alpha * 150)));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int arrowY = (int)(y + bounce);
        g2d.drawLine(centerX - indicatorWidth/2, arrowY, centerX, arrowY + 10);
        g2d.drawLine(centerX + indicatorWidth/2, arrowY, centerX, arrowY + 10);
    }

    // Sparkles around revealed answers
    private void drawAnswerSparkles(Graphics2D g2d, int x, int y, int width, int height,
                                    double currentTime, double timeSinceReveal) {
        int sparkleCount = 8;
        double sparklePhase = timeSinceReveal * 3;

        for (int i = 0; i < sparkleCount; i++) {
            double angle = (i * Math.PI * 2 / sparkleCount) + sparklePhase;
            double distance = 40 + Math.sin(currentTime * 4 + i) * 10;

            int sparkleX = (int)(x + width/2 + Math.cos(angle) * distance);
            int sparkleY = (int)(y - height/2 + Math.sin(angle) * distance);

            double opacity = Math.max(0, 1.0 - timeSinceReveal / 2.0);
            g2d.setColor(new Color(255, 255, 150, (int)(opacity * 200)));

            // Draw small star
            int size = 3;
            g2d.fillOval(sparkleX - size, sparkleY - size, size * 2, size * 2);
            g2d.drawLine(sparkleX - size - 2, sparkleY, sparkleX + size + 2, sparkleY);
            g2d.drawLine(sparkleX, sparkleY - size - 2, sparkleX, sparkleY + size + 2);
        }
    }

    // Decorative separator between Q&A pairs
    private void drawDecorativeSeparator(Graphics2D g2d, int width, int y, double currentTime, int index) {
        int lineWidth = 150;
        int centerX = width / 2;

        // Animated glow
        double glowPhase = (currentTime * 2 + index) % (Math.PI * 2);
        float glowAlpha = (float)((Math.sin(glowPhase) + 1) / 2 * 0.3 + 0.2);

        g2d.setColor(new Color(150, 150, 200, (int)(glowAlpha * 255)));
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX - lineWidth/2, y, centerX + lineWidth/2, y);

        // Small decorative circles at ends
        int circleSize = 4;
        g2d.fillOval(centerX - lineWidth/2 - circleSize, y - circleSize, circleSize * 2, circleSize * 2);
        g2d.fillOval(centerX + lineWidth/2 - circleSize, y - circleSize, circleSize * 2, circleSize * 2);
    }

    // Screen border decoration
    private void drawScreenBorder(Graphics2D g2d, int width, int height, double currentTime) {
        int borderThickness = 3;

        // Animated border glow
        double glowPhase = currentTime % (Math.PI * 2);
        float borderAlpha = (float)((Math.sin(glowPhase) + 1) / 2 * 0.3 + 0.5);

        g2d.setColor(new Color(100, 150, 255, (int)(borderAlpha * 255)));
        g2d.setStroke(new BasicStroke(borderThickness));
        g2d.drawRect(10, 10, width - 20, height - 20);

        // Corner accents
        int cornerLength = 30;
        g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Top-left
        g2d.drawLine(10, 10, 10 + cornerLength, 10);
        g2d.drawLine(10, 10, 10, 10 + cornerLength);

        // Top-right
        g2d.drawLine(width - 10, 10, width - 10 - cornerLength, 10);
        g2d.drawLine(width - 10, 10, width - 10, 10 + cornerLength);

        // Bottom-left
        g2d.drawLine(10, height - 10, 10 + cornerLength, height - 10);
        g2d.drawLine(10, height - 10, 10, height - 10 - cornerLength);

        // Bottom-right
        g2d.drawLine(width - 10, height - 10, width - 10 - cornerLength, height - 10);
        g2d.drawLine(width - 10, height - 10, width - 10, height - 10 - cornerLength);
    }
    private void generateImagePlusTextFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                            String outputPath, double currentTime,
                                            Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw black background
        BufferedImage waterBg = createWaterEffectBackground(width, height, currentTime);
        g2d.drawImage(waterBg, 0, 0, null);

        if (displayInfo.currentQuote >= formattedData.lines.size()) {
            g2d.dispose();
            ImageIO.write(image, "JPEG", new File(outputPath));
            return;
        }

        FormattedLineArabicSync currentQuoteLine = formattedData.lines.get(displayInfo.currentQuote);
        String fullArabicText = currentQuoteLine.arabicContent;
        String[] commaPhrases = fullArabicText.split("،|,");

        // Find which phrase should be displayed based on timing
        int phraseStartWordIdx = currentQuoteLine.wordStartIndex;
        String displayPhrase = null;

        if (currentWordTimings != null && displayInfo.isActive) {
            for (int phraseIdx = 0; phraseIdx < commaPhrases.length; phraseIdx++) {
                String phrase = commaPhrases[phraseIdx].trim();
                if (phrase.isEmpty()) continue;

                String[] phraseWords = phrase.split("\\s+");
                int phraseEndWordIdx = phraseStartWordIdx + phraseWords.length - 1;

                if (phraseStartWordIdx < currentWordTimings.length && phraseEndWordIdx < currentWordTimings.length) {
                    double phraseStartTime = currentWordTimings[phraseStartWordIdx].startTime;
                    double phraseEndTime = currentWordTimings[phraseEndWordIdx].endTime;

                    if (currentTime >= phraseStartTime && currentTime <= phraseEndTime) {
                        displayPhrase = phrase;
                        break;
                    }
                }

                phraseStartWordIdx += phraseWords.length;
            }
        }

        // Draw image in upper part (65% of screen)
        int imageHeight = (int)(height * 0.65);
        int imagePadding = 50;
        int imageWidth = width - (2 * imagePadding);
        int imageY = 240;

        // Load and draw the specific background image
        if (!config.specificBackgroundImage.isEmpty()) {
            try {
                BufferedImage bgImage = ImageIO.read(new File(config.specificBackgroundImage));
                if (bgImage != null) {
                    // Scale image to fit within bounds while maintaining aspect ratio
                    double imageAspect = (double)bgImage.getWidth() / bgImage.getHeight();
                    imageAspect = imageAspect * 1.1; // 30% wider appearance

                    int scaledWidth = imageWidth;
                    int scaledHeight = (int)(scaledWidth / imageAspect);

                    if (scaledHeight > imageHeight) {
                        scaledHeight = imageHeight;
                        scaledWidth = (int)(scaledHeight * imageAspect);
                    }

                    int imageX = imagePadding + (imageWidth - scaledWidth) / 2;
                    int finalImageY = imageY + (imageHeight - scaledHeight) / 2;

// Create rounded corner image
                    int cornerRadius = 40; // Adjust this value for more/less rounding
                    BufferedImage roundedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2dRounded = roundedImage.createGraphics();
                    g2dRounded.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

// Create rounded rectangle clip
                    g2dRounded.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, scaledWidth, scaledHeight, cornerRadius, cornerRadius));
                    g2dRounded.drawImage(bgImage, 0, 0, scaledWidth, scaledHeight, null);
                    g2dRounded.dispose();

// Draw the rounded image
                    g2d.drawImage(roundedImage, imageX, finalImageY, null);

// Draw rounded border
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.draw(new java.awt.geom.RoundRectangle2D.Float(imageX - 2, finalImageY - 2, scaledWidth + 4, scaledHeight + 4, cornerRadius, cornerRadius));
                }
            } catch (Exception e) {
                System.out.println("Error loading image: " + e.getMessage());
            }
        }

        // Draw text in lower part (35% of screen)
        if (displayPhrase != null && !displayPhrase.isEmpty()) {
            Font smallFont = arabicFont.deriveFont(Font.BOLD, 55f);
            FontMetrics fm = g2d.getFontMetrics(smallFont);

            int textAreaY = imageY + imageHeight-10;
            int textAreaHeight = height - textAreaY + 20;
            int textPadding = 140;
            int textWidth = width - (2 * textPadding);

            java.util.List<String> wrappedLines = wrapTextToLines(displayPhrase, fm, textWidth);

            int totalTextHeight = wrappedLines.size() * (int)(fm.getHeight() * 1.0);
            int textStartY = textAreaY + 30;

            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                String line = wrappedLines.get(lineIdx);
                int lineWidth = fm.stringWidth(line);
                int centerX = (width - lineWidth) / 2;
                int lineY = textStartY + (int)(lineIdx * fm.getHeight() * 0.8) + fm.getAscent();

                int[] shakeOffset = calculateShakeOffset(currentTime, 3);
                int shakeX = centerX + shakeOffset[0];
                int shakeY = lineY + shakeOffset[1];

                g2d.setFont(smallFont);

                // Shadow
                g2d.setColor(new Color(0, 0, 0, 200));
                for (int offset = 2; offset <= 4; offset++) {
                    g2d.drawString(line, shakeX - offset, shakeY + offset);
                    g2d.drawString(line, shakeX + offset, shakeY + offset);
                }

                // Glow
                Color[] glowColors = {
                        new Color(255, 140, 0, 180),
                        new Color(255, 165, 0, 150),
                        new Color(32, 30, 31, 120)
                };

                for (int layer = 0; layer < glowColors.length; layer++) {
                    g2d.setColor(glowColors[layer]);
                    int glowSize = 5 - layer;
                    for (int offset = 1; offset <= glowSize; offset++) {
                        g2d.drawString(line, shakeX - offset, shakeY);
                        g2d.drawString(line, shakeX + offset, shakeY);
                        g2d.drawString(line, shakeX, shakeY - offset);
                        g2d.drawString(line, shakeX, shakeY + offset);
                    }
                }

                // Main text
                g2d.setColor(Color.black);
                g2d.drawString(line, shakeX, shakeY);
            }
        }

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }

    // Add corner ribbon decoration
    private void drawCornerRibbon(Graphics2D g2d, int width, int height, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Ribbon properties
        int ribbonWidth = 200;
        int ribbonHeight = 200;

        // Choose corner: 0=top-right, 1=top-left, 2=bottom-right, 3=bottom-left
        int corner = 0; // Top-right corner

        // Pulsing glow effect
        double pulsePhase = (currentTime * 2.0) % (Math.PI * 2);
        float glowIntensity = (float)((Math.sin(pulsePhase) + 1) / 2 * 0.3 + 0.7);

        if (corner == 0) {
            // TOP-RIGHT CORNER RIBBON

            // Create ribbon polygon (triangle fold effect)
            int[] xPoints = {width, width, width - ribbonWidth};
            int[] yPoints = {0, ribbonHeight, 0};

            // Draw ribbon shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillPolygon(
                    new int[]{width + 5, width + 5, width - ribbonWidth + 5},
                    new int[]{5, ribbonHeight + 5, 5},
                    3
            );

            // Draw main ribbon with gradient
            GradientPaint ribbonGradient = new GradientPaint(
                    width - ribbonWidth/2, 0, new Color(255, 200, 0, (int)(220 * glowIntensity)),
                    width, ribbonHeight/2, new Color(200, 150, 0, (int)(255 * glowIntensity))
            );
            g2d.setPaint(ribbonGradient);
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Draw ribbon border
            g2d.setColor(new Color(150, 100, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(xPoints, yPoints, 3);

            // Draw fold shadow (creates 3D effect)
            int[] foldX = {width, width - ribbonWidth, width - ribbonWidth + 30};
            int[] foldY = {0, 0, 30};
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillPolygon(foldX, foldY, 3);

            // Optional: Add text on ribbon
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.rotate(Math.toRadians(45), width - 60, 60);
            g2d.drawString("معلومة", width - 85, 65);
            g2d.rotate(Math.toRadians(-45), width - 60, 60); // Reset rotation

        } else if (corner == 1) {
            // TOP-LEFT CORNER RIBBON

            int[] xPoints = {0, ribbonWidth, 0};
            int[] yPoints = {0, 0, ribbonHeight};

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillPolygon(
                    new int[]{-5, ribbonWidth - 5, -5},
                    new int[]{5, 5, ribbonHeight + 5},
                    3
            );

            // Main ribbon
            GradientPaint ribbonGradient = new GradientPaint(
                    ribbonWidth/2, 0, new Color(255, 200, 0, (int)(220 * glowIntensity)),
                    0, ribbonHeight/2, new Color(200, 150, 0, (int)(255 * glowIntensity))
            );
            g2d.setPaint(ribbonGradient);
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Border
            g2d.setColor(new Color(150, 100, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(xPoints, yPoints, 3);

            // Fold shadow
            int[] foldX = {0, ribbonWidth, ribbonWidth - 30};
            int[] foldY = {0, 0, 30};
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillPolygon(foldX, foldY, 3);

            // Text on ribbon
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.rotate(Math.toRadians(-45), 60, 60);
            g2d.drawString("QUIZ", 40, 65);
            g2d.rotate(Math.toRadians(45), 60, 60);
        }

        // Add shine effect
        if (corner == 0 || corner == 1) {
            double shinePhase = (currentTime * 1.5) % 2.0;
            if (shinePhase < 1.0) {
                int shineAlpha = (int)(shinePhase * 100);
                g2d.setColor(new Color(255, 255, 255, shineAlpha));
                g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                if (corner == 0) {
                    g2d.drawLine(width - 50, 10, width - 10, 50);
                } else {
                    g2d.drawLine(10, 50, 50, 10);
                }
            }
        }
    }

    //.........falling objects..........



    private BufferedImage createWaterEffectBackground(int width, int height, double currentTime) {
        BufferedImage bgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bgImage.createGraphics();

        // --- Background: deep gradient tone (dark blue night sky) ---
        GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(5, 10, 25),
                0, height, new Color(15, 35, 70)
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, width, height);

        // --- Smooth rendering ---
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Parameters for evenly distributed falling particles ---
        int particleCount = 200;   // total number of falling objects
        double fallSpeed = 35.0;  // falling speed (lower = slower)
        double swayAmplitude = 25.0; // horizontal sway range
        double swaySpeed = 1.8;   // how fast they sway left/right

        // --- Draw each independent falling particle ---
        for (int i = 0; i < particleCount; i++) {
            // Use a pseudo-random seed to give each particle unique movement
            double seed = i * 999.12345;

            // Evenly distribute horizontally
            double baseX = (i * (width / (double) particleCount)) + (Math.sin(seed) * 20);

            // Continuous fall over time with unique offsets
            double baseY = ((currentTime * fallSpeed) + (i * 137)) % height;

            // Apply gentle individual sway (independent per particle)
            double sway = Math.sin(currentTime * swaySpeed + seed) * swayAmplitude * Math.cos(seed * 0.3);

            double x = baseX + sway;
            double y = baseY;

            // Random variation in particle size and transparency
            double size = 2.5 + 4.0 * (0.5 + 0.5 * Math.sin(seed * 1.7));
            float alpha = (float) (0.4 + 0.6 * Math.abs(Math.sin(seed * 0.9 + currentTime)));

            // Light bluish-white tone
            Color color = new Color(
                    200 + (int)(40 * Math.abs(Math.sin(seed))),
                    220 + (int)(30 * Math.abs(Math.cos(seed * 1.3))),
                    255,
                    (int)(alpha * 255)
            );

            // --- Draw particle as soft glowing circle ---
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Double(x, y),
                    (float) size,
                    new float[]{0f, 1f},
                    new Color[]{
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                    }
            );

            g2d.setPaint(glow);
            g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
        }

        g2d.dispose();
        return bgImage;
    }














    private void generateArabicSyncSlideshowFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                                  String outputPath, double currentTime,
                                                  Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Load random image every second
        BufferedImage slideshowImage = loadRandomSlideshowImage(currentTime);
        if (slideshowImage != null) {
            slideshowImage = scaleImageToVideoDimensions(slideshowImage, width, height);
            g2d.drawImage(slideshowImage, 0, 0, null);
        } else {
            // Fallback black background
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, width, height);
        }

        // Draw text with Arabic audio sync
        drawArabicAudioSyncText(g2d, formattedData, displayInfo, width, height, englishFont, arabicFont, currentTime);

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }

    private BufferedImage loadRandomSlideshowImage(double currentTime) {
        try {
            // Change image every 3 seconds - REGULAR intervals
            int imageIndex = (int) (currentTime / 1.0);

            File imgchng2Dir = new File(config.imageFolder2);
            if (!imgchng2Dir.exists() || !imgchng2Dir.isDirectory()) {
                System.out.println("✗ imgchng2 folder not found for slideshow!");
                return null;
            }

            String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
            File[] allFiles = imgchng2Dir.listFiles();

            if (allFiles == null) {
                System.out.println("✗ Could not read imgchng2 directory");
                return null;
            }

            java.util.List<File> imageFiles = new java.util.ArrayList<>();
            for (File file : allFiles) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            imageFiles.add(file);
                            break;
                        }
                    }
                }
            }

            if (imageFiles.isEmpty()) {
                System.out.println("✗ No image files found in imgchng2 folder");
                return null;
            }

            // Use current timestamp as additional seed so each rerun gets different images
            long rerunSeed = System.currentTimeMillis() / 100000; // Changes every ~27 hours
            java.util.Random random = new java.util.Random((imageIndex * 987654321L) + rerunSeed);
            File selectedImage = imageFiles.get(random.nextInt(imageFiles.size()));

            return ImageIO.read(selectedImage);

        } catch (Exception e) {
            System.out.println("✗ Error loading slideshow image: " + e.getMessage());
            return null;
        }
    }

    private int calculateOptimalPuzzleSize(double duration) {
//        if (duration < 10) return 6;      // 16 pieces for short videos
//        else if (duration < 30) return 8; // 25 pieces for medium videos
//        else if (duration < 60) return 10; // 36 pieces for longer videos
//        else return 20;                    // 49 pieces for very long videos
        return 10;  // 225 pieces (15×15 grid) - very challenging!

    }

    private JigsawPiece[] generateJigsawPieces(BufferedImage backgroundImage, int puzzleGridSize) {
        int imgWidth = backgroundImage.getWidth();
        int imgHeight = backgroundImage.getHeight();
        int pieceWidth = imgWidth / puzzleGridSize;
        int pieceHeight = imgHeight / puzzleGridSize;

        java.util.List<JigsawPiece> pieces = new java.util.ArrayList<>();

        System.out.println("🧩 Creating " + (puzzleGridSize * puzzleGridSize) + " jigsaw pieces...");

        // Create puzzle pieces
        for (int row = 0; row < puzzleGridSize; row++) {
            for (int col = 0; col < puzzleGridSize; col++) {
                int x = col * pieceWidth;
                int y = row * pieceHeight;

                // Extract piece image
                BufferedImage pieceImg = new BufferedImage(pieceWidth, pieceHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = pieceImg.createGraphics();
                g2d.drawImage(backgroundImage, 0, 0, pieceWidth, pieceHeight,
                        x, y, x + pieceWidth, y + pieceHeight, null);
                g2d.dispose();

                JigsawPiece piece = new JigsawPiece(x, y, pieceWidth, pieceHeight, pieceImg);
                pieces.add(piece);
            }
        }

        // Scramble pieces positions
        java.util.Random random = new java.util.Random(42); // Fixed seed for consistency
        for (JigsawPiece piece : pieces) {
            // Random scrambled position within image bounds
            piece.currentX = random.nextInt(Math.max(1, imgWidth - piece.width));
            piece.currentY = random.nextInt(Math.max(1, imgHeight - piece.height));
        }

        System.out.println("✓ Jigsaw pieces created and scrambled");
        return pieces.toArray(new JigsawPiece[0]);
    }

    /**
     * Load single random image for effects (zoom, flip, grayscale)
     */
    private BufferedImage loadSingleRandomImageForEffects() {

        // CHECK FOR SPECIFIC IMAGE FIRST
        if (!config.specificBackgroundImage.isEmpty() && new File(config.specificBackgroundImage).exists()) {
            try {
                BufferedImage specificImage = ImageIO.read(new File(config.specificBackgroundImage));
                System.out.println("✓ Using specific background image for single effects: " + config.specificBackgroundImage);
                return specificImage;
            } catch (IOException e) {
                System.out.println("✗ Error reading specific background image, falling back to folder selection");
            }
        }
        // Load from imgchng folder
        //File imgchngDir = new File("imgchng");


        File imgchngDir = new File(config.imageFolder1);  // USER INPUT - CORRECT!


        if (!imgchngDir.exists() || !imgchngDir.isDirectory()) {
            System.out.println("✗ imgchng folder not found for single image effects!");
            return null;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
        File[] allFiles = imgchngDir.listFiles();

        if (allFiles == null) {
            System.out.println("✗ Could not read imgchng directory");
            return null;
        }

        java.util.List<File> imageFiles = new java.util.ArrayList<>();

        // Find all image files in imgchng folder
        for (File file : allFiles) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        imageFiles.add(file);
                        break;
                    }
                }
            }
        }

        if (imageFiles.isEmpty()) {
            System.out.println("✗ No image files found in imgchng folder for single image effects");
            return null;
        }

        // Select a RANDOM image from imgchng folder
        // Check for specific image name first, otherwise use random
        File selectedImage = null;
        String preferredImageName = "this.jpg"; // Change this to your desired image name

// First, try to find the preferred image
        for (File imageFile : imageFiles) {
            if (imageFile.getName().equalsIgnoreCase(preferredImageName)) {
                selectedImage = imageFile;
                System.out.println("✓ Found preferred single effects image: " + preferredImageName);
                break;
            }
        }

// If preferred image not found, select randomly
        if (selectedImage == null) {
            java.util.Random random = new java.util.Random();
            selectedImage = imageFiles.get(random.nextInt(imageFiles.size()));
            System.out.println("✓ Preferred image not found, using random single effects image: " + selectedImage.getName());
        }

        try {
            BufferedImage image = ImageIO.read(selectedImage);
            System.out.println("✓ Random single image selected for effects: " + selectedImage.getName());
            System.out.println("  (Selected from " + imageFiles.size() + " available images)");
            return image;
        } catch (IOException e) {
            System.out.println("✗ Error reading random single image for effects: " + e.getMessage());
            return null;
        }
    }

    private void updateJigsawPieces(JigsawPiece[] pieces, double completionPercentage) {
        for (JigsawPiece piece : pieces) {
            if (!piece.isInPlace) {
                // Calculate current position based on completion
                double progress = Math.min(1.0, completionPercentage * 6.0); // 4x faster

                // Smooth easing function
                double easedProgress = easeInOutCubic(progress);

                // Interpolate between current scrambled position and target position
                piece.currentX = (int) (piece.currentX + (piece.targetX - piece.currentX) * easedProgress);
                piece.currentY = (int) (piece.currentY + (piece.targetY - piece.currentY) * easedProgress);

                // Check if piece is close enough to target (within 5 pixels)
                if (Math.abs(piece.currentX - piece.targetX) < 5 &&
                        Math.abs(piece.currentY - piece.targetY) < 5) {
                    piece.currentX = piece.targetX;
                    piece.currentY = piece.targetY;
                    piece.isInPlace = true;
                }
            }
        }
    }

    private double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    }

    private BufferedImage loadJigsawBackgroundImage() {

        // CHECK FOR SPECIFIC IMAGE FIRST
        if (!config.specificBackgroundImage.isEmpty() && new File(config.specificBackgroundImage).exists()) {
            try {
                BufferedImage specificImage = ImageIO.read(new File(config.specificBackgroundImage));
                System.out.println("✓ Using specific background image for jigsaw: " + config.specificBackgroundImage);
                return specificImage;
            } catch (IOException e) {
                System.out.println("✗ Error reading specific background image, falling back to folder selection");
            }
        }
        // Load from imgchng folder instead of current directory
        File imgchngDir = new File(config.imageFolder1);
        if (!imgchngDir.exists() || !imgchngDir.isDirectory()) {
            System.out.println("✗ imgchng folder not found for jigsaw background!");
            return null;
        }

        String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
        File[] allFiles = imgchngDir.listFiles();

        if (allFiles == null) {
            System.out.println("✗ Could not read imgchng directory");
            return null;
        }

        java.util.List<File> imageFiles = new java.util.ArrayList<>();

        // Find all image files in imgchng folder
        for (File file : allFiles) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        imageFiles.add(file);
                        break;
                    }
                }
            }
        }

        if (imageFiles.isEmpty()) {
            System.out.println("✗ No image files found in imgchng folder for jigsaw background");
            return null;
        }

        // Select a RANDOM image from imgchng folder
        // Check for specific image name first, otherwise use random
        File selectedImage = null;
        String preferredImageName = "this.jpg"; // Change this to your desired image name

// First, try to find the preferred image
        for (File imageFile : imageFiles) {
            if (imageFile.getName().equalsIgnoreCase(preferredImageName)) {
                selectedImage = imageFile;
                System.out.println("✓ Found preferred jigsaw image: " + preferredImageName);
                break;
            }
        }

// If preferred image not found, select randomly
        if (selectedImage == null) {
            java.util.Random random = new java.util.Random();
            selectedImage = imageFiles.get(random.nextInt(imageFiles.size()));
            System.out.println("✓ Preferred image not found, using random jigsaw image: " + selectedImage.getName());
        }

        try {
            BufferedImage image = ImageIO.read(selectedImage);
            System.out.println("✓ Random jigsaw background image loaded from imgchng: " + selectedImage.getName());
            System.out.println("  (Selected from " + imageFiles.size() + " available images)");
            return image;
        } catch (IOException e) {
            System.out.println("✗ Error reading random jigsaw background image from imgchng: " + e.getMessage());
            return null;
        }
    }


    private Font loadArabicFont() {
        try {
            File fontFile = new File(config.fontPath2); // Changed this line
            if (fontFile.exists()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);

                Font derivedFont = font.deriveFont(Font.BOLD, 45f);
                System.out.println("✓ Successfully loaded Arabic font: " + font.getName());
                return derivedFont;
            } else {
                System.out.println("✗ Arabic font not found, using fallback");
                return new Font("amiriquran-regular", Font.PLAIN, 64);
            }
        } catch (Exception e) {
            System.out.println("✗ Error loading Arabic font: " + e.getMessage());
            return new Font("amiriquran-regular", Font.PLAIN, 64);
        }
    }

    /**
     * Load custom font
     */
    private Font loadCustomFont() {
        try {
            File fontFile = new File(config.fontPath1); // Changed this line
            if (fontFile.exists()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);

                Font derivedFont = font.deriveFont(Font.BOLD, 55f);
                System.out.println("✓ Successfully loaded custom font: " + font.getName());
                return derivedFont;
            } else {
                System.out.println("✗ Custom font not found, using fallback");
                return new Font("Arial Unicode MS", Font.PLAIN, 55);
            }
        } catch (Exception e) {
            System.out.println("✗ Error loading font: " + e.getMessage());
            return new Font("SansSerif", Font.PLAIN, 55);
        }
    }

    /**
     * Timing information for Arabic audio sync quotes
     */
    private static class QuoteTimingInfoArabicSync {
        int quoteIndex;
        double startTime;
        double endTime;

        QuoteTimingInfoArabicSync(int index, double start, double end) {
            this.quoteIndex = index;
            this.startTime = start;
            this.endTime = end;
        }
    }

    /**
     * Display info for Arabic audio sync
     */
    private static class QuoteDisplayInfoArabicSync {
        int currentQuote;
        boolean isActive; // Is this quote currently being spoken

        QuoteDisplayInfoArabicSync(int quote, boolean active) {
            this.currentQuote = quote;
            this.isActive = active;
        }
    }

    /**
     * Calculate Arabic quote timings
     */
















    /**
     * Calculate gap-free line timings using REAL audio word timings from ElevenLabs.
     * Uses proportional distribution based on actual ElevenLabs word count to ensure accurate sync.
     */
    private QuoteTimingInfoArabicSync[] calculateArabicQuoteTimings(FormattedTextDataArabicSync formattedData, WordTiming[] wordTimings, double audioDuration) {
        QuoteTimingInfoArabicSync[] quoteTimings = new QuoteTimingInfoArabicSync[formattedData.lines.size()];
        int numLines = formattedData.lines.size();
        int elevenLabsWordCount = (wordTimings != null) ? wordTimings.length : 0;

        System.out.println("📊 Calculating line timings from REAL audio:");
        System.out.println("   Total lines: " + numLines);
        System.out.println("   ElevenLabs word timings: " + elevenLabsWordCount);
        System.out.println("   Audio duration: " + String.format("%.2f", audioDuration) + "s");

        if (wordTimings == null || wordTimings.length == 0 || numLines == 0) {
            // Fallback: distribute audio evenly across lines
            double timePerLine = audioDuration / numLines;
            for (int i = 0; i < numLines; i++) {
                double startTime = i * timePerLine;
                double endTime = (i == numLines - 1) ? audioDuration : (i + 1) * timePerLine;
                quoteTimings[i] = new QuoteTimingInfoArabicSync(i, startTime, endTime);
                System.out.println("🎯 Line " + (i + 1) + ": " + String.format("%.2f", startTime) + "s → " +
                        String.format("%.2f", endTime) + "s (even distribution)");
            }
            return quoteTimings;
        }

        // Calculate total original text words for proportional distribution
        int totalOriginalWords = 0;
        for (FormattedLineArabicSync line : formattedData.lines) {
            totalOriginalWords += line.wordCount;
        }

        // Distribute ElevenLabs words proportionally across lines
        // Each line gets a share of ElevenLabs words based on its original word count
        int elevenLabsWordIndex = 0;
        double currentTime = 0;

        for (int i = 0; i < numLines; i++) {
            FormattedLineArabicSync line = formattedData.lines.get(i);

            // Calculate how many ElevenLabs words this line should get
            double proportion = (double) line.wordCount / totalOriginalWords;
            int wordsForThisLine = (int) Math.round(proportion * elevenLabsWordCount);

            // Ensure at least 1 word per line, and don't exceed remaining words
            wordsForThisLine = Math.max(1, wordsForThisLine);
            int remainingLines = numLines - i - 1;
            int remainingWords = elevenLabsWordCount - elevenLabsWordIndex;
            if (remainingLines > 0) {
                wordsForThisLine = Math.min(wordsForThisLine, remainingWords - remainingLines);
            }

            // For the last line, take all remaining words
            if (i == numLines - 1) {
                wordsForThisLine = elevenLabsWordCount - elevenLabsWordIndex;
            }

            // Get timing from actual ElevenLabs words
            double startTime = 0;
            double endTime = audioDuration;

            if (elevenLabsWordIndex < elevenLabsWordCount) {
                startTime = wordTimings[elevenLabsWordIndex].startTime;
            }

            int lastWordIdx = Math.min(elevenLabsWordIndex + wordsForThisLine - 1, elevenLabsWordCount - 1);
            if (lastWordIdx >= 0 && lastWordIdx < elevenLabsWordCount) {
                endTime = wordTimings[lastWordIdx].endTime;
            }

            // First line always starts at 0
            if (i == 0) {
                startTime = 0;
            }

            // Ensure gap-free: this line starts where previous ended
            if (i > 0 && quoteTimings[i - 1] != null) {
                startTime = quoteTimings[i - 1].endTime;
            }

            // Last line always ends at audio duration
            if (i == numLines - 1) {
                endTime = audioDuration;
            }

            // CRITICAL: Ensure end time never exceeds audio duration
            endTime = Math.min(endTime, audioDuration);

            // Ensure end time is after start time
            if (endTime <= startTime) {
                endTime = Math.min(startTime + 0.5, audioDuration);
            }

            quoteTimings[i] = new QuoteTimingInfoArabicSync(i, startTime, endTime);

            System.out.println("🎯 Line " + (i + 1) + ": " + String.format("%.2f", startTime) + "s → " +
                    String.format("%.2f", endTime) + "s (ElevenLabs words " + elevenLabsWordIndex + "-" + lastWordIdx + ")");

            elevenLabsWordIndex += wordsForThisLine;
        }

        // Final validation
        quoteTimings = validateLineTiming(quoteTimings, audioDuration);

        return quoteTimings;
    }

    /**
     * Validate and fix line timings to ensure 100% coverage of audio duration
     */
    private QuoteTimingInfoArabicSync[] validateLineTiming(QuoteTimingInfoArabicSync[] timings, double audioDuration) {
        if (timings == null || timings.length == 0) return timings;

        // Ensure first line starts at 0
        if (timings[0].startTime > 0) {
            timings[0] = new QuoteTimingInfoArabicSync(0, 0, timings[0].endTime);
        }

        // Fix any gaps between lines
        for (int i = 1; i < timings.length; i++) {
            double prevEnd = timings[i - 1].endTime;
            double currStart = timings[i].startTime;

            if (Math.abs(currStart - prevEnd) > 0.001) {
                // There's a gap or overlap - fix by using midpoint
                double boundary = prevEnd; // Use previous end as boundary
                timings[i] = new QuoteTimingInfoArabicSync(i, boundary, timings[i].endTime);
            }
        }

        // Ensure last line ends at audio duration
        int lastIdx = timings.length - 1;
        if (Math.abs(timings[lastIdx].endTime - audioDuration) > 0.001) {
            timings[lastIdx] = new QuoteTimingInfoArabicSync(lastIdx, timings[lastIdx].startTime, audioDuration);
        }

        System.out.println("✅ Line timings validated - 100% audio coverage guaranteed");
        return timings;
    }
    /**
     * Get current Arabic quote display info based on current time.
     * With gap-free timing, this should always find an exact match.
     */
    private QuoteDisplayInfoArabicSync getCurrentArabicQuoteDisplayInfo(double currentTime, QuoteTimingInfoArabicSync[] quoteTimings) {
        if (quoteTimings == null || quoteTimings.length == 0) {
            return new QuoteDisplayInfoArabicSync(0, false);
        }

        // With gap-free timing, use inclusive comparison for accurate detection
        for (int i = 0; i < quoteTimings.length; i++) {
            QuoteTimingInfoArabicSync timing = quoteTimings[i];

            // Use inclusive start, exclusive end (except for last line)
            boolean isLastLine = (i == quoteTimings.length - 1);
            boolean inRange = isLastLine
                    ? (currentTime >= timing.startTime && currentTime <= timing.endTime)
                    : (currentTime >= timing.startTime && currentTime < timing.endTime);

            if (inRange) {
                return new QuoteDisplayInfoArabicSync(i, true);
            }
        }

        // Handle edge cases (should rarely happen with validated timings)
        if (currentTime < quoteTimings[0].startTime) {
            return new QuoteDisplayInfoArabicSync(0, false);
        }

        // Default to last quote if time exceeds all timings
        return new QuoteDisplayInfoArabicSync(quoteTimings.length - 1, true);
    }

    /**
     * Generate frame with jigsaw background and Arabic audio sync
     */
    private void generateArabicSyncJigsawFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                               String outputPath, JigsawPiece[] jigsawPieces, double currentTime,
                                               Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw jigsaw background with zoom
        double zoomFactor = calculateZoomFactor(jigsawPieces, currentTime, totalDuration);
        drawJigsawBackgroundWithZoom(g2d, jigsawPieces, width, height, zoomFactor);

        // Draw text with Arabic audio sync
        drawArabicAudioSyncText(g2d, formattedData, displayInfo, width, height, englishFont, arabicFont, currentTime);

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }

    /**
     * Data structure for jigsaw puzzle pieces
     */
    private static class JigsawPiece {
        int originalX, originalY;
        int currentX, currentY;
        int targetX, targetY;
        int width, height;
        BufferedImage pieceImage;
        boolean isInPlace;

        JigsawPiece(int origX, int origY, int w, int h, BufferedImage img) {
            this.originalX = origX;
            this.originalY = origY;
            this.targetX = origX;
            this.targetY = origY;
            this.width = w;
            this.height = h;
            this.pieceImage = img;
            this.isInPlace = false;
        }
    }

    /**
     * Generate frame with single image effects and Arabic audio sync
     */
    private void drawJigsawBackground(Graphics2D g2d, JigsawPiece[] pieces, int width, int height) {
        // First, fill with black background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // Draw each puzzle piece at its current position
        for (JigsawPiece piece : pieces) {
            if (piece.pieceImage != null) {
                g2d.drawImage(piece.pieceImage, piece.currentX, piece.currentY, null);

                // Optional: Add subtle border around pieces that aren't in place
                if (!piece.isInPlace) {
                    g2d.setColor(new Color(255, 255, 255, 50));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRect(piece.currentX, piece.currentY, piece.width, piece.height);
                }
            }
        }
    }

    private void drawJigsawBackgroundWithZoom(Graphics2D g2d, JigsawPiece[] pieces, int width, int height, double zoomFactor) {
        // Fill with black background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        if (zoomFactor == 1.0) {
            // No zoom - use original method
            drawJigsawBackground(g2d, pieces, width, height);
            return;
        }

        // Apply zoom transformation
        Graphics2D zoomedG2d = (Graphics2D) g2d.create();

        // Calculate zoom center (center of the image)
        int centerX = width / 2;
        int centerY = height / 2;

        // Apply zoom transformation
        zoomedG2d.translate(centerX, centerY);
        zoomedG2d.scale(zoomFactor, zoomFactor);
        zoomedG2d.translate(-centerX, -centerY);

        // Draw each puzzle piece with zoom
        for (JigsawPiece piece : pieces) {
            if (piece.pieceImage != null) {
                zoomedG2d.drawImage(piece.pieceImage, piece.currentX, piece.currentY, null);

                // Optional: Add subtle border around pieces that aren't in place
                if (!piece.isInPlace) {
                    zoomedG2d.setColor(new Color(255, 255, 255, 50));
                    zoomedG2d.setStroke(new BasicStroke(1));
                    zoomedG2d.drawRect(piece.currentX, piece.currentY, piece.width, piece.height);
                }
            }
        }

        zoomedG2d.dispose();

        // Add zoom indicator text (optional)
        if (zoomFactor != 1.0) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            // String zoomText = String.format("Zoom: %.1fx", zoomFactor);
            //  g2d.drawString(zoomText, 20, height - 30);
        }
    }

    private double calculateSingleImageZoomFactor(double timeProgress) {
        // Create multiple zoom cycles throughout the video
        double cycleTime = timeProgress * 4 * Math.PI; // 2 complete cycles
        double zoomVariation = (Math.sin(cycleTime) + 1) / 2; // Range 0 to 1
        return 1.0 + (0.5 * zoomVariation); // Range: 1.0x to 3.0x
    }

    private double calculateFlipAngle(double timeProgress) {
        // Slower flip - every 10 seconds instead of 3
        double flipCycleTime = timeProgress * 2 * Math.PI; // Reduced from 6 * Math.PI
        return Math.sin(flipCycleTime); // Range -1 to 1 (for horizontal flip scaling)
    }

    private double calculateCrackEffect(double timeProgress) {
        // Slow crack/tear cycle - every 10 seconds
        double crackCycleTime = timeProgress * 2 * Math.PI;
        return Math.sin(crackCycleTime); // Range -1 to 1
    }

    private double calculateGrayscaleFactor(double timeProgress) {
        // Transition between color and grayscale
        double grayscaleCycleTime = timeProgress * 3 * Math.PI; // Slower grayscale cycles
        double grayscaleVariation = (Math.sin(grayscaleCycleTime) + 1) / 2; // Range 0 to 1
        return grayscaleVariation; // 0.0 = full color, 1.0 = full grayscale
    }

    private double calculateZoomFactor(JigsawPiece[] pieces, double currentTime, double totalDuration) {
        // Count how many pieces are actually in their exact positions
        int piecesInPlace = 0;
        for (JigsawPiece piece : pieces) {
            if (piece.isInPlace && piece.currentX == piece.targetX && piece.currentY == piece.targetY) {
                piecesInPlace++;
            }
        }

        // Check if puzzle is 100% complete
        boolean puzzleComplete = (piecesInPlace == pieces.length);

        if (!puzzleComplete) {
            // Puzzle not completely solved yet - no zoom
            return 1.0;
        }

        // Puzzle is 100% complete - start zoom for remaining video time
        double currentProgress = currentTime / totalDuration;
        double remainingTime = totalDuration - currentTime;
        double totalZoomTime = remainingTime;

        if (totalZoomTime <= 0) {
            return 1.0; // No time left for zoom
        }

        double zoomProgress = 1.0 - (remainingTime / (totalDuration * 0.5));
        zoomProgress = Math.max(0, Math.min(zoomProgress, 1.0));

        double cycleTime = zoomProgress * 2 * Math.PI; // One complete cycle

        // ZOOM RANGE: 1.0x (normal) to 3.0x (3x zoom in) and back to 1.0x
        double zoomVariation = 1.0 * (Math.sin(cycleTime - Math.PI / 2) + 1) / 2; // Range 0 to 1
        double baseZoom = 1.0; // Start at normal

        return baseZoom + (2.0 * zoomVariation); // Range: 1.0x to 3.0x
    }


    private void generateArabicSyncSingleImageFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                                    String outputPath, BufferedImage singleEffectImage, double currentTime,
                                                    Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Calculate proper time progress based on actual video position
        double timeProgress = currentTime / totalDuration;

        // Calculate and apply image effects
        // Calculate conditionally based on config
        double zoomFactor = config.enableZoom ? calculateSingleImageZoomFactor(timeProgress) : 1.0;
        //double flipAngle = config.enableFlip ? calculateFlipAngle(timeProgress) : 0.0;
        double flipAngle = config.enableFlip ? calculateCrackEffect(timeProgress) : 0.0;
        double grayscaleFactor = config.enableGrayscale ? calculateGrayscaleFactor(timeProgress) : 0.0;

        // Draw base image with effects - PASS BOTH timeProgress AND currentTime
        drawSingleImageWithEffects(g2d, singleEffectImage, width, height, zoomFactor, flipAngle, grayscaleFactor, timeProgress, currentTime, totalDuration);

        // Apply falling pieces effect at the end
// Apply falling pieces effect conditionally
        if (config.enableFallingPieces) {
            applyEnhancedFallingPiecesEffect(g2d, singleEffectImage, width, height, timeProgress, totalDuration);
        }
        // Draw text with Arabic audio sync
        drawArabicAudioSyncText(g2d, formattedData, displayInfo, width, height, englishFont, arabicFont, currentTime);

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }


    //lllllllllllllllllllllllllllllllllllll

    private void drawSingleImageWithEffects(Graphics2D g2d, BufferedImage originalImage, int width, int height,
                                            double zoomFactor, double flipAngle, double grayscaleFactor,
                                            double timeProgress, double currentTime, double totalDuration) {

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        Graphics2D effectG2d = (Graphics2D) g2d.create();
        effectG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        effectG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        effectG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        BufferedImage processedImage = originalImage;

        // CONDITIONAL EFFECTS based on config:
        if (config.enableColorEnhancement) {
            processedImage = applyColorEnhancement(processedImage, timeProgress);
        }

        if (config.enableWaterEffect) {
            processedImage = applyWaterEffect(processedImage, timeProgress);
        }

        if (config.enableDynamicLighting) {
            processedImage = applyDynamicLighting(processedImage, timeProgress);
        }

        if (config.enableLensDistortion) {
            processedImage = applyLensDistortion(processedImage, timeProgress);
        }

        // Apply zoom transformation
        // Apply zoom transformation
        java.util.Random random = new java.util.Random(42);
        int centerX = width / 2 + (int) (Math.sin(timeProgress * Math.PI) * 100);
        int centerY = height / 2 + (int) (Math.cos(timeProgress * Math.PI * 0.7) * 80);

// Create realistic paper tear effect if enabled
        if (config.enableFlip && Math.abs(flipAngle) > 0.01) {
            // Calculate separation distance
            int separationDistance = (int) (Math.abs(flipAngle) * 80);

            // Create jagged tear line
            int[] tearPoints = generateTearLine(height);

            // Draw left half (moves left)
            Graphics2D leftG2d = (Graphics2D) effectG2d.create();
            leftG2d.translate(centerX - separationDistance, centerY);
            leftG2d.scale(zoomFactor, zoomFactor);

            double rotationAngle = Math.sin(timeProgress * Math.PI * 2) * 2;
            leftG2d.rotate(Math.toRadians(rotationAngle));
            leftG2d.translate(-centerX, -centerY);

            // Create jagged clip path for left half
            java.awt.Polygon leftClip = new java.awt.Polygon();
            leftClip.addPoint(0, 0);
            for (int i = 0; i < tearPoints.length; i++) {
                leftClip.addPoint(width / 2 + tearPoints[i], i);
            }
            leftClip.addPoint(0, height);
            leftG2d.setClip(leftClip);
            leftG2d.drawImage(processedImage, 0, 0, null);

            // Add torn edge shadow on left side
            leftG2d.setClip(null);
            leftG2d.setColor(new Color(0, 0, 0, 80));
            leftG2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < tearPoints.length - 1; i++) {
                leftG2d.drawLine(width / 2 + tearPoints[i], i,
                        width / 2 + tearPoints[i + 1], i + 1);
            }
            leftG2d.dispose();

            // Draw right half (moves right)
            Graphics2D rightG2d = (Graphics2D) effectG2d.create();
            rightG2d.translate(centerX + separationDistance, centerY);
            rightG2d.scale(zoomFactor, zoomFactor);
            rightG2d.rotate(Math.toRadians(rotationAngle));
            rightG2d.translate(-centerX, -centerY);

            // Create jagged clip path for right half
            java.awt.Polygon rightClip = new java.awt.Polygon();
            rightClip.addPoint(width, 0);
            for (int i = 0; i < tearPoints.length; i++) {
                rightClip.addPoint(width / 2 + tearPoints[i], i);
            }
            rightClip.addPoint(width, height);
            rightG2d.setClip(rightClip);
            rightG2d.drawImage(processedImage, 0, 0, null);

            // Add torn edge highlight on right side
            rightG2d.setClip(null);
            rightG2d.setColor(new Color(255, 255, 255, 60));
            rightG2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < tearPoints.length - 1; i++) {
                rightG2d.drawLine(width / 2 + tearPoints[i] - 1, i,
                        width / 2 + tearPoints[i + 1] - 1, i + 1);
            }
            rightG2d.dispose();

        } else {
            // Normal drawing without tear
            effectG2d.translate(centerX, centerY);
            effectG2d.scale(zoomFactor, zoomFactor);

            double rotationAngle = Math.sin(timeProgress * Math.PI * 2) * 2;
            effectG2d.rotate(Math.toRadians(rotationAngle));
            effectG2d.translate(-centerX, -centerY);

            effectG2d.drawImage(processedImage, 0, 0, null);
        }

        effectG2d.dispose();

        // CONDITIONAL OVERLAY EFFECTS:
        if (config.enableVignette) {
            applyVignetteEffect(g2d, width, height, timeProgress);
        }

        if (config.enableParticles) {
            applyParticleOverlay(g2d, width, height, timeProgress);
        }

        if (config.enableLightRays) {
            applyLightRays(g2d, width, height, timeProgress);
        }

        if (config.enableSparkles) {
            applySparkleEffect(g2d, width, height, timeProgress);
        }
    }

    private int[] generateTearLine(int height) {
        int[] tearPoints = new int[height];
        java.util.Random random = new java.util.Random(42); // Fixed seed for consistency

        int currentOffset = 0;
        for (int y = 0; y < height; y++) {
            // Random jagged movement
            int change = random.nextInt(7) - 3; // -3 to +3 pixels
            currentOffset += change;

            // Keep within reasonable bounds
            currentOffset = Math.max(-20, Math.min(20, currentOffset));

            // Add occasional large tears
            if (random.nextInt(50) == 0) {
                currentOffset += random.nextInt(15) - 7;
            }

            tearPoints[y] = currentOffset;
        }

        return tearPoints;
    }


    private BufferedImage applyGrayscaleEffect(BufferedImage originalImage, double grayscaleFactor) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);

                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculate grayscale value using luminance formula
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // Blend between original color and grayscale based on grayscaleFactor
                int newR = (int) (r * (1 - grayscaleFactor) + gray * grayscaleFactor);
                int newG = (int) (g * (1 - grayscaleFactor) + gray * grayscaleFactor);
                int newB = (int) (b * (1 - grayscaleFactor) + gray * grayscaleFactor);

                // Ensure values are within valid range
                newR = Math.max(0, Math.min(255, newR));
                newG = Math.max(0, Math.min(255, newG));
                newB = Math.max(0, Math.min(255, newB));

                int newRgb = (newR << 16) | (newG << 8) | newB;
                grayscaleImage.setRGB(x, y, newRgb);
            }
        }

        return grayscaleImage;
    }

    /**
     * Draw text with Arabic audio sync (Arabic shakes when active, English static)
     */

    private java.util.List<String> wrapTextToLines(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void drawArabicAudioSyncText(Graphics2D g2d, FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                         int width, int height, Font englishFont, Font arabicFont, double currentTime) {

        if (displayInfo.currentQuote >= formattedData.lines.size()) return;

        FormattedLineArabicSync currentLine = formattedData.lines.get(displayInfo.currentQuote);
        String englishQuoteText = currentLine.englishContent;
        String arabicQuoteText = currentLine.arabicContent;

        // Font sizes - conditional for first quote and total line count
        Font displayEnglishFont, displayArabicFont;

        // Check if there's only one line in the Arabic file
        boolean isOnlyOneLine = (formattedData.lines.size() == 1);

        if (displayInfo.currentQuote == 0) {
            if (isOnlyOneLine) {
                // Single line gets smaller fonts
                displayEnglishFont = englishFont.deriveFont(englishFont.getSize() - 10f);
                displayArabicFont = arabicFont.deriveFont(arabicFont.getSize() + 15f);
            } else {
                // First quote of multiple lines gets larger fonts
                displayEnglishFont = englishFont.deriveFont(englishFont.getSize() + 8f);
                displayArabicFont = arabicFont.deriveFont(arabicFont.getSize() + 55f);
            }
        } else {
            // Other quotes get normal fonts
            displayEnglishFont = englishFont.deriveFont(englishFont.getSize() - 10f);
            displayArabicFont = arabicFont.deriveFont(arabicFont.getSize() + 15f);
        }


        if (config.removeTextAndBackground) {
            // SPECIAL MODE: Show word groups synced with audio timing
            if (currentWordTimings != null && currentWordTimings.length > 0 && displayInfo.isActive) {

                // COMMA-SEPARATED MODE
                if (config.highlightWordCount == -1) {
                    // Split Arabic text by commas
                    FormattedLineArabicSync currentQuoteLine = formattedData.lines.get(displayInfo.currentQuote);
                    String fullArabicText = currentQuoteLine.arabicContent;
                    String[] commaPhrases = fullArabicText.split("،|,");

                    // Find which phrase should be displayed based on timing
                    int phraseStartWordIdx = currentQuoteLine.wordStartIndex;
                    String displayPhrase = null;
                    int displayPhraseStartIdx = -1;

                    /*for (int phraseIdx = 0; phraseIdx < commaPhrases.length; phraseIdx++) {
                        String phrase = commaPhrases[phraseIdx].trim();
                        if (phrase.isEmpty()) continue;

                        String[] phraseWords = phrase.split("\\s+");
                        int phraseEndWordIdx = phraseStartWordIdx + phraseWords.length - 1;

                        if (phraseStartWordIdx < currentWordTimings.length && phraseEndWordIdx < currentWordTimings.length) {
                            double phraseStartTime = currentWordTimings[phraseStartWordIdx].startTime;
                            double phraseEndTime = currentWordTimings[phraseEndWordIdx].endTime;

                            if (currentTime >= phraseStartTime && currentTime <= phraseEndTime) {
                                displayPhrase = phrase;
                                displayPhraseStartIdx = phraseStartWordIdx;
                                break;
                            }
                        }

                        phraseStartWordIdx += phraseWords.length;
                    }*/

                    for (int phraseIdx = 0; phraseIdx < commaPhrases.length; phraseIdx++) {
                        String phrase = commaPhrases[phraseIdx].trim();
                        if (phrase.isEmpty()) continue;

                        String[] phraseWords = phrase.split("\\s+");
                        int phraseEndWordIdx = phraseStartWordIdx + phraseWords.length - 1;

                        // Check bounds - make sure we don't go past the end
                        if (phraseStartWordIdx < currentWordTimings.length) {
                            // Clamp end index to valid range
                            phraseEndWordIdx = Math.min(phraseEndWordIdx, currentWordTimings.length - 1);

                            double phraseStartTime = currentWordTimings[phraseStartWordIdx].startTime;
                            double phraseEndTime = currentWordTimings[phraseEndWordIdx].endTime;

                            // Check if this is the last phrase
                            boolean isLastPhrase = (phraseIdx == commaPhrases.length - 1);

                            // Display phrase during its time OR keep last phrase visible after it ends
                            if (currentTime >= phraseStartTime && (currentTime <= phraseEndTime || (isLastPhrase && currentTime > phraseEndTime))) {
                                displayPhrase = phrase;
                                displayPhraseStartIdx = phraseStartWordIdx;
                                break;
                            }
                        }

                        phraseStartWordIdx += phraseWords.length;
                    }



                    if (displayPhrase != null && !displayPhrase.isEmpty()) {
                        // Use larger font for centered display
                        Font centeredHighlightFont = displayArabicFont.deriveFont(Font.BOLD, 120f);
                        FontMetrics fm = g2d.getFontMetrics(centeredHighlightFont);

                        // Wrap text if too wide
                        int maxWidth = width - 200;
                        java.util.List<String> wrappedLines = wrapTextToLines(displayPhrase, fm, maxWidth);

                        int totalHeight = wrappedLines.size() * (int) (fm.getHeight() * 1.1);
                        int startY = (height - totalHeight) / 2;

                        // Find which word in the phrase is currently being spoken
                        String[] phraseWords = displayPhrase.split("\\s+");
                        int currentWordInPhrase = -1;
                        for (int i = 0; i < phraseWords.length; i++) {
                            int globalWordIdx = displayPhraseStartIdx + i;
                            if (globalWordIdx < currentWordTimings.length) {
                                double wordStart = currentWordTimings[globalWordIdx].startTime;
                                double wordEnd = currentWordTimings[globalWordIdx].endTime;

                                if (currentTime >= wordStart && currentTime <= wordEnd) {
                                    currentWordInPhrase = i;
                                    break;
                                }
                            }
                        }

                        // Calculate word positions in the wrapped lines to find sparkle position
                        int wordCounter = 0;
                        java.util.Map<Integer, int[]> wordPositions = new java.util.HashMap<>();
                        for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                            String line = wrappedLines.get(lineIdx);
                            String[] wordsInLine = line.split("\\s+");

                            int lineY = startY + (int) (lineIdx * fm.getHeight() * 1.1) + fm.getAscent();




                            int totalLineWidth = fm.stringWidth(line);
                            int lineStartX = (width - totalLineWidth) / 2;

                            int[] shakeOffset = calculateShakeOffset(currentTime, 4);
                            int shakeLineX = lineStartX + shakeOffset[0];
                            int shakeLineY = lineY + shakeOffset[1];

                            int currentX = shakeLineX;

                            // RTL: Map words in REVERSE order (rightmost word = first in audio)
                            for (int wordIdx = 0; wordIdx < wordsInLine.length; wordIdx++) {
                                String word = wordsInLine[wordIdx];
                                int wordWidth = fm.stringWidth(word);
                                int wordCenterX = currentX + wordWidth / 2;

                                // Store position using REVERSE index for RTL
                                int rtlWordIndex = wordCounter + (wordsInLine.length - 1 - wordIdx);
                                wordPositions.put(rtlWordIndex, new int[]{wordCenterX, shakeLineY, wordWidth});

                                currentX += wordWidth + fm.stringWidth(" ");
                            }

                            wordCounter += wordsInLine.length;
                        }

                        // Now draw the text normally (as before)



                        for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                            String line = wrappedLines.get(lineIdx);
                            int wordWidth = fm.stringWidth(line);
                            int centerX = (width - wordWidth) / 2;
                            int centerY = startY + (int) (lineIdx * fm.getHeight() * 1.1) + fm.getAscent();






                            // Apply shake effect
                            int[] shakeOffset = calculateShakeOffset(currentTime, 4);
                            int shakeX = centerX + shakeOffset[0];
                            int shakeY = centerY + shakeOffset[1];

                            g2d.setFont(centeredHighlightFont);
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                            // Draw large outer glow
                            g2d.setColor(new Color(0, 0, 0, 180));
                            for (int offset = 8; offset <= 12; offset++) {
                                g2d.drawString(line, shakeX - offset, shakeY + offset);
                                g2d.drawString(line, shakeX + offset, shakeY + offset);
                            }

                            // Draw medium glow layers
                            Color[] glowColors = {
                                    new Color(255, 140, 0, 160),
                                    new Color(255, 165, 0, 140),
                                    new Color(255, 215, 0, 120),
                                    new Color(255, 255, 0, 100),
                                    new Color(255, 255, 200, 80)
                            };

                            for (int layer = 0; layer < glowColors.length; layer++) {
                                g2d.setColor(glowColors[layer]);
                                int glowSize = 6 - layer;
                                for (int offset = 1; offset <= glowSize; offset++) {
                                    g2d.drawString(line, shakeX - offset, shakeY);
                                    g2d.drawString(line, shakeX + offset, shakeY);
                                    g2d.drawString(line, shakeX, shakeY - offset);
                                    g2d.drawString(line, shakeX, shakeY + offset);
                                    g2d.drawString(line, shakeX - offset, shakeY - offset);
                                    g2d.drawString(line, shakeX + offset, shakeY - offset);
                                    g2d.drawString(line, shakeX - offset, shakeY + offset);
                                    g2d.drawString(line, shakeX + offset, shakeY + offset);
                                }
                            }

                            // Draw main text
                            g2d.setColor(new Color(0, 0, 0, 255));
                            g2d.drawString(line, shakeX, shakeY);
                        }

                        // Add sparkles to the currently spoken word only
                        if (currentWordInPhrase >= 0 && wordPositions.containsKey(currentWordInPhrase)) {
                            int[] pos = wordPositions.get(currentWordInPhrase);
                            drawWordSparkles2(g2d, pos[0], pos[1], pos[2], fm.getHeight(), currentTime);
                        }
                    }

                } else if (config.highlightWordCount == -2) {
                    // STRIPES MODE - Show ALL comma-separated phrases as stacked stripes
                    FormattedLineArabicSync currentQuoteLine = formattedData.lines.get(displayInfo.currentQuote);
                    String fullArabicText = currentQuoteLine.arabicContent;
                    String[] commaPhrases = fullArabicText.split("،|,");

                    // Filter out empty phrases
                    java.util.List<String> validPhrases = new java.util.ArrayList<>();
                    for (String phrase : commaPhrases) {
                        String trimmed = phrase.trim();
                        if (!trimmed.isEmpty()) {
                            validPhrases.add(trimmed);
                        }
                    }

                    if (!validPhrases.isEmpty()) {
                        // Use larger font for centered display
                        // Adjust font size based on number of stripes
                        float fontSize = 70f;
                        if (validPhrases.size() > 6) {
                            fontSize = 55f; // 10 pixels smaller for more than 10 stripes
                        }
                        Font centeredHighlightFont = displayArabicFont.deriveFont(Font.BOLD, fontSize);
                        FontMetrics fm = g2d.getFontMetrics(centeredHighlightFont);

                        // Calculate total height needed for all stripes
                        int stripeSpacing = 20; // Space between stripes
                        int totalStripesHeight = 0;
                        java.util.List<java.util.List<String>> allWrappedPhrases = new java.util.ArrayList<>();

                        for (String phrase : validPhrases) {
                            int maxWidth = width - 350;
                            java.util.List<String> wrappedLines = wrapTextToLines(phrase, fm, maxWidth);
                            allWrappedPhrases.add(wrappedLines);

                            int phraseHeight = wrappedLines.size() * (int)(fm.getHeight() * 1.3);
                            totalStripesHeight += phraseHeight + stripeSpacing;
                        }

                        // Start Y position to center all stripes vertically
                        int startY = (height - totalStripesHeight) / 2;
                        int currentY = startY;

                        // Find which phrase is currently being spoken for highlighting
                        int phraseStartWordIdx = currentQuoteLine.wordStartIndex;
                        int currentlySpokenPhraseIdx = -1;

                        for (int phraseIdx = 0; phraseIdx < validPhrases.size(); phraseIdx++) {
                            String phrase = validPhrases.get(phraseIdx);
                            String[] phraseWords = phrase.split("\\s+");
                            int phraseEndWordIdx = phraseStartWordIdx + phraseWords.length - 1;

                            if (phraseStartWordIdx < currentWordTimings.length && phraseEndWordIdx < currentWordTimings.length) {
                                double phraseStartTime = currentWordTimings[phraseStartWordIdx].startTime;
                                double phraseEndTime = currentWordTimings[phraseEndWordIdx].endTime;

                                if (currentTime >= phraseStartTime && currentTime <= phraseEndTime) {
                                    currentlySpokenPhraseIdx = phraseIdx;
                                }
                            }

                            phraseStartWordIdx += phraseWords.length;
                        }

                        // Draw all stripes
                        for (int phraseIdx = 0; phraseIdx < validPhrases.size(); phraseIdx++) {
                            String phrase = validPhrases.get(phraseIdx);
                            java.util.List<String> wrappedLines = allWrappedPhrases.get(phraseIdx);

                            // Generate color for this stripe
                            Color stripeColor = generateStripeColor(phraseIdx);

                            // Check if this phrase is currently being spoken
                            boolean isCurrentPhrase = (phraseIdx == currentlySpokenPhraseIdx);
// Check if this phrase is currently being spoken

// Check if this is the first stripe
                            boolean isFirstStripe = (phraseIdx == 0);

// Apply pulsing effect to current phrase
                            // Apply pulsing effect to current phrase
                            float opacity = 1.0f;
                            int extraPadding = 0;
                            if (isCurrentPhrase) {
                                double pulse = (Math.sin(currentTime * 6.0) + 1) / 2; // 0 to 1
                                opacity = 0.7f + (float)(pulse * 0.3f); // 0.7 to 1.0
                                extraPadding = (int)(pulse * 10); // 0 to 10 pixels
                            }

                            // Draw each line of this phrase
                            //honnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn08/10/2025



                            // Draw each line of this phrase
                            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                                String line = wrappedLines.get(lineIdx);
                                int lineWidth = fm.stringWidth(line);
                                int centerX = (width - lineWidth) / 2;
                                int lineY = currentY + (int)(lineIdx * fm.getHeight() * 1.3) + fm.getAscent();

                                // Apply shake effect to currently spoken stripe
                                int shakeX = 0;
                                int shakeY = 0;
                                if (isCurrentPhrase) {
                                    int[] shakeOffset = calculateShakeOffset(currentTime, 5); // 5 pixel shake intensity
                                    shakeX = shakeOffset[0];
                                    shakeY = shakeOffset[1];
                                }

                                // Draw stripe behind text - tight fit and same length

                                // Draw stripe behind text
                                // Draw stripe behind text - tight fit and same length
                                int stripeHeight = (int)(fm.getHeight() * 1.15); // Tighter height
                                //  int stripeY = lineY - fm.getAscent() - (int)(fm.getHeight() * 0.08);
                                int stripeY = lineY - fm.getAscent() - (stripeHeight - fm.getHeight()) / 2 + 12; // Shifted down 10 pixels

                                int stripePadding = 30 + extraPadding; // Less padding

// Fixed width for all stripes (use the widest line)
                                int maxPossibleWidth = width - 250; // Fixed stripe width
                                int stripeWidth = maxPossibleWidth;
                                int stripeX = (width - stripeWidth) / 2; // Center the fixed-width stripe

                                // Apply opacity to stripe color
                                // Apply opacity to stripe color with transparency
                                // int transparency = 80; // 0=fully transparent, 255=fully opaque (adjust this value)
                                int transparency = isFirstStripe ? 255 : 80; // First stripe more opaque (200), others at 80
                                Color finalStripeColor = new Color(
                                        stripeColor.getRed(),
                                        stripeColor.getGreen(),
                                        stripeColor.getBlue(),
                                        (int)(transparency * opacity)
                                );

                                //drawTornStripe(g2d, stripeX + shakeX, stripeY + shakeY, stripeWidth, stripeHeight, finalStripeColor, currentTime + phraseIdx * 50);
                                drawTornStripe(g2d, stripeX + shakeX, stripeY + shakeY, stripeWidth, stripeHeight, finalStripeColor, phraseIdx * 12345.0);
// Draw text on top of stripe
                                g2d.setFont(centeredHighlightFont);
                                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

// Draw text shadow
                                g2d.setColor(new Color(0, 0, 0, 150));
                                g2d.drawString(line, centerX + shakeX + 2, lineY + shakeY + 2);

// Draw main text in white (brighter if currently spoken)
                                if (isCurrentPhrase) {
                                    g2d.setColor(new Color(255, 255, 255)); // Bright white
                                } else {
                                    g2d.setColor(new Color(240, 240, 240)); // Slightly dimmed
                                }
                                g2d.drawString(line, centerX + shakeX, lineY + shakeY);
                            }

                            // Move Y position for next stripe
                            currentY += wrappedLines.size() * (int)(fm.getHeight() * 1.3) + stripeSpacing;
                        }
                    }




                } else {
                    // ORIGINAL WORD-COUNT MODE (1,2,3,4 words)
                    String[] allArabicWords = formattedData.arabicSpeakableWords;
                    int displayGroupIndex = -1;

                    for (int groupIdx = 0; groupIdx < Math.ceil((double) allArabicWords.length / config.highlightWordCount); groupIdx++) {
                        int groupStartWordIdx = groupIdx * config.highlightWordCount;
                        int groupEndWordIdx = Math.min(groupStartWordIdx + config.highlightWordCount - 1, allArabicWords.length - 1);

                        if (groupStartWordIdx < currentWordTimings.length && groupEndWordIdx < currentWordTimings.length) {
                            double groupStartTime = currentWordTimings[groupStartWordIdx].startTime;
                            double groupEndTime = currentWordTimings[groupEndWordIdx].endTime;

                            if (currentTime >= groupStartTime && currentTime <= groupEndTime) {
                                displayGroupIndex = groupIdx;
                                break;
                            }
                        }
                    }

                    if (displayGroupIndex >= 0) {
                        int startWordIndex = displayGroupIndex * config.highlightWordCount;
                        StringBuilder highlightedText = new StringBuilder();
                        int wordsToShow = Math.min(config.highlightWordCount, allArabicWords.length - startWordIndex);

                        for (int i = 0; i < wordsToShow; i++) {
                            int wordIdx = startWordIndex + i;
                            if (wordIdx < allArabicWords.length) {
                                if (i > 0) highlightedText.append(" ");
                                highlightedText.append(allArabicWords[wordIdx]);
                            }
                        }

                        String currentWords = highlightedText.toString();

                        if (!currentWords.isEmpty()) {
                            Font centeredHighlightFont = displayArabicFont.deriveFont(Font.BOLD, 90f);
                            FontMetrics fm = g2d.getFontMetrics(centeredHighlightFont);

                            int wordWidth = fm.stringWidth(currentWords);
                            int centerX = (width - wordWidth) / 2;
                            int centerY = height / 2;

                            int[] shakeOffset = calculateShakeOffset(currentTime, 4);
                            int shakeX = centerX + shakeOffset[0];
                            int shakeY = centerY + shakeOffset[1];

                            g2d.setFont(centeredHighlightFont);
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                            g2d.setColor(new Color(0, 0, 0, 180));
                            for (int offset = 8; offset <= 12; offset++) {
                                g2d.drawString(currentWords, shakeX - offset, shakeY + offset);
                                g2d.drawString(currentWords, shakeX + offset, shakeY + offset);
                            }

                            Color[] glowColors = {
                                    new Color(255, 140, 0, 160),
                                    new Color(255, 165, 0, 140),
                                    new Color(255, 215, 0, 120),
                                    new Color(255, 255, 0, 100),
                                    new Color(255, 255, 200, 80)
                            };

                            for (int layer = 0; layer < glowColors.length; layer++) {
                                g2d.setColor(glowColors[layer]);
                                int glowSize = 6 - layer;
                                for (int offset = 1; offset <= glowSize; offset++) {
                                    g2d.drawString(currentWords, shakeX - offset, shakeY);
                                    g2d.drawString(currentWords, shakeX + offset, shakeY);
                                    g2d.drawString(currentWords, shakeX, shakeY - offset);
                                    g2d.drawString(currentWords, shakeX, shakeY + offset);
                                    g2d.drawString(currentWords, shakeX - offset, shakeY - offset);
                                    g2d.drawString(currentWords, shakeX + offset, shakeY - offset);
                                    g2d.drawString(currentWords, shakeX - offset, shakeY + offset);
                                    g2d.drawString(currentWords, shakeX + offset, shakeY + offset);
                                }
                            }

                            GradientPaint gradient = new GradientPaint(
                                    shakeX, shakeY - fm.getHeight() / 2, new Color(0, 0, 0, 255),
                                    shakeX, shakeY + fm.getHeight() / 2, new Color(0, 0, 0, 255)
                            );
                            g2d.setPaint(gradient);
                            g2d.drawString(currentWords, shakeX, shakeY);

                            g2d.setColor(new Color(255, 255, 255, 200));
                            Font highlightFont = centeredHighlightFont.deriveFont(centeredHighlightFont.getSize() - 5f);
                            g2d.setFont(highlightFont);

                            double shinePhase = (currentTime * 2.0) % 2.0;
                            if (shinePhase < 1.0) {
                                int shineOffset = (int) (shinePhase * wordWidth * 0.3);
                                g2d.setClip(shakeX + shineOffset - 20, shakeY - fm.getHeight(), 40, fm.getHeight());
                                g2d.drawString(currentWords, shakeX, shakeY - 2);
                                g2d.setClip(null);
                            }

                            g2d.setFont(centeredHighlightFont);
                            drawSparkleEffect(g2d, shakeX, shakeY, wordWidth, fm.getHeight(), currentTime);
                        }
                    }
                }
            }
            return; // Exit early
        }


        // NORMAL MODE: Continue with original text layout
        FontMetrics englishFm = g2d.getFontMetrics(displayEnglishFont);
        FontMetrics arabicFm = g2d.getFontMetrics(displayArabicFont);

        // Calculate text areas
        int margin = 100;
        int textWidth = width - (2 * margin);
        int englishLineHeight = (int) (englishFm.getHeight() * 1.1);
        int arabicLineHeight = (int) (arabicFm.getHeight() * 0.9);

        // Wrap both texts
        java.util.List<String> englishWrappedLines = wrapTextToLines(englishQuoteText, englishFm, textWidth);
        java.util.List<String> arabicWrappedLines = wrapTextToLines(arabicQuoteText, arabicFm, textWidth);

        // Calculate total height (Arabic above, English below, with gap)
        int arabicTotalHeight = arabicWrappedLines.size() * arabicLineHeight;
        int englishTotalHeight = englishWrappedLines.size() * englishLineHeight;

        // Reduce gap for single line
        int gapBetweenTexts = isOnlyOneLine ? 10 : 60;

        int totalTextHeight = arabicTotalHeight + gapBetweenTexts + englishTotalHeight;

        // Starting positions - center everything vertically
        int startY = (height - totalTextHeight) / 2;
        int arabicStartY = startY + arabicFm.getAscent();
        int englishStartY = startY + arabicTotalHeight + gapBetweenTexts + englishFm.getAscent();

        // Draw background
        int horizontalPadding = 25;
        int verticalPadding = 15;
        int topPadding = 12;
        int bottomPadding = 18;

        int maxActualWidth = 0;

        // Get actual width of Arabic text
        for (String arabicLine : arabicWrappedLines) {
            int actualWidth = arabicFm.stringWidth(arabicLine);
            maxActualWidth = Math.max(maxActualWidth, actualWidth);
        }

        // Get actual width of English text
        for (String englishLine : englishWrappedLines) {
            int actualWidth = englishFm.stringWidth(englishLine);
            maxActualWidth = Math.max(maxActualWidth, actualWidth);
        }

        // Ensure minimum width for very short text
        int minWidth = 200;
        maxActualWidth = Math.max(maxActualWidth, minWidth);

        // Calculate background dimensions
        int bgWidth = maxActualWidth + (2 * horizontalPadding);
        int bgHeight = totalTextHeight + topPadding + bottomPadding;

        // Center the background horizontally
        int bgX = (width - bgWidth) / 2;

        // Position background vertically to encompass all text
        int bgY = arabicStartY - arabicFm.getAscent() - topPadding;

        // Draw the perfectly fitted background
        g2d.setColor(new Color(0, 0, 0, 75));
        g2d.fillRoundRect(bgX, bgY, bgWidth, bgHeight, 40, 40);

        // Optional: Add subtle border
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(bgX, bgY, bgWidth, bgHeight, 40, 40);

        // Draw Arabic text with word-by-word highlighting
        String[] originalArabicWords = currentLine.arabicContent.split("\\s+");
        int baseArabicIndex = currentLine.wordStartIndex;
        int processedWords = 0;

        for (int lineIdx = 0; lineIdx < arabicWrappedLines.size(); lineIdx++) {
            String arabicLine = arabicWrappedLines.get(lineIdx);
            int y = arabicStartY + (lineIdx * arabicLineHeight);

            // Calculate absolute position for this line's first word
            int absoluteLineStartIndex = baseArabicIndex + processedWords;

            drawArabicCenteredLineWithWordHighlight(g2d, arabicLine, absoluteLineStartIndex,
                    displayInfo.isActive, margin, y, textWidth, displayArabicFont, arabicFm, currentTime);

            // Track processed words accurately
            String[] lineWords = arabicLine.split("\\s+");
            processedWords += lineWords.length;
        }

        // Draw English text
        for (int lineIdx = 0; lineIdx < englishWrappedLines.size(); lineIdx++) {
            String englishLine = englishWrappedLines.get(lineIdx);
            int y = englishStartY + (lineIdx * englishLineHeight);

            // English text - static, white, centered
            drawEnglishStaticCenteredLine(g2d, englishLine, margin, y, textWidth, displayEnglishFont, englishFm, currentTime);
        }
    }




    private void drawSparkleEffect(Graphics2D g2d, int centerX, int centerY, int wordWidth, int wordHeight, double currentTime) {
        // Animated sparkles around the word
        int sparkleCount = 30;
        double sparklePhase = (currentTime * 3.0) % (Math.PI * 2); // 3-second cycle

        for (int i = 0; i < sparkleCount; i++) {
            double angle = (i * Math.PI * 2 / sparkleCount) + sparklePhase;
            double distance = 80 + Math.sin(currentTime * 4 + i) * 20; // Varying distance

            int sparkleX = centerX + (int) (Math.cos(angle) * distance);
            int sparkleY = centerY + (int) (Math.sin(angle) * distance);

            // Varying sparkle sizes and opacity
            double opacity = (Math.sin(currentTime * 6 + i * 0.7) + 1) / 2; // 0 to 1
            int sparkleSize = 3 + (int) (opacity * 4);

            g2d.setColor(new Color(255, 255, 255, (int) (opacity * 255)));

            // Draw star-shaped sparkles
            drawStar(g2d, sparkleX, sparkleY, sparkleSize);
        }
    }

    private void drawStar(Graphics2D g2d, int centerX, int centerY, int size) {
        // Draw a simple 4-pointed star
        g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);
        g2d.fillRect(centerX - size, centerY - 1, size * 2, 2);
        g2d.fillRect(centerX - 1, centerY - size, 2, size * 2);
    }

    private void drawArabicCenteredLineWithWordHighlight(Graphics2D g2d, String line, int lineStartWordIndex,
                                                         boolean isQuoteActive, int x, int y, int lineWidth,
                                                         Font font, FontMetrics fm, double currentTime) {
        // ADD THIS CHECK AT THE VERY START:
        if (config.removeTextAndBackground) {
            return; // Don't draw normal text when removeTextAndBackground is enabled
        }

        String[] words = line.split("\\s+");
        boolean lineIsRTL = isRTLText(line);

        // Get current spoken Arabic word index
        int currentGlobalWordIndex = getCurrentArabicSpokenWordIndex(currentTime);

        if (lineIsRTL) {
            // RTL Arabic text with highlighting
            drawArabicRTLWithHighlight(g2d, words, lineStartWordIndex, currentGlobalWordIndex,
                    isQuoteActive, x, y, lineWidth, font, fm, currentTime);
        } else {
            // LTR text with highlighting
            drawArabicLTRWithHighlight(g2d, words, lineStartWordIndex, currentGlobalWordIndex,
                    isQuoteActive, x, y, lineWidth, font, fm, currentTime);
        }
    }

    private boolean isRTLText(String text) {
        for (char c : text.toCharArray()) {
            // Check for Arabic characters (U+0600 to U+06FF)
            if ((c >= 0x0600 && c <= 0x06FF) ||
                    // Check for Arabic Supplement (U+0750 to U+077F)
                    (c >= 0x0750 && c <= 0x077F) ||
                    // Check for Arabic Extended-A (U+08A0 to U+08FF)
                    (c >= 0x08A0 && c <= 0x08FF) ||
                    // Check for Arabic Presentation Forms (U+FB50 to U+FDFF)
                    (c >= 0xFB50 && c <= 0xFDFF) ||
                    // Check for Arabic Presentation Forms-B (U+FE70 to U+FEFF)
                    (c >= 0xFE70 && c <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    private int[] calculateShakeOffset(double currentTime, int intensity) {
        // Use time-based sine waves for smooth shake
        double shakeFrequency = 15.0; // Shake frequency (higher = faster shake)
        double timeOffset = currentTime * shakeFrequency;

        int shakeX = (int) (Math.sin(timeOffset) * intensity);
        int shakeY = (int) (Math.sin(timeOffset * 1.3) * intensity * 0.7); // Different frequency for Y

        return new int[]{shakeX, shakeY};
    }

    private void drawArabicRTLWithHighlight(Graphics2D g2d, String[] words, int lineStartWordIndex,
                                            int currentGlobalWordIndex, boolean isQuoteActive, int x, int y, int lineWidth,
                                            Font font, FontMetrics fm, double currentTime) {


        String fullLine = String.join(" ", words);
        int totalLineWidth = fm.stringWidth(fullLine);
        int startX = x + (lineWidth - totalLineWidth) / 2;

        // Calculate positions for RTL rendering
        int[] wordPositions = new int[words.length];
        int[] wordWidths = new int[words.length];

        // Calculate all word positions first (RTL)
        int tempX = startX;
        for (int i = words.length - 1; i >= 0; i--) {
            wordWidths[i] = fm.stringWidth(words[i]);
            wordPositions[i] = tempX;
            tempX += wordWidths[i];
            if (i > 0) tempX += fm.stringWidth(" ");
        }

        // Draw each word individually
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int globalWordIndex = lineStartWordIndex + i;
            boolean isCurrentWord = isQuoteActive && (globalWordIndex == currentGlobalWordIndex);

            if (isCurrentWord) {
                // CURRENTLY BEING SPOKEN - Apply effects
                int[] shakeOffset = calculateShakeOffset(currentTime, 3);
                int shakeX = wordPositions[i] + shakeOffset[0];
                int shakeY = y + shakeOffset[1];

                // Create larger, bold font
                Font highlightFont = font.deriveFont(Font.BOLD, font.getSize());
                g2d.setFont(highlightFont);

                // Draw glow effect
                g2d.setColor(new Color(255, 215, 0, 100)); // Gold glow
                for (int offset = 1; offset <= 3; offset++) {
                    g2d.drawString(word, shakeX - offset, shakeY);
                    g2d.drawString(word, shakeX + offset, shakeY);
                    g2d.drawString(word, shakeX, shakeY - offset);
                    g2d.drawString(word, shakeX, shakeY + offset);
                }

                // Draw main word in bright color (always draw highlighted words)
                g2d.setColor(new Color(255, 255, 100)); // Bright yellow
                g2d.drawString(word, shakeX, shakeY);
            } else if (!config.removeTextAndBackground) {
                // NORMAL WORD - Draw in orange (only if not in remove mode)
                g2d.setFont(font);
                g2d.setColor(Color.ORANGE);
                g2d.drawString(word, wordPositions[i], y);
            }
            // If removeTextAndBackground is true and word is not highlighted, don't draw anything
        }
    }

    private void drawArabicLTRWithHighlight(Graphics2D g2d, String[] words, int lineStartWordIndex,
                                            int currentGlobalWordIndex, boolean isQuoteActive, int x, int y, int lineWidth,
                                            Font font, FontMetrics fm, double currentTime) {

        // Calculate centering
        int totalLineWidth = 0;
        for (int i = 0; i < words.length; i++) {
            totalLineWidth += fm.stringWidth(words[i]);
            if (i < words.length - 1) {
                totalLineWidth += fm.stringWidth(" ");
            }
        }

        int startX = x + (lineWidth - totalLineWidth) / 2;
        int currentX = startX;

        // Draw each word individually
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int wordWidth = fm.stringWidth(word);
            int globalWordIndex = lineStartWordIndex + i;
            boolean isCurrentWord = isQuoteActive && (globalWordIndex == currentGlobalWordIndex);

            if (isCurrentWord) {
                // CURRENTLY BEING SPOKEN - Apply effects
                int[] shakeOffset = calculateShakeOffset(currentTime, 3);
                int shakeX = currentX + shakeOffset[0];
                int shakeY = y + shakeOffset[1];

                // Create larger, bold font
                Font highlightFont = font.deriveFont(Font.BOLD, font.getSize());
                g2d.setFont(highlightFont);

                // Draw glow effect
                g2d.setColor(new Color(255, 215, 0, 100)); // Gold glow
                for (int offset = 1; offset <= 3; offset++) {
                    g2d.drawString(word, shakeX - offset, shakeY);
                    g2d.drawString(word, shakeX + offset, shakeY);
                    g2d.drawString(word, shakeX, shakeY - offset);
                    g2d.drawString(word, shakeX, shakeY + offset);
                }

                // Draw main word in bright color (always draw highlighted words)
                g2d.setColor(new Color(255, 255, 100)); // Bright yellow
                g2d.drawString(word, shakeX, shakeY);
            } else if (!config.removeTextAndBackground) {
                // NORMAL WORD - Draw in orange (only if not in remove mode)
                g2d.setFont(font);
                g2d.setColor(Color.ORANGE);
                g2d.drawString(word, currentX, y);
            }
            // If removeTextAndBackground is true and word is not highlighted, don't draw anything

            currentX += wordWidth + fm.stringWidth(" ");
        }
    }

    /**
     * Get the index of the currently spoken word based on current time.
     * Maps ElevenLabs word index proportionally to original text word index
     * to handle word count mismatches (e.g., numbers spoken as multiple words).
     */
    private int getCurrentArabicSpokenWordIndex(double currentTime) {
        if (currentWordTimings == null || currentWordTimings.length == 0) {
            return -1;
        }

        // Find which ElevenLabs word is being spoken
        int elevenLabsWordIdx = -1;
        for (int i = 0; i < currentWordTimings.length; i++) {
            double startTime = currentWordTimings[i].startTime;
            double endTime = currentWordTimings[i].endTime;

            boolean isLastWord = (i == currentWordTimings.length - 1);
            boolean inRange = isLastWord
                    ? (currentTime >= startTime && currentTime <= endTime)
                    : (currentTime >= startTime && currentTime < endTime);

            if (inRange) {
                elevenLabsWordIdx = i;
                break;
            }
        }

        // Handle edge cases
        if (elevenLabsWordIdx == -1) {
            if (currentTime < currentWordTimings[0].startTime) {
                return -1; // Before first word
            }
            elevenLabsWordIdx = currentWordTimings.length - 1; // Past all words
        }

        // Map ElevenLabs word index proportionally to original text word index
        // This handles the case where ElevenLabs has different word count than original text
        int originalTextWordCount = 0;
        if (this.currentFormattedData != null && this.currentFormattedData.arabicSpeakableWords != null) {
            originalTextWordCount = this.currentFormattedData.arabicSpeakableWords.length;
        }

        if (originalTextWordCount == 0 || currentWordTimings.length == 0) {
            return elevenLabsWordIdx;
        }

        // Calculate proportional mapping
        double proportion = (double) elevenLabsWordIdx / currentWordTimings.length;
        int mappedIndex = (int) Math.round(proportion * originalTextWordCount);
        return Math.min(mappedIndex, originalTextWordCount - 1);
    }


    //kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk


    // ADD THIS METHOD HERE:

    private void drawEnglishStaticCenteredLine(Graphics2D g2d, String line, int x, int y, int lineWidth,
                                               Font font, FontMetrics fm, double currentTime) {
        if (config.removeTextAndBackground) {
            return; // Don't draw English text in remove mode
        }

        int textWidth = fm.stringWidth(line);
        int centeredX = x + (lineWidth - textWidth) / 2;

        // Apply shake effect to English text
        int[] shakeOffset = calculateShakeOffset(currentTime, 3); // 3 pixel shake intensity
        int shakeX = centeredX + shakeOffset[0];
        int shakeY = y + shakeOffset[1];

        g2d.setColor(Color.WHITE); // English in white, with shake
        g2d.setFont(font);
        g2d.drawString(line, shakeX, shakeY);
    }




    /**
     * Apply falling pieces effect to image at the end of video
     */
    /**
     * Enhanced falling pieces effect with rotation and fade
     */
    /**
     * Enhanced falling pieces effect with rotation and fade
     */
    /**
     * Enhanced falling pieces effect with rotation and fade
     */
    /**
     * Enhanced falling pieces effect with rotation and fade
     */


    /**
     * Enhanced falling pieces effect with rotation and fade
     */



    private static String getCleanArabicVideoName(String arabicFilePath) {
        try {
            String[] arabicLines = loadArabicTranslationsFromFile(arabicFilePath);
            if (arabicLines != null && arabicLines.length > 0) {
                String firstLine = arabicLines[0].trim();

                // Remove all non-letter characters and keep only Arabic letters and spaces
                String cleanName = firstLine.replaceAll("[^\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF\\s]", "");

                // Replace multiple spaces with single space and trim
                cleanName = cleanName.replaceAll("\\s+", " ").trim();

                // Limit length to avoid filesystem issues (max 50 characters to leave room for timestamp)
                if (cleanName.length() > 50) {
                    cleanName = cleanName.substring(0, 50).trim();
                }

                // Add timestamp (only minutes and seconds)
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("mm ss");
                String timestamp = now.format(formatter);

                String finalName = cleanName.isEmpty() ? "arabic quote video" : cleanName;
                return finalName + " " + timestamp;
            }
        } catch (Exception e) {
            System.out.println("Could not read Arabic quote for video name: " + e.getMessage());
        }

        // Fallback with timestamp
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("mm ss");
        String timestamp = now.format(formatter);
        return "arabic quote video " + timestamp;
    }


    private static String[] loadArabicTranslationsFromFile(String arabicFilePath) {
        try {
            File arabicFile = new File(arabicFilePath);
            if (!arabicFile.exists()) {
                return null;
            }

            java.util.List<String> arabicTranslations = new java.util.ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(arabicFile), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    arabicTranslations.add(line);
                }
            }
            return arabicTranslations.isEmpty() ? null : arabicTranslations.toArray(new String[0]);
        } catch (IOException e) {
            return null;
        }
    }

    private String[] loadArabicTranslationsFromFile() {
        return loadArabicTranslationsFromFile(config.arabicFilePath);
    }
    private static String[] parseNumberFormat(String text) {
        Pattern pattern = Pattern.compile("(\\d+)\\{([^}]+)\\}");
        Matcher displayMatcher = pattern.matcher(text);
        Matcher audioMatcher = pattern.matcher(text);

        StringBuffer displayBuffer = new StringBuffer();
        StringBuffer audioBuffer = new StringBuffer();

        while (displayMatcher.find()) {
            String digits = displayMatcher.group(1);
            displayMatcher.appendReplacement(displayBuffer, digits);
        }
        displayMatcher.appendTail(displayBuffer);

        while (audioMatcher.find()) {
            String words = audioMatcher.group(2);
            audioMatcher.appendReplacement(audioBuffer, words);
        }
        audioMatcher.appendTail(audioBuffer);

        return new String[]{displayBuffer.toString(), audioBuffer.toString()};
    }


    //    private double calculateTimeProgress() {
//        // You'll need to pass currentTime and totalDuration to this method
//        // For now, using a simple calculation - modify as needed
//        return (System.currentTimeMillis() % 10000) / 10000.0; // 10-second cycle
//    }
    private BufferedImage applyColorEnhancement(BufferedImage image, double timeProgress) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Dynamic color enhancement that cycles through different moods
        double colorPhase = timeProgress * Math.PI * 2;
        double saturationBoost = 1.2 + Math.sin(colorPhase) * 0.3; // 0.9 to 1.5
        double contrastBoost = 1.1 + Math.cos(colorPhase * 0.7) * 0.2; // 0.9 to 1.3

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Convert to HSB for saturation enhancement
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                hsb[1] = Math.min(1.0f, hsb[1] * (float) saturationBoost); // Enhance saturation
                hsb[2] = Math.min(1.0f, hsb[2] * (float) contrastBoost);   // Enhance brightness

                int newRgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                enhanced.setRGB(x, y, newRgb);
            }
        }
        return enhanced;
    }

    private BufferedImage applyDynamicLighting(BufferedImage image, double timeProgress) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage lit = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = lit.createGraphics();

        // Draw original image
        g2d.drawImage(image, 0, 0, null);

        // Create moving light source
        double lightPhase = timeProgress * Math.PI * 2;
        int lightX = (int) (width * 0.5 + Math.sin(lightPhase) * width * 0.3);
        int lightY = (int) (height * 0.5 + Math.cos(lightPhase * 0.8) * height * 0.3);

        // Create radial gradient for lighting
        RadialGradientPaint lightGradient = new RadialGradientPaint(
                lightX, lightY, (float) (Math.min(width, height) * 0.8),
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                        new Color(255, 255, 255, 80),  // Bright center
                        new Color(255, 255, 200, 40),  // Medium glow
                        new Color(0, 0, 0, 0)          // Transparent edge
                }
        );

        // Use SRC_OVER instead of OVERLAY
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2d.setPaint(lightGradient);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
        return lit;
    }

    private void applyLightRays(Graphics2D g2d, int width, int height, double timeProgress) {
        // Use SRC_OVER instead of OVERLAY
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

        double rayPhase = timeProgress * Math.PI;
        int rayCount = 8;

        for (int i = 0; i < rayCount; i++) {
            double angle = (i * Math.PI * 2 / rayCount) + rayPhase * 0.1;
            double rayOpacity = (Math.sin(rayPhase + i) + 1) / 2 * 0.3; // Reduced opacity

            int startX = width / 2;
            int startY = height / 2;
            int endX = (int) (startX + Math.cos(angle) * width);
            int endY = (int) (startY + Math.sin(angle) * height);

            LinearGradientPaint rayGradient = new LinearGradientPaint(
                    startX, startY, endX, endY,
                    new float[]{0.0f, 0.8f, 1.0f},
                    new Color[]{
                            new Color(255, 255, 255, (int) (rayOpacity * 255)),
                            new Color(255, 255, 255, 20),
                            new Color(255, 255, 255, 0)
                    }
            );

            g2d.setPaint(rayGradient);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(startX, startY, endX, endY);
        }

        // Reset composite
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private BufferedImage applyLensDistortion(BufferedImage image, double timeProgress) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage distorted = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Subtle barrel/pincushion distortion that changes over time
        double distortionStrength = Math.sin(timeProgress * Math.PI) * 0.1; // -0.1 to 0.1

        int centerX = width / 2;
        int centerY = height / 2;
        double maxRadius = Math.sqrt(centerX * centerX + centerY * centerY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calculate distance from center
                double dx = x - centerX;
                double dy = y - centerY;
                double radius = Math.sqrt(dx * dx + dy * dy);

                if (radius < maxRadius) {
                    // Apply lens distortion
                    double normalizedRadius = radius / maxRadius;
                    double distortionFactor = 1.0 + distortionStrength * normalizedRadius * normalizedRadius;

                    int sourceX = (int) (centerX + dx / distortionFactor);
                    int sourceY = (int) (centerY + dy / distortionFactor);

                    if (sourceX >= 0 && sourceX < width && sourceY >= 0 && sourceY < height) {
                        distorted.setRGB(x, y, image.getRGB(sourceX, sourceY));
                    }
                }
            }
        }
        return distorted;
    }

    private void applyVignetteEffect(Graphics2D g2d, int width, int height, double timeProgress) {
        // Dynamic vignette that pulses
        double vignetteStrength = 0.3 + Math.sin(timeProgress * Math.PI * 4) * 0.1; // 0.2 to 0.4

        RadialGradientPaint vignette = new RadialGradientPaint(
                width / 2, height / 2, (float) (Math.min(width, height) * 0.8),
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                        new Color(0, 0, 0, 0),                                    // Transparent center
                        new Color(0, 0, 0, (int) (vignetteStrength * 255 * 0.5)), // Medium edge
                        new Color(0, 0, 0, (int) (vignetteStrength * 255))        // Dark corners
                }
        );

        // Use SRC_OVER instead of MULTIPLY
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, width, height);

        // Reset composite
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }


    private void applyParticleOverlay(Graphics2D g2d, int width, int height, double timeProgress) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int particleCount = 200;  // Much more particles
        for (int i = 0; i < particleCount; i++) {
            double phase = timeProgress * 2 + i * 0.1;

            // Create multiple layers of particles with different movements
            double layerOffset = (i % 3) * 0.33; // 3 layers

            // X position with more variation
            double x = (Math.sin(phase + i * 0.5) * 0.4 + 0.5) * width;

            // Y position - multiple speeds for depth
            double speedMultiplier = 0.3 + (i % 5) * 0.15; // Different fall speeds
            double y = ((timeProgress * speedMultiplier + i * 0.02) % 1.0) * height;

            // Varying opacity for depth
            double baseOpacity = 0.4 + (i % 3) * 0.2; // 0.4, 0.6, or 0.8
            double opacity = (Math.sin(phase * 3) + 1) / 2 * baseOpacity;

            // Varying sizes
            int baseSize = 2 + (i % 4); // 2-5 pixels base
            int size = baseSize + (int) (Math.sin(phase * 2) * 2);

            g2d.setColor(new Color(255, 255, 255, (int) (opacity * 255)));
            g2d.fillOval((int) x, (int) y, size, size);

            // Add some particles moving diagonally
            if (i % 4 == 0) {
                double diagX = ((timeProgress * 0.3 + i * 0.05) % 1.0) * width;
                double diagY = (Math.sin(timeProgress * 2 + i) * 0.4 + 0.5) * height;
                g2d.setColor(new Color(255, 255, 255, (int) (opacity * 200)));
                g2d.fillOval((int) diagX, (int) diagY, size - 1, size - 1);
            }
        }
    }

    private void applySparkleEffect(Graphics2D g2d, int width, int height, double timeProgress) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Multiple layers of sparkles with different behaviors
        applyMajorSparkles(g2d, width, height, timeProgress);
        applyMinorSparkles(g2d, width, height, timeProgress);
        applyTwinkleStars(g2d, width, height, timeProgress);
    }

    private void applyMajorSparkles(Graphics2D g2d, int width, int height, double timeProgress) {
        int majorSparkleCount = 30;
        java.util.Random sparkleRandom = new java.util.Random(42);

        // BRIGHTER, MORE VISIBLE colors
        Color[] sparkleColors = {
                new Color(255, 50, 150),   // Hot Pink
                new Color(50, 150, 255),   // Bright Cyan
                new Color(255, 200, 0),    // Bright Gold
                new Color(200, 50, 255),   // Bright Purple
                new Color(0, 255, 150),    // Bright Mint
                new Color(255, 100, 0),    // Bright Orange
                new Color(255, 255, 0),    // Bright Yellow
                new Color(100, 255, 100)   // Bright Green
        };

        for (int i = 0; i < majorSparkleCount; i++) {
            double sparklePhase = (timeProgress * 2.0 + i * 0.3) % 1.0;

            if (sparklePhase < 0.6) {
                double fadeIn = Math.min(sparklePhase / 0.1, 1.0);
                double fadeOut = sparklePhase > 0.5 ? Math.max((0.6 - sparklePhase) / 0.1, 0.0) : 1.0;
                double opacity = fadeIn * fadeOut;

                sparkleRandom.setSeed(42 + i);
                int x = 50 + sparkleRandom.nextInt(width - 100);
                int y = 50 + sparkleRandom.nextInt(height - 100);

                double sizeMultiplier = 0.8 + Math.sin(sparklePhase * Math.PI * 4) * 0.4;
                int sparkleSize = (int) (12 * sizeMultiplier);

                Color sparkleColor = sparkleColors[i % sparkleColors.length];
                drawProfessionalSparkle(g2d, x, y, sparkleSize, opacity, sparkleColor);
            }
        }
    }

    private void applyMinorSparkles(Graphics2D g2d, int width, int height, double timeProgress) {
        // Smaller, more frequent sparkles
        int minorSparkleCount = 30;

        // Define pastel/lighter colors for minor sparkles
        Color[] minorSparkleColors = {
                new Color(255, 100, 180),  // Bright Pink
                new Color(100, 180, 255),  // Bright Cyan
                new Color(255, 220, 100),  // Bright Gold
                new Color(180, 100, 255),  // Bright Purple
                new Color(100, 255, 180),  // Bright Mint
                new Color(255, 150, 100)   // Bright Orange
        };

        for (int i = 0; i < minorSparkleCount; i++) {
            double sparklePhase = (timeProgress * 3.0 + i * 0.2) % 1.0;

            if (sparklePhase < 0.4) { // Shorter duration
                double opacity = Math.sin(sparklePhase * Math.PI / 0.4);

                // Deterministic positioning
                double angle = (i * 137.5) % 360; // Golden angle distribution
                double distance = 100 + (i * 50) % (Math.min(width, height) / 3);

                int x = (int) (width / 2 + Math.cos(Math.toRadians(angle)) * distance);
                int y = (int) (height / 2 + Math.sin(Math.toRadians(angle)) * distance);

                // Keep within bounds
                x = Math.max(10, Math.min(width - 10, x));
                y = Math.max(10, Math.min(height - 10, y));

                int sparkleSize = 6;

                // Select color for this sparkle
                Color sparkleColor = minorSparkleColors[i % minorSparkleColors.length];

                drawProfessionalSparkle(g2d, x, y, sparkleSize, opacity * 0.7, sparkleColor);
            }
        }
    }

    private void applyTwinkleStars(Graphics2D g2d, int width, int height, double timeProgress) {
        // Subtle twinkling points across the image
        int twinkleCount = 25;

        // Define soft, twinkling colors
        Color[] twinkleColors = {
                new Color(255, 150, 200),  // Visible Pink
                new Color(150, 200, 255),  // Visible Blue
                new Color(255, 255, 150),  // Visible Yellow
                new Color(200, 150, 255),  // Visible Purple
                new Color(150, 255, 200),  // Visible Mint
                new Color(255, 200, 150)   // Visible Peach
        };

        for (int i = 0; i < twinkleCount; i++) {
            double twinklePhase = (timeProgress * 4.0 + i * 0.15) % 1.0;

            // Create a gentle pulsing effect
            double opacity = (Math.sin(twinklePhase * Math.PI * 2) + 1) / 2 * 0.5; // 0 to 0.5

            if (opacity > 0.1) {
                // Grid-based positioning with slight randomness
                int gridX = (i % 5) * (width / 5) + width / 10;
                int gridY = (i / 5) * (height / 5) + height / 10;

                // Add slight offset based on time
                int offsetX = (int) (Math.sin(timeProgress * Math.PI + i) * 20);
                int offsetY = (int) (Math.cos(timeProgress * Math.PI * 0.8 + i) * 20);

                int x = gridX + offsetX;
                int y = gridY + offsetY;

                // Select color for this twinkle
                Color twinkleColor = twinkleColors[i % twinkleColors.length];

                drawSimpleTwinkle(g2d, x, y, opacity, twinkleColor);
            }
        }
    }

    private void drawProfessionalSparkle(Graphics2D g2d, int x, int y, int size, double opacity, Color sparkleColor) {
        if (opacity <= 0) return;

        Composite originalComposite = g2d.getComposite();

        // Stronger outer glow
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (opacity * 0.6))); // Increased from 0.3
        g2d.setColor(sparkleColor);
        g2d.fillOval(x - size, y - size, size * 2, size * 2);

        // Brighter main sparkle body
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity)); // Increased from 0.8
        g2d.setColor(sparkleColor);

        int halfSize = size / 2;
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Thicker lines

        g2d.drawLine(x - halfSize, y, x + halfSize, y);
        g2d.drawLine(x, y - halfSize, x, y + halfSize);

        // Bright white center
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(x - 3, y - 3, 6, 6); // Larger center

        g2d.setComposite(originalComposite);
    }

    private void drawSimpleTwinkle(Graphics2D g2d, int x, int y, double opacity, Color twinkleColor) {
        if (opacity <= 0) return;

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));

        g2d.setColor(twinkleColor);
        g2d.fillOval(x - 1, y - 1, 2, 2);

        // Tiny cross for star effect
        g2d.drawLine(x - 3, y, x + 3, y);
        g2d.drawLine(x, y - 3, x, y + 3);

        g2d.setComposite(originalComposite);
    }


//    To add image thumbnail previews when browsing, you need to create a custom file chooser with thumbnail support. Here's how to modify your browse methods:
//            1. First, add this custom thumbnail file chooser class to your code:


    private static class ThumbnailFileChooser extends JFileChooser {
        private JLabel imageLabel;
        private JPanel previewPanel;

        public ThumbnailFileChooser() {
            super();
            initializePreview();
        }

        private void initializePreview() {
            // Create preview panel
            previewPanel = new JPanel(new BorderLayout());
            previewPanel.setPreferredSize(new Dimension(200, 200));
            previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

            // Create image label for thumbnail
            imageLabel = new JLabel("No preview available", JLabel.CENTER);
            imageLabel.setPreferredSize(new Dimension(180, 150));
            imageLabel.setVerticalAlignment(JLabel.CENTER);

            previewPanel.add(imageLabel, BorderLayout.CENTER);

            // Add preview panel to file chooser
            setAccessory(previewPanel);

            // Add property change listener to update preview
            addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, e -> {
                File selectedFile = getSelectedFile();
                updatePreview(selectedFile);
            });
        }

        private void updatePreview(File file) {
            if (file == null || !file.exists() || !file.isFile()) {
                imageLabel.setIcon(null);
                imageLabel.setText("No preview available");
                return;
            }

            // Check if it's an image file
            String fileName = file.getName().toLowerCase();
            String[] imageExtensions = {".jpg",".jfif", ".jpeg", ".png", ".bmp", ".gif"};
            boolean isImage = false;

            for (String ext : imageExtensions) {
                if (fileName.endsWith(ext)) {
                    isImage = true;
                    break;
                }
            }

            if (!isImage) {
                imageLabel.setIcon(null);
                imageLabel.setText("Not an image file");
                return;
            }

            try {
                // Load and scale image for thumbnail
                BufferedImage originalImage = ImageIO.read(file);
                if (originalImage != null) {
                    ImageIcon thumbnail = createThumbnail(originalImage, 180, 150);
                    imageLabel.setIcon(thumbnail);
                    imageLabel.setText("");

                    // Add file info
                    long fileSize = file.length();
                    String sizeText = String.format("%dx%d | %.1f KB",
                            originalImage.getWidth(),
                            originalImage.getHeight(),
                            fileSize / 1024.0);
                    imageLabel.setToolTipText(sizeText);
                } else {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Cannot load image");
                }
            } catch (Exception e) {
                imageLabel.setIcon(null);
                imageLabel.setText("Error loading image");
            }
        }


        private ImageIcon createThumbnail(BufferedImage originalImage, int maxWidth, int maxHeight) {
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // Calculate scaling to maintain aspect ratio
            double scaleX = (double) maxWidth / originalWidth;
            double scaleY = (double) maxHeight / originalHeight;
            double scale = Math.min(scaleX, scaleY);

            int thumbnailWidth = (int) (originalWidth * scale);
            int thumbnailHeight = (int) (originalHeight * scale);

            // Create thumbnail
            BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            g2d.dispose();

            return new ImageIcon(thumbnail);
        }
    }


    /**
     * Professional falling pieces effect with physics and smooth easing
     */
    private void applyEnhancedFallingPiecesEffect(Graphics2D g2d, BufferedImage originalImage, int width, int height, double timeProgress, double totalDuration) {
        double audioEndTime = totalDuration - 3.0; // Start earlier for longer effect
        double effectStartTime = audioEndTime * 0.5; // Start at 50% through ending
        double effectDuration = 6.0; // Longer duration for smoother effect

        if (timeProgress * totalDuration < effectStartTime) return;
        if (timeProgress * totalDuration > effectStartTime + effectDuration) return;

        double effectProgress = (timeProgress * totalDuration - effectStartTime) / effectDuration;
        effectProgress = Math.min(effectProgress, 1.0);

        // Professional parameters
        int pieceSize = 25; // Optimal size for smooth effect
        int cols = (int) Math.ceil((double) width / pieceSize);
        int rows = (int) Math.ceil((double) height / pieceSize);

        // Enable premium rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Create depth layers for 3D effect
        for (int layer = 0; layer < 3; layer++) {
            drawFallingLayer(g2d, originalImage, width, height, effectProgress,
                    rows, cols, pieceSize, layer);
        }

        // Add premium particle effects
        addDisintegrationParticles(g2d, width, height, effectProgress);
        addShimmerEffect(g2d, width, height, effectProgress);
        addMotionBlur(g2d, effectProgress);
    }

    private void drawFallingLayer(Graphics2D g2d, BufferedImage originalImage, int width, int height,
                                  double effectProgress, int rows, int cols, int pieceSize, int layer) {

        double layerDelay = layer * 0.15; // Stagger layers for depth
        double layerProgress = Math.max(0, effectProgress - layerDelay);

        if (layerProgress <= 0) return;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * pieceSize;
                int y = row * pieceSize;

                int actualPieceWidth = Math.min(pieceSize, width - x);
                int actualPieceHeight = Math.min(pieceSize, height - y);

                if (actualPieceWidth <= 0 || actualPieceHeight <= 0) continue;

                // Advanced physics simulation
                FallingPiece piece = calculateAdvancedFalling(row, col, layerProgress,
                        x, y, actualPieceWidth, actualPieceHeight);

                if (piece.opacity > 0 && piece.y < height + 100) {
                    drawProfessionalPiece(g2d, originalImage, piece, x, y,
                            actualPieceWidth, actualPieceHeight, layer);
                }
            }
        }
    }

    private static class FallingPiece {
        double x, y, rotation, opacity, scale, velocityX, velocityY;
        boolean hasTrail;
    }

    private FallingPiece calculateAdvancedFalling(int row, int col, double progress,
                                                  int originalX, int originalY, int width, int height) {
        FallingPiece piece = new FallingPiece();

        // Sophisticated delay calculation based on image importance
        double centerDistance = Math.sqrt(Math.pow(col - 20, 2) + Math.pow(row - 35, 2));
        double normalizedDistance = centerDistance / 30.0;
        double fallDelay = (row * 0.012) + (normalizedDistance * 0.08) + (Math.random() * 0.05);

        if (progress <= fallDelay) {
            // Piece hasn't started falling yet - show with subtle pre-fall effects
            piece.x = originalX + Math.sin(progress * 20) * 2; // Subtle vibration
            piece.y = originalY + Math.sin(progress * 25) * 1;
            piece.rotation = Math.sin(progress * 15) * 2;
            piece.opacity = 1.0 - (progress / fallDelay) * 0.1; // Slight fade before falling
            piece.scale = 1.0;
            return piece;
        }

        double pieceProgress = (progress - fallDelay) / (1.0 - fallDelay);
        pieceProgress = Math.min(pieceProgress, 1.0);

        // Advanced easing with multiple phases
        double easedProgress;
        if (pieceProgress < 0.3) {
            // Slow start
            easedProgress = pieceProgress * pieceProgress * 2;
        } else if (pieceProgress < 0.7) {
            // Acceleration
            double t = (pieceProgress - 0.3) / 0.4;
            easedProgress = 0.18 + t * t * 3;
        } else {
            // Final fall with air resistance
            double t = (pieceProgress - 0.7) / 0.3;
            easedProgress = 0.9 + t * 0.8;
        }

        // Realistic physics
        double gravity = 1200; // Pixels per second squared
        double airResistance = 0.02;
        double windEffect = Math.sin(progress * 3 + col * 0.1) * 30;

        // Calculate positions
        piece.x = originalX + windEffect + (Math.sin(pieceProgress * 8 + row) * 15);
        piece.y = originalY + (easedProgress * easedProgress * gravity);

        // Complex rotation with tumbling
        piece.rotation = easedProgress * 720 + (row + col) * 45 + Math.sin(pieceProgress * 12) * 30;

        // Professional opacity curve
        if (pieceProgress < 0.8) {
            piece.opacity = 1.0 - (pieceProgress * 0.3);
        } else {
            double fadeOutProgress = (pieceProgress - 0.8) / 0.2;
            piece.opacity = 0.7 * (1.0 - fadeOutProgress * fadeOutProgress);
        }

        // Scale effect for depth
        piece.scale = 1.0 + (pieceProgress * 0.1) - (pieceProgress * pieceProgress * 0.15);

        // Trail effect for fast-moving pieces
        piece.hasTrail = pieceProgress > 0.4;

        return piece;
    }

    private void drawProfessionalPiece(Graphics2D g2d, BufferedImage originalImage, FallingPiece piece,
                                       int sourceX, int sourceY, int width, int height, int layer) {

        AffineTransform originalTransform = g2d.getTransform();
        Composite originalComposite = g2d.getComposite();

        // Layer-based depth effects
        float layerOpacity = (float) (piece.opacity * (1.0 - layer * 0.2));
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layerOpacity));

        // Motion blur trail for realism
        if (piece.hasTrail && layer == 0) {
            drawMotionTrail(g2d, originalImage, piece, sourceX, sourceY, width, height);
        }

        // 3D shadow for depth
        if (layer == 0) {
            drawPieceShadow(g2d, piece, width, height);
        }

        // Main piece rendering with advanced transformations
        g2d.translate(piece.x + width / 2, piece.y + height / 2);
        g2d.scale(piece.scale, piece.scale);
        g2d.rotate(Math.toRadians(piece.rotation));
        g2d.translate(-width / 2, -height / 2);

        // Enhanced piece rendering
        if (layer == 0) {
            // Main layer with edge enhancement
            g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 255, 255, (int) (layerOpacity * 100)));
            g2d.drawRect(-1, -1, width + 2, height + 2);
        }

        // Draw the actual piece
        g2d.drawImage(originalImage,
                0, 0, width, height,
                sourceX, sourceY, sourceX + width, sourceY + height, null);

        // Restore transforms
        g2d.setTransform(originalTransform);
        g2d.setComposite(originalComposite);
    }

    private void drawMotionTrail(Graphics2D g2d, BufferedImage originalImage, FallingPiece piece,
                                 int sourceX, int sourceY, int width, int height) {
        // Create motion blur effect
        for (int trail = 1; trail <= 3; trail++) {
            AffineTransform originalTransform = g2d.getTransform();

            double trailX = piece.x - (trail * 8);
            double trailY = piece.y - (trail * 12);
            float trailOpacity = (float) (piece.opacity * 0.3 / trail);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trailOpacity));
            g2d.translate(trailX + width / 2, trailY + height / 2);
            g2d.scale(piece.scale * 0.9, piece.scale * 0.9);
            g2d.rotate(Math.toRadians(piece.rotation - trail * 10));
            g2d.translate(-width / 2, -height / 2);

            g2d.drawImage(originalImage,
                    0, 0, width, height,
                    sourceX, sourceY, sourceX + width, sourceY + height, null);

            g2d.setTransform(originalTransform);
        }
    }

    private void drawPieceShadow(Graphics2D g2d, FallingPiece piece, int width, int height) {
        AffineTransform originalTransform = g2d.getTransform();
        Composite originalComposite = g2d.getComposite();

        // Calculate shadow offset based on piece position
        double shadowOffset = 5 + (piece.y / 100);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (piece.opacity * 0.4)));
        g2d.setColor(new Color(0, 0, 0));
        g2d.translate(piece.x + shadowOffset + width / 2, piece.y + shadowOffset + height / 2);
        g2d.scale(piece.scale * 1.1, piece.scale * 0.8); // Flatten shadow
        g2d.rotate(Math.toRadians(piece.rotation * 0.5));
        g2d.translate(-width / 2, -height / 2);

        g2d.fillRect(0, 0, width, height);

        g2d.setTransform(originalTransform);
        g2d.setComposite(originalComposite);
    }

    private void addDisintegrationParticles(Graphics2D g2d, int width, int height, double effectProgress) {
        int particleCount = (int) (effectProgress * 200);

        for (int i = 0; i < particleCount; i++) {
            double particleLife = (effectProgress * 3 + i * 0.01) % 1.0;
            if (particleLife > 0.8) continue;

            double x = Math.random() * width;
            double y = height * 0.3 + (particleLife * particleLife * height * 0.7);
            double size = 1 + Math.random() * 3;
            double opacity = (1.0 - particleLife) * 0.8;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
            g2d.setColor(new Color(255, 200, 100));
            g2d.fillOval((int) x, (int) y, (int) size, (int) size);
        }
    }

    private void addShimmerEffect(Graphics2D g2d, int width, int height, double effectProgress) {
        if (effectProgress < 0.2) return;

        double shimmerPhase = (effectProgress - 0.2) * 5;
        int shimmerCount = 12;

        for (int i = 0; i < shimmerCount; i++) {
            double angle = (i * 30 + shimmerPhase * 60) % 360;
            double distance = 50 + Math.sin(shimmerPhase + i) * 30;

            double x = width / 2 + Math.cos(Math.toRadians(angle)) * distance;
            double y = height / 2 + Math.sin(Math.toRadians(angle)) * distance;

            double opacity = (Math.sin(shimmerPhase * 4 + i) + 1) / 2 * 0.6;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
            g2d.setColor(new Color(255, 255, 255));

            // Draw shimmer burst
            for (int ray = 0; ray < 6; ray++) {
                double rayAngle = angle + ray * 60;
                int x1 = (int) x;
                int y1 = (int) y;
                int x2 = (int) (x + Math.cos(Math.toRadians(rayAngle)) * 15);
                int y2 = (int) (y + Math.sin(Math.toRadians(rayAngle)) * 15);

                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void addMotionBlur(Graphics2D g2d, double effectProgress) {
        // Reset composite for final render
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }


    private BufferedImage applyWaterEffect(BufferedImage image, double timeProgress) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage waterImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // STRONGER water wave parameters
        double waveFrequency = 0.03;    // Increased from 0.02
        double waveAmplitude = 20.0;    // Increased from 8.0 for more distortion
        double timeOffset = timeProgress * Math.PI * 6; // Faster animation

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Multiple overlapping waves for complexity
                double wave1 = Math.sin(x * waveFrequency + timeOffset) * waveAmplitude;
                double wave2 = Math.sin(y * waveFrequency * 0.7 + timeOffset * 1.3) * waveAmplitude;
                double wave3 = Math.cos((x + y) * waveFrequency * 0.5 + timeOffset * 0.8) * (waveAmplitude * 0.5);

                int sourceX = (int) (x + wave1 + wave3);
                int sourceY = (int) (y + wave2);

                // Keep within bounds
                sourceX = Math.max(0, Math.min(width - 1, sourceX));
                sourceY = Math.max(0, Math.min(height - 1, sourceY));

                // Get pixel color
                int rgb = image.getRGB(sourceX, sourceY);

                // STRONGER blue-green tint for water
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // More visible water tint
                b = Math.min(255, (int) (b * 1.25));  // Increased from 1.1
                g = Math.min(255, (int) (g * 1.15));  // Increased from 1.05
                r = (int) (r * 0.9);  // Reduce red for more blue-green

                int newRgb = (r << 16) | (g << 8) | b;
                waterImage.setRGB(x, y, newRgb);
            }
        }

        // ENHANCED water surface highlights
        Graphics2D g2d = waterImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Stronger light caustics with more visibility
        for (int i = 0; i < 5; i++) {  // Increased from 3
            double causticsPhase = timeProgress * 3 + i * 0.4;
            int causticsY = (int) ((Math.sin(causticsPhase) * 0.4 + 0.5) * height);

            // Multiple caustic bands
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)); // Increased from 0.2

            GradientPaint caustics = new GradientPaint(
                    0, causticsY - 80, new Color(150, 200, 255, 0),
                    0, causticsY, new Color(180, 220, 255, 180),  // More opaque
                    true
            );
            g2d.setPaint(caustics);
            g2d.fillRect(0, causticsY - 80, width, 160);
        }

        // Add water shimmer effect
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        for (int i = 0; i < 8; i++) {
            double shimmerPhase = timeProgress * 4 + i * 0.3;
            int shimmerX = (int) ((Math.sin(shimmerPhase * 1.5) * 0.5 + 0.5) * width);
            int shimmerY = (int) ((Math.cos(shimmerPhase) * 0.5 + 0.5) * height);

            RadialGradientPaint shimmer = new RadialGradientPaint(
                    shimmerX, shimmerY, 100,
                    new float[]{0.0f, 1.0f},
                    new Color[]{
                            new Color(200, 240, 255, 150),
                            new Color(200, 240, 255, 0)
                    }
            );
            g2d.setPaint(shimmer);
            g2d.fillOval(shimmerX - 100, shimmerY - 100, 200, 200);
        }

        g2d.dispose();
        return waterImage;
    }











   /* public void processBatchMode(String batchFolder) {
        java.util.List<BatchJob> jobs = findBatchJobs(batchFolder);

        if (jobs.isEmpty()) {
            System.out.println("No batch jobs found in: " + batchFolder);
            System.out.println("Expected format: quote1.txt, quote1AR.txt, audio1.mp3");
            return;
        }

        System.out.println("Found " + jobs.size() + " batch job(s)");

        for (int i = 0; i < jobs.size(); i++) {
            BatchJob job = jobs.get(i);
            System.out.println("\n=== Processing Batch Job " + (i + 1) + "/" + jobs.size() + " ===");

            config.textFilePath = job.englishFile;
            config.arabicFilePath = job.arabicFile;
            config.audioFilePath = job.audioFile;

            String arabicQuote = readTextFromFile(config.textFilePath);
            if (arabicQuote == null || arabicQuote.trim().isEmpty()) {
                System.out.println("Error: Text file empty - " + job.englishFile);
                continue;
            }

            for (int run = 1; run <= config.rerunCount; run++) {
                System.out.println("  Generating video " + run + " of " + config.rerunCount);

                String videoName;
                if (config.rerunCount == 1) {
                    videoName = job.outputName + ".mp4";
                } else {
                    videoName = job.outputName + "_" + run + ".mp4";
                }

                createVideoWithExactFormattingJigsawArabicAudioSync(arabicQuote, job.audioFile, videoName);

                System.out.println("  ✅ Video completed: " + videoName);

                try {
                    if (run < config.rerunCount) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("\n🎉 All " + jobs.size() + " batch job(s) completed!");

        // ADD THIS - Show completion dialog
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    null,
                    "Successfully generated " + jobs.size() + " video(s) with " + config.rerunCount + " rerun(s) each!\n" +
                            "Total videos created: " + (jobs.size() * config.rerunCount),
                    "Batch Processing Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
    }*/


    public void processBatchMode(String batchFolder) {
        try {
            File folder = new File(batchFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                System.out.println("Batch folder not found: " + batchFolder);
                return;
            }

            // Look for quote.txt and quoteAR.txt in the batch folder
            File englishFile = new File(folder, "quote.txt");
            File arabicFile = new File(folder, "quoteAR.txt");

            if (!englishFile.exists()) {
                System.out.println("ERROR: quote.txt not found in " + batchFolder);
                return;
            }

            if (!arabicFile.exists()) {
                System.out.println("ERROR: quoteAR.txt not found in " + batchFolder);
                return;
            }

            // Read all English quotes (one per line)
            java.util.List<String> englishQuotes = new java.util.ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(englishFile), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        englishQuotes.add(line);
                    }
                }
            }

            // Read all Arabic quotes (one per line)
            java.util.List<String> arabicQuotes = new java.util.ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(arabicFile), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        arabicQuotes.add(line);
                    }
                }
            }

            // Validate
            if (englishQuotes.size() != arabicQuotes.size()) {
                System.out.println("ERROR: quote.txt has " + englishQuotes.size() +
                        " lines but quoteAR.txt has " + arabicQuotes.size() + " lines!");
                return;
            }

            int totalQuotes = englishQuotes.size();
            System.out.println("Found " + totalQuotes + " quote pairs in " + batchFolder);

            // Process each quote
            for (int i = 0; i < totalQuotes; i++) {
                int quoteNumber = i + 1;
                System.out.println("\n=== Processing Quote " + quoteNumber + "/" + totalQuotes + " ===");

                String englishQuote = englishQuotes.get(i);
                String arabicQuote = arabicQuotes.get(i);

                // Find audio file: audio1.mp3, audio2.mp3, etc.
                File audioFile = findAudioFileInBatch(folder, String.valueOf(quoteNumber));
                if (audioFile == null) {
                    System.out.println("WARNING: audio" + quoteNumber + ".mp3/.wav/etc not found - skipping");
                    continue;
                }

                System.out.println("English: " + englishQuote);
                System.out.println("Arabic: " + arabicQuote);
                System.out.println("Audio: " + audioFile.getName());

                // Create temporary files for this quote
                File tempEnglishFile = File.createTempFile("batch_eng_", ".txt");
                File tempArabicFile = File.createTempFile("batch_ar_", ".txt");

                try (java.io.PrintWriter writer = new java.io.PrintWriter(tempEnglishFile, "UTF-8")) {
                    writer.println(englishQuote);
                }

                try (java.io.PrintWriter writer = new java.io.PrintWriter(tempArabicFile, "UTF-8")) {
                    writer.println(arabicQuote);
                }

                // Generate videos with reruns
                for (int run = 1; run <= config.rerunCount; run++) {
                    System.out.println("  Generating video " + run + " of " + config.rerunCount);

                    String videoName;
                    String outputBase = getCleanArabicVideoName(tempArabicFile.getAbsolutePath());

                    if (config.rerunCount == 1) {
                        videoName = outputBase + ".mp4";
                    } else {
                        videoName = outputBase + "_" + run + ".mp4";
                    }

                    config.textFilePath = tempEnglishFile.getAbsolutePath();
                    config.arabicFilePath = tempArabicFile.getAbsolutePath();

                    createVideoWithExactFormattingJigsawArabicAudioSync(
                            englishQuote, audioFile.getAbsolutePath(), videoName);

                    System.out.println("  ✅ Video completed: " + videoName);

                    if (run < config.rerunCount) {
                        Thread.sleep(1000);
                    }
                }

                // Cleanup temp files
                tempEnglishFile.delete();
                tempArabicFile.delete();
            }

            System.out.println("\n🎉 All " + totalQuotes + " quotes processed!");

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Successfully generated videos for " + totalQuotes + " quotes!\n" +
                                "Total videos: " + (totalQuotes * config.rerunCount),
                        "Batch Complete", JOptionPane.INFORMATION_MESSAGE);
            });

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static class BatchJob {
        String englishFile;
        String arabicFile;
        String audioFile;
        String outputName;

        BatchJob(String eng, String ar, String audio, String output) {
            this.englishFile = eng;
            this.arabicFile = ar;
            this.audioFile = audio;
            this.outputName = output;
        }
    }

    private java.util.List<BatchJob> findBatchJobs(String batchFolder) {
        java.util.List<BatchJob> jobs = new java.util.ArrayList<>();
        File folder = new File(batchFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Batch folder not found: " + batchFolder);
            return jobs;
        }

        // Find all English quote files (quote*.txt)
        File[] files = folder.listFiles((dir, name) ->
                name.matches("quote\\d+\\.txt"));

        if (files == null) return jobs;

        // Sort files by number
        java.util.Arrays.sort(files, (f1, f2) -> {
            String num1 = f1.getName().replaceAll("[^0-9]", "");
            String num2 = f2.getName().replaceAll("[^0-9]", "");
            return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
        });

        for (File engFile : files) {
            String fileName = engFile.getName();
            String number = fileName.replaceAll("[^0-9]", "");

            File arFile = new File(folder, "quote" + number + "AR.txt");
            File audioFile = findAudioFileInBatch(folder, number);

            if (arFile.exists() && audioFile != null) {
                // Generate output name based on Arabic file content (same as single mode)
                String outputName = getCleanArabicVideoName(arFile.getAbsolutePath());
                jobs.add(new BatchJob(
                        engFile.getAbsolutePath(),
                        arFile.getAbsolutePath(),
                        audioFile.getAbsolutePath(),
                        outputName
                ));
                System.out.println("Found batch job #" + number + ": " + engFile.getName() + ", " + arFile.getName() + ", " + audioFile.getName());
            } else {
                System.out.println("Incomplete set for quote" + number + " - skipping (missing: " +
                        (!arFile.exists() ? "Arabic file " : "") +
                        (audioFile == null ? "audio file" : "") + ")");
            }
        }

        return jobs;
    }

    private File findAudioFileInBatch(File folder, String number) {
        String[] audioExtensions = {".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"};

        // ADD THIS DEBUG LINE:
        System.out.println("Looking for audio file in: " + folder.getAbsolutePath() + " with number: " + number);

        for (String ext : audioExtensions) {
            File audioFile = new File(folder, "audio" + number + ext);
            System.out.println("  Checking: " + audioFile.getName() + " - exists: " + audioFile.exists()); // ADD THIS TOO
            if (audioFile.exists()) {
                return audioFile;
            }
        }
        return null;
    }

    private void drawSparkleEffectAroundText(Graphics2D g2d, int centerX, int centerY, int wordWidth, int wordHeight, double currentTime) {
        int sparkleCount = 20;
        double sparklePhase = (currentTime * 3.0) % (Math.PI * 2);

        for (int i = 0; i < sparkleCount; i++) {
            // Position sparkles only above and to the sides of text
            double angle = (i * Math.PI / sparkleCount) - Math.PI / 2 + sparklePhase; // Only top half
            double distance = 60 + Math.sin(currentTime * 4 + i) * 15;

            int sparkleX = centerX + (int) (Math.cos(angle) * distance);
            int sparkleY = centerY + (int) (Math.sin(angle) * distance) - wordHeight / 2;

            double opacity = (Math.sin(currentTime * 6 + i * 0.7) + 1) / 2;
            int sparkleSize = 3 + (int) (opacity * 4);

            g2d.setColor(new Color(255, 255, 255, (int) (opacity * 200)));
            drawStar(g2d, sparkleX, sparkleY, sparkleSize);
        }
    }






    private void drawWordSparkles2(Graphics2D g2d, int wordCenterX, int wordCenterY, int wordWidth, int wordHeight, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Single small star that pulses
        double pulsePhase = (currentTime * 4.0) % (Math.PI * 2);
        double pulse = (Math.sin(pulsePhase) + 1) / 2; // 0 to 1

        // Small star size varies with pulse
        int baseSize = 8;
        int starSize = baseSize + (int)(pulse * 6); // 8 to 14 pixels

        // Star opacity varies with pulse
        int opacity = 180 + (int)(pulse * 75); // 180 to 255

        // Position star above the word
        int starX = wordCenterX;
        int starY = wordCenterY - wordHeight / 2 - 30; // 30 pixels above word

        // Draw small glowing star
        g2d.setColor(new Color(255, 255, 100, opacity));
        drawLargeStar(g2d, starX, starY, starSize);

        // Add subtle glow effect
        g2d.setColor(new Color(255, 255, 255, (int)(pulse * 100)));
        drawLargeStar(g2d, starX, starY, starSize + 4);
    }

    private void drawLargeStar(Graphics2D g2d, int centerX, int centerY, int size) {
        int numPoints = 5; // 5-pointed star
        int[] xPoints = new int[numPoints * 2];
        int[] yPoints = new int[numPoints * 2];

        double outerRadius = size;
        double innerRadius = size * 0.4; // Inner points are 40% of outer

        for (int i = 0; i < numPoints * 2; i++) {
            double angle = Math.PI / 2 + (i * Math.PI / numPoints); // Start from top
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;

            xPoints[i] = centerX + (int)(Math.cos(angle) * radius);
            yPoints[i] = centerY - (int)(Math.sin(angle) * radius);
        }

        g2d.fillPolygon(xPoints, yPoints, numPoints * 2);
    }













    private Color generateStripeColor(int phraseIndex) {
        // Generate consistent random colors based on phrase index
        java.util.Random random = new java.util.Random(phraseIndex * 12345);

        // Vibrant color palette
        Color[] colorPalette = {
                new Color(220, 50, 50),    // Red
                new Color(50, 150, 220),   // Blue
                new Color(50, 180, 50),    // Green
                new Color(220, 150, 30),   // Orange/Gold
                new Color(150, 50, 200),   // Purple
                new Color(220, 80, 150),   // Pink
                new Color(30, 170, 170),   // Cyan
                new Color(200, 100, 30)    // Brown/Orange
        };

        return colorPalette[phraseIndex % colorPalette.length];
    }

    //    private void drawTornStripe(Graphics2D g2d, int x, int y, int width, int height, Color stripeColor, double currentTime) {
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        // Generate torn edges
//        int[] topEdge = generateTornEdge(width, 8, currentTime);
//        int[] bottomEdge = generateTornEdge(width, 8, currentTime + 100);
    private void drawTornStripe(Graphics2D g2d, int x, int y, int width, int height, Color stripeColor, double seed) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Generate torn edges with FIXED seed (not changing with time)
        int[] topEdge = generateTornEdge(width, 8, seed);
        int[] bottomEdge = generateTornEdge(width, 8, seed + 100);
        // Create polygon for stripe with torn edges
        java.util.List<Integer> xPoints = new java.util.ArrayList<>();
        java.util.List<Integer> yPoints = new java.util.ArrayList<>();

        // Top edge (left to right, torn)
        for (int i = 0; i < topEdge.length; i++) {
            xPoints.add(x + i);
            yPoints.add(y + topEdge[i]);
        }

        // Right edge
        xPoints.add(x + width);
        yPoints.add(y);
        xPoints.add(x + width);
        yPoints.add(y + height);

        // Bottom edge (right to left, torn)
        for (int i = bottomEdge.length - 1; i >= 0; i--) {
            xPoints.add(x + i);
            yPoints.add(y + height + bottomEdge[i]);
        }

        // Left edge
        xPoints.add(x);
        yPoints.add(y + height);
        xPoints.add(x);
        yPoints.add(y);

        // Convert lists to arrays
        int[] xArr = xPoints.stream().mapToInt(Integer::intValue).toArray();
        int[] yArr = yPoints.stream().mapToInt(Integer::intValue).toArray();

        // Draw stripe with shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillPolygon(xArr, yArr, xArr.length);

        // Draw main stripe
        for (int i = 0; i < xArr.length; i++) {
            yArr[i] -= 3; // Offset for shadow effect
        }
        g2d.setColor(stripeColor);
        g2d.fillPolygon(xArr, yArr, xArr.length);

        // Draw darker border
        g2d.setColor(new Color(stripeColor.getRed() / 2, stripeColor.getGreen() / 2, stripeColor.getBlue() / 2));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawPolygon(xArr, yArr, xArr.length);
    }

    private int[] generateTornEdge(int width, int roughness, double seed) {
        int[] edge = new int[width];
        java.util.Random random = new java.util.Random((long)(seed * 1000));

        // Generate rough, torn edge
        for (int i = 0; i < width; i++) {
            if (i % 5 == 0) {
                // Create jagged tears
                edge[i] = random.nextInt(roughness * 2) - roughness;
            } else {
                // Interpolate between tear points
                edge[i] = edge[(i / 5) * 5];
            }
        }

        return edge;
    }


























    //here
    private void generateImagesPerLineFrame(FormattedTextDataArabicSync formattedData, QuoteDisplayInfoArabicSync displayInfo,
                                            String outputPath, double currentTime,
                                            Font englishFont, Font arabicFont, double totalDuration) throws Exception {

        int width = 1080;
        int height = 1920;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw black background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        if (displayInfo.currentQuote >= formattedData.lines.size()) {
            g2d.dispose();
            ImageIO.write(image, "JPEG", new File(outputPath));
            return;
        }

        FormattedLineArabicSync currentQuoteLine = formattedData.lines.get(displayInfo.currentQuote);
        int lineNumber = displayInfo.currentQuote + 1;

        // Load all images for this line
        java.util.List<BufferedImage> lineImages = loadImagesForLine(lineNumber);

        if (!lineImages.isEmpty()) {
            BufferedImage currentImage = selectImageForTime(lineImages, currentQuoteLine, currentTime);
            BufferedImage nextImage = getNextImage(lineImages, currentQuoteLine, currentTime);

            if (currentImage != null) {
                // Store original image for effects
                BufferedImage originalImageForEffects = currentImage;

                currentImage = fitWithBlurredBackground(currentImage, width, height);

                // Check for zoom effect on the currently selected image
                String currentImageFileName = getCurrentImageFileName(lineNumber, lineImages, currentQuoteLine, currentTime);
                System.out.println("DEBUG ZOOM: Line " + lineNumber + ", CurrentImageFileName: " + currentImageFileName +
                        ", CurrentTime: " + currentTime);
                java.util.List<EffectEntry> effects = (currentImageFileName != null) ?
                        loadEffectsForImage(lineNumber, currentImageFileName) : new java.util.ArrayList<>();

                System.out.println("DEBUG ZOOM: Loaded " + effects.size() + " effects for " + currentImageFileName);
                EffectEntry zoomEffect = null;
                for (EffectEntry effect : effects) {
                    if ("zoom".equals(effect.type)) {
                        zoomEffect = effect;
                        System.out.println("DEBUG ZOOM: Found zoom effect for " + currentImageFileName +
                                " at (" + effect.x + ", " + effect.y + "), size=" + effect.size);
                        break;
                    }
                }

                if (nextImage != null && lineImages.size() > 1) {
                    nextImage = fitWithBlurredBackground(nextImage, width, height);
                    float transitionProgress = calculateTransitionAlpha(currentQuoteLine, currentTime, lineImages.size());

                    if (transitionProgress > 0) {
                        // Transition code - apply zoom to current image if it has zoom effect
                        float currentScale = 1.0f + (transitionProgress * 0.3f);
                        float currentAlpha = 1.0f - transitionProgress;

                        // Render current image (with zoom if applicable) to temporary image
                        BufferedImage currentImageToDraw = currentImage;
                        if (zoomEffect != null && displayInfo.isActive) {
                            // Calculate line duration with safety check
                            double lineDuration = currentQuoteLine.endTime - currentQuoteLine.startTime;
                            if (lineDuration <= 0) {
                                if (currentWordTimings != null && currentWordTimings.length > 0) {
                                    int startWordIdx = currentQuoteLine.wordStartIndex;
                                    int endWordIdx = startWordIdx + currentQuoteLine.wordCount - 1;
                                    if (endWordIdx < currentWordTimings.length && startWordIdx >= 0) {
                                        lineDuration = currentWordTimings[endWordIdx].endTime - currentWordTimings[startWordIdx].startTime;
                                    }
                                }
                                if (lineDuration <= 0) {
                                    lineDuration = 3.0;
                                }
                            }
                            // Render zoomed image to temporary BufferedImage
                            BufferedImage zoomedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g2dZoomed = zoomedImage.createGraphics();
                            g2dZoomed.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2dZoomed.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                            applyZoomToFittedImage(g2dZoomed, currentImage, width, height,
                                    originalImageForEffects, zoomEffect, currentQuoteLine, currentTime, lineDuration);
                            g2dZoomed.dispose();
                            currentImageToDraw = zoomedImage;
                        }

                        // Draw current image (with zoom if applicable) with transition
                        Graphics2D g2dTemp = (Graphics2D) g2d.create();
                        g2dTemp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2dTemp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));
                        int scaledWidth = (int)(width * currentScale);
                        int scaledHeight = (int)(height * currentScale);
                        int offsetX = (width - scaledWidth) / 2;
                        int offsetY = (height - scaledHeight) / 2;
                        g2dTemp.drawImage(currentImageToDraw, offsetX, offsetY, scaledWidth, scaledHeight, null);
                        g2dTemp.dispose();

                        // Draw next image (without zoom - zoom only applies to selected/current image)
                        g2dTemp = (Graphics2D) g2d.create();
                        g2dTemp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        float nextScale = 1.3f - (transitionProgress * 0.3f);
                        float nextAlpha = transitionProgress;
                        g2dTemp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, nextAlpha));
                        scaledWidth = (int)(width * nextScale);
                        scaledHeight = (int)(height * nextScale);
                        offsetX = (width - scaledWidth) / 2;
                        offsetY = (height - scaledHeight) / 2;
                        g2dTemp.drawImage(nextImage, offsetX, offsetY, scaledWidth, scaledHeight, null);
                        g2dTemp.dispose();
                    } else {
                        // No transition - check for zoom effect on current image
                        if (zoomEffect != null && displayInfo.isActive) {
                            // Calculate line duration with safety check
                            double lineDuration = currentQuoteLine.endTime - currentQuoteLine.startTime;
                            if (lineDuration <= 0) {
                                // Calculate duration from word timings if available
                                if (currentWordTimings != null && currentWordTimings.length > 0) {
                                    int startWordIdx = currentQuoteLine.wordStartIndex;
                                    int endWordIdx = startWordIdx + currentQuoteLine.wordCount - 1;
                                    if (endWordIdx < currentWordTimings.length && startWordIdx >= 0) {
                                        lineDuration = currentWordTimings[endWordIdx].endTime - currentWordTimings[startWordIdx].startTime;
                                    }
                                }
                                // If still 0, use a default duration (3 seconds)
                                if (lineDuration <= 0) {
                                    lineDuration = 3.0;
                                }
                            }
                            // Apply zoom to current image (only the selected image gets zoom)
                            applyZoomToFittedImage(g2d, currentImage, width, height,
                                    originalImageForEffects, zoomEffect, currentQuoteLine, currentTime, lineDuration);
                        } else {
                            g2d.drawImage(currentImage, 0, 0, null);
                        }
                    }
                } else {
                    // Single image or no next image - check for zoom effect
                    if (zoomEffect != null && displayInfo.isActive) {
                        // Calculate line duration with safety check
                        double lineDuration = currentQuoteLine.endTime - currentQuoteLine.startTime;
                        if (lineDuration <= 0) {
                            // Calculate duration from word timings if available
                            if (currentWordTimings != null && currentWordTimings.length > 0) {
                                int startWordIdx = currentQuoteLine.wordStartIndex;
                                int endWordIdx = startWordIdx + currentQuoteLine.wordCount - 1;
                                if (endWordIdx < currentWordTimings.length && startWordIdx >= 0) {
                                    lineDuration = currentWordTimings[endWordIdx].endTime - currentWordTimings[startWordIdx].startTime;
                                }
                            }
                            // If still 0, use a default duration (3 seconds)
                            if (lineDuration <= 0) {
                                lineDuration = 3.0;
                            }
                        }
                        // Apply zoom to fitted image (only the selected image gets zoom)
                        applyZoomToFittedImage(g2d, currentImage, width, height,
                                originalImageForEffects, zoomEffect, currentQuoteLine, currentTime, lineDuration);
                    } else {
                        g2d.drawImage(currentImage, 0, 0, null);
                    }
                }

                // ===== DRAW EFFECTS FROM SAVED CONFIG =====
                // ===== DRAW EFFECTS FROM SAVED CONFIG =====
                if (displayInfo.isActive) {
                    // currentImageFileName and effects are already defined above

                    // IMPORTANT: Only draw effects if NOT in transition
                    boolean inTransition = false;
                    if (nextImage != null && lineImages.size() > 1) {
                        float transitionProgress = calculateTransitionAlpha(currentQuoteLine, currentTime, lineImages.size());
                        inTransition = (transitionProgress > 0.0f); // In transition if progress > 0
                    }

                    // Draw effects ONLY if not transitioning and we have a current image
                    if (!inTransition && currentImageFileName != null) {
                        // Reload effects to ensure we have the latest effects for drawing (excluding zoom which is already applied)
                        effects = loadEffectsForImage(lineNumber, currentImageFileName);

                        if (!effects.isEmpty()) {
                            //System.out.println("DEBUG: Drawing effects for " + currentImageFileName);

                            // Calculate image position in frame
                            int marginHorizontal = 40;
                            int marginVertical = 200;
                            int availableWidth = width - (2 * marginHorizontal);
                            int availableHeight = height - (2 * marginVertical);

                            int originalWidth = originalImageForEffects.getWidth();
                            int originalHeight = originalImageForEffects.getHeight();
                            double originalAspect = (double) originalWidth / originalHeight;
                            boolean isPortrait = (originalHeight > originalWidth);

                            int scaledWidth, scaledHeight;
                            int offsetX, offsetY;

                            if (isPortrait) {
                                double portraitScale = 0.6;
                                scaledHeight = (int) (availableHeight * portraitScale);
                                scaledWidth = (int) (scaledHeight * originalAspect);
                                offsetX = marginHorizontal + ((availableWidth - scaledWidth) / 2);
                                offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
                            } else {
                                scaledWidth = availableWidth;
                                scaledHeight = (int) (availableWidth / originalAspect);
                                offsetX = marginHorizontal;
                                offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
                            }

                            // Draw each effect
                            for (EffectEntry effect : effects) {
                                // Transform image coordinates to video frame coordinates
                                double scaleX = (double) scaledWidth / originalWidth;
                                double scaleY = (double) scaledHeight / originalHeight;

                                int videoX = offsetX + (int)(effect.x * scaleX);
                                int videoY = offsetY + (int)(effect.y * scaleY);

                                switch (effect.type) {
                                    case "text":
                                        drawTextCaptionEffect(g2d, effect.text, videoX, videoY, arabicFont, effect.fontSize, currentTime);
                                        break;

                                    case "zoom":
                                        // Zoom animation is already applied to the whole image (see above)
                                        // No visual indicator needed - just the zoom effect itself
                                        break;

                                    case "dot":
                                        drawBlinkingDotEffect(g2d, videoX, videoY, currentTime);
                                        break;

                                    case "arrow":
                                        drawArrowPointerEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "circle":
                                        drawCircleHighlightEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "spotlight":
                                        drawSpotlightEffect(g2d, videoX, videoY, effect.size, width, height, currentTime);
                                        break;

                                    case "star":
                                        drawStarBurstEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "ripple":
                                        drawRippleWaveEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "focus":
                                        drawFocusFrameEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "underline":
                                        drawUnderlineBoxEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;

                                    case "glow":
                                        drawGlowPulseEffect(g2d, videoX, videoY, effect.size, currentTime);
                                        break;
                                }
                            }
                        }
                    } else if (inTransition) {
                        //System.out.println("DEBUG: Skipping effects - in transition");
                    }
                }
// ===== END OF EFFECTS =====
                // ===== END OF EFFECTS =====
            }
        }

        // Draw text (UNCHANGED - keep all your existing text code)
        String arabicText = currentQuoteLine.arabicContent;
        if (arabicText != null && !arabicText.isEmpty()) {
            // ... ALL YOUR EXISTING TEXT DRAWING CODE ...

            boolean isFirstLine = (displayInfo.currentQuote == 0);

            if (isFirstLine) {
                // Title text code with configurable effects
                drawFirstLineWithEffects(g2d, arabicText, arabicFont, width, height,
                        currentTime, currentQuoteLine.startTime, currentQuoteLine.endTime);
            } else {
                // Regular text code...
                Font textFont = arabicFont.deriveFont(Font.BOLD, 40f);
                FontMetrics fm = g2d.getFontMetrics(textFont);
                int textPadding = 140;
                int textWidth = width - (2 * textPadding);
                java.util.List<String> wrappedLines = wrapTextToLines(arabicText, fm, textWidth);
                int totalTextHeight = wrappedLines.size() * (int)(fm.getHeight() * 1.2);
                int textStartY = (int)(height * 0.8) - (totalTextHeight / 2);

                for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                    String line = wrappedLines.get(lineIdx);
                    int lineWidth = fm.stringWidth(line);
                    int centerX = (width - lineWidth) / 2;
                    int lineY = textStartY + (int)(lineIdx * fm.getHeight() * 1.1) + fm.getAscent();

                    g2d.setFont(textFont);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(line, centerX, lineY);
                }
            }
        }

        g2d.dispose();
        ImageIO.write(image, "JPEG", new File(outputPath));
    }

    /**
     * Draw first line text with all configurable effects
     */
    private void drawFirstLineWithEffects(Graphics2D g2d, String arabicText, Font arabicFont,
                                          int width, int height, double currentTime,
                                          double lineStartTime, double lineEndTime) {

        // Calculate animation progress (0.0 to 1.0)
        double lineDuration = lineEndTime - lineStartTime;
        double elapsed = currentTime - lineStartTime;
        double animProgress = Math.min(1.0, Math.max(0.0, elapsed / Math.max(0.5, lineDuration * 0.3))); // Animate over first 30% of line

        // Create title font with configurable size
        Font titleFont = arabicFont.deriveFont(Font.BOLD, (float) config.firstLineFontSize);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics(titleFont);

        // Calculate text wrapping with proper padding
        int textPadding = 80;
        int textWidth = width - (2 * textPadding);
        java.util.List<String> wrappedLines = wrapTextToLines(arabicText, fm, textWidth);
        int textStartY = config.imagesPerLineFirstLineY;

        // Calculate total text block dimensions for centering effects
        int totalTextHeight = 0;
        for (int i = 0; i < wrappedLines.size(); i++) {
            totalTextHeight += (int)(fm.getHeight() * 1.1);
        }
        int textCenterY = textStartY + totalTextHeight / 2;
        int textCenterX = width / 2;

        // Apply tilt/rotation if enabled
        Graphics2D g2dDraw = g2d;
        java.awt.geom.AffineTransform originalTransform = g2d.getTransform();

        if (config.firstLineTiltAngle != 0) {
            g2dDraw = (Graphics2D) g2d.create();
            g2dDraw.rotate(Math.toRadians(config.firstLineTiltAngle), textCenterX, textCenterY);
        }

        // Draw each wrapped line
        for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
            String line = wrappedLines.get(lineIdx);
            int lineWidth = fm.stringWidth(line);
            int baseX = (width - lineWidth) / 2;
            int baseY = textStartY + (int)(lineIdx * fm.getHeight() * 1.1) + fm.getAscent();

            // Apply animation-based position/alpha modifications
            int drawX = baseX;
            int drawY = baseY;
            float alpha = 1.0f;

            // Animation type effects
            switch (config.firstLineAnimationType) {
                case 1: // Fade In
                    alpha = (float) animProgress;
                    break;

                case 2: // Typewriter - reveal characters progressively
                    int charsToShow = (int)(line.length() * animProgress);
                    line = line.substring(0, Math.max(0, charsToShow));
                    break;

                case 3: // Wave - vertical oscillation per character
                    // Wave is handled per-character below
                    break;

                case 4: // Bounce
                    double bounceOffset = Math.sin(animProgress * Math.PI) * 30 * (1 - animProgress);
                    drawY = baseY - (int) bounceOffset;
                    break;

                case 5: // Glow Pulse - handled in glow section
                    break;

                case 6: // Scale In
                    float scale = (float) (0.3 + 0.7 * animProgress);
                    // Scale is applied by temporarily changing font size
                    Font scaledFont = titleFont.deriveFont(titleFont.getSize() * scale);
                    g2dDraw.setFont(scaledFont);
                    FontMetrics scaledFm = g2dDraw.getFontMetrics();
                    int scaledWidth = scaledFm.stringWidth(line);
                    drawX = (width - scaledWidth) / 2;
                    drawY = baseY - (int)((fm.getHeight() - scaledFm.getHeight()) / 2);
                    fm = scaledFm; // Update fm for this line
                    break;

                case 7: // Slide In (from right for RTL text)
                    int slideOffset = (int)((1 - animProgress) * 500);
                    drawX = baseX - slideOffset;
                    break;
            }

            // Apply shake effect
            if (config.firstLineShakeEnabled) {
                java.util.Random shakeRandom = new java.util.Random((long)(currentTime * 1000));
                int shakeX = shakeRandom.nextInt(config.firstLineShakeIntensity * 2) - config.firstLineShakeIntensity;
                int shakeY = shakeRandom.nextInt(config.firstLineShakeIntensity * 2) - config.firstLineShakeIntensity;
                drawX += shakeX;
                drawY += shakeY;
            }

            // Set composite for alpha
            Composite originalComposite = g2dDraw.getComposite();
            if (alpha < 1.0f) {
                g2dDraw.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }

            // Wave animation - draw character by character
            if (config.firstLineAnimationType == 3) {
                drawWaveText(g2dDraw, line, drawX, drawY, fm, currentTime, lineIdx);
            } else {
                // Standard drawing with all effects

                // 1. Glow effect (drawn first, behind everything)
                if (config.firstLineGlowEnabled) {
                    float glowIntensity = 1.0f;
                    if (config.firstLineAnimationType == 5) { // Glow Pulse
                        glowIntensity = 0.5f + 0.5f * (float)Math.sin(currentTime * 4);
                    }
                    int glowAlpha = (int)(100 * glowIntensity);
                    Color glowColor = new Color(
                            config.firstLineGlowColor.getRed(),
                            config.firstLineGlowColor.getGreen(),
                            config.firstLineGlowColor.getBlue(),
                            Math.min(255, glowAlpha));
                    g2dDraw.setColor(glowColor);
                    for (int offset = 12; offset > 0; offset -= 2) {
                        g2dDraw.drawString(line, drawX - offset, drawY);
                        g2dDraw.drawString(line, drawX + offset, drawY);
                        g2dDraw.drawString(line, drawX, drawY - offset);
                        g2dDraw.drawString(line, drawX, drawY + offset);
                        // Diagonals for smoother glow
                        int diag = (int)(offset * 0.7);
                        g2dDraw.drawString(line, drawX - diag, drawY - diag);
                        g2dDraw.drawString(line, drawX + diag, drawY - diag);
                        g2dDraw.drawString(line, drawX - diag, drawY + diag);
                        g2dDraw.drawString(line, drawX + diag, drawY + diag);
                    }
                }

                // 2. Shadow effect
                if (config.firstLineShadowEnabled) {
                    g2dDraw.setColor(new Color(0, 0, 0, 180));
                    g2dDraw.drawString(line, drawX + config.firstLineShadowOffset,
                            drawY + config.firstLineShadowOffset);
                }

                // 3. Outline effect
                if (config.firstLineOutlineEnabled) {
                    g2dDraw.setColor(config.firstLineOutlineColor);
                    for (int ox = -3; ox <= 3; ox++) {
                        for (int oy = -3; oy <= 3; oy++) {
                            if (ox != 0 || oy != 0) {
                                g2dDraw.drawString(line, drawX + ox, drawY + oy);
                            }
                        }
                    }
                }

                // 4. Main text color
                g2dDraw.setColor(config.firstLineTextColor);
                g2dDraw.drawString(line, drawX, drawY);

                // 5. Highlight effect (subtle white on top)
                g2dDraw.setColor(new Color(255, 255, 255, 60));
                g2dDraw.drawString(line, drawX, drawY - 1);
            }

            // Restore composite
            if (alpha < 1.0f) {
                g2dDraw.setComposite(originalComposite);
            }

            // Reset font if it was scaled
            if (config.firstLineAnimationType == 6) {
                g2dDraw.setFont(titleFont);
                fm = g2dDraw.getFontMetrics();
            }
        }

        // Dispose rotated graphics if created
        if (config.firstLineTiltAngle != 0) {
            g2dDraw.dispose();
        }
    }

    /**
     * Draw text with wave animation effect
     */
    private void drawWaveText(Graphics2D g2d, String text, int startX, int baseY,
                              FontMetrics fm, double currentTime, int lineIndex) {
        int x = startX;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = fm.charWidth(c);

            // Calculate wave offset for this character
            double wavePhase = currentTime * 3 + i * 0.3 + lineIndex * 0.5;
            int waveOffset = (int)(Math.sin(wavePhase) * 8);

            int charY = baseY + waveOffset;

            // Draw with all effects
            if (config.firstLineGlowEnabled) {
                Color glowColor = new Color(
                        config.firstLineGlowColor.getRed(),
                        config.firstLineGlowColor.getGreen(),
                        config.firstLineGlowColor.getBlue(), 80);
                g2d.setColor(glowColor);
                for (int offset = 6; offset > 0; offset -= 2) {
                    g2d.drawString(charStr, x - offset, charY);
                    g2d.drawString(charStr, x + offset, charY);
                }
            }

            if (config.firstLineShadowEnabled) {
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.drawString(charStr, x + config.firstLineShadowOffset, charY + config.firstLineShadowOffset);
            }

            if (config.firstLineOutlineEnabled) {
                g2d.setColor(config.firstLineOutlineColor);
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oy = -2; oy <= 2; oy++) {
                        if (ox != 0 || oy != 0) {
                            g2d.drawString(charStr, x + ox, charY + oy);
                        }
                    }
                }
            }

            g2d.setColor(config.firstLineTextColor);
            g2d.drawString(charStr, x, charY);

            x += charWidth;
        }
    }


    /**
     * Fit image with blurred background - professional Instagram-style
     */


    /**
     * Get the filename of the currently displayed image
     */
    /**
     * Get the filename of the currently displayed image (NOT during transitions)
     */
    private String getCurrentImageFileName(int lineNumber, java.util.List<BufferedImage> lineImages,
                                           FormattedLineArabicSync currentLine, double currentTime) {
        // Find all image files for this line
        File folder = new File(config.imagesPerLineFolder);
        File[] files = folder.listFiles();

        if (files == null) return null;

        java.util.List<String> imageNames = new java.util.ArrayList<>();

        for (File file : files) {
            String name = file.getName();
            int extractedLineNumber = extractLineNumberFromImageName(name);
            if (extractedLineNumber == lineNumber) {
                imageNames.add(name);
            }
        }

        // Sort them properly by full number
        imageNames.sort((n1, n2) -> {
            int num1 = extractFullNumberFromImageName(n1);
            int num2 = extractFullNumberFromImageName(n2);
            return Integer.compare(num1, num2);
        });

        if (imageNames.isEmpty()) return null;

        // If only one image, return it
        if (imageNames.size() == 1) {
            return imageNames.get(0);
        }

        // Calculate which image is currently displayed
        if (currentWordTimings == null || currentWordTimings.length == 0) {
            return imageNames.get(0);
        }

        int startWordIdx = currentLine.wordStartIndex;
        int endWordIdx = startWordIdx + currentLine.wordCount - 1;

        if (startWordIdx >= currentWordTimings.length || endWordIdx >= currentWordTimings.length) {
            return imageNames.get(0);
        }

        double lineStartTime = currentWordTimings[startWordIdx].startTime;
        double lineEndTime = currentWordTimings[endWordIdx].endTime;
        double lineDuration = lineEndTime - lineStartTime;

        if (lineDuration <= 0) return imageNames.get(0);

        double timeIntoLine = currentTime - lineStartTime;
        double progress = timeIntoLine / lineDuration;
        progress = Math.max(0, Math.min(1, progress));

        // Calculate which segment we're in (EXCLUDING transition zones)
        double segmentDuration = 1.0 / imageNames.size();
        int currentSegment = (int)(progress / segmentDuration);
        currentSegment = Math.min(currentSegment, imageNames.size() - 1);

        return imageNames.get(currentSegment);
    }






    /**
     * Apply rounded corners to an image
     */
    private BufferedImage applyRoundedCorners(BufferedImage image, int cornerRadius) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rounded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rounded.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Create rounded rectangle clip
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Draw the image
        g2d.drawImage(image, 0, 0, null);

        g2d.dispose();
        return rounded;
    }


    ///hereeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
    private BufferedImage fitWithBlurredBackground(BufferedImage original, int targetWidth, int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Add margins
        int marginHorizontal = 40;  // Space from left/right edges
        int marginVertical = 200;   // Space from top/bottom edges

        // Reduce available space by margins
        int availableWidth = targetWidth - (2 * marginHorizontal);
        int availableHeight = targetHeight - (2 * marginVertical);

        // Calculate aspect ratios
        double originalAspect = (double) originalWidth / originalHeight;
        double availableAspect = (double) availableWidth / availableHeight;

        // Simple check: is the original image taller than it is wide?
        boolean isPortrait = (originalHeight > originalWidth);

        int scaledWidth, scaledHeight;
        int offsetX, offsetY;

        //System.out.println("DEBUG: Image " + originalWidth + "x" + originalHeight +
        //" | Is portrait: " + isPortrait);

        if (isPortrait) {
            // Portrait/tall image - scale down more
            //System.out.println("DEBUG: Portrait image - scaling down");
            double portraitScale = 0.6;  // Scale to 50%

            scaledHeight = (int) (availableHeight * portraitScale);
            scaledWidth = (int) (scaledHeight * originalAspect);

            //System.out.println("DEBUG: Scaled to " + scaledWidth + "x" + scaledHeight);

            // Center both horizontally and vertically
            offsetX = marginHorizontal + ((availableWidth - scaledWidth) / 2);
            offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
        } else {
            // Landscape/wide image - normal sizing
            //System.out.println("DEBUG: Landscape image - normal sizing");
            scaledWidth = availableWidth;
            scaledHeight = (int) (availableWidth / originalAspect);
            offsetX = marginHorizontal;
            offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
        }

        // Create result with blurred background
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw blurred background (stretched to fill)
        BufferedImage blurred = applyGaussianBlur(original, 30);
        g2d.drawImage(blurred, 0, 0, targetWidth, targetHeight, null);

        // Darken the background
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Draw main image centered with margins (RESIZED to fit)
        // Scale the original image first
        BufferedImage scaledOriginal = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2dScale = scaledOriginal.createGraphics();
        g2dScale.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2dScale.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2dScale.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2dScale.dispose();

// ===== ADD ROUNDED CORNERS HERE =====
        int cornerRadius = 50; // Adjust this value: 30-80 pixels recommended
        BufferedImage roundedImage = applyRoundedCorners(scaledOriginal, cornerRadius);

// Draw rounded image
        g2d.drawImage(roundedImage, offsetX, offsetY, null);

        g2d.dispose();
        return result;
    }






    /**
     * Apply blur only to the edges (vignette effect)
     */

    /**
     * Apply Gaussian blur
     */
    private BufferedImage applyGaussianBlur(BufferedImage image, int radius) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Scale down for faster blur processing
        int blurWidth = width / 4;
        int blurHeight = height / 4;

        BufferedImage small = new BufferedImage(blurWidth, blurHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = small.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, blurWidth, blurHeight, null);
        g2d.dispose();

        // Apply box blur multiple times
        BufferedImage blurred = small;
        for (int i = 0; i < 3; i++) {
            blurred = boxBlur(blurred, Math.max(1, radius / 12));
        }

        return blurred;
    }

    /**
     * Box blur helper
     */
    private BufferedImage boxBlur(BufferedImage image, int radius) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, count = 0;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = Math.max(0, Math.min(width - 1, x + dx));
                        int ny = Math.max(0, Math.min(height - 1, y + dy));

                        int rgb = image.getRGB(nx, ny);
                        r += (rgb >> 16) & 0xFF;
                        g += (rgb >> 8) & 0xFF;
                        b += rgb & 0xFF;
                        count++;
                    }
                }

                r /= count;
                g /= count;
                b /= count;

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }


    private BufferedImage getNextImage(java.util.List<BufferedImage> lineImages,
                                       FormattedLineArabicSync currentLine,
                                       double currentTime) {
        if (lineImages.isEmpty() || lineImages.size() == 1) return null;

        if (currentWordTimings == null || currentWordTimings.length == 0) {
            return null;
        }

        int startWordIdx = currentLine.wordStartIndex;
        int endWordIdx = startWordIdx + currentLine.wordCount - 1;

        if (startWordIdx >= currentWordTimings.length || endWordIdx >= currentWordTimings.length) {
            return null;
        }

        double lineStartTime = currentWordTimings[startWordIdx].startTime;
        double lineEndTime = currentWordTimings[endWordIdx].endTime;
        double lineDuration = lineEndTime - lineStartTime;

        if (lineDuration <= 0) return null;

        double timeIntoLine = currentTime - lineStartTime;
        double progress = timeIntoLine / lineDuration;
        progress = Math.max(0, Math.min(1, progress));

        int currentImageIndex = (int)(progress * lineImages.size());
        currentImageIndex = Math.min(currentImageIndex, lineImages.size() - 1);

        // Get next image index
        int nextImageIndex = currentImageIndex + 1;

        if (nextImageIndex >= lineImages.size()) {
            return null; // No next image
        }

        return lineImages.get(nextImageIndex);
    }

    private float calculateTransitionAlpha(FormattedLineArabicSync currentLine,
                                           double currentTime,
                                           int totalImages) {
        if (currentWordTimings == null || currentWordTimings.length == 0) {
            return 0f;
        }

        int startWordIdx = currentLine.wordStartIndex;
        int endWordIdx = startWordIdx + currentLine.wordCount - 1;

        if (startWordIdx >= currentWordTimings.length || endWordIdx >= currentWordTimings.length) {
            return 0f;
        }

        double lineStartTime = currentWordTimings[startWordIdx].startTime;
        double lineEndTime = currentWordTimings[endWordIdx].endTime;
        double lineDuration = lineEndTime - lineStartTime;

        if (lineDuration <= 0) return 0f;

        double timeIntoLine = currentTime - lineStartTime;
        double progress = timeIntoLine / lineDuration;
        progress = Math.max(0, Math.min(1, progress));

        // Calculate which image segment we're in
        double segmentDuration = 1.0 / totalImages;
        double currentSegment = progress / segmentDuration;
        double progressInSegment = (currentSegment - Math.floor(currentSegment));

        // Transition duration (25% of segment for smooth zoom)
        double transitionDuration = 0.25;

        // Calculate if we're in transition zone (last 25% of segment)
        if (progressInSegment > (1.0 - transitionDuration)) {
            // Map the last 25% to 0.0-1.0 alpha with easing
            double rawProgress = (progressInSegment - (1.0 - transitionDuration)) / transitionDuration;

            // Apply ease-in-out for smoother zoom
            float alpha = (float)(rawProgress < 0.5
                    ? 2 * rawProgress * rawProgress
                    : 1 - Math.pow(-2 * rawProgress + 2, 2) / 2);

            return Math.min(1.0f, Math.max(0f, alpha));
        }

        return 0f; // Not in transition zone
    }





    private BufferedImage selectImageForTime(java.util.List<BufferedImage> lineImages,
                                             FormattedLineArabicSync currentLine,
                                             double currentTime) {
        if (lineImages.isEmpty()) return null;
        if (lineImages.size() == 1) return lineImages.get(0); // Only one image

        // Calculate line duration
        if (currentWordTimings == null || currentWordTimings.length == 0) {
            return lineImages.get(0); // Fallback to first image
        }

        int startWordIdx = currentLine.wordStartIndex;
        int endWordIdx = startWordIdx + currentLine.wordCount - 1;

        if (startWordIdx >= currentWordTimings.length || endWordIdx >= currentWordTimings.length) {
            return lineImages.get(0);
        }

        double lineStartTime = currentWordTimings[startWordIdx].startTime;
        double lineEndTime = currentWordTimings[endWordIdx].endTime;
        double lineDuration = lineEndTime - lineStartTime;

        if (lineDuration <= 0) return lineImages.get(0);

        // Calculate which image to show based on time progress through the line
        double timeIntoLine = currentTime - lineStartTime;
        double progress = timeIntoLine / lineDuration;

        // Clamp progress between 0 and 1
        progress = Math.max(0, Math.min(1, progress));

        // Map progress to image index
        int imageIndex = (int)(progress * lineImages.size());
        imageIndex = Math.min(imageIndex, lineImages.size() - 1); // Prevent overflow

        return lineImages.get(imageIndex);
    }

    /**
     * Extract the line number from an image filename
     * New format with underscore: 1_1.jpg -> 1, 10_2.webp -> 10, 12_1.png -> 12
     * Also supports old formats for backward compatibility:
     *   - image1.jpg -> 1, image11.jpg -> 1 (first digit)
     *   - 1.webp -> 1, 11.png -> 1 (first digit without underscore)
     */
    private static int extractLineNumberFromImageName(String fileName) {
        String name = fileName.toLowerCase();
        String numberPart = "";

        // Check if it starts with "image" (old format)
        if (name.startsWith("image")) {
            // Extract the number part after "image"
            numberPart = name.substring(5); // After "image"
        } else {
            // New format: just a number (e.g., "1_1.webp", "10_2.png", "12.jpg")
            numberPart = name;
        }

        // Remove extension
        int dotIndex = numberPart.lastIndexOf('.');
        if (dotIndex > 0) {
            numberPart = numberPart.substring(0, dotIndex);
        }

        // Check for underscore format first (new format: lineNumber_imageIndex)
        if (numberPart.contains("_")) {
            String lineNumberStr = numberPart.substring(0, numberPart.indexOf('_'));
            try {
                return Integer.parseInt(lineNumberStr);
            } catch (Exception e) {
                return -1;
            }
        }

        // Remove any dash and suffix (e.g., "1-1" -> "1") for old format
        if (numberPart.contains("-")) {
            numberPart = numberPart.substring(0, numberPart.indexOf('-'));
        }

        // Fallback: Extract first digit (old format compatibility)
        try {
            if (numberPart.isEmpty()) {
                return -1;
            }
            String firstDigit = numberPart.substring(0, 1);
            return Integer.parseInt(firstDigit);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Extract the image index from an image filename for sorting within a line
     * New format with underscore: 1_1.jpg -> 1, 1_2.jpg -> 2, 10_3.webp -> 3
     * Old format (backward compatibility): image1.jpg -> 1, image11.jpg -> 11, 82.jpg -> 82
     */
    private static int extractFullNumberFromImageName(String fileName) {
        String name = fileName.toLowerCase();
        String numberPart = "";

        // Check if it starts with "image" (old format)
        if (name.startsWith("image")) {
            // Extract the number part after "image"
            numberPart = name.substring(5); // After "image"
        } else {
            // New format: just a number (e.g., "1_1.webp", "10_2.png", "82.jpg")
            numberPart = name;
        }

        // Remove extension
        int dotIndex = numberPart.lastIndexOf('.');
        if (dotIndex > 0) {
            numberPart = numberPart.substring(0, dotIndex);
        }

        // Check for underscore format first (new format: lineNumber_imageIndex)
        if (numberPart.contains("_")) {
            String imageIndexStr = numberPart.substring(numberPart.indexOf('_') + 1);
            try {
                return Integer.parseInt(imageIndexStr);
            } catch (Exception e) {
                return 0;
            }
        }

        // Remove any dash and suffix (e.g., "1-1" -> "1") for old format
        if (numberPart.contains("-")) {
            numberPart = numberPart.substring(0, numberPart.indexOf('-'));
        }

        // Fallback: Extract full number (old format compatibility)
        try {
            if (numberPart.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(numberPart);
        } catch (Exception e) {
            return 0;
        }
    }

    private java.util.List<BufferedImage> loadImagesForLine(int lineNumber) {
        // Check cache first to avoid repeated disk reads
        if (lineImageCache.containsKey(lineNumber)) {
            return lineImageCache.get(lineNumber);
        }

        java.util.List<BufferedImage> images = new java.util.ArrayList<>();
        File folder = new File(config.imagesPerLineFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Images per line folder not found: " + config.imagesPerLineFolder);
            return images;
        }
        String[] extensions = {".jpg", ".jfif", ".jpeg", ".png", ".bmp", ".gif", ".webp"};
        File[] allFiles = folder.listFiles();

        if (allFiles == null) return images;

        // Pattern: image1.jpg, image11.jpg, image12.jpg for line 1 (first digit is 1)
        //          image2.jpg, image21.jpg, image22.jpg for line 2 (first digit is 2)
        //          image82.jpg for line 8 (first digit is 8)
        java.util.List<File> matchingFiles = new java.util.ArrayList<>();

        for (File file : allFiles) {
            String fileName = file.getName().toLowerCase();

            // Check if file matches line number pattern based on first digit
            // Supports both formats: "image1.jpg" (old) or "1.jpg", "11.jpg" (new)
            for (String ext : extensions) {
                if (fileName.endsWith(ext)) {
                    int extractedLineNumber = extractLineNumberFromImageName(fileName);
                    if (extractedLineNumber == lineNumber) {
                        matchingFiles.add(file);
                        break;
                    }
                }
            }
        }

        // Sort files by the full number: image1.jpg, image11.jpg, image12.jpg, image2.jpg, image21.jpg, etc.
        matchingFiles.sort((f1, f2) -> {
            String name1 = f1.getName().toLowerCase();
            String name2 = f2.getName().toLowerCase();

            // Extract the full number from image name (e.g., "image11.jpg" -> 11, "image1.jpg" -> 1)
            int num1 = extractFullNumberFromImageName(name1);
            int num2 = extractFullNumberFromImageName(name2);

            return Integer.compare(num1, num2);
        });

        // Load images
        for (File file : matchingFiles) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    images.add(img);
                    //  System.out.println("✓ Loaded: " + file.getName() + " for line " + lineNumber);
                }
            } catch (Exception e) {
                System.out.println("✗ Error loading image: " + file.getName());
            }
        }

        if (!images.isEmpty()) {
            //   System.out.println("  Total " + images.size() + " image(s) for line " + lineNumber);
        }

        // Cache the loaded images for faster access next time
        lineImageCache.put(lineNumber, images);

        return images;
    }

    // Clear image cache (call when starting new video generation)
    private void clearImageCache() {
        lineImageCache.clear();
        generalImageCache.clear();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    private static class ImagesPerLineManagerGUI extends JDialog {
        // UI Constants for consistent sizing
        private static final int THUMBNAIL_PANEL_SIZE = 85;
        private static final int THUMBNAIL_IMAGE_SIZE = 70;
        private static final int THUMBNAIL_SPACING = 5;
        private static final int LINE_PANEL_HEIGHT = 150;
        private static final int LEFT_PANEL_WIDTH = 250;
        private static final int IMAGES_SCROLL_WIDTH = 500;
        private static final int IMAGES_SCROLL_HEIGHT = 100;
        private static final int DIALOG_WIDTH = 900;
        private static final int DIALOG_HEIGHT = 700;

        // Supported image extensions
        private static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
                ".jpg", ".jpeg", ".jfif", ".png", ".bmp", ".gif", ".webp"
        };

        private VideoConfig config;
        private JPanel linesPanel;
        private java.util.Map<Integer, java.util.List<File>> lineImagesMap = new java.util.HashMap<>();

        /**
         * Check if a file is a supported image format
         */
        private static boolean isSupportedImageFile(File file) {
            if (file == null || !file.isFile()) return false;
            String name = file.getName().toLowerCase();
            for (String ext : SUPPORTED_IMAGE_EXTENSIONS) {
                if (name.endsWith(ext)) return true;
            }
            return false;
        }

        /**
         * Create a scaled thumbnail from a BufferedImage with high quality
         */
        private static BufferedImage createThumbnail(BufferedImage original, int targetSize) {
            if (original == null) return null;

            int width = original.getWidth();
            int height = original.getHeight();

            // Calculate scale to fit within targetSize while maintaining aspect ratio
            double scale = Math.min((double) targetSize / width, (double) targetSize / height);
            int scaledWidth = Math.max(1, (int) (width * scale));
            int scaledHeight = Math.max(1, (int) (height * scale));

            // Use high-quality scaling
            BufferedImage thumbnail = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();

            return thumbnail;
        }

        public ImagesPerLineManagerGUI(JFrame parent, VideoConfig config) {
            super(parent, "Manage Images Per Line", true);
            this.config = config;
            setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
            setLocationRelativeTo(parent);

            initializeGUI();
            loadLinesFromFiles();
        }

        private void initializeGUI() {
            setLayout(new BorderLayout());

            // Top panel with instructions
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel instructionsLabel = new JLabel(
                    "<html><b>Drag and drop images to each line</b><br>" +
                            "Images will be displayed in order for each line during the video</html>"
            );
            instructionsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            topPanel.add(instructionsLabel, BorderLayout.NORTH);

            add(topPanel, BorderLayout.NORTH);

            // Center panel with scrollable lines
            linesPanel = new JPanel();
            linesPanel.setLayout(new BoxLayout(linesPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(linesPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(scrollPane, BorderLayout.CENTER);

            // Bottom panel with buttons
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton saveButton = new JButton("Save & Close");
            saveButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            saveButton.addActionListener(e -> saveAndClose());

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dispose());

            bottomPanel.add(cancelButton);
            bottomPanel.add(saveButton);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        private void loadLinesFromFiles() {
            linesPanel.removeAll();

            // Read Arabic file to get lines
            String[] arabicLines = loadArabicTranslationsFromFile(config.arabicFilePath);

            if (arabicLines == null || arabicLines.length == 0) {
                JLabel noLinesLabel = new JLabel("No lines found in Arabic file!");
                noLinesLabel.setForeground(Color.RED);
                linesPanel.add(noLinesLabel);
                revalidate();
                repaint();
                return;
            }

            // Load existing images from folder
            loadExistingImages();

            // Create a panel for each line
            for (int i = 0; i < arabicLines.length; i++) {
                int lineNumber = i + 1;
                String arabicText = arabicLines[i].trim();

                if (!arabicText.isEmpty()) {
                    java.util.List<File> lineImages = lineImagesMap.get(lineNumber);
                    if (lineImages == null) {
                        lineImages = new java.util.ArrayList<>(); // Create empty list if no images found
                    }
                    System.out.println("Creating LineImagePanel for Line " + lineNumber + " with " + lineImages.size() + " image(s)");
                    LineImagePanel linePanel = new LineImagePanel(lineNumber, arabicText, lineImages);
                    linesPanel.add(linePanel);
                    linesPanel.add(Box.createVerticalStrut(10)); // Spacing between lines
                }
            }

            revalidate();
            repaint();
        }

        private void loadExistingImages() {
            lineImagesMap.clear(); // Clear any existing entries

            File folder = new File(config.imagesPerLineFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                System.out.println("Images per line folder not found: " + config.imagesPerLineFolder);
                return;
            }

            File[] allFiles = folder.listFiles();
            if (allFiles == null) {
                System.out.println("No files found in folder: " + config.imagesPerLineFolder);
                return;
            }

            System.out.println("Scanning folder: " + config.imagesPerLineFolder + " (found " + allFiles.length + " files)");

            // Parse existing images
            int imageCount = 0;
            for (File file : allFiles) {
                System.out.println("  Checking file: " + file.getName());

                if (!isSupportedImageFile(file)) {
                    System.out.println("    -> Not a supported image file");
                    continue;
                }

                String fileName = file.getName().toLowerCase();
                System.out.println("    -> Lowercase name: " + fileName);

                // Extract line number from filename using first digit
                // Supports both formats: image1.jpg -> 1, or just 1.webp -> 1
                int lineNumber = arabicSync.extractLineNumberFromImageName(fileName);
                System.out.println("    -> Extracted line number: " + lineNumber);
                if (lineNumber > 0) {
                    // Check for duplicates before adding
                    java.util.List<File> existingFiles = lineImagesMap.get(lineNumber);
                    if (existingFiles != null) {
                        int imageNumber = arabicSync.extractFullNumberFromImageName(fileName);
                        boolean isDuplicate = false;
                        for (File existingFile : existingFiles) {
                            int existingNumber = arabicSync.extractFullNumberFromImageName(existingFile.getName().toLowerCase());
                            if (existingNumber == imageNumber) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (isDuplicate) {
                            System.out.println("    -> ✗ Skipped duplicate: " + file.getName());
                            continue;
                        }
                    }

                    lineImagesMap.computeIfAbsent(lineNumber, k -> new java.util.ArrayList<>()).add(file);
                    imageCount++;
                    System.out.println("    -> ✓ Assigned " + file.getName() + " to Line " + lineNumber);
                } else {
                    System.out.println("    -> ✗ Skipped " + file.getName() + " (invalid line number: " + lineNumber + ")");
                }
            }

            System.out.println("Total images loaded: " + imageCount + " for " + lineImagesMap.size() + " line(s)");

            // Sort images for each line by the full number (1, 11, 12, 2, 21, etc.)
            for (java.util.List<File> images : lineImagesMap.values()) {
                images.sort((f1, f2) -> {
                    String name1 = f1.getName().toLowerCase();
                    String name2 = f2.getName().toLowerCase();

                    // Extract the full number for sorting
                    int num1 = arabicSync.extractFullNumberFromImageName(name1);
                    int num2 = arabicSync.extractFullNumberFromImageName(name2);

                    return Integer.compare(num1, num2);
                });
            }
        }

        private int extractImageIndex(String filename) {
            // Extract index from filename for sorting images within a line
            // New format: 1_1.jpg -> 1, 1_2.jpg -> 2, 10_3.webp -> 3
            // Old format: image1.jpg -> 1, image11.jpg -> 11, image82.jpg -> 82
            try {
                String name = filename.toLowerCase();
                String numberPart;

                // Check if it starts with "image" (old format)
                if (name.startsWith("image")) {
                    numberPart = name.substring(5); // After "image"
                } else {
                    numberPart = name;
                }

                // Remove extension
                int dotIndex = numberPart.lastIndexOf('.');
                if (dotIndex > 0) {
                    numberPart = numberPart.substring(0, dotIndex);
                }

                // Check for underscore format first (new format: lineNumber_imageIndex)
                if (numberPart.contains("_")) {
                    String imageIndexStr = numberPart.substring(numberPart.indexOf('_') + 1);
                    return Integer.parseInt(imageIndexStr);
                }

                // Remove any dash and suffix (e.g., "1-1" -> "1") for old format
                if (numberPart.contains("-")) {
                    numberPart = numberPart.substring(0, numberPart.indexOf('-'));
                }

                return Integer.parseInt(numberPart);
            } catch (Exception e) {
                return 0;
            }
        }


        private void saveAndClose() {
            File folder = new File(config.imagesPerLineFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            if (lineImagesMap.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No images to save!\nPlease drag and drop images first.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Copy all images to temporary names first to avoid conflicts
            java.util.List<FileCopyTask> copyTasks = new java.util.ArrayList<>();

            for (java.util.Map.Entry<Integer, java.util.List<File>> entry : lineImagesMap.entrySet()) {
                int lineNumber = entry.getKey();
                java.util.List<File> images = entry.getValue();

                for (int i = 0; i < images.size(); i++) {
                    File sourceFile = images.get(i);
                    String extension = getFileExtension(sourceFile);

                    // New format: lineNumber_imageIndex (e.g., "1_1.jpg", "1_2.jpg", "10_1.webp", "10_2.webp")
                    // This avoids ambiguity between line 1 image 1 and line 11 image 1
                    String finalFileName = lineNumber + "_" + (i + 1) + extension;

                    String tempFileName = "temp_" + System.currentTimeMillis() + "_" + lineNumber + "_" + i + extension;
                    copyTasks.add(new FileCopyTask(sourceFile, tempFileName, finalFileName));
                }
            }

            // Copy all files to temporary names
            int successfulCopies = 0;
            for (FileCopyTask task : copyTasks) {
                File tempFile = new File(folder, task.tempName);
                try {
                    java.nio.file.Files.copy(task.source.toPath(), tempFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    successfulCopies++;
                } catch (IOException ex) {
                    System.err.println("Error copying image: " + ex.getMessage());
                }
            }

            // Delete all old image files (both old and new formats)
            File[] existingFiles = folder.listFiles();
            if (existingFiles != null) {
                for (File file : existingFiles) {
                    String fileName = file.getName().toLowerCase();
                    // Skip temp files
                    if (fileName.startsWith("temp_")) {
                        continue;
                    }
                    // Delete old format: image1.jpg, image11.jpg, etc.
                    if (fileName.startsWith("image")) {
                        file.delete();
                        continue;
                    }
                    // Delete new format: 1_1.jpg, 10_2.webp, etc. (number_number.ext)
                    // Also delete old numeric format: 1.jpg, 11.jpg, etc.
                    String nameWithoutExt = fileName;
                    int dotIdx = fileName.lastIndexOf('.');
                    if (dotIdx > 0) {
                        nameWithoutExt = fileName.substring(0, dotIdx);
                    }
                    // Check if it matches number_number or just number pattern
                    if (nameWithoutExt.matches("\\d+_\\d+") || nameWithoutExt.matches("\\d+")) {
                        file.delete();
                    }
                }
            }

            // Rename all temp files to final names
            int renamedCount = 0;
            for (FileCopyTask task : copyTasks) {
                File tempFile = new File(folder, task.tempName);
                File finalFile = new File(folder, task.finalName);
                if (tempFile.exists() && tempFile.renameTo(finalFile)) {
                    renamedCount++;
                }
            }

            if (renamedCount > 0) {
                JOptionPane.showMessageDialog(this,
                        "Images saved successfully!\n" +
                                "Total lines: " + lineImagesMap.size() + "\n" +
                                "Total images: " + renamedCount + "\n" +
                                "Location: " + folder.getAbsolutePath(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No images were saved!\nPlease check the console for errors.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }

            dispose();
        }

















        // Helper class for copy tasks
        private static class FileCopyTask {
            File source;
            String tempName;
            String finalName;

            FileCopyTask(File src, String temp, String fin) {
                this.source = src;
                this.tempName = temp;
                this.finalName = fin;
            }
        }








        private String getFileExtension(File file) {
            String name = file.getName();
            int lastDot = name.lastIndexOf('.');
            return (lastDot > 0) ? name.substring(lastDot) : ".jpg";
        }

        // Inner class for each line panel





        private class LineImagePanel extends JPanel {
            private int lineNumber;
            private String arabicText;
            private JPanel imagesContainer;
            private java.util.List<ImageInfo> images = new java.util.ArrayList<>();

            /**
             * Check if an image with the same number already exists
             * @param fileName the filename to check
             * @return true if a duplicate exists, false otherwise
             */
            private boolean hasImageWithNumber(String fileName) {
                int newImageNumber = arabicSync.extractFullNumberFromImageName(fileName.toLowerCase());
                if (newImageNumber <= 0) return false;

                for (ImageInfo existing : images) {
                    int existingNumber = arabicSync.extractFullNumberFromImageName(existing.originalFile.getName().toLowerCase());
                    if (existingNumber == newImageNumber) {
                        return true;
                    }
                }
                return false;
            }

            // Helper class to store image info
            private class ImageInfo {
                File originalFile;
                BufferedImage thumbnail;

                ImageInfo(File file, BufferedImage thumb) {
                    this.originalFile = file;
                    this.thumbnail = thumb;
                }
            }

            public LineImagePanel(int lineNumber, String arabicText, java.util.List<File> existingImages) {
                this.lineNumber = lineNumber;
                this.arabicText = arabicText;

                // Load existing images from the images_per_line folder
                if (existingImages != null && !existingImages.isEmpty()) {
                    System.out.println("LineImagePanel for Line " + lineNumber + ": Loading " + existingImages.size() + " image(s)");
                    for (File file : existingImages) {
                        // Check for duplicates before adding
                        if (hasImageWithNumber(file.getName())) {
                            System.out.println("  -> Skipped duplicate image: " + file.getName());
                            continue;
                        }

                        try {
                            BufferedImage img = ImageIO.read(file);
                            if (img != null) {
                                // Create thumbnail for display
                                BufferedImage thumbnail = createThumbnail(img, THUMBNAIL_IMAGE_SIZE);
                                if (thumbnail == null) {
                                    thumbnail = img; // Fallback to original if thumbnail creation fails
                                }
                                images.add(new ImageInfo(file, thumbnail));
                                System.out.println("  -> Loaded image: " + file.getName());
                            }
                        } catch (IOException e) {
                            System.err.println("Error loading image: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                    // Sort images by their full number (1, 11, 12, 2, 21, etc.)
                    images.sort((img1, img2) -> {
                        int num1 = extractImageIndex(img1.originalFile.getName());
                        int num2 = extractImageIndex(img2.originalFile.getName());
                        return Integer.compare(num1, num2);
                    });
                    updateImagesMap();
                    System.out.println("LineImagePanel for Line " + lineNumber + ": " + images.size() + " image(s) ready to display");
                } else {
                    System.out.println("LineImagePanel for Line " + lineNumber + ": No existing images provided");
                }

                setLayout(new BorderLayout(10, 10));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, LINE_PANEL_HEIGHT));

                // Left side - Line info
                JPanel leftPanel = new JPanel(new BorderLayout());
                leftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, LINE_PANEL_HEIGHT - 30));

                JLabel lineLabel = new JLabel("Line " + lineNumber);
                lineLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

                JTextArea textArea = new JTextArea(arabicText);
                textArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 22));
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setBackground(getBackground());

                leftPanel.add(lineLabel, BorderLayout.NORTH);
                leftPanel.add(textArea, BorderLayout.CENTER);

                add(leftPanel, BorderLayout.WEST);

                // Right side - Images container with drag-drop
                JPanel rightPanel = new JPanel(new BorderLayout());

                JPanel topRightPanel = new JPanel(new BorderLayout());
                JLabel imagesLabel = new JLabel("Images (drag files or thumbnails here):");
                imagesLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                topRightPanel.add(imagesLabel, BorderLayout.WEST);

                JButton browseButton = new JButton("📁 Browse...");
                browseButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                browseButton.setToolTipText("Browse and select images from your computer");
                browseButton.addActionListener(e -> showBrowseDialog());
                topRightPanel.add(browseButton, BorderLayout.EAST);

                rightPanel.add(topRightPanel, BorderLayout.NORTH);

                imagesContainer = new JPanel();
                imagesContainer.setLayout(new FlowLayout(FlowLayout.LEFT, THUMBNAIL_SPACING, THUMBNAIL_SPACING));
                imagesContainer.setBackground(new Color(240, 240, 240));
                imagesContainer.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true));

                // Enable drag and drop for external files
                setupDragAndDrop(imagesContainer);

                // Display existing images with index
                System.out.println("LineImagePanel for Line " + lineNumber + ": Displaying " + images.size() + " image(s)");
                for (int i = 0; i < images.size(); i++) {
                    addImageThumbnailDisplay(images.get(i), i);
                    System.out.println("  -> Added thumbnail " + (i + 1) + ": " + images.get(i).originalFile.getName());
                }

                // Force repaint to ensure images are visible
                imagesContainer.revalidate();
                imagesContainer.repaint();

                JScrollPane scrollPane = new JScrollPane(imagesContainer);
                scrollPane.setPreferredSize(new Dimension(IMAGES_SCROLL_WIDTH, IMAGES_SCROLL_HEIGHT));
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

                rightPanel.add(scrollPane, BorderLayout.CENTER);

                add(rightPanel, BorderLayout.CENTER);
            }

            private void showBrowseDialog() {
                JDialog browseDialog = new JDialog((JDialog) SwingUtilities.getWindowAncestor(this),
                        "Select Images for Line " + lineNumber, true);
                browseDialog.setSize(1000, 700);
                browseDialog.setLocationRelativeTo(this);
                browseDialog.setLayout(new BorderLayout());

                // File chooser with large preview
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                // Image filter using shared extensions
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) return true;
                        return isSupportedImageFile(f);
                    }

                    @Override
                    public String getDescription() {
                        return "Image Files (*.jpg, *.jpeg, *.jfif, *.png, *.bmp, *.gif, *.webp)";
                    }
                });

                // Large preview panel
                JPanel previewPanel = new JPanel(new BorderLayout());
                previewPanel.setPreferredSize(new Dimension(400, 600));
                previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

                JLabel previewLabel = new JLabel("Select an image to preview", JLabel.CENTER);
                previewLabel.setVerticalAlignment(JLabel.CENTER);
                JScrollPane previewScroll = new JScrollPane(previewLabel);
                previewPanel.add(previewScroll, BorderLayout.CENTER);

                // Update preview when selection changes
                fileChooser.addPropertyChangeListener(evt -> {
                    if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                        File file = (File) evt.getNewValue();
                        if (file != null && file.isFile()) {
                            try {
                                BufferedImage img = ImageIO.read(file);
                                if (img != null) {
                                    // Scale image to fit preview while maintaining aspect ratio
                                    int maxWidth = 380;
                                    int maxHeight = 550;

                                    double scale = Math.min(
                                            (double) maxWidth / img.getWidth(),
                                            (double) maxHeight / img.getHeight()
                                    );

                                    int scaledWidth = (int) (img.getWidth() * scale);
                                    int scaledHeight = (int) (img.getHeight() * scale);

                                    Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                                    previewLabel.setIcon(new ImageIcon(scaledImg));
                                    previewLabel.setText("");

                                    // Show image info
                                    previewPanel.setBorder(BorderFactory.createTitledBorder(
                                            "Preview - " + file.getName() + " (" + img.getWidth() + "x" + img.getHeight() + ")"
                                    ));
                                }
                            } catch (IOException e) {
                                previewLabel.setIcon(null);
                                previewLabel.setText("Cannot preview this file");
                            }
                        }
                    }
                });

                // Split pane: file chooser on left, preview on right
                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileChooser, previewPanel);
                splitPane.setDividerLocation(580);
                splitPane.setResizeWeight(0.6);

                browseDialog.add(splitPane, BorderLayout.CENTER);

                // Buttons panel
                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

                JButton addButton = new JButton("Add Selected Images");
                addButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                addButton.addActionListener(e -> {
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    if (selectedFiles != null && selectedFiles.length > 0) {
                        int addedCount = 0;
                        int skippedCount = 0;
                        for (File file : selectedFiles) {
                            if (isSupportedImageFile(file)) {
                                // Check for duplicates before adding
                                if (hasImageWithNumber(file.getName())) {
                                    skippedCount++;
                                    continue;
                                }

                                try {
                                    BufferedImage img = ImageIO.read(file);
                                    if (img != null) {
                                        BufferedImage thumbnail = createThumbnail(img, THUMBNAIL_IMAGE_SIZE);
                                        if (thumbnail == null) {
                                            thumbnail = img;
                                        }
                                        ImageInfo imgInfo = new ImageInfo(file, thumbnail);
                                        images.add(imgInfo);
                                        addImageThumbnailDisplay(imgInfo, images.size() - 1);
                                        addedCount++;
                                    }
                                } catch (IOException ex) {
                                    System.err.println("Error loading image: " + file.getName());
                                }
                            }
                        }

                        String message;
                        if (addedCount > 0 && skippedCount > 0) {
                            message = addedCount + " image(s) added successfully!\n" + skippedCount + " duplicate(s) skipped.";
                        } else if (addedCount > 0) {
                            message = addedCount + " image(s) added successfully!";
                        } else if (skippedCount > 0) {
                            message = "All selected images are duplicates and were skipped.";
                        } else {
                            message = "No valid images were added!";
                        }

                        if (addedCount > 0 || skippedCount > 0) {
                            updateImagesMap();
                            JOptionPane.showMessageDialog(browseDialog, message,
                                    skippedCount > 0 ? "Info" : "Success",
                                    skippedCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                            if (addedCount > 0) {
                                browseDialog.dispose();
                            }
                        } else {
                            JOptionPane.showMessageDialog(browseDialog, message,
                                    "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(browseDialog,
                                "Please select at least one image!",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                });

                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(e -> browseDialog.dispose());

                buttonsPanel.add(cancelButton);
                buttonsPanel.add(addButton);

                browseDialog.add(buttonsPanel, BorderLayout.SOUTH);
                browseDialog.setVisible(true);
            }


            private void setupDragAndDrop(JPanel panel) {
                panel.setTransferHandler(new TransferHandler() {
                    @Override
                    public boolean canImport(TransferSupport support) {
                        return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    }





                    @Override
                    public boolean importData(TransferSupport support) {
                        if (!canImport(support)) {
                            //System.out.println("DEBUG: canImport returned false");
                            return false;
                        }

                        try {
                            @SuppressWarnings("unchecked")
                            java.util.List<File> files = (java.util.List<File>) support.getTransferable()
                                    .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);

                            //System.out.println("DEBUG: Dropped " + files.size() + " file(s)");

                            for (File file : files) {
                                if (isSupportedImageFile(file)) {
                                    // Check for duplicates before adding
                                    if (hasImageWithNumber(file.getName())) {
                                        continue; // Skip duplicate
                                    }

                                    try {
                                        BufferedImage img = ImageIO.read(file);
                                        if (img != null) {
                                            BufferedImage thumbnail = createThumbnail(img, THUMBNAIL_IMAGE_SIZE);
                                            if (thumbnail == null) {
                                                thumbnail = img;
                                            }
                                            ImageInfo imgInfo = new ImageInfo(file, thumbnail);
                                            images.add(imgInfo);
                                            addImageThumbnailDisplay(imgInfo, images.size() - 1);
                                            updateImagesMap();
                                        }
                                    } catch (IOException e) {
                                        System.err.println("Error loading dropped image: " + file.getName());
                                    }
                                }
                            }

                            return true;
                        } catch (Exception e) {
                            //System.out.println("DEBUG: ❌ Exception in importData:");
                            e.printStackTrace();
                            return false;
                        }
                    }




                });





            }


            /**
             * Add an image thumbnail to the display with its index
             * @param imgInfo the image information
             * @param index the position index (0-based, -1 for auto-append)
             */
            private void addImageThumbnailDisplay(ImageInfo imgInfo, int index) {
                JPanel thumbPanel = new JPanel(new BorderLayout());
                thumbPanel.setPreferredSize(new Dimension(THUMBNAIL_PANEL_SIZE, THUMBNAIL_PANEL_SIZE));
                thumbPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                thumbPanel.setBackground(Color.WHITE);

                // Create thumbnail using the improved method
                BufferedImage scaledThumb = createThumbnail(imgInfo.thumbnail, THUMBNAIL_IMAGE_SIZE);
                JLabel imgLabel = new JLabel(scaledThumb != null ? new ImageIcon(scaledThumb) : null);
                imgLabel.setHorizontalAlignment(JLabel.CENTER);
                imgLabel.setVerticalAlignment(JLabel.CENTER);

                // Add tooltip with file info
                String tooltip = buildImageTooltip(imgInfo);
                imgLabel.setToolTipText(tooltip);
                thumbPanel.setToolTipText(tooltip);

                // Create a layered panel to show index badge
                JPanel imagePanel = new JPanel(new BorderLayout());
                imagePanel.setOpaque(false);
                imagePanel.add(imgLabel, BorderLayout.CENTER);

                // Add index badge in top-left corner
                int displayIndex = (index >= 0) ? index : images.indexOf(imgInfo);
                JLabel indexLabel = new JLabel(String.valueOf(displayIndex + 1));
                indexLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                indexLabel.setForeground(Color.WHITE);
                indexLabel.setOpaque(true);
                indexLabel.setBackground(new Color(0, 100, 200));
                indexLabel.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                indexLabel.setHorizontalAlignment(JLabel.CENTER);

                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
                topPanel.setOpaque(false);
                topPanel.add(indexLabel);
                imagePanel.add(topPanel, BorderLayout.NORTH);

                thumbPanel.add(imagePanel, BorderLayout.CENTER);

                // Enable dragging this thumbnail
                setupThumbnailDragAndDrop(thumbPanel, imgInfo);

                // Add remove button
                JButton removeBtn = new JButton("×");
                removeBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                removeBtn.setMargin(new Insets(0, 4, 0, 4));
                removeBtn.setToolTipText("Remove this image");
                removeBtn.addActionListener(e -> {
                    images.remove(imgInfo);
                    imagesContainer.remove(thumbPanel);
                    refreshThumbnails(); // Refresh to update indices
                    updateImagesMap();
                });

                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                bottomPanel.setOpaque(false);
                bottomPanel.add(removeBtn);
                thumbPanel.add(bottomPanel, BorderLayout.SOUTH);

                imagesContainer.add(thumbPanel);
                imagesContainer.revalidate();
                imagesContainer.repaint();
            }

            /**
             * Build tooltip text for an image
             */
            private String buildImageTooltip(ImageInfo imgInfo) {
                StringBuilder tooltip = new StringBuilder("<html>");
                tooltip.append("<b>").append(imgInfo.originalFile.getName()).append("</b>");
                if (imgInfo.thumbnail != null) {
                    tooltip.append("<br>Size: ").append(imgInfo.thumbnail.getWidth())
                            .append(" x ").append(imgInfo.thumbnail.getHeight());
                }
                long fileSize = imgInfo.originalFile.length();
                if (fileSize > 0) {
                    String sizeStr = fileSize > 1024 * 1024
                            ? String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
                            : String.format("%.1f KB", fileSize / 1024.0);
                    tooltip.append("<br>File: ").append(sizeStr);
                }
                tooltip.append("</html>");
                return tooltip.toString();
            }

            private void setupThumbnailDragAndDrop(JPanel thumbPanel, ImageInfo imgInfo) {
                // Make thumbnail draggable
                thumbPanel.setTransferHandler(new TransferHandler() {
                    @Override
                    public int getSourceActions(JComponent c) {
                        return MOVE;
                    }

                    @Override
                    protected Transferable createTransferable(JComponent c) {
                        return new ThumbnailTransferable(imgInfo, LineImagePanel.this);
                    }

                    @Override
                    protected void exportDone(JComponent source, Transferable data, int action) {
                        // Cleanup if needed
                    }

                    @Override
                    public boolean canImport(TransferSupport support) {
                        return support.isDataFlavorSupported(ThumbnailTransferable.THUMBNAIL_FLAVOR);
                    }

                    @Override
                    public boolean importData(TransferSupport support) {
                        if (!canImport(support)) {
                            return false;
                        }

                        try {
                            ThumbnailTransferable.ThumbnailData data =
                                    (ThumbnailTransferable.ThumbnailData) support.getTransferable()
                                            .getTransferData(ThumbnailTransferable.THUMBNAIL_FLAVOR);

                            ImageInfo droppedImage = data.imageInfo;
                            LineImagePanel sourcePanel = data.sourcePanel;

                            // Get drop location
                            DropLocation dropLocation = support.getDropLocation();
                            Point dropPoint = dropLocation.getDropPoint();

                            // Find which thumbnail was dropped on
                            int targetIndex = -1;
                            Component[] components = imagesContainer.getComponents();
                            for (int i = 0; i < components.length; i++) {
                                Component comp = components[i];
                                if (comp.getBounds().contains(dropPoint)) {
                                    targetIndex = i;
                                    break;
                                }
                            }

                            // If dropped in empty space, add to end
                            if (targetIndex == -1) {
                                targetIndex = images.size();
                            }

                            // Check for duplicate before adding
                            if (hasImageWithNumber(droppedImage.originalFile.getName())) {
                                // Don't remove from source if it's a duplicate
                                JOptionPane.showMessageDialog(LineImagePanel.this,
                                        "An image with the same number already exists in this line!",
                                        "Duplicate Image", JOptionPane.WARNING_MESSAGE);
                                return false;
                            }

                            // Remove from source panel
                            sourcePanel.removeImage(droppedImage);

                            // Add to this panel at target position
                            if (targetIndex > images.size()) {
                                targetIndex = images.size();
                            }
                            images.add(targetIndex, droppedImage);

                            // Refresh display
                            // Refresh both panels
                            sourcePanel.refreshThumbnails();
                            if (sourcePanel != LineImagePanel.this) {
                                refreshThumbnails();
                            }

// Update both maps AFTER refresh
                            sourcePanel.updateImagesMap();
                            updateImagesMap();

                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                });

                // Enable drag gesture
                MouseAdapter dragGestureAdapter = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        JComponent comp = (JComponent) e.getSource();
                        TransferHandler handler = comp.getTransferHandler();
                        handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                    }
                };

                thumbPanel.addMouseListener(dragGestureAdapter);
                thumbPanel.addMouseMotionListener(dragGestureAdapter);
            }

            // Remove image from this panel
            public void removeImage(ImageInfo imgInfo) {
                images.remove(imgInfo);
                refreshThumbnails();
                updateImagesMap();
            }

            /**
             * Refresh all thumbnails with updated indices
             */
            private void refreshThumbnails() {
                imagesContainer.removeAll();
                for (int i = 0; i < images.size(); i++) {
                    addImageThumbnailDisplay(images.get(i), i);
                }
                imagesContainer.revalidate();
                imagesContainer.repaint();
                updateImagesMap();
            }

            private void updateImagesMap() {
                java.util.List<File> fileList = new java.util.ArrayList<>();
                for (ImageInfo imgInfo : images) {
                    fileList.add(imgInfo.originalFile);
                }
                lineImagesMap.put(lineNumber, fileList);
            }
        }

        // Custom Transferable for thumbnail drag-and-drop
        private static class ThumbnailTransferable implements java.awt.datatransfer.Transferable {
            public static final java.awt.datatransfer.DataFlavor THUMBNAIL_FLAVOR =
                    new java.awt.datatransfer.DataFlavor(ThumbnailData.class, "Thumbnail");

            private ThumbnailData data;

            public static class ThumbnailData {
                LineImagePanel.ImageInfo imageInfo;
                LineImagePanel sourcePanel;

                ThumbnailData(LineImagePanel.ImageInfo img, LineImagePanel panel) {
                    this.imageInfo = img;
                    this.sourcePanel = panel;
                }
            }

            public ThumbnailTransferable(LineImagePanel.ImageInfo imageInfo, LineImagePanel sourcePanel) {
                this.data = new ThumbnailData(imageInfo, sourcePanel);
            }

            @Override
            public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                return new java.awt.datatransfer.DataFlavor[]{THUMBNAIL_FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
                return THUMBNAIL_FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) {
                if (isDataFlavorSupported(flavor)) {
                    return data;
                }
                return null;
            }
        }

        // Custom Transferable for thumbnail drag-and-drop








    }



//////////////////////////////////////////////////////////////////////////////////////////////////////






























/////////////////////////////////////////////////hona 2410 2025

    /**
     * Effect data for serialization
     */
    /**
     * Effect data for serialization
     */
    private static class ImageEffectData {
        int lineNumber;
        String imageFileName;  // e.g., "image1.jpg", "image1-1.jpg", "image1-2.jpg"
        java.util.List<EffectEntry> effects;

        ImageEffectData() {
            effects = new java.util.ArrayList<>();
        }
    }

    private static class EffectEntry {
        String type;  // "text", "zoom", "dot"
        String text;  // Only for text type
        int x;
        int y;
        int size;     // Effect size (zoom radius, etc.)
        int fontSize; // Font size for text captions

        EffectEntry(String type, String text, int x, int y) {
            this.type = type;
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = 80; // Default size
            this.fontSize = 40; // Default font size
        }

        EffectEntry(String type, String text, int x, int y, int size, int fontSize) {
            this.type = type;
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.fontSize = fontSize;
        }
    }

    /**
     * Container for all image effects
     */

    /**
     * Container for all image effects
     */
    private static class ImageEffectsConfig {
        // Key format: "lineNumber:imageFileName"
        java.util.Map<String, ImageEffectData> imageEffects;

        ImageEffectsConfig() {
            imageEffects = new java.util.HashMap<>();
        }

        void saveToFile(String filePath) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                for (java.util.Map.Entry<String, ImageEffectData> entry : imageEffects.entrySet()) {
                    ImageEffectData data = entry.getValue();
                    writer.println("LINE:" + data.lineNumber);
                    writer.println("IMAGE:" + data.imageFileName);
                    for (EffectEntry effect : data.effects) {
                        writer.println("EFFECT:" + effect.type + "," +
                                (effect.text != null ? effect.text : "") + "," +
                                effect.x + "," + effect.y + "," + effect.size + "," + effect.fontSize);
                    }
                    writer.println("---");
                }
                System.out.println("✓ Effects saved to: " + filePath);
            } catch (IOException e) {
                System.out.println("✗ Error saving effects: " + e.getMessage());
            }
        }

        static ImageEffectsConfig loadFromFile(String filePath) {
            ImageEffectsConfig config = new ImageEffectsConfig();
            File file = new File(filePath);
            if (!file.exists()) return config;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                ImageEffectData currentData = null;
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("LINE:")) {
                        currentData = new ImageEffectData();
                        currentData.lineNumber = Integer.parseInt(line.substring(5));
                    } else if (line.startsWith("IMAGE:") && currentData != null) {
                        currentData.imageFileName = line.substring(6);
                    } else if (line.startsWith("EFFECT:") && currentData != null) {
                        String[] parts = line.substring(7).split(",");
                        if (parts.length >= 4) {
                            String type = parts[0];
                            String text = parts[1].isEmpty() ? null : parts[1];
                            int x = Integer.parseInt(parts[2]);
                            int y = Integer.parseInt(parts[3]);
                            EffectEntry effect = new EffectEntry(type, text, x, y);
                            if (parts.length > 4) {
                                effect.size = Integer.parseInt(parts[4]);
                            }
                            if (parts.length > 5) {
                                effect.fontSize = Integer.parseInt(parts[5]);
                            }
                            currentData.effects.add(effect);
                        }
                    } else if (line.equals("---") && currentData != null) {
                        String key = currentData.lineNumber + ":" + currentData.imageFileName;
                        config.imageEffects.put(key, currentData);
                        currentData = null;
                    }
                }
                System.out.println("✓ Effects loaded from: " + filePath);
            } catch (IOException e) {
                System.out.println("✗ Error loading effects: " + e.getMessage());
            }

            return config;
        }

        // Helper method to get effects for specific image
        ImageEffectData getEffectsForImage(int lineNumber, String imageFileName) {
            String key = lineNumber + ":" + imageFileName;
            return imageEffects.get(key);
        }
    }





    /**
     * Visual Image Effects Editor
     */
    private static class ImageEffectsEditorGUI extends JFrame {
        private VideoConfig config;
        private ImageEffectsConfig effectsConfig;
        private String effectsFilePath = "image_effects.txt";
        private JComboBox<String> imageSelectCombo;  // ADD THIS FIELD
        private java.util.List<String> currentLineImageFiles;  // ADD THIS FIELD
        private JList<String> linesList;
        private DefaultListModel<String> linesListModel;
        private ImagePanel imagePanel;
        private JList<String> effectsList;
        private DefaultListModel<String> effectsListModel;

        private JComboBox<String> effectTypeCombo;
        private JTextField textInputField;
        private JSpinner sizeSpinner;
        private JSlider fontSizeSlider;
        private JLabel fontSizeValueLabel;

        private int currentLineNumber = -1;
        private String currentImageFile = null;
        private String[] arabicLines;

        public ImageEffectsEditorGUI(VideoConfig config) {
            this.config = config;
            this.effectsConfig = ImageEffectsConfig.loadFromFile(effectsFilePath);

            setTitle("Image Effects Editor - Click on image to add effects");
            setSize(1400, 900);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            loadArabicLines();
            initializeGUI();
        }

        private void loadArabicLines() {
            try {
                List<String> lines = new ArrayList<>();
                File arabicFile = new File(config.arabicFilePath);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(arabicFile), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Remove effect syntax if present
                        if (line.contains("--")) {
                            line = line.split("--")[0].trim();
                        }
                        lines.add(line.trim());
                    }
                }

                arabicLines = lines.toArray(new String[0]);
                System.out.println("✓ Loaded " + arabicLines.length + " lines");
            } catch (Exception e) {
                System.out.println("✗ Error loading Arabic lines: " + e.getMessage());
                arabicLines = new String[0];
            }
        }

        private void initializeGUI() {
            setLayout(new BorderLayout(10, 10));

            // Left panel - Lines list
            JPanel leftPanel = createLinesPanel();
            leftPanel.setPreferredSize(new Dimension(300, 800));
            add(leftPanel, BorderLayout.WEST);

            // Center panel - Image with click detection
            imagePanel = new ImagePanel();
            add(imagePanel, BorderLayout.CENTER);

            // Right panel - Effects list and controls
            JPanel rightPanel = createEffectsPanel();
            rightPanel.setPreferredSize(new Dimension(350, 800));
            add(rightPanel, BorderLayout.EAST);

            // Bottom panel - Save/Load buttons
            JPanel bottomPanel = createBottomPanel();
            add(bottomPanel, BorderLayout.SOUTH);
        }

        private JPanel createLinesPanel() {
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createTitledBorder("Lines from quoteAR.txt"));

            // Lines list (existing code)
            linesListModel = new DefaultListModel<>();
            for (int i = 0; i < arabicLines.length; i++) {
                String lineText = "Line " + (i + 1) + ": " + arabicLines[i];
                if (lineText.length() > 50) {
                    lineText = lineText.substring(0, 47) + "...";
                }
                linesListModel.addElement(lineText);
            }

            linesList = new JList<>(linesListModel);
            linesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            linesList.setFont(new Font("Arial Unicode MS", Font.PLAIN, 12));
            linesList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = linesList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        loadLineImages(selectedIndex + 1);
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(linesList);
            panel.add(scrollPane, BorderLayout.CENTER);

            // ADD IMAGE SELECTOR PANEL
            JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel imageLabel = new JLabel("Select Image:");
            imageLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            bottomPanel.add(imageLabel, BorderLayout.NORTH);

            imageSelectCombo = new JComboBox<>();
            imageSelectCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            imageSelectCombo.addActionListener(e -> {
                int selectedIndex = imageSelectCombo.getSelectedIndex();
                if (selectedIndex >= 0) {
                    loadSpecificImage(selectedIndex);
                }
            });
            bottomPanel.add(imageSelectCombo, BorderLayout.CENTER);

            JLabel instructionLabel = new JLabel("<html><center>Select line, then<br>choose which image<br>to add effects to</center></html>");
            instructionLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
            bottomPanel.add(instructionLabel, BorderLayout.SOUTH);

            panel.add(bottomPanel, BorderLayout.SOUTH);

            return panel;
        }




        private void loadLineImages(int lineNumber) {
            currentLineNumber = lineNumber;

            // Load ALL images for this line
            java.util.List<BufferedImage> images = loadImagesForLine(lineNumber);
            currentLineImageFiles = new java.util.ArrayList<>();

            if (!images.isEmpty()) {
                // Find all image filenames for this line
                File folder = new File(config.imagesPerLineFolder);
                File[] files = folder.listFiles();

                if (files != null) {
                    // Collect all images for this line
                    java.util.List<String> imageNames = new java.util.ArrayList<>();

                    for (File file : files) {
                        String name = file.getName();
                        int extractedLineNumber = arabicSync.extractLineNumberFromImageName(name);
                        if (extractedLineNumber == lineNumber) {
                            imageNames.add(name);
                        }
                    }

                    // Sort them properly by full number
                    imageNames.sort((n1, n2) -> {
                        int num1 = arabicSync.extractFullNumberFromImageName(n1);
                        int num2 = arabicSync.extractFullNumberFromImageName(n2);
                        return Integer.compare(num1, num2);
                    });

                    currentLineImageFiles = imageNames;

                    // Update image selector dropdown
                    if (!imageNames.isEmpty()) {
                        imageSelectCombo.removeAllItems();
                        for (int i = 0; i < imageNames.size(); i++) {
                            imageSelectCombo.addItem("Image " + (i + 1) + ": " + imageNames.get(i));
                        }

                        // Load first image by default
                        loadSpecificImage(0);
                    }
                }
            } else {
                imagePanel.setImage(null, lineNumber, null);
                JOptionPane.showMessageDialog(this,
                        "No image found for Line " + lineNumber,
                        "No Image", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void loadSpecificImage(int imageIndex) {
            if (imageIndex < 0 || imageIndex >= currentLineImageFiles.size()) return;

            String imageFileName = currentLineImageFiles.get(imageIndex);
            File imageFile = new File(config.imagesPerLineFolder, imageFileName);

            try {
                BufferedImage img = ImageIO.read(imageFile);
                if (img != null) {
                    currentImageFile = imageFileName;
                    imagePanel.setImage(img, currentLineNumber, imageFileName);
                    loadEffectsForCurrentImage();
                }
            } catch (IOException e) {
                System.out.println("Error loading image: " + imageFileName);
            }
        }

        private void loadEffectsForCurrentImage() {
            effectsListModel.clear();

            if (currentLineNumber > 0 && currentImageFile != null) {
                ImageEffectData data = effectsConfig.getEffectsForImage(currentLineNumber, currentImageFile);

                if (data != null) {
                    imagePanel.setEffects(data.effects);

                    for (EffectEntry effect : data.effects) {
                        String displayText = String.format("%s at (%d, %d)",
                                effect.type.toUpperCase(), effect.x, effect.y);
                        if (effect.text != null && !effect.text.isEmpty()) {
                            displayText = effect.text + " - " + displayText;
                        }
                        effectsListModel.addElement(displayText);
                    }
                } else {
                    imagePanel.setEffects(new java.util.ArrayList<>());
                }
            }
        }



        private JPanel createEffectsPanel() {
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createTitledBorder("Effects on Current Image"));

            // Effects list
            effectsListModel = new DefaultListModel<>();
            effectsList = new JList<>(effectsListModel);
            effectsList.setFont(new Font("Arial Unicode MS", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(effectsList);
            panel.add(scrollPane, BorderLayout.CENTER);

            // Controls panel
            JPanel controlsPanel = new JPanel();
            controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
            controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Effect type selection
            JLabel typeLabel = new JLabel("Effect Type:");
            typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(typeLabel);

            effectTypeCombo = new JComboBox<>(new String[]{
                    "Text Caption",
                    "Zoom",
                    "Blinking Dot",
                    "Arrow Pointer",
                    "Circle Highlight",
                    "Spotlight",
                    "Star Burst",
                    "Ripple Wave",
                    "Focus Frame",
                    "Underline Box",
                    "Glow Pulse"
            });
            effectTypeCombo.setMaximumSize(new Dimension(300, 30));
            effectTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
            effectTypeCombo.addActionListener(e -> updateControlsVisibility());
            controlsPanel.add(effectTypeCombo);
            controlsPanel.add(Box.createVerticalStrut(10));

            // Text input (for text captions)
            JLabel textLabel = new JLabel("Caption Text:");
            textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(textLabel);

            textInputField = new JTextField();
            textInputField.setFont(new Font("Arial Unicode MS", Font.PLAIN, 14));
            textInputField.setMaximumSize(new Dimension(300, 30));
            textInputField.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(textInputField);
            controlsPanel.add(Box.createVerticalStrut(10));

            // Size control
            JLabel sizeLabel = new JLabel("Effect Size:");
            sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(sizeLabel);

            sizeSpinner = new JSpinner(new SpinnerNumberModel(80, 20, 200, 10));
            sizeSpinner.setMaximumSize(new Dimension(100, 30));
            sizeSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(sizeSpinner);
            controlsPanel.add(Box.createVerticalStrut(10));

            // Font Size slider (for text captions)
            JLabel fontSizeLabel = new JLabel("Caption Font Size:");
            fontSizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            controlsPanel.add(fontSizeLabel);

            JPanel fontSizePanel = new JPanel(new BorderLayout(5, 0));
            fontSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            fontSizePanel.setMaximumSize(new Dimension(300, 30));

            fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 20, 100, 40);
            fontSizeSlider.setMajorTickSpacing(20);
            fontSizeSlider.setMinorTickSpacing(5);
            fontSizeSlider.setPaintTicks(true);

            fontSizeValueLabel = new JLabel("40 px");
            fontSizeValueLabel.setPreferredSize(new Dimension(50, 20));

            fontSizeSlider.addChangeListener(e -> {
                fontSizeValueLabel.setText(fontSizeSlider.getValue() + " px");
            });

            fontSizePanel.add(fontSizeSlider, BorderLayout.CENTER);
            fontSizePanel.add(fontSizeValueLabel, BorderLayout.EAST);
            controlsPanel.add(fontSizePanel);
            controlsPanel.add(Box.createVerticalStrut(15));

            // Instruction label
            JLabel instructionLabel = new JLabel("<html><b>Click on image</b> to place effect<br>at that position</html>");
            instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            instructionLabel.setForeground(new Color(0, 100, 0));
            controlsPanel.add(instructionLabel);
            controlsPanel.add(Box.createVerticalStrut(15));

            // Remove button
            JButton removeButton = new JButton("Remove Selected Effect");
            removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            removeButton.addActionListener(e -> removeSelectedEffect());
            controlsPanel.add(removeButton);

            controlsPanel.add(Box.createVerticalStrut(10));

            // Clear all button
            JButton clearButton = new JButton("Clear All Effects");
            clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            clearButton.addActionListener(e -> clearAllEffects());
            controlsPanel.add(clearButton);

            panel.add(controlsPanel, BorderLayout.SOUTH);

            updateControlsVisibility();

            return panel;
        }

        private void updateControlsVisibility() {
            boolean isText = effectTypeCombo.getSelectedIndex() == 0;
            textInputField.setEnabled(isText);
        }

        private JPanel createBottomPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

            JButton saveButton = new JButton("💾 Save Effects");
            saveButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            saveButton.addActionListener(e -> saveEffects());

            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dispose());

            panel.add(saveButton);
            panel.add(closeButton);

            return panel;
        }


        private void addEffectAtPosition(int x, int y) {
            if (currentLineNumber < 0 || currentImageFile == null) {
                JOptionPane.showMessageDialog(this, "Please select a line and image first!",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String effectType;
            String text = null;

            switch (effectTypeCombo.getSelectedIndex()) {
                case 0: // Text Caption
                    text = textInputField.getText().trim();
                    if (text.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Please enter caption text!",
                                "No Text", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    effectType = "text";
                    break;
                case 1: // Zoom
                    effectType = "zoom";
                    break;
                case 2: // Blinking Dot
                    effectType = "dot";
                    break;
                case 3: // Arrow Pointer
                    effectType = "arrow";
                    break;
                case 4: // Circle Highlight
                    effectType = "circle";
                    break;
                case 5: // Spotlight
                    effectType = "spotlight";
                    break;
                case 6: // Star Burst
                    effectType = "star";
                    break;
                case 7: // Ripple Wave
                    effectType = "ripple";
                    break;
                case 8: // Focus Frame
                    effectType = "focus";
                    break;
                case 9: // Underline Box
                    effectType = "underline";
                    break;
                case 10: // Glow Pulse
                    effectType = "glow";
                    break;
                default:
                    return;
            }

            int size = (Integer) sizeSpinner.getValue();
            int fontSize = fontSizeSlider.getValue();
            EffectEntry effect = new EffectEntry(effectType, text, x, y, size, fontSize);

            // Add to config with composite key
            String key = currentLineNumber + ":" + currentImageFile;
            ImageEffectData data = effectsConfig.imageEffects.computeIfAbsent(key, k -> {
                ImageEffectData newData = new ImageEffectData();
                newData.lineNumber = currentLineNumber;
                newData.imageFileName = currentImageFile;
                return newData;
            });
            data.effects.add(effect);

            // Refresh display
            loadEffectsForCurrentImage();

            System.out.println("✓ Added " + effectType + " effect to " + currentImageFile + " at (" + x + ", " + y + ")");
        }


        private void removeSelectedEffect() {
            int selectedIndex = effectsList.getSelectedIndex();
            if (selectedIndex < 0) {
                JOptionPane.showMessageDialog(this, "Please select an effect to remove!",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (currentLineNumber > 0 && currentImageFile != null) {
                String key = currentLineNumber + ":" + currentImageFile;
                ImageEffectData data = effectsConfig.imageEffects.get(key);

                if (data != null && selectedIndex < data.effects.size()) {
                    EffectEntry removedEffect = data.effects.remove(selectedIndex);
                    System.out.println("✓ Removed effect: " + removedEffect.type + " from " + currentImageFile);

                    // If no effects left, remove the entire entry
                    if (data.effects.isEmpty()) {
                        effectsConfig.imageEffects.remove(key);
                    }

                    loadEffectsForCurrentImage();
                }
            }
        }
        private void clearAllEffects() {
            if (currentLineNumber < 0 || currentImageFile == null) {
                JOptionPane.showMessageDialog(this, "Please select a line and image first!",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Clear all effects for this image?\n" +
                            "Image: " + currentImageFile,
                    "Confirm Clear",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                String key = currentLineNumber + ":" + currentImageFile;
                effectsConfig.imageEffects.remove(key);
                loadEffectsForCurrentImage();
                System.out.println("✓ Cleared all effects for " + currentImageFile);
            }
        }
        private void saveEffects() {
            effectsConfig.saveToFile(effectsFilePath);
            JOptionPane.showMessageDialog(this,
                    "Effects saved successfully!\n" +
                            "File: " + effectsFilePath + "\n\n" +
                            "These effects will be applied when generating videos.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        }

        /**
         * Custom panel for displaying image with click detection
         */
        private class ImagePanel extends JPanel {
            private BufferedImage image;
            private BufferedImage scaledImage;
            private int imageX, imageY, imageWidth, imageHeight;
            private int lineNumber;
            private String imageFileName;
            private List<EffectEntry> effects = new ArrayList<>();
            private EffectEntry draggedEffect = null;
            private int dragOffsetX, dragOffsetY;

            public ImagePanel() {
                setBackground(Color.DARK_GRAY);
                setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (image != null) {
                            // Check if clicking on an existing effect to drag it
                            draggedEffect = findEffectAt(e.getX(), e.getY());
                            if (draggedEffect != null) {
                                // Calculate offset from effect position to mouse position
                                int panelX = imageX + (int)(draggedEffect.x / (double)image.getWidth() * imageWidth);
                                int panelY = imageY + (int)(draggedEffect.y / (double)image.getHeight() * imageHeight);
                                dragOffsetX = e.getX() - panelX;
                                dragOffsetY = e.getY() - panelY;
                                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            }
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (draggedEffect != null) {
                            // Save the updated position
                            updateEffectInConfig(draggedEffect);
                            draggedEffect = null;
                            setCursor(Cursor.getDefaultCursor());
                            repaint();
                        }
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (image != null && draggedEffect == null) {
                            // Only add new effect if not dragging
                            if (findEffectAt(e.getX(), e.getY()) == null) {
                                handleImageClick(e.getX(), e.getY());
                            }
                        }
                    }
                });

                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (draggedEffect != null && image != null) {
                            // Update effect position
                            int newPanelX = e.getX() - dragOffsetX;
                            int newPanelY = e.getY() - dragOffsetY;

                            // Convert panel coordinates to image coordinates
                            int newImageX = (int)((newPanelX - imageX) / (double)imageWidth * image.getWidth());
                            int newImageY = (int)((newPanelY - imageY) / (double)imageHeight * image.getHeight());

                            // Clamp to image bounds
                            newImageX = Math.max(0, Math.min(newImageX, image.getWidth() - 1));
                            newImageY = Math.max(0, Math.min(newImageY, image.getHeight() - 1));

                            draggedEffect.x = newImageX;
                            draggedEffect.y = newImageY;
                            repaint();
                        }
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        // Change cursor when hovering over an effect
                        if (image != null && findEffectAt(e.getX(), e.getY()) != null) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        } else {
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                });
            }

            private EffectEntry findEffectAt(int panelX, int panelY) {
                for (EffectEntry effect : effects) {
                    int effectPanelX = imageX + (int)(effect.x / (double)image.getWidth() * imageWidth);
                    int effectPanelY = imageY + (int)(effect.y / (double)image.getHeight() * imageHeight);

                    // Check if click is within effect bounds (use a reasonable hit area)
                    int hitSize = 40; // Hit area size
                    if (panelX >= effectPanelX - hitSize && panelX <= effectPanelX + hitSize &&
                            panelY >= effectPanelY - hitSize && panelY <= effectPanelY + hitSize) {
                        return effect;
                    }
                }
                return null;
            }

            private void updateEffectInConfig(EffectEntry effect) {
                // The effect is already updated in place since it's the same object
                // Just trigger a save notification
                System.out.println("✓ Moved effect to (" + effect.x + ", " + effect.y + ")");
            }

            public void setImage(BufferedImage img, int lineNum, String fileName) {
                this.image = img;
                this.lineNumber = lineNum;
                this.imageFileName = fileName;
                this.effects.clear();
                repaint();
            }

            public void setEffects(List<EffectEntry> effects) {
                this.effects = new ArrayList<>(effects);
                repaint();
            }

            private void handleImageClick(int clickX, int clickY) {
                // Convert panel coordinates to image coordinates
                if (clickX >= imageX && clickX <= imageX + imageWidth &&
                        clickY >= imageY && clickY <= imageY + imageHeight) {

                    int imageRelativeX = (int)((clickX - imageX) / (double)imageWidth * image.getWidth());
                    int imageRelativeY = (int)((clickY - imageY) / (double)imageHeight * image.getHeight());

                    System.out.println("Clicked at image position: (" + imageRelativeX + ", " + imageRelativeY + ")");
                    addEffectAtPosition(imageRelativeX, imageRelativeY);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (image == null) {
                    g.setColor(Color.GRAY);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    String msg = "No image - Select a line";
                    FontMetrics fm = g.getFontMetrics();
                    int msgWidth = fm.stringWidth(msg);
                    g.drawString(msg, (getWidth() - msgWidth) / 2, getHeight() / 2);
                    return;
                }

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Calculate scaled size to fit panel
                int panelWidth = getWidth() - 20;
                int panelHeight = getHeight() - 20;

                double scale = Math.min(
                        (double)panelWidth / image.getWidth(),
                        (double)panelHeight / image.getHeight()
                );

                imageWidth = (int)(image.getWidth() * scale);
                imageHeight = (int)(image.getHeight() * scale);
                imageX = (getWidth() - imageWidth) / 2;
                imageY = (getHeight() - imageHeight) / 2;

                // Draw image
                g2d.drawImage(image, imageX, imageY, imageWidth, imageHeight, null);

                // Draw border around image
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(imageX - 1, imageY - 1, imageWidth + 2, imageHeight + 2);

                // Draw effects preview
                drawEffectsPreviews(g2d);

                // Draw line number label
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(imageX + 10, imageY + 10, 100, 30, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g2d.drawString("Line " + lineNumber, imageX + 20, imageY + 30);
            }

            private void drawEffectsPreviews(Graphics2D g2d) {
                for (EffectEntry effect : effects) {
                    // Convert image coordinates to panel coordinates
                    int panelX = imageX + (int)(effect.x / (double)image.getWidth() * imageWidth);
                    int panelY = imageY + (int)(effect.y / (double)image.getHeight() * imageHeight);

                    switch (effect.type) {
                        case "text":
                            drawTextPreview(g2d, effect.text, panelX, panelY, effect.fontSize);
                            break;
                        case "zoom":
                            drawZoomPreview(g2d, panelX, panelY, effect.size);
                            break;
                        case "dot":
                            drawDotPreview(g2d, panelX, panelY);
                            break;
                    }
                }
            }

            private void drawTextPreview(Graphics2D g2d, String text, int x, int y, int fontSize) {
                // Scale font size for preview (preview is smaller than actual video)
                int previewFontSize = Math.max(12, fontSize / 3);
                Font font = new Font("Arial Unicode MS", Font.BOLD, previewFontSize);
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();

                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                int padding = 8;

                // Background
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRoundRect(x, y, textWidth + padding * 2, textHeight + padding, 10, 10);

                // Border
                g2d.setColor(new Color(255, 215, 0));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x, y, textWidth + padding * 2, textHeight + padding, 10, 10);

                // Text
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, x + padding, y + padding + fm.getAscent());
            }

            private void drawZoomPreview(Graphics2D g2d, int x, int y, int size) {
                int radius = size / 2;

                // Circle
                g2d.setColor(new Color(255, 215, 0, 150));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

                // Inner circle
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x - radius + 5, y - radius + 5, radius * 2 - 10, radius * 2 - 10);

                // Crosshair
                g2d.setColor(new Color(255, 215, 0));
                g2d.drawLine(x - 10, y, x + 10, y);
                g2d.drawLine(x, y - 10, x, y + 10);

                // Label
                g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g2d.setColor(Color.YELLOW);
                g2d.drawString("ZOOM", x - 20, y - radius - 5);
            }

            private void drawDotPreview(Graphics2D g2d, int x, int y) {
                // Outer glow
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.fillOval(x - 15, y - 15, 30, 30);

                // Main dot
                g2d.setColor(new Color(255, 50, 50));
                g2d.fillOval(x - 8, y - 8, 16, 16);

                // Highlight
                g2d.setColor(new Color(255, 200, 200));
                g2d.fillOval(x - 3, y - 3, 6, 6);

                // Label
                g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g2d.setColor(Color.RED);
                g2d.drawString("DOT", x + 15, y + 5);
            }
        }

        // Helper method to load images (reuse existing method)
        private java.util.List<BufferedImage> loadImagesForLine(int lineNumber) {
            java.util.List<BufferedImage> images = new java.util.ArrayList<>();
            File folder = new File(config.imagesPerLineFolder);

            if (!folder.exists() || !folder.isDirectory()) {
                return images;
            }

            String[] extensions = {".jpg", ".jpeg", ".jfif", ".png", ".bmp", ".gif", ".webp"};
            File[] allFiles = folder.listFiles();

            if (allFiles == null) return images;

            java.util.List<File> matchingFiles = new java.util.ArrayList<>();

            for (File file : allFiles) {
                String fileName = file.getName().toLowerCase();
                // Supports both formats: "image1.jpg" (old) or "1.jpg", "11.jpg" (new)
                for (String ext : extensions) {
                    if (fileName.endsWith(ext)) {
                        int extractedLineNumber = extractLineNumberFromImageName(fileName);
                        if (extractedLineNumber == lineNumber) {
                            matchingFiles.add(file);
                            break;
                        }
                    }
                }
            }

            matchingFiles.sort((f1, f2) -> {
                String name1 = f1.getName();
                String name2 = f2.getName();

                boolean hasDash1 = name1.contains("-");
                boolean hasDash2 = name2.contains("-");

                if (!hasDash1 && hasDash2) return -1;
                if (hasDash1 && !hasDash2) return 1;
                if (!hasDash1 && !hasDash2) return name1.compareTo(name2);

                String num1 = name1.replaceAll(".*-(\\d+).*", "$1");
                String num2 = name2.replaceAll(".*-(\\d+).*", "$1");

                try {
                    int n1 = Integer.parseInt(num1);
                    int n2 = Integer.parseInt(num2);
                    return Integer.compare(n1, n2);
                } catch (NumberFormatException e) {
                    return name1.compareTo(name2);
                }
            });

            for (File file : matchingFiles) {
                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        images.add(img);
                    }
                } catch (Exception e) {
                    System.out.println("✗ Error loading image: " + file.getName());
                }
            }

            return images;
        }
    }

    /**
     * Load effects for a specific line from the saved config
     */
    private List<EffectEntry> loadEffectsForLine(int lineNumber) {
        String effectsFilePath = "image_effects.txt";
        File effectsFile = new File(effectsFilePath);

        if (!effectsFile.exists()) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(effectsFile))) {
            List<EffectEntry> effects = new ArrayList<>();
            String line;
            boolean inCorrectLine = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("LINE:")) {
                    int currentLine = Integer.parseInt(line.substring(5));
                    inCorrectLine = (currentLine == lineNumber);
                } else if (line.startsWith("EFFECT:") && inCorrectLine) {
                    String[] parts = line.substring(7).split(",");
                    if (parts.length >= 4) {
                        String type = parts[0];
                        String text = parts[1].isEmpty() ? null : parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        EffectEntry effect = new EffectEntry(type, text, x, y);
                        if (parts.length > 4) {
                            effect.size = Integer.parseInt(parts[4]);
                        }
                        effects.add(effect);
                    }
                } else if (line.equals("---")) {
                    if (inCorrectLine) {
                        return effects; // Found all effects for this line
                    }
                }
            }

            return effects;
        } catch (Exception e) {
            System.out.println("✗ Error loading effects: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Load effects for a specific image in a line
     */
    private java.util.List<EffectEntry> loadEffectsForImage(int lineNumber, String imageFileName) {
        String effectsFilePath = "image_effects.txt";
        File effectsFile = new File(effectsFilePath);

        if (!effectsFile.exists()) {
            return new java.util.ArrayList<>();
        }

        // Extract image number from filename for matching
        // Match by full number ONLY for exact matching (e.g., "11.webp" only matches "11.webp", not "1.webp")
        int imageFullNumber = arabicSync.extractFullNumberFromImageName(imageFileName);

        try (BufferedReader reader = new BufferedReader(new FileReader(effectsFile))) {
            java.util.List<EffectEntry> effects = new java.util.ArrayList<>();
            String line;
            boolean inCorrectImage = false;
            int currentLineNum = -1;
            String currentImageName = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("LINE:")) {
                    currentLineNum = Integer.parseInt(line.substring(5));
                    inCorrectImage = (currentLineNum == lineNumber);
                } else if (line.startsWith("IMAGE:") && inCorrectImage) {
                    currentImageName = line.substring(6);
                    // Extract numbers from saved image name
                    int savedFullNumber = arabicSync.extractFullNumberFromImageName(currentImageName);
                    // Match by full number ONLY for exact matching
                    // This ensures "11.webp" only matches "11.webp", not "1.webp"
                    boolean matches = (savedFullNumber == imageFullNumber);
                    if (matches) {
                        System.out.println("MATCHED: Line " + lineNumber + ", Looking for: " + imageFileName +
                                " (fullNumber=" + imageFullNumber + "), Found: " + currentImageName +
                                " (fullNumber=" + savedFullNumber + ")");
                    }
                    inCorrectImage = matches;
                } else if (line.startsWith("EFFECT:") && inCorrectImage) {
                    String[] parts = line.substring(7).split(",");
                    if (parts.length >= 4) {
                        String type = parts[0];
                        String text = parts[1].isEmpty() ? null : parts[1];
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        EffectEntry effect = new EffectEntry(type, text, x, y);
                        if (parts.length > 4) {
                            effect.size = Integer.parseInt(parts[4]);
                        }
                        effects.add(effect);
                    }
                } else if (line.equals("---")) {
                    if (inCorrectImage && !effects.isEmpty()) {
                        return effects; // Found effects for this specific image
                    }
                    inCorrectImage = false;
                    currentImageName = null;
                }
            }

            return effects;
        } catch (Exception e) {
            System.out.println("✗ Error loading effects: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Draw text caption effect (from saved config)
     */
    private void drawTextCaptionEffect(Graphics2D g2d, String text, int x, int y, Font arabicFont, int fontSize, double currentTime) {
        Font captionFont = arabicFont.deriveFont(Font.BOLD, (float) fontSize);
        g2d.setFont(captionFont);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fm = g2d.getFontMetrics(captionFont);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int padding = 15;
        int boxWidth = textWidth + (2 * padding);
        int boxHeight = textHeight + (2 * padding);

        // Semi-transparent background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 20, 20);

        // Border
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 20, 20);

        // Text
        int textX = x + padding;
        int textY = y + padding + fm.getAscent();

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.drawString(text, textX + 2, textY + 2);

        // Main text
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
    }

    /**
     * Draw zoom effect (simplified for saved config)
     */
    /**
     * Draw a small focus indicator at the zoom target point
     */
    private void drawZoomPointIndicator(Graphics2D g2d, int x, int y, double currentTime) {
        // Small pulsing circle at the zoom target
        double pulse = (Math.sin(currentTime * 6) + 1) / 2; // 0 to 1
        int baseSize = 15;
        int size = baseSize + (int)(pulse * 5);

        // Outer glow
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 215, 0, 50));
        g2d.fillOval(x - size - 5, y - size - 5, (size + 5) * 2, (size + 5) * 2);

        // Main circle
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - size, y - size, size * 2, size * 2);

        // Center crosshair
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x - 8, y, x + 8, y);
        g2d.drawLine(x, y - 8, x, y + 8);
    }

    /**
     * Draw image with animated zoom effect - zooms IN to target point then OUT
     * @param g2d Graphics context
     * @param image The image to draw
     * @param width Video frame width
     * @param height Video frame height
     * @param zoomTargetX Target X in original image coordinates
     * @param zoomTargetY Target Y in original image coordinates
     * @param originalImageWidth Original image width (for coordinate conversion)
     * @param originalImageHeight Original image height (for coordinate conversion)
     * @param lineDuration Duration of the line/image display in seconds
     * @param timeInLine Current time position within this line
     * @param maxZoom Maximum zoom level (e.g., 1.5 for 150%)
     */
    /**
     * Apply zoom transformation directly to Graphics2D when drawing a fitted image
     */
    private void applyZoomToFittedImage(Graphics2D g2d, BufferedImage fittedImage,
                                        int width, int height,
                                        BufferedImage originalImage, EffectEntry zoomEffect,
                                        FormattedLineArabicSync currentQuoteLine, double currentTime, double lineDuration) {
        double timeInLine = currentTime - currentQuoteLine.startTime;
        double maxZoom = 1.0 + (zoomEffect.size / 100.0); // size 80 = 1.8x zoom

        // Safety check: ensure lineDuration is valid to avoid division by zero
        if (lineDuration <= 0) {
            lineDuration = 3.0; // Default to 3 seconds
        }

        // Calculate zoom factor: zoom in first half, zoom out second half
        double progress = timeInLine / lineDuration;
        progress = Math.max(0, Math.min(1, progress)); // Clamp to 0-1

        double zoomFactor;
        if (progress <= 0.5) {
            // First half: zoom IN from 1.0 to maxZoom
            zoomFactor = 1.0 + (progress * 2 * (maxZoom - 1.0));
        } else {
            // Second half: zoom OUT from maxZoom back to 1.0
            zoomFactor = maxZoom - ((progress - 0.5) * 2 * (maxZoom - 1.0));
        }

        // Debug output (only print occasionally to avoid spam)
        if (Math.random() < 0.01) { // Print 1% of the time
            System.out.println("ZOOM: progress=" + String.format("%.2f", progress) +
                    ", zoomFactor=" + String.format("%.2f", zoomFactor) +
                    ", maxZoom=" + String.format("%.2f", maxZoom) +
                    ", target=(" + zoomEffect.x + "," + zoomEffect.y + ")");
        }

        // Calculate where the zoom target is in the fitted image
        int marginHorizontal = 40;
        int marginVertical = 200;
        int availableWidth = width - (2 * marginHorizontal);
        int availableHeight = height - (2 * marginVertical);

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double originalAspect = (double) originalWidth / originalHeight;
        boolean isPortrait = (originalHeight > originalWidth);

        int scaledWidth, scaledHeight;
        int offsetX, offsetY;

        if (isPortrait) {
            double portraitScale = 0.6;
            scaledHeight = (int) (availableHeight * portraitScale);
            scaledWidth = (int) (scaledHeight * originalAspect);
            offsetX = marginHorizontal + ((availableWidth - scaledWidth) / 2);
            offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
        } else {
            scaledWidth = availableWidth;
            scaledHeight = (int) (availableWidth / originalAspect);
            offsetX = marginHorizontal;
            offsetY = marginVertical + ((availableHeight - scaledHeight) / 2);
        }

        // Convert zoom target from original image coords to fitted image coords
        double scaleX = (double) scaledWidth / originalWidth;
        double scaleY = (double) scaledHeight / originalHeight;
        int zoomTargetX_fitted = offsetX + (int)(zoomEffect.x * scaleX);
        int zoomTargetY_fitted = offsetY + (int)(zoomEffect.y * scaleY);

        // First draw the blurred background (without zoom)
        BufferedImage blurred = applyGaussianBlur(originalImage, 30);
        g2d.drawImage(blurred, 0, 0, width, height, null);

        // Darken the background
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Now draw the zoomed image portion
        Graphics2D g2dZoom = (Graphics2D) g2d.create();
        g2dZoom.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2dZoom.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2dZoom.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate the scaled image size with zoom
        int zoomedScaledWidth = (int)(scaledWidth * zoomFactor);
        int zoomedScaledHeight = (int)(scaledHeight * zoomFactor);

        // Calculate offset to keep zoom target point in the same position
        // The target point should stay at zoomTargetX_fitted, zoomTargetY_fitted
        // After zooming, where will the target be?
        double targetRatioX = (double)(zoomTargetX_fitted - offsetX) / scaledWidth;
        double targetRatioY = (double)(zoomTargetY_fitted - offsetY) / scaledHeight;

        // Where the target will be in the zoomed image
        double zoomedTargetX = offsetX + (targetRatioX * zoomedScaledWidth);
        double zoomedTargetY = offsetY + (targetRatioY * zoomedScaledHeight);

        // Offset needed to keep target in same position
        int zoomOffsetX = offsetX - (int)(zoomedTargetX - zoomTargetX_fitted);
        int zoomOffsetY = offsetY - (int)(zoomedTargetY - zoomTargetY_fitted);

        // Clip to image area only (not the whole frame) to prevent overflow
        g2dZoom.setClip(offsetX, offsetY, scaledWidth, scaledHeight);

        // Draw the zoomed original image
        g2dZoom.drawImage(originalImage, zoomOffsetX, zoomOffsetY, zoomedScaledWidth, zoomedScaledHeight, null);
        g2dZoom.dispose();
    }

    /**
     * Apply zoom animation to an image and return the zoomed image
     * Zooms IN to target point then OUT
     * @param image The original image
     * @param zoomTargetX Target X in original image coordinates
     * @param zoomTargetY Target Y in original image coordinates
     * @param lineDuration Duration of the line/image display in seconds
     * @param timeInLine Current time position within this line
     * @param maxZoom Maximum zoom level (e.g., 1.5 for 150%)
     * @return The zoomed image
     */
    private BufferedImage applyZoomToImage(BufferedImage image,
                                           int zoomTargetX, int zoomTargetY,
                                           double lineDuration, double timeInLine,
                                           double maxZoom) {
        if (image == null) return image;

        // Calculate animation progress (0 to 1)
        double progress = timeInLine / lineDuration;
        progress = Math.max(0, Math.min(1, progress)); // Clamp to 0-1

        // Calculate zoom factor: zoom in first half, zoom out second half
        double zoomFactor;
        if (progress <= 0.5) {
            // First half: zoom IN from 1.0 to maxZoom
            zoomFactor = 1.0 + (progress * 2 * (maxZoom - 1.0));
        } else {
            // Second half: zoom OUT from maxZoom back to 1.0
            zoomFactor = maxZoom - ((progress - 0.5) * 2 * (maxZoom - 1.0));
        }

        // If no zoom, return original
        if (zoomFactor == 1.0) {
            return image;
        }

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Calculate the scaled size
        int scaledWidth = (int) (originalWidth * zoomFactor);
        int scaledHeight = (int) (originalHeight * zoomFactor);

        // Calculate offset to keep zoom target centered
        // The target point should stay at the same position in the image as we zoom
        double targetRatioX = (double) zoomTargetX / originalWidth;
        double targetRatioY = (double) zoomTargetY / originalHeight;

        // Where the target will be after scaling
        double scaledTargetX = targetRatioX * scaledWidth;
        double scaledTargetY = targetRatioY * scaledHeight;

        // Offset needed to keep target in same position (center the target)
        int offsetX = (int) (zoomTargetX - scaledTargetX);
        int offsetY = (int) (zoomTargetY - scaledTargetY);

        // Create new image with zoomed size (make it larger to accommodate zoom)
        int resultWidth = Math.max(scaledWidth, originalWidth);
        int resultHeight = Math.max(scaledHeight, originalHeight);

        // Adjust offsets to center the zoomed image
        offsetX += (resultWidth - scaledWidth) / 2;
        offsetY += (resultHeight - scaledHeight) / 2;

        BufferedImage zoomedImage = new BufferedImage(resultWidth, resultHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2dZoom = zoomedImage.createGraphics();
        g2dZoom.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2dZoom.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2dZoom.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background with black
        g2dZoom.setColor(Color.BLACK);
        g2dZoom.fillRect(0, 0, resultWidth, resultHeight);

        // Draw the scaled image
        g2dZoom.drawImage(image, offsetX, offsetY, scaledWidth, scaledHeight, null);
        g2dZoom.dispose();

        return zoomedImage;
    }

    /**
     * Draw zoom effect that actually magnifies the specified area of the image (legacy magnifier style)
     */
    private void drawZoomEffectSimple(Graphics2D g2d, BufferedImage originalImage,
                                      int imageX, int imageY, int videoX, int videoY,
                                      int size, double currentTime) {
        if (originalImage == null) return;

        int zoomRadius = size / 2;  // Circle radius on screen
        double zoomLevel = 3.0;     // 3x magnification (increase for more zoom)

        // Calculate the area in the ORIGINAL image to extract
        int sourceRadius = (int)(zoomRadius / zoomLevel);
        int sourceX = imageX - sourceRadius;
        int sourceY = imageY - sourceRadius;
        int sourceSize = sourceRadius * 2;

        // Bounds check - ensure we don't go outside image
        if (sourceX < 0) {
            sourceSize += sourceX;
            sourceX = 0;
        }
        if (sourceY < 0) {
            sourceSize += sourceY;
            sourceY = 0;
        }
        if (sourceX + sourceSize > originalImage.getWidth()) {
            sourceSize = originalImage.getWidth() - sourceX;
        }
        if (sourceY + sourceSize > originalImage.getHeight()) {
            sourceSize = originalImage.getHeight() - sourceY;
        }

        if (sourceSize <= 0) return;

        try {
            // Extract the region from ORIGINAL image
            BufferedImage zoomedRegion = originalImage.getSubimage(sourceX, sourceY, sourceSize, sourceSize);

            // Create circular clip for magnifier
            Graphics2D g2dClip = (Graphics2D) g2d.create();
            g2dClip.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2dClip.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Set circular clip
            g2dClip.setClip(new java.awt.geom.Ellipse2D.Double(
                    videoX - zoomRadius, videoY - zoomRadius,
                    zoomRadius * 2, zoomRadius * 2
            ));

            // Draw the zoomed region - SCALED UP to fill the circle
            g2dClip.drawImage(zoomedRegion,
                    videoX - zoomRadius, videoY - zoomRadius,
                    zoomRadius * 2, zoomRadius * 2, null);
            g2dClip.dispose();

            // Draw animated magnifier border with glow
            double pulse = (Math.sin(currentTime * 4) + 1) / 2; // 0 to 1

            // Outer glow
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 6; i >= 1; i--) {
                int alpha = 30 + (int)(pulse * 20);
                g2d.setColor(new Color(255, 215, 0, alpha / i));
                g2d.setStroke(new BasicStroke(i * 2));
                g2d.drawOval(videoX - zoomRadius - i, videoY - zoomRadius - i,
                        (zoomRadius + i) * 2, (zoomRadius + i) * 2);
            }

            // Main border (gold)
            int borderAlpha = 200 + (int)(pulse * 55); // 200-255
            g2d.setColor(new Color(255, 215, 0, borderAlpha));
            g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawOval(videoX - zoomRadius, videoY - zoomRadius,
                    zoomRadius * 2, zoomRadius * 2);

            // Inner highlight ring (white)
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(videoX - zoomRadius + 4, videoY - zoomRadius + 4,
                    (zoomRadius - 4) * 2, (zoomRadius - 4) * 2);

            // Inner shadow for depth
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(videoX - zoomRadius + 8, videoY - zoomRadius + 8,
                    (zoomRadius - 8) * 2, (zoomRadius - 8) * 2);

            // Add "lens glare" effect
            double glarePhase = (currentTime * 2) % (Math.PI * 2);
            if (glarePhase < Math.PI) {
                double glareOpacity = Math.sin(glarePhase) * 0.6;
                RadialGradientPaint glare = new RadialGradientPaint(
                        videoX - zoomRadius/3, videoY - zoomRadius/3,
                        zoomRadius/2,
                        new float[]{0.0f, 1.0f},
                        new Color[]{
                                new Color(255, 255, 255, (int)(glareOpacity * 180)),
                                new Color(255, 255, 255, 0)
                        }
                );
                g2d.setPaint(glare);
                g2d.fillOval(videoX - zoomRadius, videoY - zoomRadius,
                        zoomRadius * 2, zoomRadius * 2);
            }

        } catch (Exception e) {
            System.out.println("Zoom effect error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Draw blinking dot effect (from saved config)
     */
    private void drawBlinkingDotEffect(Graphics2D g2d, int x, int y, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double blinkPhase = (currentTime * 3) % (Math.PI * 2);
        double opacity = (Math.sin(blinkPhase) + 1) / 2;

        double sizePulse = (Math.sin(currentTime * 4) + 1) / 2;
        int dotSize = 12 + (int)(sizePulse * 8);

        // Outer glow
        int glowSize = dotSize * 3;
        RadialGradientPaint glowGradient = new RadialGradientPaint(
                x, y, glowSize / 2.0f,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                        new Color(255, 50, 50, (int)(opacity * 200)),
                        new Color(255, 100, 0, (int)(opacity * 100)),
                        new Color(255, 150, 0, 0)
                }
        );
        g2d.setPaint(glowGradient);
        g2d.fillOval(x - glowSize/2, y - glowSize/2, glowSize, glowSize);

        // Main dot
        RadialGradientPaint dotGradient = new RadialGradientPaint(
                x, y, dotSize / 2.0f,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                        new Color(255, 255, 255, (int)(opacity * 255)),
                        new Color(255, 50, 50, (int)(opacity * 255)),
                        new Color(200, 0, 0, (int)(opacity * 255))
                }
        );
        g2d.setPaint(dotGradient);
        g2d.fillOval(x - dotSize/2, y - dotSize/2, dotSize, dotSize);

        // Outer ring
        g2d.setColor(new Color(255, 255, 0, (int)(opacity * 200)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - dotSize/2 - 3, y - dotSize/2 - 3,
                dotSize + 6, dotSize + 6);
    }

    /**
     * Draw animated arrow pointer effect
     */
    private void drawArrowPointerEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Bouncing animation
        double bounce = Math.sin(currentTime * 6) * 10;
        int animY = y - (int)bounce - size/2;

        // Arrow size based on effect size
        int arrowWidth = size;
        int arrowHeight = (int)(size * 1.2);

        // Create arrow shape pointing down
        int[] xPoints = {x, x - arrowWidth/2, x - arrowWidth/4, x - arrowWidth/4, x + arrowWidth/4, x + arrowWidth/4, x + arrowWidth/2};
        int[] yPoints = {animY + arrowHeight, animY + arrowHeight/2, animY + arrowHeight/2, animY, animY, animY + arrowHeight/2, animY + arrowHeight/2};

        // Glow effect
        for (int i = 5; i >= 1; i--) {
            g2d.setColor(new Color(255, 100, 0, 40/i));
            g2d.setStroke(new BasicStroke(i * 3));
            g2d.drawPolygon(xPoints, yPoints, 7);
        }

        // Fill with gradient
        GradientPaint gradient = new GradientPaint(
                x, animY, new Color(255, 200, 0),
                x, animY + arrowHeight, new Color(255, 100, 0)
        );
        g2d.setPaint(gradient);
        g2d.fillPolygon(xPoints, yPoints, 7);

        // Border
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawPolygon(xPoints, yPoints, 7);

        // Inner highlight
        g2d.setColor(new Color(255, 255, 200, 150));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawPolygon(xPoints, yPoints, 7);
    }

    /**
     * Draw pulsing circle highlight effect
     */
    private void drawCircleHighlightEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = (Math.sin(currentTime * 4) + 1) / 2; // 0 to 1
        int baseRadius = size / 2;
        int animRadius = baseRadius + (int)(pulse * 15);

        // Multiple concentric rings with decreasing opacity
        for (int ring = 3; ring >= 0; ring--) {
            int ringRadius = animRadius + ring * 8;
            int alpha = (int)(200 - ring * 50 - pulse * 30);
            alpha = Math.max(0, Math.min(255, alpha));

            // Outer glow
            g2d.setColor(new Color(0, 200, 255, alpha / 3));
            g2d.setStroke(new BasicStroke(6 - ring));
            g2d.drawOval(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2);

            // Main ring
            g2d.setColor(new Color(100, 220, 255, alpha));
            g2d.setStroke(new BasicStroke(3 - ring * 0.5f));
            g2d.drawOval(x - ringRadius, y - ringRadius, ringRadius * 2, ringRadius * 2);
        }

        // Center crosshair
        g2d.setColor(new Color(255, 255, 255, (int)(150 + pulse * 100)));
        g2d.setStroke(new BasicStroke(2));
        int crossSize = 15;
        g2d.drawLine(x - crossSize, y, x + crossSize, y);
        g2d.drawLine(x, y - crossSize, x, y + crossSize);
    }

    /**
     * Draw spotlight effect - darkens everything except the highlighted area
     */
    private void drawSpotlightEffect(Graphics2D g2d, int x, int y, int size, int width, int height, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = (Math.sin(currentTime * 3) + 1) / 2;
        int spotRadius = size + (int)(pulse * 20);

        // Create spotlight mask (darken outside)
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskG2d = mask.createGraphics();
        maskG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill with semi-transparent dark
        maskG2d.setColor(new Color(0, 0, 0, 150));
        maskG2d.fillRect(0, 0, width, height);

        // Clear the spotlight area with radial gradient
        maskG2d.setComposite(AlphaComposite.Clear);
        RadialGradientPaint clearGradient = new RadialGradientPaint(
                x, y, spotRadius,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(0, 0, 0, 255), new Color(0, 0, 0, 200), new Color(0, 0, 0, 0)}
        );
        maskG2d.setPaint(clearGradient);
        maskG2d.fillOval(x - spotRadius, y - spotRadius, spotRadius * 2, spotRadius * 2);
        maskG2d.dispose();

        // Draw the mask
        g2d.drawImage(mask, 0, 0, null);

        // Draw spotlight ring
        g2d.setColor(new Color(255, 255, 200, (int)(100 + pulse * 100)));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval(x - spotRadius, y - spotRadius, spotRadius * 2, spotRadius * 2);

        // Inner glow ring
        g2d.setColor(new Color(255, 255, 255, (int)(50 + pulse * 50)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - spotRadius + 5, y - spotRadius + 5, (spotRadius - 5) * 2, (spotRadius - 5) * 2);
    }

    /**
     * Draw animated star burst effect
     */
    private void drawStarBurstEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double rotation = currentTime * 2; // Slow rotation
        double pulse = (Math.sin(currentTime * 5) + 1) / 2;
        int numPoints = 8;
        int outerRadius = size / 2 + (int)(pulse * 10);
        int innerRadius = outerRadius / 2;

        // Create star shape
        java.awt.geom.Path2D star = new java.awt.geom.Path2D.Double();
        for (int i = 0; i < numPoints * 2; i++) {
            double angle = rotation + (Math.PI * i / numPoints);
            int radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();

        // Outer glow
        for (int i = 4; i >= 1; i--) {
            g2d.setColor(new Color(255, 215, 0, 50 / i));
            g2d.setStroke(new BasicStroke(i * 4));
            g2d.draw(star);
        }

        // Fill with gradient
        RadialGradientPaint starGradient = new RadialGradientPaint(
                x, y, outerRadius,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                        new Color(255, 255, 200),
                        new Color(255, 215, 0),
                        new Color(255, 150, 0)
                }
        );
        g2d.setPaint(starGradient);
        g2d.fill(star);

        // Border
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(star);

        // Center sparkle
        g2d.setColor(new Color(255, 255, 255, (int)(200 + pulse * 55)));
        int sparkleSize = 8 + (int)(pulse * 4);
        g2d.fillOval(x - sparkleSize/2, y - sparkleSize/2, sparkleSize, sparkleSize);
    }

    /**
     * Draw expanding ripple wave effect
     */
    private void drawRippleWaveEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int maxRadius = size;
        int numRipples = 3;
        double rippleSpeed = 2.0;

        for (int i = 0; i < numRipples; i++) {
            // Calculate ripple phase (0 to 1, cycling)
            double phase = ((currentTime * rippleSpeed + i * 0.33) % 1.0);
            int rippleRadius = (int)(phase * maxRadius);
            int alpha = (int)(255 * (1.0 - phase)); // Fade out as it expands

            if (rippleRadius > 0 && alpha > 0) {
                // Outer ring with gradient stroke effect
                for (int j = 3; j >= 1; j--) {
                    g2d.setColor(new Color(0, 150, 255, alpha / (j + 1)));
                    g2d.setStroke(new BasicStroke(j * 2));
                    g2d.drawOval(x - rippleRadius, y - rippleRadius, rippleRadius * 2, rippleRadius * 2);
                }

                // Main ripple ring
                g2d.setColor(new Color(100, 200, 255, alpha));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(x - rippleRadius, y - rippleRadius, rippleRadius * 2, rippleRadius * 2);
            }
        }

        // Center dot
        double centerPulse = (Math.sin(currentTime * 8) + 1) / 2;
        int centerSize = 10 + (int)(centerPulse * 5);
        g2d.setColor(new Color(150, 220, 255, 255));
        g2d.fillOval(x - centerSize/2, y - centerSize/2, centerSize, centerSize);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(x - centerSize/4, y - centerSize/4, centerSize/2, centerSize/2);
    }

    /**
     * Draw camera-style focus frame effect
     */
    private void drawFocusFrameEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = (Math.sin(currentTime * 3) + 1) / 2;
        int frameSize = size;
        int cornerLength = frameSize / 4;
        int cornerThickness = 4;

        int left = x - frameSize / 2;
        int top = y - frameSize / 2;
        int right = x + frameSize / 2;
        int bottom = y + frameSize / 2;

        // Animated color
        int green = 200 + (int)(pulse * 55);
        Color frameColor = new Color(50, green, 50);
        Color glowColor = new Color(100, 255, 100, (int)(100 + pulse * 100));

        // Draw corner brackets with glow
        g2d.setStroke(new BasicStroke(cornerThickness + 4));
        g2d.setColor(glowColor);
        drawFocusCorners(g2d, left, top, right, bottom, cornerLength);

        g2d.setStroke(new BasicStroke(cornerThickness));
        g2d.setColor(frameColor);
        drawFocusCorners(g2d, left, top, right, bottom, cornerLength);

        // Inner highlight
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(200, 255, 200, 150));
        drawFocusCorners(g2d, left + 2, top + 2, right - 2, bottom - 2, cornerLength - 4);

        // Center crosshair (subtle)
        g2d.setColor(new Color(100, 255, 100, (int)(80 + pulse * 50)));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x - 10, y, x + 10, y);
        g2d.drawLine(x, y - 10, x, y + 10);
    }

    private void drawFocusCorners(Graphics2D g2d, int left, int top, int right, int bottom, int len) {
        // Top-left corner
        g2d.drawLine(left, top, left + len, top);
        g2d.drawLine(left, top, left, top + len);
        // Top-right corner
        g2d.drawLine(right, top, right - len, top);
        g2d.drawLine(right, top, right, top + len);
        // Bottom-left corner
        g2d.drawLine(left, bottom, left + len, bottom);
        g2d.drawLine(left, bottom, left, bottom - len);
        // Bottom-right corner
        g2d.drawLine(right, bottom, right - len, bottom);
        g2d.drawLine(right, bottom, right, bottom - len);
    }

    /**
     * Draw animated underline/box effect
     */
    private void drawUnderlineBoxEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = (Math.sin(currentTime * 4) + 1) / 2;
        int boxWidth = size;
        int boxHeight = size / 3;

        int left = x - boxWidth / 2;
        int top = y - boxHeight / 2;

        // Animated drawing effect - line grows from center
        double progress = (currentTime * 2) % 1.0;
        int drawnWidth = (int)(boxWidth * Math.min(progress * 2, 1.0));
        int offsetX = (boxWidth - drawnWidth) / 2;

        // Glow effect
        for (int i = 4; i >= 1; i--) {
            g2d.setColor(new Color(255, 200, 0, 40 / i));
            g2d.setStroke(new BasicStroke(i * 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawRoundRect(left + offsetX, top, drawnWidth, boxHeight, 10, 10);
        }

        // Main box
        GradientPaint boxGradient = new GradientPaint(
                left, top, new Color(255, 180, 0, 200),
                left, top + boxHeight, new Color(255, 100, 0, 200)
        );
        g2d.setPaint(boxGradient);
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawRoundRect(left + offsetX, top, drawnWidth, boxHeight, 10, 10);

        // Inner highlight
        g2d.setColor(new Color(255, 255, 200, (int)(100 + pulse * 100)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(left + offsetX + 2, top + 2, Math.max(0, drawnWidth - 4), Math.max(0, boxHeight - 4), 8, 8);
    }

    /**
     * Draw soft glowing pulse effect
     */
    private void drawGlowPulseEffect(Graphics2D g2d, int x, int y, int size, double currentTime) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = (Math.sin(currentTime * 3) + 1) / 2;
        double fastPulse = (Math.sin(currentTime * 7) + 1) / 2;
        int baseRadius = size / 2;
        int glowRadius = baseRadius + (int)(pulse * 30);

        // Multiple glow layers with different colors
        Color[] glowColors = {
                new Color(255, 100, 150), // Pink
                new Color(150, 100, 255), // Purple
                new Color(100, 200, 255)  // Cyan
        };

        for (int layer = 0; layer < glowColors.length; layer++) {
            double layerPhase = currentTime * 2 + layer * Math.PI / 3;
            double layerPulse = (Math.sin(layerPhase) + 1) / 2;
            int layerRadius = glowRadius + layer * 10 - (int)(layerPulse * 15);

            Color baseColor = glowColors[layer];
            RadialGradientPaint glowGradient = new RadialGradientPaint(
                    x, y, layerRadius,
                    new float[]{0.0f, 0.5f, 1.0f},
                    new Color[]{
                            new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(layerPulse * 100)),
                            new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(layerPulse * 50)),
                            new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0)
                    }
            );
            g2d.setPaint(glowGradient);
            g2d.fillOval(x - layerRadius, y - layerRadius, layerRadius * 2, layerRadius * 2);
        }

        // Center bright spot
        int centerSize = 15 + (int)(fastPulse * 10);
        RadialGradientPaint centerGradient = new RadialGradientPaint(
                x, y, centerSize,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                        new Color(255, 255, 255, 255),
                        new Color(255, 200, 255, 200),
                        new Color(255, 150, 255, 0)
                }
        );
        g2d.setPaint(centerGradient);
        g2d.fillOval(x - centerSize, y - centerSize, centerSize * 2, centerSize * 2);
    }











}
