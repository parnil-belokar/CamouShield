package org.example.cams;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;

public class LandingPage extends StackPane {

    private final CamouShield app;

    public LandingPage(CamouShield app) {
        this.app = app;
        setupUI();
    }

    private void setupUI() {
        // Simple blue gradient background
        this.setStyle("-fx-background-color: linear-gradient(to bottom right, #4a90e2, #a1c4fd);");

        // Decorative shapes
        Circle circle1 = new Circle(150, Color.web("#357ABD", 0.3));
        circle1.setEffect(new DropShadow(40, Color.web("#357ABD", 0.4)));
        circle1.setTranslateX(-400);
        circle1.setTranslateY(-200);

        Circle circle2 = new Circle(80, Color.web("#ffffff", 0.15));
        circle2.setTranslateX(350);
        circle2.setTranslateY(250);

        getChildren().addAll(circle1, circle2);

        // Main content
        VBox contentBox = new VBox(24);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(0, 0, 100, 0));

        Label appName = new Label("CamouShield");
        appName.setStyle("-fx-font-size: 48px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label tagline = new Label("Invisible security that thinks like you");
        tagline.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255, 255, 255, 0.85);");

        HBox buttons = new HBox(30);
        buttons.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("landing-btn");
        loginBtn.setOnAction(e -> app.showAuthPage(false)); // show login

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().add("landing-btn");
        registerBtn.setOnAction(e -> app.showAuthPage(true)); // show register


        buttons.getChildren().addAll(loginBtn, registerBtn);

        contentBox.getChildren().addAll(appName, tagline, buttons);
        getChildren().add(contentBox);
    }
}
