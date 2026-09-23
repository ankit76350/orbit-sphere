package com.orbitastra.backend.dto.crm.admissionreview.request;

import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

/**
 * What #28 may be asked for. Every field is optional; sending none returns the school's reviews,
 * soonest due first.
 *
 * <p><b>Bound from the query string</b>, not a body — this is a GET.
 *
 * <p><b>The first two are the index, in its order.</b>
 * {@code school_reviewer_status_due_idx} is {@code {schoolId, reviewerDocsId, status, dueAt}} —
 * one person, one state, soonest first, which is exactly the queue this endpoint is named for.
 *
 * <p><b>There is no {@code schoolId} here, and there must never be one.</b> The school comes from
 * the caller's cookie.
 */
public record AdmissionReviewSearchRequest(

        /**
         * Whose queue. Example: "67aa15d9dc3f7d0088888888"
         *
         * <p><b>Absent returns every reviewer's</b>, which is the admissions office's view rather
         * than one person's. There is no "me" — nothing in this project knows who is calling yet.
         */
        String reviewerDocsId,

        /**
         * One state. Example: PENDING
         *
         * <p>Absent returns every state, {@code CANCELLED} ones included.
         */
        AdmissionReviewStatus status,

        /** One form's reviews, for somebody walking down from an application. */
        String admissionApplicationDocsId,

        /** One stage of the school's process. */
        Integer reviewRound,

        /**
         * What is late. Example: true
         *
         * <p><b>Two conditions, not one</b>: past its due date <i>and</i> still {@code PENDING} or
         * {@code IN_PROGRESS}. A review finished a month late also has a past due date and is not
         * owed by anybody.
         *
         * <p>{@code false} is the mirror — everything not late, which includes the finished ones
         * and the ones with no due date at all. Absent returns both.
         */
        Boolean overdue,

        Integer page,
        Integer size,
        String sort) {
}
