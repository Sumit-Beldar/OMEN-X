package com.omenx.frontend;

import com.omenx.backend.osint.ReverseImageScanner;
import com.omenx.backend.service.ScanService;

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
// Dark Glassmorphic Visual Reconnaissance & Reverse Image Intelligence
// =========================================================
public class ReverseImageUI {

    private static final String BG      = "#060810";
    private static final String TEXT    = "#F1F5F9";
    private static final String MUTED   = "#64748B";
    private static final String CYAN    = "#22D3EE";
    private static final String GREEN   = "#10B981";
    private static final String RED     = "#EF4444";

    private final Runnable backAction;
    private final ScanService scanService;
    private final ReverseImageScanner scanner;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private AppHeader header;

    public ReverseImageUI(
            Runnable backAction,
            ScanService scanService
    ) {
        this.backAction = backAction;
        this.scanService = scanService;
        this.scanner = new ReverseImageScanner();
    }

    public BorderPane createView() {

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // ── Standardized Header ──
        header = new AppHeader(
                ModuleIcons.get("Reverse Image"),
                "REVERSE IMAGE SEARCH",
                "Identify identical, visually matching imagery & source domains across search engines",
                backAction
        );
        root.setTop(header);

        // ── Selected Image Preview Box ──
        ImageView preview = new ImageView();
        preview.setFitWidth(220);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);

        Label fileName = new Label("No image selected");
        fileName.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

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
        Label status = new Label("Ready — select an image file to begin reverse visual search.");
        status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        // ── Results Header Pattern ──
        HBox resultsHeader = AppHeader.createPanelHeader(
                "◈",
                "REVERSE IMAGE MATCHES & CITATIONS",
                "READY",
                AppHeader.StatusType.READY,
                "0 MATCHES"
        );

        Label resultCount = new Label("0 matches");
        resultCount.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        // ── Results Box ──
        VBox resultsBox = new VBox(12);
        resultsBox.setPadding(new Insets(8, 16, 16, 16));

        Label placeholder = new Label("Upload an image and run search to see visually matching web results.");
        placeholder.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        resultsBox.getChildren().add(placeholder);

        VBox resultsCardWrapper = new VBox(4, resultsHeader, resultsBox);
        resultsCardWrapper.getStyleClass().add("glass-panel");

        ScrollPane scroll = new ScrollPane(resultsCardWrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Engine Quick Links ──
        Label engineLabel = new Label("// QUICK ENGINE REDIRECTS //");
        engineLabel.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");

        Button googleButton = createEngineButton("Google Images", "https://images.google.com");
        Button yandexButton = createEngineButton("Yandex Visual", "https://yandex.com/images");
        Button bingButton   = createEngineButton("Bing Visual", "https://www.bing.com/visualsearch");
        Button tinEyeButton = createEngineButton("TinEye", "https://tineye.com");

        HBox engineRow = new HBox(10, engineLabel, googleButton, yandexButton, bingButton, tinEyeButton);
        engineRow.setAlignment(Pos.CENTER_LEFT);
        engineRow.setPadding(new Insets(10, 16, 10, 16));
        engineRow.getStyleClass().add("glass-panel");

        // ── Selected File Reference ──
        final File[] chosenFile = {null};

        // File Chooser Handler
        selectButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Image for Reverse Search");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Images (*.png, *.jpg, *.jpeg, *.webp)",
                            "*.png", "*.jpg", "*.jpeg", "*.webp"
                    )
            );

            File file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                chosenFile[0] = file;
                fileName.setText(file.getName() + "  (" + (file.length() / 1024) + " KB)");
                searchButton.setDisable(false);
                status.setText("Image loaded: " + file.getName() + " — ready to execute reverse search.");
                status.setStyle("-fx-text-fill: " + CYAN + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

                try {
                    preview.setImage(new Image(file.toURI().toString()));
                } catch (Exception ex) {
                    status.setText("Could not preview image: " + ex.getMessage());
                    status.setStyle("-fx-text-fill: " + RED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
                }
            }
        });

        // Search Action
        searchButton.setOnAction(e -> {
            if (chosenFile[0] == null) return;

            searchButton.setDisable(true);
            selectButton.setDisable(true);
            status.setText("Searching Google Images lens index for visually matching results…");
            status.setStyle("-fx-text-fill: " + CYAN + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
            header.setStatus(AppHeader.StatusType.SCANNING, "SEARCHING LENS");

            resultsBox.getChildren().clear();
            Label searching = new Label("Querying reverse visual index across search providers…");
            searching.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
            resultsBox.getChildren().add(searching);

            executor.submit(() -> {
                try {
                    List<ReverseImageScanner.Match> matches =
                            scanner.scan(chosenFile[0]);

                    Platform.runLater(() -> {
                        resultsBox.getChildren().clear();
                        searchButton.setDisable(false);
                        selectButton.setDisable(false);

                        if (matches.isEmpty()) {
                            Label noResults = new Label("No matching results found across search engines for this image.");
                            noResults.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
                            resultsBox.getChildren().add(noResults);
                            resultCount.setText("0 matches");
                            status.setText("Search completed — 0 matches identified.");
                            status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
                            header.setStatus(AppHeader.StatusType.READY, "0 MATCHES");
                        } else {
                            resultCount.setText(matches.size() + " matches");
                            status.setText("Search complete — " + matches.size() + " visually matching results identified.");
                            status.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
                            header.setStatus(AppHeader.StatusType.READY, matches.size() + " MATCHES");

                            int rank = 1;
                            for (ReverseImageScanner.Match m : matches) {
                                resultsBox.getChildren().add(createMatchCard(m, rank++));
                            }
                        }

                        if (scanService != null && chosenFile[0] != null) {
                            scanService.saveScan(
                                    "Reverse Image",
                                    chosenFile[0].getName(),
                                    matches.size(),
                                    java.time.LocalTime.now().format(
                                            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                    )
                            );
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        resultsBox.getChildren().clear();
                        Label errorLabel = new Label("Error executing reverse search: " + cleanError(ex.getMessage()));
                        errorLabel.setStyle("-fx-text-fill: " + RED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
                        resultsBox.getChildren().add(errorLabel);
                        status.setText("Reverse image search failed: " + cleanError(ex.getMessage()));
                        status.setStyle("-fx-text-fill: " + RED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
                        header.setStatus(AppHeader.StatusType.ERROR, "ERROR");
                        searchButton.setDisable(false);
                        selectButton.setDisable(false);
                    });
                }
            });
        });

        VBox content = new VBox(
                16,
                imagePreview,
                buttons,
                status,
                engineRow,
                scroll
        );
        content.setPadding(new Insets(20, 26, 26, 26));

        root.setCenter(content);
        return root;
    }

    private Button createEngineButton(String name, String url) {
        Button btn = new Button(name + "  ↗");
        btn.getStyleClass().add("btn-ghost");
        btn.setStyle("-fx-padding: 5 12 5 12; -fx-font-size: 10px;");
        btn.setOnAction(e -> openUrl(url));
        return btn;
    }

    private HBox createMatchCard(ReverseImageScanner.Match match, int rank) {
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
        rankLabel.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");

        String title = match.getTitle();
        if (title == null || title.isBlank()) {
            title = "Untitled result";
        }

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");

        String domain = match.getDomain();
        if (domain == null || domain.isBlank()) {
            domain = match.getSource();
        }

        Label domainLabel = new Label(domain == null ? "" : domain);
        domainLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");

        Label linkLabel = new Label(match.getLink());
        linkLabel.setWrapText(true);
        linkLabel.setMaxWidth(600);
        linkLabel.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

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
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("activity-row");

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