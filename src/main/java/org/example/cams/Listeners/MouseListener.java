package org.example.cams.Listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import org.example.cams.ListenersHolder; // Import Holder
import org.example.cams.ProtectionManager;

import java.io.File;
import java.util.*;

public class MouseListener implements NativeMouseInputListener, NativeMouseWheelListener {

    private static final int MICRO_BATCH_SIZE = 50;
    private final List<Map<String, Object>> events = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private int eventsInBatch = 0;

    public MouseListener() {
        System.out.println("MouseListener (Micro-Batching) Initialized.");
    }

    private void addEvent(Map<String, Object> event) {
        // CHECK PAUSE STATE
        if (ListenersHolder.isPaused()) return;

        synchronized (events) {
            events.add(event);
            eventsInBatch++;

            if (eventsInBatch >= MICRO_BATCH_SIZE) {
                List<Map<String, Object>> snapshot = new ArrayList<>(events);
                events.clear();
                int count = eventsInBatch;
                eventsInBatch = 0;
                new Thread(() -> flushBatch(snapshot, count)).start();
            }
        }
    }

    private void flushBatch(List<Map<String, Object>> batchEvents, int count) {
        if (batchEvents.isEmpty()) return;
        File file = saveBatch(batchEvents);
        if (file != null) {
            ProtectionManager.onMicroBatchComplete(file, "mouse", count);
        }
    }

    private File saveBatch(List<Map<String, Object>> batchEvents) {
        try {
            File folder = new File("data");
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, "micro_mouse_" + System.currentTimeMillis() + ".json");
            ObjectNode features = computeFeatures(batchEvents);
            mapper.writeValue(file, features);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ObjectNode computeFeatures(List<Map<String, Object>> eventsList) {
        ObjectNode node = mapper.createObjectNode();
        double totalDist = 0;
        List<Double> speeds = new ArrayList<>();

        for (int i = 1; i < eventsList.size(); i++) {
            Map<String, Object> e1 = eventsList.get(i-1);
            Map<String, Object> e2 = eventsList.get(i);

            double dx = ((Number) e2.get("x")).doubleValue() - ((Number) e1.get("x")).doubleValue();
            double dy = ((Number) e2.get("y")).doubleValue() - ((Number) e1.get("y")).doubleValue();
            double dt = ((Number) e2.get("timestamp")).doubleValue() - ((Number) e1.get("timestamp")).doubleValue();
            double dist = Math.sqrt(dx*dx + dy*dy);

            totalDist += dist;
            if (dt > 0) speeds.add(dist/dt);
        }

        node.put("total_distance", totalDist);
        node.put("avg_speed", speeds.stream().mapToDouble(d->d).average().orElse(0));
        node.put("event_count", eventsList.size());
        return node;
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) { record("move", e.getX(), e.getY(), null); }
    @Override
    public void nativeMouseDragged(NativeMouseEvent e) { record("drag", e.getX(), e.getY(), null); }
    @Override
    public void nativeMouseClicked(NativeMouseEvent e) { record("click", e.getX(), e.getY(), e.getButton()); }
    @Override public void nativeMousePressed(NativeMouseEvent e) {}
    @Override public void nativeMouseReleased(NativeMouseEvent e) {}
    @Override public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {}

    private void record(String type, int x, int y, Integer btn) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("x", x);
        map.put("y", y);
        map.put("button", btn);
        map.put("timestamp", System.currentTimeMillis() / 1000.0);
        addEvent(map);
    }
}