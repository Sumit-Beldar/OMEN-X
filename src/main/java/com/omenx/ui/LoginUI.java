package com.omenx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

// =========================================================
// OMEN-X CYBER LOGIN UI
// Authentication screen for authorized investigation personnel
// =========================================================

public class LoginUI {

    // =========================================================
    // AUTHORIZED CREDENTIALS DATABASE
    // =========================================================

    private static final Map<String, String> CREDENTIALS = new LinkedHashMap<>();

    static {
        CREDENTIALS.put("Omen-X(Member1)", "SkibidiSahur@67");
        CREDENTIALS.put("Omen-X(Member2)", "mPhg@69");
        CREDENTIALS.put("Omen-X(Member3)", "Usersayshello");
        CREDENTIALS.put("Omen-X(Member4)", "iwannabehackertoo");
        CREDENTIALS.put("Omen-X(Member5)", "tspmofr@911");

        // Allow lowercase / shortcut aliases
        CREDENTIALS.put("member1", "SkibidiSahur@67");
        CREDENTIALS.put("member2", "mPhg@69");
        CREDENTIALS.put("member3", "Usersayshello");
        CREDENTIALS.put("member4", "iwannabehackertoo");
        CREDENTIALS.put("member5", "tspmofr@911");
    }

    private final Consumer<String> onLoginSuccess;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public LoginUI(Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public VBox createView() {

        // Logo & Branding Header
        Label logo = new Label("OMEN-X");
        logo.setStyle(
                "-fx-text-fill: #00F2FE;" +
                "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 3px;"
        );

        Label tagline = new Label("SECURITY & OSINT PLATFORM");
        tagline.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 2px;"
        );

        VBox brandBox = new VBox(2, logo, tagline);
        brandBox.setAlignment(Pos.CENTER);

        Label authBadge = new Label("SECURE SYSTEM AUTHENTICATION");
        authBadge.getStyleClass().addAll("badge", "badge-cyan");

        // Username Field
        Label userLabel = new Label("USERNAME");
        userLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. Omen-X(Member1)");
        usernameField.getStyleClass().add("cyber-input");
        usernameField.setPrefHeight(42);

        VBox userGroup = new VBox(6, userLabel, usernameField);

        // Password Field
        Label passLabel = new Label("PASSWORD");
        passLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password...");
        passwordField.getStyleClass().add("cyber-input");
        passwordField.setPrefHeight(42);

        VBox passGroup = new VBox(6, passLabel, passwordField);

        // Error Feedback Label
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setAlignment(Pos.CENTER);

        // Submit Button
        Button loginButton = new Button("AUTHENTICATE  →");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(44);

        // Quick Hint Box
        Label hintHeader = new Label("AUTHORIZED ACCOUNTS");
        hintHeader.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label hintText = new Label("Omen-X(Member1) ... Omen-X(Member5)");
        hintText.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");

        VBox hintBox = new VBox(2, hintHeader, hintText);
        hintBox.setAlignment(Pos.CENTER);
        hintBox.setPadding(new Insets(10));
        hintBox.setStyle("-fx-background-color: #080D16; -fx-border-color: #131B2A; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        // Action Logic
        Runnable doLogin = () -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password.");
                return;
            }

            String validPass = CREDENTIALS.get(user);
            if (validPass == null) {
                // Try case-insensitive matching
                for (Map.Entry<String, String> entry : CREDENTIALS.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(user)) {
                        validPass = entry.getValue();
                        user = entry.getKey();
                        break;
                    }
                }
            }

            if (validPass != null && validPass.equals(pass)) {
                errorLabel.setText("");
                String finalUser = user.startsWith("Omen-X") ? user : "Omen-X(" + user.substring(0, 1).toUpperCase() + user.substring(1) + ")";
                onLoginSuccess.accept(finalUser);
            } else {
                errorLabel.setText("Invalid username or password. Access Denied.");
            }
        };

        loginButton.setOnAction(e -> doLogin.run());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> doLogin.run());

        // Card Container
        VBox card = new VBox(
                16,
                brandBox,
                authBadge,
                userGroup,
                passGroup,
                errorLabel,
                loginButton,
                hintBox
        );

        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);
        card.setPadding(new Insets(32, 36, 32, 36));
        card.getStyleClass().add("cyber-card");

        VBox outer = new VBox(card);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));
        outer.setStyle("-fx-background-color: #05080E;");
        VBox.setVgrow(card, Priority.NEVER);

        return outer;
    }
}
