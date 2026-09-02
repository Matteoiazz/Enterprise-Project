package com.tripify.catalog_service.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Confronto a tempo costante per la chiave di servizio condivisa (header X-Internal-Key):
 * un normale String.equals() esce al primo carattere diverso, quindi il tempo di risposta
 * rivelerebbe quanti caratteri iniziali della chiave sono stati indovinati.
 */
public final class InternalKeyValidator {

    private InternalKeyValidator() {
    }

    public static boolean matches(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }
}
