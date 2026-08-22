package com.orbitastra.backend.models.new_new.academics.examination.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One component mark inside a {@link ReportCardSubjectResult} snapshot, such as
 * the theory or practical part of a subject.
 *
 * <p>This exists so a published report card can print the component breakdown —
 * "Theory 72/80, Practical 18/20" — without reading back StudentMark rows or the
 * ExamSchedule that defined the component.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardComponentResult {

    // Copied from ExamSchedule.componentCode. Example: "THEORY"
    @NotBlank
    private String componentCode;

    // Preserved display name. Example: "Theory Paper"
    private String componentName;

    // Example: 80.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumMarks;

    // Null when the student was absent or exempt for this component.
    // Example: 72.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal obtainedMarks;
}
