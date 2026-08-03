package com.orbitastra.backend.models.new_new.academics.examination;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.ExamParticipationStatus;
import com.orbitastra.backend.models.new_new.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One student's mark for one ExamSchedule component. */
@Document(collection = "student_marks")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_exam_schedule_student_mark_uniq",
                def = "{'schoolId': 1, 'examScheduleDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_exam_student_marks_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'studentDocsId': 1, 'subjectDocsId': 1}"),
        @CompoundIndex(
                name = "school_exam_mark_entry_queue_idx",
                def = "{'schoolId': 1, 'examDocsId': 1, 'examScheduleDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMark extends AcademicStudentSchoolBase {

    // Links to Exam.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String examDocsId;

    // Links to ExamSchedule.id. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String examScheduleDocsId;

    // Links to the student's StudentAcademicRecord.id for this year.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Copied from ExamSchedule for efficient reporting; links to Subject.id.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String subjectDocsId;

    // Example: ExamParticipationStatus.ATTENDED
    @NotNull
    @Builder.Default
    private ExamParticipationStatus participationStatus = ExamParticipationStatus.ATTENDED;

    // Null for absent, exempt, or withheld results. Example: 86.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedMarks;

    // Grade calculated from the selected GradingScheme. Example: "A2"
    private String gradeCode;

    // Example: MarkEntryStatus.LOCKED
    @NotNull
    @Builder.Default
    private MarkEntryStatus status = MarkEntryStatus.DRAFT;

    // Links to the Staff.id that entered the mark.
    // Example: "67aa15d9dc3f7d0055555555"
    private String enteredByDocsId;

    // Example: 2026-08-20T10:00:00Z
    private Instant enteredAt;

    // Example: 2026-08-22T11:00:00Z
    private Instant lockedAt;

    // Links to the Staff.id that locked the mark.
    // Example: "67aa15d9dc3f7d0066666666"
    private String lockedByDocsId;

    // Example: "Strong conceptual understanding."
    private String remarks;
}
