package com.orbitastra.backend.models.academics;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcadmicStudentSchool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "academic_results")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicResult extends AcadmicStudentSchool {

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
