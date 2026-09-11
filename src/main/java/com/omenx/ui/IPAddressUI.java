package com.omenx.ui;

import com.omenx.osint.IPScanner;
import com.omenx.osint.IPScanner.IPResult;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// =========================================================
// IP ADDRESS UI
// Tactical OSINT IP Geolocation & Network Intelligence
// =========================================================

public class IPAddressUI {

    // =========================================================
    // COLORS (Tactical Crimson Unified)
    // =========================================================

    private static final String BG      = "#090C12";
    private static final String PANEL   = "#111520";
    private static final String SURFACE = "#141824";
    private static final String BORDER  = "#1C2234";
    private static final String TEXT    = "#F1F5F9";
    private static final String MUTED   = "#788698";
    private static final String RED     = "#DC2626";
    private static final String ERROR   = "#EF4444";
    private static final String SUCCESS = "#22C55E";

    // =========================================================
    // SERVICES
    // =========================================================

    private final IPScanner scanner;
    private final ScanService scanService;
    private final Runnable backAction;

    // =========================================================
    // UI
    // =========================================================

    private AppHeader header;
    private Label statusLabel;
    private VBox resultsContainer;

    private TextField ipField;
    private Button scanButton;

    // Stat strip labels (populated after each scan)
    private Label statCountry;
    private Label statIsp;
    private Label statThreat;
    private Label statType;
    private Label resultsTarget;

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
        root.setStyle("-fx-background-color: " + BG + ";");

        // ── Standardized Header ──
        header = new AppHeader(
                "[]>",
                "IP GEOLOCATION",
                "Geolocate IP, resolve ASN, carrier ISP & threat reputation",
                backAction
        );
        root.setTop(header);

        // ── IP Tag + Input Row (matches USERNAME/EMAIL/DOMAIN/PHONE modules) ──
        Label ipTag = new Label("IP");
        ipTag.setPrefHeight(44);
        ipTag.setMinWidth(80);
        ipTag.setAlignment(Pos.CENTER);
        ipTag.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px 0 0 6px;" +
                "-fx-background-radius: 6px 0 0 6px;" +
                "-fx-font-family: 'Consolas', monospace;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        ipField = new TextField();
        ipField.setPromptText("Enter IP address (e.g., 8.8.8.8)...");
        ipField.setPrefHeight(44);
        ipField.getStyleClass().add("cyber-input");
        HBox.setHgrow(ipField, Priority.ALWAYS);

        scanButton = new Button("EXECUTE SCAN");
        scanButton.setPrefHeight(44);
        scanButton.getStyleClass().add("btn-primary");
        scanButton.setOnAction(e -> scanIP());
        ipField.setOnAction(e -> scanIP());

        HBox inputBox = new HBox(0, ipTag, ipField, scanButton);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        // ── Clear / Copy action row ──
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("btn-ghost");
        clearButton.setOnAction(e -> {
            ipField.clear();
            resultsContainer.getChildren().clear();
            statCountry.setText("—");
            statIsp.setText("—");
            statThreat.setText("—");
            statType.setText("—");
            statusLabel.setText("Enter an IP address to begin investigation.");
            statusLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            header.setStatus(AppHeader.StatusType.READY, "READY");
        });

        Button copyButton = new Button("Copy Results");
        copyButton.getStyleClass().add("btn-ghost");
        copyButton.setOnAction(e -> {
            if (lastResult == null) return;
            StringBuilder sb = new StringBuilder();
            sb.append("OMEN-X • IP Geolocation scan\n");
            sb.append("Target: ").append(lastResult.getIp()).append("\n\n");
            sb.append("Country: ").append(safe(lastResult.getCountry())).append("\n");
            sb.append("ISP: ").append(safe(lastResult.getProvider())).append("\n");
            sb.append("ASN: ").append(safe(lastResult.getAsn())).append("\n");
            sb.append("Abuse Confidence: ").append(safe(lastResult.getAbuseConfidenceScore())).append("\n");
            sb.append("Usage Type: ").append(safe(lastResult.getUsageType())).append("\n");
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(sb.toString());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            statusLabel.setText("Results copied to clipboard.");
        });

        HBox actionRow = new HBox(10, clearButton, copyButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // ── Status ──
        statusLabel = new Label("Enter an IP address to begin investigation.");
        statusLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        // ── 4-Tile Stat Strip ──
        statCountry = new Label("—");
        statIsp     = new Label("—");
        statThreat  = new Label("—");
        statType    = new Label("—");

        HBox statStrip = new HBox(12,
                createStatTile("COUNTRY",      statCountry),
                createStatTile("ISP / ASN",    statIsp),
                createStatTile("THREAT LEVEL", statThreat),
                createStatTile("IP TYPE",      statType)
        );
        statStrip.setAlignment(Pos.CENTER_LEFT);

        // ── Investigation Results header ──
        Label resultsTitle = new Label("Investigation Results");
        resultsTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        resultsTarget = new Label("No target selected");
        resultsTarget.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        HBox resultsHeader = new HBox(12, resultsTitle, resultsTarget);
        resultsHeader.setAlignment(Pos.CENTER_LEFT);

        // ── Results ──
        resultsContainer = new VBox(16);

        // ── Main Content ──
        VBox content = new VBox(
                16,
                inputBox,
                actionRow,
                statusLabel,
                statStrip,
                resultsHeader,
                resultsContainer
        );

        content.setPadding(new Insets(20, 28, 28, 28));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("scroll-pane");

        root.setCenter(scrollPane);
        return root;
    }

    private VBox createStatTile(String title, Label value) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );
        value.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        value.setWrapText(true);
        VBox tile = new VBox(4, titleLabel, value);
        tile.setPadding(new Insets(12, 16, 12, 16));
        tile.setPrefWidth(170);
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.getStyleClass().add("cyber-card");
        return tile;
    }

    // =========================================================
    // SCAN IP
    // =========================================================

    private void scanIP() {
        String ip = ipField.getText().trim();

        if (ip.isEmpty()) {
            statusLabel.setText("Please enter an IP address.");
            statusLabel.setStyle("-fx-text-fill: " + ERROR + ";");
            header.setStatus(AppHeader.StatusType.WARNING, "IP REQUIRED");
            return;
        }

        scanButton.setDisable(true);
        ipField.setDisable(true);
        statusLabel.setText("Scanning IP address " + ip + "...");
        statusLabel.setStyle("-fx-text-fill: " + MUTED + ";");
        header.setStatus(AppHeader.StatusType.SCANNING, "SCANNING " + ip);

        resultsContainer.getChildren().clear();

        Thread thread = new Thread(() -> {
            try {
                IPResult result = scanner.scan(ip);
                Platform.runLater(() -> {
                    lastResult = result;
                    displayResult(result);
                    saveScan(result);
                    scanButton.setDisable(false);
                    ipField.setDisable(false);
                    header.setStatus(AppHeader.StatusType.READY, "COMPLETE");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Scan failed: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: " + ERROR + ";");
                    header.setStatus(AppHeader.StatusType.ERROR, "ERROR");
                    scanButton.setDisable(false);
                    ipField.setDisable(false);
                });
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    // =========================================================
    // DISPLAY RESULT
    // =========================================================

    private void displayResult(IPResult result) {
        statusLabel.setText("Scan completed successfully.");
        statusLabel.setStyle("-fx-text-fill: " + SUCCESS + ";");

        // Populate stat strip tiles
        String country = safe(result.getCountry());
        String cc = safe(result.getCountryCode());
        statCountry.setText(country.equals("Unknown") ? "—" : country + " (" + cc + ")");

        String isp = safe(result.getProvider());
        String asn = safe(result.getAsn());
        statIsp.setText(isp.equals("Unknown") ? "—" : isp + (asn.equals("Unknown") ? "" : " / " + asn));

        String abuseScore = safe(result.getAbuseConfidenceScore());
        if (abuseScore.equals("Unknown") || abuseScore.equals("0")) {
            statThreat.setText("LOW");
            statThreat.setStyle("-fx-text-fill: " + SUCCESS + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        } else {
            try {
                int score = Integer.parseInt(abuseScore.replaceAll("[^0-9]", ""));
                if (score >= 50) {
                    statThreat.setText("HIGH (" + score + "%)");
                    statThreat.setStyle("-fx-text-fill: " + ERROR + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                } else if (score > 0) {
                    statThreat.setText("MEDIUM (" + score + "%)");
                    statThreat.setStyle("-fx-text-fill: #EAB308; -fx-font-size: 13px; -fx-font-weight: bold;");
                } else {
                    statThreat.setText("LOW");
                    statThreat.setStyle("-fx-text-fill: " + SUCCESS + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException ex) {
                statThreat.setText(abuseScore);
            }
        }

        String usageType = safe(result.getUsageType());
        statType.setText(usageType.equals("Unknown") ? "—" : usageType);

        // Update results target label
        resultsTarget.setText(safe(result.getIp()));
        resultsTarget.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        // LOCATION
        Label locationTitle = sectionTitle("LOCATION METADATA");
        GridPane locationGrid = createGrid();
        addRow(locationGrid, 0, "City", result.getCity());
        addRow(locationGrid, 1, "Region", safe(result.getRegion()) + " (" + safe(result.getRegionCode()) + ")");
        addRow(locationGrid, 2, "Postal code", result.getPostalCode());
        addRow(locationGrid, 3, "Country", safe(result.getCountry()) + " (" + safe(result.getCountryCode()) + ")");
        addRow(locationGrid, 4, "Continent", safe(result.getContinent()) + " (" + safe(result.getContinentCode()) + ")");
        addRow(locationGrid, 5, "Coordinates", result.getCoordinates());
        addRow(locationGrid, 6, "Timezone", result.getTimezone());
        VBox locationPanel = createPanel(locationTitle, locationGrid);

        // NETWORK
        Label networkTitle = sectionTitle("NETWORK & ROUTING INTEL");
        GridPane networkGrid = createGrid();
        addRow(networkGrid, 0, "IP address", result.getIp());
        addRow(networkGrid, 1, "IP version", result.getIpVersion());
        addRow(networkGrid, 2, "Public IP", result.getPublicIp());
        addRow(networkGrid, 3, "Hostname", result.getHostname());
        addRow(networkGrid, 4, "ISP / Provider", result.getProvider());
        addRow(networkGrid, 5, "Organization", result.getOrganization());
        addRow(networkGrid, 6, "ASN", result.getAsn());
        addRow(networkGrid, 7, "Domain", result.getAbuseDomain());
        addRow(networkGrid, 8, "Usage Type", result.getUsageType());
        addRow(networkGrid, 9, "Associated Hostnames", result.getAbuseHostnames());
        VBox networkPanel = createPanel(networkTitle, networkGrid);

        // SECURITY
        Label securityTitle = sectionTitle("SECURITY & REPUTATION");
        GridPane securityGrid = createGrid();
        addRow(securityGrid, 0, "Abuse Confidence", result.getAbuseConfidenceScore());
        addRow(securityGrid, 1, "Total Reports", result.getTotalReports());
        addRow(securityGrid, 2, "Distinct Reporters", result.getDistinctReporters());
        addRow(securityGrid, 3, "Tor Exit Node", result.getTor());
        addRow(securityGrid, 4, "Whitelisted", result.getWhitelisted());
        addRow(securityGrid, 5, "Last Reported", result.getLastReportedAt());
        addRow(securityGrid, 6, "Security Status", result.getAbuseStatus());
        VBox securityPanel = createPanel(securityTitle, securityGrid);

        // MAP BUTTON
        Button mapButton = new Button("📍  Open on Interactive Map");
        mapButton.setPrefHeight(40);
        mapButton.getStyleClass().add("btn-accent");
        mapButton.setOnAction(e -> openMap(result));

        HBox mapBox = new HBox(mapButton);
        mapBox.setAlignment(Pos.CENTER_LEFT);

        resultsContainer.getChildren().addAll(
                locationPanel,
                networkPanel,
                securityPanel,
                mapBox
        );
    }

    private void saveScan(IPResult result) {
        int findings = countFindings(result);
        scanService.saveScan(
                "IP Address",
                result.getIp(),
                findings,
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    private int countFindings(IPResult result) {
        int count = 0;
        if (!isUnknown(result.getCity())) count++;
        if (!isUnknown(result.getRegion())) count++;
        if (!isUnknown(result.getPostalCode())) count++;
        if (!isUnknown(result.getCountry())) count++;
        if (!isUnknown(result.getCoordinates())) count++;
        if (!isUnknown(result.getTimezone())) count++;

        if (!isUnknown(result.getIp())) count++;
        if (!isUnknown(result.getIpVersion())) count++;
        if (!isUnknown(result.getPublicIp())) count++;
        if (!isUnknown(result.getHostname())) count++;
        if (!isUnknown(result.getProvider())) count++;
        if (!isUnknown(result.getOrganization())) count++;
        if (!isUnknown(result.getAsn())) count++;
        if (!isUnknown(result.getAbuseDomain())) count++;
        if (!isUnknown(result.getUsageType())) count++;
        if (!isUnknown(result.getAbuseHostnames())) count++;

        if (!isUnknown(result.getAbuseConfidenceScore())) count++;
        if (!isUnknown(result.getTotalReports())) count++;
        if (!isUnknown(result.getDistinctReporters())) count++;
        if (!isUnknown(result.getTor())) count++;
        if (!isUnknown(result.getWhitelisted())) count++;
        if (!isUnknown(result.getLastReportedAt())) count++;

        return count;
    }

    private boolean isUnknown(String value) {
        return value == null
                || value.isBlank()
                || value.equalsIgnoreCase("Unknown / Unknown")
                || value.equalsIgnoreCase("Unknown")
                || value.equalsIgnoreCase("Not available")
                || value.equalsIgnoreCase("Not checked")
                || value.equalsIgnoreCase("None found");
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "Unknown" : value;
    }

    private void openMap(IPResult result) {
        try {
            String latitude = result.getLatitude();
            String longitude = result.getLongitude();

            if (isUnknown(latitude) || isUnknown(longitude)) {
                statusLabel.setText("Coordinates are not available.");
                statusLabel.setStyle("-fx-text-fill: " + ERROR + ";");
                return;
            }

            String mapURL = "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + "#map=12/" + latitude + "/" + longitude;

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(mapURL));
            } else {
                statusLabel.setText("Unable to open browser.");
                statusLabel.setStyle("-fx-text-fill: " + ERROR + ";");
            }
        } catch (Exception ex) {
            statusLabel.setText("Could not open map: " + ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: " + ERROR + ";");
        }
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(12);
        return grid;
    }

    private void addRow(GridPane grid, int row, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setMinWidth(180);
        keyLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label valueLabel = new Label(safe(value));
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");

        grid.add(keyLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        return label;
    }

    private VBox createPanel(javafx.scene.Node... nodes) {
        VBox panel = new VBox(14, nodes);
        panel.setPadding(new Insets(18));
        panel.getStyleClass().add("cyber-card");
        return panel;
    }

    public void shutdown() {
        scanner.shutdown();
    }
}