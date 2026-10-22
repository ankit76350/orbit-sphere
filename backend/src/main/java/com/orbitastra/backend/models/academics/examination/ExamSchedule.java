package com.orbitastra.backend.models.academics.examination;

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

import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.embedded.ExamRoomAllocation;
import com.orbitastra.backend.models.base.SchoolBase;

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
 * <p>{@code rooms} are where the paper is written — one, or several when the students do
 * not fit in one, each with its seat count and its own invigilators. They are what two different
 * clashes hang off. Two papers cannot be in one hall at one time, and a hall cannot be used for a
 * paper while a lesson is timetabled in it — the exam service has to check both, because the
 * timetable and the datesheet are built by different people at different times of year.
 *
 * <p>A room can hold more than one exam at a time, as long as everyone in it then fits its
 * {@code capacity}; and the seats of all the rooms must cover the children sitting. Seating forty-five children in a room that holds thirty-six is
 * the sort of thing that only becomes visible on the morning of the paper.
 *
 * <p>This document is the hall register's header. The register's rows are the
 * entries for this paper in {@code ExamCandidate.papers}, in the same way
 * {@code AttendanceSession} is the header for {@code StudentAttendanceRecord}.
 * Reaching {@code COMPLETED} closes attendance for the component.
 */
@Document(collection = "exam_schedules")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_exam_class_section_subject_component_uniq",
                def = "{'schoolId': 1, 'examDocsId': 1, 'classDocsId': 1, 'sectionNo': 1, 'subjectCode': 1, 'componentName': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_exam_date_status_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'examDate': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_invigilator_exam_date_idx",
                def = "{'schoolId': 1, 'rooms.invigilatorDocsIds': 1, 'examDate': 1, 'startTime': 1}"),
        @CompoundIndex(
                name = "school_exam_schedule_room_idx",
                def = "{'schoolId': 1, 'rooms.roomDocsId': 1, 'examDate': 1, 'startTime': 1}")
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

    // Shared by the papers added together — the sections that sit this subject at one time. A
    // room's seatCount is for all of them together, so a room's seats are counted once per sitting,
    // not once per section. Example: "67aa15d9dc3f7d0099999999"
    private String sittingDocsId;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String classDocsId;

    // References an embedded SchoolClass.sections[].sectionNo. Example: "A"
    @NotBlank
    private String sectionNo;

    //? References SchoolClass.subjects[].subjectCode. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // The part of the subject this paper is — "Theory", "Practical", "Oral". Empty when the
    // subject has one paper. Unique with the exam, class, section and subject, ignoring case;
    // there is no component code. Example: "Theory"
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

    // The rooms this paper is written in — one, or several when the students do not fit in one
    // — each with how many sit there and who is on duty. Empty while a datesheet is still a draft
    // and nobody has allocated rooms.
    @Builder.Default
    private List<ExamRoomAllocation> rooms = new ArrayList<>();

    // Links to the private question-paper DocumentRecord.id.
    // Example: "67aa15d9dc3f7d0088888888"
    private String questionPaperDocumentDocsId;

    // Example: ExamScheduleStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private ExamScheduleStatus status = ExamScheduleStatus.DRAFT;
}
