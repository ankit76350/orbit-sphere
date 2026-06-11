package com.orbitastra.backend.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "academic_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicResult {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private String studentName;

    private String grade;

    private String examName;

    private List<SubjectMark> marks;

    private Double totalPercentage;

    private String overallGrade;

    private String feedback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectMark {
        private String subject;
        private Integer maxMarks;
        private Integer obtainedMarks;
        private String grade;
    }
}
