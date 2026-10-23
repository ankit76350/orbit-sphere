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
 * One child in full, with their contacts resolved.
 *
 * <p>One shape is used for both write and read responses so that a caller gets the
 * same structure after admitting a child and when opening that child's page.
 *
 * <p>Guardians are resolved into full responses instead of exposing only their IDs.
 *
 * <p>No class, section, or roll number is included here. Those belong to the
 * academic record.
 */
public record StudentResponse(

        String studentDocsId,
        String admissionNo,

        /**
         * The application this child came from.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionApplicationDocsId,

        String fullName,
        LocalDate dateOfBirth,
        Gender gender,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        CountryCode nationalityCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        SchoolLocale preferredLanguage,

        /**
         * The child's own phone number, if available.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String phoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailAddress,

        /**
         * Resolved guardians, not guardian IDs.
         */
        List<GuardianResponse> guardians,

        LocalDate admissionDate,

        StudentStatus status,

        /**
         * Whether the child has been placed into an academic record.
         */
        Boolean placed,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String currentAcademicRecordDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String profilePhotoDocumentId,

        Instant createdAt,
        Instant updatedAt,
        Long version,

        /**
         * What the caller should do next.
         * Null on normal reads.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static StudentResponse of(
            Student child,
            List<GuardianResponse> guardians,
            String nextStep) {

        return new StudentResponse(
                child.getId(),
                child.getAdmissionNo(),
                child.getAdmissionApplicationDocsId(),
                child.getFullName(),
                child.getDateOfBirth(),
                child.getGender(),
                child.getNationalityCode(),
                child.getPreferredLanguage(),
                child.getPhoneNumber(),
                child.getEmailAddress(),
                guardians,
                child.getAdmissionDate(),
                child.getStatus(),
                child.getCurrentAcademicRecordDocsId() != null,
                child.getCurrentAcademicRecordDocsId(),
                child.getProfilePhotoDocumentId(),
                child.getCreatedAt(),
                child.getUpdatedAt(),
                child.getVersion(),
                nextStep);
    }
}