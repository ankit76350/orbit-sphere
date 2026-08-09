package com.orbitastra.backend.models.new_new.academics.examination;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One dated subject paper or practical component within an Exam, for one class
 * section.
 *
 * <p>{@code sectionNo} is required. One row per section keeps the uniqueness key
 * meaningful — a nullable "all sections" row could otherwise coexist with a
 * section-specific row and both would apply to the same students. It also lets
 * each section carry its own date, room, and invigilators, which is normal when
 * a hall cannot seat the whole grade.
 *
 * <p>This document is the register header for {@link ExamAttendance}, in the same
 * way {@code AttendanceSession} is the header for
 * {@code StudentAttendanceRecord}. Reaching {@code COMPLETED} closes attendance
 * for the component.
 */
@Document(collection = "exam_schedules")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_exam_class_section_subject_component_uniq",
                def = "{'schoolId': 1, 'examDocsId': 1, 'classDocsId': 1, 'sectionNo': 1, 'subjectCode': 1, 'componentCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_exam_date_status_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'examDate': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_invigilator_exam_date_idx",
                def = "{'schoolId': 1, 'invigilatorDocsIds': 1, 'examDate': 1, 'startTime': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSchedule extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to Exam.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String examDocsId;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String classDocsId;

    // References an embedded SchoolClass.sections[].sectionNo. Example: "A"
    @NotBlank
    private String sectionNo;

    //? References SchoolClass.subjects[].subjectCode. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Distinguishes theory, practical, oral, or another component.
    // Example: "THEORY"
    @NotBlank
    @Builder.Default
    private String componentCode = "MAIN";

    // Example: "Theory Paper"
    private String componentName;

    // Example: 2026-08-12
    @NotNull
    private LocalDate examDate;

    // Example: 09:00:00
    @NotNull
    private LocalTime startTime;

    // Example: 12:00:00
    @NotNull
    private LocalTime endTime;

    // Example: 100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumMarks;

    // Example: 33.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal passingMarks;

    // Optionally links to a future facility/resource document.
    // Example: "67aa15d9dc3f7d0066666666"
    private String roomResourceDocsId;

    // Links to invigilator Staff.id values.
    // Example: ["67aa15d9dc3f7d0077777777"]
    @Builder.Default
    private List<String> invigilatorDocsIds = new ArrayList<>();

    // Links to the private question-paper DocumentRecord.id.
    // Example: "67aa15d9dc3f7d0088888888"
    private String questionPaperDocumentDocsId;

    // Example: ExamScheduleStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private ExamScheduleStatus status = ExamScheduleStatus.DRAFT;
}
