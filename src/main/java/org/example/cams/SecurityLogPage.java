package org.example.cams;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class SecurityLogPage extends StackPane {

    private final VBox logContainer;

    public SecurityLogPage(CamouShield app) {
        this.setStyle("-fx-background-color: #f4f6f8;");

        // --- Background Deco ---
        Circle c1 = new Circle(200, Color.web("#4a90e2", 0.05));
        c1.setTranslateX(400); c1.setTranslateY(-300);
        this.getChildren().add(c1);

        // --- Header ---
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-font-size: 14px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> app.showHomePage());

        Label title = new Label("Security Audit Log");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = new Button("Clear History");
        clearBtn.setStyle("-fx-background-color: white; -fx-text-fill: #d32f2f; -fx-border-color: #d32f2f; -fx-border-radius: 4; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            LogManager.clearLogs();
            refreshLogs();
        });

        HBox topBar = new HBox(15, backBtn, title, spacer, clearBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 40, 20, 40));

        // --- Log List Container ---
        logContainer = new VBox(10);
        logContainer.setPadding(new Insets(20));
        logContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(logContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // --- Layout ---
        BorderPane layout = new BorderPane();
        layout.setTop(topBar);
        layout.setCenter(scrollPane);

        this.getChildren().add(layout);

        refreshLogs();
    }

    private void refreshLogs() {
        logContainer.getChildren().clear();

        if (LogManager.getLogs().isEmpty()) {
            Label empty = new Label("No security events recorded.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
            logContainer.getChildren().add(empty);
            return;
        }

        for (LogManager.LogEntry entry : LogManager.getLogs()) {
            logContainer.getChildren().add(createLogCard(entry));
        }
    }

    private HBox createLogCard(LogManager.LogEntry entry) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // Status Indicator
        Circle statusDot = new Circle(6);
        switch (entry.severity) {
            case CRITICAL: statusDot.setFill(Color.RED); break;
            case WARNING:  statusDot.setFill(Color.ORANGE); break;
            case SUCCESS:  statusDot.setFill(Color.GREEN); break;
            default:       statusDot.setFill(Color.GREY); break;
        }

        // Time
        Label timeLbl = new Label(entry.timestamp);
        timeLbl.setMinWidth(140);
        timeLbl.setStyle("-fx-text-fill: #777; -fx-font-family: 'Monospaced';");

        // Content
        VBox content = new VBox(4);
        Label titleLbl = new Label(entry.title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");

        Label detailLbl = new Label(entry.details);
        detailLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 13px;");
        content.getChildren().addAll(titleLbl, detailLbl);

        card.getChildren().addAll(statusDot, timeLbl, content);
        return card;
    }
}