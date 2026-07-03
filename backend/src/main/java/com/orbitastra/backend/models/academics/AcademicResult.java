package com.orbitastra.backend.models.academics;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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

    @Indexed
    private String schoolId;

    @Indexed
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
