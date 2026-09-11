package com.omenx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// =========================================================
// OMEN-X REUSABLE APP HEADER COMPONENT
// Standardized across all 8 modules, Dashboard, and views
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

        rightBox = new HBox(12, statusPill, backButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(titleBlock, rightBox);
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
            // Insert before statusPill or backButton
            rightBox.getChildren().add(0, extraBadge);
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
