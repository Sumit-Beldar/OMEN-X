package com.omenx.ui;

import com.omenx.osint.ReverseImageScanner;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// =========================================================
// REVERSE IMAGE UI
// Handles image selection and reverse-image investigation.
// =========================================================

public class ReverseImageUI {

    private static final String BG = "#0A0E12";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String PURPLE = "#8B5CF6";

    private final Runnable backAction;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReverseImageUI(
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
                        "Reverse Image Scanner"
                );

        title.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 20px;" +
                " -fx-font-weight: bold;"
        );

        Button backButton =
                createGhostButton(
                        "←  Dashboard"
                );

        backButton.setOnAction(
                e -> backAction.run()
        );

        HBox header =
                new HBox(
                        16,
                        title,
                        backButton
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                title,
                Priority.ALWAYS
        );

        Label description =
                new Label(
                        "Upload an image to investigate its source and location."
                );

        description.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        Button selectButton =
                new Button(
                        "Select Image…"
                );

        selectButton.setPrefHeight(40);

        selectButton.setStyle(
                "-fx-background-color: " + PURPLE + ";" +
                "-fx-text-fill: white;" +
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
                        10,
                        createEmptyState(
                                "Select an image to begin reverse image analysis."
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
        // SELECT IMAGE
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

            File file =
                    chooser.showOpenDialog(
                            selectButton
                                    .getScene()
                                    .getWindow()
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
                            "Analyzing image…"
                    )
            );

            executor.submit(() -> {

                try {

                    ReverseImageScanner scanner =
                            new ReverseImageScanner();

                    ReverseImageScanner.Result result =
                            scanner.scan(file);

                    Platform.runLater(() -> {

                        renderResults(
                                results,
                                result
                        );

                        selectButton.setDisable(false);
                    });

                } catch (Exception ex) {

                    Platform.runLater(() -> {

                        results.getChildren().setAll(
                                createEmptyState(
                                        "Reverse image scan failed: "
                                                + ex.getMessage()
                                )
                        );

                        selectButton.setDisable(false);
                    });
                }
            });
        });

        // =====================================================
        // CONTENT
        // =====================================================

        HBox controls =
                new HBox(
                        12,
                        selectButton,
                        fileLabel
                );

        controls.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox content =
                new VBox(
                        18,
                        header,
                        description,
                        controls,
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
    // RENDER RESULTS
    // =========================================================

    private void renderResults(
            VBox results,
            ReverseImageScanner.Result result
    ) {

        results.getChildren().clear();

        results.getChildren().add(
                createSectionHeader(
                        "REVERSE IMAGE ANALYSIS"
                )
        );

        if (result == null) {

            results.getChildren().add(
                    createEmptyState(
                            "No result was returned."
                    )
            );

            return;
        }

        Label resultLabel =
                new Label(
                        result.toString()
                );

        resultLabel.setWrapText(true);

        resultLabel.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 13px;"
        );

        VBox resultBox =
                new VBox(
                        resultLabel
                );

        resultBox.setPadding(
                new Insets(16)
        );

        resultBox.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );

        results.getChildren().add(
                resultBox
        );
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
    // SHUTDOWN
    // =========================================================

    public void shutdown() {

        executor.shutdownNow();
    }
}