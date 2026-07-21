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
    @CompoundIndex(name = "student_academic_year_unique_idx", def = "{'studentDocId': 1, 'academicYear': 1}", unique = true),
    @CompoundIndex(name = "school_year_student_no_unique_idx", def = "{'schoolId': 1, 'academicYear': 1, 'studentNo': 1}", unique = true, partialFilter = "{'studentNo': {'$type': 'string'}}"),
    @CompoundIndex(name = "class_doc_section_no_year_roll_unique_idx", def = "{'classDocId': 1, 'sectionNo': 1, 'academicYear': 1, 'rollNo': 1}", unique = true, partialFilter = "{'classDocId': {'$type': 'string'}, 'sectionNo': {'$type': 'string'}, 'rollNo': {'$type': 'string'}}")
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

    private String academicYear; // References AcademicYear.name (unique per school), e.g. "2026-2027"

    private String studentNo; // year-specific student number (e.g. "STD-003")

    private String rollNo; // year-specific roll number

    private String classDocId; // year-specific class ID

    private String sectionNo; // year-specific section number

    private String hostelRoomNo; // year-specific hostel room number

    private StudentStatus status;


}
