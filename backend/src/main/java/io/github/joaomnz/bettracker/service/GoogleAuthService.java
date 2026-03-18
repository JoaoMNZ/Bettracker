package io.github.joaomnz.bettracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.github.joaomnz.bettracker.dto.auth.GoogleUserInfo;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleAuthService {
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserInfo verifyToken(String idTokenString){
        try{
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if(idToken == null){
                throw new BusinessRuleException("Invalid Google ID token.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            Boolean emailVerified = payload.getEmailVerified();
            if(emailVerified == null || !emailVerified){
                throw new BusinessRuleException("Google account email is not verified. Cannot proceed.");
            }

            return new GoogleUserInfo(
                    payload.getEmail(),
                    (String) payload.get("name"),
                    payload.getSubject(),
                    true
            );

        } catch(GeneralSecurityException | IOException exception) {
            throw new BusinessRuleException("Failed to verify Google token securely.");
        }
    }
}
