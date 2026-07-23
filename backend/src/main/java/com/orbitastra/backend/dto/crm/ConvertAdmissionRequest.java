package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
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
 * payload; the admission's school is authoritative. Academic placement may be
 * supplied either in {@code currentAcademicRecord} or through the top-level
 * placement fields.
 */
@Data
public class ConvertAdmissionRequest {

    /** Ignored — the school is taken from the admission. Accepted for payload parity only. */
    private String schoolId;

    private String academicYear;

    private String studentNo;

    private String rollNo;

    private String classDocId;

    private String classId;

    private String sectionNo;

    private String hostelRoomNo;

    private String admissionNo;

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

    /**
     * Optional initial academic record supplied on the conversion request. Nested
     * values win; omitted nested values are filled from top-level placement fields.
     */
    public StudentAcademicRecord toAcademicRecord() {
        StudentAcademicRecord record = currentAcademicRecord == null
                ? null
                : currentAcademicRecord.toModel();
        boolean hasTopLevelPlacement = academicYear != null
                || studentNo != null
                || rollNo != null
                || classDocId != null
                || classId != null
                || sectionNo != null
                || hostelRoomNo != null;
        if (record == null && !hasTopLevelPlacement) {
            return null;
        }
        if (record == null) {
            record = StudentAcademicRecord.builder().build();
        }
        if (record.getAcademicYear() == null) record.setAcademicYear(academicYear);
        if (record.getStudentNo() == null) record.setStudentNo(studentNo);
        if (record.getRollNo() == null) record.setRollNo(rollNo);
        if (record.getClassDocId() == null) {
            record.setClassDocId(classDocId != null ? classDocId : classId);
        }
        if (record.getSectionNo() == null) record.setSectionNo(sectionNo);
        if (record.getHostelRoomNo() == null) record.setHostelRoomNo(hostelRoomNo);
        return record;
    }
}
