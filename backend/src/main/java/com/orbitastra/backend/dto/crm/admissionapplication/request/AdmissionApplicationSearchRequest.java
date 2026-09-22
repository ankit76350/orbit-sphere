package com.orbitastra.backend.dto.crm.admissionapplication.request;

import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * What #24 may be asked for. Every field is optional; sending none returns the school's
 * applications, newest first.
 *
 * <p><b>Bound from the query string</b>, not a body — this is a GET.
 *
 * <p><b>The four named filters are the ones the index serves.</b>
 * {@code school_cycle_class_status_idx} is
 * {@code {schoolId, admissionCycleDocsId, appliedClassDocsId, status, submittedAt}} — cycle, class
 * and status in that order, which is exactly the worklist an admission officer opens.
 *
 * <p><b>There is no {@code schoolId} here, and there must never be one.</b> The school comes from
 * the caller's cookie.
 */
public record AdmissionApplicationSearchRequest(

        /** One round's applications. Example: "67aa15d9dc3f7d0011111111" */
        String admissionCycleDocsId,

        /** One class's applicants. Example: "67aa15d9dc3f7d0033333333" */
        String appliedClassDocsId,

        /**
         * One stage of the pipeline. Example: SUBMITTED
         *
         * <p>Absent returns every status, including {@code DRAFT} forms nobody has submitted and
         * {@code WITHDRAWN} ones the family pulled out of.
         */
        AdmissionApplicationStatus status,

        /**
         * Whose worklist. Example: "67aa15d9dc3f7d0044444444"
         *
         * <p>An officer is assigned by #22, which is not built, so every application currently has
         * none — and this filter returns nothing for any id until it is.
         */
        String assignedAdmissionOfficerDocsId,

        /**
         * The applicant's name or the application number, matched anywhere and ignoring case.
         * Example: "aarav"
         *
         * <p>Two fields rather than one because a school looks a child up by either: the parent
         * gives a name on the phone, the file carries a number. What the caller types is quoted
         * before it becomes a regex, so "APP/2026/09" searches for those characters.
         */
        String search,

        /**
         * Applications from a lead, or walk-ins. Example: true
         *
         * <p>{@code true} returns only those that name an inquiry; {@code false} only those that
         * do not. Absent returns both. It answers "how much of our intake came from the leads we
         * worked", which is the question the funnel report (#34) is built on.
         */
        Boolean fromInquiry,

        Integer page,
        Integer size,
        String sort) {
}
