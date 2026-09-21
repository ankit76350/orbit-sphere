package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

/**
 * One admission cycle, as the API gives it back. Endpoint #1, and the reads when they are built.
 *
 * <p>The dates go back exactly as they were sent, in ISO-8601. A screen turns them into something
 * a person can read; the API does not, because a client may be in a different timezone than the
 * school.
 */
public record AdmissionCycleResponse(

        /** The id that applications will store as {@code admissionCycleDocsId}. */
        String admissionCycleId,

        String academicYear,
        String name,
        AdmissionCycleStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant inquiryOpenAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant applicationOpenAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant applicationCloseAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant enrollmentDeadlineAt,

        /**
         * How many classes have seats set up. Zero on a new cycle.
         *
         * <p>A count and not the table itself: the table can be long, and the only thing a caller
         * needs right after creating a cycle is whether it is still empty.
         */
        int capacityCount,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notes,

        /**
         * What to do next. A <b>write</b> field — it says what just happened.
         *
         * <p>Left out of the JSON when null, which is what the reads pass. A read changed nothing,
         * so it has nothing to say about what happens next.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** The same cycle, for a read. No {@code nextStep}: nothing just happened. */
    public static AdmissionCycleResponse fromCycle(AdmissionCycle cycle) {
        return fromCycle(cycle, null);
    }

    public static AdmissionCycleResponse fromCycle(AdmissionCycle cycle, String nextStep) {
        return new AdmissionCycleResponse(
                cycle.getId(),
                cycle.getAcademicYear(),
                cycle.getName(),
                cycle.getStatus(),
                cycle.getInquiryOpenAt(),
                cycle.getApplicationOpenAt(),
                cycle.getApplicationCloseAt(),
                cycle.getEnrollmentDeadlineAt(),
                // Null safe because a cycle stored with an explicit null reads back null, even
                // though a missing field reads back as an empty list.
                cycle.getCapacities() == null ? 0 : cycle.getCapacities().size(),
                cycle.getNotes(),
                nextStep);
    }
}
