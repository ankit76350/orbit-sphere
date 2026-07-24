package com.orbitastra.backend.dto.student;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
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

    private String walletDocsId;

    private String medicalRecordDocsId;

    private List<String> documents;

    private List<String> medicalRemark;

    private StudentStatus status;

    private LocalDate admissionDate;

    @Valid
    private List<StudentGuardianRequest> guardians;

    @Valid
    private AcademicRecordRequest currentAcademicRecord;

    private String academicYear;
    private String studentNo;
    private String rollNo;
    private String classDocsId;
    private String sectionNo;
    private String hostelRoomNo;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "Academic placement must be provided inside currentAcademicRecord; top-level fields are not supported.")
    public boolean isAcademicPlacementNestedOnly() {
        return academicYear == null && studentNo == null && rollNo == null
                && classDocsId == null && sectionNo == null && hostelRoomNo == null;
    }
}
