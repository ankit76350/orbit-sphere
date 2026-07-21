package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.GuardianLinkRequest;
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
 * <p>{@code schoolId} and {@code academicYear} are accepted only so the frontend
 * can reuse the exact create-student payload shape, and both are ignored:
 * {@code schoolId} comes from the admission, and the academic year is not set at
 * conversion — a student is enrolled into a year separately, afterwards, via
 * {@code POST /api/students/{id}/academic-records}.
 */
@Data
public class ConvertAdmissionRequest {

    private String admissionId;

    /** Ignored — the school is taken from the admission. Accepted for payload parity only. */
    private String schoolId;

    /** Ignored — a student's academic year is assigned after enrolment, not at conversion. Accepted for payload parity only. */
    private String academicYear;

    private String admissionNo;

    private String name;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl;

    private String walletId;

    private String medicalRecordId;

    private StudentStatus status;

    private LocalDate admissionDate;

    @Valid
    private List<GuardianLinkRequest> guardians;

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
                .walletId(walletId)
                .medicalRecordId(medicalRecordId)
                .status(status != null ? status : StudentStatus.ACTIVE)
                .admissionDate(admissionDate)
                .guardians(GuardianLinkRequest.toModels(guardians))
                .build();
    }

    /**
     * Optional initial academic record supplied on the convert request. Usually null — the
     * academic year is assigned after enrolment — but honoured when a caller sets placement inline.
     */
    public StudentAcademicRecord toAcademicRecord() {
        return currentAcademicRecord == null ? null : currentAcademicRecord.toModel();
    }
}
