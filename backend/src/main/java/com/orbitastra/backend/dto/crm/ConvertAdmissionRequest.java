package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Overrides supplied when converting an admission into an enrolled student.
 * Every field is optional — the service forces {@code schoolId} from the
 * admission and prefills identity from its applicant snapshot; anything else set
 * here overrides that. {@code id} and audit timestamps are never accepted.
 *
 * <p>{@code schoolId} is accepted only so clients can reuse a create-student
 * payload; the admission's school is authoritative. Academic placement must be
 * supplied inside {@code currentAcademicRecord}.
 */
@Data
public class ConvertAdmissionRequest {

    /** Ignored — the school is taken from the admission. Accepted for payload parity only. */
    private String schoolId;

    private String admissionNo;

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

    /** Builds the Student payload the conversion service augments with admission data. */
    public Student toStudent() {
        return Student.builder()
                .admissionNo(admissionNo)
                .name(name)
                .dob(dob)
                .gender(gender)
                .bloodGroup(bloodGroup)
                .photoUrl(photoUrl)
                .walletDocsId(walletDocsId)
                .medicalRecordDocsId(medicalRecordDocsId)
                .documents(documents)
                .medicalRemark(medicalRemark)
                .status(status != null ? status : StudentStatus.ACTIVE)
                .admissionDate(admissionDate)
                .build();
    }

    /** Optional initial academic record supplied on the conversion request. */
    public StudentAcademicRecord toAcademicRecord() {
        return currentAcademicRecord == null ? null : currentAcademicRecord.toModel();
    }

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
