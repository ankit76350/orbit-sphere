package com.orbitastra.backend.dto.student.academicrecord.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * One academic record, with the names behind its three ids.
 *
 * <h2>The names are why this is not just the document</h2>
 *
 * <p>A record stores {@code studentDocsId} and {@code classDocsId} and nothing readable. The
 * caller sent both of those, so echoing them back says nothing — <b>a screen that has just placed
 * a child wants to show "Arya is in Grade 7 B"</b>, and the ids alone cannot.
 *
 * <p>Both names come from documents #14 already read to validate the request, so they cost
 * nothing extra.
 *
 * <h2>{@code effectiveUntil} and {@code previousAcademicRecordDocsId} are always null here</h2>
 *
 * <p>They are on the response because they are on the record, and because the endpoints that fill
 * them — #16 closes a record, #17 moves one and chains it back through
 * {@code previousAcademicRecordDocsId} — will return this same shape. <b>A field that appears
 * later is worse than one that is null now</b>: a client written against the first shape breaks on
 * the second.
 */
public record StudentAcademicRecordResponse(

        String academicRecordDocsId,

        String academicYear,

        String studentDocsId,

        /** Read from the child this record was written for. */
        String studentName,

        /** Null for a child admitted without one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        String classDocsId,

        /** The class's own name, such as "Grade 7". */
        String className,

        String sectionNo,

        /**
         * Null when none was sent.
         *
         * <p><b>Not generated</b> — see the request. A record with no roll number is a real state.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String rollNo,

        LocalDate effectiveFrom,

        /** Null until #16 closes the record or #17 moves the child out of it. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate effectiveUntil,

        /** Always {@code ACTIVE} from #14. */
        AcademicRecordStatus status,

        /** Null from #14. #17 sets it, chaining a moved child back to where they sat before. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String previousAcademicRecordDocsId,

        /** For the next write to this record — #16 and #17 will both want it. */
        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * Builds the answer from the saved record and the two documents behind its ids.
     *
     * <p>{@code className} is passed rather than taken from a class object, because the only
     * thing needed off that document is its name and #14 has it in hand already.
     */
    public static StudentAcademicRecordResponse of(StudentAcademicRecord record, Student child,
            String className, String nextStep) {

        return new StudentAcademicRecordResponse(
                record.getId(),
                record.getAcademicYear(),
                record.getStudentDocsId(),
                child.getFullName(),
                child.getAdmissionNo(),
                record.getClassDocsId(),
                className,
                record.getSectionNo(),
                record.getRollNo(),
                record.getEffectiveFrom(),
                record.getEffectiveUntil(),
                record.getStatus(),
                record.getPreviousAcademicRecordDocsId(),
                record.getVersion(),
                nextStep);
    }
}
