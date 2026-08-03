package com.orbitastra.backend.models.new_new.academics.grading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.new_new.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Versioned school grading rules that support percentage, point, and
 * descriptor-based systems used by different boards and programmes.
 */
@Document(collection = "grading_schemes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_grading_code_version_uniq",
                def = "{'schoolId': 1, 'schemeCode': 1, 'schemeVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_grading_active_name_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GradingScheme extends SchoolBase {

    // Stable key. Example: "CBSE_PERCENTAGE"
    @NotBlank
    private String schemeCode;

    // Example: "CBSE Percentage Grading"
    @NotBlank
    private String name;

    // Example: "2026.1"
    @NotBlank
    private String schemeVersion;

    // Example: GradingScaleType.PERCENTAGE
    @NotNull
    private GradingScaleType scaleType;

    // Maximum value interpreted by the bands. Example: 100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumValue;

    // Ordered, non-overlapping result bands.
    @Builder.Default
    private List<GradeBand> gradeBands = new ArrayList<>();

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
