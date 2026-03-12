package org.example.cams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class SessionManager {

    private static final String SESSION_FILE = "user_session.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    // Attempt to load session and auto-login
    public static boolean tryAutoLogin() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) return false;

        try {
            JsonNode root = mapper.readTree(file);
            String refreshToken = root.path("refreshToken").asText();

            // We only really need the refresh token to get a fresh session
            if (refreshToken == null || refreshToken.isEmpty()) return false;

            System.out.println("Found saved session. Refreshing token...");

            // Refresh the token with Firebase
            JsonNode refreshResponse = FirebaseService.refreshToken(refreshToken);

            // Parse new tokens
            String newIdToken = refreshResponse.path("id_token").asText(); // Note: Firebase returns 'id_token' here, not 'idToken'
            String newRefreshToken = refreshResponse.path("refresh_token").asText();
            String userId = refreshResponse.path("user_id").asText();

            // We need email too, usually strictly not returned by refresh,
            // but we stored it or can fetch it via getProfile
            String email = root.path("email").asText();

            // Update AuthState
            AuthState.login(email, newIdToken, newRefreshToken, userId);

            // Save updated credentials (refresh token might have changed)
            saveSession(email, newIdToken, newRefreshToken, userId);

            return true;

        } catch (Exception e) {
            System.err.println("Auto-login failed: " + e.getMessage());
            clearSession(); // Corrupt or invalid session
            return false;
        }
    }

    public static void saveSession(String email, String idToken, String refreshToken, String localId) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("email", email);
            root.put("idToken", idToken);
            root.put("refreshToken", refreshToken);
            root.put("localId", localId);
            mapper.writeValue(new File(SESSION_FILE), root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearSession() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}