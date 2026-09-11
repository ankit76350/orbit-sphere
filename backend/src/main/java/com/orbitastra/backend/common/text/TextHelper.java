package com.orbitastra.backend.common.text;

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
 * <p>Static rather than a Spring bean, unlike {@link CoreHelper}. That one holds policy
 * worth being able to swap or mock — a reserved word list, the IANA zone set — and it rejects
 * things. These are pure string functions with nothing to decide and nothing to refuse, and
 * making them a bean would add a constructor parameter to every service that ever formats a
 * field, which is all of them.
 *
 * <p>Which is also why normalising lives here and not in CoreHelper: trimming decides
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

    /**
     * Free text turned into a stable code. "maths-2" becomes {@code MATHS_2}.
     *
     * <p><b>Added twice.</b> It arrived on 2026-09-10 for a {@code classCode} that was removed
     * the same day, and went with it; {@code subjectCode} brought it back on 2026-09-11 with a
     * caller that is staying. The transformation is the one every module needs for a code other
     * collections reference — {@code subjectCode}, {@code termCode}, {@code planCode} — so it
     * lives here rather than as a regex per module. Two copies of a key derivation that drift
     * apart mean one name produces two different keys depending on which endpoint created it,
     * and nothing would report that.
     *
     * <p><b>Never throws, and never decides anything.</b> A source of nothing but punctuation
     * produces {@code ""}, and it is the caller's job to say whether that is a refusal — which it
     * is on a create, and is not on a lookup, where an unusable code simply matches nothing.
     * Refusing in here would make a URL typo a complaint about shape rather than a 404.
     *
     * <p><b>Not for a value that is also displayed.</b> {@code sectionNo} is stored exactly as a
     * school typed it, because it is the display value as well as the reference — "Blue" must not
     * become "BLUE". A subject has {@code name} for display, which is what leaves its code free
     * to be a code.
     *
     * @param maxLength the storage limit for this kind of code; 40 for every code in this system
     * @return the normalized code, or {@code ""} when nothing usable was left
     */
    public static String toCode(String source, int maxLength) {
        if (source == null || source.isBlank()) {
            return "";
        }

        // Anything that is not a letter or a digit becomes one underscore: spaces, hyphens,
        // ampersands, punctuation. Then the ends are trimmed, so "Maths (Advanced)" does not
        // produce a code ending in an underscore.
        String code = source.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (code.length() > maxLength) {
            code = code.substring(0, maxLength).replaceAll("_+$", "");
        }
        return code;
    }
}
