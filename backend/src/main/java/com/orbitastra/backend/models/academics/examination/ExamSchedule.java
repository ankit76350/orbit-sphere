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
 * <p>{@code facilityResourceDocsId} is the hall, and it is the field two different clashes
 * hang off. Two papers cannot be in one hall at one time, and a hall cannot be used for a
 * paper while a lesson is timetabled in it — the exam service has to check both, because the
 * timetable and the datesheet are built by different people at different times of year.
 *
 * <p>The number of students sitting must also fit the hall's {@code capacity}. Seating
 * forty-five children in a room that holds thirty-six is the sort of thing that only becomes
 * visible on the morning of the paper.
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
                def = "{'schoolId': 1, 'invigilatorDocsIds': 1, 'examDate': 1, 'startTime': 1}"),
        @CompoundIndex(
                name = "school_exam_schedule_room_idx",
                def = "{'schoolId': 1, 'facilityResourceDocsId': 1, 'examDate': 1, 'startTime': 1}",
                partialFilter = "{'facilityResourceDocsId': {'$type': 'string'}}")
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

    // Links to FacilityResource.id for the hall or room this paper is written in. Renamed
    // from roomResourceDocsId on 2026-08-21: the comment used to say it pointed at "a
    // future facility/resource document", and that future arrived when `facilities` was
    // built. The name now says which collection, like every other link in the system.
    //
    // Null while a datesheet is still a draft and nobody has allocated halls. A published
    // datesheet with no room on it is a datesheet that will be argued about on the morning.
    // Example: "67c31122dc3f7d0011223344"
    private String facilityResourceDocsId;

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
