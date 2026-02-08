package com.cadac.stone_inscription.auth.utill;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class GenrateRefreshToken {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String doGenrateRefreshToken() {

        byte[] randomBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

    }

    public static String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8));

            // Store as Base64 (recommended for DB)
            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not supported", e);
        }
    }
}