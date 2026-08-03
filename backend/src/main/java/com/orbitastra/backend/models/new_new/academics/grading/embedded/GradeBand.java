package com.orbitastra.backend.models.new_new.academics.grading.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One bounded result range embedded in a GradingScheme. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeBand {

    // Example: "A1"
    @NotBlank
    private String gradeCode;

    // Inclusive lower boundary. Example: 91.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal minimumValue;

    // Inclusive upper boundary. Example: 100.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumValue;

    // Optional grade point. Example: 10.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal gradePoint;

    // Example: "Outstanding"
    private String description;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean passed = true;
}
