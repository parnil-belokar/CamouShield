package org.example.cams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class FirestoreService {

    // TODO: REPLACE THIS WITH YOUR ACTUAL FIREBASE PROJECT ID
    private static final String PROJECT_ID = "AIzaSyA-8Fq8WxRNJRvl4XpYrpQgvlMlCbyeSuY";

    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Saves the security question and answer for a user.
     * Also saves a mapping of Email -> UID to allow lookup during "Forgot Password".
     */
    public static void saveSecurityQuestion(String uid, String email, String question, String answer) {
        try {
            // 1. Save to users/{uid}
            String userUrl = BASE_URL + "/users/" + uid;
            saveDoc(userUrl, uid, question, answer);

            // 2. Save to email_lookup/{sanitized_email}
            // We replace '.' with '_' because Firestore IDs cannot contain some characters easily in URLs
            String sanitizedEmail = email.replace(".", "_");
            String emailUrl = BASE_URL + "/email_lookup/" + sanitizedEmail;
            saveDoc(emailUrl, uid, question, answer);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to save security question to Firestore. Check PROJECT_ID.");
        }
    }

    private static void saveDoc(String url, String uid, String question, String answer) throws Exception {
        // Firestore REST API expects a specific JSON structure:
        // { "fields": { "key": { "stringValue": "value" } } }

        ObjectNode root = mapper.createObjectNode();
        ObjectNode fields = root.putObject("fields");

        fields.putObject("uid").put("stringValue", uid);
        fields.putObject("question").put("stringValue", question);
        fields.putObject("answer").put("stringValue", answer.toLowerCase().trim()); // Normalize answer

        String body = mapper.writeValueAsString(root);

        // We use PATCH to create or update
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Fetches security data by UID (used in Lock Screen)
     */
    public static SecurityData getSecurityDataByUid(String uid) {
        return fetch(BASE_URL + "/users/" + uid);
    }

    /**
     * Fetches security data by Email (used in Login -> Forgot Password)
     */
    public static SecurityData getSecurityDataByEmail(String email) {
        String sanitizedEmail = email.replace(".", "_");
        return fetch(BASE_URL + "/email_lookup/" + sanitizedEmail);
    }

    private static SecurityData fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode fields = root.path("fields");

                String q = fields.path("question").path("stringValue").asText();
                String a = fields.path("answer").path("stringValue").asText();
                String u = fields.path("uid").path("stringValue").asText();

                if (!q.isEmpty()) return new SecurityData(u, q, a);
            }
        } catch (Exception e) {
            // Ignore (doc doesn't exist)
        }
        return null;
    }

    public static class SecurityData {
        public String uid;
        public String question;
        public String answer;

        public SecurityData(String uid, String question, String answer) {
            this.uid = uid;
            this.question = question;
            this.answer = answer;
        }
    }
}