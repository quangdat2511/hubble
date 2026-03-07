package com.hubble.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String clientId;

    /**
     * Verify Google ID Token and extract user info.
     * @param idTokenString the ID token from Google Sign-In
     * @return GoogleUserInfo or null if verification fails
     */
    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return new GoogleUserInfo(
                        payload.getEmail(),
                        (String) payload.get("name"),
                        (String) payload.get("picture"),
                        payload.getEmailVerified()
                );
            }
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
        }
        return null;
    }

    @Getter
    public static class GoogleUserInfo {
        private final String email;
        private final String name;
        private final String pictureUrl;
        private final boolean emailVerified;

        public GoogleUserInfo(String email, String name, String pictureUrl, boolean emailVerified) {
            this.email = email;
            this.name = name;
            this.pictureUrl = pictureUrl;
            this.emailVerified = emailVerified;
        }
    }
}
