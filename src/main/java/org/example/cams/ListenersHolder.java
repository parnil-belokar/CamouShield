package org.example.cams;

import com.github.kwhat.jnativehook.GlobalScreen;
import org.example.cams.Listeners.KeyListener;
import org.example.cams.Listeners.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListenersHolder {
    private static KeyListener keyListener;
    private static MouseListener mouseListener;
    private static boolean started = false;

    // New flag to pause collection without unhooking
    private static volatile boolean paused = false;

    public static synchronized void startListeners() throws Exception {
        if (started) return;

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook();
        }

        keyListener = new KeyListener();
        mouseListener = new MouseListener();

        GlobalScreen.addNativeKeyListener(keyListener);
        GlobalScreen.addNativeMouseListener(mouseListener);
        GlobalScreen.addNativeMouseMotionListener(mouseListener);
        GlobalScreen.addNativeMouseWheelListener(mouseListener);
        started = true;
        paused = false;
    }

    public static synchronized void stopListeners() {
        if (!started) return;
        try {
            GlobalScreen.removeNativeKeyListener(keyListener);
            GlobalScreen.removeNativeMouseListener(mouseListener);
            GlobalScreen.removeNativeMouseMotionListener(mouseListener);
            GlobalScreen.removeNativeMouseWheelListener(mouseListener);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            started = false;
        }
    }

    // New methods to control pause state
    public static void setPaused(boolean isPaused) {
        paused = isPaused;
        System.out.println("Monitoring " + (isPaused ? "PAUSED" : "RESUMED"));
    }

    public static boolean isPaused() {
        return paused;
    }
}