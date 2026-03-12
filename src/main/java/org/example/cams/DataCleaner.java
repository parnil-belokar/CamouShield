package org.example.cams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class DataCleaner {

    private static final String DATA_FOLDER = "data";

    /**
     * Deletes all temporary JSON and JSONL files in the data directory.
     * Call this on application startup to ensure a clean slate.
     */
    public static void cleanStaleData() {
        File folder = new File(DATA_FOLDER);
        if (!folder.exists()) return;

        System.out.println("Cleaning up stale data files...");

        try (Stream<Path> walk = Files.walk(folder.toPath())) {
            walk.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().endsWith(".json") || f.getName().endsWith(".jsonl"))
                    .forEach(file -> {
                        if (file.delete()) {
                            System.out.println("Deleted stale file: " + file.getName());
                        } else {
                            System.err.println("Failed to delete: " + file.getName());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error cleaning data folder: " + e.getMessage());
        }
    }
}