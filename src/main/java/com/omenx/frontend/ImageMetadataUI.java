package com.omenx.frontend;

import com.omenx.backend.osint.ImageMetadataScanner;
import com.omenx.backend.service.ScanService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// =========================================================
// IMAGE METADATA UI
// Dark Glassmorphic EXIF Metadata & Geolocation Extraction
// =========================================================
public class ImageMetadataUI {

    private static final String BG      = "#060810";
    private static final String TEXT    = "#F1F5F9";
    private static final String MUTED   = "#64748B";
    private static final String CYAN    = "#22D3EE";
    private static final String GREEN   = "#10B981";

    private final Runnable backAction;
    private final ScanService scanService;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private AppHeader header;

    public ImageMetadataUI(Runnable backAction) {
        this(backAction, null);
    }

    public ImageMetadataUI(Runnable backAction, ScanService scanService) {
        this.backAction = backAction;
        this.scanService = scanService;
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================
    public VBox createView() {

        header = new AppHeader(
                ModuleIcons.get("EXIF Metadata"),
                "EXIF METADATA & FORENSICS",
                "Extract camera optical signatures, timestamps, hardware profiles & GPS coordinates",
                backAction
        );

        Label dropIcon = new Label("📁");
        dropIcon.setStyle("-fx-font-size: 32px;");

        Label dropText = new Label("Drag & Drop image file here, or click to browse");
        dropText.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label dropSub = new Label("Supports JPG, PNG, GIF, TIFF, WEBP EXIF & GPS extraction");
        dropSub.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        Button selectButton = new Button("Choose Image File…");
        selectButton.getStyleClass().add("btn-primary");

        VBox dropZone = new VBox(10, dropIcon, dropText, dropSub, selectButton);
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setAlignment(Pos.CENTER);

        VBox results = new VBox(
                8,
                createEmptyState("Select or drop an image above to analyze EXIF metadata signatures.")
        );

        ScrollPane scroll = new ScrollPane(results);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Handler for processing file selection/drop
        java.util.function.Consumer<File> processFile = file -> {
            if (file == null) return;

            selectButton.setDisable(true);
            header.setStatus(AppHeader.StatusType.SCANNING, "EXTRACTING EXIF");
            results.getChildren().setAll(
                    createEmptyState("Reading EXIF and image metadata signatures…")
            );

            executor.submit(() -> {
                ImageMetadataScanner.Result result =
                        new ImageMetadataScanner().scan(file);

                Platform.runLater(() -> {
                    renderImageMetadataWithPreview(results, result, file);
                    selectButton.setDisable(false);
                    header.setStatus(AppHeader.StatusType.READY, "COMPLETE");
                });
            });
        };

        selectButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Image File for EXIF Analysis");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Image Files",
                            "*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.webp"
                    )
            );
            Window window = selectButton.getScene().getWindow();
            File selectedFile = chooser.showOpenDialog(window);
            if (selectedFile != null) {
                processFile.accept(selectedFile);
            }
        });

        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                String name = file.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".png") || name.endsWith(".gif")
                        || name.endsWith(".tif") || name.endsWith(".tiff")
                        || name.endsWith(".webp")) {
                    processFile.accept(file);
                    success = true;
                } else {
                    results.getChildren().setAll(
                            createEmptyState("Unsupported file type: " + file.getName() + " (Select an image)")
                    );
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        VBox content = new VBox(
                16,
                header,
                dropZone,
                scroll
        );

        content.setPadding(new Insets(20, 26, 20, 26));
        content.setStyle("-fx-background-color: " + BG + ";");
        return content;
    }

    // =========================================================
    // RENDER METADATA
    // =========================================================
    private void renderImageMetadataWithPreview(
            VBox results,
            ImageMetadataScanner.Result result,
            File file
    ) {
        results.getChildren().clear();

        if (file != null && file.exists()) {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 280, 280, true, true);
                javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                imgView.setFitWidth(240);
                imgView.setFitHeight(240);
                imgView.setPreserveRatio(true);

                Label fileTitle = new Label("📷  " + file.getName());
                fileTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

                Label pathLabel = new Label(file.getAbsolutePath());
                pathLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

                VBox previewBox = new VBox(8, imgView, fileTitle, pathLabel);
                previewBox.setAlignment(Pos.CENTER);
                previewBox.getStyleClass().add("glass-panel");
                previewBox.setPadding(new Insets(16));

                results.getChildren().add(previewBox);
            } catch (Exception ignored) {}
        }

        if (scanService != null && file != null) {
            scanService.saveScan(
                    "Image Metadata",
                    file.getName(),
                    result.metadata != null ? result.metadata.size() : 0,
                    java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    )
            );
        }

        renderImageMetadata(results, result);
    }

    private void renderImageMetadata(
            VBox results,
            ImageMetadataScanner.Result result
    ) {
        if (result.metadata.isEmpty()) {
            results.getChildren().add(
                    createEmptyState("No EXIF metadata was found in this file.")
            );
            return;
        }

        results.getChildren().add(createSectionHeader("FILE SPECIFICATIONS"));
        addMetadataRow(results, "File Name", result.metadata.get("File Name"));
        addMetadataRow(results, "Source", result.metadata.get("Source"));
        addMetadataRow(results, "Extension", result.metadata.get("Extension"));
        addMetadataRow(results, "Size", result.metadata.get("Size"));
        addMetadataRow(results, "Detected MIME", result.metadata.get("Detected MIME"));
        addMetadataRow(results, "Signature", result.metadata.get("Signature"));
        addMetadataRow(results, "Width", result.metadata.get("Width"));
        addMetadataRow(results, "Height", result.metadata.get("Height"));

        results.getChildren().add(createSectionHeader("CAMERA & OPTICS HARDWARE"));
        addMetadataRow(results, "Camera Make", result.metadata.get("Camera Make"));
        addMetadataRow(results, "Camera Model", result.metadata.get("Camera Model"));
        addMetadataRow(results, "Software", result.metadata.get("Software"));
        addMetadataRow(results, "Exposure", result.metadata.get("Exposure"));
        addMetadataRow(results, "F-Number", result.metadata.get("F-Number"));
        addMetadataRow(results, "ISO", result.metadata.get("ISO"));

        results.getChildren().add(createSectionHeader("TIMESTAMPS & CHRONOLOGY"));
        addMetadataRow(results, "Date Taken", result.metadata.get("Date Taken"));
        addMetadataRow(results, "Original Date", result.metadata.get("Original Date"));

        results.getChildren().add(createSectionHeader("GEOLOCATION FORENSICS"));
        addMetadataRow(results, "GPS Status", result.hasGps ? "GPS coordinates detected" : "GPS not available");
        addMetadataRow(results, "Latitude", result.metadata.get("GPS Latitude"));
        addMetadataRow(results, "Longitude", result.metadata.get("GPS Longitude"));

        if (result.hasGps && result.latitude != null && result.longitude != null) {
            String coordinates = String.format(Locale.US, "%.6f, %.6f", result.latitude, result.longitude);
            addMetadataRow(results, "Coordinates", coordinates);

            Button mapButton = new Button("📍  Open Coordinates in Map  →");
            mapButton.getStyleClass().add("btn-accent");
            mapButton.setOnAction(e -> openUrl(
                    "https://www.google.com/maps?q=" + result.latitude + "," + result.longitude
            ));

            HBox buttonContainer = new HBox(mapButton);
            buttonContainer.setPadding(new Insets(6, 0, 0, 0));
            results.getChildren().add(buttonContainer);
        }
    }

    private void addMetadataRow(VBox results, String label, String value) {
        String display = value == null || value.isBlank() ? "—" : value;

        Label key = new Label(label);
        key.setMinWidth(160);
        key.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label val = new Label(display);
        val.setWrapText(true);
        val.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        HBox.setHgrow(val, Priority.ALWAYS);

        HBox row = new HBox(16, key, val);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("activity-row");

        results.getChildren().add(row);
    }

    private Label createSectionHeader(String text) {
        Label label = new Label("// " + text.toUpperCase() + " //");
        label.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 14 0 4 2; -fx-letter-spacing: 1.5px;");
        return label;
    }

    private Label createEmptyState(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        label.setPadding(new Insets(24, 0, 16, 0));
        return label;
    }

    private void openUrl(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            }
        } catch (Exception ignored) {}
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}