package com.orbitastra.backend.dto.crm.inquiry.request;

import jakarta.validation.constraints.Size;

/**
 * What #15 asks with — <b>is this family already known?</b>
 *
 * <p><b>It is asked BEFORE every new lead</b>, by the person at the desk, which is the whole reason
 * #8 does not refuse duplicates itself. #8's own doc says so: refusing there would mean guessing
 * that two children sharing a phone number are one enquiry, which a family with two children is
 * not. <b>The judgement belongs to the person who can see the answer</b>, and this is what shows
 * them the answer.
 *
 * <p><b>One of the two is required.</b> A search with neither is a request to list every lead in
 * the school, which is #13's job and not this one's.
 *
 * <p><b>Both together is an OR, not an AND.</b> A family that gave a phone number last year and an
 * email this year is the same family, and a match on either is the thing worth seeing. Requiring
 * both would miss exactly the case the endpoint exists for.
 */
public record InquiryMatchRequest(

        /**
         * A phone number, in any shape.
         *
         * <p><b>Matched on its DIGITS, ignoring everything else.</b> {@code "+91 98765 43210"},
         * {@code "098765-43210"} and {@code "9876543210"} all find each other, because #8 stores
         * whatever the desk typed and the desk types it differently every time. A duplicate check
         * that only matched byte-identical strings would miss the duplicates it exists to catch —
         * which is the one failure that makes the endpoint pointless.
         *
         * <p><b>It matches at the END of the stored number</b>, so a query of ten digits finds a
         * stored number carrying a country code. The reverse also works: a stored ten digits is
         * found by a query that includes one.
         */
        @Size(max = 40) String phone,

        /**
         * An email address.
         *
         * <p><b>Matched whole and case-insensitively.</b> {@code "Priya@Example.com"} and
         * {@code "priya@example.com"} are the same mailbox, and a check that treated them as two
         * families would be wrong about the commonest way an address gets retyped.
         *
         * <p><b>Whole, not partial</b> — unlike #13's {@code search}, which matches anywhere. This
         * is a question about identity, not a lookup: {@code "a@b.com"} must not match
         * {@code "maria@b.com"} merely because the letters appear in it.
         */
        @Size(max = 160) String email) {
}
