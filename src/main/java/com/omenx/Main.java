package com.omenx;

import com.omenx.osint.UsernameScanner;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // =========================
        // SIDEBAR
        // =========================

        Label logo = new Label("OMEN-X");
        logo.setStyle(
                "-fx-text-fill: #00ff9d;" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle = new Label("OSINT INTELLIGENCE PLATFORM");
        subtitle.setStyle(
                "-fx-text-fill: #718096;" +
                "-fx-font-size: 11px;"
        );

        Button dashboardButton = createMenuButton("▣  Dashboard");
        Button usernameButton = createMenuButton("◉  Username");
        Button emailButton = createMenuButton("✉  Email");
        Button domainButton = createMenuButton("◎  Domain");
        Button ipButton = createMenuButton("◆  IP Address");
        Button phoneButton = createMenuButton("☎  Phone");
        Button reportsButton = createMenuButton("▤  Reports");

        VBox sidebar = new VBox(
                10,
                logo,
                subtitle,
                dashboardButton,
                usernameButton,
                emailButton,
                domainButton,
                ipButton,
                phoneButton,
                reportsButton
        );

        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setPrefWidth(230);

        sidebar.setStyle(
                "-fx-background-color: #080d12;"
        );

        // =========================
        // TITLE
        // =========================

        Label title = new Label("OSINT INVESTIGATION");

        title.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Investigate publicly available information from a target."
        );

        description.setStyle(
                "-fx-text-fill: #718096;" +
                "-fx-font-size: 14px;"
        );

        // =========================
        // SEARCH TYPE
        // =========================

        ComboBox<String> typeBox = new ComboBox<>();

        typeBox.getItems().addAll(
                "Username",
                "Email",
                "Domain",
                "IP Address",
                "Phone"
        );

        typeBox.setValue("Username");
        typeBox.setPrefWidth(160);

        // =========================
        // TARGET
        // =========================

        TextField targetField = new TextField();

        targetField.setPromptText("Enter username...");
        targetField.setPrefWidth(430);

        // =========================
        // SEARCH BUTTON
        // =========================

        Button searchButton = new Button("SEARCH");

        searchButton.setPrefWidth(120);
        searchButton.setPrefHeight(40);

        searchButton.setStyle(
                "-fx-background-color: #00ff9d;" +
                "-fx-text-fill: #00140d;" +
                "-fx-font-weight: bold;"
        );

        HBox searchBox = new HBox(
                10,
                typeBox,
                targetField,
                searchButton
        );

        searchBox.setAlignment(Pos.CENTER_LEFT);

        // =========================
        // RESULTS
        // =========================

        Label resultsTitle = new Label("INVESTIGATION RESULTS");

        resultsTitle.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        TextArea resultsArea = new TextArea();

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setPrefHeight(450);

        resultsArea.setStyle(
                "-fx-control-inner-background: #101820;" +
                "-fx-text-fill: #a0aec0;" +
                "-fx-font-family: 'Consolas';" +
                "-fx-font-size: 14px;"
        );

        resultsArea.setText(
                "No investigation started.\n\n" +
                "Enter a username and click SEARCH."
        );

        VBox resultsCard = new VBox(
                15,
                resultsTitle,
                resultsArea
        );

        resultsCard.setPadding(new Insets(25));

        resultsCard.setStyle(
                "-fx-background-color: #101820;" +
                "-fx-border-color: #263442;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        // =========================
        // REAL OSINT SEARCH
        // =========================

        searchButton.setOnAction(event -> {

            String type = typeBox.getValue();
            String target = targetField.getText().trim();

            if (target.isEmpty()) {

                resultsArea.setText(
                        "ERROR\n" +
                        "-----\n\n" +
                        "Please enter a target."
                );

                return;
            }

            // USERNAME OSINT

            if (type.equals("Username")) {

                resultsArea.setText(
                        "SCANNING...\n\n" +
                        "Target: " + target + "\n\n" +
                        "Please wait..."
                );

                UsernameScanner scanner =
                        new UsernameScanner();

                Map<String, String> scanResults =
                        scanner.scan(target);

                StringBuilder output =
                        new StringBuilder();

                output.append(
                        "===== OMEN-X USERNAME OSINT =====\n\n"
                );

                output.append(
                        "Target: "
                ).append(target).append("\n\n");

                output.append(
                        "PUBLIC PROFILE CHECKS\n"
                );

                output.append(
                        "---------------------\n\n"
                );

                for (Map.Entry<String, String> entry
                        : scanResults.entrySet()) {

                    output.append(
                            entry.getKey()
                    );

                    output.append(
                            " : "
                    );

                    output.append(
                            entry.getValue()
                    );

                    output.append("\n\n");
                }

                output.append(
                        "===== END OF SCAN ====="
                );

                resultsArea.setText(
                        output.toString()
                );

            } else {

                resultsArea.setText(
                        "MODULE NOT IMPLEMENTED\n\n" +
                        "Type: " + type + "\n" +
                        "Target: " + target + "\n\n" +
                        "This OSINT module will be added next."
                );
            }
        });

        // ENTER KEY = SEARCH

        targetField.setOnAction(
                event -> searchButton.fire()
        );

        // =========================
        // MAIN CONTENT
        // =========================

        VBox mainContent = new VBox(
                20,
                title,
                description,
                searchBox,
                resultsCard
        );

        mainContent.setPadding(
                new Insets(45)
        );

        mainContent.setStyle(
                "-fx-background-color: #070b0f;"
        );

        // =========================
        // ROOT
        // =========================

        BorderPane root = new BorderPane();

        root.setLeft(sidebar);
        root.setCenter(mainContent);

        // =========================
        // WINDOW
        // =========================

        Scene scene = new Scene(
                root,
                1200,
                750
        );

        stage.setTitle(
                "OMEN-X OSINT Platform"
        );

        stage.setScene(scene);

        stage.show();
    }

    // =========================
    // SIDEBAR BUTTON
    // =========================

    private Button createMenuButton(
            String text) {

        Button button =
                new Button(text);

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        button.setPrefHeight(42);

        button.setAlignment(
                Pos.CENTER_LEFT
        );

        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #a0aec0;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 12px 15px;"
        );

        return button;
    }

    public static void main(String[] args) {

        launch(args);
    }
}