package com.orbitastra.backend.models.new_new.student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.common.enums.Gender;
import com.orbitastra.backend.models.new_new.student.embedded.GuardianLink;
import com.orbitastra.backend.models.new_new.student.enums.StudentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Stable personal profile of one student belonging to a school.
 *
 * <p>Academic-year placement is stored in StudentAcademicRecord. Health, hostel,
 * transport, attendance, fees, and wallet data belong to their own modules and
 * are not duplicated in this profile.
 */
@Document(collection = "students")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_admission_no_uniq",
                def = "{'schoolId': 1, 'admissionNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_admission_application_uniq",
                def = "{'schoolId': 1, 'admissionApplicationDocsId': 1}",
                unique = true,
                partialFilter = "{'admissionApplicationDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_student_status_name_idx",
                def = "{'schoolId': 1, 'status': 1, 'fullName': 1}"),
        @CompoundIndex(
                name = "school_guardian_students_idx",
                def = "{'schoolId': 1, 'guardians.guardianDocsId': 1}"),
        @CompoundIndex(
                name = "school_current_academic_record_uniq",
                def = "{'schoolId': 1, 'currentAcademicRecordDocsId': 1}",
                unique = true,
                partialFilter = "{'currentAcademicRecordDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends SchoolBase {

    // Generated using NumberSequenceType.STUDENT_ADMISSION.
    // Example: "ADM/2026/000001"
    @NotBlank
    private String admissionNo;

    // Optionally references AdmissionApplication.id when converted from CRM.
    // Example: "67aa15d9dc3f7d0011111111"
    private String admissionApplicationDocsId;

    // Example: "Aarav Sharma"
    @NotBlank
    private String fullName;

    // Example: 2018-08-14
    @NotNull
    private LocalDate dateOfBirth;

    // Example: Gender.MALE
    @NotNull
    private Gender gender;

    //! ISO 3166-1 alpha-2 nationality code. Example: "IN"
    private String nationalityCode;

    //! IETF language tag. Example: "en-IN"
    private String preferredLanguage;

    //! Optional direct contact for older students. Example: "+919876543210"
    private String phoneNumber;

    //! Optional direct email for older students. Example: "aarav@example.com"
    private String emailAddress;

    // Guardian relationships are owned by this student profile.
    @Builder.Default
    private List<GuardianLink> guardians = new ArrayList<>();

    // Example: 2026-04-01
    @NotNull
    private LocalDate admissionDate;

    // Example: StudentStatus.ACTIVE
    @NotNull
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    // References the student's current StudentAcademicRecord.id.
    // Null until an academic record is assigned.
    // Example: "67aa15d9dc3f7d0033333333"
    private String currentAcademicRecordDocsId;

    // References DocumentRecord.id for the profile photo.
    // Example: "67aa15d9dc3f7d0022222222"
    private String profilePhotoDocumentId;
}
