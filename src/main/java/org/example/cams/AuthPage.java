package org.example.cams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class AuthPage extends StackPane {

    private final TextField emailField, emailFieldReg;
    private final PasswordField passField, passFieldReg;

    // NEW: Registration Fields
    private final ComboBox<String> questionBox;
    private final TextField answerField;

    private final Label messageLabel;
    private final VBox loginBox, registerBox;

    public AuthPage(CamouShield app, boolean showRegister) {

        this.getStyleClass().add("auth-root");
        this.setStyle("-fx-background-color: linear-gradient(to bottom right, #4a90e2, #a1c4fd);");

        Circle circle1 = new Circle(140, Color.web("#357ABD", 0.28));
        circle1.setTranslateX(-420);
        circle1.setTranslateY(-220);

        Circle circle2 = new Circle(90, Color.web("#ffffff", 0.18));
        circle2.setTranslateX(380);
        circle2.setTranslateY(240);

        Button backButton = new Button("← Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> app.showLandingPage());

        HBox topBar = new HBox(backButton);
        topBar.setAlignment(Pos.TOP_LEFT);
        topBar.setPadding(new Insets(20, 30, 0, 30));

        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #d93025; -fx-font-size: 13px;");
        messageLabel.setVisible(false);

        // ---------- LOGIN FORM ----------
        Label loginTitle = new Label("Login");
        loginTitle.getStyleClass().add("form-title");

        Label loginSub = new Label("Sign in to your CamouShield account");
        loginSub.getStyleClass().add("form-subtitle");

        emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.getStyleClass().add("text-input");

        passField = new PasswordField();
        passField.setPromptText("Password");
        passField.getStyleClass().add("text-input");

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setOnAction(e -> handleLogin(app));

        // NEW: Forgot Password Link
        Hyperlink forgotLink = new Hyperlink("Forgot Password?");
        forgotLink.setStyle("-fx-text-fill: #1554c3; -fx-font-size: 12px;");
        forgotLink.setOnAction(e -> showForgotPasswordDialog());

        Hyperlink toRegLink = new Hyperlink("Need an account? Register");
        toRegLink.getStyleClass().add("form-link");
        toRegLink.setOnAction(e -> showRegister(true));

        VBox loginInner = new VBox(12, emailField, passField, loginBtn, forgotLink, messageLabel, toRegLink);
        loginInner.setAlignment(Pos.CENTER);
        loginInner.setMaxWidth(320);

        VBox loginCard = new VBox(10, loginTitle, loginSub, loginInner);
        loginCard.setAlignment(Pos.CENTER_LEFT);
        loginCard.getStyleClass().add("form-container");

        // ---------- REGISTER FORM ----------
        Label regTitle = new Label("Create account");
        regTitle.getStyleClass().add("form-title");

        Label regSub = new Label("Start securing your activity with CamouShield");
        regSub.getStyleClass().add("form-subtitle");

        emailFieldReg = new TextField();
        emailFieldReg.setPromptText("Email");
        emailFieldReg.getStyleClass().add("text-input");

        passFieldReg = new PasswordField();
        passFieldReg.setPromptText("Password");
        passFieldReg.getStyleClass().add("text-input");

        // NEW: Security Questions
        questionBox = new ComboBox<>();
        questionBox.getItems().addAll(
                "What was the name of your first pet?",
                "What is your mother's maiden name?",
                "What city were you born in?",
                "What is your favorite book?"
        );
        questionBox.setPromptText("Select Security Question");
        questionBox.setMaxWidth(Double.MAX_VALUE);
        questionBox.getStyleClass().add("text-input");

        answerField = new TextField();
        answerField.setPromptText("Security Answer");
        answerField.getStyleClass().add("text-input");

        Button regBtn = new Button("Register");
        regBtn.getStyleClass().add("btn-primary");
        regBtn.setOnAction(e -> handleRegister(app));

        Hyperlink toLoginLink = new Hyperlink("Already have an account? Login");
        toLoginLink.getStyleClass().add("form-link");
        toLoginLink.setOnAction(e -> showRegister(false));

        VBox registerInner = new VBox(12, emailFieldReg, passFieldReg, questionBox, answerField, regBtn, toLoginLink);
        registerInner.setAlignment(Pos.CENTER);
        registerInner.setMaxWidth(320);

        VBox registerCard = new VBox(10, regTitle, regSub, registerInner);
        registerCard.setAlignment(Pos.CENTER_LEFT);
        registerCard.getStyleClass().add("form-container");

        loginBox = loginCard;
        registerBox = registerCard;

        loginBox.setVisible(!showRegister);
        registerBox.setVisible(showRegister);

        StackPane centerStack = new StackPane(loginBox, registerBox);
        centerStack.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);
        root.setPadding(new Insets(40));

        this.getChildren().addAll(circle1, circle2, root);
    }

    private void showRegister(boolean show) {
        loginBox.setVisible(!show);
        registerBox.setVisible(show);
        messageLabel.setVisible(false);
    }

    private void handleLogin(CamouShield app) {
        String email = emailField.getText();
        String pass = passField.getText();
        messageLabel.setVisible(false);

        new Thread(() -> {
            try {
                String response = FirebaseService.signIn(email, pass);
                parseAndSetSession(response, email);
                Platform.runLater(app::showHomePage);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageLabel.setText(e.getMessage().replace("Firebase Error:", "").trim());
                    messageLabel.setVisible(true);
                });
            }
        }).start();
    }

    private void handleRegister(CamouShield app) {
        String email = emailFieldReg.getText();
        String pass = passFieldReg.getText();
        String question = questionBox.getValue();
        String answer = answerField.getText();

        if (question == null || answer.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please complete the security question.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        new Thread(() -> {
            try {
                String response = FirebaseService.signUp(email, pass);
                // Extract UID to save security question
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                String localId = root.path("localId").asText();

                // SAVE Q&A TO FIRESTORE
                FirestoreService.saveSecurityQuestion(localId, email, question, answer);

                parseAndSetSession(response, email);
                Platform.runLater(app::showHomePage);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String msg = e.getMessage().replace("Firebase Error:", "").trim();
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
                    alert.setHeaderText("Registration failed");
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void showForgotPasswordDialog() {
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Forgot Password");
        emailDialog.setHeaderText("Account Recovery");
        emailDialog.setContentText("Enter your email:");

        emailDialog.showAndWait().ifPresent(email -> {
            if (email.trim().isEmpty()) return;

            // Try to fetch security question
            new Thread(() -> {
                FirestoreService.SecurityData data = FirestoreService.getSecurityDataByEmail(email);

                Platform.runLater(() -> {
                    if (data == null) {
                        // User not found or no question set. Fallback to just sending email?
                        // For security, we might just say "If account exists..."
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Recovery");
                        alert.setHeaderText("User verify failed");
                        alert.setContentText("We could not find security details for this email. Check your spelling or contact admin.");
                        alert.show();
                    } else {
                        askSecurityQuestion(data, email);
                    }
                });
            }).start();
        });
    }

    private void askSecurityQuestion(FirestoreService.SecurityData data, String email) {
        TextInputDialog qDialog = new TextInputDialog();
        qDialog.setTitle("Security Check");
        qDialog.setHeaderText(data.question);
        qDialog.setContentText("Answer:");

        qDialog.showAndWait().ifPresent(answer -> {
            if (answer.trim().toLowerCase().equals(data.answer)) {
                // Correct! Send Reset Email
                new Thread(() -> {
                    try {
                        FirebaseService.sendPasswordResetEmail(email);
                        Platform.runLater(() -> {
                            Alert info = new Alert(Alert.AlertType.INFORMATION);
                            info.setTitle("Success");
                            info.setHeaderText("Identity Verified");
                            info.setContentText("A password reset link has been sent to " + email);
                            info.show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR, "Incorrect answer.", ButtonType.OK);
                error.show();
            }
        });
    }

    private void parseAndSetSession(String jsonResponse, String email) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            String idToken = root.path("idToken").asText();
            String localId = root.path("localId").asText();
            String refreshToken = root.path("refreshToken").asText();

            AuthState.login(email, idToken, refreshToken, localId);
            SessionManager.saveSession(email, idToken, refreshToken, localId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}