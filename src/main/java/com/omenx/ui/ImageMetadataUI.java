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

public class ImageMetadataUI {

    private static final String BG = "#0A0E12";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String GREEN = "#3DDC97";
    private static final String BLUE = "#5B9BD5";

    private final Runnable backAction;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public ImageMetadataUI(
            Runnable backAction
    ) {

        this.backAction = backAction;
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

        Button selectButton =
                new Button(
                        "Select Image…"
                );

        selectButton.setPrefHeight(40);

        selectButton.setStyle(
                "-fx-background-color: " + GREEN + ";" +
                "-fx-text-fill: #0A0E12;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
        );

        Label fileLabel =
                new Label(
                        "No image selected"
                );

        fileLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox results =
                new VBox(
                        8,
                        createEmptyState(
                                "Select an image to begin metadata analysis."
                        )
                );

        ScrollPane scroll =
                new ScrollPane(results);

        scroll.setFitToWidth(true);

        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;"
        );

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );

        // =====================================================
        // IMAGE SELECTION
        // =====================================================

        selectButton.setOnAction(e -> {

            FileChooser chooser =
                    new FileChooser();

            chooser.setTitle(
                    "Select Image"
            );

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Image Files",
                            "*.jpg",
                            "*.jpeg",
                            "*.png",
                            "*.gif",
                            "*.tif",
                            "*.tiff",
                            "*.webp"
                    )
            );

            Window window =
                    selectButton
                            .getScene()
                            .getWindow();

            File file =
                    chooser.showOpenDialog(
                            window
                    );

            if (file == null) {
                return;
            }

            fileLabel.setText(
                    file.getName()
            );

            selectButton.setDisable(true);

            results.getChildren().setAll(
                    createEmptyState(
                            "Reading EXIF and image metadata…"
                    )
            );

            executor.submit(() -> {

                ImageMetadataScanner.Result result =
                        new ImageMetadataScanner()
                                .scan(file);

                Platform.runLater(() -> {

                    renderImageMetadata(
                            results,
                            result
                    );

                    selectButton.setDisable(false);
                });
            });
        });

        VBox content =
                new VBox(
                        18,
                        header,
                        new HBox(
                                12,
                                selectButton,
                                fileLabel
                        ),
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

    private void renderImageMetadata(
            VBox results,
            ImageMetadataScanner.Result result
    ) {

        results.getChildren().clear();

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