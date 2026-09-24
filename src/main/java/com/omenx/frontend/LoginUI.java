package com.omenx.frontend;

import com.omenx.backend.service.EmailService;
import com.omenx.backend.service.PasswordUtil;
import com.omenx.database.UserDatabase;
import com.omenx.database.model.UserRecord;
import javafx.animation.*;
import javafx.application.Platform;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LoginUI {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final int COOLDOWN_SECONDS = 60;

    private final Consumer<String> onLoginSuccess;
    private final UserDatabase userDatabase;
    private final EmailService emailService;
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    private AnimationTimer backgroundTimer;
    private boolean isPaused = false;
    private boolean isAuthenticating = false;

    private StackPane cardContainer;
    private Timeline cooldownTimeline;
    private long lastResendTime = 0;

    private static class Particle {
        double x, y;
        double vy;
        double vx;
        double size;
        double alpha;
        double pulseOffset;
    }

    public LoginUI(Consumer<String> onLoginSuccess) {
        this(onLoginSuccess, new UserDatabase(), new EmailService());
    }

    public LoginUI(Consumer<String> onLoginSuccess, UserDatabase userDatabase, EmailService emailService) {
        this.onLoginSuccess = onLoginSuccess;
        this.userDatabase = userDatabase;
        this.emailService = emailService;
    }

    public void stopAnimation() {
        if (backgroundTimer != null) {
            backgroundTimer.stop();
            backgroundTimer = null;
        }
        if (cooldownTimeline != null) {
            cooldownTimeline.stop();
            cooldownTimeline = null;
        }
        asyncExecutor.shutdownNow();
    }

    public StackPane createView() {
        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: #04060A;");

        Canvas canvas = new Canvas(1340, 860);
        canvas.widthProperty().bind(rootStack.widthProperty());
        canvas.heightProperty().bind(rootStack.heightProperty());

        GraphicsContext gc = canvas.getGraphicsContext2D();

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

                if (now - lastUpdate < 16_000_000) return;
                lastUpdate = now;

                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w <= 0 || h <= 0) return;

                gc.clearRect(0, 0, w, h);

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

                gc.setFill(Color.rgb(34, 211, 238, gridAlpha * 2.2));
                for (double x = gridSize * 2; x < w; x += gridSize * 3) {
                    for (double y = gridSize * 2; y < h; y += gridSize * 3) {
                        gc.fillRect(x - 1, y - 1, 3, 3);
                    }
                }

                gc.setLineWidth(1.0);
                double[] rings = {140, 280, 440, 620, 820};
                for (int idx = 0; idx < rings.length; idx++) {
                    double r = rings[idx];
                    double ringAlpha = 0.020 + (idx % 2 == 0 ? 0.012 : 0.005);
                    gc.setStroke(Color.rgb(34, 211, 238, ringAlpha));
                    gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

                    if (r < w * 0.5) {
                        gc.setFill(Color.rgb(100, 116, 139, 0.25));
                        gc.setFont(javafx.scene.text.Font.font("Consolas", 8));
                        gc.fillText((int) (r * 1.5) + " KM", cx + r + 4, cy - 3);
                    }
                }

                gc.setStroke(Color.rgb(34, 211, 238, 0.025));
                gc.strokeLine(0, cy, w, cy);
                gc.strokeLine(cx, 0, cx, h);

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

                gc.setFill(Color.rgb(100, 116, 139, 0.55));
                gc.setFont(javafx.scene.text.Font.font("Consolas", 10));

                gc.fillText("[SYS] OMEN-X INTEL PLATFORM // v2.5", 32, 34);
                gc.fillText("CLEARANCE: ALPHA-RESTRICTED // PROTOCOL 9", w - 310, 34);

                gc.fillText("SIGINT TELEMETRY: ACTIVE // ENC: AES-256-GCM", 32, h - 26);
                gc.fillText("GATEWAY: SECURE LOCAL NODE // PORT: 8443", w - 285, h - 26);

                gc.setStroke(Color.rgb(34, 211, 238, 0.35));
                gc.setLineWidth(1.5);
                double bSize = 22;
                double bOffset = 20;

                gc.strokeLine(bOffset, bOffset, bOffset + bSize, bOffset);
                gc.strokeLine(bOffset, bOffset, bOffset, bOffset + bSize);
                gc.strokeLine(w - bOffset, bOffset, w - bOffset - bSize, bOffset);
                gc.strokeLine(w - bOffset, bOffset, w - bOffset, bOffset + bSize);
                gc.strokeLine(bOffset, h - bOffset, bOffset + bSize, h - bOffset);
                gc.strokeLine(bOffset, h - bOffset, bOffset, h - bOffset - bSize);
                gc.strokeLine(w - bOffset, h - bOffset, w - bOffset - bSize, h - bOffset);
                gc.strokeLine(w - bOffset, h - bOffset, w - bOffset, h - bOffset - bSize);
            }
        };

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

        cardContainer = new StackPane();
        cardContainer.setAlignment(Pos.CENTER);

        ScrollPane scrollWrapper = new ScrollPane();
        scrollWrapper.setContent(cardContainer);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setFitToHeight(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        StackPane centerWrapper = new StackPane(scrollWrapper);
        centerWrapper.setAlignment(Pos.CENTER);

        rootStack.getChildren().addAll(canvas, centerWrapper);

        showLoginCard(null, false);

        return rootStack;
    }

    private void setCard(VBox newCard) {
        if (cardContainer == null) return;

        if (cardContainer.getChildren().isEmpty()) {
            cardContainer.getChildren().add(newCard);
            playEntranceAnimation(newCard);
            return;
        }

        Node current = cardContainer.getChildren().get(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(160), current);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            cardContainer.getChildren().clear();
            cardContainer.getChildren().add(newCard);
            playEntranceAnimation(newCard);
        });
        fadeOut.play();
    }

    private void playEntranceAnimation(Node card) {
        card.setOpacity(0.0);
        card.setTranslateY(18.0);

        FadeTransition cardFade = new FadeTransition(Duration.millis(320), card);
        cardFade.setFromValue(0.0);
        cardFade.setToValue(1.0);
        cardFade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition cardSlide = new TranslateTransition(Duration.millis(320), card);
        cardSlide.setFromY(18.0);
        cardSlide.setToY(0.0);
        cardSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(cardFade, cardSlide);
        entrance.play();
    }

    private VBox createBaseCard() {
        VBox card = new VBox(14);
        card.setMaxWidth(460);
        card.setMinWidth(380);
        card.setPadding(new Insets(30, 36, 26, 36));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(14, 20, 32, 0.90), rgba(8, 12, 20, 0.95));" +
                "-fx-border-color: linear-gradient(to bottom, rgba(34, 211, 238, 0.40) 0%, rgba(255, 255, 255, 0.08) 50%, rgba(34, 211, 238, 0.15) 100%);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.75), 32, 0.25, 0, 8);"
        );
        return card;
    }

    private VBox createBrandHeader() {
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
        return brandBox;
    }

    private Node createFooter() {
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
        return new VBox(10, bottomDivider, footerMeta);
    }

    public void showLoginCard(String initialBannerMessage, boolean isSuccess) {
        isAuthenticating = false;
        VBox card = createBaseCard();

        VBox brandBox = createBrandHeader();

        HBox authRow = new HBox(8);
        authRow.setAlignment(Pos.CENTER);

        Label authTitle = new Label("[◈] OPERATOR AUTHENTICATION");
        authTitle.setStyle("-fx-text-fill: #F1F5F9; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label authBadge = new Label("● LIVE GATEWAY");
        authBadge.getStyleClass().addAll("badge", "badge-cyan");
        authBadge.setMinWidth(Region.USE_PREF_SIZE);

        authRow.getChildren().addAll(authTitle, authBadge);

        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 12, 8, 12));

        Label bannerIcon = new Label("◈");
        Label bannerText = new Label("");
        bannerText.setWrapText(true);
        HBox.setHgrow(bannerText, Priority.ALWAYS);
        banner.getChildren().addAll(bannerIcon, bannerText);

        if (initialBannerMessage != null && !initialBannerMessage.isBlank()) {
            bannerText.setText(initialBannerMessage);
            if (isSuccess) {
                banner.setStyle("-fx-background-color: rgba(16, 185, 129, 0.14); -fx-border-color: rgba(16, 185, 129, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setStyle("-fx-text-fill: #6EE7B7; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            banner.setVisible(true);
            banner.setManaged(true);
        } else {
            banner.setVisible(false);
            banner.setManaged(false);
        }

        Label userLabel = new Label("[@>]  OPERATOR IDENTIFIER OR EMAIL");
        userLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter operator callsign or registered email...");
        usernameField.getStyleClass().add("cyber-input");
        usernameField.setPrefHeight(42);

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

        PasswordInputComponent passComponent = new PasswordInputComponent(
                "[#>]  SECURITY ACCESS KEY",
                "Enter security access key..."
        );

        CheckBox rememberMeCheck = new CheckBox("Retain operator profile credentials on this terminal");
        rememberMeCheck.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        rememberMeCheck.setSelected(true);

        VBox passGroup = new VBox(6, passComponent.container, rememberMeCheck);

        Button loginButton = new Button("AUTHENTICATE SESSION  →");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(44);

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

        HBox signupLinkRow = new HBox(6);
        signupLinkRow.setAlignment(Pos.CENTER);

        Label noAccountLabel = new Label("Don't have an operator profile?");
        noAccountLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        Hyperlink signupLink = new Hyperlink("Sign Up");
        signupLink.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-underline: true; -fx-padding: 0;");
        signupLink.setOnAction(e -> showSignupCard());

        signupLinkRow.getChildren().addAll(noAccountLabel, signupLink);

        Hyperlink verifyLink = new Hyperlink("Enter 6-digit verification code →");
        verifyLink.setStyle("-fx-text-fill: #F59E0B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        verifyLink.setVisible(false);
        verifyLink.setManaged(false);

        Runnable doLogin = () -> {
            if (isAuthenticating) return;

            String identifier = usernameField.getText().trim();
            String pass = passComponent.getText().trim();

            if (identifier.isEmpty() || pass.isEmpty()) {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("⚠");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("OPERATOR ID & SECURITY KEY REQUIRED.");
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
                banner.setVisible(true);
                banner.setManaged(true);
                verifyLink.setVisible(false);
                verifyLink.setManaged(false);
                shakeNode(card);
                return;
            }

            UserDatabase.AuthResult authResult = userDatabase.authenticate(identifier, pass);

            if (authResult.status() == UserDatabase.AuthStatus.SUCCESS) {
                isAuthenticating = true;
                banner.setVisible(false);
                banner.setManaged(false);
                verifyLink.setVisible(false);
                verifyLink.setManaged(false);

                loginButton.setDisable(true);
                loginButton.setText("VERIFYING CREDENTIALS...");
                loginButton.setStyle("-fx-background-color: linear-gradient(to right, #0891B2, #06B6D4); -fx-text-fill: #F1F5F9;");

                UserRecord user = authResult.user();
                final String resolvedUser = (user != null && user.getUsername() != null) ? user.getUsername() : identifier;

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

            } else if (authResult.status() == UserDatabase.AuthStatus.UNVERIFIED) {
                UserRecord user = authResult.user();
                String email = user != null ? user.getEmail() : identifier;

                banner.setStyle("-fx-background-color: rgba(245, 158, 11, 0.14); -fx-border-color: rgba(245, 158, 11, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("!");
                bannerIcon.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("ACCESS RESTRICTED: Account email is not verified yet. Please verify your 6-digit code.");
                bannerText.setStyle("-fx-text-fill: #FDE68A; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
                banner.setVisible(true);
                banner.setManaged(true);

                verifyLink.setOnAction(e -> showVerifyCard(email, "Please enter your 6-digit verification code to activate your account."));
                verifyLink.setVisible(true);
                verifyLink.setManaged(true);

                shakeNode(card);

            } else {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("⚠");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("ACCESS DENIED: Invalid operator identifier or security key.");
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
                banner.setVisible(true);
                banner.setManaged(true);
                verifyLink.setVisible(false);
                verifyLink.setManaged(false);
                shakeNode(card);
            }
        };

        loginButton.setOnAction(e -> doLogin.run());
        usernameField.setOnAction(e -> passComponent.passwordField.requestFocus());
        passComponent.setOnAction(e -> doLogin.run());

        card.getChildren().addAll(
                brandBox,
                authRow,
                userGroup,
                passGroup,
                banner,
                verifyLink,
                loginButton,
                signupLinkRow,
                createFooter()
        );

        setCard(card);
    }

    public void showSignupCard() {
        VBox card = createBaseCard();
        VBox brandBox = createBrandHeader();

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER);

        Label title = new Label("[+] OPERATOR ENROLLMENT");
        title.setStyle("-fx-text-fill: #F1F5F9; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label badge = new Label("● NEW OPERATOR");
        badge.getStyleClass().addAll("badge", "badge-cyan");
        badge.setMinWidth(Region.USE_PREF_SIZE);

        headerRow.getChildren().addAll(title, badge);

        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 12, 8, 12));
        banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
        banner.setVisible(false);
        banner.setManaged(false);

        Label bannerIcon = new Label("⚠");
        bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label bannerText = new Label("");
        bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        bannerText.setWrapText(true);
        HBox.setHgrow(bannerText, Priority.ALWAYS);

        banner.getChildren().addAll(bannerIcon, bannerText);

        Label userLabel = new Label("[@>]  OPERATOR IDENTIFIER");
        userLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose operator callsign (e.g. Omen-X(Member6))...");
        usernameField.getStyleClass().add("cyber-input");
        usernameField.setPrefHeight(40);

        VBox userGroup = new VBox(4, userLabel, usernameField);

        Label emailLabel = new Label("[#>]  OFFICIAL OPERATOR EMAIL");
        emailLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter email for 6-digit verification code...");
        emailField.getStyleClass().add("cyber-input");
        emailField.setPrefHeight(40);

        VBox emailGroup = new VBox(4, emailLabel, emailField);

        PasswordInputComponent passComp = new PasswordInputComponent(
                "[$>]  SECURITY ACCESS KEY (MIN 8 CHARS)",
                "Create master access key..."
        );

        PasswordInputComponent confirmComp = new PasswordInputComponent(
                "[$>]  CONFIRM SECURITY ACCESS KEY",
                "Re-enter security access key..."
        );

        Button signupButton = new Button("CREATE OPERATOR PROFILE  →");
        signupButton.getStyleClass().add("btn-primary");
        signupButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setPrefHeight(44);

        HBox loginLinkRow = new HBox(6);
        loginLinkRow.setAlignment(Pos.CENTER);

        Label hasAccountLabel = new Label("Already enrolled as an operator?");
        hasAccountLabel.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        Hyperlink loginLink = new Hyperlink("Log In");
        loginLink.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-underline: true; -fx-padding: 0;");
        loginLink.setOnAction(e -> showLoginCard(null, false));

        loginLinkRow.getChildren().addAll(hasAccountLabel, loginLink);

        Runnable doSignup = () -> {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String pass = passComp.getText();
            String confirmPass = confirmComp.getText();

            if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                bannerText.setText("ALL FIELDS ARE REQUIRED.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            if (username.length() < 3) {
                bannerText.setText("Operator identifier must be at least 3 characters.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                bannerText.setText("Invalid email format. Please enter a valid address.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            String passError = PasswordUtil.validatePassword(pass);
            if (passError != null) {
                bannerText.setText(passError);
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            if (!pass.equals(confirmPass)) {
                bannerText.setText("Security access keys do not match.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            if (userDatabase.existsByUsername(username)) {
                bannerText.setText("Operator identifier '" + username + "' is already registered.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            if (userDatabase.existsByEmail(email)) {
                bannerText.setText("Email '" + email + "' is already associated with an account.");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            String code = String.format("%06d", new Random().nextInt(1_000_000));
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

            signupButton.setDisable(true);
            signupButton.setText("CREATING ACCOUNT & DISPATCHING CODE...");

            asyncExecutor.submit(() -> {
                try {
                    userDatabase.createUser(username, email, pass, code, expiresAt);
                    EmailService.SendResult emailResult = emailService.sendVerificationCode(email, username, code);

                    Platform.runLater(() -> {
                        String notice = "A 6-digit verification code has been dispatched to " + email + ".";
                        if (!emailResult.success() && emailResult.errorMessage() != null) {
                            if (!emailService.isConfigured()) {
                                notice += " (SMTP placeholder active. Code: " + code + ")";
                            } else {
                                notice += " (Note: SMTP error: " + emailResult.errorMessage() + ")";
                            }
                        }
                        showVerifyCard(email, notice);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        signupButton.setDisable(false);
                        signupButton.setText("CREATE OPERATOR PROFILE  →");
                        bannerText.setText("Registration error: " + ex.getMessage());
                        banner.setVisible(true);
                        banner.setManaged(true);
                        shakeNode(card);
                    });
                }
            });
        };

        signupButton.setOnAction(e -> doSignup.run());
        usernameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passComp.passwordField.requestFocus());
        passComp.setOnAction(e -> confirmComp.passwordField.requestFocus());
        confirmComp.setOnAction(e -> doSignup.run());

        card.getChildren().addAll(
                brandBox,
                headerRow,
                userGroup,
                emailGroup,
                passComp.container,
                confirmComp.container,
                banner,
                signupButton,
                loginLinkRow,
                createFooter()
        );

        setCard(card);
    }

    public void showVerifyCard(String targetEmail, String initialNotice) {
        VBox card = createBaseCard();
        VBox brandBox = createBrandHeader();

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER);

        Label title = new Label("[#] IDENTITY VERIFICATION");
        title.setStyle("-fx-text-fill: #F1F5F9; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        Label badge = new Label("● 6-DIGIT CODE");
        badge.getStyleClass().addAll("badge", "badge-cyan");
        badge.setMinWidth(Region.USE_PREF_SIZE);

        headerRow.getChildren().addAll(title, badge);

        Label noticeLabel = new Label("Enter the 6-digit numeric verification code sent to:");
        noticeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        noticeLabel.setWrapText(true);

        Label emailBadge = new Label(targetEmail != null ? targetEmail : "operator@omenx.local");
        emailBadge.setStyle(
                "-fx-background-color: rgba(34, 211, 238, 0.10); " +
                "-fx-text-fill: #22D3EE; " +
                "-fx-border-color: rgba(34, 211, 238, 0.35); " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 6 12 6 12; " +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold;"
        );
        emailBadge.setMaxWidth(Double.MAX_VALUE);
        emailBadge.setAlignment(Pos.CENTER);

        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 12, 8, 12));

        Label bannerIcon = new Label("◈");
        Label bannerText = new Label("");
        bannerText.setWrapText(true);
        HBox.setHgrow(bannerText, Priority.ALWAYS);
        banner.getChildren().addAll(bannerIcon, bannerText);

        if (initialNotice != null && !initialNotice.isBlank()) {
            bannerText.setText(initialNotice);
            banner.setStyle("-fx-background-color: rgba(34, 211, 238, 0.12); -fx-border-color: rgba(34, 211, 238, 0.35); -fx-border-radius: 6px; -fx-background-radius: 6px;");
            bannerIcon.setStyle("-fx-text-fill: #22D3EE; -fx-font-size: 12px; -fx-font-weight: bold;");
            bannerText.setStyle("-fx-text-fill: #BAE6FD; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
            banner.setVisible(true);
            banner.setManaged(true);
        } else {
            banner.setVisible(false);
            banner.setManaged(false);
        }

        Label codeLabel = new Label("[*>]  6-DIGIT VERIFICATION CODE");
        codeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        TextField codeField = new TextField();
        codeField.setPromptText("• • • • • •");
        codeField.getStyleClass().add("cyber-input");
        codeField.setPrefHeight(48);
        codeField.setAlignment(Pos.CENTER);
        codeField.setStyle(
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 20px; " +
                "-fx-font-weight: 800; " +
                "-fx-letter-spacing: 8px; " +
                "-fx-text-fill: #22D3EE;"
        );

        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 6) {
                digits = digits.substring(0, 6);
            }
            if (!digits.equals(newVal)) {
                codeField.setText(digits);
            }
        });

        Button resendButton = new Button("RESEND VERIFICATION CODE");
        resendButton.getStyleClass().add("btn-ghost");
        resendButton.setStyle(
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 12 6 12; " +
                "-fx-cursor: hand;"
        );

        startCooldown(resendButton);

        resendButton.setOnAction(e -> {
            long now = System.currentTimeMillis();
            if ((now - lastResendTime) < (COOLDOWN_SECONDS * 1000L)) {
                return;
            }

            String newCode = String.format("%06d", new Random().nextInt(1_000_000));
            LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(15);

            boolean updated = userDatabase.updateVerificationCode(targetEmail, newCode, newExpiry);
            if (updated) {
                startCooldown(resendButton);
                banner.setStyle("-fx-background-color: rgba(34, 211, 238, 0.12); -fx-border-color: rgba(34, 211, 238, 0.35); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("✓");
                bannerIcon.setStyle("-fx-text-fill: #22D3EE; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("New 6-digit verification code generated.");
                bannerText.setStyle("-fx-text-fill: #BAE6FD; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
                banner.setVisible(true);
                banner.setManaged(true);

                asyncExecutor.submit(() -> {
                    EmailService.SendResult sendRes = emailService.sendVerificationCode(targetEmail, targetEmail, newCode);
                    Platform.runLater(() -> {
                        if (!sendRes.success() && sendRes.errorMessage() != null) {
                            if (!emailService.isConfigured()) {
                                bannerText.setText("New code generated: " + newCode + " (SMTP placeholder active).");
                            } else {
                                bannerText.setText("New code sent (SMTP notice: " + sendRes.errorMessage() + ")");
                            }
                        }
                    });
                });
            } else {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("⚠");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("Failed to update verification code for " + targetEmail);
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
                banner.setVisible(true);
                banner.setManaged(true);
            }
        });

        HBox resendRow = new HBox(8, resendButton);
        resendRow.setAlignment(Pos.CENTER);

        Button verifyButton = new Button("VERIFY & ACTIVATE ACCOUNT  →");
        verifyButton.getStyleClass().add("btn-primary");
        verifyButton.setMaxWidth(Double.MAX_VALUE);
        verifyButton.setPrefHeight(44);

        HBox navRow = new HBox(12);
        navRow.setAlignment(Pos.CENTER);

        Hyperlink backSignupLink = new Hyperlink("← Back to Signup");
        backSignupLink.setStyle("-fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
        backSignupLink.setOnAction(e -> showSignupCard());

        Label dotSep = new Label("•");
        dotSep.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        Hyperlink backLoginLink = new Hyperlink("Return to Login");
        backLoginLink.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-underline: true;");
        backLoginLink.setOnAction(e -> showLoginCard(null, false));

        navRow.getChildren().addAll(backSignupLink, dotSep, backLoginLink);

        Runnable doVerify = () -> {
            String code = codeField.getText().trim();
            if (code.length() != 6) {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("⚠");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("Please enter the full 6-digit numeric verification code.");
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
                return;
            }

            UserDatabase.VerificationResult vResult = userDatabase.verifyCode(targetEmail, code);

            if (vResult.status() == UserDatabase.VerificationStatus.SUCCESS || vResult.status() == UserDatabase.VerificationStatus.ALREADY_VERIFIED) {
                verifyButton.setDisable(true);
                verifyButton.setText("● ACCOUNT VERIFIED • REDIRECTING");
                verifyButton.setStyle("-fx-background-color: linear-gradient(to right, #10B981, #059669); -fx-text-fill: #FFFFFF;");

                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(e -> {
                    showLoginCard("Account verified — you can now log in.", true);
                });
                pause.play();

            } else if (vResult.status() == UserDatabase.VerificationStatus.EXPIRED) {
                banner.setStyle("-fx-background-color: rgba(245, 158, 11, 0.14); -fx-border-color: rgba(245, 158, 11, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("!");
                bannerIcon.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText("Verification code has expired. Please click 'Resend Verification Code' for a new code.");
                bannerText.setStyle("-fx-text-fill: #FDE68A; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);

            } else {
                banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.14); -fx-border-color: rgba(239, 68, 68, 0.45); -fx-border-radius: 6px; -fx-background-radius: 6px;");
                bannerIcon.setText("⚠");
                bannerIcon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                bannerText.setText(vResult.message() != null ? vResult.message() : "Invalid verification code. Please retry.");
                bannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");
                banner.setVisible(true);
                banner.setManaged(true);
                shakeNode(card);
            }
        };

        verifyButton.setOnAction(e -> doVerify.run());
        codeField.setOnAction(e -> doVerify.run());

        card.getChildren().addAll(
                brandBox,
                headerRow,
                noticeLabel,
                emailBadge,
                codeLabel,
                codeField,
                resendRow,
                banner,
                verifyButton,
                navRow,
                createFooter()
        );

        setCard(card);
        Platform.runLater(codeField::requestFocus);
    }

    private void startCooldown(Button resendBtn) {
        lastResendTime = System.currentTimeMillis();
        resendBtn.setDisable(true);

        if (cooldownTimeline != null) {
            cooldownTimeline.stop();
        }

        cooldownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long elapsed = (System.currentTimeMillis() - lastResendTime) / 1000L;
            long remaining = COOLDOWN_SECONDS - elapsed;
            if (remaining > 0) {
                resendBtn.setText("RESEND CODE (" + remaining + "s)");
                resendBtn.setDisable(true);
            } else {
                resendBtn.setText("RESEND VERIFICATION CODE");
                resendBtn.setDisable(false);
                if (cooldownTimeline != null) {
                    cooldownTimeline.stop();
                }
            }
        }));
        cooldownTimeline.setCycleCount(Timeline.INDEFINITE);
        cooldownTimeline.play();
        resendBtn.setText("RESEND CODE (" + COOLDOWN_SECONDS + "s)");
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    private static class PasswordInputComponent {
        final VBox container;
        final PasswordField passwordField;
        final TextField revealedField;
        final Button toggleBtn;

        PasswordInputComponent(String labelText, String promptText) {
            Label label = new Label(labelText);
            label.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

            passwordField = new PasswordField();
            passwordField.setPromptText(promptText);
            passwordField.getStyleClass().add("cyber-input");
            passwordField.setPrefHeight(40);
            passwordField.setStyle("-fx-padding: 8 44 8 14;");

            revealedField = new TextField();
            revealedField.setPromptText(promptText);
            revealedField.getStyleClass().add("cyber-input");
            revealedField.setPrefHeight(40);
            revealedField.setStyle("-fx-padding: 8 44 8 14;");
            revealedField.setVisible(false);
            revealedField.setManaged(false);

            revealedField.textProperty().bindBidirectional(passwordField.textProperty());

            toggleBtn = new Button("SHOW");
            toggleBtn.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-text-fill: #64748B; " +
                    "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 6 10 6 10;"
            );
            toggleBtn.setTooltip(new Tooltip("Toggle Key Visibility"));

            toggleBtn.setOnAction(e -> {
                if (passwordField.isVisible()) {
                    passwordField.setVisible(false);
                    passwordField.setManaged(false);
                    revealedField.setVisible(true);
                    revealedField.setManaged(true);
                    toggleBtn.setText("HIDE");
                    toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
                    revealedField.requestFocus();
                    revealedField.positionCaret(revealedField.getText().length());
                } else {
                    revealedField.setVisible(false);
                    revealedField.setManaged(false);
                    passwordField.setVisible(true);
                    passwordField.setManaged(true);
                    toggleBtn.setText("SHOW");
                    toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
                    passwordField.requestFocus();
                    passwordField.positionCaret(passwordField.getText().length());
                }
            });

            StackPane passInputStack = new StackPane(passwordField, revealedField, toggleBtn);
            StackPane.setAlignment(toggleBtn, Pos.CENTER_RIGHT);
            StackPane.setMargin(toggleBtn, new Insets(0, 4, 0, 0));

            container = new VBox(4, label, passInputStack);
        }

        String getText() {
            return passwordField.getText() != null ? passwordField.getText() : "";
        }

        void setOnAction(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
            passwordField.setOnAction(handler);
            revealedField.setOnAction(handler);
        }
    }
}
