package org.example.cams;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Path;

public class ReAuthDialog {

    private static boolean isOpen = false;
    private static Stage dialogStage;
    private static Label errorLabel;
    private static Stage securityQuestionStage; // Sub-dialog

    public static void show(Stage owner, Path dataPath, String dataType) {
        if (isOpen) return;
        isOpen = true;

        LogManager.addLog(
                "Security Lockout Triggered",
                "Biometric mismatch detected in " + dataType + " patterns.",
                LogManager.Severity.CRITICAL
        );

        ListenersHolder.setPaused(true);

        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setAlwaysOnTop(true);

        Label title = new Label("SYSTEM LOCKED");
        title.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-font-size: 28px; -fx-font-family: 'Arial Black';");

        Label msg = new Label("Biometric mismatch detected.\nFor security, your screen has been temporarily locked.\nPlease verify your password to resume.");
        msg.setStyle("-fx-text-alignment: center; -fx-font-size: 16px; -fx-text-fill: #333;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setMaxWidth(300);
        passField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        Button verifyBtn = new Button("Unlock System");
        verifyBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");

        // NEW: Forgot Password / Security Question Option
        Hyperlink forgotLink = new Hyperlink("Forgot Password? Use Security Question");
        forgotLink.setStyle("-fx-text-fill: #d32f2f; -fx-underline: true;");
        forgotLink.setOnAction(e -> launchSecurityQuestionFlow(dataPath, dataType));

        verifyBtn.setOnAction(e -> attemptUnlock(passField.getText(), dataPath, dataType));
        passField.setOnAction(e -> attemptUnlock(passField.getText(), dataPath, dataType));

        dialogStage.setOnCloseRequest(e -> e.consume());

        VBox root = new VBox(20, title, msg, passField, verifyBtn, forgotLink, errorLabel);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-border-color: #d32f2f; -fx-border-width: 8px;");

        Scene scene = new Scene(root);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) event.consume();
        });

        dialogStage.setScene(scene);
        dialogStage.setFullScreen(true);
        dialogStage.setFullScreenExitHint("");
        dialogStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        dialogStage.show();

        Platform.runLater(passField::requestFocus);
    }

    private static void attemptUnlock(String inputPass, Path dataPath, String dataType) {
        if (inputPass == null || inputPass.isEmpty()) return;

        errorLabel.setText("Verifying...");
        String email = AuthState.get().getEmail();

        new Thread(() -> {
            try {
                FirebaseService.signIn(email, inputPass);
                unlockSuccess(dataPath, dataType, "Password Verified");
            } catch (Exception ex) {
                Platform.runLater(() -> errorLabel.setText("Incorrect Password."));
            }
        }).start();
    }

    // NEW: Logic to handle Security Question Unlock
    private static void launchSecurityQuestionFlow(Path dataPath, String dataType) {
        String uid = AuthState.get().getLocalId();

        // Fetch Question
        new Thread(() -> {
            FirestoreService.SecurityData data = FirestoreService.getSecurityDataByUid(uid);
            Platform.runLater(() -> {
                if (data == null) {
                    errorLabel.setText("No security question set for this user.");
                } else {
                    showSecurityQuestionInput(data, dataPath, dataType);
                }
            });
        }).start();
    }

    private static void showSecurityQuestionInput(FirestoreService.SecurityData data, Path dataPath, String dataType) {
        // We create a simpler input dialog ON TOP of the lock screen
        TextInputDialog qDialog = new TextInputDialog();
        qDialog.initOwner(dialogStage); // Child of lock screen
        qDialog.initModality(Modality.APPLICATION_MODAL);
        qDialog.setTitle("Security Verification");
        qDialog.setHeaderText(data.question);
        qDialog.setContentText("Answer:");

        qDialog.showAndWait().ifPresent(answer -> {
            if (answer.trim().toLowerCase().equals(data.answer)) {
                unlockSuccess(dataPath, dataType, "Security Question Verified");
            } else {
                errorLabel.setText("Incorrect Security Answer.");
            }
        });
    }

    private static void unlockSuccess(Path dataPath, String dataType, String method) {
        Platform.runLater(() -> {
            LogManager.addLog(
                    "System Unlocked",
                    method + ". Monitoring resumed.",
                    LogManager.Severity.SUCCESS
            );

            System.out.println(method + ". Unlocking...");
            dialogStage.close();
            isOpen = false;
            ListenersHolder.setPaused(false);
            ProtectionManager.forceTrain(dataPath, dataType);
        });
    }
}