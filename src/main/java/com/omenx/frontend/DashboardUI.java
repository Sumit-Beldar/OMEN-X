package com.omenx.frontend;

import com.omenx.database.model.ScanRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// =========================================================
// OMEN-X COMMAND CENTER & INTELLIGENCE DASHBOARD
// Dark Glassmorphic Intelligence Dashboard (OSIRIS-inspired)
// Categorized Icon-Grid Tool Launcher with Real-time Filtering
// =========================================================
public class DashboardUI {

    private static final String TEXT   = "#F1F5F9";
    private static final String MUTED  = "#64748B";
    private static final String CYAN   = "#22D3EE";
    private static final String GREEN  = "#10B981";
    private static final String RED    = "#EF4444";
    private static final String AMBER  = "#F59E0B";

    private final ModuleAction moduleAction;

    private Label totalScansLabel;
    private Label findingsLabel;

    // Filter and Tool Launcher state
    private final List<ModuleCardItem> allModuleCards = new ArrayList<>();
    private final Map<String, VBox> categorySections = new LinkedHashMap<>();
    private Label filterCountLabel;

    public DashboardUI(ModuleAction moduleAction) {
        this.moduleAction = moduleAction;
    }

    // Module item data structure for categorized launcher
    private static class ModuleCardItem {
        final String id;
        final String title;
        final String category;
        final String icon;
        final String tag;
        final String description;
        final Runnable action;
        final VBox cardNode;

        ModuleCardItem(String id, String title, String category, String icon, String tag, String description, Runnable action, VBox cardNode) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.icon = icon;
            this.tag = tag;
            this.description = description;
            this.action = action;
            this.cardNode = cardNode;
        }

        boolean matches(String query) {
            if (query == null || query.isBlank()) return true;
            String q = query.toLowerCase().trim();
            return id.toLowerCase().contains(q) ||
                    title.toLowerCase().contains(q) ||
                    category.toLowerCase().contains(q) ||
                    tag.toLowerCase().contains(q) ||
                    description.toLowerCase().contains(q);
        }
    }

    // =========================================================
    // CREATE DASHBOARD
    // =========================================================
    public VBox createDashboard(List<ScanRecord> history) {
        allModuleCards.clear();
        categorySections.clear();

        // ── Standardized Header ──
        AppHeader header = new AppHeader(
                ModuleIcons.get("Dashboard"),
                "COMMAND CENTER & INTELLIGENCE DASHBOARD",
                "Unified reconnaissance, threat analysis & media forensics engine",
                null,
                false
        );
        header.setStatus(AppHeader.StatusType.ONLINE, "SYSTEMS ONLINE");

        Label activeModulesBadge = new Label("9 / 9 MODULES ONLINE");
        activeModulesBadge.getStyleClass().addAll("badge", "badge-cyan");
        activeModulesBadge.setMinWidth(Region.USE_PREF_SIZE);
        header.addExtraPill(activeModulesBadge);

        // ── Metrics Grid (Glass Cards) ──
        totalScansLabel = new Label(String.valueOf(history.size()));
        findingsLabel = new Label(
                String.valueOf(history.stream().mapToInt(ScanRecord::getFound).sum())
        );

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(14);
        metricsGrid.setVgap(14);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            metricsGrid.getColumnConstraints().add(col);
        }

        metricsGrid.add(createMetricCard("TOTAL SCANS", totalScansLabel, "Investigations logged", CYAN, "metric-accent-cyan"), 0, 0);
        metricsGrid.add(createMetricCard("FINDINGS IDENTIFIED", findingsLabel, "Threats & intelligence hits", GREEN, "metric-accent-green"), 1, 0);
        metricsGrid.add(createMetricCard("ACTIVE MODULES", new Label("9"), "Recon & forensic tools", CYAN, "metric-accent-cyan"), 2, 0);
        metricsGrid.add(createMetricCard("ENGINE STATUS", new Label("OPERATIONAL"), "Threat feeds ready", AMBER, "metric-accent-amber"), 3, 0);

        // =====================================================
        // CATEGORIZED ICON-GRID TOOL LAUNCHER PANEL
        // =====================================================
        VBox toolLauncherPanel = new VBox(16);
        toolLauncherPanel.getStyleClass().add("glass-panel");
        toolLauncherPanel.setPadding(new Insets(0, 0, 18, 0));

        // Standardized Panel Header Pattern
        HBox launcherHeader = AppHeader.createPanelHeader(
                "◈",
                "INTELLIGENCE TOOL LAUNCHER & MODULE INDEX",
                "ONLINE",
                AppHeader.StatusType.ONLINE,
                "9 TOOLS"
        );

        // Filter / Search Input Bar
        TextField filterInput = new TextField();
        filterInput.setPromptText("Filter modules by name or capability (e.g. 'osiris', 'username', 'domain', 'threat', 'exif')...");
        filterInput.getStyleClass().add("filter-input");
        HBox.setHgrow(filterInput, Priority.ALWAYS);

        filterCountLabel = new Label("SHOWING 9 OF 9 TOOLS");
        filterCountLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold;");

        Button clearFilterBtn = new Button("✕");
        clearFilterBtn.getStyleClass().add("btn-ghost");
        clearFilterBtn.setStyle("-fx-padding: 6 10 6 10; -fx-font-size: 10px;");
        clearFilterBtn.setOnAction(e -> filterInput.clear());

        HBox filterBar = new HBox(12, filterInput, clearFilterBtn, filterCountLabel);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 18, 4, 18));

        // Category Groups Container
        VBox categoriesContainer = new VBox(18);
        categoriesContainer.setPadding(new Insets(4, 18, 8, 18));

        // Define the 9 modules grouped into 6 categories
        registerTool("OSIRIS", "OSIRIS Global OSINT", "GLOBAL RECON & SURVEILLANCE", ModuleIcons.get("OSIRIS"), "3D GEOINT", "Local Next.js 3D threat, satellite & flight intelligence", moduleAction::openOsirisLauncher);
        registerTool("Username", "Username Recognition", "IDENTITY & PROFILING", ModuleIcons.get("Username"), "OSINT RECON", "Search 500+ social & public platforms", () -> moduleAction.open("Username"));
        registerTool("Phone", "Phone Intel", "TELECOM & CARRIER", ModuleIcons.get("Phone"), "TELCO METADATA", "Carrier lookup & validation", () -> moduleAction.open("Phone"));
        registerTool("Email", "Email Exposure", "THREAT & VULNERABILITY", ModuleIcons.get("Email"), "BREACH AUDIT", "Breach records & deliverability", () -> moduleAction.open("Email"));
        registerTool("Malware Analysis", "Malware Threat Analysis", "THREAT & VULNERABILITY", ModuleIcons.get("Malware Analysis"), "BINARY STATIC", "File hashes & VirusTotal threat analysis", () -> moduleAction.open("Malware Analysis"));
        registerTool("IP Address", "IP Geolocation", "NETWORK & INFRASTRUCTURE", ModuleIcons.get("IP Address"), "GEO / ASN", "Geolocate IP, resolve ASN & reputation", () -> moduleAction.open("IP Address"));
        registerTool("Domain", "Domain & DNS", "NETWORK & INFRASTRUCTURE", ModuleIcons.get("Domain"), "DNS / WHOIS", "WHOIS, DNS records & active subdomains", () -> moduleAction.open("Domain"));
        registerTool("EXIF Metadata", "EXIF Metadata", "MEDIA FORENSICS", ModuleIcons.get("EXIF Metadata"), "EXIF / GPS", "Camera signatures & GPS forensics", moduleAction::openImageMetadata);
        registerTool("Reverse Image", "Reverse Image", "MEDIA FORENSICS", ModuleIcons.get("Reverse Image"), "VISUAL SEARCH", "Multi-engine visual match indexing", moduleAction::openReverseImage);

        // Build Category sections
        Map<String, List<ModuleCardItem>> grouped = new LinkedHashMap<>();
        for (ModuleCardItem item : allModuleCards) {
            grouped.computeIfAbsent(item.category, k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<ModuleCardItem>> entry : grouped.entrySet()) {
            String catName = entry.getKey();
            List<ModuleCardItem> items = entry.getValue();

            Label catHeader = new Label("// " + catName + " //");
            catHeader.getStyleClass().add("category-label");

            GridPane catGrid = new GridPane();
            catGrid.setHgap(12);
            catGrid.setVgap(12);

            for (int i = 0; i < 4; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(25);
                col.setHgrow(Priority.ALWAYS);
                catGrid.getColumnConstraints().add(col);
            }

            int col = 0;
            int row = 0;
            for (ModuleCardItem item : items) {
                catGrid.add(item.cardNode, col, row);
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
            }

            VBox catSection = new VBox(8, catHeader, catGrid);
            categorySections.put(catName, catSection);
            categoriesContainer.getChildren().add(catSection);
        }

        // Live filtering listener
        filterInput.textProperty().addListener((obs, oldVal, newVal) -> filterModules(newVal));

        toolLauncherPanel.getChildren().addAll(launcherHeader, filterBar, categoriesContainer);

        // Footer Notice
        Label notice = new Label(
                "OMEN-X platform queries open-source & public intelligence databases only. Verify authorizations before executing scans."
        );
        notice.setStyle("-fx-text-fill: #475569; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        notice.setAlignment(Pos.CENTER);
        notice.setMaxWidth(Double.MAX_VALUE);

        // Assemble Layout
        VBox content = new VBox(
                20,
                header,
                metricsGrid,
                toolLauncherPanel,
                notice
        );

        content.setPadding(new Insets(20, 26, 24, 26));
        content.setStyle("-fx-background-color: #060810;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return wrapper;
    }

    private void registerTool(String id, String title, String category, String icon, String tag, String description, Runnable action) {
        VBox card = createToolCard(icon, title, tag, description, action);
        ModuleCardItem item = new ModuleCardItem(id, title, category, icon, tag, description, action, card);
        allModuleCards.add(item);
    }

    private void filterModules(String query) {
        int visibleCount = 0;
        Map<String, Integer> categoryVisibleCounts = new LinkedHashMap<>();

        for (ModuleCardItem item : allModuleCards) {
            boolean matches = item.matches(query);
            item.cardNode.setVisible(matches);
            item.cardNode.setManaged(matches);
            if (matches) {
                visibleCount++;
                categoryVisibleCounts.put(item.category, categoryVisibleCounts.getOrDefault(item.category, 0) + 1);
            }
        }

        // Show/hide category headers if all cards inside are hidden
        categorySections.forEach((catName, section) -> {
            boolean hasVisible = categoryVisibleCounts.getOrDefault(catName, 0) > 0;
            section.setVisible(hasVisible);
            section.setManaged(hasVisible);
        });

        if (filterCountLabel != null) {
            filterCountLabel.setText("SHOWING " + visibleCount + " OF " + allModuleCards.size() + " TOOLS");
        }
    }

    // Tool card builder (Icon centered on top, label below, subtle border, cyan glow on hover)
    private VBox createToolCard(String iconStr, String titleStr, String tagStr, String descStr, Runnable action) {
        Label icon = new Label(iconStr);
        icon.getStyleClass().add("tool-card-icon");

        Label title = new Label(titleStr.toUpperCase());
        title.getStyleClass().add("tool-card-label");
        title.setWrapText(true);

        Label tag = new Label(tagStr);
        tag.getStyleClass().add("tool-card-tag");

        Label desc = new Label(descStr);
        desc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-alignment: center;");
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setAlignment(Pos.CENTER);

        VBox card = new VBox(6, icon, title, tag, desc);
        card.getStyleClass().add("tool-card");
        card.setAlignment(Pos.CENTER);
        card.setMinHeight(120);
        card.setPrefHeight(128);
        card.setMaxWidth(Double.MAX_VALUE);

        card.setOnMouseClicked(e -> action.run());
        return card;
    }

    // Metric card builder
    private VBox createMetricCard(String title, Label value, String description, String accentColor, String accentClass) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;"
        );

        value.setStyle(
                "-fx-text-fill: " + accentColor + "; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 24px; -fx-font-weight: 800;"
        );

        Label desc = new Label(description);
        desc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");

        VBox card = new VBox(6, titleLabel, value, desc);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(100);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.getStyleClass().addAll("glass-panel", accentClass);

        return card;
    }

    public void refreshDashboard(List<ScanRecord> history) {
        if (history == null) return;
        if (totalScansLabel != null) totalScansLabel.setText(String.valueOf(history.size()));
        if (findingsLabel != null) {
            findingsLabel.setText(
                    String.valueOf(history.stream().mapToInt(ScanRecord::getFound).sum())
            );
        }
    }

    public interface ModuleAction {
        void open(String type);
        void openImageMetadata();
        void openReverseImage();
        void openOsirisLauncher();
    }
}