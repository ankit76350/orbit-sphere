package com.orbitastra.backend.dto.student;

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

    // Optional on student-create (resolved to the school's current year when omitted);
    // required by the promote endpoint, which the service enforces.
    private String academicYear;

    private String studentNo;

    private String rollNo;

    private String classDocId;

    private String sectionId;

    private String hostelRoomId;

    private StudentStatus status;

    public StudentAcademicRecord toModel() {
        return StudentAcademicRecord.builder()
                .academicYear(academicYear)
                .studentNo(studentNo)
                .rollNo(rollNo)
                .classDocId(classDocId)
                .sectionId(sectionId)
                .hostelRoomId(hostelRoomId)
                .status(status)
                .build();
    }
}
