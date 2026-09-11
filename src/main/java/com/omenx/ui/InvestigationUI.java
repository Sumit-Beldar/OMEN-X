package com.omenx.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// =========================================================
// INVESTIGATION UI
// Tactical multi-source investigation workspace
// =========================================================

public class InvestigationUI {

    private static final String BG      = "#090C12";
    private static final String PANEL   = "#111520";
    private static final String SURFACE = "#141824";
    private static final String BORDER  = "#1C2234";
    private static final String TEXT    = "#F1F5F9";
    private static final String MUTED   = "#788698";

    private final Runnable backAction;

    public InvestigationUI(Runnable backAction) {
        this.backAction = backAction;
    }

    public BorderPane createView() {

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        AppHeader header = new AppHeader(
                "//>",
                "INVESTIGATION WORKSPACE",
                "Combine OSINT modules into a single tactical investigation case",
                backAction
        );
        root.setTop(header);

        Label statusTitle = new Label("CASE STATUS");
        statusTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label status = new Label("No active multi-module investigation in progress.");
        status.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        VBox statusBox = createPanel(statusTitle, status);

        Label modulesTitle = new Label("AVAILABLE INTELLIGENCE SOURCES");
        modulesTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        VBox username = createModule("@>  Username Recognition", "Public profiles, handles & social accounts");
        VBox email = createModule("#>  Email Exposure", "Breach records, domain validity & deliverability");
        VBox domain = createModule("::>  Domain & DNS", "WHOIS, DNS records & subdomains");
        VBox image = createModule("<>  EXIF & Visual Forensics", "Metadata, geolocation & reverse image matching");

        VBox modules = new VBox(
                10,
                modulesTitle,
                username,
                email,
                domain,
                image
        );

        VBox content = new VBox(
                18,
                statusBox,
                modules
        );

        content.setPadding(new Insets(20, 28, 28, 28));
        root.setCenter(content);

        return root;
    }

    private VBox createModule(String title, String description) {
        Label moduleTitle = new Label(title);
        moduleTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label moduleDescription = new Label(description);
        moduleDescription.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");

        VBox card = new VBox(5, moduleTitle, moduleDescription);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("cyber-card");
        return card;
    }

    private VBox createPanel(javafx.scene.Node... nodes) {
        VBox panel = new VBox(8, nodes);
        panel.setPadding(new Insets(16));
        panel.getStyleClass().add("cyber-card");
        return panel;
    }
}