package com.orbitastra.backend.services.core.helper;

import java.util.Locale;

/**
 * Tidying up free text on the way into the database.
 *
 * <p>Both methods turn blank into null rather than storing an empty string, and that is the
 * whole point of them. A field that is sometimes {@code null}, sometimes {@code ""} and
 * sometimes {@code "  "} needs three checks everywhere it is read, and the third one gets
 * forgotten. Worse, a MongoDB unique index treats {@code ""} as a real value: two records with
 * an empty optional field collide, while two with null do not when the index is partial. One
 * representation for "not provided" avoids both problems.
 *
 * <p>Static rather than a Spring bean, unlike {@link CoreValidator}. That one holds policy
 * worth being able to swap or mock — a reserved word list, the IANA zone set — and it rejects
 * things. These are pure string functions with nothing to decide and nothing to refuse, and
 * making them a bean would add a constructor parameter to every service that ever formats a
 * field, which is all of them.
 *
 * <p>Which is also why normalising lives here and not in CoreValidator: trimming decides
 * nothing, and a validator folder that also holds formatters stops meaning anything.
 *
 * <p>{@code Locale.ROOT} on the lowercase is deliberate and not decoration. The default-locale
 * {@code toLowerCase()} lowercases a capital I to a dotless ı under a Turkish locale, so an
 * email address stored on a machine with that locale would not match the same address stored
 * anywhere else. Server-side normalisation must never depend on where the server is.
 */
public final class TextHelper {

    private TextHelper() {
    }

    /** Trimmed, or null when there was nothing but whitespace. */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Trimmed and lowercased for storage, or null when blank.
     *
     * <p>For values compared case-insensitively — an email address, a country code read back as
     * a key. Not for anything a person typed and expects to see again as they wrote it.
     */
    public static String lowercaseOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Trimmed and uppercased for storage, or null when blank. ISO country codes, currencies. */
    public static String uppercaseOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
