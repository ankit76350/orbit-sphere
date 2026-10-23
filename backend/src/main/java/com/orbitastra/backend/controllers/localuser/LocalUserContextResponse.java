package com.orbitastra.backend.controllers.localuser;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The id token that was issued, and what went into it.
 *
 * <h2>The body repeats the cookie on purpose</h2>
 *
 * <p>The cookie is {@code HttpOnly}, so the page that called this cannot read it with JavaScript.
 * Without this echo a caller would have to open developer tools to find out what it had just been
 * given — and in an API testing tool, "what did that actually do" is the whole question.
 *
 * <p>It is also what makes the call useful to a caller that is <b>not</b> a browser. {@code curl}
 * and Postman get the same answer in the body that a browser gets in a header.
 *
 * <h2>Handing the token back is not a leak</h2>
 *
 * <p>Every claim in it was supplied by the caller in the request that asked for it, and the
 * endpoint issues one to anybody. There is nothing in the token its holder did not already know,
 * which is a plainer way of saying the thing {@code warning} says.
 */
public record LocalUserContextResponse(

        /** The cookie's name — {@code idtoken} — so a caller knows what to look for or clear. */
        String cookieName,

        /**
         * The signed HS256 token, ready to paste into a decoder.
         *
         * <p><b>Absent when the request expired the cookie</b> ({@code maxAgeSeconds: 0}). A token
         * that is already expired at the moment it is signed is not a useful thing to return, so
         * none is minted.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String idToken,

        /**
         * The claims that went into it, before the standard ones were added.
         *
         * <p>{@code iss}, {@code iat} and {@code exp} are facts about the <i>token</i> rather than
         * about whoever it describes, so they are added during signing and are not repeated here.
         */
        Map<String, String> claims,

        /** How many characters the token came to, against the browser's ~4 KB cookie cap. */
        int tokenLength,

        /** How long the cookie will live, in seconds. Zero means it was expired immediately. */
        int maxAgeSeconds,

        /** When the token stops being valid. Absent when none was issued. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant expiresAt,

        /**
         * Whether the cookie was marked {@code Secure}.
         *
         * <p><b>False over plain HTTP, and that is not a bug.</b> A {@code Secure} cookie is
         * discarded by the browser on an {@code http://} page, so setting it on
         * {@code http://localhost} would mean the cookie silently never arrived. It follows the
         * request's own scheme.
         */
        boolean secure,

        /**
         * Whether the token was signed with the built-in development secret.
         *
         * <p><b>True means the signature is worth nothing to anybody who has read the source</b>,
         * which is everybody working on this. Reported rather than hidden, so a response cannot
         * imply a guarantee it is not making.
         */
        boolean signedWithDefaultSecret,

        /** Repeated on every response, because this is the one endpoint most likely to be misread. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning) {
}
