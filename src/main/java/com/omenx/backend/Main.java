// =========================================================
// PACKAGE
// Main OMEN-X JavaFX application package
// =========================================================
package com.omenx.backend;

// =========================================================
// OSINT MODULE IMPORTS
// Connects Main.java to the individual investigation scanners
// =========================================================
import com.omenx.backend.osint.UsernameScanner;
import com.omenx.backend.osint.DomainScanner;
import com.omenx.backend.osint.EmailScanner;
import com.omenx.backend.osint.PhoneScanner;

// =========================================================
// MODEL / SERVICE / UI IMPORTS
// Keeps data storage and dedicated UI modules outside Main.java
// =========================================================
import com.omenx.database.model.ScanRecord;
import com.omenx.backend.service.ScanService;
import com.omenx.backend.service.ReportService;
import com.omenx.frontend.AppHeader;
import com.omenx.frontend.DashboardUI;
import com.omenx.frontend.ImageMetadataUI;
import com.omenx.frontend.ReverseImageUI;
import com.omenx.frontend.IPAddressUI;
import com.omenx.frontend.MalwareAnalysisUI;
import com.omenx.frontend.LoginUI;
import com.omenx.frontend.OsirisLauncherUI;
import com.omenx.frontend.ModuleIcons;
import com.omenx.backend.osiris.OsirisConfig;
import com.omenx.backend.osiris.OsirisRuntimeManager;
import com.omenx.backend.osiris.OsirisInstaller;
import com.omenx.backend.osiris.OsirisProcessManager;

// =========================================================
// JAVAFX IMPORTS
// =========================================================
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

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
// MAIN APPLICATION — DARK GLASS INTELLIGENCE DASHBOARD
// =========================================================
public class Main extends Application {

    // =========================================================
    // COLOR SYSTEM (Dark Glassmorphic Intelligence System)
    // =========================================================
    private static final String BG          = "#060810";
    private static final String PANEL       = "rgba(13, 19, 32, 0.78)";
    private static final String SURFACE     = "rgba(16, 24, 40, 0.85)";
    private static final String SURFACE_2   = "rgba(20, 32, 52, 0.90)";
    private static final String BORDER      = "rgba(34, 211, 238, 0.20)";
    private static final String BORDER_SOFT = "rgba(34, 211, 238, 0.40)";
    private static final String TEXT        = "#F1F5F9";
    private static final String MUTED       = "#64748B";
    private static final String CYAN        = "#22D3EE";
    private static final String GREEN       = "#10B981";
    private static final String RED         = "#EF4444";
    private static final String YELLOW      = "#F59E0B";

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
    private OsirisConfig osirisConfig;
    private OsirisRuntimeManager osirisRuntimeManager;
    private OsirisInstaller osirisInstaller;
    private OsirisProcessManager osirisProcessManager;
    private OsirisLauncherUI osirisLauncherUI;

    // =========================================================
    // MAIN APPLICATION CONTAINER & CHROME
    // =========================================================
    private BorderPane root;

    private HBox topStatusBar;
    private HBox bottomStatusBar;
    private Label topClockLabel;
    private Label bottomStatusLabel;
    private Timeline clockTimeline;

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
        root.setTop(createTopStatusBar());
        root.setLeft(createSidebar());
        root.setBottom(createBottomStatusBar());
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

            @Override
            public void openOsirisLauncher() {
                Main.this.openOsirisLauncher();
            }
        });

        // Initialize OSIRIS Subsystem
        osirisConfig = new OsirisConfig();
        osirisRuntimeManager = new OsirisRuntimeManager(osirisConfig);
        osirisInstaller = new OsirisInstaller(osirisConfig, osirisRuntimeManager);
        osirisProcessManager = new OsirisProcessManager(osirisConfig, osirisRuntimeManager, osirisInstaller);
        osirisLauncherUI = new OsirisLauncherUI(osirisConfig, osirisRuntimeManager, osirisInstaller, osirisProcessManager, this::showDashboard);

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

        stage.setTitle("OMEN-X  •  Global OSINT & Intelligence Dashboard");
        stage.setMinWidth(1100);
        stage.setMinHeight(750);
        stage.setScene(scene);
        stage.show();

        // Cleanup on Close
        stage.setOnCloseRequest(e -> {
            if (clockTimeline != null) clockTimeline.stop();
            executor.shutdownNow();
            if (imageMetadataUI != null) imageMetadataUI.shutdown();
            if (reverseImageUI != null) reverseImageUI.shutdown();
            if (loginUI != null) loginUI.stopAnimation();
            if (osirisProcessManager != null) osirisProcessManager.shutdown();
        });
    }

    // =========================================================
    // TOP STATUS BAR (Live Intelligence Chrome)
    // =========================================================
    private HBox createTopStatusBar() {
        topStatusBar = new HBox(16);
        topStatusBar.getStyleClass().add("top-status-bar");
        topStatusBar.setAlignment(Pos.CENTER_LEFT);

        // Brand + Wordmark + Tagline
        Label logo = new Label("OMEN-X");
        logo.getStyleClass().add("top-bar-logo");

        Label tagline = new Label("// GLOBAL OSINT INTELLIGENCE PLATFORM");
        tagline.getStyleClass().add("top-bar-tagline");

        HBox brandBox = new HBox(10, logo, tagline);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brandBox, Priority.ALWAYS);

        // Separator utility
        Region div1 = createDivider();
        Region div2 = createDivider();
        Region div3 = createDivider();
        Region div4 = createDivider();

        // Status Readouts
        topClockLabel = new Label();
        topClockLabel.getStyleClass().add("top-bar-item-value");
        updateClock();

        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        HBox clockBox = new HBox(6, createBarLabel("TIME:"), topClockLabel);
        clockBox.setAlignment(Pos.CENTER_LEFT);

        Label connVal = new Label("ENCRYPTED");
        connVal.setStyle("-fx-text-fill: #10B981; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox connBox = new HBox(6, createBarLabel("GATEWAY:"), connVal);
        connBox.setAlignment(Pos.CENTER_LEFT);

        Label modVal = new Label("9 / 9 ONLINE");
        modVal.getStyleClass().add("top-bar-item-value");
        HBox modBox = new HBox(6, createBarLabel("MODULES:"), modVal);
        modBox.setAlignment(Pos.CENTER_LEFT);

        Label verVal = new Label("v2.5-PROD");
        verVal.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        HBox verBox = new HBox(6, createBarLabel("BUILD:"), verVal);
        verBox.setAlignment(Pos.CENTER_LEFT);

        topStatusBar.getChildren().addAll(brandBox, clockBox, div1, connBox, div2, modBox, div3, verBox);
        return topStatusBar;
    }

    private Region createDivider() {
        Region div = new Region();
        div.getStyleClass().add("top-bar-divider");
        return div;
    }

    private Label createBarLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("top-bar-item");
        return lbl;
    }

    private void updateClock() {
        if (topClockLabel != null) {
            topClockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " UTC");
        }
    }

    // =========================================================
    // BOTTOM STATUS BAR
    // =========================================================
    private HBox createBottomStatusBar() {
        bottomStatusBar = new HBox(16);
        bottomStatusBar.getStyleClass().add("bottom-status-bar");
        bottomStatusBar.setAlignment(Pos.CENTER_LEFT);

        bottomStatusLabel = new Label("System ready • All reconnaissance modules operational");
        bottomStatusLabel.getStyleClass().add("bottom-status-text");
        HBox.setHgrow(bottomStatusLabel, Priority.ALWAYS);

        Label onlineIndicator = new Label("● ONLINE");
        onlineIndicator.getStyleClass().addAll("badge", "badge-green");

        Label clearance = new Label("CLEARANCE: ALPHA-RESTRICTED");
        clearance.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");

        bottomStatusBar.getChildren().addAll(bottomStatusLabel, clearance, onlineIndicator);
        return bottomStatusBar;
    }

    // =========================================================
    // PERSISTENT SIDEBAR NAVIGATION (Dark Glass Rail)
    // =========================================================
    private VBox createSidebar() {
        sidebarButtons.clear();

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(265);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(280);
        sidebar.setPadding(new Insets(18, 16, 16, 16));
        sidebar.setSpacing(4);

        // Classification stripe
        Label classLabel = new Label("// CLEARANCE: ALPHA-RESTRICTED //");
        classLabel.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");
        classLabel.setAlignment(Pos.CENTER);
        classLabel.setMaxWidth(Double.MAX_VALUE);

        Region stripe = new Region();
        stripe.setPrefHeight(1);
        stripe.setMaxWidth(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color: rgba(34, 211, 238, 0.16);");

        Label logo = new Label("OMEN-X");
        logo.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', sans-serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-letter-spacing: 3px; -fx-effect: dropshadow(gaussian, rgba(34, 211, 238, 0.35), 14, 0.2, 0, 0);");

        Label sub = new Label("INTELLIGENCE DASHBOARD");
        sub.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");

        VBox brand = new VBox(4, classLabel, stripe, logo, sub);
        brand.setPadding(new Insets(4, 0, 14, 6));

        Label navLabel = new Label("MODULE NAVIGATION");
        navLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        navLabel.setPadding(new Insets(8, 6, 6, 6));

        sidebar.getChildren().addAll(brand, navLabel);

        addSidebarItem(sidebar, "Dashboard", "Command Center", this::showDashboard);
        addSidebarItem(sidebar, "OSIRIS", "OSIRIS Platform", this::openOsirisLauncher);
        addSidebarItem(sidebar, "Username", "Username Recognition", () -> openModule("Username"));
        addSidebarItem(sidebar, "Email", "Email Exposure", () -> openModule("Email"));
        addSidebarItem(sidebar, "Domain", "Domain & DNS", () -> openModule("Domain"));
        addSidebarItem(sidebar, "IP Address", "IP Geolocation", () -> openModule("IP Address"));
        addSidebarItem(sidebar, "Phone", "Phone Intel", () -> openModule("Phone"));
        addSidebarItem(sidebar, "EXIF Metadata", "EXIF Metadata", this::openImageMetadata);
        addSidebarItem(sidebar, "Reverse Image", "Reverse Image", this::openReverseImageScanner);
        addSidebarItem(sidebar, "Malware Analysis", "Malware Threat", this::openMalwareAnalysis);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Operator Connection Widget
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

        Label statusText = new Label("+ SECURE RECON GATEWAY");
        statusText.getStyleClass().add("operator-status-text");

        Button logoutBtn = new Button("DISCONNECT SESSION");
        logoutBtn.getStyleClass().add("btn-ghost-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            if (clockTimeline != null) clockTimeline.stop();
            currentUsername = null;
            root.setTop(null);
            root.setLeft(null);
            root.setBottom(null);
            root.setCenter(loginUI.createView());
        });

        VBox statusBox = new VBox(8, userHeader, statusText, logoutBtn);
        statusBox.getStyleClass().add("operator-card");

        sidebar.getChildren().add(statusBox);
        return sidebar;
    }

    private void addSidebarItem(VBox sidebar, String key, String text, Runnable action) {
        Label iconLbl = new Label(com.omenx.frontend.ModuleIcons.get(key));
        iconLbl.setMinWidth(22);
        iconLbl.setPrefWidth(22);
        iconLbl.setAlignment(Pos.CENTER);
        iconLbl.setStyle("-fx-font-size: 13px;");

        Button btn = new Button(text, iconLbl);
        btn.getStyleClass().add("sidebar-button");
        btn.setGraphicTextGap(10);
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
        setStatus("Command Center active • All intelligence modules ready", MUTED);
    }

    // =========================================================
    // INVESTIGATION SCAN VIEW (Standardized with Glass Panels)
    // =========================================================
    private VBox createScanView() {

        scanHeader = new AppHeader(
                ModuleIcons.get("Username"),
                "USERNAME RECOGNITION",
                "Search public platforms & social profiles",
                this::showDashboard
        );

        // Module Selector Tag
        selectedTypeLabel = new Label("USERNAME");
        selectedTypeLabel.setPrefHeight(44);
        selectedTypeLabel.setMinWidth(130);
        selectedTypeLabel.setAlignment(Pos.CENTER);
        selectedTypeLabel.setStyle(
                "-fx-background-color: rgba(14, 20, 32, 0.90);" +
                "-fx-text-fill: #22D3EE;" +
                "-fx-border-color: rgba(34, 211, 238, 0.25);" +
                "-fx-border-radius: 8px 0 0 8px;" +
                "-fx-background-radius: 8px 0 0 8px;" +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        // Target Input
        targetField = new TextField();
        targetField.setPromptText("Enter target username, email or domain…");
        targetField.setPrefHeight(44);
        targetField.getStyleClass().add("cyber-input");
        targetField.setStyle("-fx-border-radius: 0; -fx-background-radius: 0;");
        HBox.setHgrow(targetField, Priority.ALWAYS);

        searchButton = new Button("EXECUTE SCAN");
        searchButton.setPrefHeight(44);
        searchButton.getStyleClass().add("btn-primary");
        searchButton.setStyle("-fx-border-radius: 0 8px 8px 0; -fx-background-radius: 0 8px 8px 0;");
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
        lastScanLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        HBox actionRow = new HBox(10, clearButton, exportButton, lastScanLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lastScanLabel, Priority.ALWAYS);
        lastScanLabel.setAlignment(Pos.CENTER_RIGHT);

        // Statistics Cards (Glass Panels)
        foundValue = new Label("0");
        notFoundValue = new Label("0");
        unknownValue = new Label("0");
        scannedValue = new Label("0");

        HBox stats = new HBox(
                12,
                createStatCard("Found Hits", foundValue, "metric-accent-green", GREEN),
                createStatCard("Not Found", notFoundValue, "metric-accent-cyan", MUTED),
                createStatCard("Unknown / Indeterminate", unknownValue, "metric-accent-amber", YELLOW),
                createStatCard("Platforms Polled", scannedValue, "metric-accent-cyan", CYAN)
        );

        // Results Section with Standardized Panel Header Pattern
        HBox resultsHeader = AppHeader.createPanelHeader(
                "◈",
                "INVESTIGATION FINDINGS & SOURCE TRACES",
                "READY",
                AppHeader.StatusType.READY,
                " "
        );

        resultsTarget = new Label("No target selected");
        resultsTarget.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        HBox targetSubRow = new HBox(8, new Label("TARGET:"), resultsTarget);
        targetSubRow.setAlignment(Pos.CENTER_LEFT);
        targetSubRow.setPadding(new Insets(2, 16, 8, 16));
        targetSubRow.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        // Results Container
        resultsContainer = new VBox(8);
        resultsContainer.setPadding(new Insets(8, 16, 16, 16));
        resultsContainer.getChildren().add(
                createEmptyState("Enter a target above and press Execute Scan to query OSINT intelligence feeds.")
        );

        VBox resultsCardWrapper = new VBox(4, resultsHeader, targetSubRow, resultsContainer);
        resultsCardWrapper.getStyleClass().add("glass-panel");

        resultsScroll = new ScrollPane(resultsCardWrapper);
        resultsScroll.setFitToWidth(true);
        resultsScroll.getStyleClass().add("scroll-pane");
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        // In-panel Status Bar
        statusBarLabel = new Label("Ready");
        statusBarLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        HBox statusBar = new HBox(statusBarLabel);
        statusBar.setPadding(new Insets(6, 0, 0, 0));
        statusBar.setStyle("-fx-border-color: " + BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        VBox content = new VBox(
                16,
                scanHeader,
                searchBar,
                actionRow,
                stats,
                resultsScroll,
                statusBar
        );

        content.setPadding(new Insets(20, 26, 20, 26));
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
            setStatus("Malware Threat Analysis workspace active", MUTED);
            return;
        }

        if ("IP Address".equals(type)) {
            root.setCenter(ipAddressUI.createView());
            setStatus("IP Geolocation workspace active", MUTED);
            return;
        }

        selectedType[0] = type;
        selectedTypeLabel.setText(type.toUpperCase());

        switch (type) {
            case "Email" -> {
                scanHeader.setHeaderIcon(ModuleIcons.get("Email"));
                scanHeader.setHeaderTitle("EMAIL EXPOSURE");
                scanHeader.setHeaderSubtitle("Check breach records, domain validity & email deliverability");
                targetField.setPromptText("Enter email address (e.g. user@target.com)…");
            }
            case "Domain" -> {
                scanHeader.setHeaderIcon(ModuleIcons.get("Domain"));
                scanHeader.setHeaderTitle("DOMAIN & DNS INTEL");
                scanHeader.setHeaderSubtitle("Analyze WHOIS records, DNS routing & active subdomains");
                targetField.setPromptText("Enter domain name (e.g. example.com)…");
            }
            case "Phone" -> {
                scanHeader.setHeaderIcon(ModuleIcons.get("Phone"));
                scanHeader.setHeaderTitle("PHONE INTEL");
                scanHeader.setHeaderSubtitle("Carrier metadata, validity, geographic area & telecom routing");
                targetField.setPromptText("Enter phone number with country code (e.g. +14155552671)…");
            }
            default -> {
                scanHeader.setHeaderIcon(ModuleIcons.get("Username"));
                scanHeader.setHeaderTitle("USERNAME RECOGNITION");
                scanHeader.setHeaderSubtitle("Search public platforms & social profiles for traces");
                targetField.setPromptText("Enter username…");
            }
        }

        scanHeader.setStatus(AppHeader.StatusType.READY, "READY");
        targetField.clear();
        clearResults();
        root.setCenter(scanView);
        targetField.requestFocus();
        setStatus(type + " workspace active • Enter target to scan", MUTED);
    }

    private void openImageMetadata() {
        updateSidebarActive("EXIF Metadata");
        root.setCenter(imageMetadataUI.createView());
        setStatus("EXIF Metadata & GPS forensics workspace active", MUTED);
    }

    private void openReverseImageScanner() {
        updateSidebarActive("Reverse Image");
        root.setCenter(reverseImageUI.createView());
        setStatus("Reverse Image intelligence workspace active", MUTED);
    }

    private void openMalwareAnalysis() {
        updateSidebarActive("Malware Analysis");
        root.setCenter(malwareAnalysisUI.getView());
        setStatus("Malware Threat Analysis workspace active", MUTED);
    }

    private void openOsirisLauncher() {
        updateSidebarActive("OSIRIS");
        root.setCenter(osirisLauncherUI.getView());
        setStatus("OSIRIS Global OSINT Platform • 3D Surveillance & Reconnaissance Engine", MUTED);
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
        setStatus("Scanning " + target + " across open sources…", CYAN);
        scanHeader.setStatus(AppHeader.StatusType.SCANNING, "SCANNING " + target);

        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(createEmptyState("Analyzing publicly available intelligence feeds…"));

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
                    scanResults.put(type + " Intelligence", "UNKNOWN | Intelligence module not fully connected.");
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
                scanFailed ? "Scan failed  •  check target domain" : "Scan complete  •  " + found + " findings identified",
                scanFailed ? RED : GREEN
        );

        scanHeader.setStatus(
                scanFailed ? AppHeader.StatusType.ERROR : AppHeader.StatusType.READY,
                scanFailed ? "FAILED" : "COMPLETE (" + found + ")"
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

    }

    private void addDomainRow(String label, String value) {
        if (value == null || value.isBlank()) {
            value = "—";
        }

        Label key = new Label(label);
        key.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        key.setMinWidth(140);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);

        HBox row = new HBox(16, key, val);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("activity-row");

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
            statusBarLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        }
        if (bottomStatusLabel != null) {
            bottomStatusLabel.setText(text);
            bottomStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        }
    }

    // =========================================================
    // EMAIL RESULT CARD
    // =========================================================
    private VBox createEmailResultCard(String platform, String result) {
        if (result == null) result = "UNKNOWN | EMAIL_RESULT:\nstatus: UNKNOWN\ntarget: —";

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

        String target = "—";
        String status = "—";
        String details = null;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.startsWith("target: "))  target  = line.substring(8);
            else if (line.startsWith("status: "))  status  = line.substring(8);
            else if (line.startsWith("details: ")) details = line.substring(9);
        }

        VBox card = new VBox(0);
        card.getStyleClass().add("glass-panel");

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
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", badgeClass);

        HBox cardHeader = new HBox(12, platformLabel, badge);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(12, 16, 12, 16));
        cardHeader.setStyle("-fx-border-color: transparent transparent rgba(34, 211, 238, 0.16) transparent; -fx-border-width: 0 0 1 0;");
        card.getChildren().add(cardHeader);

        VBox body_vbox = new VBox(10);
        body_vbox.setPadding(new Insets(12, 16, 14, 16));
        body_vbox.getChildren().add(createInfoRow("TARGET", target));
        body_vbox.getChildren().add(createInfoRow("BREACH STATUS", status));

        if (details != null && !details.isBlank() && "FOUND".equals(classification)) {
            String[] breachNames = details.split("\\|");
            Label breachTitle = new Label("KNOWN BREACHES  (" + breachNames.length + ")");
            breachTitle.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-padding: 6 0 2 0;");

            javafx.scene.layout.FlowPane tags = new javafx.scene.layout.FlowPane(8, 6);
            for (String name : breachNames) {
                name = name.trim();
                if (name.isEmpty()) continue;
                Label tag = new Label(name);
                tag.setStyle(
                        "-fx-background-color: rgba(239, 68, 68, 0.16);" +
                        "-fx-text-fill: #F87171;" +
                        "-fx-border-color: rgba(239, 68, 68, 0.35);" +
                        "-fx-border-radius: 4px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 8 3 8;"
                );
                tags.getChildren().add(tag);
            }
            body_vbox.getChildren().addAll(breachTitle, tags);
        } else if (details != null && !details.isBlank()) {
            body_vbox.getChildren().add(createInfoRow("NOTE", details));
        }

        card.getChildren().add(body_vbox);
        return card;
    }

    // =========================================================
    // PHONE RESULT CARD
    // =========================================================
    private VBox createPhoneResultCard(String platform, String result) {
        if (result == null) result = "UNKNOWN | PHONE_RESULT:\nnote: No data.";

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
        card.getStyleClass().add("glass-panel");

        String badgeText = "FOUND".equals(classification) ? "DATA IDENTIFIED" : classification;
        String badgeClass = "FOUND".equals(classification) ? "badge-green" : "badge-amber";

        Label platformLabel = new Label(platform);
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", badgeClass);

        HBox cardHeader = new HBox(12, platformLabel, badge);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(12, 16, 12, 16));
        cardHeader.setStyle("-fx-border-color: transparent transparent rgba(34, 211, 238, 0.16) transparent; -fx-border-width: 0 0 1 0;");
        card.getChildren().add(cardHeader);

        VBox bodyBox = new VBox(0);
        bodyBox.setPadding(new Insets(8, 16, 14, 16));

        VBox currentSection = null;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.startsWith("PHONE_RESULT:") || line.isEmpty()) continue;

            if (line.startsWith("section: ")) {
                String sectionName = line.substring(9);
                Label sectionTitle = new Label(sectionName);
                sectionTitle.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-padding: 10 0 4 0;");
                currentSection = new VBox(6);
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
                noteLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
                noteLabel.setWrapText(true);
                bodyBox.getChildren().add(noteLabel);
            }
        }

        card.getChildren().add(bodyBox);
        return card;
    }

    // =========================================================
    // INFO ROW
    // =========================================================
    private HBox createInfoRow(String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.setMinWidth(160);
        keyLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        Label valueLabel = new Label(value == null || value.isBlank() ? "—" : value);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
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
        platformLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label statusLabel = new Label();
        String url = null;

        if (result.startsWith("FOUND |")) {
            statusLabel.setText("Found");
            statusLabel.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
            url = result.substring("FOUND |".length()).trim();
        } else if (result.startsWith("NOT FOUND")) {
            statusLabel.setText("Not found");
            statusLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        } else {
            statusLabel.setText("Indeterminate");
            statusLabel.setStyle("-fx-text-fill: " + YELLOW + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
        }

        VBox left = new VBox(2, platformLabel, statusLabel);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add("activity-row");
        row.getChildren().add(left);

        if (url != null && !url.isBlank()) {
            final String targetUrl = url;
            Hyperlink link = new Hyperlink(targetUrl);
            link.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
            link.setOnAction(e -> openUrl(targetUrl));
            row.getChildren().add(link);
            HBox.setHgrow(link, Priority.ALWAYS);
        } else if (!result.startsWith("FOUND") && !result.startsWith("NOT FOUND")) {
            String details = result.contains("|") ? result.split("\\|", 2)[1].trim() : result;
            Label detailsLabel = new Label(details);
            detailsLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");
            detailsLabel.setWrapText(true);
            row.getChildren().add(detailsLabel);
        }

        return new VBox(row);
    }

    private Label createSectionHeader(String title) {
        Label header = new Label("// " + title.toUpperCase() + " //");
        header.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 14 0 4 2; -fx-letter-spacing: 1.5px;");
        return header;
    }

    private Label createEmptyState(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        label.setPadding(new Insets(24, 0, 16, 0));
        return label;
    }

    private VBox createStatCard(String title, Label value, String accentClass, String accentColor) {
        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        value.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 22px; -fx-font-weight: 800;");

        VBox card = new VBox(4, t, value);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setPrefWidth(140);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().addAll("glass-panel", accentClass);

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