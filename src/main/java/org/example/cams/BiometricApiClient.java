package org.example.cams;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class BiometricApiClient {

    private static final String BASE_URL = "http://localhost:8000";
    private final HttpClient client = HttpClient.newHttpClient();

    public boolean trainModel(Path file, String formFieldName) {
        if (file == null) return false;
        try {
            String boundary = UUID.randomUUID().toString();
            byte[] body = buildMultipartData(boundary, file, formFieldName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/train"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // System.out.println("Train Response: " + response.body());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyUser(Path file, String formFieldName) {
        if (file == null) return true; // Fail open if no file
        try {
            String boundary = UUID.randomUUID().toString();
            byte[] body = buildMultipartData(boundary, file, formFieldName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/verify"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().contains("\"is_same_user\":true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // NEW: Method to reset backend data
    public boolean resetModel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/reset"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Reset Response: " + response.body());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private byte[] buildMultipartData(String boundary, Path file, String paramName) throws Exception {
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + file.getFileName() + "\"\r\n" +
                "Content-Type: application/json\r\n\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, payload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, payload, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, payload, headerBytes.length + fileBytes.length, footerBytes.length);
        return payload;
    }
}