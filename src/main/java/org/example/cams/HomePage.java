package org.example.cams;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class HomePage extends StackPane {

    private Label statusLabel;
    private ProgressBar progressBar;
    private Label progressText;
    private ToggleButton startupToggle;

    public HomePage() {
        this.getStyleClass().add("home-root");

        // --- Background Shapes ---
        Circle softCircle = new Circle(140, Color.web("#ffffff", 0.1));
        softCircle.setTranslateX(-350);
        softCircle.setTranslateY(250);

        Rectangle diagonal = new Rectangle(350, 200, Color.web("#ffffff", 0.08));
        diagonal.setArcWidth(40);
        diagonal.setArcHeight(40);
        diagonal.setRotate(-12);
        diagonal.setTranslateX(450);
        diagonal.setTranslateY(-200);

        // --- USER INFO ---
        String email = (AuthState.get() != null) ?
                AuthState.get().getEmail() : "user@example.com";
        String displayName = formatDisplayName(email);


        // --- TOP SECTION ---
        Label greetingLabel = new Label("Welcome back,");
        greetingLabel.getStyleClass().add("user-greeting-label");

        Label userLabel = new Label(displayName);
        userLabel.getStyleClass().add("user-name-label");

        VBox greetingBox = new VBox(3, greetingLabel, userLabel);

        Button logsBtn = new Button("Logs");
        logsBtn.getStyleClass().add("profile-btn");
        logsBtn.setOnAction(e -> CamouShield.getInstance().showSecurityLogPage());

        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("profile-btn");
        profileBtn.setOnAction(e-> CamouShield.getInstance().showProfilePage());

        HBox topBar = new HBox(15, greetingBox, new Region(), logsBtn, profileBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.getStyleClass().add("app-topbar");


        // --- MIDDLE SECTION (Status) ---
        Label sectionTitle = new Label("System Status");
        sectionTitle.getStyleClass().add("section-title");

        Label statusTitle = new Label("Current State");
        statusTitle.getStyleClass().add("status-title-label");

        statusLabel = new Label("Idle");
        statusLabel.getStyleClass().add("main-status-label");

        VBox statusBox = new VBox(10, statusTitle, statusLabel);
        statusBox.getStyleClass().add("glass-box");
        statusBox.setAlignment(Pos.CENTER);

        // --- Controls ---
        Button startBtn = new Button("Start Protection");
        startBtn.getStyleClass().addAll("btn", "btn-primary");

        Button stopBtn = new Button("Stop Monitoring");
        stopBtn.getStyleClass().addAll("btn", "btn-secondary-danger");
        stopBtn.setDisable(true);

        // NEW: Startup Toggle
        startupToggle = new ToggleButton("Run on Startup: OFF");
        startupToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-cursor: hand;");

        // Check current status
        boolean isCurrentlyStartup = StartupManager.isRunOnStartupEnabled();
        startupToggle.setSelected(isCurrentlyStartup);
        updateToggleStyle(isCurrentlyStartup);

        startupToggle.setOnAction(e -> {
            boolean selected = startupToggle.isSelected();
            StartupManager.setRunOnStartup(selected);
            updateToggleStyle(selected);
        });

        HBox controlRow = new HBox(30, startBtn, stopBtn);
        controlRow.setAlignment(Pos.CENTER);

        VBox controlsBox = new VBox(20, controlRow, startupToggle);
        controlsBox.setAlignment(Pos.CENTER);

        VBox middleSection = new VBox(20, sectionTitle, statusBox, controlsBox);
        middleSection.setPadding(new Insets(60, 0, 0, 0));
        middleSection.setAlignment(Pos.TOP_CENTER);


        // --- BOTTOM ACTIVITY BAR ---
        Label activityTitle = new Label("Current Activity");
        activityTitle.getStyleClass().add("activity-title");

        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("full-white-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        progressText = new Label("Not monitoring");
        progressText.getStyleClass().add("progress-text-label");

        VBox progressDisplay = new VBox(8, activityTitle, progressText, progressBar);
        progressDisplay.getStyleClass().add("progress-container");
        progressDisplay.setFillWidth(true);


        // --- LAYOUT ---
        BorderPane layout = new BorderPane();
        layout.setTop(topBar);
        layout.setCenter(middleSection);
        layout.setBottom(progressDisplay);

        this.getChildren().addAll(softCircle, diagonal, layout);


        // --- BUTTON ACTIONS ---
        startBtn.setOnAction(e -> {
            try {
                ListenersHolder.startListeners();
                statusLabel.setText("Monitoring");
                startBtn.setDisable(true);
                stopBtn.setDisable(false);
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                progressText.setText("Monitoring in progress...");
            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Error");
            }
        });

        stopBtn.setOnAction(e -> {
            ListenersHolder.stopListeners();
            statusLabel.setText("Idle");
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
            progressBar.setProgress(0);
            progressText.setText("Not monitoring");
        });
    }

    private void updateToggleStyle(boolean isSelected) {
        if (isSelected) {
            startupToggle.setText("Run on Startup: ON");
            startupToggle.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        } else {
            startupToggle.setText("Run on Startup: OFF");
            startupToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white;");
        }
    }


    private String formatDisplayName(String email) {
        String namePart = email.contains("@") ?
                email.substring(0, email.indexOf('@')) : email;

        namePart = namePart.replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();

        if (namePart.isEmpty()) return "User";

        String[] parts = namePart.split("\\s+");
        return Character.toUpperCase(parts[0].charAt(0))
                + parts[0].substring(1);
    }
}