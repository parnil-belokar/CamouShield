package org.example.cams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogManager {

    private static final String LOG_FILE = "security_logs.json";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObservableList<LogEntry> logs = FXCollections.observableArrayList();

    static {
        loadLogs();
    }

    public static void addLog(String title, String details, Severity severity) {
        LogEntry entry = new LogEntry(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                title,
                details,
                severity
        );
        logs.add(0, entry); // Add to top
        saveLogs();
    }

    public static ObservableList<LogEntry> getLogs() {
        return logs;
    }

    public static void clearLogs() {
        logs.clear();
        saveLogs();
    }

    private static void saveLogs() {
        try {
            mapper.writeValue(new File(LOG_FILE), new ArrayList<>(logs));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadLogs() {
        File file = new File(LOG_FILE);
        if (file.exists()) {
            try {
                List<LogEntry> loaded = mapper.readValue(file, new TypeReference<List<LogEntry>>() {});
                logs.setAll(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Inner Class for Data ---
    public static class LogEntry {
        public String timestamp;
        public String title;
        public String details;
        public Severity severity;

        // Default constructor for Jackson
        public LogEntry() {}

        public LogEntry(String timestamp, String title, String details, Severity severity) {
            this.timestamp = timestamp;
            this.title = title;
            this.details = details;
            this.severity = severity;
        }
    }

    public enum Severity {
        INFO, WARNING, CRITICAL, SUCCESS
    }
}