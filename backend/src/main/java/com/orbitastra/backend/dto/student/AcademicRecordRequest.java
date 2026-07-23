package com.orbitastra.backend.dto.student;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.Data;

/**
 * Client-settable fields of a student's per-year academic record. Reused when
 * creating/updating a student (nested) and by the academic-record / promote
 * endpoints. {@code schoolId} and {@code studentDocId} are derived server-side
 * from the owning student, never accepted here.
 */
@Data
public class AcademicRecordRequest {

    // Optional only for nested student creation (the service can resolve it from
    // the student's admission date); required by the academic-record and
    // promote endpoints.
    private String academicYear;

    private String studentNo;

    private String rollNo;

    private String classDocId;

    private String sectionNo;

    private String hostelRoomNo;

    private StudentStatus status;

    private static final Set<String> UNSUPPORTED_FIELDS = Set.of(
            "classId", "sectionId", "hostelRoomId", "schoolId", "studentDocId", "id");

    /** Reject deprecated aliases and server-owned identifiers instead of silently ignoring them. */
    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object value) {
        if (UNSUPPORTED_FIELDS.contains(fieldName)) {
            throw new IllegalArgumentException(
                    "Unsupported academic-record field '" + fieldName
                            + "'. Use classDocId, sectionNo, and hostelRoomNo; schoolId and studentDocId are server-owned.");
        }
    }

    public StudentAcademicRecord toModel() {
        return StudentAcademicRecord.builder()
                .academicYear(academicYear)
                .studentNo(studentNo)
                .rollNo(rollNo)
                .classDocId(classDocId)
                .sectionNo(sectionNo)
                .hostelRoomNo(hostelRoomNo)
                .status(status)
                .build();
    }
}
