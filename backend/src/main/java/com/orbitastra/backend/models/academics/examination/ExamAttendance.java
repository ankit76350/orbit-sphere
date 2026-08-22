package com.orbitastra.backend.models.new_new.academics.examination;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One student's examination-hall attendance row for one ExamSchedule component,
 * including the answer copy issued to that student.
 *
 * <p>{@code ExamSchedule} is the register header, so this collection stands in
 * the same relation to it as {@code StudentAttendanceRecord} does to
 * {@code AttendanceSession}. No separate session document is needed: the
 * schedule already owns the date, time, room, invigilators, and status.
 *
 * <p>Attendance is separate from {@link StudentMark} because the two are written
 * by different people at different times. The invigilator records presence and
 * the copy number in the hall; the evaluator enters marks days later. Keeping
 * them apart also allows blind evaluation — marks can be entered against
 * {@code answerCopyNo} alone, and the service resolves the student through this
 * document without exposing identity to the evaluator.
 *
 * <p>{@code answerCopyNo} is unique within one Exam so the same physical answer
 * booklet cannot be recorded against two students. Schools that restart copy
 * numbering per subject must prefix the number, for example {@code "MATH-0001"}.
 * Supplementary booklets go in {@code additionalAnswerCopyNos}; MongoDB cannot
 * extend the unique index across that list, so the service must check those
 * values against both fields before saving.
 *
 * <p>Attendance closes when the owning {@code ExamSchedule} reaches
 * {@code COMPLETED}; there is no separate per-row lock. Marks must not be
 * accepted for a student whose attendance row is {@code ABSENT}.
 */
@Document(collection = "exam_attendances")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_exam_schedule_student_attendance_uniq",
                def = "{'schoolId': 1, 'examScheduleDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_exam_answer_copy_no_uniq",
                def = "{'schoolId': 1, 'examDocsId': 1, 'answerCopyNo': 1}",
                unique = true,
                partialFilter = "{'answerCopyNo': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_exam_schedule_attendance_status_idx",
                def = "{'schoolId': 1, 'examScheduleDocsId': 1, 'attendanceStatus': 1}"),
        @CompoundIndex(
                name = "school_student_exam_attendance_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': -1, 'examDocsId': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttendance extends AcademicStudentSchoolBase {

    // Links to Exam.id; also the uniqueness scope of answerCopyNo.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String examDocsId;

    // Links to ExamSchedule.id, the register header for this component.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String examScheduleDocsId;

    // Links to the student's StudentAcademicRecord.id for this year.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Example: ExamAttendanceStatus.PRESENT
    @NotNull
    private ExamAttendanceStatus attendanceStatus;

    // Serial number of the answer copy issued to the student. Null when the
    // student is ABSENT. Example: "A0012345"
    private String answerCopyNo;

    // Serial numbers of supplementary booklets issued during the paper.
    // Example: ["A0012901", "A0012902"]
    @Builder.Default
    private List<String> additionalAnswerCopyNos = new ArrayList<>();

    // Seat or desk allotted in the examination hall. Example: "R2-14"
    private String seatNo;

    // Time the student was admitted to the hall; a value after
    // ExamSchedule.startTime indicates late admission.
    // Example: 2026-08-12T03:32:00Z
    private Instant reportedAt;

    // Time the answer copy was handed in; a value before ExamSchedule.endTime
    // indicates early departure. Example: 2026-08-12T06:30:00Z
    private Instant submittedAt;

    // Links to the invigilator Staff.id that recorded this row.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String markedByDocsId;

    // Example: 2026-08-12T03:35:00Z
    @NotNull
    private Instant markedAt;

    // References DocumentRecord.id values, such as an unfair-means report.
    // Example: ["67aa15d9dc3f7d0055555555"]
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Example: "Admitted 20 minutes late with the exam controller's approval."
    private String remarks;
}
