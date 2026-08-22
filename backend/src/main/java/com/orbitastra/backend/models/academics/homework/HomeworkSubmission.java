package com.orbitastra.backend.models.new_new.academics.homework;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.HomeworkSubmissionStatus;
import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One numbered homework attempt submitted by one student. */
@Document(collection = "homework_submissions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_homework_student_attempt_uniq",
                def = "{'schoolId': 1, 'homeworkDocsId': 1, 'studentDocsId': 1, 'attemptNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_homework_submission_queue_idx",
                def = "{'schoolId': 1, 'homeworkDocsId': 1, 'status': 1, 'submittedAt': 1}"),
        @CompoundIndex(
                name = "school_student_submission_history_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'academicYear': 1, 'submittedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkSubmission extends AcademicStudentSchoolBase {

    // Links to Homework.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String homeworkDocsId;

    // Links to the student's StudentAcademicRecord.id for this year.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String studentAcademicRecordDocsId;

    // Starts at 1 and increases for resubmissions. Example: 1
    @NotNull
    @Builder.Default
    private Integer attemptNumber = 1;

    // Example: HomeworkSubmissionStatus.SUBMITTED
    @NotNull
    @Builder.Default
    private HomeworkSubmissionStatus status = HomeworkSubmissionStatus.DRAFT;

    // Example: "My explanation and final answer..."
    private String responseText;

    // Links to submitted DocumentRecord.id values.
    // Example: ["67aa15d9dc3f7d0033333333"]
    @Builder.Default
    private List<String> attachmentDocumentDocsIds = new ArrayList<>();

    // Example: 2026-06-04T14:15:00Z
    private Instant submittedAt;

    // Example: 18.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedScore;

    // Links to the grading Staff.id.
    // Example: "67aa15d9dc3f7d0044444444"
    private String gradedByDocsId;

    // Example: 2026-06-06T09:30:00Z
    private Instant gradedAt;

    // Example: "Correct method; review the final simplification."
    private String feedback;
}
