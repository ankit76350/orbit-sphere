package com.orbitastra.backend.controllers.localuser;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import tools.jackson.databind.ObjectMapper;

/**
 * Remembers, in the browser, who a tester is pretending to be.
 *
 * <h2>THIS IS NOT AUTHENTICATION, AND THE SIGNATURE DOES NOT MAKE IT SO</h2>
 *
 * <p>Since 2026-09-19 the cookie holds a <b>signed HS256 JWT</b> rather than plain JSON, and that
 * changes exactly one thing: a token can no longer be <i>edited</i> in developer tools without the
 * signature failing.
 *
 * <p><b>It changes nothing about who may ask for one.</b> Nothing authenticates the caller, so
 * anybody can POST any {@code schoolId} and receive a validly signed token asserting it. The
 * signature proves <i>this server issued the token</i>. It says nothing about whether the claims
 * inside are true, because the server had no way to check them and did not try.
 *
 * <p>That distinction is the whole of it: <b>tamper-evident, not trustworthy</b>.
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
 * <p><b>The cookie is called {@code idtoken}</b> and holds the JWT. Its three claims are
 * {@code schoolId}, {@code staffDocsId} and {@code academicYear}, plus the standard {@code iss},
 * {@code iat} and {@code exp} — and any extras the caller sent.
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
 * <p>Nothing is read and nothing is written. There is no domain operation here — only a token and
 * an HTTP header built from a request body — so a service would be a pass-through with a name.
 *
 * <p><b>The signing is in this file too</b>, rather than in a class of its own. It is used by one
 * endpoint and nothing else, which is what this project's folder rules mean by single-use staying
 * inline — and a three-method helper reachable from one caller is a file to open rather than a
 * seam worth having.
 */
@RestController
@RequestMapping("/local-user")
public class LocalUserController {

    private static final Logger log = LoggerFactory.getLogger(LocalUserController.class);

    /** What the cookie is called, and what the token inside it is called. */
    public static final String COOKIE_NAME = "idtoken";

    /**
     * The most a token may come to.
     *
     * <p><b>Browsers drop an oversized cookie without telling anybody</b> — no error, no header, it
     * simply never arrives. Roughly 4 KB is the common limit for the whole {@code name=value} pair,
     * so this leaves comfortable room for the name and the attributes and refuses anything larger
     * rather than setting one that quietly vanishes.
     *
     * <p>A JWT is <b>bigger than the JSON it carries</b> — base64url costs a third on top, and the
     * header, signature and three standard claims are about 150 characters before any of the
     * caller's data — so this bites sooner than it did when the cookie held raw JSON.
     */
    private static final int MAX_ENCODED_VALUE = 3_500;

    /** Repeated on every response, because this is the endpoint most likely to be misread. */
    private static final String NOT_A_CREDENTIAL =
            "The signature proves this server issued the token, NOT that its claims are true: "
                    + "nothing authenticates the caller, so anybody can ask for a token saying "
                    + "anything. It must never be used for authentication or to resolve a tenant.";

    /** What an unconfigured deployment signs with, and the reason for the warning below. */
    static final String UNSAFE_DEFAULT_SECRET = "orbit-sphere-local-development-secret-change-me";

    private static final String ALGORITHM = "HmacSHA256";

    /** {@code {"alg":"HS256","typ":"JWT"}}, which never varies, so it is encoded once. */
    private static final String JWT_HEADER = encode(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    //! JACKSON 3, so the type is tools.jackson.databind.ObjectMapper - NOT
    //! com.fasterxml.jackson.databind, which is what every example on the internet says. Only
    //! jackson-annotations is still 2.x under the old package, which is why @JsonInclude on the
    //! DTOs looks like it disagrees. GlobalExceptionHandler carries the same note.
    private final ObjectMapper json;
    private final String secret;
    private final String issuer;

    public LocalUserController(
            ObjectMapper json,
            @Value("${app.local-user.jwt-secret:" + UNSAFE_DEFAULT_SECRET + "}") String secret,
            @Value("${app.local-user.jwt-issuer:orbit-sphere}") String issuer) {
        this.json = json;
        this.secret = secret;
        this.issuer = issuer;
    }

    /**
     * Say so once, at startup, rather than on every token.
     *
     * <p>A signing secret that ships as its own default signs tokens anybody with this source can
     * forge. That matters less here than it usually would — the endpoint issues a token to anybody
     * who asks anyway — but the day one of these is trusted for anything, this is the line that
     * will have made it worthless, and a warning in the log is cheaper than finding out later.
     */
    @PostConstruct
    void warnAboutTheDefaultSecret() {
        if (UNSAFE_DEFAULT_SECRET.equals(secret)) {
            log.warn("id tokens are being signed with the built-in development secret. "
                    + "Set app.local-user.jwt-secret before anything relies on the signature.");
        }
    }

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

        int maxAge = sent.resolvedMaxAgeSeconds();
        if (maxAge < 0) {
            throw ApiException.badRequest("INVALID_MAX_AGE",
                    "maxAgeSeconds cannot be negative. Send 0 to expire the cookie now, or leave "
                            + "it out for the default.");
        }

        //! step 3 - the token. ITS EXPIRY IS THE COOKIE'S, deliberately: two lifetimes for one
        //! thing is how a browser ends up holding a cookie whose token expired an hour ago, or
        //! keeping a token past the moment the cookie was meant to go.
        //!
        //! A LIFETIME OF ZERO MINTS NOTHING. maxAgeSeconds 0 means "clear this", and a token that
        //! is already expired at the instant it is signed is a thing nobody wants to read in a
        //! decoder. The cookie is set to an empty value and expired instead.
        String token = "";
        Instant expiresAt = null;

        if (maxAge > 0) {
            Instant issuedAt = Instant.now();
            expiresAt = issuedAt.plus(Duration.ofSeconds(maxAge));
            token = sign(stored, issuedAt, expiresAt);

            //! step 4 - refuse what the browser would drop. An oversized cookie is discarded
            //! silently, so a 200 here with nothing stored would be the worst possible answer.
            //!
            //! A JWT IS BIGGER THAN ITS CLAIMS - base64url costs a third, and the header,
            //! signature and three standard claims are ~150 characters before the caller's data -
            //! so this bites sooner than it did when the cookie held raw JSON.
            if (token.length() > MAX_ENCODED_VALUE) {
                throw ApiException.badRequest("CONTEXT_TOO_LARGE",
                        "That context signs to a token of " + token.length() + " characters and "
                                + "the limit is " + MAX_ENCODED_VALUE + ". A browser drops an "
                                + "oversized cookie without reporting it, so this is refused "
                                + "rather than stored and lost.");
            }
        }

        //! step 5 - SECURE FOLLOWS THE REQUEST'S OWN SCHEME. Hard-coding it true would mean the
        //! cookie is thrown away by every browser on http://localhost, which is where this is
        //! used - and the call would still answer 200.
        boolean secure = httpRequest.isSecure();

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAge))
                .build();

        //! step 6 - the answer. The body repeats the token and its claims because HttpOnly means
        //! the page that called this cannot read the cookie, and because curl and Postman are
        //! callers too. Handing the token back is not a leak: the caller supplied every claim in
        //! it, and it is issued to anybody who asks.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LocalUserContextResponse(
                        COOKIE_NAME,
                        token.isEmpty() ? null : token,
                        stored,
                        token.length(),
                        maxAge,
                        expiresAt,
                        secure,
                        UNSAFE_DEFAULT_SECRET.equals(secret),
                        NOT_A_CREDENTIAL));
    }

    /**
     * Sign these claims into an HS256 JWT.
     *
     * <h2>A SIGNATURE PROVES WHO WROTE THE TOKEN, NOT THAT THE CLAIMS ARE TRUE</h2>
     *
     * <p>Worth repeating beside the code that does it: the caller says {@code schoolId} and gets a
     * token that validly asserts it, because nothing authenticated the caller. This stops a token
     * being <i>edited</i> after it is issued and does nothing about one being <i>asked for</i>.
     *
     * <h2>Why there is no JWT library</h2>
     *
     * <p>The build has no JOSE dependency, and this needs one thing: HMAC-SHA256 over
     * {@code base64url(header) + "." + base64url(claims)}, which is exactly what the JDK's
     * {@link Mac} does. Adding {@code nimbus-jose-jwt} to the pom for it would change the build for
     * everybody so that a dev-convenience endpoint can produce a string the JDK already produces.
     *
     * <p><b>That reasoning ends the moment anything needs to VERIFY a token.</b> Parsing an
     * attacker-controlled JWT — algorithm confusion, {@code alg: none}, claim type coercion — is
     * precisely where a library earns its place, and writing that by hand is how the well-known JWT
     * vulnerabilities happen. This method only signs.
     *
     * <p><b>{@code iss}, {@code iat} and {@code exp} are added HERE, after the caller's claims.</b>
     * They are facts about the token rather than about whoever it describes, and putting them last
     * is what stops an {@code extra} called {@code exp} from minting a token that never expires.
     */
    private String sign(Map<String, String> claims, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>(claims);
        payload.put("iss", issuer);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String body = JWT_HEADER + '.' + encode(json.writeValueAsBytes(payload));
        return body + '.' + encode(hmac(body));
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            //! UNREACHABLE WITH A NON-EMPTY SECRET. HmacSHA256 is required of every JDK, so
            //! getInstance cannot fail; init only rejects an empty key. Rethrown rather than
            //! swallowed so that if it ever does happen it is a 500 with a cause, not a null token.
            throw new IllegalStateException("could not sign the id token", e);
        }
    }

    /** Base64url, unpadded — what a JWT uses, and what {@code Base64.getUrlEncoder} does not do. */
    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
