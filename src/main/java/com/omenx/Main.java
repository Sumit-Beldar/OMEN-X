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
    root.setCenter(
        new ImageMetadataUI(
            () -> root.setCenter(dashboardView)
        ).createView()
    );
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
                        this::showDashboard
                );

        // =====================================================
        // INITIALIZE REVERSE IMAGE UI
        // =====================================================

        reverseImageUI =
                new ReverseImageUI(
                        this::showDashboard
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

        // =====================================================
        // SHOW DASHBOARD
        // =====================================================

        root.setCenter(
                dashboardView
        );

        // =====================================================
        // CREATE MAIN SCENE
        // =====================================================

        Scene scene =
                new Scene(
                        root,
                        1240,
                        820
                );

        stage.setTitle(
                "OMEN-X  •  Open Source Intelligence"
        );


        stage.setMinWidth(980);
        stage.setMinHeight(680);

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
    // SHOW DASHBOARD
    // Returns the application to the main dashboard
    // =========================================================

    private void showDashboard() {

        if (dashboardUI != null) {

            dashboardUI.refreshRecentActivity(
                    scanService.getHistory()
            );
        }

        root.setCenter(
                dashboardView
        );
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
                "Enter username…"
        );

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

        HBox.setHgrow(
                targetField,
                Priority.ALWAYS
        );


        // =====================================================
        // SCAN BUTTON
        // =====================================================

        searchButton =
                new Button("Scan");

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

        searchButton.setOnAction(
                e -> runScan()
        );

        targetField.setOnAction(
                e -> runScan()
        );


        HBox searchBar =
                new HBox(
                        selectedTypeLabel,
                        targetField,
                        searchButton
                );


        // =====================================================
        // ACTION BUTTONS
        // =====================================================

        clearButton =
                createGhostButton(
                        "Clear"
                );

        clearButton.setOnAction(
                e -> clearResults()
        );

        exportButton =
                createGhostButton(
                        "Copy Results"
                );

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

        root.setCenter(
                imageMetadataUI.createView()
        );
    }


    // =========================================================
    // OPEN REVERSE IMAGE MODULE
    // Delegates the UI to ReverseImageUI
    // =========================================================

    private void openReverseImageScanner() {

        root.setCenter(
                reverseImageUI.createView()
        );
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

                else {

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