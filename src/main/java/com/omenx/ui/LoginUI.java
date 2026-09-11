package com.omenx.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

// =========================================================
// OMEN-X CYBER LOGIN UI
// Military-grade authentication screen with animated tactical HUD
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
    private AnimationTimer backgroundTimer;

    // Ambient particle model
    private static class Particle {
        double x, y;
        double vy;
        double size;
        double alpha;
        double pulseSpeed;
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public LoginUI(Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void stopAnimation() {
        if (backgroundTimer != null) {
            backgroundTimer.stop();
            backgroundTimer = null;
        }
    }

    // =========================================================
    // CREATE VIEW
    // =========================================================

    public StackPane createView() {

        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: #07090F;");

        // =====================================================
        // 1. ANIMATED TACTICAL CYBER CANVAS BACKGROUND
        // =====================================================

        Canvas canvas = new Canvas(1340, 860);
        canvas.widthProperty().bind(rootStack.widthProperty());
        canvas.heightProperty().bind(rootStack.heightProperty());

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Generate ambient floating particles
        Random random = new Random();
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Particle p = new Particle();
            p.x = random.nextDouble() * 1400;
            p.y = random.nextDouble() * 900;
            p.vy = 0.2 + random.nextDouble() * 0.4;
            p.size = 1.0 + random.nextDouble() * 2.2;
            p.alpha = 0.15 + random.nextDouble() * 0.35;
            p.pulseSpeed = 0.02 + random.nextDouble() * 0.03;
            particles.add(p);
        }

        final double[] scanlineY = {0.0};
        final double[] pulse = {0.0};

        backgroundTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w <= 0 || h <= 0) return;

                gc.clearRect(0, 0, w, h);

                // Base deep dark background
                gc.setFill(Color.web("#07090F"));
                gc.fillRect(0, 0, w, h);

                // Subtle tactical grid
                double gridSize = 48.0;
                pulse[0] += 0.02;
                double gridAlpha = 0.035 + Math.sin(pulse[0]) * 0.012;
                gc.setStroke(Color.rgb(220, 38, 38, Math.max(0.015, gridAlpha)));
                gc.setLineWidth(1.0);

                for (double x = 0; x < w; x += gridSize) {
                    gc.strokeLine(x, 0, x, h);
                }
                for (double y = 0; y < h; y += gridSize) {
                    gc.strokeLine(0, y, w, y);
                }

                // Grid intersection points (crosshairs)
                gc.setFill(Color.rgb(220, 38, 38, gridAlpha * 2.2));
                for (double x = gridSize; x < w; x += gridSize * 2) {
                    for (double y = gridSize; y < h; y += gridSize * 2) {
                        gc.fillRect(x - 1, y - 1, 3, 3);
                    }
                }

                // Slow horizontal scanline
                scanlineY[0] = (scanlineY[0] + 0.9) % h;
                LinearGradient scanGrad = new LinearGradient(
                        0, scanlineY[0] - 30,
                        0, scanlineY[0],
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.TRANSPARENT),
                        new Stop(0.8, Color.rgb(220, 38, 38, 0.08)),
                        new Stop(1.0, Color.rgb(220, 38, 38, 0.22))
                );
                gc.setFill(scanGrad);
                gc.fillRect(0, Math.max(0, scanlineY[0] - 30), w, 30);
                gc.setStroke(Color.rgb(220, 38, 38, 0.35));
                gc.setLineWidth(1.2);
                gc.strokeLine(0, scanlineY[0], w, scanlineY[0]);

                // Ambient crimson particles drifting upward
                for (Particle p : particles) {
                    p.y -= p.vy;
                    if (p.y < -10) {
                        p.y = h + 10;
                        p.x = random.nextDouble() * w;
                    }
                    double pAlpha = p.alpha + Math.sin(pulse[0] + p.x) * 0.1;
                    gc.setFill(Color.rgb(220, 38, 38, Math.max(0.05, Math.min(0.6, pAlpha))));
                    gc.fillOval(p.x, p.y, p.size, p.size);
                }

                // HUD Corner Telemetry
                gc.setFill(Color.rgb(120, 134, 152, 0.45));
                gc.setFont(javafx.scene.text.Font.font("Consolas", 10));
                gc.fillText("[SYS] OMEN-X TACTICAL RECONNAISSANCE CORE // v2.5", 28, 32);
                gc.fillText("CLEARANCE: ALPHA-RESTRICTED // PROTOCOL 9", w - 290, 32);
                gc.fillText("TELEMETRY: ACTIVE // ENCRYPTION: AES-256-GCM", 28, h - 24);
                gc.fillText("NODE: SECURE GATEWAY // PORT: 8443", w - 240, h - 24);

                // Corner bracket accents
                gc.setStroke(Color.rgb(220, 38, 38, 0.4));
                gc.setLineWidth(1.5);
                // Top-Left
                gc.strokeLine(18, 18, 42, 18);
                gc.strokeLine(18, 18, 18, 42);
                // Top-Right
                gc.strokeLine(w - 18, 18, w - 42, 18);
                gc.strokeLine(w - 18, 18, w - 18, 42);
                // Bottom-Left
                gc.strokeLine(18, h - 18, 42, h - 18);
                gc.strokeLine(18, h - 18, 18, h - 42);
                // Bottom-Right
                gc.strokeLine(w - 18, h - 18, w - 42, h - 18);
                gc.strokeLine(w - 18, h - 18, w - 18, h - 42);
            }
        };
        backgroundTimer.start();

        // =====================================================
        // 2. CENTER AUTHENTICATION CARD
        // =====================================================

        Label classTag = new Label("// RESTRICTED ACCESS //");
        classTag.setStyle(
                "-fx-text-fill: #DC2626; -fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2px;"
        );

        Label logo = new Label("OMEN-X");
        logo.setStyle(
                "-fx-text-fill: #DC2626;" +
                "-fx-font-family: 'Consolas', 'Segoe UI', sans-serif;" +
                "-fx-font-size: 34px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 4px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(220, 38, 38, 0.4), 16, 0, 0, 0);"
        );

        Label tagline = new Label("TACTICAL OSINT & THREAT INTELLIGENCE");
        tagline.setStyle(
                "-fx-text-fill: #788698;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 2px;"
        );

        VBox brandBox = new VBox(4, classTag, logo, tagline);
        brandBox.setAlignment(Pos.CENTER);

        Label authBadge = new Label("IDENTITY VERIFICATION REQUIRED");
        authBadge.getStyleClass().addAll("badge", "badge-red");
        authBadge.setMinWidth(Region.USE_PREF_SIZE);

        // Username Field
        Label userLabel = new Label("OPERATOR IDENTIFIER");
        userLabel.setStyle("-fx-text-fill: #788698; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. Omen-X(Member1)");
        usernameField.getStyleClass().add("cyber-input");
        usernameField.setPrefHeight(42);

        VBox userGroup = new VBox(6, userLabel, usernameField);

        // Password Field
        Label passLabel = new Label("SECURITY ACCESS KEY");
        passLabel.setStyle("-fx-text-fill: #788698; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter security key...");
        passwordField.getStyleClass().add("cyber-input");
        passwordField.setPrefHeight(42);

        VBox passGroup = new VBox(6, passLabel, passwordField);

        // Error Feedback Label
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setAlignment(Pos.CENTER);

        // Submit Button
        Button loginButton = new Button("AUTHENTICATE SESSION  →");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(44);

        // Quick Hint Box
        Label hintHeader = new Label("AUTHORIZED OPERATOR PROFILES");
        hintHeader.setStyle("-fx-text-fill: #788698; -fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label hintText = new Label("Omen-X(Member1)  •••  Omen-X(Member5)");
        hintText.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        VBox hintBox = new VBox(4, hintHeader, hintText);
        hintBox.setAlignment(Pos.CENTER);
        hintBox.setPadding(new Insets(10, 14, 10, 14));
        hintBox.setStyle("-fx-background-color: #0B0E17; -fx-border-color: #1C2234; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        // Action Logic
        Runnable doLogin = () -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and access key.");
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
                stopAnimation();
                String finalUser = user.startsWith("Omen-X") ? user : "Omen-X(" + user.substring(0, 1).toUpperCase() + user.substring(1) + ")";
                onLoginSuccess.accept(finalUser);
            } else {
                errorLabel.setText("ACCESS DENIED: Invalid operator identifier or access key.");
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
        card.setMaxWidth(400);
        card.setPadding(new Insets(36, 38, 36, 38));
        card.getStyleClass().add("cyber-card");

        rootStack.getChildren().addAll(canvas, card);
        StackPane.setAlignment(card, Pos.CENTER);

        return rootStack;
    }
}
