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

    //! plan code — used by endpoint 1 -------------------------------------------------

    /** Uppercase letters, digits and single inner underscores. No leading or trailing one. */
    private static final Pattern PLAN_CODE_SHAPE =
            Pattern.compile("^[A-Z0-9](?:[A-Z0-9_]{0,38}[A-Z0-9])?$");

    /**
     * Validates and normalizes a plan code.
     *
     * <p>Uppercased rather than refused for being lowercase, because {@code premium} and
     * {@code PREMIUM} are obviously the same intent and the unique index cannot tell — two
     * documents would exist and a school could be sold either.
     *
     * @return the normalized code
     */
    public String validatePlanCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("PLAN_CODE_REQUIRED", "A plan code is required.");
        }
        String normalized = raw.trim().toUpperCase().replaceAll("[\\s-]+", "_");

        if (!PLAN_CODE_SHAPE.matcher(normalized).matches()) {
            throw ApiException.conflict("PLAN_CODE_INVALID",
                    "A plan code must be 1 to 40 characters of letters, digits and inner "
                            + "underscores. Received: " + normalized);
        }
        return normalized;
    }

    //! money — used by endpoint 1 -----------------------------------------------------

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

    //! limits and windows — used by endpoint 1 ----------------------------------------

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
