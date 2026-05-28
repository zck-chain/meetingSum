package com.meetingsum.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {}

    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String hashFilename(String originalName) {
        try {
            String salt = Instant.now().toString() + "-" + UUID.randomUUID();
            String raw = originalName + "-" + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
