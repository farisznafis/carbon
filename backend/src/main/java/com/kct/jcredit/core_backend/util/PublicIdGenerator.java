package com.kct.jcredit.core_backend.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure public identifiers for API-facing entities.
 * <p>
 * These public IDs replace numeric DB IDs in API paths and responses,
 * preventing enumeration attacks and decoupling API contracts from DB internals.
 */
public final class PublicIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final String SITE_PREFIX = "SITE";
    private static final String PROJECT_PREFIX = "PROJ";
    private static final int RANDOM_PART_LENGTH = 16;

    private PublicIdGenerator() {
        // Utility class — no instantiation
    }

    /**
     * Generates a public ID for a Site entity.
     * <p>
     * Format: {@code SITE} + 16 random Base62 characters (total 20 chars).
     * Collision probability: 62^16 ≈ 4.7 × 10^28 possible values.
     *
     * @return a new unique site public ID, e.g. {@code SITEaB3kM9pQ7xL2vN8s}
     */
    public static String generateSitePublicId() {
        return SITE_PREFIX + generateRandomBase62(RANDOM_PART_LENGTH);
    }

        public static String generateProjectPublicId() {
        return PROJECT_PREFIX + generateRandomBase62(RANDOM_PART_LENGTH);
    }

    private static String generateRandomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(SECURE_RANDOM.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }
}
