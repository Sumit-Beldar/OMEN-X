// =========================================================
// PACKAGE
// Main OMEN-X JavaFX application package
// =========================================================
package com.omenx;

// =========================================================
// OSINT MODULE IMPORTS
// Connects Main.java to the individual investigation scanners
// =========================================================
import com.omenx.osint.UsernameScanner;
import com.omenx.osint.DomainScanner;
import com.omenx.osint.EmailScanner;
import com.omenx.osint.PhoneScanner;

// =========================================================
// MODEL / SERVICE / UI IMPORTS
// Keeps data storage and dedicated UI modules outside Main.java
// =========================================================
import com.omenx.model.ScanRecord;
import com.omenx.service.ScanService;
import com.omenx.service.ReportService;
import com.omenx.ui.AppHeader;
import com.omenx.ui.DashboardUI;
import com.omenx.ui.ImageMetadataUI;
import com.omenx.ui.ReverseImageUI;
import com.omenx.ui.IPAddressUI;
import com.omenx.ui.MalwareAnalysisUI;
import com.omenx.ui.LoginUI;

// =========================================================
// JAVAFX IMPORTS
// =========================================================
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

// =========================================================
// JAVA STANDARD LIBRARY IMPORTS
// =========================================================
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// =========================================================
// MAIN APPLICATION
// =========================================================
public class Main extends Application {

    // =========================================================
    // COLOR SYSTEM (Tactical Crimson Unified)
    // =========================================================
    private static final String BG          = "#090C12";
    private static final String PANEL       = "#111520";
    private static final String SURFACE     = "#141824";
    private static final String SURFACE_2   = "#1A1F30";
    private static final String BORDER      = "#1C2234";
    private static final String BORDER_SOFT = "#2D354E";
    private static final String TEXT        = "#F1F5F9";
    private static final String MUTED       = "#788698";
    private static final String GREEN       = "#22C55E";
    private static final String RED         = "#DC2626";
    private static final String YELLOW      = "#EAB308";

    // =========================================================
    // APPLICATION STATE
    // =========================================================
    private final String[] selectedType = {"Username"};

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // SERVICES
    // Handles scan history and reports
    // =========================================================
    private ScanService scanService;
    private ReportService reportService;

    // =========================================================
    // UI MODULES
    // =========================================================
    private DashboardUI dashboardUI;
    private ImageMetadataUI imageMetadataUI;
    private ReverseImageUI reverseImageUI;
    private IPAddressUI ipAddressUI;
    private MalwareAnalysisUI malwareAnalysisUI;
    private LoginUI loginUI;

    // =========================================================
    // MAIN APPLICATION CONTAINER
    // =========================================================
    private BorderPane root;

    private VBox dashboardView;
    private VBox scanView;
    private AppHeader scanHeader;

    // =========================================================
    // INVESTIGATION WORKSPACE CONTROLS
    // =========================================================
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
    private String currentUsername = null;
    private final Map<String, Button> sidebarButtons = new LinkedHashMap<>();

    private void onLoginSuccess(String username) {
        this.currentUsername = username;
        root.setLeft(createSidebar());
        showDashboard();
    }

    // =========================================================
    // APPLICATION STARTUP
    // =========================================================
    @Override
    public void start(Stage stage) {

        // Initialize Services
        scanService = new ScanService();
        reportService = new ReportService(scanService);

        // Root Container
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        // Initialize Dashboard UI
        dashboardUI = new DashboardUI(new DashboardUI.ModuleAction() {
            @Override
            public void open(String type) {
                openModule(type);
            }

            @Override
            public void openImageMetadata() {
                Main.this.openImageMetadata();
            }

            @Override
            public void openReverseImage() {
                openReverseImageScanner();
            }
        });

        // Initialize Dedicated Intelligence UIs
        imageMetadataUI = new ImageMetadataUI(this::showDashboard, scanService);
        reverseImageUI = new ReverseImageUI(this::showDashboard, scanService);
        ipAddressUI = new IPAddressUI(this::showDashboard, scanService);
        malwareAnalysisUI = new MalwareAnalysisUI(stage, this::showDashboard, scanService);

        // Build Dashboard View & Scan View
        dashboardView = dashboardUI.createDashboard(scanService.getHistory());
        scanView = createScanView();

        // Initialize Login UI & Start on Login Screen
        loginUI = new LoginUI(this::onLoginSuccess);
        root.setCenter(loginUI.createView());

        // Create Main Scene
        Scene scene = new Scene(root, 1340, 860);

        try {
            if (getClass().getResource("/styles/app.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/app.css").toExternalForm()
                );
            }
        } catch (Exception ignored) {
        }

        stage.setTitle("OMEN-X  •  Tactical OSINT Platform");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();

        // Cleanup on Close
        stage.setOnCloseRequest(e -> {
            executor.shutdownNow();
            if (imageMetadataUI != null) imageMetadataUI.shutdown();
            if (reverseImageUI != null) reverseImageUI.shutdown();
            if (loginUI != null) loginUI.stopAnimation();
        });
    }

    // =========================================================
    // PERSISTENT SIDEBAR NAVIGATION
    // =========================================================
    private VBox createSidebar() {
        sidebarButtons.clear();

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(260);
        sidebar.setMaxWidth(280);
        sidebar.setPadding(new Insets(18, 16, 16, 16));
        sidebar.setSpacing(4);

        // Classification stripe
        Label classLabel = new Label("// RESTRICTED //");
        classLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-family: 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        classLabel.setAlignment(Pos.CENTER);
        classLabel.setMaxWidth(Double.MAX_VALUE);

        Region stripe = new Region();
        stripe.setPrefHeight(1);
        stripe.setMaxWidth(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color: #1C2234;");

        Label logo = new Label("OMEN-X");
        logo.setStyle("-fx-text-fill: #DC2626; -fx-font-family: 'Consolas', sans-serif; -fx-font-size: 24px; -fx-font-weight: bold; -fx-letter-spacing: 3px;");

        Label sub = new Label("TACTICAL OSINT PLATFORM");
        sub.setStyle("-fx-text-fill: #788698; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        VBox brand = new VBox(4, classLabel, stripe, logo, sub);
        brand.setPadding(new Insets(4, 0, 14, 6));

        Label navLabel = new Label("OPERATIONS");
        navLabel.setStyle("-fx-text-fill: #788698; -fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        navLabel.setPadding(new Insets(8, 6, 6, 6));

        sidebar.getChildren().addAll(brand, navLabel);

        addSidebarItem(sidebar, "Dashboard", "//>  Command Center", this::showDashboard);
        addSidebarItem(sidebar, "Username", "@>  Username Recognition", () -> openModule("Username"));
        addSidebarItem(sidebar, "Email", "#>  Email Exposure", () -> openModule("Email"));
        addSidebarItem(sidebar, "Domain", "::>  Domain & DNS", () -> openModule("Domain"));
        addSidebarItem(sidebar, "IP Address", "[]>  IP Geolocation", () -> openModule("IP Address"));
        addSidebarItem(sidebar, "Phone", "{}>  Phone Intel", () -> openModule("Phone"));
        addSidebarItem(sidebar, "EXIF Metadata", "<>  EXIF Metadata", this::openImageMetadata);
        addSidebarItem(sidebar, "Reverse Image", ">>  Reverse Image", this::openReverseImageScanner);
        addSidebarItem(sidebar, "Malware Analysis", "!!>  Malware Threat", this::openMalwareAnalysis);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Operator Connection Widget (Unclippable)
        String rawUser = currentUsername != null ? currentUsername : "Omen-X:M1";
        String displayUser = rawUser;
        if (rawUser.contains("Member")) {
            int idx = rawUser.indexOf("Member");
            String num = rawUser.substring(idx).replace(")", "");
            displayUser = "OPR • " + num;
        }

        Label userLabel = new Label(displayUser);
        userLabel.getStyleClass().add("operator-label");
        userLabel.setTooltip(new Tooltip("Authorized Operator: " + rawUser));
        HBox.setHgrow(userLabel, Priority.ALWAYS);

        Label onlineDot = new Label("● ACTIVE");
        onlineDot.getStyleClass().addAll("badge", "badge-green");
        onlineDot.setMinWidth(Region.USE_PREF_SIZE);

        HBox userHeader = new HBox(8, userLabel, onlineDot);
        userHeader.setAlignment(Pos.CENTER_LEFT);

        Label statusText = new Label("+ SYSTEMS OPERATIONAL");
        statusText.getStyleClass().add("operator-status-text");

        Button logoutBtn = new Button("DISCONNECT SESSION");
        logoutBtn.getStyleClass().add("btn-ghost-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            currentUsername = null;
            root.setLeft(null);
            root.setCenter(loginUI.createView());
        });

        VBox statusBox = new VBox(8, userHeader, statusText, logoutBtn);
        statusBox.getStyleClass().add("operator-card");

        sidebar.getChildren().add(statusBox);
        return sidebar;
    }

    private void addSidebarItem(VBox sidebar, String key, String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            action.run();
            updateSidebarActive(key);
        });
        sidebarButtons.put(key, btn);
        sidebar.getChildren().add(btn);
    }

    private void updateSidebarActive(String key) {
        sidebarButtons.forEach((k, btn) -> {
            if (k.equals(key)) {
                if (!btn.getStyleClass().contains("active")) {
                    btn.getStyleClass().add("active");
                }
            } else {
                btn.getStyleClass().remove("active");
            }
        });
    }

    // =========================================================
    // SHOW DASHBOARD
    // =========================================================
    private void showDashboard() {
        if (dashboardUI != null) {
            dashboardUI.refreshDashboard(scanService.getHistory());
        }
        root.setCenter(dashboardView);
        updateSidebarActive("Dashboard");
    }

    // =========================================================
    // INVESTIGATION SCAN VIEW (Standardized with AppHeader)
    // =========================================================
    private VBox createScanView() {

        // Reusable AppHeader for Username, Email, Domain, Phone
        scanHeader = new AppHeader(
                "@>",
                "USERNAME RECOGNITION",
                "Search 500+ public platforms & social profiles",
                this::showDashboard
        );

        // Module Selector Tag
        selectedTypeLabel = new Label("USERNAME");
        selectedTypeLabel.setPrefHeight(42);
        selectedTypeLabel.setMinWidth(130);
        selectedTypeLabel.setAlignment(Pos.CENTER);
        selectedTypeLabel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px 0 0 6px;" +
                "-fx-background-radius: 6px 0 0 6px;" +
                "-fx-font-family: 'Consolas', monospace;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        // Target Input
        targetField = new TextField();
        targetField.setPromptText("Enter target username, email or domain…");
        targetField.setPrefHeight(44);
        targetField.getStyleClass().add("cyber-input");
        HBox.setHgrow(targetField, Priority.ALWAYS);

        searchButton = new Button("EXECUTE SCAN");
        searchButton.setPrefHeight(44);
        searchButton.getStyleClass().add("btn-primary");
        searchButton.setOnAction(e -> runScan());
        targetField.setOnAction(e -> runScan());

        HBox searchBar = new HBox(
                0,
                selectedTypeLabel,
                targetField,
                searchButton
        );

        clearButton = new Button("Clear");
        clearButton.getStyleClass().add("btn-ghost");
        clearButton.setOnAction(e -> clearResults());

        exportButton = new Button("Copy Results");
        exportButton.getStyleClass().add("btn-ghost");
        exportButton.setOnAction(e -> copyResults());

        lastScanLabel = new Label("");
        lastScanLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox actionRow = new HBox(10, clearButton, exportButton, lastScanLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lastScanLabel, Priority.ALWAYS);
        lastScanLabel.setAlignment(Pos.CENTER_RIGHT);

        // Statistics
        foundValue = new Label("0");
        notFoundValue = new Label("0");
        unknownValue = new Label("0");
        scannedValue = new Label("0");

        HBox stats = new HBox(
                12,
                createStatCard("Found", foundValue, "metric-accent-green", GREEN),
                createStatCard("Not Found", notFoundValue, "metric-accent-red", RED),
                createStatCard("Unknown", unknownValue, "metric-accent-amber", YELLOW),
                createStatCard("Platforms", scannedValue, "metric-accent-red", TEXT)
        );

        // Results Header
        Label resultsTitle = new Label("Investigation Results");
        resultsTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        resultsTarget = new Label("No target selected");
        resultsTarget.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox resultsHeader = new HBox(12, resultsTitle, resultsTarget);
        resultsHeader.setAlignment(Pos.CENTER_LEFT);

        // Results Container
        resultsContainer = new VBox(8);
        resultsContainer.setPadding(new Insets(4, 0, 8, 0));
        resultsContainer.getChildren().add(
                createEmptyState("Enter a target and press Execute Scan to begin investigation.")
        );

        resultsScroll = new ScrollPane(resultsContainer);
        resultsScroll.setFitToWidth(true);
        resultsScroll.getStyleClass().add("scroll-pane");
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        // Status Bar
        statusBarLabel = new Label("Ready");
        statusBarLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        HBox statusBar = new HBox(statusBarLabel);
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        statusBar.setStyle("-fx-border-color: " + BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        VBox content = new VBox(
                18,
                scanHeader,
                searchBar,
                actionRow,
                stats,
                resultsHeader,
                resultsScroll,
                statusBar
        );

        content.setPadding(new Insets(20, 28, 20, 28));
        content.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        return content;
    }

    // =========================================================
    // OPEN MODULE
    // =========================================================
    private void openModule(String type) {
        updateSidebarActive(type);

        if ("Malware Analysis".equals(type)) {
            root.setCenter(malwareAnalysisUI.getView());
            return;
        }

        if ("IP Address".equals(type)) {
            root.setCenter(ipAddressUI.createView());
            return;
        }

        selectedType[0] = type;
        selectedTypeLabel.setText(type.toUpperCase());

        switch (type) {
            case "Email" -> {
                scanHeader.setHeaderIcon("#>");
                scanHeader.setHeaderTitle("EMAIL EXPOSURE");
                scanHeader.setHeaderSubtitle("Check breach records, domain validity & email account deliverability");
                targetField.setPromptText("Enter email address…");
            }
            case "Domain" -> {
                scanHeader.setHeaderIcon("::>");
                scanHeader.setHeaderTitle("DOMAIN & DNS INTEL");
                scanHeader.setHeaderSubtitle("Analyze WHOIS records, DNS routing & active subdomains");
                targetField.setPromptText("Enter domain name…");
            }
            case "Phone" -> {
                scanHeader.setHeaderIcon("{}>");
                scanHeader.setHeaderTitle("PHONE INTEL");
                scanHeader.setHeaderSubtitle("Carrier info, validity, geographic area & telecom metadata");
                targetField.setPromptText("Enter phone number…");
            }
            default -> {
                scanHeader.setHeaderIcon("@>");
                scanHeader.setHeaderTitle("USERNAME RECOGNITION");
                scanHeader.setHeaderSubtitle("Search 500+ public platforms & social profiles");
                targetField.setPromptText("Enter username…");
            }
        }

        scanHeader.setStatus(AppHeader.StatusType.READY, "READY");
        targetField.clear();
        clearResults();
        root.setCenter(scanView);
        targetField.requestFocus();
    }

    private void openImageMetadata() {
        updateSidebarActive("EXIF Metadata");
        root.setCenter(imageMetadataUI.createView());
    }

    private void openReverseImageScanner() {
        updateSidebarActive("Reverse Image");
        root.setCenter(reverseImageUI.createView());
    }

    private void openMalwareAnalysis() {
        updateSidebarActive("Malware Analysis");
        root.setCenter(malwareAnalysisUI.getView());
    }

    // =========================================================
    // RUN SCAN
    // =========================================================
    private void runScan() {
        String type = selectedType[0];
        String target = targetField.getText().trim();

        if (target.isEmpty()) {
            setStatus("Please enter a target", RED);
            scanHeader.setStatus(AppHeader.StatusType.WARNING, "TARGET EMPTY");
            resultsContainer.getChildren().clear();
            resultsContainer.getChildren().add(createEmptyState("Target cannot be empty."));
            return;
        }

        searchButton.setDisable(true);
        searchButton.setText("Scanning…");
        setStatus("Scanning " + target + "…", GREEN);
        scanHeader.setStatus(AppHeader.StatusType.SCANNING, "SCANNING " + target);

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
                } else if ("Phone".equals(type)) {
                    PhoneScanner scanner = new PhoneScanner();
                    PhoneScanner.PhoneResult phoneResult = scanner.scan(target, "IN");
                    scanResults = new LinkedHashMap<>();
                    scanResults.put("Phone Intelligence", buildPhoneResult(phoneResult));
                } else {
                    scanResults = new LinkedHashMap<>();
                    scanResults.put(type + " Intelligence", "UNKNOWN | This intelligence module is not fully connected yet.");
                }
            } catch (Exception ex) {
                scanResults = new LinkedHashMap<>();
                scanResults.put("Error", "UNKNOWN | " + ex.getMessage());
            }

            Map<String, String> finalResults = scanResults;

            Platform.runLater(() -> {
                renderResults(finalResults, type, target);
                searchButton.setDisable(false);
                searchButton.setText("EXECUTE SCAN");
            });
        });
    }

    // =========================================================
    // RENDER RESULTS
    // =========================================================
    private void renderResults(Map<String, String> scanResults, String type, String target) {
        resultsContainer.getChildren().clear();

        int found = 0;
        int notFound = 0;
        int unknown = 0;

        boolean scanFailed = "Domain".equals(type) && scanResults.containsKey("error");

        if ("Domain".equals(type)) {
            renderDomainResults(scanResults);
            if (!scanFailed) {
                found = (int) scanResults.values().stream().filter(v -> v != null && !v.isBlank()).count();
                unknown = 1;
            } else {
                unknown = 1;
            }
        } else {
            if (scanResults.isEmpty()) {
                resultsContainer.getChildren().add(createEmptyState("No results returned."));
            } else {
                for (Map.Entry<String, String> entry : scanResults.entrySet()) {
                    String platform = entry.getKey();
                    String result = entry.getValue();

                    if (result != null && result.startsWith("FOUND")) {
                        found++;
                    } else if (result != null && result.startsWith("NOT FOUND")) {
                        notFound++;
                    } else {
                        unknown++;
                    }

                    // Route to specialized card renderers for Email and Phone
                    if ("Email Intelligence".equals(platform)) {
                        resultsContainer.getChildren().add(createEmailResultCard(platform, result));
                    } else if ("Phone Intelligence".equals(platform)) {
                        resultsContainer.getChildren().add(createPhoneResultCard(platform, result));
                    } else {
                        resultsContainer.getChildren().add(createResultRow(platform, result));
                    }
                }
            }
        }

        foundValue.setText(String.valueOf(found));
        notFoundValue.setText(String.valueOf(notFound));
        unknownValue.setText(String.valueOf(unknown));
        scannedValue.setText(String.valueOf(scanResults.size()));

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (!scanFailed) {
            scanService.saveScan(type, target, found, time);
        }

        dashboardUI.refreshDashboard(scanService.getHistory());

        lastScanLabel.setText("Last scan  •  " + time);
        setStatus(
                scanFailed ? "Scan failed  •  check the target" : "Scan complete  •  " + found + " findings identified",
                scanFailed ? RED : GREEN
        );

        scanHeader.setStatus(
                scanFailed ? AppHeader.StatusType.ERROR : AppHeader.StatusType.READY,
                scanFailed ? "FAILED" : "COMPLETE"
        );
    }

    // =========================================================
    // DOMAIN RESULTS
    // =========================================================
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
        key.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        key.setMinWidth(140);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);

        HBox row = new HBox(16, key, val);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("cyber-card");

        resultsContainer.getChildren().add(row);
    }

    // =========================================================
    // CLEAR & COPY RESULTS
    // =========================================================
    private void clearResults() {
        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(
                createEmptyState("Enter a target and press Execute Scan to begin investigation.")
        );
        resultsTarget.setText("No target selected");
        foundValue.setText("0");
        notFoundValue.setText("0");
        unknownValue.setText("0");
        scannedValue.setText("0");
        lastScanLabel.setText("");
        setStatus("Ready", MUTED);
        scanHeader.setStatus(AppHeader.StatusType.READY, "READY");
    }

    private void copyResults() {
        if (resultsContainer.getChildren().isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("OMEN-X • ").append(selectedType[0]).append(" scan\n");
        sb.append("Target: ").append(resultsTarget.getText()).append("\n\n");

        for (var node : resultsContainer.getChildren()) {
            if (node instanceof VBox outer && !outer.getChildren().isEmpty() && outer.getChildren().get(0) instanceof HBox row) {
                for (var child : row.getChildren()) {
                    if (child instanceof VBox left && left.getChildren().size() >= 2) {
                        Label platform = (Label) left.getChildren().get(0);
                        Label status = (Label) left.getChildren().get(1);
                        sb.append(platform.getText()).append("  →  ").append(status.getText()).append("\n");
                    }
                }
            }
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Results copied to clipboard", GREEN);
    }

    private void setStatus(String text, String color) {
        if (statusBarLabel != null) {
            statusBarLabel.setText(text);
            statusBarLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        }
    }

    // =========================================================
    // EMAIL RESULT CARD
    // Structured card renderer for EMAIL_RESULT: format strings.
    // =========================================================
    private VBox createEmailResultCard(String platform, String result) {
        if (result == null) result = "UNKNOWN | EMAIL_RESULT:\nstatus: UNKNOWN\ntarget: —";

        // Strip classification prefix
        String body = result;
        String classification = "UNKNOWN";
        if (result.startsWith("FOUND | ")) {
            body = result.substring("FOUND | ".length());
            classification = "FOUND";
        } else if (result.startsWith("NOT FOUND | ")) {
            body = result.substring("NOT FOUND | ".length());
            classification = "NOT FOUND";
        } else if (result.startsWith("UNKNOWN | ")) {
            body = result.substring("UNKNOWN | ".length());
        }

        // Parse structured key:value lines
        String target = "—";
        String status = "—";
        String details = null;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.startsWith("target: "))  target  = line.substring(8);
            else if (line.startsWith("status: "))  status  = line.substring(8);
            else if (line.startsWith("details: ")) details = line.substring(9);
        }

        // Build outer card
        VBox card = new VBox(0);
        card.getStyleClass().add("cyber-card");
        card.setStyle("-fx-background-color: " + PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // ── Card Header: platform name + status badge ──
        String badgeText;
        String badgeClass;
        if ("FOUND".equals(classification)) {
            badgeText = "BREACH DETECTED";
            badgeClass = "badge-red";
        } else if ("NOT FOUND".equals(classification)) {
            badgeText = "CLEAN";
            badgeClass = "badge-green";
        } else {
            badgeText = "UNKNOWN";
            badgeClass = "badge-amber";
        }

        Label platformLabel = new Label(platform);
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", badgeClass);

        HBox cardHeader = new HBox(12, platformLabel, badge);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(12, 16, 12, 16));
        cardHeader.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        card.getChildren().add(cardHeader);

        // ── Card Body: labeled rows ──
        VBox body_vbox = new VBox(10);
        body_vbox.setPadding(new Insets(12, 16, 14, 16));
        body_vbox.getChildren().add(createInfoRow("TARGET", target));
        body_vbox.getChildren().add(createInfoRow("BREACH STATUS", status));

        // ── Breach pill tags ──
        if (details != null && !details.isBlank() && "FOUND".equals(classification)) {
            String[] breachNames = details.split("\\|");
            Label breachTitle = new Label("KNOWN BREACHES  (" + breachNames.length + ")");
            breachTitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-padding: 6 0 2 0;");

            javafx.scene.layout.FlowPane tags = new javafx.scene.layout.FlowPane(8, 6);
            for (String name : breachNames) {
                name = name.trim();
                if (name.isEmpty()) continue;
                Label tag = new Label(name);
                tag.setStyle(
                        "-fx-background-color: #35191E;" +
                        "-fx-text-fill: #EF4444;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 10 3 10;" +
                        "-fx-background-radius: 4px;"
                );
                tags.getChildren().add(tag);
            }
            body_vbox.getChildren().addAll(breachTitle, tags);
        } else if (details != null && !details.isBlank()) {
            // Show plain details for error/note cases
            body_vbox.getChildren().add(createInfoRow("NOTE", details));
        }

        card.getChildren().add(body_vbox);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + SURFACE_2 + "; -fx-border-color: " + BORDER_SOFT + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));
        card.setOnMouseExited(e ->  card.setStyle("-fx-background-color: " + PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));

        return card;
    }

    // =========================================================
    // PHONE RESULT CARD
    // Structured card renderer for PHONE_RESULT: format strings.
    // =========================================================
    private VBox createPhoneResultCard(String platform, String result) {
        if (result == null) result = "UNKNOWN | PHONE_RESULT:\nnote: No data.";

        // Strip classification prefix
        String body = result;
        String classification = "FOUND";
        if (result.startsWith("FOUND | ")) {
            body = result.substring("FOUND | ".length());
        } else if (result.startsWith("NOT FOUND | ")) {
            body = result.substring("NOT FOUND | ".length());
            classification = "NOT FOUND";
        } else if (result.startsWith("UNKNOWN | ")) {
            body = result.substring("UNKNOWN | ".length());
            classification = "UNKNOWN";
        }

        VBox card = new VBox(0);
        card.getStyleClass().add("cyber-card");
        card.setStyle("-fx-background-color: " + PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // ── Card Header ──
        String badgeText = "FOUND".equals(classification) ? "DATA FOUND" : classification;
        String badgeClass = "FOUND".equals(classification) ? "badge-green" : "badge-amber";

        Label platformLabel = new Label(platform);
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", badgeClass);

        HBox cardHeader = new HBox(12, platformLabel, badge);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(12, 16, 12, 16));
        cardHeader.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent; -fx-border-width: 0 0 1 0;");
        card.getChildren().add(cardHeader);

        // ── Card Body: parse sections and fields ──
        VBox bodyBox = new VBox(0);
        bodyBox.setPadding(new Insets(8, 16, 14, 16));

        VBox currentSection = null;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.startsWith("PHONE_RESULT:") || line.isEmpty()) continue;

            if (line.startsWith("section: ")) {
                String sectionName = line.substring(9);
                Label sectionTitle = new Label(sectionName);
                sectionTitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-padding: 10 0 4 0;");
                currentSection = new VBox(8);
                currentSection.getChildren().add(sectionTitle);
                bodyBox.getChildren().add(currentSection);

            } else if (line.startsWith("field: ") && currentSection != null) {
                String fieldContent = line.substring(7);
                int eqIdx = fieldContent.indexOf('=');
                String fieldName  = eqIdx > 0 ? fieldContent.substring(0, eqIdx).trim() : fieldContent;
                String fieldValue = eqIdx > 0 ? fieldContent.substring(eqIdx + 1).trim() : "—";
                currentSection.getChildren().add(createInfoRow(fieldName, fieldValue));

            } else if (line.startsWith("note: ")) {
                Label noteLabel = new Label(line.substring(6));
                noteLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
                noteLabel.setWrapText(true);
                bodyBox.getChildren().add(noteLabel);
            }
        }

        card.getChildren().add(bodyBox);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + SURFACE_2 + "; -fx-border-color: " + BORDER_SOFT + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));
        card.setOnMouseExited(e ->  card.setStyle("-fx-background-color: " + PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));

        return card;
    }

    // =========================================================
    // INFO ROW — shared labeled-row helper
    // =========================================================
    private HBox createInfoRow(String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.setMinWidth(160);
        keyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        Label valueLabel = new Label(value == null || value.isBlank() ? "—" : value);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        HBox row = new HBox(12, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    // =========================================================
    // RESULT ROW
    // =========================================================
    private VBox createResultRow(String platform, String result) {
        if (result == null) result = "UNKNOWN";

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
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("cyber-card");
        row.getChildren().add(left);

        if (url != null && !url.isBlank()) {
            final String targetUrl = url;
            Hyperlink link = new Hyperlink(targetUrl);
            link.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
            link.setOnAction(e -> openUrl(targetUrl));
            row.getChildren().add(link);
            HBox.setHgrow(link, Priority.ALWAYS);
        } else if (!result.startsWith("FOUND") && !result.startsWith("NOT FOUND")) {
            String details = result.contains("|") ? result.split("\\|", 2)[1].trim() : result;
            Label detailsLabel = new Label(details);
            detailsLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            detailsLabel.setWrapText(true);
            row.getChildren().add(detailsLabel);
        }

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + SURFACE_2 + "; -fx-border-color: " + BORDER_SOFT + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: " + PANEL + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px; -fx-background-radius: 8px;"));

        return new VBox(row);
    }

    private Label createSectionHeader(String title) {
        Label header = new Label(title);
        header.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 16 0 6 2; -fx-letter-spacing: 1px;");
        return header;
    }

    private Label createEmptyState(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        label.setPadding(new Insets(28, 0, 20, 0));
        return label;
    }

    private VBox createStatCard(String title, Label value, String accentClass, String accentColor) {
        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        value.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        VBox card = new VBox(4, t, value);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setPrefWidth(140);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().addAll("cyber-card", accentClass);

        return card;
    }

    private String buildPhoneResult(PhoneScanner.PhoneResult result) {
        if (result == null) {
            return "UNKNOWN | PHONE_RESULT:\nnote: No phone intelligence was returned.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FOUND | PHONE_RESULT:\n");

        sb.append("section: IDENTITY\n");
        sb.append("field: Phone Number=").append(safe(result.getInputNumber())).append("\n");
        sb.append("field: Country=").append(safe(result.getCountry())).append("\n");
        sb.append("field: Country Code=").append(safe(result.getCountryCode())).append("\n");
        sb.append("field: National Format=").append(safe(result.getNationalFormat())).append("\n");
        sb.append("field: International Format=").append(safe(result.getInternationalFormat())).append("\n");
        sb.append("field: E.164 Format=").append(safe(result.getE164Format())).append("\n");

        sb.append("section: VALIDATION\n");
        sb.append("field: Possible Number=").append(result.isPossible() ? "Yes" : "No").append("\n");
        sb.append("field: Valid Number=").append(result.isValid() ? "Yes" : "No").append("\n");
        sb.append("field: Number Type=").append(safe(result.getNumberType())).append("\n");
        sb.append("field: Region Match=").append(result.isRegionMatch() ? "Yes" : "No").append("\n");

        sb.append("section: CARRIER / NETWORK\n");
        sb.append("field: Carrier=").append(safe(result.getOriginalCarrier())).append("\n");
        sb.append("field: Line Type=").append(safe(result.getNumberType())).append("\n");

        sb.append("section: LOCATION\n");
        sb.append("field: Region=").append(safe(result.getRegion())).append("\n");
        sb.append("field: Geographic Area=").append(safe(result.getGeographicDescription())).append("\n");
        sb.append("field: Time Zone(s)=").append(safe(result.getTimezones())).append("\n");

        return sb.toString().trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
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

    public static void main(String[] args) {
        launch(args);
    }
}