package com.orbitastra.backend.models.undone.exams;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The marks-entry buffer for one (exam, grade, subject) combination — a teacher
 * enters obtained marks here per student, and it is locked once finalised. When
 * results are published these feed the per-student {@code academics.AcademicResult}
 * report card, so the two are complementary (entry sheet vs published card).
 */
@Document(collection = "exam_marks_sheets")
@CompoundIndex(name = "exam_grade_subject_uniq",
        def = "{'examId': 1, 'grade': 1, 'subject': 1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMarksSheet extends BaseDocument {

    @Indexed
    private String examId;

    private String grade;

    private String subject;

    @Builder.Default
    private Integer maxMarks = 100;

    @Builder.Default
    private Boolean locked = false;

    @Builder.Default
    private List<StudentMark> marks = new java.util.ArrayList<>();

    /** One student's obtained marks in this sheet. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentMark {
        private String studentId;
        private Integer obtainedMarks;
    }
}
