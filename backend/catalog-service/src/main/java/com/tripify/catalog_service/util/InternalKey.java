package com.tripify.catalog_service.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class InternalKey {

    private InternalKey() {
    }

    public static boolean matches(String expected, String provided) {
        if (expected == null || expected.isEmpty() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
