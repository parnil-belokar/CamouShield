package org.example.cams.Listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.example.cams.ListenersHolder; // Import Holder
import org.example.cams.ProtectionManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KeyListener implements NativeKeyListener {

    private static final int MICRO_BATCH_SIZE = 50;

    private final Map<String, Integer> keyFrequency = new HashMap<>();
    private final List<Double> dwellTimes = new ArrayList<>();
    private final List<Double> flightTimes = new ArrayList<>();
    private final Set<Integer> currentlyPressed = new HashSet<>();
    private final Map<Integer, Long> pressTimestamps = new HashMap<>();

    private int backspaceCount = 0;
    private int deleteCount = 0;
    private int nonAlphanumericCount = 0;
    private int spaceCount = 0;

    private long lastReleaseTime = -1;
    private final ObjectMapper mapper = new ObjectMapper();
    private int keystrokesInBatch = 0;

    public KeyListener() {
        System.out.println("KeyListener (Micro-Batching) Initialized.");
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // CHECK PAUSE STATE
        if (ListenersHolder.isPaused()) return;

        long now = System.currentTimeMillis();
        int keyCode = e.getKeyCode();

        if (currentlyPressed.contains(keyCode)) return;
        currentlyPressed.add(keyCode);
        pressTimestamps.put(keyCode, now);

        keystrokesInBatch++;

        String keyText = NativeKeyEvent.getKeyText(keyCode);
        if (!keyText.matches("[A-Za-z0-9]")) nonAlphanumericCount++;
        if (keyCode == NativeKeyEvent.VC_SPACE) spaceCount++;
        if (keyCode == NativeKeyEvent.VC_BACKSPACE) backspaceCount++;

        keyFrequency.put(keyText, keyFrequency.getOrDefault(keyText, 0) + 1);

        if (keystrokesInBatch >= MICRO_BATCH_SIZE) {
            flushBatch();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // CHECK PAUSE STATE
        if (ListenersHolder.isPaused()) return;

        long now = System.currentTimeMillis();
        lastReleaseTime = now;
        int keyCode = e.getKeyCode();
        currentlyPressed.remove(keyCode);
        Long pressTime = pressTimestamps.remove(keyCode);
        if (pressTime != null) dwellTimes.add((now - pressTime) / 1000.0);
        if (lastReleaseTime != -1) flightTimes.add((now - lastReleaseTime) / 1000.0);
    }

    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    private void flushBatch() {
        File file = saveMicroBatch();
        if (file != null) {
            ProtectionManager.onMicroBatchComplete(file, "keyboard", keystrokesInBatch);
        }
        resetCounters();
    }

    private void resetCounters() {
        keyFrequency.clear();
        dwellTimes.clear();
        flightTimes.clear();
        pressTimestamps.clear();
        currentlyPressed.clear();
        backspaceCount = 0;
        nonAlphanumericCount = 0;
        spaceCount = 0;
        keystrokesInBatch = 0;
    }

    private File saveMicroBatch() {
        try {
            File folder = new File("data");
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, "micro_key_" + System.currentTimeMillis() + ".json");

            ObjectNode root = mapper.createObjectNode();
            ObjectNode metrics = root.putObject("metrics");

            double meanDwell = dwellTimes.stream().mapToDouble(d -> d).average().orElse(0.0);
            double meanFlight = flightTimes.stream().mapToDouble(d -> d).average().orElse(0.0);

            metrics.put("mean_dwell", meanDwell);
            metrics.put("mean_flight", meanFlight);
            metrics.put("backspace_rate", keystrokesInBatch > 0 ? (double)backspaceCount/keystrokesInBatch : 0);
            metrics.put("non_alphanumeric_count", nonAlphanumericCount);

            mapper.writeValue(file, root);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}