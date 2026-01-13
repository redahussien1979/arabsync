//private static class ThumbnailFileChooser extends JFileChooser {
//    private JLabel imageLabel;
//    private JPanel previewPanel;
//
//    public ThumbnailFileChooser() {
//        super();
//        initializePreview();
//    }
//
//    private void initializePreview() {
//        // Create preview panel
//        previewPanel = new JPanel(new BorderLayout());
//        previewPanel.setPreferredSize(new Dimension(200, 200));
//        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
//
//        // Create image label for thumbnail
//        imageLabel = new JLabel("No preview available", JLabel.CENTER);
//        imageLabel.setPreferredSize(new Dimension(180, 150));
//        imageLabel.setVerticalAlignment(JLabel.CENTER);
//
//        previewPanel.add(imageLabel, BorderLayout.CENTER);
//
//        // Add preview panel to file chooser
//        setAccessory(previewPanel);
//
//        // Add property change listener to update preview
//        addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, e -> {
//            File selectedFile = getSelectedFile();
//            updatePreview(selectedFile);
//        });
//    }
//
//    private void updatePreview(File file) {
//        if (file == null || !file.exists() || !file.isFile()) {
//            imageLabel.setIcon(null);
//            imageLabel.setText("No preview available");
//            return;
//        }
//
//        // Check if it's an image file
//        String fileName = file.getName().toLowerCase();
//        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
//        boolean isImage = false;
//
//        for (String ext : imageExtensions) {
//            if (fileName.endsWith(ext)) {
//                isImage = true;
//                break;
//            }
//        }
//
//        if (!isImage) {
//            imageLabel.setIcon(null);
//            imageLabel.setText("Not an image file");
//            return;
//        }
//
//        try {
//            // Load and scale image for thumbnail
//            BufferedImage originalImage = ImageIO.read(file);
//            if (originalImage != null) {
//                ImageIcon thumbnail = createThumbnail(originalImage, 180, 150);
//                imageLabel.setIcon(thumbnail);
//                imageLabel.setText("");
//
//                // Add file info
//                long fileSize = file.length();
//                String sizeText = String.format("%dx%d | %.1f KB",
//                        originalImage.getWidth(),
//                        originalImage.getHeight(),
//                        fileSize / 1024.0);
//                imageLabel.setToolTipText(sizeText);
//            } else {
//                imageLabel.setIcon(null);
//                imageLabel.setText("Cannot load image");
//            }
//        } catch (Exception e) {
//            imageLabel.setIcon(null);
//            imageLabel.setText("Error loading image");
//        }
//    }
//
//    private ImageIcon createThumbnail(BufferedImage originalImage, int maxWidth, int maxHeight) {
//        int originalWidth = originalImage.getWidth();
//        int originalHeight = originalImage.getHeight();
//
//        // Calculate scaling to maintain aspect ratio
//        double scaleX = (double) maxWidth / originalWidth;
//        double scaleY = (double) maxHeight / originalHeight;
//        double scale = Math.min(scaleX, scaleY);
//
//        int thumbnailWidth = (int) (originalWidth * scale);
//        int thumbnailHeight = (int) (originalHeight * scale);
//
//        // Create thumbnail
//        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = thumbnail.createGraphics();
//        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
//        g2d.dispose();
//
//        return new ImageIcon(thumbnail);
//    }
//}