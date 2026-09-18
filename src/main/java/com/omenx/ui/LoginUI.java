package com.omenx.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

// =========================================================
// OMEN-X DARK GLASS CYBER LOGIN UI
// Intelligence Authentication & Command Gateway
// Dark Glassmorphic Aesthetic (OSIRIS-aligned)
// Atmospheric Radar Canvas, Hairline Accents, Fluid Motion
// =========================================================
public class LoginUI {

    // =========================================================
    // DESIGN TOKENS (1:1 with DashboardUI & app.css)
    // =========================================================
    private static final String CYAN   = "#22D3EE";
    private static final String GREEN  = "#10B981";
    private static final String RED    = "#EF4444";
    private static final String AMBER  = "#F59E0B";
    private static final String TEXT   = "#F1F5F9";
    private static final String MUTED  = "#64748B";
    private static final String MUTED_LIGHT = "#94A3B8";

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
    private boolean isPaused = false;
    private boolean isAuthenticating = false;

    // Ambient floating particle model
    private static class Particle {
        double x, y;
        double vy;
        double vx;
        double size;
        double alpha;
        double pulseOffset;
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
        rootStack.setStyle("-fx-background-color: #04060A;");

        // =====================================================
        // 1. FULL-BLEED ATMOSPHERIC BACKGROUND CANVAS
        // Layered: Radial Vignette + Precision Grid + Tactical Radar Rings + Telemetry
        // =====================================================
        Canvas canvas = new Canvas(1340, 860);
        canvas.widthProperty().bind(rootStack.widthProperty());
        canvas.heightProperty().bind(rootStack.heightProperty());

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Generate ambient telemetry particles
        Random random = new Random();
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Particle p = new Particle();
            p.x = random.nextDouble() * 1600;
            p.y = random.nextDouble() * 1000;
            p.vy = 0.18 + random.nextDouble() * 0.35;
            p.vx = (random.nextDouble() - 0.5) * 0.12;
            p.size = 1.0 + random.nextDouble() * 2.0;
            p.alpha = 0.12 + random.nextDouble() * 0.30;
            p.pulseOffset = random.nextDouble() * Math.PI * 2;
            particles.add(p);
        }

        final double[] sweepAngle = {0.0};
        final double[] scanY = {0.0};
        final double[] pulse = {0.0};

        backgroundTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (isPaused) return;

                // Throttle to ~60fps
                if (now - lastUpdate < 16_000_000) return;
                lastUpdate = now;

                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w <= 0 || h <= 0) return;

                gc.clearRect(0, 0, w, h);

                // ── 1. Deep Vignette Background ──
                double cx = w * 0.5;
                double cy = h * 0.5;
                double radius = Math.max(w, h) * 0.75;

                RadialGradient vignette = new RadialGradient(
                        0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#0B1323")),
                        new Stop(0.45, Color.web("#060A13")),
                        new Stop(1.0, Color.web("#030508"))
                );
                gc.setFill(vignette);
                gc.fillRect(0, 0, w, h);

                // ── 2. Subtle Precision Grid ──
                double gridSize = 46.0;
                pulse[0] += 0.018;
                double gridAlpha = 0.022 + Math.sin(pulse[0]) * 0.008;

                gc.setStroke(Color.rgb(34, 211, 238, gridAlpha));
                gc.setLineWidth(1.0);

                for (double x = 0; x < w; x += gridSize) {
                    gc.strokeLine(x, 0, x, h);
                }
                for (double y = 0; y < h; y += gridSize) {
                    gc.strokeLine(0, y, w, y);
                }

                // Grid intersection crosshairs
                gc.setFill(Color.rgb(34, 211, 238, gridAlpha * 2.2));
                for (double x = gridSize * 2; x < w; x += gridSize * 3) {
                    for (double y = gridSize * 2; y < h; y += gridSize * 3) {
                        gc.fillRect(x - 1, y - 1, 3, 3);
                    }
                }

                // ── 3. Concentric Tactical Radar & Range Rings ──
                gc.setLineWidth(1.0);
                double[] rings = {140, 280, 440, 620, 820};
                for (int idx = 0; idx < rings.length; idx++) {
                    double r = rings[idx];
                    double ringAlpha = 0.020 + (idx % 2 == 0 ? 0.012 : 0.005);
                    gc.setStroke(Color.rgb(34, 211, 238, ringAlpha));
                    gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

                    // Degree markers on rings
                    if (r < w * 0.5) {
                        gc.setFill(Color.rgb(100, 116, 139, 0.25));
                        gc.setFont(javafx.scene.text.Font.font("Consolas", 8));
                        gc.fillText((int) (r * 1.5) + " KM", cx + r + 4, cy - 3);
                    }
                }

                // Radar Cross Axes (0°, 90°, 180°, 270°)
                gc.setStroke(Color.rgb(34, 211, 238, 0.025));
                gc.strokeLine(0, cy, w, cy);
                gc.strokeLine(cx, 0, cx, h);

                // ── 4. Tactical Radar Sweep Arc ──
                sweepAngle[0] += 0.008;
                double maxSweepRadius = Math.min(w, h) * 0.55;
                double sweepEndAngle = sweepAngle[0];
                double sweepLength = Math.toRadians(35);

                for (int step = 0; step < 12; step++) {
                    double stepAngle = sweepEndAngle - (sweepLength * (step / 12.0));
                    double alpha = (1.0 - (step / 12.0)) * 0.040;
                    gc.setStroke(Color.rgb(34, 211, 238, alpha));
                    gc.setLineWidth(1.2);
                    double sx = cx + Math.cos(stepAngle) * maxSweepRadius;
                    double sy = cy + Math.sin(stepAngle) * maxSweepRadius;
                    gc.strokeLine(cx, cy, sx, sy);
                }

                // ── 5. Slow Telemetry Scanline ──
                scanY[0] = (scanY[0] + 0.7) % h;
                LinearGradient scanGrad = new LinearGradient(
                        0, scanY[0] - 28,
                        0, scanY[0],
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.TRANSPARENT),
                        new Stop(0.85, Color.rgb(34, 211, 238, 0.04)),
                        new Stop(1.0, Color.rgb(34, 211, 238, 0.16))
                );
                gc.setFill(scanGrad);
                gc.fillRect(0, Math.max(0, scanY[0] - 28), w, 28);
                gc.setStroke(Color.rgb(34, 211, 238, 0.22));
                gc.setLineWidth(1.0);
                gc.strokeLine(0, scanY[0], w, scanY[0]);

                // ── 6. Drifting Ambient Intelligence Particles ──
                for (Particle p : particles) {
                    p.y -= p.vy;
                    p.x += p.vx;
                    if (p.y < -10) {
                        p.y = h + 10;
                        p.x = random.nextDouble() * w;
                    }
                    if (p.x < -10) p.x = w + 10;
                    if (p.x > w + 10) p.x = -10;

                    double pAlpha = p.alpha + Math.sin(pulse[0] * 1.5 + p.pulseOffset) * 0.08;
                    gc.setFill(Color.rgb(34, 211, 238, Math.max(0.04, Math.min(0.55, pAlpha))));
                    gc.fillOval(p.x, p.y, p.size, p.size);
                }

                // ── 7. Corner Telemetry Readouts ──
                gc.setFill(Color.rgb(100, 116, 139, 0.55));
                gc.setFont(javafx.scene.text.Font.font("Consolas", 10));

                gc.fillText("[SYS] OMEN-X INTEL PLATFORM // v2.5", 32, 34);
                gc.fillText("CLEARANCE: ALPHA-RESTRICTED // PROTOCOL 9", w - 310, 34);

                gc.fillText("SIGINT TELEMETRY: ACTIVE // ENC: AES-256-GCM", 32, h - 26);
                gc.fillText("GATEWAY: SECURE LOCAL NODE // PORT: 8443", w - 285, h - 26);

                // Corner Reticle Brackets (hairline cyan accents)
                gc.setStroke(Color.rgb(34, 211, 238, 0.35));
                gc.setLineWidth(1.5);
                double bSize = 22;
                double bOffset = 20;

                // Top-Left
                gc.strokeLine(bOffset, bOffset, bOffset + bSize, bOffset);
                gc.strokeLine(bOffset, bOffset, bOffset, bOffset + bSize);
                // Top-Right
                gc.strokeLine(w - bOffset, bOffset, w - bOffset - bSize, bOffset);
                gc.strokeLine(w - bOffset, bOffset, w - bOffset, bOffset + bSize);
                // Bottom-Left
                gc.strokeLine(bOffset, h - bOffset, bOffset + bSize, h - bOffset);
                gc.strokeLine(bOffset, h - bOffset, bOffset, h - bOffset - bSize);
                // Bottom-Right
                gc.strokeLine(w - bOffset, h - bOffset, w - bOffset - bSize, h - bOffset);
                gc.strokeLine(w - bOffset, h - bOffset, w - bOffset, h - bOffset - bSize);
            }
        };

        // Window focus listener to pause animation when minimized or unfocused
        rootStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((fObs, oldF, isFocused) -> {
                            isPaused = !isFocused;
                        });
                    }
                });
            }
        });

        backgroundTimer.start();

        // =====================================================
        // 2. CENTER PIECE: DARK GLASS AUTHENTICATION CARD
        // Frosted layering, specular cyan border, hairline glow
        // =====================================================
        VBox card = new VBox(14);
        card.setMaxWidth(440);
        card.setMinWidth(380);
        card.setPadding(new Insets(32, 36, 28, 36));

        // Glass panel styling with elevated specular border and subtle inner glow
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(14, 20, 32, 0.90), rgba(8, 12, 20, 0.95));" +
                "-fx-border-color: linear-gradient(to bottom, rgba(34, 211, 238, 0.40) 0%, rgba(255, 255, 255, 0.08) 50%, rgba(34, 211, 238, 0.15) 100%);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.75), 32, 0.25, 0, 8);"
        );

        // ── Brand Header Block ──
        Label classTag = new Label("// RESTRICTED ACCESS //");
        classTag.setStyle(
                "-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 2.5px;"
        );

        Label logo = new Label("OMEN-X");
        logo.setStyle(
                "-fx-text-fill: #22D3EE;" +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', sans-serif;" +
                "-fx-font-size: 32px;" +
                "-fx-font-weight: 800;" +
                "-fx-letter-spacing: 4px;" +
                "-fx-effect: dropshadow(gaussian, rgba(34, 211, 238, 0.45), 18, 0.25, 0, 0);"
        );

        Label tagline = new Label("GLOBAL OSINT & INTELLIGENCE COMMAND WORKSTATION");
        tagline.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-letter-spacing: 1.5px;"
        );

        Region headerDivider = new Region();
        headerDivider.setPrefHeight(1);
        headerDivider.setMaxWidth(Double.MAX_VALUE);
        headerDivider.setStyle("-fx-background-color: linear-gradient(to right, transparent, rgba(34, 211, 238, 0.35), transparent);");

        VBox brandBox = new VBox(4, classTag, logo, tagline, headerDivider);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(0, 0, 4, 0));

        // ── Section Title & Status ──
        HBox authRow = new HBox(8);
        authRow.setAlignment(Pos.CENTER);

        Label authTitle = new Label("[◈] OPERATOR AUTHENTICATION");
        authTitle.setStyle("-fx-text-fill: #F1F5F9; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label authBadge = new Label("● LIVE GATEWAY");
        authBadge.getStyleClass().addAll("badge", "badge-cyan");
        authBadge.setMinWidth(Region.USE_PREF_SIZE);

        authRow.getChildren().addAll(authTitle, authBadge);

        // ── Field 1: Operator Identifier (Username) ──
        Label userLabel = new Label("[@>]  OPERATOR IDENTIFIER");
        userLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter operator identifier (e.g. Omen-X(Member1))...");
        usernameField.getStyleClass().add("cyber-input");
        usernameField.setPrefHeight(42);

        // Quick Selector Chips for fast switching between authorized operators
        Label chipsTitle = new Label("AUTHORIZED PROFILES:");
        chipsTitle.setStyle("-fx-text-fill: #475569; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        HBox chipsBox = new HBox(6);
        chipsBox.setAlignment(Pos.CENTER_LEFT);
        chipsBox.getChildren().add(chipsTitle);

        for (int m = 1; m <= 5; m++) {
            final String memberId = "Omen-X(Member" + m + ")";
            Button chip = new Button("M" + m);
            chip.setStyle(
                    "-fx-background-color: rgba(34, 211, 238, 0.08); " +
                    "-fx-text-fill: #22D3EE; " +
                    "-fx-border-color: rgba(34, 211, 238, 0.25); " +
                    "-fx-border-radius: 4px; " +
                    "-fx-background-radius: 4px; " +
                    "-fx-padding: 3 8 3 8; " +
                    "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;"
            );
            chip.setTooltip(new Tooltip("Select " + memberId));
            chip.setOnAction(e -> {
                usernameField.setText(memberId);
                usernameField.requestFocus();
                usernameField.positionCaret(memberId.length());
            });

            chip.setOnMouseEntered(e -> chip.setStyle(
                    "-fx-background-color: rgba(34, 211, 238, 0.22); " +
                    "-fx-text-fill: #FFFFFF; " +
                    "-fx-border-color: #22D3EE; " +
                    "-fx-border-radius: 4px; " +
                    "-fx-background-radius: 4px; " +
                    "-fx-padding: 3 8 3 8; " +
                    "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(34, 211, 238, 0.35), 8, 0.2, 0, 0);"
            ));
            chip.setOnMouseExited(e -> chip.setStyle(
                    "-fx-background-color: rgba(34, 211, 238, 0.08); " +
                    "-fx-text-fill: #22D3EE; " +
                    "-fx-border-color: rgba(34, 211, 238, 0.25); " +
                    "-fx-border-radius: 4px; " +
                    "-fx-background-radius: 4px; " +
                    "-fx-padding: 3 8 3 8; " +
                    "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;"
            ));

            chipsBox.getChildren().add(chip);
        }

        VBox userGroup = new VBox(6, userLabel, usernameField, chipsBox);

        // ── Field 2: Security Access Key (Password) with Show/Hide Toggle ──
        Label passLabel = new Label("[#>]  SECURITY ACCESS KEY");
        passLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        // Masked PasswordField
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter security key...");
        passwordField.getStyleClass().add("cyber-input");
        passwordField.setPrefHeight(42);
        passwordField.setStyle("-fx-padding: 10 44 10 14;");

        // Revealed TextField (synced)
        TextField revealedPasswordField = new TextField();
        revealedPasswordField.setPromptText("Enter security key...");
        revealedPasswordField.getStyleClass().add("cyber-input");
        revealedPasswordField.setPrefHeight(42);
        revealedPasswordField.setStyle("-fx-padding: 10 44 10 14;");
        revealedPasswordField.setVisible(false);
        revealedPasswordField.setManaged(false);

        revealedPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Password Show/Hide toggle button
        Button togglePasswordBtn = new Button("SHOW");
        togglePasswordBtn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #64748B; " +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 6 10 6 10;"
        );
        togglePasswordBtn.setTooltip(new Tooltip("Toggle Key Visibility"));

        togglePasswordBtn.setOnAction(e -> {
            if (passwordField.isVisible()) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                revealedPasswordField.setVisible(true);
                revealedPasswordField.setManaged(true);
                togglePasswordBtn.setText("HIDE");
                togglePasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
                revealedPasswordField.requestFocus();
                revealedPasswordField.positionCaret(revealedPasswordField.getText().length());
            } else {
                revealedPasswordField.setVisible(false);
                revealedPasswordField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                togglePasswordBtn.setText("SHOW");
                togglePasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });

        StackPane passInputStack = new StackPane(passwordField, revealedPasswordField, togglePasswordBtn);
        StackPane.setAlignment(togglePasswordBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(togglePasswordBtn, new Insets(0, 4, 0, 0));

        // Remember Me & Session Checkbox
        CheckBox rememberMeCheck = new CheckBox("Retain operator profile credentials on this terminal");
        rememberMeCheck.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        rememberMeCheck.setSelected(true);

        VBox passGroup = new VBox(6, passLabel, passInputStack, rememberMeCheck);

        // ── Inline Error Feedback Banner ──
        HBox errorBanner = new HBox(8);
        errorBanner.setAlignment(Pos.CENTER_LEFT);
        errorBanner.setPadding(new Insets(8, 12, 8, 12));
        errorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);

        Label errorIcon = new Label("⚠");
        errorIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label errorText = new Label("");
        errorText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        errorText.setWrapText(true);
        HBox.setHgrow(errorText, Priority.ALWAYS);

        errorBanner.getChildren().addAll(errorIcon, errorText);

        // ── Primary Action Button ──
        Button loginButton = new Button("AUTHENTICATE SESSION  →");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(44);

        // Button tactile scale micro-interactions
        loginButton.setOnMousePressed(e -> {
            if (!isAuthenticating) {
                loginButton.setScaleX(0.98);
                loginButton.setScaleY(0.98);
            }
        });
        loginButton.setOnMouseReleased(e -> {
            loginButton.setScaleX(1.0);
            loginButton.setScaleY(1.0);
        });

        // ── Footer Status & Security Line ──
        Region bottomDivider = new Region();
        bottomDivider.setPrefHeight(1);
        bottomDivider.setMaxWidth(Double.MAX_VALUE);
        bottomDivider.setStyle("-fx-background-color: rgba(34, 211, 238, 0.16);");

        HBox footerMeta = new HBox(8);
        footerMeta.setAlignment(Pos.CENTER);

        Label certBadge = new Label("● ENC: AES-256-GCM");
        certBadge.setStyle("-fx-text-fill: #10B981; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px; -fx-font-weight: bold;");

        Region dot = new Region();
        dot.setPrefSize(3, 3);
        dot.setStyle("-fx-background-color: #475569; -fx-background-radius: 2px;");

        Label legalNotice = new Label("AUTHORIZED OPERATORS ONLY // ALL ACTIONS AUDITED");
        legalNotice.setStyle("-fx-text-fill: #475569; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 9px;");

        footerMeta.getChildren().addAll(certBadge, dot, legalNotice);

        // ── Action & Authentication Flow ──
        Runnable doLogin = () -> {
            if (isAuthenticating) return;

            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                errorText.setText("OPERATOR ID & SECURITY KEY REQUIRED.");
                errorBanner.setVisible(true);
                errorBanner.setManaged(true);
                shakeNode(card);
                return;
            }

            String validPass = CREDENTIALS.get(user);
            if (validPass == null) {
                // Case-insensitive lookup
                for (Map.Entry<String, String> entry : CREDENTIALS.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(user)) {
                        validPass = entry.getValue();
                        user = entry.getKey();
                        break;
                    }
                }
            }

            if (validPass != null && validPass.equals(pass)) {
                isAuthenticating = true;
                errorBanner.setVisible(false);
                errorBanner.setManaged(false);

                loginButton.setDisable(true);
                loginButton.setText("VERIFYING CREDENTIALS...");
                loginButton.setStyle("-fx-background-color: linear-gradient(to right, #0891B2, #06B6D4); -fx-text-fill: #F1F5F9;");

                final String resolvedUser = user.startsWith("Omen-X") ? user : "Omen-X(" + user.substring(0, 1).toUpperCase() + user.substring(1) + ")";

                // Elegant 300ms verification pause before entering dashboard
                PauseTransition pause = new PauseTransition(Duration.millis(320));
                pause.setOnFinished(ev -> {
                    loginButton.setText("● ACCESS GRANTED • INITIALIZING");
                    loginButton.setStyle("-fx-background-color: linear-gradient(to right, #10B981, #059669); -fx-text-fill: #FFFFFF;");

                    FadeTransition fadeOut = new FadeTransition(Duration.millis(220), card);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(e2 -> {
                        stopAnimation();
                        onLoginSuccess.accept(resolvedUser);
                    });
                    fadeOut.play();
                });
                pause.play();

            } else {
                errorText.setText("ACCESS DENIED: Invalid operator identifier or security key.");
                errorBanner.setVisible(true);
                errorBanner.setManaged(true);
                shakeNode(card);
            }
        };

        loginButton.setOnAction(e -> doLogin.run());
        usernameField.setOnAction(e -> {
            if (passwordField.isVisible()) {
                passwordField.requestFocus();
            } else {
                revealedPasswordField.requestFocus();
            }
        });
        passwordField.setOnAction(e -> doLogin.run());
        revealedPasswordField.setOnAction(e -> doLogin.run());

        // Assemble Card Content
        card.getChildren().addAll(
                brandBox,
                authRow,
                userGroup,
                passGroup,
                errorBanner,
                loginButton,
                bottomDivider,
                footerMeta
        );

        // ── Entrance Micro-Interaction ──
        card.setOpacity(0.0);
        card.setTranslateY(24.0);

        FadeTransition cardFade = new FadeTransition(Duration.millis(380), card);
        cardFade.setFromValue(0.0);
        cardFade.setToValue(1.0);
        cardFade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition cardSlide = new TranslateTransition(Duration.millis(380), card);
        cardSlide.setFromY(24.0);
        cardSlide.setToY(0.0);
        cardSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(cardFade, cardSlide);
        entrance.setDelay(Duration.millis(80));
        entrance.play();

        // Wrap card inside a transparent ScrollPane to guarantee no clipping on 720p or small displays
        ScrollPane scrollWrapper = new ScrollPane();
        scrollWrapper.setContent(card);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setFitToHeight(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        // Centering layout
        StackPane centerWrapper = new StackPane(scrollWrapper);
        centerWrapper.setAlignment(Pos.CENTER);
        StackPane.setAlignment(card, Pos.CENTER);

        rootStack.getChildren().addAll(canvas, centerWrapper);
        return rootStack;
    }

    // =========================================================
    // TACTILE ERROR SHAKE ANIMATION
    // =========================================================
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }
}
