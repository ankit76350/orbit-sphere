package com.orbitastra.backend.common.mongo;

import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Building text criteria out of what a caller typed.
 *
 * <p>Extracted when the plan list needed the same three things the school list already had. One
 * copy each was fine; two copies of {@link #escapeRegex} is the kind of duplication where one
 * gets a fix and the other quietly does not — and the fix in question is a security one.
 *
 * <p>Every method here takes caller input and returns a criteria that treats it as a
 * <b>literal</b>. Nothing in this class builds a pattern out of what somebody sent.
 */
public final class CriteriaText {

    private CriteriaText() {
    }

    /**
     * A partial, case-insensitive match. {@code "orbit"} finds "Orbit Astra International".
     */
    public static Criteria containsIgnoreCase(String field, String value) {
        return Criteria.where(field).regex(escapeRegex(value.trim()), "i");
    }

    /**
     * An exact match that ignores case.
     *
     * <p>Anchored at both ends, so it cannot also match a longer value: without the anchors,
     * asking for city "Pune" would return "Punegaon" as well.
     */
    public static Criteria exactIgnoreCase(String field, String value) {
        return Criteria.where(field).regex("^" + escapeRegex(value.trim()) + "$", "i");
    }

    /**
     * Makes caller input safe to put inside a regular expression.
     *
     * <p><b>This is not optional.</b> A search term goes into a Mongo regex, so without it a
     * caller can send a pattern rather than a word: {@code .*} matches every row, and a
     * nested-quantifier pattern can hold a database thread for a very long time on very little
     * input.
     *
     * <p>Escaping also gives the caller what they meant. Somebody searching for {@code "st."}
     * wants a full stop, not "any character".
     */
    public static String escapeRegex(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if ("\\.[]{}()*+-?^$|/".indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
