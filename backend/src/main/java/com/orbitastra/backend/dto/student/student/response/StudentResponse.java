package com.orbitastra.backend.dto.student.student.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.dto.student.guardian.response.GuardianResponse;
import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.enums.StudentStatus;

/**
 * One child in full, with their contacts resolved. The answer to #1 and to #5.
 *
 * <p><b>One shape for the write and the read on purpose.</b> What a caller gets back from
 * admitting a child is the same thing they get from opening that child's page a minute later, so
 * a client has one object to understand rather than two that are nearly the same. The only
 * difference is {@code matched} inside each guardian, which #1 fills in and #5 leaves out.
 *
 * <p><b>The guardians are the whole point of resolving anything.</b> A student document stores
 * only ids and flags; a page that showed {@code 67aa15d9…} where a mother's name belongs would be
 * useless, so they are read in <b>one</b> query and joined here.
 *
 * <p><b>No class, no section, no roll number.</b> Those live on the academic record, which #14
 * writes and which does not exist yet. {@code placed} says whether there is one rather than
 * pretending there might be.
 */
public record StudentResponse(

        String studentDocsId,
        String admissionNo,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        StudentStatus status,
        LocalDate admissionDate,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        CountryCode nationalityCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        SchoolLocale preferredLanguage,

        /** The child's own, for an older student. Most children have neither. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String phoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailAddress,

        /** Resolved people, not ids. Never empty: #1 refuses a child with no contact. */
        List<GuardianResponse> guardians,

        /**
         * The application this child came from. <b>Absent for most of the roll</b> — a transfer, a
         * walk-in and every child a school typed in when it started using the product have none.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionApplicationDocsId,

        /**
         * Whether the child has been put in a class and a section yet.
         *
         * <p>A plain boolean rather than the record itself, because #14 is not built and the
         * honest answer today is "no" for everybody. When it is built this becomes the obvious
         * place to hang the current class on.
         */
        Boolean placed,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String currentAcademicRecordDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String profilePhotoDocumentId,

        Instant createdAt,
        Instant updatedAt,
        Long version,

        /** What to do next. Null on a read: a read changed nothing. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static StudentResponse of(Student child, List<GuardianResponse> guardians,
            String nextStep) {
        return new StudentResponse(
                child.getId(),
                child.getAdmissionNo(),
                child.getFullName(),
                child.getDateOfBirth(),
                child.getGender(),
                child.getStatus(),
                child.getAdmissionDate(),
                child.getNationalityCode(),
                child.getPreferredLanguage(),
                child.getPhoneNumber(),
                child.getEmailAddress(),
                guardians,
                child.getAdmissionApplicationDocsId(),
                child.getCurrentAcademicRecordDocsId() != null,
                child.getCurrentAcademicRecordDocsId(),
                child.getProfilePhotoDocumentId(),
                child.getCreatedAt(),
                child.getUpdatedAt(),
                child.getVersion(),
                nextStep);
    }
}
