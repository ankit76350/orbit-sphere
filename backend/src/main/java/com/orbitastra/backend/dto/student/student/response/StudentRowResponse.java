package com.orbitastra.backend.dto.student.student.response;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.enums.StudentStatus;

/**
 * One row of #4's roll, and of #6's answer.
 *
 * <p><b>Thinner than #5 on purpose.</b> The guardians are left off, and they are the expensive
 * part: a page of fifty children with two contacts each would be a hundred people's phone numbers
 * and addresses drawn on a screen that shows none of them. #5 opens one child and has them all.
 *
 * <p><b>But the guardian COUNT is here</b>, because "this child has no contact on file" is a real
 * thing to see from a list, and it costs nothing — the links are already on the document.
 *
 * <p><b>No {@code nextStep}:</b> a read changed nothing.
 */
public record StudentRowResponse(

        String studentDocsId,
        String admissionNo,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        StudentStatus status,
        LocalDate admissionDate,

        /** How many contacts the school holds for them. */
        int guardianCount,

        /** Whether anybody has put them in a class yet. #14 is what does that, and is not built. */
        Boolean placed,

        /** Absent unless the child came through admissions. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionApplicationDocsId,

        Instant createdAt) {

    public static StudentRowResponse fromStudent(Student child) {
        return new StudentRowResponse(
                child.getId(),
                child.getAdmissionNo(),
                child.getFullName(),
                child.getDateOfBirth(),
                child.getGender(),
                child.getStatus(),
                child.getAdmissionDate(),
                child.getGuardians() == null ? 0 : child.getGuardians().size(),
                child.getCurrentAcademicRecordDocsId() != null,
                child.getAdmissionApplicationDocsId(),
                child.getCreatedAt());
    }
}
