package com.omenx;

import com.omenx.osint.UsernameScanner;
import com.omenx.osint.DomainScanner;
import com.omenx.osint.EmailScanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    // ====================== COLOR SYSTEM ======================
    private static final String BG          = "#0A0E12";
    private static final String PANEL       = "#11161C";
    private static final String SURFACE     = "#161C23";
    private static final String SURFACE_2   = "#1A2129";
    private static final String BORDER      = "#1E262F";
    private static final String BORDER_SOFT = "#252D37";
    private static final String TEXT        = "#E8EEF4";
    private static final String MUTED       = "#6B7785";
    private static final String MUTED_2     = "#8A96A3";
    private static final String GREEN       = "#3DDC97";
    private static final String RED         = "#FF6B6B";
    private static final String YELLOW      = "#F0C75E";
    private static final String BLUE        = "#5B9BD5";
    private static final String PURPLE      = "#A78BFA";
    private static final String CYAN        = "#56B6C2";

    // ====================== STATE ======================
    private final String[] selectedType = {"Username"};
    private final List<ScanRecord> history = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private BorderPane root;
    private VBox dashboardView;
    private VBox scanView;

    // Scan view controls
    private Label selectedTypeLabel;
    private TextField targetField;
    private Button searchButton;
    private Button clearButton;
    private Button exportButton;
    private Label foundValue;
    private Label notFoundValue;
    private Label unknownValue;
    private Label scannedValue;
    private Label resultsTarget;
    private VBox resultsContainer;
    private ScrollPane resultsScroll;
    private Label statusBarLabel;
    private Label lastScanLabel;

    // Dashboard labels that update
    private Label totalScansLabel;
    private Label findingsLabel;
    private VBox recentActivityBox;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // Build both views
        dashboardView = createDashboard();
        scanView = createScanView();

        root.setCenter(dashboardView);

        Scene scene = new Scene(root, 1240, 820);
        stage.setTitle("OMEN-X  •  Open Source Intelligence");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> executor.shutdownNow());
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    private VBox createDashboard() {
        // Header
        Label logo = new Label("OMEN-X");
        logo.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 28px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label tagline = new Label("Open Source Intelligence Platform");
        tagline.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        Label status = new Label("●  All systems operational");
        status.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox headerRight = new HBox(status);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(20, new VBox(4, logo, tagline), headerRight);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        // Overview metrics
        totalScansLabel = new Label("0");
        findingsLabel = new Label("0");
        Label modulesLabel = new Label("5");
        Label statusLabel = new Label("Online");

        HBox metrics = new HBox(12,
                createMetricCard("TOTAL SCANS", totalScansLabel, "Investigations run", BLUE),
                createMetricCard("FINDINGS", findingsLabel, "Potential matches", GREEN),
                createMetricCard("MODULES", modulesLabel, "Intelligence sources", PURPLE),
                createMetricCard("STATUS", statusLabel, "Engine state", YELLOW)
        );

        // Modules section
        Label modulesTitle = new Label("Intelligence Modules");
        modulesTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label modulesSub = new Label("Select a module to begin an investigation");
        modulesSub.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(14);
        moduleGrid.setVgap(14);

        moduleGrid.add(createModuleCard("Username", "Public profiles & social accounts", GREEN, "Ready", () -> openModule("Username")), 0, 0);
        moduleGrid.add(createModuleCard("Email", "Breach data & exposure checks", BLUE, "Ready", () -> openModule("Email")), 1, 0);
        moduleGrid.add(createModuleCard("Domain", "WHOIS, DNS & infrastructure", PURPLE, "Ready", () -> openModule("Domain")), 2, 0);
        moduleGrid.add(createModuleCard("IP Address", "Geolocation & reputation", YELLOW, "Ready", () -> openModule("IP Address")), 0, 1);
        moduleGrid.add(createModuleCard("Phone", "Carrier & public records", CYAN, "Ready", () -> openModule("Phone")), 1, 1);

        VBox modulesSection = new VBox(10, modulesTitle, modulesSub, moduleGrid);

        // Recent activity
        Label recentTitle = new Label("Recent Activity");
        recentTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 15px; -fx-font-weight: bold;");

        recentActivityBox = new VBox(8);
        refreshRecentActivity();

        VBox recentSection = new VBox(10, recentTitle, recentActivityBox);

        // Footer notice
        Label notice = new Label("OMEN-X only uses publicly available information. Always operate within legal and ethical boundaries.");
        notice.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        notice.setPadding(new Insets(6, 0, 0, 0));

        VBox content = new VBox(32,
                header,
                metrics,
                modulesSection,
                recentSection,
                notice
        );
        content.setPadding(new Insets(36, 40, 40, 40));
        content.setStyle("-fx-background-color: " + BG + ";");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private void refreshRecentActivity() {
        recentActivityBox.getChildren().clear();

        if (history.isEmpty()) {
            Label empty = new Label("No investigations yet. Select a module to get started.");
            empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
            empty.setPadding(new Insets(12, 0, 0, 0));
            recentActivityBox.getChildren().add(empty);
            return;
        }

        int limit = Math.min(6, history.size());
        for (int i = history.size() - 1; i >= history.size() - limit; i--) {
            ScanRecord r = history.get(i);
            recentActivityBox.getChildren().add(createRecentRow(r));
        }
    }

    private HBox createRecentRow(ScanRecord r) {
        Label type = new Label(r.type);
        type.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        type.setMinWidth(90);

        Label target = new Label(r.target);
        target.setStyle("-fx-text-fill: " + MUTED_2 + "; -fx-font-size: 12px;");
        HBox.setHgrow(target, Priority.ALWAYS);

        Label result = new Label(r.found + " found");
        result.setStyle("-fx-text-fill: " + (r.found > 0 ? GREEN : MUTED) + "; -fx-font-size: 12px;");

        Label time = new Label(r.time);
        time.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        time.setMinWidth(70);
        time.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(16, type, target, result, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );
        return row;
    }

    // =========================================================
    // SCAN VIEW
    // =========================================================

    private VBox createScanView() {
        // Header
        Label title = new Label("OMEN-X");
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitle = new Label("Investigation Workspace");
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        Button backBtn = createGhostButton("←  Dashboard");
        backBtn.setOnAction(e -> {
            refreshRecentActivity();
            updateDashboardStats();
            root.setCenter(dashboardView);
        });

        Label ready = new Label("● Ready");
        ready.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px;");

        HBox headerRight = new HBox(14, backBtn, ready);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(16, new VBox(2, title, subtitle), headerRight);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        // Search bar
        selectedTypeLabel = new Label("USERNAME");
        selectedTypeLabel.setPrefHeight(42);
        selectedTypeLabel.setMinWidth(130);
        selectedTypeLabel.setAlignment(Pos.CENTER);
        selectedTypeLabel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px 0 0 7px;" +
                "-fx-background-radius: 7px 0 0 7px;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        targetField = new TextField();
        targetField.setPromptText("Enter username…");
        targetField.setPrefHeight(42);
        targetField.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-prompt-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 1 0 1 0;" +
                "-fx-padding: 0 16px;" +
                "-fx-font-size: 13px;"
        );
        HBox.setHgrow(targetField, Priority.ALWAYS);

        searchButton = new Button("Scan");
        searchButton.setPrefHeight(42);
        searchButton.setPrefWidth(100);
        searchButton.setStyle(
                "-fx-background-color: " + GREEN + ";" +
                "-fx-text-fill: #0A0E12;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 13px;" +
                "-fx-background-radius: 0 7px 7px 0;" +
                "-fx-cursor: hand;"
        );
        searchButton.setOnAction(e -> runScan());

        targetField.setOnAction(e -> runScan());

        HBox searchBar = new HBox(selectedTypeLabel, targetField, searchButton);

        // Action row
        clearButton = createGhostButton("Clear");
        clearButton.setOnAction(e -> clearResults());

        exportButton = createGhostButton("Copy Results");
        exportButton.setOnAction(e -> copyResults());

        lastScanLabel = new Label("");
        lastScanLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox actionRow = new HBox(10, clearButton, exportButton, lastScanLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lastScanLabel, Priority.ALWAYS);
        lastScanLabel.setAlignment(Pos.CENTER_RIGHT);

        // Stats
        foundValue = new Label("0");
        notFoundValue = new Label("0");
        unknownValue = new Label("0");
        scannedValue = new Label("0");

        HBox stats = new HBox(10,
                createStatCard("Found", foundValue, GREEN),
                createStatCard("Not Found", notFoundValue, RED),
                createStatCard("Unknown", unknownValue, YELLOW),
                createStatCard("Platforms", scannedValue, TEXT)
        );

        // Results header
        Label resultsTitle = new Label("Results");
        resultsTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        resultsTarget = new Label("No target selected");
        resultsTarget.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox resultsHeader = new HBox(12, resultsTitle, resultsTarget);
        resultsHeader.setAlignment(Pos.CENTER_LEFT);

        // Results container
        resultsContainer = new VBox(8);
        resultsContainer.setPadding(new Insets(4, 0, 8, 0));

        Label empty = createEmptyState("Enter a target and press Scan to begin investigation.");
        resultsContainer.getChildren().add(empty);

        resultsScroll = new ScrollPane(resultsContainer);
        resultsScroll.setFitToWidth(true);
        resultsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        // Status bar
        statusBarLabel = new Label("Ready");
        statusBarLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox statusBar = new HBox(statusBarLabel);
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        statusBar.setStyle("-fx-border-color: " + BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        VBox content = new VBox(20,
                header,
                searchBar,
                actionRow,
                stats,
                resultsHeader,
                resultsScroll,
                statusBar
        );
        content.setPadding(new Insets(28, 32, 24, 32));
        content.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        return content;
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    private void openModule(String type) {
        selectedType[0] = type;
        selectedTypeLabel.setText(type.toUpperCase());

        switch (type) {
            case "Email"      -> targetField.setPromptText("Enter email address…");
            case "Domain"     -> targetField.setPromptText("Enter domain name…");
            case "IP Address" -> targetField.setPromptText("Enter IP address…");
            case "Phone"      -> targetField.setPromptText("Enter phone number…");
            default           -> targetField.setPromptText("Enter username…");
        }

        targetField.clear();
        clearResults();
        root.setCenter(scanView);
        targetField.requestFocus();
    }

    private void runScan() {
        String type = selectedType[0];
        String target = targetField.getText().trim();

        if (target.isEmpty()) {
            setStatus("Please enter a target", RED);
            resultsContainer.getChildren().clear();
            resultsContainer.getChildren().add(createEmptyState("Target cannot be empty."));
            return;
        }

        // UI loading state
        searchButton.setDisable(true);
        searchButton.setText("…");
        setStatus("Scanning " + target + "…", GREEN);
        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(createEmptyState("Analyzing publicly available sources…"));
        resultsTarget.setText(target);

        foundValue.setText("0");
        notFoundValue.setText("0");
        unknownValue.setText("0");
        scannedValue.setText("0");

        executor.submit(() -> {
            Map<String, String> scanResults;

            try {
                if ("Username".equals(type)) {
    UsernameScanner scanner = new UsernameScanner();
    scanResults = scanner.scan(target);
} else if ("Email".equals(type)) {
    EmailScanner scanner = new EmailScanner();
    String emailResult = scanner.scan(target);
    scanResults = new LinkedHashMap<>();
    scanResults.put("Email Intelligence", emailResult);
} else if ("Domain".equals(type)) {
    DomainScanner scanner = new DomainScanner();
    scanResults = scanner.scan(target);
} else {
    scanResults = new LinkedHashMap<>();
    scanResults.put(type + " Intelligence",
            "UNKNOWN | This intelligence module is not fully connected yet.");
}
            } catch (Exception ex) {
                scanResults = new LinkedHashMap<>();
                scanResults.put("Error", "UNKNOWN | " + ex.getMessage());
            }

            Map<String, String> finalResults = scanResults;

            Platform.runLater(() -> {
                renderResults(finalResults, type, target);
                searchButton.setDisable(false);
                searchButton.setText("Scan");
            });
        });
    }

      private void renderResults(Map<String, String> scanResults, String type, String target) {
        resultsContainer.getChildren().clear();

        int found = 0, notFound = 0, unknown = 0;

        if ("Domain".equals(type)) {
            renderDomainResults(scanResults);

            found = (int) scanResults.values().stream()
                    .filter(v -> v != null && !v.isBlank()).count();
            unknown = 1;
        } else {
            if (scanResults.isEmpty()) {
                resultsContainer.getChildren().add(createEmptyState("No results returned."));
            } else {
                for (Map.Entry<String, String> entry : scanResults.entrySet()) {
                    String platform = entry.getKey();
                    String result = entry.getValue();

                    if (result.startsWith("FOUND")) found++;
                    else if (result.startsWith("NOT FOUND")) notFound++;
                    else unknown++;

                    resultsContainer.getChildren().add(createResultRow(platform, result));
                }
            }
        }

        foundValue.setText(String.valueOf(found));
        notFoundValue.setText(String.valueOf(notFound));
        unknownValue.setText(String.valueOf(unknown));
        scannedValue.setText(String.valueOf(scanResults.size()));

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        history.add(new ScanRecord(type, target, found, time));
        if (history.size() > 30) history.remove(0);

        lastScanLabel.setText("Last scan  •  " + time);
        setStatus("Scan complete  •  " + found + " found", GREEN);
    }

    private void renderDomainResults(Map<String, String> data) {
        if (data.containsKey("error")) {
            resultsContainer.getChildren().add(createEmptyState(data.get("error")));
            return;
        }

        resultsContainer.getChildren().add(createSectionHeader("Domain Information"));
        addDomainRow("Domain", data.get("domain"));

        resultsContainer.getChildren().add(createSectionHeader("DNS Records"));
        addDomainRow("A Records", data.get("dns_a"));
        addDomainRow("MX Records", data.get("dns_mx"));
        addDomainRow("NS Records", data.get("dns_ns"));
        addDomainRow("TXT Records", data.get("dns_txt"));
        addDomainRow("CNAME", data.get("dns_cname"));

        resultsContainer.getChildren().add(createSectionHeader("Web Presence"));
        addDomainRow("HTTPS", data.get("https"));
        addDomainRow("HTTP", data.get("http"));

        resultsContainer.getChildren().add(createSectionHeader("SSL Certificate"));
        addDomainRow("Subject", data.get("ssl_subject"));
        addDomainRow("Issuer", data.get("ssl_issuer"));
        addDomainRow("Validity", data.get("ssl_validity"));

        resultsContainer.getChildren().add(createSectionHeader("Common Subdomains"));
        addDomainRow("Found", data.get("subdomains"));

        resultsContainer.getChildren().add(createSectionHeader("WHOIS"));
        addDomainRow("Registrar", data.get("whois_registrar"));
        addDomainRow("Organization", data.get("whois_org"));
        addDomainRow("Created", data.get("whois_created"));
        addDomainRow("Expires", data.get("whois_expires"));
        addDomainRow("Updated", data.get("whois_updated"));
        addDomainRow("WHOIS Server", data.get("whois_server"));

        resultsContainer.getChildren().add(createSectionHeader("Reputation"));
        addDomainRow("Blacklist Status", data.get("reputation"));
    }

    private void addDomainRow(String label, String value) {
        if (value == null || value.isBlank()) {
            value = "—";
        }

        Label key = new Label(label);
        key.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        key.setMinWidth(130);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);

        HBox row = new HBox(16, key, val);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));
        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );

        resultsContainer.getChildren().add(row);
    }

    private Label createSectionHeader(String title) {
        Label header = new Label(title);
        header.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 16 0 6 2;"
        );
        return header;
    }

    private void clearResults() {
        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(createEmptyState("Enter a target and press Scan to begin investigation."));
        resultsTarget.setText("No target selected");
        foundValue.setText("0");
        notFoundValue.setText("0");
        unknownValue.setText("0");
        scannedValue.setText("0");
        lastScanLabel.setText("");
        setStatus("Ready", MUTED);
    }

    private void copyResults() {
        if (resultsContainer.getChildren().isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("OMEN-X Scan Results\n");
        sb.append("Target: ").append(resultsTarget.getText()).append("\n");
        sb.append("----------------------------------------\n");

        for (var node : resultsContainer.getChildren()) {
            if (node instanceof VBox box && !box.getChildren().isEmpty()) {
                // simple extraction
                sb.append(box.toString()).append("\n");
            }
        }

        // Better extraction
        sb = new StringBuilder();
        sb.append("OMEN-X • ").append(selectedType[0]).append(" scan\n");
        sb.append("Target: ").append(resultsTarget.getText()).append("\n\n");

        for (var node : resultsContainer.getChildren()) {
            if (node instanceof VBox outer && !outer.getChildren().isEmpty()
                    && outer.getChildren().get(0) instanceof HBox row) {
                // We store platform + status in the row
                for (var child : row.getChildren()) {
                    if (child instanceof VBox left && left.getChildren().size() >= 2) {
                        Label p = (Label) left.getChildren().get(0);
                        Label s = (Label) left.getChildren().get(1);
                        sb.append(p.getText()).append("  →  ").append(s.getText()).append("\n");
                    }
                }
            }
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Results copied to clipboard", GREEN);
    }

    private void updateDashboardStats() {
        totalScansLabel.setText(String.valueOf(history.size()));
        int totalFound = history.stream().mapToInt(r -> r.found).sum();
        findingsLabel.setText(String.valueOf(totalFound));
    }

    private void setStatus(String text, String color) {
        statusBarLabel.setText(text);
        statusBarLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
    }

    // =========================================================
    // UI BUILDERS
    // =========================================================

    private VBox createMetricCard(String title, Label value, String desc, String accent) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        value.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        d.setWrapText(true);

        VBox card = new VBox(6, t, value, d);
        card.setPrefWidth(210);
        card.setPrefHeight(100);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );
        return card;
    }

    private VBox createModuleCard(String title, String desc, String accent, String status, Runnable action) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        d.setWrapText(true);

        Label s = new Label("●  " + status);
        s.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 11px;");

        Button open = new Button("Open module  →");
        open.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + accent + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 0 0 0;" +
                "-fx-cursor: hand;"
        );
        open.setOnAction(e -> action.run());

        VBox card = new VBox(8, t, d, s, open);
        card.setPrefWidth(240);
        card.setPrefHeight(140);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER_SOFT + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        ));
        card.setOnMouseClicked(e -> action.run());

        return card;
    }

    private VBox createStatCard(String title, Label value, String accent) {
        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        value.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        VBox card = new VBox(4, t, value);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setPrefWidth(140);
        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );
        return card;
    }

    private VBox createResultRow(String platform, String result) {
        Label platformLabel = new Label(platform);
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label statusLabel = new Label();
        String url = null;

        if (result.startsWith("FOUND |")) {
            statusLabel.setText("Found");
            statusLabel.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            url = result.substring("FOUND |".length()).trim();
        } else if (result.startsWith("NOT FOUND")) {
            statusLabel.setText("Not found");
            statusLabel.setStyle("-fx-text-fill: " + RED + "; -fx-font-size: 12px;");
        } else {
            statusLabel.setText("Unknown");
            statusLabel.setStyle("-fx-text-fill: " + YELLOW + "; -fx-font-size: 12px;");
        }

        VBox left = new VBox(3, platformLabel, statusLabel);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 16, 13, 16));
        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );
        row.getChildren().add(left);

        if (url != null && !url.isBlank()) {
            final String targetUrl = url;
            Hyperlink link = new Hyperlink(targetUrl);
            link.setStyle("-fx-text-fill: " + BLUE + "; -fx-font-size: 12px;");
            link.setOnAction(e -> openUrl(targetUrl));
            row.getChildren().add(link);
            HBox.setHgrow(link, Priority.ALWAYS);
        } else if (!result.startsWith("FOUND") && !result.startsWith("NOT FOUND")) {
            Label details = new Label(result.contains("|") ? result.split("\\|", 2)[1].trim() : result);
            details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            details.setWrapText(true);
            row.getChildren().add(details);
        }

        // Hover
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + SURFACE_2 + ";" +
                "-fx-border-color: " + BORDER_SOFT + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        ));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        ));

        return new VBox(row);
    }

    private Label createEmptyState(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        l.setPadding(new Insets(28, 0, 20, 0));
        return l;
    }

    private Button createGhostButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 14;" +
                "-fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER_SOFT + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 14;" +
                "-fx-cursor: hand;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 14;" +
                "-fx-cursor: hand;"
        ));
        return b;
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            setStatus("Could not open link", RED);
        }
    }

    // =========================================================
    // DATA
    // =========================================================

    private static class ScanRecord {
        final String type;
        final String target;
        final int found;
        final String time;

        ScanRecord(String type, String target, int found, String time) {
            this.type = type;
            this.target = target;
            this.found = found;
            this.time = time;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}