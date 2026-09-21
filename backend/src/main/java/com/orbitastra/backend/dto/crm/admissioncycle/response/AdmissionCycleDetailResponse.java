package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

/**
 * One admission cycle in full. Endpoint #6.
 *
 * <p><b>What this has that a list row does not:</b> {@code notes}, and the seat table itself
 * rather than a count of it. Both are left off #5 because notes can be two thousand characters and
 * a seat table can be twenty rows — a hundred list rows carrying either would be a lot of data
 * nobody reads.
 *
 * <p><b>What it still does not have is how the seats are DOING.</b> Offered, accepted, enrolled,
 * free — those are counted from {@code admission_applications} and they are
 * [#7](../../../controllers/crm/README.md). This endpoint reads one document and reports what the
 * school configured, not what has happened against it.
 */
public record AdmissionCycleDetailResponse(

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
         * The seat table as the school set it up, in the order it was stored.
         *
         * <p>Empty until #4 is built and used. An empty table is a normal state for a DRAFT cycle,
         * not a missing one.
         */
        List<Seat> capacities,

        /** How many classes the table covers. The same number #5 gives, kept so both agree. */
        int capacityCount,

        /** Every seat in the table added up, so a caller does not have to. */
        int totalSeats,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notes,

        Instant createdAt,
        Instant updatedAt) {

    /**
     * One class's seats.
     *
     * <p><b>The name is resolved, and it can be absent.</b> A stored {@code classDocsId} naming a
     * class this school no longer has reads back with no name — which is the honest answer, and
     * the same call {@code timetable} #7 makes. Inventing a name, or dropping the row, would hide
     * a real problem: a cycle holding seats for a class that is gone.
     */
    public record Seat(
            String classDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            Integer totalSeats,
            Integer reservedSeats) {
    }
}
