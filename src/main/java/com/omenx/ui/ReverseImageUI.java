package com.omenx.ui;

import com.omenx.ReverseImageScanner;
import com.omenx.service.ScanService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// =========================================================
// REVERSE IMAGE UI
//
// Select image
//      ↓
// ReverseImageScanner
//      ↓
// Internet search
//      ↓
// Results displayed inside OMEN-X
// =========================================================

public class ReverseImageUI {

    // =========================================================
    // COLORS
    // =========================================================

    private static final String BG = "#0A0E12";
    private static final String PANEL = "#11161C";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String PURPLE = "#8B5CF6";
    private static final String GREEN = "#4ADE80";
    private static final String RED = "#F87171";

    // =========================================================
    // ACTIONS
    // =========================================================

    private final Runnable backAction;
    private final ScanService scanService;

    // =========================================================
    // SCANNER
    // =========================================================

    private final ReverseImageScanner scanner;

    // =========================================================
    // EXECUTOR
    // =========================================================

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReverseImageUI(
            Runnable backAction,
            ScanService scanService
    ) {

        this.backAction = backAction;
        this.scanService = scanService;

        this.scanner =
                new ReverseImageScanner();
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public BorderPane createView() {

        BorderPane root =
                new BorderPane();

        root.setStyle(
                "-fx-background-color: " + BG + ";"
        );

        // =====================================================
        // HEADER
        // =====================================================

        Label title =
                new Label(
                        "Reverse Image Search"
                );

        title.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 20px;" +
                " -fx-font-weight: bold;"
        );

        Button backButton =
                new Button(
                        "←  Dashboard"
                );

        backButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 7 14;" +
                "-fx-cursor: hand;"
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

        header.setPadding(
                new Insets(
                        24,
                        32,
                        18,
                        32
                )
        );

        root.setTop(header);

        // =====================================================
        // SUBTITLE
        // =====================================================

        Label subtitle =
                new Label(
                        "Search the Internet for visually similar images."
                );

        subtitle.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        // =====================================================
        // SELECTED IMAGE
        // =====================================================

        ImageView preview =
                new ImageView();

        preview.setFitWidth(220);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);

        Label fileName =
                new Label(
                        "No image selected"
                );

        fileName.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        VBox imagePreview =
                new VBox(
                        10,
                        preview,
                        fileName
                );

        imagePreview.setAlignment(
                Pos.CENTER
        );

        imagePreview.setPadding(
                new Insets(20)
        );

        imagePreview.setMinHeight(210);

        imagePreview.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // =====================================================
        // BUTTONS
        // =====================================================

        Button selectButton =
                new Button(
                        "Choose Image File…"
                );

        selectButton.setPrefHeight(42);
        selectButton.getStyleClass().add("btn-ghost");

        Button searchButton =
                new Button(
                        "Search Internet"
                );

        searchButton.setPrefHeight(42);
        searchButton.setDisable(true);
        searchButton.getStyleClass().add("btn-primary");

        HBox buttons =
                new HBox(
                        12,
                        selectButton,
                        searchButton
                );

        buttons.setAlignment(
                Pos.CENTER_LEFT
        );

        // =====================================================
        // STATUS
        // =====================================================

        Label status =
                new Label(
                        "Ready"
                );

        status.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        // =====================================================
        // RESULTS TITLE
        // =====================================================

        Label resultsTitle =
                new Label(
                        "Search Results"
                );

        resultsTitle.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 15px;" +
                " -fx-font-weight: bold;"
        );

        Label resultCount =
                new Label(
                        "0 matches"
                );

        resultCount.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        HBox resultsHeader =
                new HBox(
                        12,
                        resultsTitle,
                        resultCount
                );

        resultsHeader.setAlignment(
                Pos.CENTER_LEFT
        );

        // =====================================================
        // RESULTS CONTAINER
        // =====================================================

        VBox results =
                new VBox(12);

        results.setPadding(
                new Insets(4)
        );

        // =====================================================
        // SCROLL PANE
        // =====================================================

        ScrollPane scrollPane =
                new ScrollPane(
                        results
                );

        scrollPane.setFitToWidth(true);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";"
        );

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );

        // =====================================================
        // SELECT IMAGE
        // =====================================================

        final File[] selectedImage =
                new File[1];

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
                            "*.webp",
                            "*.bmp"
                    )
            );

            File file =
                    chooser.showOpenDialog(
                            root.getScene()
                                    .getWindow()
                    );

            if (file == null) {
                return;
            }

            selectedImage[0] = file;

            // -------------------------------------------------
            // PREVIEW
            // -------------------------------------------------

            try {

                Image image =
                        new Image(
                                file.toURI()
                                        .toString(),
                                220,
                                160,
                                true,
                                true
                        );

                preview.setImage(image);

            } catch (Exception ex) {

                preview.setImage(null);
            }

            fileName.setText(
                    file.getName()
            );

            status.setText(
                    "Image selected — ready to search"
            );

            status.setStyle(
                    "-fx-text-fill: " + GREEN +
                    "; -fx-font-size: 12px;"
            );

            searchButton.setDisable(false);

            results.getChildren().clear();

            resultCount.setText(
                    "0 matches"
            );
        });

        // =====================================================
        // SEARCH
        // =====================================================

        searchButton.setOnAction(e -> {

            File file =
                    selectedImage[0];

            if (file == null) {

                status.setText(
                        "Please select an image first."
                );

                return;
            }

            // -------------------------------------------------
            // DISABLE BUTTON
            // -------------------------------------------------

            searchButton.setDisable(true);
            selectButton.setDisable(true);

            status.setText(
                    "Uploading image and searching the Internet..."
            );

            status.setStyle(
                    "-fx-text-fill: " + PURPLE +
                    "; -fx-font-size: 12px;"
            );

            results.getChildren().clear();

            resultCount.setText(
                    "Searching..."
            );

            // -------------------------------------------------
            // BACKGROUND SEARCH
            // -------------------------------------------------

            executor.submit(() -> {

                try {

                    List<ReverseImageScanner.Match>
                            matches =
                            scanner.scan(file);

                    Platform.runLater(() -> {

                        renderResults(
                                results,
                                resultCount,
                                status,
                                matches
                        );
                        // Save reverse-image scan
    scanService.saveScan(
            "Reverse Image",
            file.getName(),
            matches.size(),
            java.time.LocalTime.now()
                    .format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    )
    );
                        searchButton.setDisable(false);
                        selectButton.setDisable(false);
                    });

                } catch (Exception ex) {

                    Platform.runLater(() -> {

                        status.setText(
                                "Search failed: "
                                        + cleanError(
                                                ex.getMessage()
                                        )
                        );

                        status.setStyle(
                                "-fx-text-fill: " + RED +
                                "; -fx-font-size: 12px;"
                        );

                        resultCount.setText(
                                "Search failed"
                        );

                        searchButton.setDisable(false);
                        selectButton.setDisable(false);
                    });
                }
            });
        });

        // =====================================================
        // IMAGE PANEL
        // =====================================================

        VBox imagePanel =
                new VBox(
                        15,
                        imagePreview,
                        buttons,
                        status
                );

        imagePanel.setPadding(
                new Insets(18)
        );

        imagePanel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        VBox content =
                new VBox(
                        16,
                        subtitle,
                        imagePanel,
                        resultsHeader,
                        scrollPane
                );

        content.setPadding(
                new Insets(
                        10,
                        32,
                        32,
                        32
                )
        );

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );

        root.setCenter(content);

        return root;
    }

    // =========================================================
    // RENDER RESULTS
    // =========================================================

    private void renderResults(
            VBox results,
            Label resultCount,
            Label status,
            List<ReverseImageScanner.Match> matches
    ) {

        results.getChildren().clear();

        if (matches == null ||
                matches.isEmpty()) {

            resultCount.setText(
                    "0 matches"
            );

            status.setText(
                    "Search completed — no results found."
            );

            status.setStyle(
                    "-fx-text-fill: " + MUTED +
                    "; -fx-font-size: 12px;"
            );

            Label empty =
                    new Label(
                            "No matching images were returned."
                    );

            empty.setStyle(
                    "-fx-text-fill: " + MUTED +
                    "; -fx-font-size: 13px;"
            );

            empty.setPadding(
                    new Insets(25)
            );

            results.getChildren().add(
                    empty
            );

            return;
        }

        resultCount.setText(
                matches.size()
                        + " matches"
        );

        status.setText(
                "Search completed successfully."
        );

        status.setStyle(
                "-fx-text-fill: " + GREEN +
                "; -fx-font-size: 12px;"
        );

        int rank = 1;

        for (
                ReverseImageScanner.Match match
                : matches
        ) {

            results.getChildren().add(
                    createResultCard(
                            rank,
                            match
                    )
            );

            rank++;
        }
    }

    // =========================================================
    // RESULT CARD
    // =========================================================

    private HBox createResultCard(
            int rank,
            ReverseImageScanner.Match match
    ) {

        // =====================================================
        // THUMBNAIL
        // =====================================================

        ImageView imageView =
                new ImageView();

        imageView.setFitWidth(130);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        String imageUrl =
                match.getThumbnail();

        if (imageUrl == null ||
                imageUrl.isBlank()) {

            imageUrl =
                    match.getImage();
        }

        if (imageUrl != null &&
                !imageUrl.isBlank()) {

            try {

                imageView.setImage(
                        new Image(
                                imageUrl,
                                130,
                                100,
                                true,
                                true,
                                true
                        )
                );

            } catch (Exception ignored) {
            }
        }

        VBox imageBox =
                new VBox(
                        imageView
                );

        imageBox.setAlignment(
                Pos.CENTER
        );

        imageBox.setPrefWidth(140);

        // =====================================================
        // RANK
        // =====================================================

        Label rankLabel =
                new Label(
                        "#" + rank
                );

        rankLabel.setStyle(
                "-fx-text-fill: " + PURPLE +
                "; -fx-font-size: 11px;" +
                " -fx-font-weight: bold;"
        );

        // =====================================================
        // TITLE
        // =====================================================

        String title =
                match.getTitle();

        if (title == null ||
                title.isBlank()) {

            title =
                    "Untitled result";
        }

        Label titleLabel =
                new Label(
                        title
                );

        titleLabel.setWrapText(true);

        titleLabel.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 14px;" +
                " -fx-font-weight: bold;"
        );

        // =====================================================
        // DOMAIN
        // =====================================================

        String domain =
                match.getDomain();

        if (domain == null ||
                domain.isBlank()) {

            domain =
                    match.getSource();
        }

        Label domainLabel =
                new Label(
                        domain == null
                                ? ""
                                : domain
                );

        domainLabel.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 11px;"
        );

        // =====================================================
        // LINK
        // =====================================================

        Label linkLabel =
                new Label(
                        match.getLink()
                );

        linkLabel.setWrapText(true);

        linkLabel.setMaxWidth(600);

        linkLabel.setStyle(
                "-fx-text-fill: #7C9CF5;" +
                "-fx-font-size: 11px;"
        );

        // =====================================================
        // OPEN BUTTON
        // =====================================================

        Button openButton =
                new Button(
                        "Open Source"
                );

        openButton.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 6 12;" +
                "-fx-cursor: hand;"
        );

        openButton.setOnAction(e ->
                openUrl(
                        match.getLink()
                )
        );

        // =====================================================
        // DETAILS
        // =====================================================

        VBox details =
                new VBox(
                        6,
                        rankLabel,
                        titleLabel,
                        domainLabel,
                        linkLabel,
                        openButton
                );

        details.setAlignment(
                Pos.TOP_LEFT
        );

        HBox.setHgrow(
                details,
                Priority.ALWAYS
        );

        // =====================================================
        // CARD
        // =====================================================

        HBox card =
                new HBox(
                        18,
                        imageBox,
                        details
                );

        card.setAlignment(
                Pos.TOP_LEFT
        );

        card.setPadding(
                new Insets(16)
        );

        card.setMaxWidth(
                Double.MAX_VALUE
        );

        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 9px;" +
                "-fx-background-radius: 9px;"
        );

        return card;
    }

    // =========================================================
    // OPEN URL
    // =========================================================

    private void openUrl(
            String url
    ) {

        if (url == null ||
                url.isBlank()) {

            return;
        }

        try {

            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop().browse(
                        new URI(url)
                );
            }

        } catch (Exception ex) {

            System.err.println(
                    "Unable to open URL: "
                            + ex.getMessage()
            );
        }
    }

    // =========================================================
    // CLEAN ERROR
    // =========================================================

    private String cleanError(
            String message
    ) {

        if (message == null ||
                message.isBlank()) {

            return "Unknown error";
        }

        return message
                .replace("\n", " ")
                .replace("\r", " ");
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {

        executor.shutdownNow();

        scanner.shutdown();
    }
}