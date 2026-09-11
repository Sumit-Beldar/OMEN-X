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
// Tactical Visual Reconnaissance & Reverse Image Intelligence
// =========================================================

public class ReverseImageUI {

    // =========================================================
    // COLORS (Tactical Crimson Unified)
    // =========================================================

    private static final String BG      = "#090C12";
    private static final String PANEL   = "#111520";
    private static final String SURFACE = "#141824";
    private static final String BORDER  = "#1C2234";
    private static final String TEXT    = "#F1F5F9";
    private static final String MUTED   = "#788698";
    private static final String GREEN   = "#22C55E";
    private static final String RED     = "#DC2626";

    // =========================================================
    // ACTIONS & SERVICES
    // =========================================================

    private final Runnable backAction;
    private final ScanService scanService;
    private final ReverseImageScanner scanner;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private AppHeader header;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReverseImageUI(
            Runnable backAction,
            ScanService scanService
    ) {
        this.backAction = backAction;
        this.scanService = scanService;
        this.scanner = new ReverseImageScanner();
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public BorderPane createView() {

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // ── Standardized Header ──
        header = new AppHeader(
                ">>",
                "REVERSE IMAGE",
                "Identify identical and visually matching imagery across search engines",
                backAction
        );
        root.setTop(header);

        // ── Selected Image Preview Box ──
        ImageView preview = new ImageView();
        preview.setFitWidth(220);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);

        Label fileName = new Label("No image selected");
        fileName.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        VBox imagePreview = new VBox(10, preview, fileName);
        imagePreview.setAlignment(Pos.CENTER);
        imagePreview.setPadding(new Insets(20));
        imagePreview.setMinHeight(240);
        imagePreview.getStyleClass().add("drop-zone");

        // ── Buttons ──
        Button selectButton = new Button("Choose Image File…");
        selectButton.setPrefHeight(42);
        selectButton.getStyleClass().add("btn-ghost");

        Button searchButton = new Button("EXECUTE SEARCH");
        searchButton.setPrefHeight(42);
        searchButton.setDisable(true);
        searchButton.getStyleClass().add("btn-primary");

        HBox buttons = new HBox(12, selectButton, searchButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        // ── Status ──
        Label status = new Label("Ready — select an image file to begin.");
        status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        // ── Results Header ──
        Label resultsTitle = new Label("Search Results");
        resultsTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label resultCount = new Label("0 matches");
        resultCount.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox resultsHeader = new HBox(12, resultsTitle, resultCount);
        resultsHeader.setAlignment(Pos.CENTER_LEFT);

        // ── Results Container ──
        VBox results = new VBox(12);
        results.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(results);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        final File[] selectedImage = new File[1];

        selectButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Image for Reverse Search");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Image Files",
                            "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp", "*.bmp"
                    )
            );

            File file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) return;

            selectedImage[0] = file;

            try {
                Image image = new Image(file.toURI().toString(), 220, 160, true, true);
                preview.setImage(image);
            } catch (Exception ex) {
                preview.setImage(null);
            }

            fileName.setText(file.getName());
            status.setText("Image selected — ready to search");
            status.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px;");
            header.setStatus(AppHeader.StatusType.READY, "IMAGE LOADED");
            searchButton.setDisable(false);
            results.getChildren().clear();
            resultCount.setText("0 matches");
        });

        // ── Search Action ──
        searchButton.setOnAction(e -> {
            File file = selectedImage[0];
            if (file == null) {
                status.setText("Please select an image first.");
                return;
            }

            searchButton.setDisable(true);
            selectButton.setDisable(true);
            status.setText("Uploading image and searching the Internet...");
            status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            header.setStatus(AppHeader.StatusType.SCANNING, "SEARCHING ENGINES");

            results.getChildren().clear();
            resultCount.setText("Searching...");

            executor.submit(() -> {
                try {
                    List<ReverseImageScanner.Match> matches = scanner.scan(file);

                    Platform.runLater(() -> {
                        renderResults(results, resultCount, status, matches);
                        scanService.saveScan(
                                "Reverse Image",
                                file.getName(),
                                matches.size(),
                                java.time.LocalTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                )
                        );
                        searchButton.setDisable(false);
                        selectButton.setDisable(false);
                        header.setStatus(AppHeader.StatusType.READY, "COMPLETE");
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setText("Search failed: " + cleanError(ex.getMessage()));
                        status.setStyle("-fx-text-fill: " + RED + "; -fx-font-size: 12px;");
                        resultCount.setText("Search failed");
                        header.setStatus(AppHeader.StatusType.ERROR, "FAILED");
                        searchButton.setDisable(false);
                        selectButton.setDisable(false);
                    });
                }
            });
        });

        VBox imagePanel = new VBox(
                14,
                imagePreview,
                buttons,
                status
        );
        imagePanel.setPadding(new Insets(18));
        imagePanel.getStyleClass().add("cyber-card");

        VBox content = new VBox(
                16,
                imagePanel,
                resultsHeader,
                scrollPane
        );

        content.setPadding(new Insets(20, 28, 28, 28));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

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

        if (matches == null || matches.isEmpty()) {
            resultCount.setText("0 matches");
            status.setText("Search completed — no visually similar results found.");
            status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

            Label empty = new Label("No matching images were returned.");
            empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
            empty.setPadding(new Insets(25));
            results.getChildren().add(empty);
            return;
        }

        resultCount.setText(matches.size() + " matches");
        status.setText("Search completed successfully.");
        status.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px;");

        int rank = 1;
        for (ReverseImageScanner.Match match : matches) {
            results.getChildren().add(createResultCard(rank, match));
            rank++;
        }
    }

    // =========================================================
    // RESULT CARD
    // =========================================================

    private HBox createResultCard(int rank, ReverseImageScanner.Match match) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(130);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        String imageUrl = match.getThumbnail();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = match.getImage();
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                imageView.setImage(new Image(imageUrl, 130, 100, true, true, true));
            } catch (Exception ignored) {}
        }

        VBox imageBox = new VBox(imageView);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setPrefWidth(140);

        Label rankLabel = new Label("#" + rank);
        rankLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");

        String title = match.getTitle();
        if (title == null || title.isBlank()) {
            title = "Untitled result";
        }

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        String domain = match.getDomain();
        if (domain == null || domain.isBlank()) {
            domain = match.getSource();
        }

        Label domainLabel = new Label(domain == null ? "" : domain);
        domainLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        Label linkLabel = new Label(match.getLink());
        linkLabel.setWrapText(true);
        linkLabel.setMaxWidth(600);
        linkLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");

        Button openButton = new Button("Open Source  →");
        openButton.getStyleClass().add("btn-accent");
        openButton.setOnAction(e -> openUrl(match.getLink()));

        VBox details = new VBox(
                6,
                rankLabel,
                titleLabel,
                domainLabel,
                linkLabel,
                openButton
        );
        details.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox card = new HBox(18, imageBox, details);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("cyber-card");

        return card;
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.err.println("Unable to open URL: " + ex.getMessage());
        }
    }

    private String cleanError(String message) {
        if (message == null || message.isBlank()) return "Unknown error";
        return message.replace("\n", " ").replace("\r", " ");
    }

    public void shutdown() {
        executor.shutdownNow();
        scanner.shutdown();
    }
}