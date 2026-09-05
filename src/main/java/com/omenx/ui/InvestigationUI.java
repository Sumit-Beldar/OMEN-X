package com.omenx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// =========================================================
// INVESTIGATION UI
// Main investigation workspace.
// This class contains only UI.
// Scanner logic stays inside the respective scanner classes.
// =========================================================

public class InvestigationUI {

    // =========================================================
    // COLOR SYSTEM
    // =========================================================

    private static final String BG = "#0A0E12";
    private static final String PANEL = "#11161C";
    private static final String SURFACE = "#161C23";
    private static final String BORDER = "#1E262F";
    private static final String TEXT = "#E8EEF4";
    private static final String MUTED = "#6B7785";
    private static final String BLUE = "#5B9BD5";

    // =========================================================
    // ACTIONS
    // =========================================================

    private final Runnable backAction;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public InvestigationUI(Runnable backAction) {
        this.backAction = backAction;
    }

    // =========================================================
    // MAIN VIEW
    // Creates the Investigation screen.
    // =========================================================

    public BorderPane createView() {

        BorderPane root = new BorderPane();

        root.setStyle(
                "-fx-background-color: " + BG + ";"
        );

        // =====================================================
        // HEADER
        // =====================================================

        Label title =
                new Label("Investigation");

        title.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 20px;" +
                " -fx-font-weight: bold;"
        );

        Button backButton =
                new Button("←  Dashboard");

        backButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + MUTED + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 6 14;" +
                "-fx-cursor: hand;"
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
                        "Combine OSINT modules into a single investigation."
                );

        subtitle.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        // =====================================================
        // INVESTIGATION STATUS
        // =====================================================

        Label statusTitle =
                new Label("Investigation Workspace");

        statusTitle.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 15px;" +
                " -fx-font-weight: bold;"
        );

        Label status =
                new Label(
                        "No active investigation"
                );

        status.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 12px;"
        );

        VBox statusBox =
                createPanel(
                        statusTitle,
                        status
                );

        // =====================================================
        // MODULE INFORMATION
        // =====================================================

        Label modulesTitle =
                new Label(
                        "Available Intelligence Modules"
                );

        modulesTitle.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 15px;" +
                " -fx-font-weight: bold;"
        );

        VBox username =
                createModule(
                        "Username",
                        "Public profiles & social accounts"
                );

        VBox email =
                createModule(
                        "Email",
                        "Exposure & breach checks"
                );

        VBox domain =
                createModule(
                        "Domain",
                        "WHOIS, DNS & infrastructure"
                );

        VBox image =
                createModule(
                        "Image",
                        "Metadata & reverse image analysis"
                );

        VBox modules =
                new VBox(
                        10,
                        modulesTitle,
                        username,
                        email,
                        domain,
                        image
                );

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        VBox content =
                new VBox(
                        18,
                        subtitle,
                        statusBox,
                        modules
                );

        content.setPadding(
                new Insets(
                        10,
                        32,
                        32,
                        32
                )
        );

        root.setCenter(content);

        return root;
    }

    // =========================================================
    // CREATE MODULE CARD
    // =========================================================

    private VBox createModule(
            String title,
            String description
    ) {

        Label moduleTitle =
                new Label(title);

        moduleTitle.setStyle(
                "-fx-text-fill: " + TEXT +
                "; -fx-font-size: 13px;" +
                " -fx-font-weight: bold;"
        );

        Label moduleDescription =
                new Label(description);

        moduleDescription.setStyle(
                "-fx-text-fill: " + MUTED +
                "; -fx-font-size: 11px;"
        );

        VBox card =
                new VBox(
                        5,
                        moduleTitle,
                        moduleDescription
                );

        card.setPadding(
                new Insets(14)
        );

        card.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );

        return card;
    }

    // =========================================================
    // CREATE PANEL
    // =========================================================

    private VBox createPanel(
            javafx.scene.Node... nodes
    ) {

        VBox panel =
                new VBox(
                        8,
                        nodes
                );

        panel.setPadding(
                new Insets(16)
        );

        panel.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7px;" +
                "-fx-background-radius: 7px;"
        );

        return panel;
    }
}