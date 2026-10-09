package com.orbitastra.backend.dto.student.academicrecord.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * What a transfer did: <b>both ends of it</b>.
 *
 * <h2>Two records changed, so two are reported</h2>
 *
 * <p>Returning only the new one would hide half the write. A caller needs to see that the old
 * record was closed, when it was closed, and what it closed as — because that is the part they did
 * not ask for and the part that cannot be undone.
 *
 * <p><b>{@code transferredFrom} is null when the child had no open record.</b> Nothing was closed, the
 * transfer was a first placement, and saying so beats reporting an empty object that reads like a
 * failure.
 */
public record StudentAcademicRecordTransferResponse(

        String studentDocsId,

        String studentName,

        /** Null for a child admitted without one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        String academicYear,

        /**
         * The record that was closed, or null when there was none.
         *
         * <p>Null is the answer to <i>"was this a transfer at all?"</i> — see {@code transferred} below.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Side transferredFrom,

        /** The record the child now holds. Always present. */
        Side now,

        /**
         * True when a record was closed to make room for this one.
         *
         * <p><b>Worth a field of its own</b> rather than leaving a caller to test
         * {@code transferredFrom} for null: the two outcomes are a transfer and a first placement, and a
         * screen says different things about them.
         */
        boolean transferred,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** One end of the transfer — the record as it stands after the write. */
    public record Side(

            String academicRecordDocsId,

            String classDocsId,

            /** Null when the class document is gone. The placement still happened. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            String sectionNo,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String rollNo,

            LocalDate effectiveFrom,

            /**
             * Set on the closed side, null on the open one.
             *
             * <p>The two meet on the same day: the old record's last day is the new one's first.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            LocalDate effectiveUntil,

            AcademicRecordStatus status,

            /** On the new record, the id of the one it replaced. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String previousAcademicRecordDocsId,

            Long version) {
    }
}
