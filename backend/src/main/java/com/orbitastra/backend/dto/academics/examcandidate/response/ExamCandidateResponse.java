package com.orbitastra.backend.dto.academics.examcandidate.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;

/**
 * One child on an exam's candidate list: who they are, the class, section and roll number from
 * their academic record, whether they may sit, and every debarment and return.
 */
public record ExamCandidateResponse(

        String examCandidateDocsId,

        String studentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String fullName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        String studentAcademicRecordDocsId,

        /** From the academic record. Absent when the record cannot be found. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String sectionNo,

        /** Absent when the section has no roll numbers yet. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String rollNo,

        ExamCandidateStatus status,

        /** Why they are debarred. Absent while ELIGIBLE. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ExamDebarmentReason debarmentReason,

        /** What the office wrote about the debarment. Absent while ELIGIBLE. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String debarmentNote,

        /** Every debarment and return, oldest first. Absent while there has been none. */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<StatusChange> statusChanges,

        /** For the next write to this candidate — debar and readmit want it. */
        Long version) {

    /** One debarment or return. */
    public record StatusChange(ExamCandidateStatus status, ExamDebarmentReason reason, String note,
            Instant changedAt, String changedByDocsId) {
    }

    /**
     * Builds the answer from the candidate, the child and their academic record (either may be
     * missing) and the class's name.
     */
    public static ExamCandidateResponse of(ExamCandidate candidate, Student child,
            StudentAcademicRecord record, String className) {
        List<StatusChange> changes = candidate.getStatusChanges() == null ? List.of()
                : candidate.getStatusChanges().stream()
                        .map(one -> new StatusChange(one.getStatus(), one.getReason(), one.getNote(),
                                one.getChangedAt(), one.getChangedByDocsId()))
                        .toList();
        return new ExamCandidateResponse(
                candidate.getId(),
                candidate.getStudentDocsId(),
                child == null ? null : child.getFullName(),
                child == null ? null : child.getAdmissionNo(),
                candidate.getStudentAcademicRecordDocsId(),
                record == null ? null : record.getClassDocsId(),
                className,
                record == null ? null : record.getSectionNo(),
                record == null ? null : record.getRollNo(),
                candidate.getStatus(),
                candidate.getDebarmentReason(),
                candidate.getDebarmentNote(),
                changes,
                candidate.getVersion());
    }
}
