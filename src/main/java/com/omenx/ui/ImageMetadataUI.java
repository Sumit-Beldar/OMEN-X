package com.omenx.ui;

import com.omenx.osint.ImageMetadataScanner;

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
// Handles image selection and metadata display.
// =========================================================

import com.omenx.service.ScanService;

public class ImageMetadataUI {

    private static final String BG = "#0A0E12";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String GREEN = "#3DDC97";
    private static final String BLUE = "#5B9BD5";

    private final Runnable backAction;
    private final ScanService scanService;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public ImageMetadataUI(
            Runnable backAction
    ) {
        this(backAction, null);
    }

    public ImageMetadataUI(
            Runnable backAction,
            ScanService scanService
    ) {
        this.backAction = backAction;
        this.scanService = scanService;
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public VBox createView() {

        Label title =
                new Label(
                        "Image Metadata Scanner"
                );

        title.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 20px;" +
                " -fx-font-weight: bold;"
        );

        Button backBtn =
                createGhostButton(
                        "←  Dashboard"
                );

        backBtn.setOnAction(
                e -> backAction.run()
        );

        HBox header =
                new HBox(
                        16,
                        title,
                        backBtn
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                title,
                Priority.ALWAYS
        );

        Label dropIcon = new Label("📁");
        dropIcon.setStyle("-fx-font-size: 28px;");

        Label dropText = new Label("Drag & Drop image file here, or click to browse");
        dropText.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label dropSub = new Label("Supports JPG, PNG, GIF, TIFF, WEBP EXIF extraction");
        dropSub.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        Button selectButton = new Button("Choose Image File…");
        selectButton.getStyleClass().add("btn-primary");

        VBox dropZone = new VBox(8, dropIcon, dropText, dropSub, selectButton);
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setAlignment(Pos.CENTER);

        VBox results =
                new VBox(
                        8,
                        createEmptyState(
                                "Select or drop an image above to analyze EXIF metadata."
                        )
                );

        ScrollPane scroll =
                new ScrollPane(results);

        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );

        // Handler for processing file selection/drop
        java.util.function.Consumer<File> processFile = file -> {
            if (file == null) return;

            selectButton.setDisable(true);
            results.getChildren().setAll(
                    createEmptyState("Reading EXIF and image metadata…")
            );

            executor.submit(() -> {
                ImageMetadataScanner.Result result =
                        new ImageMetadataScanner().scan(file);

                Platform.runLater(() -> {
                    renderImageMetadataWithPreview(results, result, file);
                    selectButton.setDisable(false);
                });
            });
        };

        selectButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Image File");
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
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                processFile.accept(db.getFiles().get(0));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        VBox content =
                new VBox(
                        18,
                        header,
                        dropZone,
                        scroll
                );

        content.setPadding(
                new Insets(
                        28,
                        32,
                        24,
                        32
                )
        );

        content.setStyle(
                "-fx-background-color: " + BG + ";"
        );

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
                fileTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label pathLabel = new Label(file.getAbsolutePath());
                pathLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");

                VBox previewBox = new VBox(8, imgView, fileTitle, pathLabel);
                previewBox.setAlignment(Pos.CENTER);
                previewBox.getStyleClass().add("cyber-card");
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
                    createEmptyState(
                            "No metadata was found."
                    )
            );

            return;
        }

        results.getChildren().add(
                createSectionHeader(
                        "FILE INFORMATION"
                )
        );

        addMetadataRow(
                results,
                "File Name",
                result.metadata.get("File Name")
        );

        addMetadataRow(
                results,
                "Source",
                result.metadata.get("Source")
        );

        addMetadataRow(
                results,
                "Extension",
                result.metadata.get("Extension")
        );

        addMetadataRow(
                results,
                "Size",
                result.metadata.get("Size")
        );

        addMetadataRow(
                results,
                "Detected MIME",
                result.metadata.get("Detected MIME")
        );

        addMetadataRow(
                results,
                "Signature",
                result.metadata.get("Signature")
        );

        addMetadataRow(
                results,
                "Width",
                result.metadata.get("Width")
        );

        addMetadataRow(
                results,
                "Height",
                result.metadata.get("Height")
        );

        results.getChildren().add(
                createSectionHeader(
                        "CAMERA INFORMATION"
                )
        );

        addMetadataRow(
                results,
                "Camera Make",
                result.metadata.get("Camera Make")
        );

        addMetadataRow(
                results,
                "Camera Model",
                result.metadata.get("Camera Model")
        );

        addMetadataRow(
                results,
                "Software",
                result.metadata.get("Software")
        );

        addMetadataRow(
                results,
                "Exposure",
                result.metadata.get("Exposure")
        );

        addMetadataRow(
                results,
                "F-Number",
                result.metadata.get("F-Number")
        );

        addMetadataRow(
                results,
                "ISO",
                result.metadata.get("ISO")
        );

        results.getChildren().add(
                createSectionHeader(
                        "DATE & TIME"
                )
        );

        addMetadataRow(
                results,
                "Date Taken",
                result.metadata.get("Date Taken")
        );

        addMetadataRow(
                results,
                "Original Date",
                result.metadata.get("Original Date")
        );

        results.getChildren().add(
                createSectionHeader(
                        "GPS INFORMATION"
                )
        );

        addMetadataRow(
                results,
                "GPS Status",
                result.hasGps
                        ? "GPS coordinates detected"
                        : "GPS not available"
        );

        addMetadataRow(
                results,
                "Latitude",
                result.metadata.get("GPS Latitude")
        );

        addMetadataRow(
                results,
                "Longitude",
                result.metadata.get("GPS Longitude")
        );

        if (
                result.hasGps &&
                result.latitude != null &&
                result.longitude != null
        ) {

            String coordinates =
                    String.format(
                            Locale.US,
                            "%.6f, %.6f",
                            result.latitude,
                            result.longitude
                    );

            addMetadataRow(
                    results,
                    "Coordinates",
                    coordinates
            );

            Button mapButton =
                    new Button(
                            "Open Coordinates in Map  →"
                    );

            mapButton.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: " + BLUE + ";" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
            );

            mapButton.setOnAction(
                    e -> openUrl(
                            "https://www.google.com/maps?q="
                                    + result.latitude
                                    + ","
                                    + result.longitude
                    )
            );

            results.getChildren().add(
                    mapButton
            );
        }
    }

    // =========================================================
    // METADATA ROW
    // =========================================================

    private void addMetadataRow(
            VBox results,
            String label,
            String value
    ) {

        String display =
                value == null || value.isBlank()
                        ? "—"
                        : value;

        Label key =
                new Label(label);

        key.setMinWidth(150);

        key.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        Label val =
                new Label(display);

        val.setWrapText(true);

        val.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 12px;"
        );

        HBox.setHgrow(
                val,
                Priority.ALWAYS
        );

        HBox row =
                new HBox(
                        16,
                        key,
                        val
                );

        row.setAlignment(
                Pos.TOP_LEFT
        );

        row.setPadding(
                new Insets(
                        9,
                        14,
                        9,
                        14
                )
        );

        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );

        results.getChildren().add(row);
    }

    // =========================================================
    // SECTION HEADER
    // =========================================================

    private Label createSectionHeader(
            String text
    ) {

        Label label =
                new Label(text);

        label.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 12px;" +
                " -fx-font-weight: bold;"
        );

        label.setPadding(
                new Insets(
                        14,
                        0,
                        4,
                        0
                )
        );

        return label;
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private Label createEmptyState(
            String text
    ) {

        Label label =
                new Label(text);

        label.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 13px;"
        );

        label.setPadding(
                new Insets(
                        28,
                        0,
                        20,
                        0
                )
        );

        return label;
    }

    // =========================================================
    // GHOST BUTTON
    // =========================================================

    private Button createGhostButton(
            String text
    ) {

        Button button =
                new Button(text);

        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 14;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // =========================================================
    // OPEN URL
    // =========================================================

    private void openUrl(
            String url
    ) {

        try {

            if (
                    java.awt.Desktop
                            .isDesktopSupported()
            ) {

                java.awt.Desktop
                        .getDesktop()
                        .browse(
                                java.net.URI.create(url)
                        );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {

        executor.shutdownNow();
    }
}