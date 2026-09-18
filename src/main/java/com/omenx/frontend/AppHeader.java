package com.omenx.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// =========================================================
// OMEN-X REUSABLE APP HEADER & PANEL HEADER COMPONENT
// Follows the OSIRIS standardized panel header pattern:
// Icon + Uppercase Title (letter-spaced) + Live Status Dot
// + Secondary Stat + Expand / Action Glyphs
// =========================================================
public class AppHeader extends HBox {

    public enum StatusType {
        READY,
        ONLINE,
        SCANNING,
        ERROR,
        WARNING
    }

    private final Label iconLabel;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final Label statusPill;
    private final Button backButton;
    private final Label expandIcon;
    private final HBox rightBox;

    public AppHeader(String iconBracket, String title, String subtitle, Runnable backAction) {
        this(iconBracket, title, subtitle, backAction, true);
    }

    public AppHeader(String iconBracket, String title, String subtitle, Runnable backAction, boolean showBackButton) {
        super(16);
        this.getStyleClass().add("app-header");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setFillHeight(true);

        // ── Left Title Block ──
        iconLabel = new Label(iconBracket != null && !iconBracket.isBlank() ? iconBracket + "  " : "");
        iconLabel.getStyleClass().add("header-icon");
        iconLabel.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("header-title");
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(iconLabel, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label(subtitle != null ? subtitle : "");
        subtitleLabel.getStyleClass().add("header-subtitle");

        VBox titleBlock = new VBox(2, titleRow, subtitleLabel);
        titleBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        // ── Right Action Block ──
        statusPill = new Label("● READY");
        statusPill.getStyleClass().addAll("status-pill", "status-pill-ready");

        backButton = new Button("←  Dashboard");
        backButton.getStyleClass().add("btn-header-back");
        if (backAction != null) {
            backButton.setOnAction(e -> backAction.run());
        }
        backButton.setVisible(showBackButton);
        backButton.setManaged(showBackButton);

        expandIcon = new Label("⤢");
        expandIcon.getStyleClass().add("panel-action-icon");
        expandIcon.setTooltip(new Tooltip("Active intelligence workspace"));

        rightBox = new HBox(12, statusPill, backButton, expandIcon);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(titleBlock, rightBox);
    }

    // ── Factory: Reusable Inner Panel Header Pattern ──
    public static HBox createPanelHeader(String iconBracket, String title, String statusText, StatusType statusType, String secondaryStat) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-border-color: transparent transparent rgba(34, 211, 238, 0.16) transparent; -fx-border-width: 0 0 1 0;");

        Label icon = new Label(iconBracket != null ? iconBracket + " " : "");
        icon.setStyle("-fx-text-fill: #22D3EE; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label titleLbl = new Label(title != null ? title.toUpperCase() : "");
        titleLbl.setStyle("-fx-text-fill: #F1F5F9; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: 800; -fx-letter-spacing: 1.5px;");

        HBox left = new HBox(4, icon, titleLbl);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        header.getChildren().add(left);

        if (statusText != null && !statusText.isBlank()) {
            Label dot = new Label("● " + statusText.toUpperCase());
            dot.getStyleClass().add("badge");
            if (statusType == StatusType.ERROR) {
                dot.getStyleClass().add("badge-red");
            } else if (statusType == StatusType.WARNING) {
                dot.getStyleClass().add("badge-amber");
            } else if (statusType == StatusType.SCANNING) {
                dot.getStyleClass().add("badge-cyan");
            } else {
                dot.getStyleClass().add("badge-green");
            }
            header.getChildren().add(dot);
        }

        if (secondaryStat != null && !secondaryStat.isBlank()) {
            Label statBadge = new Label(secondaryStat.toUpperCase());
            statBadge.getStyleClass().addAll("badge", "badge-cyan");
            header.getChildren().add(statBadge);
        }

        Label expand = new Label("⤢");
        expand.getStyleClass().add("panel-action-icon");
        expand.setTooltip(new Tooltip("Panel view options"));

        Label chevron = new Label("⌄");
        chevron.getStyleClass().add("panel-action-icon");
        chevron.setTooltip(new Tooltip("Collapse / Expand"));

        header.getChildren().addAll(expand, chevron);
        return header;
    }

    // ── Status Management ──
    public void setStatus(StatusType type, String text) {
        statusPill.getStyleClass().removeAll(
                "status-pill-ready",
                "status-pill-online",
                "status-pill-scanning",
                "status-pill-error",
                "status-pill-warning"
        );

        String prefix = switch (type) {
            case READY, ONLINE -> "● ";
            case SCANNING -> "◈ ";
            case ERROR -> "✖ ";
            case WARNING -> "▲ ";
        };

        statusPill.setText(prefix + (text != null ? text.toUpperCase() : type.name()));

        switch (type) {
            case READY, ONLINE -> statusPill.getStyleClass().add("status-pill-ready");
            case SCANNING -> statusPill.getStyleClass().add("status-pill-scanning");
            case ERROR -> statusPill.getStyleClass().add("status-pill-error");
            case WARNING -> statusPill.getStyleClass().add("status-pill-warning");
        }
    }

    public void addExtraPill(Label extraBadge) {
        if (extraBadge != null) {
            int insertIndex = Math.max(0, rightBox.getChildren().indexOf(statusPill));
            rightBox.getChildren().add(insertIndex, extraBadge);
        }
    }

    public void setHeaderTitle(String title) {
        titleLabel.setText(title);
    }

    public void setHeaderSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public void setHeaderIcon(String iconBracket) {
        iconLabel.setText(iconBracket != null && !iconBracket.isBlank() ? iconBracket + "  " : "");
    }

    public void setBackButtonVisible(boolean visible) {
        backButton.setVisible(visible);
        backButton.setManaged(visible);
    }

    public Button getBackButton() {
        return backButton;
    }

    public Label getStatusPill() {
        return statusPill;
    }
}
