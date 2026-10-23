package com.orbitastra.backend.dto.student.academicrecord.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * What #19 answers: one academic record in full, with the child and the class named.
 *
 * <p>The same fields a row of #20 carries, plus the child's name and admission number, because
 * this is read on its own and not under a child who is already on the screen.
 */
public record StudentAcademicRecordDetailResponse(

        String academicRecordDocsId,

        String academicYear,

        String studentDocsId,

        /** Null when the child is no longer in this school. The record still reads. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String studentName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        String classDocsId,

        /** Null when the class document is gone. The child was still in that class. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String rollNo,

        LocalDate effectiveFrom,

        /** Null while the record is open. Set by #16 or #17 when it is closed. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate effectiveUntil,

        AcademicRecordStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String previousAcademicRecordDocsId,

        /**
         * True when the child's {@code currentAcademicRecordDocsId} points at this record. It is
         * the child's copy of the fact, kept beside {@code status} so the two can be compared.
         */
        boolean current,

        /** For the next write to this record — #15 and #16 both want it. */
        Long version) {

    /** Builds the answer from the record, the child (may be null) and the class name. */
    public static StudentAcademicRecordDetailResponse of(StudentAcademicRecord record,
            Student child, String className) {

        return new StudentAcademicRecordDetailResponse(
                record.getId(),
                record.getAcademicYear(),
                record.getStudentDocsId(),
                child == null ? null : child.getFullName(),
                child == null ? null : child.getAdmissionNo(),
                record.getClassDocsId(),
                className,
                record.getSectionNo(),
                record.getRollNo(),
                record.getEffectiveFrom(),
                record.getEffectiveUntil(),
                record.getStatus(),
                record.getPreviousAcademicRecordDocsId(),
                child != null && record.getId().equals(child.getCurrentAcademicRecordDocsId()),
                record.getVersion());
    }
}
