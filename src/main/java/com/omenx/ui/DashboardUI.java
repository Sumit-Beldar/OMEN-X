package com.omenx.ui;

import com.omenx.model.ScanRecord;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

// =========================================================
// OMEN-X DASHBOARD UI
// Futuristic OSINT dashboard
// =========================================================

public class DashboardUI {

    // =========================================================
    // COLORS
    // =========================================================

    private static final String BG          = "#070B10";
    private static final String PANEL       = "#0D141B";
    private static final String SURFACE     = "#111A22";
    private static final String BORDER      = "#24313C";
    private static final String TEXT        = "#EAF2F8";
    private static final String MUTED       = "#71808D";

    private static final String GREEN       = "#3DDC97";
    private static final String BLUE        = "#55C7FF";
    private static final String PURPLE      = "#A78BFA";
    private static final String YELLOW      = "#F0C75E";
    private static final String CYAN        = "#56D4E8";
    private static final String ORANGE      = "#FF8A65";
    private static final String VIOLET      = "#8B5CF6";

    // =========================================================
    // CALLBACKS
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
        // TOP HEADER
        // =====================================================

        Label logo = new Label("OMEN-X");

        logo.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 34px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 3px;"
        );

        Label tagline = new Label(
                "OPEN SOURCE INTELLIGENCE PLATFORM"
        );

        tagline.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 11px;" +
                "-fx-letter-spacing: 2px;"
        );

        VBox branding = new VBox(
                4,
                logo,
                tagline
        );

        Label systemStatus = new Label(
                "●  ALL SYSTEMS OPERATIONAL"
        );

        systemStatus.setStyle(
                "-fx-text-fill: " + GREEN + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        HBox header = new HBox(
                branding,
                systemStatus
        );

        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(branding, Priority.ALWAYS);

        header.setPadding(
                new Insets(18, 22, 18, 22)
        );

        header.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // =====================================================
        // METRICS
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

        Label modulesLabel = new Label("7");
        Label statusLabel = new Label("ONLINE");

        HBox metrics = new HBox(
                12,
                createMetricCard(
                        "TOTAL SCANS",
                        totalScansLabel,
                        "Investigations run",
                        BLUE
                ),
                createMetricCard(
                        "FINDINGS",
                        findingsLabel,
                        "Potential matches",
                        GREEN
                ),
                createMetricCard(
                        "MODULES",
                        modulesLabel,
                        "Intelligence sources",
                        PURPLE
                ),
                createMetricCard(
                        "STATUS",
                        statusLabel,
                        "Engine state",
                        YELLOW
                )
        );

        // =====================================================
        // INTELLIGENCE MODULES
        // =====================================================

        Label modulesTitle = new Label(
                "INTELLIGENCE MODULES"
        );

        modulesTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        Label modulesSub = new Label(
                "Select a module to begin an investigation"
        );

        modulesSub.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 12px;"
        );

        VBox modulesHeading = new VBox(
                4,
                modulesTitle,
                modulesSub
        );

        // =====================================================
        // MODULE GRID
        // =====================================================

        GridPane moduleGrid = new GridPane();

        moduleGrid.setHgap(14);
        moduleGrid.setVgap(14);

        moduleGrid.add(
                createModuleCard(
                        "Username",
                        "Public profiles & social accounts",
                        GREEN,
                        () -> moduleAction.open("Username")
                ),
                0, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "Email",
                        "Breach data & exposure checks",
                        BLUE,
                        () -> moduleAction.open("Email")
                ),
                1, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "Domain",
                        "WHOIS, DNS & infrastructure",
                        PURPLE,
                        () -> moduleAction.open("Domain")
                ),
                2, 0
        );

        moduleGrid.add(
                createModuleCard(
                        "IP Address",
                        "Geolocation & reputation",
                        YELLOW,
                        () -> moduleAction.open("IP Address")
                ),
                0, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "Phone",
                        "Carrier & public records",
                        CYAN,
                        () -> moduleAction.open("Phone")
                ),
                1, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "Image Metadata",
                        "EXIF data & GPS location",
                        ORANGE,
                        moduleAction::openImageMetadata
                ),
                2, 1
        );

        moduleGrid.add(
                createModuleCard(
                        "Reverse Image",
                        "Find image source & location",
                        VIOLET,
                        moduleAction::openReverseImage
                ),
                0, 2
        );

        // =====================================================
        // MODULE SECTION
        // =====================================================

        VBox modulesSection = new VBox(
                12,
                modulesHeading,
                moduleGrid
        );

        modulesSection.setPadding(
                new Insets(20)
        );

        modulesSection.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // =====================================================
        // RECENT ACTIVITY
        // =====================================================

        Label recentTitle = new Label(
                "RECENT ACTIVITY"
        );

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

        recentSection.setPadding(
                new Insets(20)
        );

        recentSection.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // =====================================================
        // FOOTER
        // =====================================================

        Label notice = new Label(
                "OMEN-X uses publicly available information only. " +
                "Always operate within legal and ethical boundaries."
        );

        notice.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        notice.setAlignment(Pos.CENTER);

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        VBox content = new VBox(
                18,
                header,
                metrics,
                modulesSection,
                recentSection,
                notice
        );

        content.setPadding(
                new Insets(28, 32, 28, 32)
        );

        content.setStyle(
                "-fx-background-color: " + BG + ";"
        );

        // =====================================================
        // SCROLL CONTAINER
        // =====================================================

        ScrollPane scroll = new ScrollPane(
                content
        );

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";"
        );

        VBox wrapper = new VBox(
                scroll
        );

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );

        return wrapper;
    }

    // =========================================================
    // RECENT ACTIVITY
    // =========================================================

    public void refreshRecentActivity(
            List<ScanRecord> history
    ) {

        if (recentActivityBox == null) {
            return;
        }

        recentActivityBox
                .getChildren()
                .clear();

        if (history.isEmpty()) {

            Label empty = new Label(
                    "No investigations yet. Select a module to get started."
            );

            empty.setStyle(
                    "-fx-text-fill: " + MUTED + ";" +
                    "-fx-font-size: 12px;"
            );

            empty.setPadding(
                    new Insets(10)
            );

            recentActivityBox
                    .getChildren()
                    .add(empty);

            return;
        }

        int limit = Math.min(
                6,
                history.size()
        );

        for (
                int i = history.size() - 1;
                i >= history.size() - limit;
                i--
        ) {

            recentActivityBox
                    .getChildren()
                    .add(
                            createRecentRow(
                                    history.get(i)
                            )
                    );
        }
    }

    // =========================================================
    // RECENT ACTIVITY ROW
    // =========================================================

    private HBox createRecentRow(
            ScanRecord record
    ) {

        Label type = new Label(
                record.getType()
        );

        type.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        type.setMinWidth(100);

        Label target = new Label(
                record.getTarget()
        );

        target.setStyle(
                "-fx-text-fill: #9AA8B5;" +
                "-fx-font-size: 12px;"
        );

        HBox.setHgrow(
                target,
                Priority.ALWAYS
        );

        Label result = new Label(
                record.getFound() + " found"
        );

        result.setStyle(
                "-fx-text-fill: " +
                (
                        record.getFound() > 0
                                ? GREEN
                                : MUTED
                ) +
                ";" +
                "-fx-font-size: 11px;"
        );

        Label time = new Label(
                record.getTime()
        );

        time.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        time.setMinWidth(70);

        time.setAlignment(
                Pos.CENTER_RIGHT
        );

        HBox row = new HBox(
                16,
                type,
                target,
                result,
                time
        );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        row.setPadding(
                new Insets(11, 14, 11, 14)
        );

        row.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;"
        );

        return row;
    }

    // =========================================================
    // METRIC CARD
    // =========================================================

    private VBox createMetricCard(
            String title,
            Label value,
            String description,
            String accent
    ) {

        Label titleLabel = new Label(
                title
        );

        titleLabel.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1px;"
        );

        value.setStyle(
                "-fx-text-fill: " + accent + ";" +
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(
                description
        );

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 10px;"
        );

        VBox card = new VBox(
                5,
                titleLabel,
                value,
                desc
        );

        card.setPrefWidth(210);
        card.setPrefHeight(105);

        card.setPadding(
                new Insets(16)
        );

        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        return card;
    }

    // =========================================================
    // MODULE CARD
    // =========================================================

    private VBox createModuleCard(
            String title,
            String description,
            String accent,
            Runnable action
    ) {

        Label titleLabel = new Label(
                title
        );

        titleLabel.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;"
        );

        Label desc = new Label(
                description
        );

        desc.setStyle(
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-font-size: 11px;"
        );

        desc.setWrapText(true);

        Label status = new Label(
                "●  Ready"
        );

        status.setStyle(
                "-fx-text-fill: " + accent + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;"
        );

        Button open = new Button(
                "OPEN MODULE  →"
        );

        open.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: " + accent + ";" +
                "-fx-border-radius: 4px;" +
                "-fx-background-radius: 4px;" +
                "-fx-text-fill: " + accent + ";" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 10 6 10;" +
                "-fx-cursor: hand;"
        );

        open.setOnAction(
                e -> action.run()
        );

        VBox card = new VBox(
                9,
                titleLabel,
                desc,
                status,
                open
        );

        card.setPrefWidth(220);
        card.setPrefHeight(135);

        card.setPadding(
                new Insets(16)
        );

        card.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        card.setOnMouseEntered(
                e -> card.setStyle(
                        "-fx-background-color: #17222C;" +
                        "-fx-border-color: " + accent + ";" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-cursor: hand;"
                )
        );

        card.setOnMouseExited(
                e -> card.setStyle(
                        "-fx-background-color: " + SURFACE + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;"
                )
        );

        card.setOnMouseClicked(
                e -> action.run()
        );

        return card;
    }

    // =========================================================
    // MODULE ACTION INTERFACE
    // =========================================================

    public interface ModuleAction {

        void open(String type);

        void openImageMetadata();

        void openReverseImage();
    }
}