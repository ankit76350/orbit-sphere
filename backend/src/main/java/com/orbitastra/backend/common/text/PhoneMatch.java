package com.orbitastra.backend.common.text;

import java.util.regex.Pattern;

/**
 * What "the same phone number" means, in one place.
 *
 * <p><b>It lives in {@code common} because three different layers need it and none of them may
 * call the others.</b> {@code GuardianRepositoryImpl} and {@code StudentRepositoryImpl} both build
 * the query, and {@code StudentHelper} answers the same question for the services — a copy in each
 * is three chances for the check a desk makes, the refusal they get, and the row the database
 * finds to disagree about one number. That has already happened twice in this module.
 *
 * <p><b>People do not type a number the same way twice.</b> "+91 98765 43210", "098765-43210" and
 * "9876543210" are one number, and a comparison of the stored strings says they are three.
 */
public final class PhoneMatch {

    /** Ten digits is a whole Indian mobile number. See {@link #loosePattern}. */
    public static final int FULL_PHONE_DIGITS = 10;

    private PhoneMatch() {
    }

    /**
     * Just the digits somebody typed.
     *
     * <p>Different from what is <i>stored</i>, which keeps a leading "+". This is for asking a
     * question about a number rather than writing one down.
     */
    public static String digitsOf(String typed) {
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

    /**
     * The part of a query worth comparing on — its <b>last ten digits</b> once it is long enough.
     *
     * <p>So a country code or a trunk 0 on either side stops mattering: a stored "+919876543210"
     * and a typed "09876543210" both come down to "9876543210".
     */
    public static String needleFrom(String digits) {
        return digits.length() < FULL_PHONE_DIGITS
                ? digits
                : digits.substring(digits.length() - FULL_PHONE_DIGITS);
    }

    /** Whether a query is short enough that it has to match the <b>whole</b> stored number. */
    public static boolean isWholeNumber(String digits) {
        return digits.length() < FULL_PHONE_DIGITS;
    }

    /**
     * The regular expression that makes two spellings of one number the same question.
     *
     * <p><b>Anything is allowed between the digits</b>, which is what makes the spacing, the
     * brackets and the hyphens stop mattering.
     *
     * <p><b>The END anchor is always there; the START anchor depends on how much was given.</b> A
     * full-length number is compared on its last ten digits, so a stored value carrying a country
     * code still matches. A <i>shorter</i> query must match the whole number — without that,
     * "543210" matches the tail of every number ending in those six digits, which is a false "we
     * already know them" and the worst answer any of the callers can give.
     *
     * <p><b>Every digit is quoted as it is joined.</b> The strip leaves nothing but digits, so
     * there is nothing left to quote — belt and braces, and the day somebody widens the strip it is
     * already right.
     */
    public static String loosePattern(String digits, boolean wholeNumber) {
        StringBuilder pattern = new StringBuilder(wholeNumber ? "^[^0-9]*" : "");
        boolean first = true;
        for (char digit : digits.toCharArray()) {
            if (!first) {
                pattern.append("[^0-9]*");
            }
            pattern.append(Pattern.quote(String.valueOf(digit)));
            first = false;
        }
        return pattern.append("[^0-9]*$").toString();
    }
}
