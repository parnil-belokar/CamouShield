package org.example.cams;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class PythonManager {

    private static Process pythonProcess;
    private static final int PORT = 8000;
    private static final String SCRIPT_NAME = "biometric_auth.py";

    public static void startServer() {
        if (isServerRunning()) {
            System.out.println("ML Server is already running on port " + PORT);
            return;
        }

        System.out.println("Starting ML Server...");

        try {
            // Attempt to find python executable
            // On Windows, it might be "python" or "python3" or "py"
            // We try a few common commands
            String pythonCmd = detectPythonCommand();

            if (pythonCmd == null) {
                System.err.println("CRITICAL: Python not found in PATH. Cannot start ML model.");
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(pythonCmd, SCRIPT_NAME);

            // Redirect output so we can see python logs in Java console (optional)
            pb.redirectErrorStream(true);

            // Ensure the process starts in the correct directory (Current working dir)
            pb.directory(new File(System.getProperty("user.dir")));

            pythonProcess = pb.start();

            // Thread to read Python output (prevents buffer blocking)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[PYTHON] " + line);
                    }
                } catch (IOException e) {
                    // Process closed
                }
            }).start();

            // Register shutdown hook to kill python when Java closes
            Runtime.getRuntime().addShutdownHook(new Thread(PythonManager::stopServer));

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to launch Python script.");
        }
    }

    public static void stopServer() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("Stopping ML Server...");
            pythonProcess.destroy(); // Try graceful kill
            try {
                // Wait a bit, then force kill if needed
                Thread.sleep(1000);
                if (pythonProcess.isAlive()) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Check if something is listening on port 8000
    private static boolean isServerRunning() {
        try (Socket socket = new Socket("localhost", PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String detectPythonCommand() {
        String[] commands = {"python", "python3", "py"};
        for (String cmd : commands) {
            try {
                Process p = new ProcessBuilder(cmd, "--version").start();
                int exitCode = p.waitFor();
                if (exitCode == 0) return cmd;
            } catch (Exception ignored) {}
        }
        return null;
    }
}