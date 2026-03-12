package org.example.cams;

import java.io.IOException;

public class OSLockUtil {
    public static void lockScreen() {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        try {
            System.out.println("Attempting to lock screen for OS: " + os);
            if (os.contains("win")) {
                rt.exec("rundll32.exe user32.dll,LockWorkStation");
            } else if (os.contains("mac")) {
                String[] cmd = {"/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend"};
                rt.exec(cmd);
            } else if (os.contains("nix") || os.contains("nux")) {
                try {
                    rt.exec("gnome-screensaver-command -l");
                } catch (Exception e) {
                    rt.exec("dm-tool lock");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}