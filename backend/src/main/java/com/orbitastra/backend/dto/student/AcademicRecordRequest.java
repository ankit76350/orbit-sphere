package com.orbitastra.backend.dto.student;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.Data;

/**
 * Client-settable fields of a student's per-year academic record. Reused when
 * creating/updating a student (nested) and by the academic-record / promote
 * endpoints. {@code schoolId} and {@code studentDocsId} are derived server-side
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

    private String classDocsId;

    private String sectionNo;

    private String hostelRoomNo;

    private StudentStatus status;

    private static final Set<String> UNSUPPORTED_FIELDS = Set.of(
            "classDocsId", "sectionNo", "hostelRoomId", "schoolId", "studentDocsId", "id");

    /** Reject deprecated aliases and server-owned identifiers instead of silently ignoring them. */
    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object value) {
        if (UNSUPPORTED_FIELDS.contains(fieldName)) {
            throw new IllegalArgumentException(
                    "Unsupported academic-record field '" + fieldName
                            + "'. Use classDocsId, sectionNo, and hostelRoomNo; schoolId and studentDocsId are server-owned.");
        }
    }

    public StudentAcademicRecord toModel() {
        return StudentAcademicRecord.builder()
                .academicYear(academicYear)
                .studentNo(studentNo)
                .rollNo(rollNo)
                .classDocsId(classDocsId)
                .sectionNo(sectionNo)
                .hostelRoomNo(hostelRoomNo)
                .status(status)
                .build();
    }
}
