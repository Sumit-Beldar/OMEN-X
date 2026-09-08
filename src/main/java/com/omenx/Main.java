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

// =========================================================
// MODEL / SERVICE / UI IMPORTS
// Keeps data storage and dedicated UI modules outside Main.java
// =========================================================
import com.omenx.model.ScanRecord;
import com.omenx.service.ScanService;
import com.omenx.service.ReportService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// =========================================================
// MAIN APPLICATION
// =========================================================
public class Main extends Application {

    // =========================================================
    // COLOR SYSTEM
    // Existing OMEN-X dashboard color system
    // =========================================================
    private static final String BG          = "#07080C";
    private static final String PANEL       = "#0E0F14";
    private static final String SURFACE     = "#131419";
    private static final String SURFACE_2   = "#18191F";
    private static final String BORDER      = "#1C1012";
    private static final String BORDER_SOFT = "#2A1518";
    private static final String TEXT        = "#E8E4E4";
    private static final String MUTED       = "#6B6060";
    private static final String MUTED_2     = "#8A7E7E";
    private static final String GREEN       = "#22C55E";
    private static final String RED         = "#DC2626";
    private static final String YELLOW      = "#EAB308";
    private static final String BLUE        = "#DC2626";
    private static final String PURPLE      = "#F97316";
    private static final String CYAN        = "#DC2626";

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

    // =========================================================
    // MAIN APPLICATION CONTAINER
    // =========================================================

    private BorderPane root;

    private VBox dashboardView;
    private VBox scanView;
    private IPAddressUI ipAddressUI;
    private MalwareAnalysisUI malwareAnalysisUI;

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
    private LoginUI loginUI;
    private String currentUsername = null;
    private final Map<String, Button> sidebarButtons = new LinkedHashMap<>();

    private void onLoginSuccess(String username) {
        this.currentUsername = username;
        root.setLeft(createSidebar());
        showDashboard();
    }

    // =========================================================
    // APPLICATION STARTUP
    // Creates services, UI modules, dashboard and scan workspace
    // =========================================================

    @Override
    public void start(Stage stage) {

        // =====================================================
        // INITIALIZE SERVICES
        // =====================================================

        scanService =
                new ScanService();

        reportService =
                new ReportService(
                        scanService
                );

        // =====================================================
        // MAIN ROOT CONTAINER
        // =====================================================

        root =
                new BorderPane();

        root.setStyle(
                "-fx-background-color: " + BG + ";"
        );

        // =====================================================
        // INITIALIZE DASHBOARD UI
        // =====================================================

        dashboardUI =
                new DashboardUI(
                        new DashboardUI.ModuleAction() {

                            @Override
                            public void open(
                                    String type
                            ) {
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
                        }
                );

        // =====================================================
        // INITIALIZE IMAGE METADATA UI
        // =====================================================

        imageMetadataUI =
                new ImageMetadataUI(
                        this::showDashboard,
                        scanService
                );

        // =====================================================
        // INITIALIZE REVERSE IMAGE UI
        // =====================================================

        reverseImageUI =
                new ReverseImageUI(
                        this::showDashboard,
                        scanService
                );

        // =====================================================
        // BUILD DASHBOARD
        // =====================================================

        dashboardView =
                dashboardUI.createDashboard(
                        scanService.getHistory()
                );

        // =====================================================
        // BUILD INVESTIGATION WORKSPACE
        // =====================================================

        scanView =
                createScanView();

        ipAddressUI = new IPAddressUI(
                this::showDashboard,
                scanService
        );

        // =====================================================
        // INITIALIZE MALWARE ANALYSIS UI
        // =====================================================
        malwareAnalysisUI = new MalwareAnalysisUI(
                stage,
                this::showDashboard,
                scanService
        );

        // =====================================================
        // INITIALIZE LOGIN UI & START ON LOGIN SCREEN
        // =====================================================

        loginUI = new LoginUI(this::onLoginSuccess);
        root.setCenter(loginUI.createView());

        // =====================================================
        // CREATE MAIN SCENE
        // =====================================================

        Scene scene =
                new Scene(
                        root,
                        1340,
                        860
                );

        try {
            if (getClass().getResource("/styles/app.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/app.css").toExternalForm()
                );
            }
        } catch (Exception ignored) {
        }

        stage.setTitle(
                "OMEN-X  •  Open Source Intelligence Platform"
        );

        stage.setMinWidth(1080);
        stage.setMinHeight(720);

        stage.setScene(scene);

        stage.show();

        // =====================================================
        // CLEANUP WHEN APPLICATION CLOSES
        // =====================================================

        stage.setOnCloseRequest(
                e -> {

                    executor.shutdownNow();

                    imageMetadataUI.shutdown();

                    reverseImageUI.shutdown();
                }
        );
    }


    // =========================================================
    // PERSISTENT SIDEBAR NAVIGATION
    // =========================================================

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.setPadding(new Insets(18, 14, 16, 14));
        sidebar.setSpacing(4);

        // Classification stripe
        Label classLabel = new Label("// RESTRICTED //");
        classLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
        classLabel.setAlignment(Pos.CENTER);
        classLabel.setMaxWidth(Double.MAX_VALUE);

        Region stripe = new Region();
        stripe.setPrefHeight(1);
        stripe.setMaxWidth(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color: #1C1012;");

        Label logo = new Label("OMEN-X");
        logo.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 24px; -fx-font-weight: bold; -fx-letter-spacing: 3px;");

        Label sub = new Label("TACTICAL OSINT PLATFORM");
        sub.setStyle("-fx-text-fill: #4A4040; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        VBox brand = new VBox(3, classLabel, stripe, logo, sub);
        brand.setPadding(new Insets(4, 0, 14, 6));

        Label navLabel = new Label("OPERATIONS");
        navLabel.setStyle("-fx-text-fill: #4A4040; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
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

        // Operator Session Box
        String displayUser = currentUsername != null ? currentUsername : "Omen-X:M1";
        Label userLabel = new Label("[OPR]  " + displayUser);
        userLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("DISCONNECT");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B6060; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-border-color: #1C1012; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        logoutBtn.setOnAction(e -> {
            currentUsername = null;
            root.setLeft(null);
            root.setCenter(loginUI.createView());
        });

        HBox userHeader = new HBox(8, userLabel, logoutBtn);
        userHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userLabel, Priority.ALWAYS);

        Label statusText = new Label("+ SYSTEMS OPERATIONAL");
        statusText.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 10px; -fx-font-weight: bold;");

        VBox statusBox = new VBox(6, userHeader, statusText);
        statusBox.setPadding(new Insets(10, 12, 10, 12));
        statusBox.setStyle("-fx-background-color: #0A0B10; -fx-border-color: #1C1012; -fx-border-radius: 6px; -fx-background-radius: 6px;");

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
    // Returns the application to the main dashboard
    // =========================================================

    private void showDashboard() {

        if (dashboardUI != null) {
            dashboardUI.refreshDashboard(
                    scanService.getHistory()
            );
        }

        root.setCenter(
                dashboardView
        );

        updateSidebarActive("Dashboard");
    }


    // =========================================================
    // INVESTIGATION SCAN VIEW
    // Builds Username, Email, Domain, IP and Phone workspace
    // =========================================================

    private VBox createScanView() {

        // =====================================================
        // HEADER
        // =====================================================

        Label title =
                new Label("OMEN-X");

        title.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 20px;" +
                " -fx-font-weight: bold;"
        );

        Label subtitle =
                new Label(
                        "Investigation Workspace"
                );

        subtitle.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        Button backBtn =
                createGhostButton(
                        "←  Dashboard"
                );

        backBtn.setOnAction(
                e -> showDashboard()
        );

        Label ready =
                new Label(
                        "● Ready"
                );

        ready.setStyle(
                "-fx-text-fill: " + GREEN +
                "; -fx-font-size: 12px;"
        );

        HBox headerRight =
                new HBox(
                        14,
                        backBtn,
                        ready
                );

        headerRight.setAlignment(
                Pos.CENTER_RIGHT
        );

        HBox header =
                new HBox(
                        16,
                        new VBox(
                                2,
                                title,
                                subtitle
                        ),
                        headerRight
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                header.getChildren().get(0),
                Priority.ALWAYS
        );


        // =====================================================
        // MODULE SELECTOR
        // =====================================================

        selectedTypeLabel =
                new Label(
                        "USERNAME"
                );

        selectedTypeLabel.setPrefHeight(42);
        selectedTypeLabel.setMinWidth(130);

        selectedTypeLabel.setAlignment(
                Pos.CENTER
        );

        selectedTypeLabel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px 0 0 7px;" +
                "-fx-background-radius: 7px 0 0 7px;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );


        // =====================================================
        // TARGET INPUT
        // =====================================================

        targetField =
                new TextField();

        targetField.setPromptText(
                "Enter target username, email or domain…"
        );

        targetField.setPrefHeight(44);
        targetField.getStyleClass().add("cyber-input");

        HBox.setHgrow(
                targetField,
                Priority.ALWAYS
        );

        searchButton =
                new Button("EXECUTE SCAN");

        searchButton.setPrefHeight(44);
        searchButton.getStyleClass().add("btn-primary");

        searchButton.setOnAction(
                e -> runScan()
        );

        targetField.setOnAction(
                e -> runScan()
        );

        HBox searchBar =
                new HBox(
                        10,
                        selectedTypeLabel,
                        targetField,
                        searchButton
                );

        clearButton =
                new Button("Clear");
        clearButton.getStyleClass().add("btn-ghost");

        clearButton.setOnAction(
                e -> clearResults()
        );

        exportButton =
                new Button("Copy Results");
        exportButton.getStyleClass().add("btn-ghost");

        exportButton.setOnAction(
                e -> copyResults()
        );

        lastScanLabel =
                new Label("");

        lastScanLabel.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 11px;"
        );

        HBox actionRow =
                new HBox(
                        10,
                        clearButton,
                        exportButton,
                        lastScanLabel
                );

        actionRow.setAlignment(
                Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                lastScanLabel,
                Priority.ALWAYS
        );

        lastScanLabel.setAlignment(
                Pos.CENTER_RIGHT
        );


        // =====================================================
        // STATISTICS
        // =====================================================

        foundValue =
                new Label("0");

        notFoundValue =
                new Label("0");

        unknownValue =
                new Label("0");

        scannedValue =
                new Label("0");

        HBox stats =
                new HBox(
                        10,
                        createStatCard(
                                "Found",
                                foundValue,
                                GREEN
                        ),
                        createStatCard(
                                "Not Found",
                                notFoundValue,
                                RED
                        ),
                        createStatCard(
                                "Unknown",
                                unknownValue,
                                YELLOW
                        ),
                        createStatCard(
                                "Platforms",
                                scannedValue,
                                TEXT
                        )
                );


        // =====================================================
        // RESULTS HEADER
        // =====================================================

        Label resultsTitle =
                new Label("Results");

        resultsTitle.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 14px;" +
                " -fx-font-weight: bold;"
        );

        resultsTarget =
                new Label(
                        "No target selected"
                );

        resultsTarget.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        HBox resultsHeader =
                new HBox(
                        12,
                        resultsTitle,
                        resultsTarget
                );

        resultsHeader.setAlignment(
                Pos.CENTER_LEFT
        );


        // =====================================================
        // RESULTS CONTAINER
        // =====================================================

        resultsContainer =
                new VBox(8);

        resultsContainer.setPadding(
                new Insets(
                        4,
                        0,
                        8,
                        0
                )
        );

        resultsContainer.getChildren().add(
                createEmptyState(
                        "Enter a target and press Scan to begin investigation."
                )
        );

        resultsScroll =
                new ScrollPane(
                        resultsContainer
                );

        resultsScroll.setFitToWidth(true);

        resultsScroll.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;"
        );

        resultsScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        resultsScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        VBox.setVgrow(
                resultsScroll,
                Priority.ALWAYS
        );


        // =====================================================
        // STATUS BAR
        // =====================================================

        statusBarLabel =
                new Label("Ready");

        statusBarLabel.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 11px;"
        );

        HBox statusBar =
                new HBox(
                        statusBarLabel
                );

        statusBar.setPadding(
                new Insets(
                        8,
                        0,
                        0,
                        0
                )
        );

        statusBar.setStyle(
                "-fx-border-color: " +
                BORDER +
                " transparent transparent transparent;" +
                "-fx-border-width: 1 0 0 0;"
        );


        // =====================================================
        // FINAL CONTENT
        // =====================================================

        VBox content =
                new VBox(
                        20,
                        header,
                        searchBar,
                        actionRow,
                        stats,
                        resultsHeader,
                        resultsScroll,
                        statusBar
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

        VBox.setVgrow(
                resultsScroll,
                Priority.ALWAYS
        );

        return content;
    }


    // =========================================================
    // OPEN MODULE
    // Selects Username / Email / Domain / IP / Phone
    // =========================================================

    private void openModule(
            String type
    ) {
        updateSidebarActive(type);

        if ("Malware Analysis".equals(type)) {
            root.setCenter(malwareAnalysisUI.getView());
            return;
        }

        if ("IP Address".equals(type)) {
            root.setCenter(
                    ipAddressUI.createView()
            );
            return;
        }

        selectedType[0] =
                type;

        selectedTypeLabel.setText(
                type.toUpperCase()
        );

        switch (type) {

            case "Email" ->
                    targetField.setPromptText(
                            "Enter email address…"
                    );

            case "Domain" ->
                    targetField.setPromptText(
                            "Enter domain name…"
                    );

            case "IP Address" ->
                    targetField.setPromptText(
                            "Enter IP address…"
                    );

            case "Phone" ->
                    targetField.setPromptText(
                            "Enter phone number…"
                    );

            default ->
                    targetField.setPromptText(
                            "Enter username…"
                    );
        }

        targetField.clear();

        clearResults();

        root.setCenter(
                scanView
        );

        targetField.requestFocus();
    }


    // =========================================================
    // OPEN IMAGE METADATA MODULE
    // Delegates the UI to ImageMetadataUI
    // =========================================================

    private void openImageMetadata() {
        updateSidebarActive("EXIF Metadata");
        root.setCenter(
                imageMetadataUI.createView()
        );
    }


    // =========================================================
    // OPEN REVERSE IMAGE MODULE
    // Delegates the UI to ReverseImageUI
    // =========================================================

    private void openReverseImageScanner() {
        updateSidebarActive("Reverse Image");
        root.setCenter(
                reverseImageUI.createView()
        );
    }


    // =========================================================
    // OPEN MALWARE ANALYSIS MODULE
    // Delegates the file analysis UI to MalwareAnalysisUI
    // =========================================================
    private void openMalwareAnalysis() {
        updateSidebarActive("Malware Analysis");
        root.setCenter(malwareAnalysisUI.getView());
    }


    // =========================================================
    // RUN SCAN
    // Executes the selected scanner in the background
    // =========================================================

    private void runScan() {

        String type =
                selectedType[0];

        String target =
                targetField
                        .getText()
                        .trim();

        // =====================================================
        // VALIDATE TARGET
        // =====================================================

        if (target.isEmpty()) {

            setStatus(
                    "Please enter a target",
                    RED
            );

            resultsContainer
                    .getChildren()
                    .clear();

            resultsContainer
                    .getChildren()
                    .add(
                            createEmptyState(
                                    "Target cannot be empty."
                            )
                    );

            return;
        }


        // =====================================================
        // LOADING STATE
        // =====================================================

        searchButton.setDisable(true);

        searchButton.setText("…");

        setStatus(
                "Scanning " + target + "…",
                GREEN
        );

        resultsContainer
                .getChildren()
                .clear();

        resultsContainer
                .getChildren()
                .add(
                        createEmptyState(
                                "Analyzing publicly available sources…"
                        )
                );

        resultsTarget.setText(
                target
        );

        foundValue.setText("0");
        notFoundValue.setText("0");
        unknownValue.setText("0");
        scannedValue.setText("0");


        // =====================================================
        // BACKGROUND SCAN
        // =====================================================

        executor.submit(() -> {

            Map<String, String> scanResults;

            try {

                // =================================================
                // USERNAME SCANNER
                // =================================================

                if ("Username".equals(type)) {

                    UsernameScanner scanner =
                            new UsernameScanner();

                    scanResults =
                            scanner.scan(target);
                }

                // =================================================
                // EMAIL SCANNER
                // =================================================

                else if ("Email".equals(type)) {

                    EmailScanner scanner =
                            new EmailScanner();

                    String emailResult =
                            scanner.scan(target);

                    scanResults =
                            new LinkedHashMap<>();

                    scanResults.put(
                            "Email Intelligence",
                            emailResult
                    );
                }

                // =================================================
                // DOMAIN SCANNER
                // =================================================

                else if ("Domain".equals(type)) {

                    DomainScanner scanner =
                            new DomainScanner();

                    scanResults =
                            scanner.scan(target);
                }

                // =================================================
                // OTHER MODULES
                // =================================================

                else if ("Phone".equals(type)) {

    PhoneScanner scanner =
            new PhoneScanner();

    PhoneScanner.PhoneResult phoneResult =
            scanner.scan(
                    target,
                    "IN"
            );

    scanResults =
            new LinkedHashMap<>();

    scanResults.put(
            "Phone Intelligence",
            buildPhoneResult(phoneResult)
    );

} else {

    scanResults =
            new LinkedHashMap<>();

    scanResults.put(
            type + " Intelligence",
            "UNKNOWN | This intelligence module is not fully connected yet."
    );
}

            } catch (Exception ex) {

                scanResults =
                        new LinkedHashMap<>();

                scanResults.put(
                        "Error",
                        "UNKNOWN | " +
                        ex.getMessage()
                );
            }


            Map<String, String> finalResults =
                    scanResults;


            // =====================================================
            // RETURN TO JAVAFX THREAD
            // =====================================================

            Platform.runLater(() -> {

                renderResults(
                        finalResults,
                        type,
                        target
                );

                searchButton.setDisable(false);

                searchButton.setText(
                        "Scan"
                );
            });
        });
    }


    // =========================================================
    // RENDER RESULTS
    // Converts scanner results into visual result rows
    // =========================================================

    private void renderResults(
            Map<String, String> scanResults,
            String type,
            String target
    ) {

        resultsContainer
                .getChildren()
                .clear();

        int found = 0;
        int notFound = 0;
        int unknown = 0;


        // =====================================================
        // DOMAIN RESULTS
        // =====================================================

        if ("Domain".equals(type)) {

            renderDomainResults(
                    scanResults
            );

            found =
                    (int)
                    scanResults
                            .values()
                            .stream()
                            .filter(
                                    v ->
                                            v != null &&
                                            !v.isBlank()
                            )
                            .count();

            unknown = 1;
        }

        // =====================================================
        // NORMAL RESULTS
        // =====================================================

        else {

            if (scanResults.isEmpty()) {

                resultsContainer
                        .getChildren()
                        .add(
                                createEmptyState(
                                        "No results returned."
                                )
                        );

            } else {

                for (
                        Map.Entry<String, String> entry :
                        scanResults.entrySet()
                ) {

                    String platform =
                            entry.getKey();

                    String result =
                            entry.getValue();

                    if (
                            result != null &&
                            result.startsWith("FOUND")
                    ) {

                        found++;

                    } else if (
                            result != null &&
                            result.startsWith("NOT FOUND")
                    ) {

                        notFound++;

                    } else {

                        unknown++;
                    }

                    resultsContainer
                            .getChildren()
                            .add(
                                    createResultRow(
                                            platform,
                                            result
                                    )
                            );
                }
            }
        }


        // =====================================================
        // UPDATE STATISTICS
        // =====================================================

        foundValue.setText(
                String.valueOf(found)
        );

        notFoundValue.setText(
                String.valueOf(notFound)
        );

        unknownValue.setText(
                String.valueOf(unknown)
        );

        scannedValue.setText(
                String.valueOf(
                        scanResults.size()
                )
        );


        // =====================================================
        // SAVE SCAN TO DATABASE
        // =====================================================

        String time =
                LocalDateTime
                        .now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "HH:mm"
                                )
                        );

        scanService.saveScan(
                type,
                target,
                found,
                time
        );
        // =====================================================
// REFRESH DASHBOARD METRICS
// =====================================================

dashboardUI.refreshDashboard(
        scanService.getHistory()
);


        // =====================================================
        // UPDATE STATUS
        // =====================================================

        lastScanLabel.setText(
                "Last scan  •  " + time
        );

        setStatus(
                "Scan complete  •  " +
                found +
                " found",
                GREEN
        );
    }


    // =========================================================
    // DOMAIN RESULTS
    // Displays detailed DomainScanner information
    // =========================================================

    private void renderDomainResults(
            Map<String, String> data
    ) {

        if (data.containsKey("error")) {

            resultsContainer
                    .getChildren()
                    .add(
                            createEmptyState(
                                    data.get("error")
                            )
                    );

            return;
        }


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "Domain Information"
                        )
                );

        addDomainRow(
                "Domain",
                data.get("domain")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "DNS Records"
                        )
                );

        addDomainRow(
                "A Records",
                data.get("dns_a")
        );

        addDomainRow(
                "MX Records",
                data.get("dns_mx")
        );

        addDomainRow(
                "NS Records",
                data.get("dns_ns")
        );

        addDomainRow(
                "TXT Records",
                data.get("dns_txt")
        );

        addDomainRow(
                "CNAME",
                data.get("dns_cname")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "Web Presence"
                        )
                );

        addDomainRow(
                "HTTPS",
                data.get("https")
        );

        addDomainRow(
                "HTTP",
                data.get("http")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "SSL Certificate"
                        )
                );

        addDomainRow(
                "Subject",
                data.get("ssl_subject")
        );

        addDomainRow(
                "Issuer",
                data.get("ssl_issuer")
        );

        addDomainRow(
                "Validity",
                data.get("ssl_validity")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "Common Subdomains"
                        )
                );

        addDomainRow(
                "Found",
                data.get("subdomains")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "WHOIS"
                        )
                );

        addDomainRow(
                "Registrar",
                data.get("whois_registrar")
        );

        addDomainRow(
                "Organization",
                data.get("whois_org")
        );

        addDomainRow(
                "Created",
                data.get("whois_created")
        );

        addDomainRow(
                "Expires",
                data.get("whois_expires")
        );

        addDomainRow(
                "Updated",
                data.get("whois_updated")
        );

        addDomainRow(
                "WHOIS Server",
                data.get("whois_server")
        );


        resultsContainer
                .getChildren()
                .add(
                        createSectionHeader(
                                "Reputation"
                        )
                );

        addDomainRow(
                "Blacklist Status",
                data.get("reputation")
        );
    }


    // =========================================================
    // DOMAIN RESULT ROW
    // =========================================================

    private void addDomainRow(
            String label,
            String value
    ) {

        if (
                value == null ||
                value.isBlank()
        ) {

            value = "—";
        }

        Label key =
                new Label(label);

        key.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        key.setMinWidth(130);


        Label val =
                new Label(value);

        val.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 12px;"
        );

        val.setWrapText(true);

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
                        8,
                        14,
                        8,
                        14
                )
        );

        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );

        resultsContainer
                .getChildren()
                .add(row);
    }


    // =========================================================
    // CLEAR RESULTS
    // =========================================================

    private void clearResults() {

        resultsContainer
                .getChildren()
                .clear();

        resultsContainer
                .getChildren()
                .add(
                        createEmptyState(
                                "Enter a target and press Scan to begin investigation."
                        )
                );

        resultsTarget.setText(
                "No target selected"
        );

        foundValue.setText("0");
        notFoundValue.setText("0");
        unknownValue.setText("0");
        scannedValue.setText("0");

        lastScanLabel.setText("");

        setStatus(
                "Ready",
                MUTED
        );
    }


    // =========================================================
    // COPY RESULTS
    // Copies the current investigation results
    // =========================================================

    private void copyResults() {

        if (
                resultsContainer
                        .getChildren()
                        .isEmpty()
        ) {

            return;
        }

        StringBuilder sb =
                new StringBuilder();

        sb.append(
                "OMEN-X • "
        )
        .append(
                selectedType[0]
        )
        .append(
                " scan\n"
        );

        sb.append(
                "Target: "
        )
        .append(
                resultsTarget.getText()
        )
        .append(
                "\n\n"
        );


        for (
                var node :
                resultsContainer.getChildren()
        ) {

            if (
                    node instanceof VBox outer &&
                    !outer.getChildren().isEmpty() &&
                    outer.getChildren().get(0)
                            instanceof HBox row
            ) {

                for (
                        var child :
                        row.getChildren()
                ) {

                    if (
                            child instanceof VBox left &&
                            left.getChildren().size() >= 2
                    ) {

                        Label platform =
                                (Label)
                                left.getChildren()
                                        .get(0);

                        Label status =
                                (Label)
                                left.getChildren()
                                        .get(1);

                        sb.append(
                                platform.getText()
                        )
                        .append(
                                "  →  "
                        )
                        .append(
                                status.getText()
                        )
                        .append(
                                "\n"
                        );
                    }
                }
            }
        }


        ClipboardContent content =
                new ClipboardContent();

        content.putString(
                sb.toString()
        );

        Clipboard
                .getSystemClipboard()
                .setContent(content);

        setStatus(
                "Results copied to clipboard",
                GREEN
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    private void setStatus(
            String text,
            String color
    ) {

        if (statusBarLabel == null) {
            return;
        }

        statusBarLabel.setText(
                text
        );

        statusBarLabel.setStyle(
                "-fx-text-fill: " +
                color +
                "; -fx-font-size: 11px;"
        );
    }


    // =========================================================
    // RESULT ROW
    // Creates a visual row for scanner results
    // =========================================================

    private VBox createResultRow(
            String platform,
            String result
    ) {

        if (result == null) {
            result = "UNKNOWN";
        }

        Label platformLabel =
                new Label(platform);

        platformLabel.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 13px;" +
                " -fx-font-weight: bold;"
        );


        Label statusLabel =
                new Label();

        String url = null;


        if (
                result.startsWith("FOUND |")
        ) {

            statusLabel.setText(
                    "Found"
            );

            statusLabel.setStyle(
                    "-fx-text-fill: " +
                    GREEN +
                    "; -fx-font-size: 12px;" +
                    " -fx-font-weight: bold;"
            );

            url =
                    result
                            .substring(
                                    "FOUND |".length()
                            )
                            .trim();

        } else if (
                result.startsWith("NOT FOUND")
        ) {

            statusLabel.setText(
                    "Not found"
            );

            statusLabel.setStyle(
                    "-fx-text-fill: " +
                    RED +
                    "; -fx-font-size: 12px;"
            );

        } else {

            statusLabel.setText(
                    "Unknown"
            );

            statusLabel.setStyle(
                    "-fx-text-fill: " +
                    YELLOW +
                    "; -fx-font-size: 12px;"
            );
        }


        VBox left =
                new VBox(
                        3,
                        platformLabel,
                        statusLabel
                );


        HBox row =
                new HBox(16);

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        row.setPadding(
                new Insets(
                        13,
                        16,
                        13,
                        16
                )
        );

        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );

        row.getChildren()
                .add(left);


        if (
                url != null &&
                !url.isBlank()
        ) {

            final String targetUrl =
                    url;

            Hyperlink link =
                    new Hyperlink(
                            targetUrl
                    );

            link.setStyle(
                    "-fx-text-fill: " +
                    BLUE +
                    "; -fx-font-size: 12px;"
            );

            link.setOnAction(
                    e -> openUrl(targetUrl)
            );

            row.getChildren()
                    .add(link);

            HBox.setHgrow(
                    link,
                    Priority.ALWAYS
            );

        } else if (
                !result.startsWith("FOUND") &&
                !result.startsWith("NOT FOUND")
        ) {

            String details =
                    result.contains("|")
                            ? result.split(
                                    "\\|",
                                    2
                            )[1].trim()
                            : result;

            Label detailsLabel =
                    new Label(details);

            detailsLabel.setStyle(
                    "-fx-text-fill: " +
                    MUTED +
                    "; -fx-font-size: 12px;"
            );

            detailsLabel.setWrapText(
                    true
            );

            row.getChildren()
                    .add(detailsLabel);
        }


        // =====================================================
        // HOVER EFFECT
        // =====================================================

        row.setOnMouseEntered(
                e -> row.setStyle(
                        "-fx-background-color: " +
                        SURFACE_2 +
                        ";" +
                        "-fx-border-color: " +
                        BORDER_SOFT +
                        ";" +
                        "-fx-border-radius: 7px;" +
                        "-fx-background-radius: 7px;"
                )
        );

        row.setOnMouseExited(
                e -> row.setStyle(
                        "-fx-background-color: " +
                        SURFACE +
                        ";" +
                        "-fx-border-color: " +
                        BORDER +
                        ";" +
                        "-fx-border-radius: 7px;" +
                        "-fx-background-radius: 7px;"
                )
        );


        return new VBox(row);
    }


    // =========================================================
    // SECTION HEADER
    // =========================================================

    private Label createSectionHeader(
            String title
    ) {

        Label header =
                new Label(title);

        header.setStyle(
                "-fx-text-fill: " + TEXT +
                ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 16 0 6 2;"
        );

        return header;
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
                "-fx-text-fill: " +
                MUTED +
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
    // STAT CARD
    // =========================================================

    private VBox createStatCard(
            String title,
            Label value,
            String accent
    ) {

        Label t =
                new Label(
                        title.toUpperCase()
                );

        t.setStyle(
                "-fx-text-fill: " +
                MUTED +
                "; -fx-font-size: 10px;" +
                " -fx-font-weight: bold;"
        );

        value.setStyle(
                "-fx-text-fill: " +
                accent +
                "; -fx-font-size: 22px;" +
                " -fx-font-weight: bold;"
        );

        VBox card =
                new VBox(
                        4,
                        t,
                        value
                );

        card.setPadding(
                new Insets(
                        12,
                        16,
                        12,
                        16
                )
        );

        card.setPrefWidth(
                140
        );

        card.setStyle(
                "-fx-background-color: " +
                PANEL +
                ";" +
                "-fx-border-color: " +
                BORDER +
                ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );

        return card;
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

        button.setOnMouseEntered(
                e -> button.setStyle(
                        "-fx-background-color: " +
                        SURFACE +
                        ";" +
                        "-fx-text-fill: " +
                        TEXT +
                        ";" +
                        "-fx-border-color: " +
                        BORDER_SOFT +
                        ";" +
                        "-fx-border-radius: 6px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 6 14;" +
                        "-fx-cursor: hand;"
                )
        );

        button.setOnMouseExited(
                e -> button.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: " +
                        MUTED +
                        ";" +
                        "-fx-border-color: " +
                        BORDER +
                        ";" +
                        "-fx-border-radius: 6px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 6 14;" +
                        "-fx-cursor: hand;"
                )
        );

        return button;
    }


    // =========================================================
    // PHONE RESULT FORMATTER
    // Converts the detailed PhoneScanner result into text that
    // can be rendered by the existing result-row UI.
    // =========================================================
    private String buildPhoneResult(PhoneScanner.PhoneResult result) {

        if (result == null) {
            return "UNKNOWN | No phone intelligence was returned.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("FOUND | ");
        sb.append("BASIC INFORMATION\n");
        sb.append("Phone Number: ").append(safe(result.getInputNumber())).append("\n");
        sb.append("Country: ").append(safe(result.getCountry())).append("\n");
        sb.append("Country Code: ").append(safe(result.getCountryCode())).append("\n");
        sb.append("National Number: ").append(safe(result.getNationalNumber())).append("\n");
        sb.append("International Format: ").append(safe(result.getInternationalFormat())).append("\n");
        sb.append("National Format: ").append(safe(result.getNationalFormat())).append("\n");
        sb.append("E.164 Format: ").append(safe(result.getE164Format())).append("\n\n");

        sb.append("VALIDATION\n");
        sb.append("Possible Number: ").append(result.isPossible() ? "Yes" : "No").append("\n");
        sb.append("Valid Number: ").append(result.isValid() ? "Yes" : "No").append("\n");
        sb.append("Number Type: ").append(safe(result.getNumberType())).append("\n");
        sb.append("Country/Region Match: ").append(result.isRegionMatch() ? "Yes" : "No").append("\n\n");

        sb.append("CARRIER / NETWORK\n");
        sb.append("Original Carrier: ").append(safe(result.getOriginalCarrier())).append("\n");
        sb.append("Line Type: ").append(safe(result.getNumberType())).append("\n\n");

        sb.append("LOCATION\n");
        sb.append("Region: ").append(safe(result.getRegion())).append("\n");
        sb.append("Geographic Area: ").append(safe(result.getGeographicDescription())).append("\n");
        sb.append("Region Description: ").append(safe(result.getRegionDescription())).append("\n");
        sb.append("Time Zone(s): ").append(safe(result.getTimezones())).append("\n\n");

        sb.append("OSINT / REPUTATION\n");
        sb.append("Spam Reports: Not checked\n");
        sb.append("Fraud Reports: Not checked\n");
        sb.append("Public Caller Name: Not available\n");
        sb.append("Public References: Not checked");

        return sb.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
    }


    // =========================================================
    // OPEN URL
    // =========================================================

    private void openUrl(
            String url
    ) {

        try {

            if (
                    Desktop.isDesktopSupported()
            ) {

                Desktop
                        .getDesktop()
                        .browse(
                                new URI(url)
                        );
            }

        } catch (Exception ex) {

            setStatus(
                    "Could not open link",
                    RED
            );
        }
    }


    // =========================================================
    // APPLICATION ENTRY POINT
    // =========================================================

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}