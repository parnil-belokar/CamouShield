package org.example.cams;

import java.io.File;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StartupManager {

    private static final String APP_NAME = "CamouShield";
    private static final String OS = System.getProperty("os.name").toLowerCase();

    /**
     * Toggles startup status.
     * @param enable true to add to startup, false to remove.
     */
    public static void setRunOnStartup(boolean enable) {
        String path = getRunningPath();
        if (path == null) {
            System.err.println("Could not determine application path.");
            return;
        }

        try {
            if (OS.contains("win")) {
                handleWindows(enable, path);
            } else if (OS.contains("nix") || OS.contains("nux")) {
                handleLinux(enable, path);
            } else if (OS.contains("mac")) {
                handleMac(enable, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the app is currently set to run on startup.
     */
    public static boolean isRunOnStartupEnabled() {
        try {
            if (OS.contains("win")) {
                // Query Windows Registry
                Process p = Runtime.getRuntime().exec("reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v " + APP_NAME);
                return p.waitFor() == 0; // 0 means found
            }
            // Add simple file checks for Linux/Mac if needed
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void handleWindows(boolean enable, String path) throws Exception {
        if (enable) {
            // Add to Registry with the --background flag
            // Command: "Path\To\App.exe" --background
            // OR: java -jar "Path\To\App.jar" --background

            String command;
            if (path.endsWith(".jar")) {
                command = "javaw -jar \\\"" + path + "\\\" --background";
            } else {
                command = "\\\"" + path + "\\\" --background";
            }

            String regCommand = "reg add HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v " + APP_NAME + " /t REG_SZ /d \"" + command + "\" /f";
            Runtime.getRuntime().exec(regCommand);
        } else {
            // Remove from Registry
            String regCommand = "reg delete HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v " + APP_NAME + " /f";
            Runtime.getRuntime().exec(regCommand);
        }
    }

    private static void handleLinux(boolean enable, String path) throws Exception {
        String configDir = System.getProperty("user.home") + "/.config/autostart/";
        File dir = new File(configDir);
        if (!dir.exists()) dir.mkdirs();

        File desktopFile = new File(dir, "camoushield.desktop");

        if (enable) {
            String execCmd = path.endsWith(".jar") ? "java -jar \"" + path + "\" --background" : "\"" + path + "\" --background";
            String content = "[Desktop Entry]\n" +
                    "Type=Application\n" +
                    "Name=" + APP_NAME + "\n" +
                    "Exec=" + execCmd + "\n" +
                    "Terminal=false\n";
            try (FileWriter fw = new FileWriter(desktopFile)) {
                fw.write(content);
            }
        } else {
            if (desktopFile.exists()) desktopFile.delete();
        }
    }

    private static void handleMac(boolean enable, String path) throws Exception {
        // MacOS usually requires bundling as .app, but we can try osascript for login items
        if (enable) {
            String cmd = "osascript -e 'tell application \"System Events\" to make login item at end with properties {path:\"" + path + "\", hidden:true}'";
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        } else {
            String cmd = "osascript -e 'tell application \"System Events\" to delete login item \"" + APP_NAME + "\"'";
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        }
    }

    // Helper to find where the JAR or EXE is running from
    private static String getRunningPath() {
        try {
            return new File(StartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}