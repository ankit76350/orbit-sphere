package com.orbitastra.backend.dto.student;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    // The entire nested record is optional, but its academic year is required
    // whenever any academic-record value is supplied.
    private String academicYear;

    private String identityNo;

    private String rollNo;

    @JsonAlias("classDocId")
    private String classDocsId;

    private String sectionNo;

    private String hostelRoomNo;

    private StudentStatus status;

    /** Reject unknown, deprecated, and server-owned fields instead of silently ignoring them. */
    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object value) {
        throw new IllegalArgumentException(
                "Unsupported academic-record field '" + fieldName
                        + "'. Allowed fields are academicYear, identityNo, rollNo, classDocsId, "
                        + "sectionNo, hostelRoomNo, and status. "
                        + "classDocId is accepted only as a legacy alias for classDocsId; "
                        + "schoolId, studentDocsId, and id are server-owned.");
    }

    public StudentAcademicRecord toModel() {
        // Jackson creates this DTO even when the client sends
        // "currentAcademicRecord": {}. Treat an empty (or blank-only) object as
        // omitted so it cannot create an empty academic-record document or a
        // currentAcademicRecordDocsId pointer on the student.
        if (!hasAnyValue()) {
            return null;
        }
        if (!hasText(academicYear)) {
            throw new IllegalArgumentException(
                    "currentAcademicRecord.academicYear is required when currentAcademicRecord contains academic details.");
        }

        return StudentAcademicRecord.builder()
                .academicYear(academicYear)
                .identityNo(identityNo)
                .rollNo(rollNo)
                .classDocsId(classDocsId)
                .sectionNo(sectionNo)
                .hostelRoomNo(hostelRoomNo)
                .status(status)
                .build();
    }

    public boolean hasAnyValue() {
        return hasText(academicYear)
                || hasText(identityNo)
                || hasText(rollNo)
                || hasText(classDocsId)
                || hasText(sectionNo)
                || hasText(hostelRoomNo)
                || status != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
