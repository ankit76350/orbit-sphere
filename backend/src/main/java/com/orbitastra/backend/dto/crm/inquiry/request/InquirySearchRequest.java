package com.orbitastra.backend.dto.crm.inquiry.request;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

/**
 * What #13 filters by — <b>the counsellor's worklist</b>.
 *
 * <p><b>{@code status} is the one the index leads with</b>, in its order:
 * {@code school_inquiry_pipeline_idx} is {@code {schoolId, status, nextFollowUpAt}}.
 *
 * <p><b>It filtered by counsellor until 2026-09-24</b>, and does not any more: a lead is no longer
 * owned by anybody. That field and #11 — the endpoint that would have set it — were removed
 * together, so "whose worklist" is a question this module no longer asks. What is left is the two
 * that matter most anyway: <i>what state</i>, and <i>what is late</i>.
 *
 * <p><b>{@code overdue} is the one that matters.</b> A follow-up date in the past, on a lead that
 * is not finished. A past date alone is not enough: a lead somebody closed last month has one too,
 * and nobody needs chasing about it.
 */
public record InquirySearchRequest(

        String academicYear,

        InquiryStatus status,

        /** Past its follow-up date and still open. Not simply "has a past date". */
        Boolean overdue,

        /**
         * The child's name <b>or</b> the inquiry number, anywhere, ignoring case.
         *
         * <p>Two fields because a desk looks a lead up by either: the parent gives a name on the
         * phone, the note on the pad carries a number. The same call #24 makes.
         */
        String search,

        Integer page,
        Integer size,
        String sort) {
}
