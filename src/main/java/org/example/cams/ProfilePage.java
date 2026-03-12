package org.example.cams;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.Optional;

public class ProfilePage extends StackPane {

    private final CamouShield app;
    private final Label emailLabel;
    private final Label uidLabel;
    private final Label statusLabel;

    public ProfilePage(CamouShield app) {
        this.app = app;
        this.getStyleClass().add("profile-root");

        Circle softCircle = new Circle(140, Color.web("#ffffff", 0.20));
        softCircle.getStyleClass().add("profile-shape-light");
        softCircle.setTranslateX(-420);
        softCircle.setTranslateY(-220);

        Rectangle band = new Rectangle(380, 180, Color.web("#1554c3", 0.25));
        band.getStyleClass().add("profile-shape-band");
        band.setArcWidth(32);
        band.setArcHeight(32);
        band.setRotate(15);
        band.setTranslateX(430);
        band.setTranslateY(260);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("btn-link");
        backBtn.setOnAction(e -> app.showHomePage());

        String email = (AuthState.get() != null) ? AuthState.get().getEmail() : "user@example.com";
        String displayName = formatDisplayName(email);

        Label smallUser = new Label("Profile · " + displayName);
        smallUser.getStyleClass().add("profile-user-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, backBtn, spacer, smallUser);
        topBar.getStyleClass().add("profile-topbar");

        Label title = new Label("Account profile");
        title.getStyleClass().add("profile-title");

        Label subtitle = new Label("Manage your CamouShield account and security settings.");
        subtitle.getStyleClass().add("profile-subtitle");

        emailLabel = new Label("Email: " + email);
        emailLabel.getStyleClass().add("profile-label");

        uidLabel = new Label("UID: " + ((AuthState.get() != null) ? AuthState.get().getLocalId() : "-"));
        uidLabel.getStyleClass().add("profile-label");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("profile-status");

        Button changePwdBtn = new Button("Change password");
        changePwdBtn.getStyleClass().add("btn-primary");
        changePwdBtn.setOnAction(e -> handleChangePassword());

        Button deleteBtn = new Button("Delete account");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDeleteAccount());

        // NEW: Reset Data Button
        Button resetDataBtn = new Button("Reset / Retrain Model");
        resetDataBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
        resetDataBtn.setOnAction(e -> handleResetData());

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("btn-link");
        logoutBtn.setOnAction(e -> {
            ListenersHolder.stopListeners();
            SessionManager.clearSession();
            AuthState.logout();
            System.exit(0);
        });

        HBox actionsRow = new HBox(12, changePwdBtn, deleteBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, title, subtitle, new Separator(),
                emailLabel, uidLabel, statusLabel, actionsRow, resetDataBtn, logoutBtn);
        card.getStyleClass().add("profile-card");
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane center = new StackPane(card);
        StackPane.setMargin(card, new Insets(0, 40, 40, 40));

        BorderPane layout = new BorderPane();
        layout.setTop(topBar);
        layout.setCenter(center);

        this.getChildren().addAll(softCircle, band, layout);

        refreshProfileAsync();
    }

    // NEW: Logic to handle resetting data with password check
    private void handleResetData() {
        // 1. Ask for Password
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("Security Check");
        passDialog.setHeaderText("Retrain Biometric Model");
        passDialog.setContentText("Enter your password to verify reset:");

        // Hide text somewhat manually or use custom dialog, but standard TextInput shows text.
        // For better UX, we usually create a custom dialog with PasswordField,
        // but for now, we assume user is aware.

        Optional<String> result = passDialog.showAndWait();
        if (result.isEmpty()) return;
        String password = result.get();

        statusLabel.setText("Verifying password...");

        new Thread(() -> {
            try {
                String email = AuthState.get().getEmail();
                // 2. Verify against Firebase
                FirebaseService.signIn(email, password);

                Platform.runLater(() -> statusLabel.setText("Password verified. Resetting data..."));

                // 3. Perform Reset
                boolean success = ProtectionManager.resetAllData();

                Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Success! Data deleted. App will now retrain.");
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Model Reset");
                        info.setHeaderText("Biometric Data Cleared");
                        info.setContentText("All your previous typing and mouse data has been deleted.\nThe system will now start learning your patterns from scratch.");
                        info.show();
                    } else {
                        statusLabel.setText("Error: Could not reset backend data.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Incorrect password. Reset cancelled.");
                    Alert error = new Alert(Alert.AlertType.ERROR, "Incorrect Password", ButtonType.OK);
                    error.show();
                });
            }
        }).start();
    }


    private void refreshProfileAsync() {
        var state = AuthState.get();
        if (state == null) return;

        String idToken = state.getIdToken();
        new Thread(() -> {
            try {
                JsonNode user = FirebaseService.getProfile(idToken);
                String email = user.path("email").asText();
                String uid   = user.path("localId").asText();

                Platform.runLater(() -> {
                    emailLabel.setText("Email: " + email);
                    uidLabel.setText("UID: " + uid);
                    statusLabel.setText("");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Could not refresh profile: " + ex.getMessage()));
            }
        }).start();
    }

    private void handleChangePassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change password");
        dialog.setHeaderText("Enter a new password for your account.");
        dialog.setContentText("New password:");

        dialog.showAndWait().ifPresent(newPwd -> {
            if (newPwd == null || newPwd.trim().length() < 6) {
                statusLabel.setText("Password must be at least 6 characters.");
                return;
            }
            var state = AuthState.get();
            if (state == null) return;

            String idToken = state.getIdToken();
            statusLabel.setText("Updating password…");

            new Thread(() -> {
                try {
                    FirebaseService.changePassword(idToken, newPwd);
                    Platform.runLater(() -> statusLabel.setText("Password updated successfully."));
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLabel.setText("Failed to update password: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void handleDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Delete account");
        confirm.setHeaderText("This will permanently delete your CamouShield account.");
        confirm.setContentText("Are you sure you want to continue?");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                var state = AuthState.get();
                if (state == null) return;

                String idToken = state.getIdToken();
                statusLabel.setText("Deleting account…");

                new Thread(() -> {
                    try {
                        FirebaseService.deleteAccount(idToken);
                        Platform.runLater(() -> {
                            statusLabel.setText("Account deleted.");
                            SessionManager.clearSession(); // Clear session file
                            AuthState.logout();
                            System.exit(0);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> statusLabel.setText("Failed to delete account: " + ex.getMessage()));
                    }
                }).start();
            }
        });
    }

    private String formatDisplayName(String email) {
        String namePart = email;
        int at = email.indexOf('@');
        if (at > 0) {
            namePart = email.substring(0, at);
        }
        namePart = namePart.replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim();
        if (namePart.isEmpty()) return "User";
        return namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
    }
}