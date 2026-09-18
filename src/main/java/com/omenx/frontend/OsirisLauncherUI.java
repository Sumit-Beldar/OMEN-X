package com.omenx.frontend;

import com.omenx.backend.osiris.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

// =========================================================
// OSIRIS LAUNCHER UI
// Dark Glassmorphic Intelligence Dashboard Interface
// Staged Progress, Live Telemetry Console & API Settings Modal
// =========================================================
public class OsirisLauncherUI implements OsirisProgressListener {

    private static final String CYAN   = "#22D3EE";
    private static final String GREEN  = "#10B981";
    private static final String RED    = "#EF4444";
    private static final String AMBER  = "#F59E0B";
    private static final String TEXT   = "#F1F5F9";
    private static final String MUTED  = "#64748B";

    private final OsirisConfig config;
    private final OsirisRuntimeManager runtimeManager;
    private final OsirisInstaller installer;
    private final OsirisProcessManager processManager;
    private final Runnable backToDashboardAction;

    private AppHeader appHeader;
    private Label statusBadge;
    private Label portBadge;

    private ProgressBar progressBar;
    private Label progressPercentLabel;
    private Label progressDetailLabel;
    private Label stageTitleLabel;

    private TextArea consoleTextArea;
    private CheckBox autoScrollCheck;

    private Button launchBtn;
    private Button openBrowserBtn;
    private Button stopBtn;
    private Button settingsBtn;
    private Button rebuildBtn;
    private Button retryBtn;

    private VBox errorBanner;
    private Label errorBannerText;

    private VBox mainView;

    public OsirisLauncherUI(
            OsirisConfig config,
            OsirisRuntimeManager runtimeManager,
            OsirisInstaller installer,
            OsirisProcessManager processManager,
            Runnable backToDashboardAction
    ) {
        this.config = config;
        this.runtimeManager = runtimeManager;
        this.installer = installer;
        this.processManager = processManager;
        this.backToDashboardAction = backToDashboardAction;
    }

    public VBox getView() {
        if (mainView == null) {
            mainView = buildView();
        }
        refreshStateDisplay();
        return mainView;
    }

    // =========================================================
    // BUILD MAIN VIEW
    // =========================================================
    private VBox buildView() {
        // ── Panel Header ──
        appHeader = new AppHeader(
                "[O]>",
                "OSIRIS GLOBAL OSINT ENGINE",
                "Autonomous Next.js 3D threat, satellite, flight & maritime reconnaissance platform",
                backToDashboardAction,
                false
        );

        statusBadge = new Label("CHECKING...");
        statusBadge.getStyleClass().addAll("badge", "badge-cyan");

        portBadge = new Label("PORT: " + config.getPort());
        portBadge.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-background-color: rgba(255,255,255,0.06); -fx-padding: 4 8 4 8; -fx-background-radius: 4px;");

        appHeader.addExtraPill(statusBadge);
        appHeader.addExtraPill(portBadge);

        // ── Error Banner (Hidden by default) ──
        errorBanner = new VBox(6);
        errorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.12); -fx-border-color: rgba(239, 68, 68, 0.50); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 12 16 12 16;");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);

        Label errTitle = new Label("⚠ SYSTEM EXECUTION ERROR");
        errTitle.setStyle("-fx-text-fill: #EF4444; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");

        errorBannerText = new Label();
        errorBannerText.setStyle("-fx-text-fill: #FCA5A5; -fx-font-size: 11px;");
        errorBannerText.setWrapText(true);

        errorBanner.getChildren().addAll(errTitle, errorBannerText);

        // ── Progress & Stage Card ──
        VBox progressCard = createProgressCard();

        // ── Action Toolbar ──
        HBox toolbar = createToolbar();

        // ── Telemetry Console Log Panel ──
        VBox consoleCard = createConsoleCard();

        // Assemble Layout
        VBox content = new VBox(16, appHeader, errorBanner, progressCard, toolbar, consoleCard);
        content.setPadding(new Insets(20, 26, 24, 26));
        content.setStyle("-fx-background-color: #060810;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color: #060810;");
        return wrapper;
    }

    // =========================================================
    // PROGRESS CARD WITH 5 STAGES
    // =========================================================
    private VBox createProgressCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-panel");
        card.setPadding(new Insets(18, 20, 18, 20));

        // Header inside progress card
        stageTitleLabel = new Label("ENGINE STATUS: READY TO DEPLOY");
        stageTitleLabel.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        progressPercentLabel = new Label("0%");
        progressPercentLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox stageHeader = new HBox(stageTitleLabel, new Region(), progressPercentLabel);
        HBox.setHgrow(stageHeader.getChildren().get(1), Priority.ALWAYS);
        stageHeader.setAlignment(Pos.CENTER_LEFT);

        // Progress Bar
        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: #22D3EE;");

        // Progress Detail Subtitle
        progressDetailLabel = new Label("Click 'Launch OSIRIS' to boot local intelligence engine.");
        card.getChildren().addAll(stageHeader, progressBar, progressDetailLabel);
        return card;
    }

    // =========================================================
    // ACTION TOOLBAR
    // =========================================================
    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        launchBtn = new Button("▶  LAUNCH OSIRIS");
        launchBtn.getStyleClass().add("btn-primary");
        launchBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10 22 10 22;");
        launchBtn.setOnAction(e -> triggerLaunch());

        openBrowserBtn = new Button("⤤  OPEN IN BROWSER");
        openBrowserBtn.getStyleClass().add("btn-ghost");
        openBrowserBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10 18 10 18;");
        openBrowserBtn.setOnAction(e -> processManager.openBrowser("http://localhost:" + processManager.getActivePort(), this));

        stopBtn = new Button("■  STOP SERVER");
        stopBtn.getStyleClass().add("btn-ghost-danger");
        stopBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10 18 10 18;");
        stopBtn.setOnAction(e -> triggerStop());

        settingsBtn = new Button("⚙  API KEYS & PORT");
        settingsBtn.getStyleClass().add("btn-ghost");
        settingsBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10 16 10 16;");
        settingsBtn.setOnAction(e -> openSettingsModal());

        rebuildBtn = new Button("↻  REINSTALL / RESET");
        rebuildBtn.getStyleClass().add("btn-ghost");
        rebuildBtn.setStyle("-fx-font-size: 11px; -fx-padding: 10 14 10 14; -fx-text-fill: #94A3B8;");
        rebuildBtn.setOnAction(e -> triggerReinstall());

        retryBtn = new Button("↻  RETRY");
        retryBtn.getStyleClass().add("btn-primary");
        retryBtn.setStyle("-fx-font-size: 12px; -fx-padding: 10 20 10 20; -fx-background-color: #EF4444; -fx-border-color: #F87171;");
        retryBtn.setOnAction(e -> triggerLaunch());
        retryBtn.setVisible(false);
        retryBtn.setManaged(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(launchBtn, openBrowserBtn, stopBtn, retryBtn, spacer, settingsBtn, rebuildBtn);
        return toolbar;
    }

    // =========================================================
    // TELEMETRY CONSOLE LOG CARD
    // =========================================================
    private VBox createConsoleCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-panel");
        card.setPadding(new Insets(14, 18, 16, 18));

        HBox consoleHeader = new HBox(12);
        consoleHeader.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("::> ENGINE TELEMETRY & BUILD STREAM");
        title.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        autoScrollCheck = new CheckBox("AUTOSCROLL");
        autoScrollCheck.setSelected(true);
        autoScrollCheck.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px;");

        Button copyBtn = new Button("COPY LOG");
        copyBtn.getStyleClass().add("btn-ghost");
        copyBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 10 4 10;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(consoleTextArea.getText());
            clipboard.setContent(content);
            copyBtn.setText("COPIED!");
            Platform.runLater(() -> {
                new Thread(() -> {
                    try { Thread.sleep(1200); } catch (Exception ignored) {}
                    Platform.runLater(() -> copyBtn.setText("COPY LOG"));
                }).start();
            });
        });

        Button clearBtn = new Button("CLEAR");
        clearBtn.getStyleClass().add("btn-ghost");
        clearBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 10 4 10;");
        clearBtn.setOnAction(e -> {
            consoleTextArea.clear();
            processManager.clearLogs();
        });

        consoleHeader.getChildren().addAll(title, spacer, autoScrollCheck, copyBtn, clearBtn);

        consoleTextArea = new TextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setWrapText(false);
        consoleTextArea.setPrefRowCount(14);
        consoleTextArea.setStyle(
                "-fx-control-inner-background: #030712; " +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #93C5FD; " +
                "-fx-background-color: transparent; " +
                "-fx-border-color: rgba(34, 211, 238, 0.20); " +
                "-fx-border-radius: 4px;"
        );

        // Pre-fill existing logs
        for (String l : processManager.getLogBuffer()) {
            consoleTextArea.appendText(l + "\n");
        }

        card.getChildren().addAll(consoleHeader, consoleTextArea);
        return card;
    }

    // =========================================================
    // TRIGGER ACTIONS
    // =========================================================
    private void triggerLaunch() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        retryBtn.setVisible(false);
        retryBtn.setManaged(false);

        processManager.launch(this);
    }

    private void triggerStop() {
        processManager.stop();
        refreshStateDisplay();
    }

    private void triggerReinstall() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Reinstall");
        confirm.setHeaderText("Reinstall OSIRIS Platform?");
        confirm.setContentText("This will wipe the current build cache and re-clone/re-install OSIRIS. Continue?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                errorBanner.setVisible(false);
                errorBanner.setManaged(false);
                retryBtn.setVisible(false);
                retryBtn.setManaged(false);

                new Thread(() -> {
                    try {
                        processManager.stopSync();
                        installer.cleanAndReinstall(this);
                        Platform.runLater(this::triggerLaunch);
                    } catch (Exception e) {
                        Platform.runLater(() -> onError("Reinstall failed: " + e.getMessage(), e));
                    }
                }).start();
            }
        });
    }

    // =========================================================
    // STATE DISPLAY REFRESH
    // =========================================================
    public void refreshStateDisplay() {
        Platform.runLater(() -> {
            OsirisState state = processManager.getCurrentState();
            portBadge.setText("PORT: " + processManager.getActivePort());

            switch (state) {
                case NOT_INSTALLED -> {
                    appHeader.setStatus(AppHeader.StatusType.READY, "NOT INSTALLED");
                    statusBadge.setText("SETUP REQUIRED");
                    statusBadge.setStyle("-fx-text-fill: #F59E0B; -fx-background-color: rgba(245, 158, 11, 0.15);");
                    launchBtn.setText("▶  INSTALL & LAUNCH OSIRIS");
                    launchBtn.setDisable(false);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(true);
                }
                case READY_CACHED -> {
                    appHeader.setStatus(AppHeader.StatusType.ONLINE, "READY (CACHED)");
                    statusBadge.setText("CACHED BUILD");
                    statusBadge.setStyle("-fx-text-fill: #22D3EE; -fx-background-color: rgba(34, 211, 238, 0.15);");
                    launchBtn.setText("▶  LAUNCH OSIRIS");
                    launchBtn.setDisable(false);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(true);
                }
                case INSTALLING, STARTING -> {
                    appHeader.setStatus(AppHeader.StatusType.SCANNING, state == OsirisState.INSTALLING ? "INSTALLING..." : "STARTING...");
                    statusBadge.setText(state == OsirisState.INSTALLING ? "COMPILING" : "BOOTING");
                    statusBadge.setStyle("-fx-text-fill: #22D3EE; -fx-background-color: rgba(34, 211, 238, 0.25);");
                    launchBtn.setDisable(true);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(false);
                }
                case RUNNING -> {
                    appHeader.setStatus(AppHeader.StatusType.ONLINE, "ONLINE");
                    statusBadge.setText("ACTIVE");
                    statusBadge.setStyle("-fx-text-fill: #10B981; -fx-background-color: rgba(16, 185, 129, 0.20);");
                    launchBtn.setText("●  RUNNING");
                    launchBtn.setDisable(true);
                    openBrowserBtn.setDisable(false);
                    stopBtn.setDisable(false);
                }
                case STOPPING -> {
                    appHeader.setStatus(AppHeader.StatusType.WARNING, "STOPPING...");
                    launchBtn.setDisable(true);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(true);
                }
                case STOPPED -> {
                    appHeader.setStatus(AppHeader.StatusType.READY, "OFFLINE");
                    statusBadge.setText("STOPPED");
                    statusBadge.setStyle("-fx-text-fill: #94A3B8; -fx-background-color: rgba(255, 255, 255, 0.08);");
                    launchBtn.setText("▶  LAUNCH OSIRIS");
                    launchBtn.setDisable(false);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(true);
                }
                case FAILED -> {
                    appHeader.setStatus(AppHeader.StatusType.ERROR, "ERROR");
                    statusBadge.setText("FAILED");
                    statusBadge.setStyle("-fx-text-fill: #EF4444; -fx-background-color: rgba(239, 68, 68, 0.20);");
                    launchBtn.setDisable(true);
                    retryBtn.setVisible(true);
                    retryBtn.setManaged(true);
                    openBrowserBtn.setDisable(true);
                    stopBtn.setDisable(true);
                }
            }
        });
    }

    // =========================================================
    // PROGRESS LISTENER CALLBACKS
    // =========================================================
    @Override
    public void onStageChanged(OsirisStage stage, String stageTitle, String stageDesc) {
        Platform.runLater(() -> {
            stageTitleLabel.setText("STAGE " + stage.getStepNumber() + ": " + stageTitle);
            progressDetailLabel.setText(stageDesc);
        });
    }

    @Override
    public void onProgress(double percent, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(percent);
            progressPercentLabel.setText((int) (percent * 100) + "%");
            if (message != null && !message.isBlank()) {
                progressDetailLabel.setText(message);
            }
        });
    }

    @Override
    public void onLog(String logLine, boolean isError) {
        Platform.runLater(() -> {
            if (consoleTextArea != null) {
                consoleTextArea.appendText(logLine + "\n");
                if (autoScrollCheck != null && autoScrollCheck.isSelected()) {
                    consoleTextArea.setScrollTop(Double.MAX_VALUE);
                }
            }
        });
    }

    @Override
    public void onStateChanged(OsirisState newState) {
        refreshStateDisplay();
    }

    @Override
    public void onSuccess(String targetUrl) {
        Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            progressPercentLabel.setText("100%");
            stageTitleLabel.setText("SYSTEM ONLINE • " + targetUrl);
            progressDetailLabel.setText("OSIRIS global intelligence dashboard is serving at " + targetUrl);
            refreshStateDisplay();
        });
    }

    @Override
    public void onError(String errorMessage, Throwable error) {
        Platform.runLater(() -> {
            errorBannerText.setText(errorMessage != null ? errorMessage : "An unexpected failure occurred.");
            errorBanner.setVisible(true);
            errorBanner.setManaged(true);
            stageTitleLabel.setText("EXECUTION FAILED");
            progressDetailLabel.setText("Inspect telemetry logs below for diagnostic output.");
            refreshStateDisplay();
        });
    }

    // =========================================================
    // SETTINGS MODAL DIALOG (API Keys & Port)
    // =========================================================
    private void openSettingsModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("OSIRIS Configuration & API Credentials");

        VBox layout = new VBox(14);
        layout.setStyle("-fx-background-color: #0A0E17; -fx-padding: 24; -fx-border-color: rgba(34, 211, 238, 0.3); -fx-border-width: 1px;");
        layout.setPrefWidth(520);

        Label header = new Label("// OSIRIS CREDENTIALS & PORT SETTINGS //");
        header.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label desc = new Label("Configure live tracking API keys and target port. Keys are stored locally in ~/.omenx/config/osiris.properties and synced to .env.local.");
        desc.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        desc.setWrapText(true);

        // Inputs
        TextField portField = createSettingField("Server Port", String.valueOf(config.getPort()), layout);
        TextField openSkyUserField = createSettingField("OpenSky Client ID (Flight Tracking)", config.getOpenskyClientId(), layout);
        PasswordField openSkySecretField = createSettingPasswordField("OpenSky Client Secret", config.getOpenskyClientSecret(), layout);
        TextField n2yoField = createSettingField("N2YO API Key (Satellite Orbit Feeds)", config.getN2yoApiKey(), layout);
        TextField firmsField = createSettingField("NASA FIRMS Key (Global Thermal/Fire)", config.getFirmsApiKey(), layout);
        TextField aisField = createSettingField("AIS Key (Maritime Vessel Positions)", config.getAisApiKey(), layout);

        // Buttons
        Button saveBtn = new Button("SAVE & APPLY");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                int p = Integer.parseInt(portField.getText().trim());
                config.setPort(p);
            } catch (Exception ignored) {
            }
            config.setOpenskyClientId(openSkyUserField.getText().trim());
            config.setOpenskyClientSecret(openSkySecretField.getText().trim());
            config.setN2yoApiKey(n2yoField.getText().trim());
            config.setFirmsApiKey(firmsField.getText().trim());
            config.setAisApiKey(aisField.getText().trim());
            config.save();

            portBadge.setText("PORT: " + config.getPort());
            dialog.close();
        });

        Button cancelBtn = new Button("CANCEL");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        layout.getChildren().addAll(header, desc, btnBox);

        Scene scene = new Scene(layout);
        try {
            if (getClass().getResource("/styles/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            }
        } catch (Exception ignored) {
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private TextField createSettingField(String labelText, String val, VBox parent) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold;");

        TextField tf = new TextField(val != null ? val : "");
        tf.setStyle("-fx-background-color: #060810; -fx-text-fill: #F1F5F9; -fx-border-color: rgba(34, 211, 238, 0.25); -fx-border-radius: 4px; -fx-padding: 6 10 6 10; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        VBox box = new VBox(4, lbl, tf);
        parent.getChildren().add(box);
        return tf;
    }

    private PasswordField createSettingPasswordField(String labelText, String val, VBox parent) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: bold;");

        PasswordField pf = new PasswordField();
        pf.setText(val != null ? val : "");
        pf.setStyle("-fx-background-color: #060810; -fx-text-fill: #F1F5F9; -fx-border-color: rgba(34, 211, 238, 0.25); -fx-border-radius: 4px; -fx-padding: 6 10 6 10; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 11px;");

        VBox box = new VBox(4, lbl, pf);
        parent.getChildren().add(box);
        return pf;
    }
}
