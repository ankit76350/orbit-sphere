package com.orbitastra.backend.common.time;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dates as a person reads them, for anything a person is going to read.
 *
 * <pre>
 * Dates.readable(Instant.parse("2027-10-08T16:31:00Z"), "Asia/Kolkata")
 *     -> "Friday 8 October 2027 10:01PM"
 * </pre>
 *
 * <p><b>Why this exists.</b> An {@code Instant} concatenated into a string prints its ISO-8601
 * form — {@code 2027-10-08T23:59:59Z} — and that was going into API messages a human being reads:
 * <i>"SUB/2026/09/000002 runs to 2027-10-08T23:59:59Z, which has not passed yet"</i>. Correct, and
 * nobody reads it. Every message that names a date now comes through here, so the format is one
 * decision in one place rather than thirty string concatenations that drift apart.
 *
 * <p><b>THE ZONE IS NOT COSMETIC — it decides the calendar day.</b> A billing period starts at
 * midnight in the school's own timezone, which is stored as an instant 5½ hours earlier for an
 * Indian school:
 *
 * <pre>
 * 2026-09-07T18:30:00Z  in UTC          -> "Monday 7 September 2026 6:30PM"
 * 2026-09-07T18:30:00Z  in Asia/Kolkata -> "Tuesday 8 September 2026 12:00AM"   &lt;- what the school means
 * </pre>
 *
 * <p>So <b>pass the school's zone whenever there is a school in hand</b>. Rendering that instant in
 * UTC would tell a school its period began on the 7th when its own calendar says the 8th, and
 * being off by a day is worse than being unreadable. The no-zone overload exists for the platform
 * catalogue, where a plan's selling window belongs to no school and UTC is the honest answer.
 *
 * <p><b>THE TIME IS ALWAYS SHOWN</b>, even when it looks redundant. Two of these messages compare
 * one date against another — "must be after", "is before the start of today" — and both ends can
 * fall on the same day. Dropping the time there produces <i>"(8 September 2026) must be after
 * (8 September 2026)"</i>, which reads as a contradiction and tells the caller nothing about what
 * to change.
 *
 * <p><b>{@code Locale.ENGLISH} is pinned deliberately, not left to the JVM.</b> The default locale
 * changes the output: {@code en_IN} renders "10:01pm" in lower case, and a JVM started elsewhere
 * would render the month name in another language. An API message is part of the contract, so it
 * cannot depend on how the process was launched.
 */
public final class Dates {

    /**
     * "Friday 8 October 2027 10:01PM". The weekday is included because these dates decide whether
     * a school is working, and a bare number does not say that.
     */
    private static final DateTimeFormatter WITH_TIME =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy h:mma", Locale.ENGLISH);

    /** "Friday 8 October 2027". A LocalDate has no time to show, so none is invented. */
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

    /**
     * What a null date reads as.
     *
     * <p>Sooner said than printed: several of these values are genuinely nullable — a subscription
     * mid-migration with no period end, a plan with no closing date — and concatenating one gave
     * the literal string "null" in a message somebody had to act on.
     */
    private static final String NOT_SET = "(not set)";

    private Dates() {
    }

    /**
     * An instant as the school's own calendar sees it.
     *
     * <p>{@code zone} is the school's {@code defaultTimeZone}. An unset or unrecognisable one
     * falls back to UTC rather than throwing: a message explaining a refusal must not itself fail,
     * and a wrong-by-hours date still tells the caller more than a 500 does.
     */
    public static String readable(Instant instant, String zone) {
        if (instant == null) {
            return NOT_SET;
        }

        return WITH_TIME.format(instant.atZone(zoneOrUtc(zone)));
    }

    /**
     * An instant in UTC, for the platform surface where no school owns the date.
     *
     * <p>Use the two-argument form wherever a school is in scope — see the note on this class about
     * the calendar day. This one is for plan selling windows and anything else the platform sets
     * for itself.
     */
    public static String readable(Instant instant) {
        return readable(instant, null);
    }

    /**
     * A calendar date, which is already a day and needs no zone.
     *
     * <p>Academic year and holiday dates are stored as {@code LocalDate} precisely because they
     * are days rather than moments. Converting one to an instant to print a time would invent
     * information.
     */
    public static String readable(LocalDate date) {
        if (date == null) {
            return NOT_SET;
        }

        return DATE_ONLY.format(date);
    }

    /**
     * The school's zone, or UTC when it is unset or unusable.
     *
     * <p>Inline in each method rather than shared with the callers that also need a ZoneId: this
     * one is about rendering, and a formatter that quietly failed on a bad zone string would take
     * a refusal message down with it.
     */
    private static ZoneId zoneOrUtc(String zone) {
        if (zone == null || zone.isBlank()) {
            return ZoneOffset.UTC;
        }

        try {
            return ZoneId.of(zone.trim());
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
