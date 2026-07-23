package com.orbitastra.backend.dto.student;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for admitting a student. Server-owned fields ({@code id} and
 * audit timestamps) are not accepted from the request body. Academic placement
 * is accepted only inside {@code currentAcademicRecord}; guardian payloads are
 * automatically deduplicated server-side.
 */
@Data
public class CreateStudentRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "admissionNo is required")
    private String admissionNo;

    @NotBlank(message = "name is required")
    private String name;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl;

    @JsonAlias("walletId")
    private String walletDocsId;

    @JsonAlias("medicalRecordId")
    private String medicalRecordDocsId;

    private List<String> documents;

    private List<String> medicalRemark;

    private StudentStatus status;

    private LocalDate admissionDate;

    @Valid
    private List<StudentGuardianRequest> guardians;

    @Valid
    private AcademicRecordRequest currentAcademicRecord;

    private static final Set<String> TOP_LEVEL_ACADEMIC_FIELDS = Set.of(
            "academicYear", "studentNo", "rollNo", "classDocId", "classId", "sectionNo", "hostelRoomNo");

    /**
     * Keep server-owned/unknown fields ignored for compatibility, but reject the
     * old top-level academic placement shape instead of silently accepting it.
     */
    @JsonAnySetter
    public void rejectLegacyAcademicField(String fieldName, Object value) {
        if (TOP_LEVEL_ACADEMIC_FIELDS.contains(fieldName)) {
            throw new IllegalArgumentException(
                    "Academic placement must be provided inside currentAcademicRecord; top-level field '"
                            + fieldName + "' is not supported.");
        }
    }
}
