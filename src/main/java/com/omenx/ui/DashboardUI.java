package com.omenx.ui;

import com.omenx.model.ScanRecord;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

// =========================================================
// OMEN-X DASHBOARD UI
// Futuristic OSINT dashboard with Cyber-Dark theme integration
// =========================================================

public class DashboardUI {

    // =========================================================
    // COLOR PALETTE
    // =========================================================

    private static final String BG          = "#080C14";
    private static final String PANEL       = "#0E1624";
    private static final String BORDER      = "#1B283A";
    private static final String TEXT        = "#EAF2F8";
    private static final String MUTED       = "#64748B";

    private static final String GREEN       = "#10B981";
    private static final String BLUE        = "#00F2FE";
    private static final String PURPLE      = "#8B5CF6";
    private static final String YELLOW      = "#F59E0B";
    private static final String RED         = "#EF4444";
    private static final String CYAN        = "#00D2FF";

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
        // TOP HEADER HERO
        // =====================================================

        Label logo = new Label("INTELLIGENCE DASHBOARD");
        logo.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        Label tagline = new Label(
                "Real-time reconnaissance, threat analysis & EXIF forensics"
        );
        tagline.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        VBox branding = new VBox(
                4,
                logo,
                tagline
        );

        Label systemStatus = new Label("● ONLINE");
        systemStatus.getStyleClass().addAll("badge", "badge-green");

        Label activeModulesBadge = new Label("8 MODULES");
        activeModulesBadge.getStyleClass().addAll("badge", "badge-cyan");

        HBox statusPills = new HBox(8, activeModulesBadge, systemStatus);
        statusPills.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(
                branding,
                statusPills
        );

        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(branding, Priority.ALWAYS);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.getStyleClass().add("cyber-card");

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

        HBox metrics = new HBox(
                14,
                createMetricCard(
                        "TOTAL SCANS",
                        totalScansLabel,
                        "Investigations executed",
                        BLUE,
                        "metric-accent-cyan"
                ),
                createMetricCard(
                        "FINDINGS",
                        findingsLabel,
                        "Matches & hits identified",
                        GREEN,
                        "metric-accent-green"
                ),
                createMetricCard(
                        "ACTIVE MODULES",
                        modulesLabel,
                        "Intel sources online",
                        PURPLE,
                        "metric-accent-purple"
                ),
                createMetricCard(
                        "ENGINE STATUS",
                        statusLabel,
                        "High performance",
                        YELLOW,
                        "metric-accent-yellow"
                )
        );

        // =====================================================
        // INTELLIGENCE MODULES GRID
        // =====================================================

        Label modulesTitle = new Label("INTELLIGENCE MODULES");
        modulesTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        Label modulesSub = new Label("Select a module to launch an investigation workspace");
        modulesSub.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        VBox modulesHeading = new VBox(4, modulesTitle, modulesSub);

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(14);
        moduleGrid.setVgap(14);

        moduleGrid.add(
                createModuleCard(
                        "◉ Username Recognition",
                        "Search 500+ public platforms & social profiles",
                        "OSINT",
                        "badge-green",
                        () -> moduleAction.open("Username")
                ),
                0, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "✉ Email Exposure",
                        "Check breach records, domain & deliverability",
                        "BREACH",
                        "badge-cyan",
                        () -> moduleAction.open("Email")
                ),
                1, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "🌐 Domain & DNS",
                        "Analyze WHOIS, DNS records & subdomains",
                        "INFRA",
                        "badge-purple",
                        () -> moduleAction.open("Domain")
                ),
                2, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "⊕ IP Geolocation",
                        "Geolocate IP, ASN, ISP & threat reputation",
                        "NETWORK",
                        "badge-yellow",
                        () -> moduleAction.open("IP Address")
                ),
                0, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "📱 Phone Intel",
                        "Carrier info, validity & geographic metadata",
                        "TELECOM",
                        "badge-cyan",
                        () -> moduleAction.open("Phone")
                ),
                1, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "📷 EXIF Metadata",
                        "Extract camera settings, dates & GPS location",
                        "EXIF",
                        "badge-purple",
                        moduleAction::openImageMetadata
                ),
                2, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "🔍 Reverse Image",
                        "Identify identical & visually matching images",
                        "VISUAL",
                        "badge-green",
                        moduleAction::openReverseImage
                ),
                0, 2
        );

        moduleGrid.add(
                createModuleCard(
                        "☣ Malware Analysis",
                        "Scan file hashes with VirusTotal threat engine",
                        "THREAT",
                        "badge-red",
                        () -> moduleAction.open("Malware Analysis")
                ),
                1, 2
        );

        VBox modulesSection = new VBox(
                14,
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
                "-fx-letter-spacing: 1px;"
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
                "-fx-font-size: 10px;"
        );
        notice.setAlignment(Pos.CENTER);

        // =====================================================
        // CONTAINER LAYOUT
        // =====================================================

        VBox content = new VBox(
                20,
                header,
                metrics,
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
            Label empty = new Label("No investigations run yet. Select a module above to get started.");
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
        type.setMinWidth(110);

        Label target = new Label(record.getTarget());
        target.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        HBox.setHgrow(target, Priority.ALWAYS);

        Label result = new Label(record.getFound() > 0 ? record.getFound() + " MATCHES" : "CLEAN");
        result.getStyleClass().add("badge");
        result.getStyleClass().add(record.getFound() > 0 ? "badge-green" : "badge-cyan");

        Label time = new Label(record.getTime());
        time.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");
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
        desc.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");

        VBox card = new VBox(
                4,
                titleLabel,
                value,
                desc
        );

        card.setPrefWidth(210);
        card.setPrefHeight(100);
        card.setPadding(new Insets(16));
        card.getStyleClass().addAll("cyber-card", accentCssClass);

        return card;
    }

    // =========================================================
    // MODULE CARD BUILDER
    // =========================================================

    private VBox createModuleCard(
            String title,
            String description,
            String categoryTag,
            String badgeStyleClass,
            Runnable action
    ) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label tag = new Label(categoryTag);
        tag.getStyleClass().addAll("badge", badgeStyleClass);

        HBox cardHeader = new HBox(titleLabel, tag);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        desc.setWrapText(true);

        Button open = new Button("LAUNCH WORKSPACE  →");
        open.getStyleClass().add("btn-accent");
        open.setOnAction(e -> action.run());

        VBox card = new VBox(
                10,
                cardHeader,
                desc,
                open
        );

        card.setPrefWidth(220);
        card.setPrefHeight(130);
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