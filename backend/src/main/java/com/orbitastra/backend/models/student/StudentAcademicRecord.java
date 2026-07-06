package com.orbitastra.backend.models.student;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_academic_records")
@CompoundIndexes({
    @CompoundIndex(name = "student_academic_year_unique_idx", def = "{'studentDocId': 1, 'academicYearId': 1}", unique = true),
    @CompoundIndex(name = "school_year_student_id_unique_idx", def = "{'schoolId': 1, 'academicYearId': 1, 'studentId': 1}", unique = true, partialFilter = "{'studentId': {'$type': 'string'}}"),
    @CompoundIndex(name = "class_doc_section_year_roll_unique_idx", def = "{'classDocId': 1, 'sectionId': 1, 'academicYearId': 1, 'rollNo': 1}", unique = true, partialFilter = "{'classDocId': {'$type': 'string'}, 'sectionId': {'$type': 'string'}, 'rollNo': {'$type': 'string'}}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicRecord {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String studentDocId; // References Student.id

    private String academicYearId; // e.g. "2026-2027"

    private String studentId; // year-specific student identifier

    private String rollNo; // year-specific roll number

    private String classDocId; // year-specific class ID

    private String sectionId; // year-specific section ID

    private String hostelRoomId; // year-specific hostel room ID

    private StudentStatus status;


}
