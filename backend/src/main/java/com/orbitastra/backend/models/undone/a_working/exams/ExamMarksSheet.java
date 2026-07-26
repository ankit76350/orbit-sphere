package com.orbitastra.backend.models.undone.a_working.exams;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.exams.enums.ExamMarkStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The marks-entry buffer for one (exam, grade, subject) combination — a teacher
 * enters obtained marks here per student, and it is locked once finalised. When
 * results are published these feed the per-student
 * {@code academics.AcademicResult}
 * report card, so the two are complementary (entry sheet vs published card).
 */
@Document(collection = "exam_marks_sheets")
@CompoundIndex(
    name = "exam_class_subject_uniq",
    def = "{'examDocsId': 1, 'classDocsId': 1, 'subjectDocsId': 1}",
    unique = true
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMarksSheet extends SchoolBase {

    @Indexed
    private String examDocsId;

    private String classDocsId;

    private String sectionNo;

    private String subjectName;

    @Builder.Default
    private Integer maxMarks = 100;

    @Builder.Default
    private boolean locked = false;

    @Builder.Default
    private List<StudentMark> marks = new java.util.ArrayList<>();

    /** One student's obtained marks in this sheet. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentMark {

        /** Student whose marks are recorded. */
        private String studentDocsId;

        /** Marks obtained by the student. Null until evaluated or if not applicable. */
        private Integer obtainedMarks;

        /** Current status of the student's exam entry. */
        @Builder.Default
        private ExamMarkStatus status = ExamMarkStatus.PENDING;

        /** Optional remarks such as medical leave, grace marks, or re-evaluation. */
        private String remarks;
    }
}
