package com.orbitastra.backend.dto.student.academicrecord.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * Where one child has been, year by year.
 *
 * <h2>The child is named once, not on every row</h2>
 *
 * <p>Every record belongs to the same student — it is their history — so repeating the name down
 * the list would be noise. The ids a caller sent come back at the top, with the name behind them.
 *
 * <h2>The class name is resolved, and in one read</h2>
 *
 * <p>A record carries {@code classDocsId} and nothing readable. Reading a class per row is the
 * N+1 the plan warns about for #21; <b>the distinct ids go into a single query</b> instead, so a
 * child with eight years of history costs two reads, not nine.
 *
 * <p><b>{@code className} can be null.</b> A class deleted after the record was written leaves the
 * id pointing at nothing, and that is worth showing as a gap rather than hiding the row — the
 * child was in that class whatever happened to the class document afterwards.
 */
public record StudentAcademicRecordHistoryResponse(

        String studentDocsId,

        String studentName,

        /** Null for a child admitted without one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        /**
         * The year this was narrowed to, echoed back.
         *
         * <p><b>Absent when the whole history was asked for</b>, which is how a caller tells the
         * two apart without comparing what they sent.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String academicYear,

        int recordCount,

        /**
         * Newest year first, and within a year the most recent placement first.
         *
         * <p><b>Empty is a real answer</b>, not a 404: a child admitted in January and not yet
         * placed has no records, and that is the normal state the roll shows as
         * {@code placed: false}.
         */
        List<Row> records) {

    /**
     * One placement.
     *
     * <p><b>Terminal records are here too</b>, which is the point of a history. {@code status}
     * and {@code effectiveUntil} are what tell them apart from the open one.
     */
    public record Row(

            String academicRecordDocsId,

            String academicYear,

            String classDocsId,

            /** Null when the class document is gone. The placement still happened. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String className,

            String sectionNo,

            @JsonInclude(JsonInclude.Include.NON_NULL)
            String rollNo,

            LocalDate effectiveFrom,

            /** Null while the record is open. #16 and #17 set it. */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            LocalDate effectiveUntil,

            AcademicRecordStatus status,

            /**
             * Where the child sat before this, when #17 transferred them.
             *
             * <p>Null on a placement written by #14, and on the first record of a year.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String previousAcademicRecordDocsId,

            /**
             * Whether the student document points at this one.
             *
             * <p><b>Not the same question as {@code status == ACTIVE}</b>, and keeping both is
             * how a disagreement between the two becomes visible: the pointer is a second place
             * the same fact is written, so it can be stale in a way the record's own status
             * cannot. Exactly one row should carry {@code true} for a placed child.
             */
            boolean current,

            Long version) {
    }
}
