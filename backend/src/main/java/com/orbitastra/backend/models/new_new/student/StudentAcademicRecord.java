package com.orbitastra.backend.models.new_new.student;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.student.enums.AcademicRecordStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Academic-year class and section record for one Student.
 *
 * <p>A student can have historical academic records, but only one ACTIVE record
 * in an academic year. Student and AcademicYear references are inherited from
 * AcademicStudentSchoolBase.
 */
@Document(collection = "student_academic_records")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_active_academic_record_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'status': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_year_class_section_active_roll_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionDocsId': 1, 'rollNo': 1, 'status': 1}",
                unique = true,
                partialFilter = "{'rollNo': {'$type': 'string'}, 'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_year_class_section_roster_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_student_academic_record_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': -1, 'effectiveFrom': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAcademicRecord extends AcademicStudentSchoolBase {

    // Links to the academic class/grade document. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // Optional until the student is assigned to a section.
    // Example: "67aa15d9dc3f7d0022222222"
    private String sectionNo;

    // Year- and class-specific roll number. Example: "23"
    private String rollNo;

    // Example: 2026-04-01
    @NotNull
    private LocalDate effectiveFrom;

    // Null while this academic record remains current. Example: 2027-03-31
    private LocalDate effectiveUntil;

    // Example: AcademicRecordStatus.ACTIVE
    @NotNull
    @Builder.Default
    private AcademicRecordStatus status = AcademicRecordStatus.ACTIVE;

    // Previous academic record replaced by this placement.
    // Example: "67aa15d9dc3f7d0033333333"
    private String previousAcademicRecordDocsId;
}
