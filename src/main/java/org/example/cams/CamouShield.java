package org.example.cams;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class CamouShield extends Application {

    private Stage primaryStage;
    private static CamouShield instance;
    private boolean isMonitoring = false;

    @Override
    public void start(Stage stage) {
        // 0. AUTO-START ML SERVER
        // This ensures the backend is ready before we try to login or verify.
        PythonManager.startServer();

        // 1. Prevent JavaFX from shutting down when the window is hidden
        Platform.setImplicitExit(false);

        // 2. Clean up stale data
        DataCleaner.cleanStaleData();

        this.primaryStage = stage;
        instance = this;

        ProtectionManager.setMainApp(this);

        primaryStage.setTitle("CamouShield - Active Protection");
        primaryStage.setResizable(false);

        // 3. Initialize System Tray
        setupSystemTray();

        // 4. Check for Auto-Login
        boolean isLoggedIn = SessionManager.tryAutoLogin();

        // 5. Handle "Background Start" vs "Normal Start"
        boolean launchInBackground = getParameters().getRaw().contains("--background");

        if (isLoggedIn) {
            System.out.println("Auto-login successful.");

            try {
                ListenersHolder.startListeners();
                isMonitoring = true;
                System.out.println("Background protection started.");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!launchInBackground) {
                showHomePage();
                primaryStage.show();
            }
        } else {
            showLandingPage();
            primaryStage.show();
        }

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            primaryStage.hide();
        });
    }

    // --- (Keep existing methods: setupSystemTray, showWindow, showPages...) ---

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;

        final SystemTray tray = SystemTray.getSystemTray();
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new java.awt.Color(53, 122, 189));
        g2.fillOval(0, 0, 16, 16);
        g2.dispose();

        TrayIcon trayIcon = new TrayIcon(image, "CamouShield - Protected");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> Platform.runLater(this::showWindow));

        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Open Dashboard");
        openItem.addActionListener(e -> Platform.runLater(this::showWindow));

        MenuItem exitItem = new MenuItem("Exit Completely");
        exitItem.addActionListener(e -> {
            ListenersHolder.stopListeners();
            PythonManager.stopServer(); // Stop Python on full exit
            SystemTray.getSystemTray().remove(trayIcon);
            Platform.exit();
            System.exit(0);
        });

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        try { tray.add(trayIcon); } catch (AWTException e) {}
    }

    public void showWindow() {
        if (primaryStage.isIconified()) primaryStage.setIconified(false);
        if (!primaryStage.isShowing()) {
            if (AuthState.get() != null) showHomePage();
            else showLandingPage();
            primaryStage.show();
        }
        primaryStage.toFront();
    }

    public void showLandingPage() {
        LandingPage landing = new LandingPage(this);
        Scene scene = new Scene(landing, 1280, 720);
        var url = LandingPage.class.getResource("/landing-styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showAuthPage(boolean showRegister) {
        AuthPage page = new AuthPage(this, showRegister);
        Scene scene = new Scene(page, 1280, 720);
        var url = AuthPage.class.getResource("/auth-styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showProfilePage() {
        ProfilePage profile = new ProfilePage(this);
        Scene scene = new Scene(profile, 1280, 720);
        var url = ProfilePage.class.getResource("/profile-styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showSecurityLogPage() {
        SecurityLogPage logPage = new SecurityLogPage(this);
        Scene scene = new Scene(logPage, 1280, 720);
        var url = AuthPage.class.getResource("/home-styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showHomePage() {
        HomePage home = new HomePage();
        Scene scene = new Scene(home, 1280, 720);
        var url = AuthPage.class.getResource("/home-styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        primaryStage.setScene(scene);
    }

    public static CamouShield getInstance() {
        return instance;
    }

    public void triggerSecurityCheck(Path dataPath, String dataType) {
        Platform.runLater(() -> {
            showWindow();
            ReAuthDialog.show(primaryStage, dataPath, dataType);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}