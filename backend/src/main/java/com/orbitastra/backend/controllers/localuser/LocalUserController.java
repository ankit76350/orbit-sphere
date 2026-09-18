package com.orbitastra.backend.controllers.localuser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.localuser.request.LocalUserContextRequest;
import com.orbitastra.backend.dto.localuser.response.LocalUserContextResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Remembers, in the browser, who a tester is pretending to be.
 *
 * <h2>THIS IS NOT AUTHENTICATION, AND NOTHING MAY EVER TREAT IT AS SUCH</h2>
 *
 * <p>Everything this endpoint stores was handed to it by the caller and is written to the cookie
 * <b>unsigned and unencrypted</b>. Anybody can open developer tools, change {@code schoolId} to
 * another school's id, and send it back. That is not a flaw to be fixed later with a bigger cookie —
 * it is what a client-supplied value <i>is</i>.
 *
 * <p>So, concretely, the line that must not be crossed:
 *
 * <p><b>{@link com.orbitastra.backend.common.current.CurrentSchoolResolver} must never read this
 * cookie.</b> The day it does, every tenant boundary in this codebase becomes a text field the
 * caller controls, and every {@code schoolId} check that the repositories carry — the ones that
 * exist precisely so one school cannot reach another's data — is satisfied with whatever the
 * attacker typed. The tenant still comes from {@code X-School-Subdomain}, which is no more
 * trustworthy but is at least not <i>presented</i> as identity.
 *
 * <p>What this is for: a tester on the API tool stops retyping three ids into every request. That
 * is the whole of it.
 *
 * <h2>Two things were measured before this was written — 2026-09-18</h2>
 *
 * <p><b>1. A cookie set on a direct cross-origin call is dropped by the browser.</b>
 * {@code DevCorsConfig} sets {@code allowCredentials(false)}, and without credentials a browser
 * ignores {@code Set-Cookie} on a cross-origin XHR — silently, with a 200 in the network tab. The
 * tester reaches the backend through its Vite proxy, which makes the call same-origin and works;
 * a page calling {@code http://localhost:3456} straight would appear to succeed and store nothing.
 *
 * <p><b>2. The tester's proxy only forwards {@code /platform} and {@code /schools}.</b> A path of
 * {@code /local-user} would have been answered by the dev server with its own 404 and never reached
 * Spring at all, so a proxy rule for it was added alongside this.
 *
 * <h2>Why there is no service</h2>
 *
 * <p>Nothing is read and nothing is written. There is no domain operation here — only an HTTP
 * header built from a request body — so a service would be a pass-through with a name. The encoding
 * and the size check are single-use, which this project's folder rules keep inline.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/local-user")
public class LocalUserController {

    /** What the cookie is called. Prefixed, so it cannot collide with the dev server's own. */
    public static final String COOKIE_NAME = "orbit_local_user";

    /**
     * The most an encoded cookie value may come to.
     *
     * <p><b>Browsers drop an oversized cookie without telling anybody</b> — no error, no header, it
     * simply never arrives. Roughly 4 KB is the common limit for the whole {@code name=value} pair,
     * so this leaves comfortable room for the name and the attributes and refuses anything larger
     * rather than setting one that quietly vanishes.
     */
    private static final int MAX_ENCODED_VALUE = 3_500;

    /** Repeated on every response, because this is the endpoint most likely to be misread. */
    private static final String NOT_A_CREDENTIAL =
            "This cookie is unsigned and caller-supplied: it records what somebody CLAIMS to be, "
                    + "not who they are. It must never be used for authentication or to resolve a "
                    + "tenant.";

    //! JACKSON 3, so the type is tools.jackson.databind.ObjectMapper - NOT
    //! com.fasterxml.jackson.databind, which is what every example on the internet says and what
    //! this file was written with first. Only jackson-annotations is still 2.x under the old
    //! package, which is why @JsonInclude on the DTOs looks like it disagrees. GlobalExceptionHandler
    //! carries the same note.
    private final ObjectMapper json;

    /**
     * Store the caller's acting context in a cookie, and echo back what was stored.
     *
     * <p><b>Always sets a cookie</b>, on every call. Sending an empty body stores an empty context,
     * which is a real thing to want; sending {@code maxAgeSeconds: 0} expires it, which is how a
     * context is cleared without a second endpoint existing.
     *
     * <p><b>The attributes, and why each one.</b> {@code Path=/} so it is sent to every API call.
     * {@code HttpOnly} so a script on the page cannot read it — the response body is how a caller
     * sees what was stored, so nothing needs to. {@code SameSite=Lax}, which is enough for the
     * tester because {@code localhost:1400} and {@code localhost:3456} are the same site even
     * though they are different origins. {@code Secure} <b>only when the request itself was
     * HTTPS</b>: a Secure cookie is discarded on an {@code http://} page, so setting it
     * unconditionally would mean the cookie silently never arrived in local development.
     *
     * <p><b>No gates.</b> This touches no school and no subscription — there is nothing for a gate
     * to ask about, and requiring a usable subscription to remember an id in a browser would be
     * asking the database a question about a string.
     */
    @PostMapping
    public ResponseEntity<LocalUserContextResponse> storeContext(
            @Valid @RequestBody(required = false) LocalUserContextRequest request,
            HttpServletRequest httpRequest) {

        //! step 1 - an absent body is an empty context, not a refusal. `required = false` above is
        //! what allows POST with no body at all, which is the shortest way to clear the three
        //! named fields while keeping the cookie.
        LocalUserContextRequest sent = request == null
                ? new LocalUserContextRequest(null, null, null, null, null)
                : request;

        //! step 2 - what goes in. Only the fields that carry something: a cookie holding
        //! {"staffDocsId":""} would read back as a staff member whose id is the empty string,
        //! which is not the same fact as "no staff member was chosen".
        Map<String, String> stored = new LinkedHashMap<>();
        putIfPresent(stored, "staffDocsId", sent.staffDocsId());
        putIfPresent(stored, "schoolId", sent.schoolId());
        putIfPresent(stored, "academicYear", sent.academicYear());

        for (Map.Entry<String, String> one : sent.safeExtra().entrySet()) {
            String key = TextHelper.blankToNull(one.getKey());
            if (key == null) {
                throw ApiException.badRequest("BLANK_EXTRA_KEY",
                        "An entry in 'extra' has a blank name. A value needs something to be "
                                + "called before it can be read back out.");
            }
            //! THE THREE NAMED FIELDS WIN. Letting 'extra' overwrite schoolId would mean two
            //! places in one request setting one value, and the answer would depend on map order.
            if (stored.containsKey(key)) {
                throw ApiException.badRequest("EXTRA_KEY_RESERVED",
                        "'" + key + "' is already set as a named field, so it cannot also be sent "
                                + "in 'extra'. Send it once.");
            }
            putIfPresent(stored, key, one.getValue());
        }

        //! step 3 - the value. JSON so it stays readable in developer tools, URL-encoded because a
        //! raw cookie value may not contain a comma, a semicolon, a space or a quote - all of
        //! which JSON produces on its own.
        String encoded;
        try {
            encoded = URLEncoder.encode(json.writeValueAsString(stored), StandardCharsets.UTF_8);
        } catch (JacksonException e) {
            //! UNCHECKED IN JACKSON 3, so this catch is belt and braces rather than required: a
            //! flat Map<String, String> has nothing in it that can fail to serialise. It is here
            //! so that if that ever stops being true the answer is a 400 naming the field, not a
            //! 500 naming a library.
            throw ApiException.badRequest("CONTEXT_NOT_ENCODABLE",
                    "That context could not be written as JSON: " + e.getMessage());
        }

        //! step 4 - refuse what the browser would drop. An oversized cookie is discarded silently,
        //! so a 200 here with nothing stored would be the worst possible answer.
        if (encoded.length() > MAX_ENCODED_VALUE) {
            throw ApiException.badRequest("CONTEXT_TOO_LARGE",
                    "That context encodes to " + encoded.length() + " characters and the limit is "
                            + MAX_ENCODED_VALUE + ". A browser drops an oversized cookie without "
                            + "reporting it, so this is refused rather than stored and lost.");
        }

        int maxAge = sent.resolvedMaxAgeSeconds();
        if (maxAge < 0) {
            throw ApiException.badRequest("INVALID_MAX_AGE",
                    "maxAgeSeconds cannot be negative. Send 0 to expire the cookie now, or leave "
                            + "it out for the default.");
        }

        //! step 5 - SECURE FOLLOWS THE REQUEST'S OWN SCHEME. Hard-coding it true would mean the
        //! cookie is thrown away by every browser on http://localhost, which is where this is
        //! used - and the call would still answer 200.
        boolean secure = httpRequest.isSecure();

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, encoded)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAge))
                .build();

        //! step 6 - the answer. The body repeats the cookie because HttpOnly means the page that
        //! called this cannot read it, and because curl and Postman are callers too.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LocalUserContextResponse(
                        COOKIE_NAME,
                        stored,
                        encoded.length(),
                        maxAge,
                        secure,
                        NOT_A_CREDENTIAL));
    }

    /**
     * Put a value in only when it carries something.
     *
     * <p>Blank is absent, the same reading the rest of this project uses: a stored empty string is
     * a value, and "no staff member was chosen" is not one.
     */
    private static void putIfPresent(Map<String, String> into, String key, String value) {
        String trimmed = TextHelper.blankToNull(value);
        if (trimmed != null) {
            into.put(key, trimmed);
        }
    }
}
