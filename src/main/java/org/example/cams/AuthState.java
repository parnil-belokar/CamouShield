package org.example.cams;

public class AuthState {
    private static AuthState instance;
    private String email;
    private String idToken;
    private String refreshToken;
    private String localId; // Firebase User ID

    private AuthState(String email, String idToken, String refreshToken, String localId) {
        this.email = email;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.localId = localId;
    }

    public static synchronized void login(String email, String idToken, String refreshToken, String localId) {
        instance = new AuthState(email, idToken, refreshToken, localId);
    }

    public static synchronized void logout() {
        instance = null;
    }

    public static synchronized AuthState get() {
        return instance;
    }

    public String getLocalId() { return localId; }
    public String getEmail() { return email; }
    public String getIdToken() { return idToken; }
    public String getRefreshToken() { return refreshToken; }
}