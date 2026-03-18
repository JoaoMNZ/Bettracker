package io.github.joaomnz.bettracker.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    public static String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return urlEncoder.encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return encoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required algorithm not available", exception);
        }
    }
}