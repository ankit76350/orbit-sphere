package com.orbitastra.backend.dto.student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.Builder;
import lombok.Data;

/**
 * API view of a {@link Student}. Mirrors the persisted student fields and embeds
 * the resolved {@link StudentAcademicRecord} for the current academic year — a
 * read-only projection the persisted entity no longer carries. The service builds
 * this by resolving the current record separately, keeping the entity clean.
 */
@Data
@Builder
public class StudentResponse {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String id;
    private String schoolId;
    private String admissionNo;
    /** Persisted pointer to the current-year {@link StudentAcademicRecord} document id. */
    private String currentAcademicRecordId;
    private String name;
    private LocalDate dob;
    private Gender gender;
    private String bloodGroup;
    private String photoUrl;
    private String walletId;
    private List<GuardianLink> guardians;
    private String medicalRecordId;
    private StudentStatus status;
    private LocalDate admissionDate;
    /** The full current-year academic record (resolved, not persisted on the student). */
    private StudentAcademicRecord currentAcademicRecord;

    /** Builds a response from the student entity and its resolved current academic record. */
    public static StudentResponse of(Student s, StudentAcademicRecord currentRecord) {
        if (s == null) return null;
        return StudentResponse.builder()
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .id(s.getId())
                .schoolId(s.getSchoolId())
                .admissionNo(s.getAdmissionNo())
                .currentAcademicRecordId(s.getCurrentAcademicRecordId())
                .name(s.getName())
                .dob(s.getDob())
                .gender(s.getGender())
                .bloodGroup(s.getBloodGroup())
                .photoUrl(s.getPhotoUrl())
                .walletId(s.getWalletId())
                .guardians(s.getGuardians())
                .medicalRecordId(s.getMedicalRecordId())
                .status(s.getStatus())
                .admissionDate(s.getAdmissionDate())
                .currentAcademicRecord(currentRecord)
                .build();
    }
}
