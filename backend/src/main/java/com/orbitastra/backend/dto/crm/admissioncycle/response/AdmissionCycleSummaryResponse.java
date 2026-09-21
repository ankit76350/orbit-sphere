package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

/**
 * One row of #5's list.
 *
 * <p><b>Thinner than what #1 gives back, on purpose.</b> {@code notes} can be two thousand
 * characters and nothing on a list reads it, so a hundred rows would carry two hundred thousand
 * characters nobody looks at. It is on #6, which opens one cycle.
 *
 * <p>The seat table is a <b>count</b> for the same reason. What a list needs to say is whether the
 * seats have been set up at all; the table itself belongs on the one-cycle read.
 *
 * <p><b>No {@code nextStep}.</b> A read changed nothing, so it has nothing to say about what
 * happens next, and the field on every row of a list is noise a client then has to decide whether
 * to trust.
 */
public record AdmissionCycleSummaryResponse(

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

        /** How many classes have seats set up. Zero until #4 is built and used. */
        int capacityCount,

        /** When the round was set up. What the default order sorts on. */
        Instant createdAt) {

    public static AdmissionCycleSummaryResponse fromCycle(AdmissionCycle cycle) {
        return new AdmissionCycleSummaryResponse(
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
                cycle.getCreatedAt());
    }
}
