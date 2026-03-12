package org.example.cams;

import javafx.application.Platform;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtectionManager {

    // == CONFIGURATION ==
    private static final int INITIAL_TRAIN_THRESHOLD = 300;
    private static final int VERIFY_BATCH_SIZE = 150;

    // == STATE ==
    private static boolean isModelTrained = false;
    private static final AtomicBoolean isBusy = new AtomicBoolean(false);
    private static CamouShield mainApp;
    private static final BiometricApiClient apiClient = new BiometricApiClient();

    private static final AtomicInteger keyStagingCount = new AtomicInteger(0);
    private static final AtomicInteger mouseStagingCount = new AtomicInteger(0);

    public static void setMainApp(CamouShield app) {
        mainApp = app;
    }

    /**
     * Resets local counters and flags, AND calls the Python API to delete data.
     */
    public static boolean resetAllData() {
        // 1. Reset Local Java State
        isModelTrained = false;
        keyStagingCount.set(0);
        mouseStagingCount.set(0);
        resetStaging("keyboard");
        resetStaging("mouse");

        // 2. Call Python Backend to delete files
        return apiClient.resetModel();
    }

    public static synchronized void onMicroBatchComplete(File microFile, String type, int count) {
        try {
            if (AuthState.get() == null) return;

            File stagingFile = getStagingFile(type);
            appendContent(microFile, stagingFile);

            int totalEvents;
            if (type.equals("keyboard")) totalEvents = keyStagingCount.addAndGet(count);
            else totalEvents = mouseStagingCount.addAndGet(count);

            System.out.println("[" + type + "] Staging Buffer: " + totalEvents + " events.");

            if (isBusy.get()) return;

            if (!isModelTrained) {
                // INITIAL TRAINING PHASE
                if (totalEvents >= INITIAL_TRAIN_THRESHOLD) {
                    processCluster(stagingFile, type, "TRAIN");
                }
            } else {
                // VERIFICATION PHASE
                if (totalEvents >= VERIFY_BATCH_SIZE) {
                    processCluster(stagingFile, type, "VERIFY");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (microFile != null && microFile.exists()) {
                microFile.delete();
            }
        }
    }

    private static void processCluster(File clusterFile, String type, String action) {
        new Thread(() -> {
            if (!isBusy.compareAndSet(false, true)) return;

            try {
                Path path = clusterFile.toPath();
                String formField = type.equals("keyboard") ? "keyboard_file" : "mouse_file";

                if (action.equals("TRAIN")) {
                    System.out.println(">>> Sending Cluster for INITIAL TRAINING (" + type + ")...");
                    boolean success = apiClient.trainModel(path, formField);
                    if (success) {
                        isModelTrained = true;
                        resetStaging(type);
                        System.out.println(">>> Initial Training Complete.");
                    }
                }
                else if (action.equals("VERIFY")) {
                    System.out.println(">>> Sending Cluster for VERIFICATION (" + type + ")...");
                    boolean authorized = apiClient.verifyUser(path, formField);

                    if (authorized) {
                        System.out.println(">>> User Verified. Extending model with new data.");
                        apiClient.trainModel(path, formField);
                        resetStaging(type);
                    } else {
                        System.err.println(">>> INTRUDER DETECTED. Triggering Auth.");
                        Platform.runLater(() -> {
                            if (mainApp != null) mainApp.triggerSecurityCheck(path, formField);
                            else OSLockUtil.lockScreen();
                        });
                    }
                }
            } finally {
                isBusy.set(false);
            }
        }).start();
    }

    public static void forceTrain(Path dataPath, String fieldName) {
        new Thread(() -> {
            System.out.println(">>> False Positive Confirmed. Force Training...");
            boolean success = apiClient.trainModel(dataPath, fieldName);
            if (success) {
                String type = fieldName.contains("key") ? "keyboard" : "mouse";
                resetStaging(type);
            }
        }).start();
    }

    private static void resetStaging(String type) {
        File file = getStagingFile(type);
        try {
            new FileWriter(file, false).close();
        } catch (IOException e) { e.printStackTrace(); }

        if (type.equals("keyboard")) keyStagingCount.set(0);
        else mouseStagingCount.set(0);
    }

    private static File getStagingFile(String type) {
        File folder = new File("data");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, "staging_" + type + ".jsonl");
    }

    private static void appendContent(File source, File dest) {
        try {
            String content = Files.readString(source.toPath());
            if (dest.exists() && dest.length() > 0 && !content.startsWith("\n")) {
                content = "\n" + content;
            }
            Files.writeString(dest.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}