package com.orbitastra.backend.dto.academics.examcandidate.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.student.Student;

/**
 * One child on an exam's candidate list, with their name, admission number and roll number.
 */
public record ExamCandidateResponse(

        String examCandidateDocsId,

        String studentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String fullName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        /** From the academic record they were added with. Absent when the section has no rolls yet. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String rollNo,

        String studentAcademicRecordDocsId,

        ExamCandidateStatus status,

        /** Why they are debarred. Absent while ELIGIBLE. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ExamDebarmentReason debarmentReason,

        /** For the next write to this candidate — debar and readmit want it. */
        Long version) {

    /** Builds the answer from the candidate, the child (when found) and the roll number. */
    public static ExamCandidateResponse of(ExamCandidate candidate, Student child, String rollNo) {
        return new ExamCandidateResponse(
                candidate.getId(),
                candidate.getStudentDocsId(),
                child == null ? null : child.getFullName(),
                child == null ? null : child.getAdmissionNo(),
                rollNo,
                candidate.getStudentAcademicRecordDocsId(),
                candidate.getStatus(),
                candidate.getDebarmentReason(),
                candidate.getVersion());
    }
}
