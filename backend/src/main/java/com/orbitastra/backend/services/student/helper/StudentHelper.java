package com.orbitastra.backend.services.student.helper;

import org.springframework.stereotype.Component;

/**
 * What the student module's services and their utils both need.
 *
 * <p><b>It exists because a second service arrived.</b> There was none while {@code student} had
 * only {@code StudentService}: a helper is for what two things share, and one thing shares nothing.
 * {@code GuardianService} (#7) was what created the sharing — it has to normalise a phone number
 * exactly the way {@code StudentServiceUtils} does, because the two write into the same unique
 * index and a number stored one way by one of them and another way by the other is a duplicate the
 * database cannot see.
 *
 * <p><b>One file per module, named after the module.</b> Not after what it happens to hold today,
 * because that name dates the moment something else goes in.
 *
 * <p><b>Flat.</b> A method here never calls another method here; only services and their utils call
 * these.
 */
@Component
public class StudentHelper {

    // What people type into phone numbers and nobody stores: spaces (including the non-breaking
    // one that comes from pasting out of a spreadsheet), brackets, hyphens and dots.
    private static final String PHONE_NOISE = "[\\s\\u00A0()\\-.]";

    /**
     * Strips what people type into a phone number and nobody stores, keeping a leading "+".
     *
     * <p><b>THIS IS THE MATCH KEY, which is why it has to be in one place.</b> A guardian's number
     * is unique per school — {@code school_guardian_phone_uniq} — so #1 looking a person up and #7
     * refusing a duplicate have to agree, character for character, about what the stored form of a
     * number is. Two copies of this rule drifting apart would mean #1 quietly creating the person
     * #7 says already exists.
     *
     * <p><b>A number made of nothing but punctuation comes back null</b> — "( )", "--" — rather
     * than as an empty string. A blank number stored behind a request that looked filled in is
     * worse than no number, and it would also take the one slot the unique index allows for
     * "no phone".
     *
     * Used by:
     * - StudentServiceUtils.linkGuardians()
     * - GuardianService.createGuardian()
     */
    public String normalisePhone(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.replaceAll(PHONE_NOISE, "");
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * Just the digits somebody typed, for comparing two spellings of one phone number.
     *
     * <p><b>Different from {@link #normalisePhone}, which is what gets STORED</b> — that keeps a
     * leading "+". This is for asking a question about a number rather than writing one down, and
     * the two are not interchangeable: a stored "+919876543210" and a typed "9876543210" are the
     * same digits and different strings.
     *
     * <p>It moved here from {@code StudentServiceUtils} on 2026-10-07, when #9 arrived and gave it
     * a second caller in a different service. #9 is the check somebody makes before #7 refuses, so
     * a second copy of this rule drifting apart would mean the check and the refusal disagreed
     * about one number.
     *
     * Used by:
     * - GuardianService.listGuardians()
     * - StudentServiceUtils.linkGuardians()
     */
    public String digitsOf(String typed) {
        if (typed == null) {
            return "";
        }

        StringBuilder digits = new StringBuilder();
        for (char each : typed.toCharArray()) {
            if (each >= '0' && each <= '9') {
                digits.append(each);
            }
        }
        return digits.toString();
    }
}
