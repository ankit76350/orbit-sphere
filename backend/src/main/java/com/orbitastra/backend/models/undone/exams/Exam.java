package com.orbitastra.backend.models.undone.exams;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.exams.enums.ExamTerm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An examination event (e.g. "Midterm 2026") with its per-subject datesheet.
 *
 * <p>This is the exam DEFINITION / scheduling record. The marks captured against
 * it live in {@link ExamMarksSheet}, and the published, per-student report card
 * is the existing {@code academics.AcademicResult} — this model does not
 * duplicate either.
 */
@Document(collection = "exams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    // References AcademicYear.name (unique per school), e.g. "2026-2027".
    @Indexed
    private String academicYear;

    private String name;

    private ExamTerm term;

    // Which classes/grades this exam applies to (e.g. "All Classes", "Grade 10").
    private String appliesTo;

    private LocalDate startDate;

    @Builder.Default
    private Boolean published = false;

    @Builder.Default
    private List<DatesheetEntry> datesheet = new java.util.ArrayList<>();

    /** One subject sitting in the exam datesheet. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatesheetEntry {
        private LocalDate date;
        private String subject;
        private LocalTime startTime;
        private LocalTime endTime;
        private String room;
        private String invigilator; // references Staff.id
    }
}
