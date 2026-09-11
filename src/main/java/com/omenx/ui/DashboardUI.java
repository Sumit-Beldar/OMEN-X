package com.omenx.ui;

import com.omenx.model.ScanRecord;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

// =========================================================
// OMEN-X DASHBOARD UI
// Futuristic OSINT dashboard with Crimson Tactical theme integration
// =========================================================

public class DashboardUI {

    // =========================================================
    // COLOR PALETTE (Tactical Crimson Unified)
    // =========================================================

    private static final String BG          = "#090C12";
    private static final String TEXT        = "#F1F5F9";
    private static final String MUTED       = "#788698";

    private static final String GREEN       = "#22C55E";
    private static final String RED         = "#DC2626";
    private static final String YELLOW      = "#EAB308";

    // =========================================================
    // CALLBACKS & COMPONENTS
    // =========================================================

    private final ModuleAction moduleAction;

    private Label totalScansLabel;
    private Label findingsLabel;
    private VBox recentActivityBox;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardUI(ModuleAction moduleAction) {
        this.moduleAction = moduleAction;
    }

    // =========================================================
    // CREATE DASHBOARD
    // =========================================================

    public VBox createDashboard(List<ScanRecord> history) {

        // =====================================================
        // TOP HEADER HERO (Using Standardized AppHeader)
        // =====================================================

        AppHeader header = new AppHeader(
                "//>",
                "INTELLIGENCE DASHBOARD",
                "Real-time reconnaissance, threat intelligence & EXIF forensics",
                null,
                false // Hide back button on dashboard
        );
        header.setStatus(AppHeader.StatusType.ONLINE, "SYSTEMS ONLINE");

        Label activeModulesBadge = new Label("8 MODULES");
        activeModulesBadge.getStyleClass().addAll("badge", "badge-red");
        activeModulesBadge.setMinWidth(Region.USE_PREF_SIZE);
        header.addExtraPill(activeModulesBadge);

        // =====================================================
        // METRICS SECTION
        // =====================================================

        totalScansLabel = new Label(
                String.valueOf(history.size())
        );

        findingsLabel = new Label(
                String.valueOf(
                        history.stream()
                                .mapToInt(ScanRecord::getFound)
                                .sum()
                )
        );

        Label modulesLabel = new Label("8");
        Label statusLabel = new Label("READY");

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(14);
        metricsGrid.setVgap(14);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            metricsGrid.getColumnConstraints().add(col);
        }

        metricsGrid.add(
                createMetricCard(
                        "TOTAL SCANS",
                        totalScansLabel,
                        "Investigations executed",
                        RED,
                        "metric-accent-red"
                ),
                0, 0
        );

        metricsGrid.add(
                createMetricCard(
                        "FINDINGS",
                        findingsLabel,
                        "Matches & hits identified",
                        GREEN,
                        "metric-accent-green"
                ),
                1, 0
        );

        metricsGrid.add(
                createMetricCard(
                        "ACTIVE MODULES",
                        modulesLabel,
                        "Intel sources online",
                        RED,
                        "metric-accent-red"
                ),
                2, 0
        );

        metricsGrid.add(
                createMetricCard(
                        "ENGINE STATUS",
                        statusLabel,
                        "High performance operational",
                        YELLOW,
                        "metric-accent-yellow"
                ),
                3, 0
        );

        // =====================================================
        // INTELLIGENCE MODULES GRID
        // =====================================================

        Label modulesTitle = new Label("INTELLIGENCE MODULES");
        modulesTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1.5px;"
        );

        Label modulesSub = new Label("Select an active intelligence module to launch a dedicated workspace");
        modulesSub.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        VBox modulesHeading = new VBox(4, modulesTitle, modulesSub);

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(14);
        moduleGrid.setVgap(14);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            moduleGrid.getColumnConstraints().add(col);
        }

        moduleGrid.add(
                createModuleCard(
                        "@>  Username Recognition",
                        "Search 500+ public platforms & social profiles for identity traces.",
                        "OSINT",
                        "badge-green",
                        () -> moduleAction.open("Username")
                ),
                0, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "#>  Email Exposure",
                        "Check breach records, domain validity & email account deliverability.",
                        "BREACH",
                        "badge-red",
                        () -> moduleAction.open("Email")
                ),
                1, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "::>  Domain & DNS",
                        "Analyze WHOIS records, DNS routing, mail exchange & active subdomains.",
                        "INFRA",
                        "badge-red",
                        () -> moduleAction.open("Domain")
                ),
                2, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "[]>  IP Geolocation",
                        "Geolocate IP, resolve ASN, carrier ISP & query threat reputation.",
                        "NETWORK",
                        "badge-yellow",
                        () -> moduleAction.open("IP Address")
                ),
                3, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "{}>  Phone Intel",
                        "Carrier info, validity, geographic area & telecom metadata.",
                        "TELECOM",
                        "badge-yellow",
                        () -> moduleAction.open("Phone")
                ),
                0, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "<>  EXIF Metadata",
                        "Extract camera models, timestamps, software & GPS coordinates.",
                        "EXIF",
                        "badge-green",
                        moduleAction::openImageMetadata
                ),
                1, 1
        );

        moduleGrid.add(
                createModuleCard(
                        ">>  Reverse Image",
                        "Identify identical and visually matching imagery across search engines.",
                        "VISUAL",
                        "badge-green",
                        moduleAction::openReverseImage
                ),
                2, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "!!>  Malware Analysis",
                        "Scan file hashes with multi-engine static & VirusTotal threat analysis.",
                        "THREAT",
                        "badge-red",
                        () -> moduleAction.open("Malware Analysis")
                ),
                3, 1
        );

        VBox modulesSection = new VBox(
                16,
                modulesHeading,
                moduleGrid
        );

        modulesSection.setPadding(new Insets(20));
        modulesSection.getStyleClass().add("cyber-card");

        // =====================================================
        // RECENT ACTIVITY FEED
        // =====================================================

        Label recentTitle = new Label("RECENT INVESTIGATION LOGS");
        recentTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1.5px;"
        );

        recentActivityBox = new VBox(8);
        refreshRecentActivity(history);

        VBox recentSection = new VBox(
                12,
                recentTitle,
                recentActivityBox
        );

        recentSection.setPadding(new Insets(20));
        recentSection.getStyleClass().add("cyber-card");

        // =====================================================
        // FOOTER NOTICE
        // =====================================================

        Label notice = new Label(
                "OMEN-X platform queries open-source & public intelligence databases only. Operate within legal boundaries."
        );
        notice.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 11px;"
        );
        notice.setAlignment(Pos.CENTER);
        notice.setMaxWidth(Double.MAX_VALUE);

        // =====================================================
        // CONTAINER LAYOUT
        // =====================================================

        VBox content = new VBox(
                20,
                header,
                metricsGrid,
                modulesSection,
                recentSection,
                notice
        );

        content.setPadding(new Insets(24, 28, 24, 28));
        content.setStyle("-fx-background-color: " + BG + ";");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return wrapper;
    }

    // =========================================================
    // REFRESH DASHBOARD METRICS
    // =========================================================

    public void refreshDashboard(List<ScanRecord> history) {
        if (history == null) return;

        if (totalScansLabel != null) {
            totalScansLabel.setText(String.valueOf(history.size()));
        }

        if (findingsLabel != null) {
            findingsLabel.setText(
                    String.valueOf(
                            history.stream()
                                    .mapToInt(ScanRecord::getFound)
                                    .sum()
                    )
            );
        }

        refreshRecentActivity(history);
    }

    // =========================================================
    // RECENT ACTIVITY FEED
    // =========================================================

    public void refreshRecentActivity(List<ScanRecord> history) {
        if (recentActivityBox == null) return;

        recentActivityBox.getChildren().clear();

        if (history == null || history.isEmpty()) {
            Label empty = new Label("No investigations run yet. Select an intelligence module above to begin.");
            empty.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
            empty.setPadding(new Insets(10));
            recentActivityBox.getChildren().add(empty);
            return;
        }

        int limit = Math.min(6, history.size());
        for (int i = history.size() - 1; i >= history.size() - limit; i--) {
            recentActivityBox.getChildren().add(
                    createRecentRow(history.get(i))
            );
        }
    }

    // =========================================================
    // RECENT ACTIVITY ROW
    // =========================================================

    private HBox createRecentRow(ScanRecord record) {
        Label type = new Label(record.getType());
        type.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        type.setMinWidth(130);

        Label target = new Label(record.getTarget());
        target.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        HBox.setHgrow(target, Priority.ALWAYS);

        String scanType = record.getType();
        int findings = record.getFound();

        // Choose a meaningful label based on scan type and finding count
        String badgeText;
        String badgeStyle;
        if (findings > 0) {
            if ("Email".equals(scanType)) {
                badgeText = "BREACH";
            } else if ("Malware Analysis".equals(scanType)) {
                badgeText = "MALICIOUS";
            } else if ("IP Address".equals(scanType)) {
                badgeText = "AT RISK";
            } else {
                badgeText = findings + " MATCHES";
            }
            badgeStyle = "badge-red";
        } else {
            badgeText = "CLEAN";
            badgeStyle = "badge-green";
        }

        Label result = new Label(badgeText);
        result.getStyleClass().add("badge");
        result.getStyleClass().add(badgeStyle);
        result.setMinWidth(Region.USE_PREF_SIZE);

        Label time = new Label(record.getTime());
        time.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        time.setMinWidth(80);
        time.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(
                16,
                type,
                target,
                result,
                time
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("activity-row");
        return row;
    }

    // =========================================================
    // METRIC CARD BUILDER
    // =========================================================

    private VBox createMetricCard(
            String title,
            Label value,
            String description,
            String accentColor,
            String accentCssClass
    ) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        value.setStyle(
                "-fx-text-fill: " + accentColor + ";" +
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox card = new VBox(
                6,
                titleLabel,
                value,
                desc
        );

        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(105);
        card.setPadding(new Insets(16));
        card.getStyleClass().addAll("cyber-card", accentCssClass);

        return card;
    }

    // =========================================================
    // MODULE CARD BUILDER (Unclippable, Responsive)
    // =========================================================

    private VBox createModuleCard(
            String title,
            String description,
            String categoryTag,
            String badgeStyleClass,
            Runnable action
    ) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        titleLabel.setWrapText(false);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label tag = new Label(categoryTag);
        tag.getStyleClass().addAll("badge", badgeStyleClass);
        tag.setMinWidth(Region.USE_PREF_SIZE); // Never clipped!

        HBox cardHeader = new HBox(8, titleLabel, tag);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-line-spacing: 2px;");
        desc.setWrapText(true);
        desc.setMinHeight(36);
        VBox.setVgrow(desc, Priority.ALWAYS);

        Button open = new Button("LAUNCH WORKSPACE  →");
        open.getStyleClass().add("btn-accent");
        open.setMaxWidth(Double.MAX_VALUE);
        open.setOnAction(e -> action.run());

        VBox card = new VBox(
                12,
                cardHeader,
                desc,
                open
        );

        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(165);
        card.setPrefHeight(170);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("cyber-card-interactive");

        card.setOnMouseClicked(e -> action.run());
        return card;
    }

    // =========================================================
    // CALLBACK INTERFACE
    // =========================================================

    public interface ModuleAction {
        void open(String type);
        void openImageMetadata();
        void openReverseImage();
    }
}