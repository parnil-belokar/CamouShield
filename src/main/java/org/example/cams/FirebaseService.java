package org.example.cams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class FirebaseService {

    private static final String API_KEY = "AIzaSyA-8Fq8WxRNJRvl4XpYrpQgvlMlCbyeSuY";

    private static final String SIGN_UP_URL   = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
    private static final String SIGN_IN_URL   = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
    private static final String LOOKUP_URL    = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + API_KEY;
    private static final String UPDATE_URL    = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + API_KEY;
    private static final String DELETE_URL    = "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=" + API_KEY;
    private static final String REFRESH_URL   = "https://securetoken.googleapis.com/v1/token?key=" + API_KEY;
    private static final String RESET_PWD_URL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + API_KEY;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String signUp(String email, String password) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("email", email);
        json.put("password", password);
        json.put("returnSecureToken", true);
        return sendRequest(SIGN_UP_URL, json);
    }

    public static String signIn(String email, String password) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("email", email);
        json.put("password", password);
        json.put("returnSecureToken", true);
        return sendRequest(SIGN_IN_URL, json);
    }

    // Send Password Reset Email
    public static void sendPasswordResetEmail(String email) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("requestType", "PASSWORD_RESET");
        json.put("email", email);
        sendRequest(RESET_PWD_URL, json);
    }

    // Refresh the ID Token using the long-lived Refresh Token
    public static JsonNode refreshToken(String refreshToken) throws Exception {
        String body = "grant_type=refresh_token&refresh_token=" + refreshToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REFRESH_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            JsonNode errorNode = mapper.readTree(response.body());
            String msg = errorNode.path("error").path("message").asText();
            throw new RuntimeException("Token Refresh Failed: " + msg);
        }

        return mapper.readTree(response.body());
    }

    // Get user profile info from Firebase using idToken
    public static JsonNode getProfile(String idToken) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("idToken", idToken);
        String body = sendRequest(LOOKUP_URL, json);
        return mapper.readTree(body).path("users").get(0); // first user
    }

    // Change password for current user
    public static void changePassword(String idToken, String newPassword) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("idToken", idToken);
        json.put("password", newPassword);
        json.put("returnSecureToken", false);
        sendRequest(UPDATE_URL, json);
    }

    // Delete current user account
    public static void deleteAccount(String idToken) throws Exception {
        ObjectNode json = mapper.createObjectNode();
        json.put("idToken", idToken);
        sendRequest(DELETE_URL, json);
    }

    private static String sendRequest(String url, ObjectNode jsonBody) throws Exception {
        String requestBody = mapper.writeValueAsString(jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            JsonNode errorNode = mapper.readTree(response.body());
            String msg = errorNode.path("error").path("message").asText();
            throw new RuntimeException("Firebase Error: " + msg);
        }

        return response.body();
    }
}