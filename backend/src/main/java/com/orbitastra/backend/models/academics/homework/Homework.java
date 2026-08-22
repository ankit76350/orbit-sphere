package com.orbitastra.backend.models.academics.homework;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.academics.enums.HomeworkScope;
import com.orbitastra.backend.models.academics.enums.HomeworkStatus;
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
 * One homework assignment published by a teacher.
 *
 * <p>Student submissions are separate HomeworkSubmission documents so this
 * document does not grow with the class roster or submission history.
 */
@Document(collection = "homework")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_class_section_homework_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'sectionNo': 1, 'status': 1, 'dueAt': 1}"),
        @CompoundIndex(
                name = "school_teacher_homework_idx",
                def = "{'schoolId': 1, 'teacherDocsId': 1, 'academicYear': 1, 'status': 1, 'publishedAt': -1}"),
        @CompoundIndex(
                name = "school_selected_student_homework_idx",
                def = "{'schoolId': 1, 'targetStudentDocsIds': 1, 'status': 1, 'dueAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Homework extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // References SchoolClass.sections[].sectionNo when section-specific.
    // Example: "A"
    private String sectionNo;

    // References SchoolClass.subjects[].subjectCode.
    // Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Links to the assigning Staff.id.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String teacherDocsId;

    // Example: "Algebraic identities worksheet"
    @NotBlank
    private String title;

    // Example: "Complete exercises 4.3 and 4.4 and show all working."
    @NotBlank
    private String instructions;

    // Example: HomeworkScope.SECTION
    @NotNull
    private HomeworkScope scope;

    // Used only when scope is SELECTED_STUDENTS.
    // Example: ["67aa15d9dc3f7d0055555555"]
    @Builder.Default
    private List<String> targetStudentDocsIds = new ArrayList<>();

    // Links to DocumentRecord.id values supplied with the assignment.
    // Example: ["67aa15d9dc3f7d0066666666"]
    @Builder.Default
    private List<String> attachmentDocumentDocsIds = new ArrayList<>();

    // Example: 2026-06-01T03:30:00Z
    private Instant openAt;

    // Example: 2026-06-05T17:30:00Z
    @NotNull
    private Instant dueAt;

    // Optional maximum score. Example: 20.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumScore;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean lateSubmissionAllowed = false;

    // Example: 2
    @NotNull
    @Builder.Default
    private Integer maximumAttempts = 1;

    // Example: HomeworkStatus.PUBLISHED
    @NotNull
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.DRAFT;

    // Example: 2026-06-01T03:30:00Z
    private Instant publishedAt;
}
