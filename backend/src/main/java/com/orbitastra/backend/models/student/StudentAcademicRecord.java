package com.orbitastra.backend.models.student;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_academic_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicRecord {

    @Id
    private String id;

    private String schoolId;

    private String studentDocId; // References Student.id

    private String academicYearId; // e.g. "2026-2027"

    private String studentId; // year-specific student identifier

    private String rollNo; // year-specific roll number

    private String classId; // year-specific class ID

    private String sectionId; // year-specific section ID

    private String hostelRoomId; // year-specific hostel room ID

    private StudentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
