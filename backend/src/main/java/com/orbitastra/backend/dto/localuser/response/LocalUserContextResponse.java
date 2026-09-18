package com.orbitastra.backend.dto.localuser.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What was put in the cookie, echoed back.
 *
 * <h2>The body repeats the cookie on purpose</h2>
 *
 * <p>The cookie is {@code HttpOnly}, so the page that called this cannot read it back with
 * JavaScript. Without this echo a caller would have to open developer tools to find out what it had
 * just stored — and in an API testing tool, "what did that actually do" is the whole question.
 *
 * <p>It is also what makes the call useful to a caller that is <b>not</b> a browser. {@code curl}
 * and Postman get the same answer in the body that a browser gets in a header.
 */
public record LocalUserContextResponse(

        /** The cookie's name, so a caller knows what to look for or clear. */
        String cookieName,

        /** Exactly what the cookie now holds. */
        Map<String, String> stored,

        /** How many characters the encoded cookie value came to, against the browser's ~4 KB cap. */
        int encodedLength,

        /** How long it will live, in seconds. Zero means it was expired immediately. */
        int maxAgeSeconds,

        /**
         * Whether the cookie was marked {@code Secure}.
         *
         * <p><b>False over plain HTTP, and that is not a bug.</b> A {@code Secure} cookie is
         * discarded by the browser on an {@code http://} page, so setting it on
         * {@code http://localhost} would mean the cookie silently never arrived. It follows the
         * request's own scheme.
         */
        boolean secure,

        /** Repeated on every response, because this is the one endpoint most likely to be misread. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning) {
}
