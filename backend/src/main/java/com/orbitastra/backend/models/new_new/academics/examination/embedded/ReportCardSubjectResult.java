package com.orbitastra.backend.models.new_new.academics.examination.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Immutable subject-result snapshot embedded in one ReportCard version. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardSubjectResult {

    // References SchoolClass.subjects[].subjectCode. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Preserved display name. Example: "Mathematics"
    @NotBlank
    private String subjectName;

    // Example: 200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumMarks;

    // Example: 172.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedMarks;

    // Example: 86.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentage;

    // Example: "A2"
    private String gradeCode;

    // Example: "Shows consistent progress in algebra."
    private String teacherRemark;
}
