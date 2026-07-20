package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.GuardianLinkRequest;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Overrides supplied when converting an admission into an enrolled student.
 * Every field is optional — the service forces {@code schoolId} and
 * {@code academicYear} from the admission and prefills identity from its
 * applicant snapshot; anything set here overrides that. {@code id},
 * {@code schoolId} and audit timestamps are never accepted from the body.
 */
@Data
public class ConvertAdmissionRequest {

    private String admissionId;

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
                .currentAcademicRecord(currentAcademicRecord == null ? null : currentAcademicRecord.toModel())
                .build();
    }
}
