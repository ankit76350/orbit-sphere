package com.orbitastra.backend.common.current;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * The {@code idtoken} cookie: signing it, and reading it back.
 *
 * <h2>Why this is one class, having been deleted as one before</h2>
 *
 * <p>It was {@code IdTokenFactory}, deleted on 2026-09-19 because it had exactly one caller and
 * this project's rule is that single-use logic stays inline. <b>It now has two</b> —
 * {@code LocalUserController} signs, {@link CurrentSchoolResolver} and
 * {@link CurrentUserResolver} read — and two callers needing one secret is precisely the threshold
 * that rule sets for extracting. Two copies of a signing secret is two places for it to drift, and
 * the failure when it does is every token being rejected by the thing that issued it.
 *
 * <h2>Why there is no JWT library, and why that was nearly the wrong call</h2>
 *
 * <p>The build has no JOSE dependency. Signing needs one thing — HMAC-SHA256 over
 * {@code base64url(header) + "." + base64url(claims)} — which is exactly what the JDK's
 * {@link Mac} does, so adding {@code nimbus-jose-jwt} to the pom would change the build for
 * everybody to produce a string the JDK already produces.
 *
 * <p><b>That reasoning ends where verification begins</b>, and this class now verifies. Parsing an
 * attacker-controlled JWT is precisely where a library earns its place, and doing it by hand is how
 * the well-known JWT vulnerabilities happen. It is written by hand anyway because the surface here
 * is small and fixed — one algorithm, no key rotation, no {@code kid}, no {@code jku}, no nested
 * tokens, none of the features that are where those vulnerabilities actually live — and because
 * every one of the classic mistakes is named and defended against below. <b>Add the library the
 * moment that stops being true</b>: another algorithm, asymmetric keys, or tokens issued anywhere
 * but {@code POST /local-user}.
 *
 * <h2>Verifying is not the mirror of signing, and this is where JWTs go wrong</h2>
 *
 * <p>Signing is arithmetic. <b>Verifying reads attacker-controlled input</b>, and the well-known
 * JWT vulnerabilities all live here. What this does about each:
 *
 * <ul>
 *   <li><b>{@code alg: none} and algorithm confusion.</b> The header's {@code alg} is <i>never
 *       read</i>. HS256 is recomputed unconditionally, so a token claiming another algorithm — or
 *       none — simply fails the comparison like any other forgery. Trusting that field is the
 *       single most common way this is got wrong.</li>
 *   <li><b>Timing.</b> The comparison is {@link MessageDigest#isEqual}, which does not stop at the
 *       first differing byte.</li>
 *   <li><b>Expiry.</b> {@code exp} is checked. A signature that is valid forever is a session that
 *       never ends.</li>
 *   <li><b>Malformed input.</b> Anything unparseable is a refusal, never a 500 — a caller can put
 *       any bytes in a cookie, and a stack trace is not an answer.</li>
 * </ul>
 *
 * <h2>What a valid token still does not prove</h2>
 *
 * <p><b>That its claims are true.</b> {@code POST /local-user} issues a token to anybody who asks,
 * for any {@code schoolId}, because nothing authenticates the caller. Verification here proves the
 * token was issued by this server and has not been edited since. It does not — and cannot — prove
 * the person holding it is entitled to that school.
 *
 * <p>That is not a regression: the header this replaces was plain text anybody could set. It
 * becomes a real boundary only when {@code POST /local-user} stops issuing tokens to unauthenticated
 * callers, and that is the piece still missing.
 */
@Component
public class IdTokenCookie {

    private static final Logger log = LoggerFactory.getLogger(IdTokenCookie.class);

    /** The cookie, and the token inside it. */
    public static final String COOKIE_NAME = "idtoken";

    /** What an unconfigured deployment signs with, and the reason for the warning below. */
    public static final String UNSAFE_DEFAULT_SECRET =
            "orbit-sphere-local-development-secret-change-me";

    private static final String ALGORITHM = "HmacSHA256";

    /** Where the verified claims are parked for the rest of the request. */
    private static final String CLAIMS_ATTRIBUTE = IdTokenCookie.class.getName() + ".claims";

    /** Parked separately, because "verified, and empty" and "no cookie" are different answers. */
    private static final String NO_COOKIE_ATTRIBUTE = IdTokenCookie.class.getName() + ".absent";

    /** {@code {"alg":"HS256","typ":"JWT"}}, which never varies, so it is encoded once. */
    private static final String JWT_HEADER = encode(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    //! JACKSON 3 - tools.jackson.databind, NOT com.fasterxml.jackson.databind. Only
    //! jackson-annotations is still 2.x under the old package.
    private final ObjectMapper json;
    private final String secret;
    private final String issuer;

    public IdTokenCookie(
            ObjectMapper json,
            @Value("${app.local-user.jwt-secret:" + UNSAFE_DEFAULT_SECRET + "}") String secret,
            @Value("${app.local-user.jwt-issuer:orbit-sphere}") String issuer) {
        this.json = json;
        this.secret = secret;
        this.issuer = issuer;
    }

    /** Said once at startup rather than on every token. */
    @PostConstruct
    void warnAboutTheDefaultSecret() {
        if (usingDefaultSecret()) {
            log.warn("id tokens are being signed with the built-in development secret, and the "
                    + "tenant is now resolved from them. Set app.local-user.jwt-secret.");
        }
    }

    /** Whether the built-in secret is in use, so a response can say so rather than implying safety. */
    public boolean usingDefaultSecret() {
        return UNSAFE_DEFAULT_SECRET.equals(secret);
    }

    /**
     * Sign these claims into an HS256 JWT.
     *
     * <p><b>{@code iss}, {@code iat} and {@code exp} are added here, after the caller's claims.</b>
     * They are facts about the token rather than about whoever it describes, and putting them last
     * is what stops a caller-supplied {@code exp} minting a token that never expires.
     */
    public String sign(Map<String, String> claims, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>(claims);
        payload.put("iss", issuer);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String body = JWT_HEADER + '.' + encode(json.writeValueAsBytes(payload));
        return body + '.' + encode(hmac(body));
    }

    /**
     * The verified claims on this request, or null when there is no {@code idtoken} cookie.
     *
     * <p><b>Absent and invalid are different answers.</b> No cookie returns null, so a caller can
     * fall back to something else. A cookie that is present but forged, corrupt or expired
     * <i>throws</i>: it is a caller asserting something untrue, and quietly treating that as
     * "not signed in" would hide the one case worth seeing.
     */
    public Map<String, String> read(HttpServletRequest request) {
        String raw = rawCookie(request);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split("\\.");
        if (parts.length != 3) {
            throw ApiException.badRequest("ID_TOKEN_MALFORMED",
                    "The " + COOKIE_NAME + " cookie is not a JWT. Sign in again.");
        }

        //! THE HEADER'S alg IS NEVER READ. HS256 is recomputed unconditionally, so a token that
        //! says "alg": "none" - or RS256, or anything else - fails this comparison like any other
        //! forgery. Believing that field is the single most common way JWT verification is broken.
        String signingInput = parts[0] + '.' + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] presented;
        try {
            presented = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("ID_TOKEN_MALFORMED",
                    "The " + COOKIE_NAME + " cookie's signature is not valid base64url.");
        }

        //! CONSTANT TIME. Arrays.equals stops at the first differing byte, which leaks how much of
        //! a guessed signature was right.
        //!
        //! NOT COVERED BY ANY TEST, AND CANNOT BE. Swapping this for Arrays.equals was mutation M7
        //! and it survived both suites, because the two are identical in everything except how long
        //! they take - and a test that measured that would be a flake. It is pinned here instead:
        //! if you are reading this because a linter suggested Arrays.equals, the answer is no.
        if (!MessageDigest.isEqual(expected, presented)) {
            throw ApiException.badRequest("ID_TOKEN_INVALID",
                    "The " + COOKIE_NAME + " cookie's signature does not match. It was not issued "
                            + "by this server, or it has been edited. Sign in again.");
        }

        //! Map<?, ?>, NOT Map<String, Object>. readValue(bytes, Map.class) hands back a raw Map, and
        //! declaring the target with type arguments only buys an unchecked warning - a promise the
        //! compiler cannot keep about JSON a caller wrote. A wildcard is the honest type; nothing
        //! below needs more than get and forEach.
        Map<?, ?> payload;
        try {
            payload = json.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
        } catch (IllegalArgumentException | JacksonException e) {
            //! JACKSON 3's exceptions are UNCHECKED, so nothing forces this catch to exist - which
            //! is exactly why it has to be written deliberately. Without it a cookie holding
            //! "aGVsbG8" is a 500.
            throw ApiException.badRequest("ID_TOKEN_MALFORMED",
                    "The " + COOKIE_NAME + " cookie's claims could not be read.");
        }

        //! EXPIRY IS CHECKED. A signature that is valid forever is a session that never ends, and
        //! the exp claim is worthless unless something reads it.
        Object exp = payload.get("exp");
        if (!(exp instanceof Number expiry)) {
            throw ApiException.badRequest("ID_TOKEN_MALFORMED",
                    "The " + COOKIE_NAME + " cookie has no expiry.");
        }
        if (Instant.now().getEpochSecond() >= expiry.longValue()) {
            throw ApiException.badRequest("ID_TOKEN_EXPIRED",
                    "The " + COOKIE_NAME + " cookie expired. Sign in again.");
        }

        //! Strings only. Everything this token carries is an id or a year, and a caller that put a
        //! nested object in an extra should not be able to make a resolver read one.
        Map<String, String> claims = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (key instanceof String name && value instanceof String text) {
                claims.put(name, text);
            }
        });
        return claims;
    }

    /**
     * One trimmed claim from the verified token, or null when it is absent, blank, or there is no
     * cookie at all.
     *
     * <p>This is what the resolvers call. Blank collapses to null deliberately: signing in with no
     * staff member chosen sends {@code staffDocsId: ""}, and "chose nobody" and "did not say" are
     * the same answer to every caller here.
     */
    public String claim(HttpServletRequest request, String name) {
        Map<String, String> claims = verifiedClaims(request);
        if (claims == null) {
            return null;
        }
        String value = claims.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * {@link #read} once per request, not once per claim.
     *
     * <p>Two resolvers each want a different claim, and without this every one of them would redo
     * an HMAC and a JSON parse over the same bytes. The attribute lives for exactly one request, so
     * there is nothing to invalidate.
     *
     * <p><b>Only a usable result is cached.</b> A bad cookie throws, and throwing again on the next
     * ask is the same answer for the same reason — caching the failure would save nothing and make
     * the stack trace lie about where it came from.
     */
    private Map<String, String> verifiedClaims(HttpServletRequest request) {
        Object cached = request.getAttribute(CLAIMS_ATTRIBUTE);
        if (cached instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, String> claims = (Map<String, String>) cached;
            return claims;
        }
        if (Boolean.TRUE.equals(request.getAttribute(NO_COOKIE_ATTRIBUTE))) {
            return null;
        }

        Map<String, String> claims = read(request);
        if (claims == null) {
            request.setAttribute(NO_COOKIE_ATTRIBUTE, Boolean.TRUE);
        } else {
            request.setAttribute(CLAIMS_ATTRIBUTE, claims);
        }
        return claims;
    }

    /** The cookie's raw value, or null. */
    private static String rawCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            //! UNREACHABLE WITH A NON-EMPTY SECRET. HmacSHA256 is required of every JDK, so
            //! getInstance cannot fail; init only rejects an empty key.
            throw new IllegalStateException("could not sign or verify the id token", e);
        }
    }

    /** Base64url, unpadded — what a JWT uses, and what {@code Base64.getUrlEncoder} does not do. */
    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
