package com.omenx.ui;

import com.omenx.IPScanner;
import com.omenx.IPScanner.IPResult;
import com.omenx.service.ScanService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// =========================================================
// IP ADDRESS UI
//
// Provides:
// - IP input
// - IP intelligence
// - Location information
// - Network information
// - Security / reputation information
// - Coordinates
// - OpenStreetMap integration
// - ScanService integration
// =========================================================

public class IPAddressUI {

    // =========================================================
    // COLORS
    // =========================================================

    private static final String BG = "#0A0E12";
    private static final String PANEL = "#11161C";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String BLUE = "#5B9BD5";
    private static final String ERROR = "#E57373";
    private static final String SUCCESS = "#81C784";

    // =========================================================
    // SERVICES
    // =========================================================

    private final IPScanner scanner;
    private final ScanService scanService;
    private final Runnable backAction;

    // =========================================================
    // UI
    // =========================================================

    private Label statusLabel;
    private VBox resultsContainer;

    private TextField ipField;
    private Button scanButton;

    // =========================================================
    // LAST RESULT
    // =========================================================

    private IPResult lastResult;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public IPAddressUI(
            Runnable backAction,
            ScanService scanService
    ) {

        this.backAction = backAction;
        this.scanService = scanService;
        this.scanner = new IPScanner();
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public BorderPane createView() {

        BorderPane root = new BorderPane();

        root.setStyle(
                "-fx-background-color: "
                        + BG
                        + ";"
        );

        // =====================================================
        // HEADER
        // =====================================================

        Label title =
                new Label(
                        "IP Address Scanner"
                );

        title.setStyle(
                "-fx-text-fill: "
                        + TEXT
                        + ";"
                        + "-fx-font-size: 20px;"
                        + "-fx-font-weight: bold;"
        );

        Button backButton =
                new Button(
                        "←  Dashboard"
                );

        backButton.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: "
                        + MUTED
                        + ";"
                        + "-fx-border-color: "
                        + BORDER
                        + ";"
                        + "-fx-border-radius: 6px;"
                        + "-fx-background-radius: 6px;"
                        + "-fx-padding: 6 14;"
                        + "-fx-cursor: hand;"
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
                javafx.scene.layout.Priority.ALWAYS
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
                        "Investigate an IP address and retrieve available network intelligence."
                );

        subtitle.setStyle(
                "-fx-text-fill: "
                        + MUTED
                        + ";"
                        + "-fx-font-size: 12px;"
        );

        // =====================================================
        // INPUT
        // =====================================================

        ipField =
                new TextField();

        ipField.setPromptText(
                "Enter IP address (e.g., 8.8.8.8)..."
        );

        ipField.setPrefHeight(42);
        ipField.getStyleClass().add("cyber-input");

        scanButton =
                new Button(
                        "Scan IP"
                );

        scanButton.setPrefHeight(42);
        scanButton.getStyleClass().add("btn-primary");

        scanButton.setOnAction(
                e -> scanIP()
        );

        ipField.setOnAction(
                e -> scanIP()
        );

        HBox inputBox =
                new HBox(
                        10,
                        ipField,
                        scanButton
                );

        inputBox.setAlignment(
                Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                ipField,
                javafx.scene.layout.Priority.ALWAYS
        );

        // =====================================================
        // STATUS
        // =====================================================

        statusLabel =
                new Label(
                        "Enter an IP address to begin."
                );

        statusLabel.setStyle(
                "-fx-text-fill: "
                        + MUTED
                        + ";"
                        + "-fx-font-size: 12px;"
        );

        // =====================================================
        // RESULTS
        // =====================================================

        resultsContainer =
                new VBox(
                        18
                );

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        VBox content =
                new VBox(
                        14,
                        subtitle,
                        inputBox,
                        statusLabel,
                        resultsContainer
                );

        content.setPadding(
                new Insets(
                        10,
                        32,
                        32,
                        32
                )
        );

        ScrollPane scrollPane =
        new ScrollPane(
                content
        );

scrollPane.setFitToWidth(
        true
);

scrollPane.setFitToHeight(
        false
);

scrollPane.setHbarPolicy(
        ScrollPane.ScrollBarPolicy.NEVER
);

scrollPane.setVbarPolicy(
        ScrollPane.ScrollBarPolicy.AS_NEEDED
);

scrollPane.setStyle(
        "-fx-background: "
                + BG
                + ";"
                + "-fx-background-color: "
                + BG
                + ";"
);

root.setCenter(
        scrollPane
);

return root;
    }

    // =========================================================
    // SCAN IP
    // =========================================================

    private void scanIP() {

        String ip =
                ipField
                        .getText()
                        .trim();

        if (ip.isEmpty()) {

            statusLabel.setText(
                    "Please enter an IP address."
            );

            statusLabel.setStyle(
                    "-fx-text-fill: "
                            + ERROR
                            + ";"
            );

            return;
        }

        scanButton.setDisable(
                true
        );

        ipField.setDisable(
                true
        );

        statusLabel.setText(
                "Scanning IP address..."
        );

        statusLabel.setStyle(
                "-fx-text-fill: "
                        + MUTED
                        + ";"
        );

        resultsContainer
                .getChildren()
                .clear();

        // =====================================================
        // BACKGROUND THREAD
        // =====================================================

        Thread thread =
                new Thread(
                        () -> {

                            try {

                                IPResult result =
                                        scanner.scan(
                                                ip
                                        );

                                Platform.runLater(
                                        () -> {

                                            lastResult =
                                                    result;

                                            displayResult(
                                                    result
                                            );

                                            saveScan(
                                                    result
                                            );

                                            scanButton
                                                    .setDisable(
                                                            false
                                                    );

                                            ipField
                                                    .setDisable(
                                                            false
                                                    );
                                        }
                                );

                            } catch (Exception ex) {

                                Platform.runLater(
                                        () -> {

                                            statusLabel
                                                    .setText(
                                                            "Scan failed: "
                                                                    + ex.getMessage()
                                                    );

                                            statusLabel
                                                    .setStyle(
                                                            "-fx-text-fill: "
                                                                    + ERROR
                                                                    + ";"
                                                    );

                                            scanButton
                                                    .setDisable(
                                                            false
                                                    );

                                            ipField
                                                    .setDisable(
                                                            false
                                                    );
                                        }
                                );
                            }
                        }
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    // =========================================================
    // DISPLAY RESULT
    // =========================================================

    private void displayResult(
            IPResult result
    ) {

        statusLabel.setText(
                "Scan completed successfully."
        );

        statusLabel.setStyle(
                "-fx-text-fill: "
                        + SUCCESS
                        + ";"
        );

        // =====================================================
        // LOCATION
        // =====================================================

        Label locationTitle =
                sectionTitle(
                        "LOCATION"
                );

        GridPane locationGrid =
                createGrid();

        addRow(
                locationGrid,
                0,
                "City",
                result.getCity()
        );

        addRow(
                locationGrid,
                1,
                "Region",
                safe(
                        result.getRegion()
                )
                        + " ("
                        + safe(
                        result.getRegionCode()
                )
                        + ")"
        );

        addRow(
                locationGrid,
                2,
                "Postal code",
                result.getPostalCode()
        );

        addRow(
                locationGrid,
                3,
                "Country",
                safe(
                        result.getCountry()
                )
                        + " ("
                        + safe(
                        result.getCountryCode()
                )
                        + ")"
        );

        addRow(
                locationGrid,
                4,
                "Continent",
                safe(
                        result.getContinent()
                )
                        + " ("
                        + safe(
                        result.getContinentCode()
                )
                        + ")"
        );

        addRow(
                locationGrid,
                5,
                "Coordinates",
                result.getCoordinates()
        );

        addRow(
                locationGrid,
                6,
                "Timezone",
                result.getTimezone()
        );

        VBox locationPanel =
                createPanel(
                        locationTitle,
                        locationGrid
                );

        // =====================================================
        // NETWORK
        // =====================================================

        Label networkTitle =
                sectionTitle(
                        "NETWORK"
                );

        GridPane networkGrid =
                createGrid();

        addRow(
                networkGrid,
                0,
                "IP address",
                result.getIp()
        );

        addRow(
                networkGrid,
                1,
                "IP version",
                result.getIpVersion()
        );

        addRow(
                networkGrid,
                2,
                "Public IP",
                result.getPublicIp()
        );

        addRow(
                networkGrid,
                3,
                "Hostname",
                result.getHostname()
        );

        addRow(
                networkGrid,
                4,
                "ISP / Provider",
                result.getProvider()
        );

        addRow(
                networkGrid,
                5,
                "Organization",
                result.getOrganization()
        );

        addRow(
                networkGrid,
                6,
                "ASN",
                result.getAsn()
        );

        addRow(
                networkGrid,
                7,
                "Domain",
                result.getAbuseDomain()
        );

        addRow(
                networkGrid,
                8,
                "Usage Type",
                result.getUsageType()
        );

        addRow(
                networkGrid,
                9,
                "Associated Hostnames",
                result.getAbuseHostnames()
        );

        VBox networkPanel =
                createPanel(
                        networkTitle,
                        networkGrid
                );

        // =====================================================
        // SECURITY / REPUTATION
        // =====================================================

        Label securityTitle =
                sectionTitle(
                        "SECURITY / REPUTATION"
                );

        GridPane securityGrid =
                createGrid();

        addRow(
                securityGrid,
                0,
                "Abuse Confidence",
                result.getAbuseConfidenceScore()
        );

        addRow(
                securityGrid,
                1,
                "Total Reports",
                result.getTotalReports()
        );

        addRow(
                securityGrid,
                2,
                "Distinct Reporters",
                result.getDistinctReporters()
        );

        addRow(
                securityGrid,
                3,
                "Tor",
                result.getTor()
        );

        addRow(
                securityGrid,
                4,
                "Whitelisted",
                result.getWhitelisted()
        );

        addRow(
                securityGrid,
                5,
                "Last Reported",
                result.getLastReportedAt()
        );

        addRow(
                securityGrid,
                6,
                "Security Status",
                result.getAbuseStatus()
        );

        VBox securityPanel =
                createPanel(
                        securityTitle,
                        securityGrid
                );

        // =====================================================
        // MAP BUTTON
        // =====================================================

        Button mapButton =
                new Button(
                        "📍  Open on Map"
                );

        mapButton.setPrefHeight(
                40
        );

        mapButton.setStyle(
                "-fx-background-color: "
                        + SURFACE
                        + ";"
                        + "-fx-text-fill: "
                        + TEXT
                        + ";"
                        + "-fx-border-color: "
                        + BORDER
                        + ";"
                        + "-fx-border-radius: 6px;"
                        + "-fx-background-radius: 6px;"
                        + "-fx-padding: 0 18;"
                        + "-fx-cursor: hand;"
        );

        mapButton.setOnAction(
                e -> openMap(result)
        );

        HBox mapBox =
                new HBox(
                        mapButton
                );

        mapBox.setAlignment(
                Pos.CENTER_LEFT
        );

        // =====================================================
        // ADD RESULTS
        // =====================================================

        resultsContainer
                .getChildren()
                .addAll(
                        locationPanel,
                        networkPanel,
                        securityPanel,
                        mapBox
                );
    }

    // =========================================================
    // SAVE SCAN
    // =========================================================

    private void saveScan(
            IPResult result
    ) {

        int findings =
                countFindings(
                        result
                );

        scanService.saveScan(

                "IP Address",

                result.getIp(),

                findings,

                LocalTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "HH:mm"
                                )
                        )
        );
    }

    // =========================================================
    // COUNT FINDINGS
    // =========================================================

    private int countFindings(
            IPResult result
    ) {

        int count = 0;

        // -----------------------------------------------------
        // LOCATION
        // -----------------------------------------------------

        if (!isUnknown(
                result.getCity()
        ))
            count++;

        if (!isUnknown(
                result.getRegion()
        ))
            count++;

        if (!isUnknown(
                result.getPostalCode()
        ))
            count++;

        if (!isUnknown(
                result.getCountry()
        ))
            count++;

        if (!isUnknown(
                result.getCoordinates()
        ))
            count++;

        if (!isUnknown(
                result.getTimezone()
        ))
            count++;

        // -----------------------------------------------------
        // NETWORK
        // -----------------------------------------------------

        if (!isUnknown(
                result.getIp()
        ))
            count++;

        if (!isUnknown(
                result.getIpVersion()
        ))
            count++;

        if (!isUnknown(
                result.getPublicIp()
        ))
            count++;

        if (!isUnknown(
                result.getHostname()
        ))
            count++;

        if (!isUnknown(
                result.getProvider()
        ))
            count++;

        if (!isUnknown(
                result.getOrganization()
        ))
            count++;

        if (!isUnknown(
                result.getAsn()
        ))
            count++;

        if (!isUnknown(
                result.getAbuseDomain()
        ))
            count++;

        if (!isUnknown(
                result.getUsageType()
        ))
            count++;

        if (!isUnknown(
                result.getAbuseHostnames()
        ))
            count++;

        // -----------------------------------------------------
        // SECURITY
        // -----------------------------------------------------

        if (!isUnknown(
                result.getAbuseConfidenceScore()
        ))
            count++;

        if (!isUnknown(
                result.getTotalReports()
        ))
            count++;

        if (!isUnknown(
                result.getDistinctReporters()
        ))
            count++;

        if (!isUnknown(
                result.getTor()
        ))
            count++;

        if (!isUnknown(
                result.getWhitelisted()
        ))
            count++;

        if (!isUnknown(
                result.getLastReportedAt()
        ))
            count++;

        return count;
    }

    // =========================================================
    // UNKNOWN CHECK
    // =========================================================

    private boolean isUnknown(
            String value
    ) {

        return value == null
                || value.isBlank()
                || value.equalsIgnoreCase(
                        "Unknown / Unknown"
                )
                || value.equalsIgnoreCase(
                        "Unknown"
                )
                || value.equalsIgnoreCase(
                        "Not available"
                )
                || value.equalsIgnoreCase(
                        "Not checked"
                )
                || value.equalsIgnoreCase(
                        "None found"
                );
    }

    // =========================================================
    // SAFE TEXT
    // =========================================================

    private String safe(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return "Unknown";
        }

        return value;
    }

    // =========================================================
    // OPEN MAP
    // =========================================================

    private void openMap(
            IPResult result
    ) {

        try {

            String latitude =
                    result.getLatitude();

            String longitude =
                    result.getLongitude();

            if (isUnknown(latitude)
                    || isUnknown(longitude)) {

                statusLabel.setText(
                        "Coordinates are not available."
                );

                statusLabel.setStyle(
                        "-fx-text-fill: "
                                + ERROR
                                + ";"
                );

                return;
            }

            String mapURL =
                    "https://www.openstreetmap.org/"
                            + "?mlat="
                            + latitude
                            + "&mlon="
                            + longitude
                            + "#map=12/"
                            + latitude
                            + "/"
                            + longitude;

            if (Desktop.isDesktopSupported()) {

                Desktop.getDesktop()
                        .browse(
                                new URI(
                                        mapURL
                                )
                        );

            } else {

                statusLabel.setText(
                        "Unable to open browser."
                );

                statusLabel.setStyle(
                        "-fx-text-fill: "
                                + ERROR
                                + ";"
                );
            }

        } catch (Exception ex) {

            statusLabel.setText(
                    "Could not open map: "
                            + ex.getMessage()
            );

            statusLabel.setStyle(
                    "-fx-text-fill: "
                            + ERROR
                            + ";"
            );
        }
    }

    // =========================================================
    // CREATE GRID
    // =========================================================

    private GridPane createGrid() {

        GridPane grid =
                new GridPane();

        grid.setHgap(
                30
        );

        grid.setVgap(
                12
        );

        return grid;
    }

    // =========================================================
    // ADD ROW
    // =========================================================

    private void addRow(
            GridPane grid,
            int row,
            String key,
            String value
    ) {

        Label keyLabel =
                new Label(
                        key
                );

        keyLabel.setMinWidth(
                180
        );

        keyLabel.setStyle(
                "-fx-text-fill: "
                        + MUTED
                        + ";"
                        + "-fx-font-size: 12px;"
        );

        Label valueLabel =
                new Label(
                        safe(value)
                );

        valueLabel.setWrapText(
                true
        );

        valueLabel.setStyle(
                "-fx-text-fill: "
                        + TEXT
                        + ";"
                        + "-fx-font-size: 12px;"
        );

        grid.add(
                keyLabel,
                0,
                row
        );

        grid.add(
                valueLabel,
                1,
                row
        );
    }

    // =========================================================
    // SECTION TITLE
    // =========================================================

    private Label sectionTitle(
            String text
    ) {

        Label label =
                new Label(
                        text
                );

        label.setStyle(
                "-fx-text-fill: "
                        + TEXT
                        + ";"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
        );

        return label;
    }

    // =========================================================
    // PANEL
    // =========================================================

    private VBox createPanel(
            javafx.scene.Node... nodes
    ) {

        VBox panel =
                new VBox(
                        14,
                        nodes
                );

        panel.setPadding(
                new Insets(
                        18
                )
        );

        panel.getStyleClass().add("cyber-card");

        return panel;
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {

        scanner.shutdown();
    }
}