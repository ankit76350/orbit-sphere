package com.orbitastra.backend.services.plans.helper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Every check the plans module makes on what a caller sent.
 *
 * <p>One file per module, the same arrangement {@code CoreValidator} uses: when the next endpoint
 * needs to know whether a currency code is real, there is one place it already lives and no
 * decision to make about where to put it.
 *
 * <p>These throw rather than return a flag. A validator that returns false leaves every caller
 * to invent its own message and status code, and they drift.
 */
@Component
@RequiredArgsConstructor
public class PlanValidator {

    //! plan code — used by endpoints 1 and 2 -------------------------------------------------

    /** Uppercase letters, digits and single inner underscores. No leading or trailing one. */
    private static final Pattern PLAN_CODE_SHAPE =
            Pattern.compile("^[A-Z0-9](?:[A-Z0-9_]{0,38}[A-Z0-9])?$");

    /**
     * The plan code to store, given what the caller sent and what the plan is called.
     *
     * <p><b>Normally nobody sends a code.</b> It is derived from the name — "Premium Plus"
     * becomes {@code PREMIUM_PLUS} — so a create form asks for one thing rather than making an
     * operator type the same words twice in two shapes.
     *
     * <p>An explicit code is still accepted, for the case the derivation cannot help with: the
     * name that would produce a code somebody else already has, or a code that has to match
     * something outside this system.
     *
     * <p><b>Why the code exists at all, given the name is right there.</b> It is the family key:
     * the only thing joining version 1, 2 and 3 of one plan. A subscription stores a document id
     * and a version number, so without it "version 2" means version 2 of nothing, and copying a
     * published plan into a new version has no way to say which family the copy joins. The name
     * cannot do that job, because a name is display text somebody will want to change — and a
     * key that can change is not a key.
     *
     * @return the code to store, uppercased and normalized
     */
    public String resolvePlanCode(String rawCode, String name) {
        boolean derived = rawCode == null || rawCode.isBlank();
        String source = derived ? name : rawCode;

        if (source == null || source.isBlank()) {
            throw ApiException.badRequest("PLAN_CODE_REQUIRED",
                    "A plan code is required, and could not be worked out because the plan has "
                            + "no name either.");
        }

        // Anything that is not a letter or a digit becomes one underscore: spaces, hyphens,
        // ampersands, punctuation. Then the ends are trimmed, so "Premium (2026)" does not
        // produce a code ending in an underscore.
        String normalized = source.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40).replaceAll("_+$", "");
        }

        if (!PLAN_CODE_SHAPE.matcher(normalized).matches()) {
            // A name of nothing but punctuation cannot produce a code. Say which it was, because
            // "PLAN_CODE_INVALID" about a code the caller never sent is baffling otherwise.
            throw ApiException.conflict("PLAN_CODE_INVALID",
                    derived
                            ? "No plan code could be worked out from the name '" + name
                                    + "'. Send a planCode of letters, digits and inner "
                                    + "underscores."
                            : "A plan code must be 1 to 40 characters of letters, digits and "
                                    + "inner underscores. Received: " + normalized);
        }
        return normalized;
    }

    /**
     * The same normalization as {@link #resolvePlanCode}, for finding a plan rather than making
     * one. Never throws.
     *
     * <p>A code arrives in a URL, where it may have been typed by a person: {@code
     * /platform/plans/premium-plus/versions/1} should find {@code PREMIUM_PLUS}. What it must not
     * do is refuse — a code of the wrong shape simply matches no plan, and "no such plan" is a
     * 404, not a complaint about the shape of something the caller was reading off a link.
     */
    public String normalizePlanCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    //! money — used by endpoints 1 and 2 ----------------------------------------------

    /**
     * Validates and normalizes a price.
     *
     * <p><b>Zero is allowed and negative is not.</b> A free tier is a real plan; a plan we pay
     * the school to be on is not a thing.
     *
     * <p>More than two decimal places is refused rather than rounded. Rounding somebody's price
     * for them is how 1999.999 quietly becomes 2000.00 on every invoice for a year.
     *
     * @return the price at exactly two decimal places, so stored values compare predictably
     */
    public BigDecimal validatePrice(String label, BigDecimal raw) {
        if (raw == null) {
            throw ApiException.badRequest("PRICE_REQUIRED", label + " is required.");
        }
        if (raw.signum() < 0) {
            throw ApiException.badRequest("PRICE_NEGATIVE",
                    label + " cannot be negative. Received: " + raw.toPlainString());
        }
        if (raw.stripTrailingZeros().scale() > 2) {
            throw ApiException.badRequest("PRICE_TOO_PRECISE",
                    label + " cannot have more than two decimal places. Received: "
                            + raw.toPlainString());
        }
        return raw.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    /**
     * Validates and normalizes a currency code.
     *
     * <p>Checked against the JDK's ISO 4217 list rather than a hand-written one, for the same
     * reason time zones are: a three-letter code that looks plausible and does not exist — RUP,
     * INS — is a typo nobody notices until an invoice is issued in it.
     *
     * @return the code in upper case
     */
    public String validateCurrencyCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("CURRENCY_REQUIRED", "A currency code is required.");
        }
        String normalized = raw.trim().toUpperCase();
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException unknown) {
            throw ApiException.conflict("CURRENCY_INVALID",
                    "'" + normalized + "' is not an ISO 4217 currency code. Example: INR.");
        }
        return normalized;
    }

    //! limits and windows — used by endpoints 1 and 2 ----------------------------------------

    /**
     * A plan's student or user ceiling.
     *
     * <p>Must be at least one. A plan capped at zero students cannot be used by anybody, and the
     * entitlement service would block the first thing the school tried to do — which reads as a
     * bug in the platform rather than as the plan it was sold.
     */
    public void validateLimit(String label, Long value) {
        if (value == null) {
            throw ApiException.badRequest("LIMIT_REQUIRED", label + " is required.");
        }
        if (value < 1) {
            throw ApiException.badRequest("LIMIT_TOO_LOW",
                    label + " must be at least 1. Received: " + value);
        }
    }

    /**
     * The window in which a plan version may be sold.
     *
     * <p>Both ends are optional — a draft usually has neither, and #4 stamps
     * {@code effectiveFrom} when it publishes. Only the pair together can be wrong.
     */
    public void validateSellingWindow(Instant effectiveFrom, Instant effectiveUntil) {
        if (effectiveFrom == null || effectiveUntil == null) {
            return;
        }
        if (!effectiveFrom.isBefore(effectiveUntil)) {
            throw ApiException.badRequest("INVALID_SELLING_WINDOW",
                    "effectiveFrom (" + effectiveFrom + ") must be before effectiveUntil ("
                            + effectiveUntil + ").");
        }
    }
}
