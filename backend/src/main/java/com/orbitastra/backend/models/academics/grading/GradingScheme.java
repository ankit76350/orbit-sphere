package com.orbitastra.backend.models.academics.grading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.base.SchoolBase;

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
 *
 * <p><b>A scheme is identified by {@code name} + {@code schemeVersion}.</b> The
 * index named a {@code schemeCode} field that never existed anywhere in this
 * project, which MongoDB would have indexed as null on every document — making
 * one version string usable once per school, so "CBSE Percentage" 2026.1 and
 * "IB Points" 2026.1 could not coexist. Corrected 2026-09-13 to index the field
 * that is actually there.
 *
 * <p><b>Which makes {@code name} a key, not a label.</b> Unlike a class or a
 * term — both addressed by id, both freely renameable — renaming a scheme
 * changes what identifies it, so the two versions of one rulebook stop looking
 * related. <b>There is deliberately no rename endpoint</b>, and one must not be
 * added without moving the index off {@code name} first.
 *
 * <p><b>Never edit a scheme that has been used.</b> Report cards store
 * {@code gradingSchemeDocsId} and must stay reprintable years later: move A1
 * from 91–100 to 90–100 in place and every card ever issued silently reprints
 * with different grades. Create the next {@code schemeVersion} instead and
 * deactivate the old one, which keeps it resolvable without offering it for new
 * work.
 *
 * <p>Band coherence — bounds present, ordered, non-overlapping, inside
 * {@code maximumValue} — is a service rule; see {@code GradingHelper}.
 */
@Document(collection = "grading_schemes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_grading_name_version_uniq",
                def = "{'schoolId': 1, 'name': 1, 'schemeVersion': 1}",
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

    // Identifies the scheme with schemeVersion, so it is a key rather than a label:
    // there is no rename endpoint. Example: "CBSE Percentage Grading"
    @NotBlank
    private String name;

    // Example: "2026.1"
    //If the school changes A1 from 91–100 to 90–100, create a new version instead of changing the scheme used by old report cards.
    @NotBlank
    private String schemeVersion;

    // Example: GradingScaleType.PERCENTAGE
    @NotNull
    private GradingScaleType scaleType;

    // Maximum value the bands are read against: 100 for PERCENTAGE, 7 for an IB POINT
    // scale. Null for DESCRIPTOR, which has nothing to measure. Example: 100.00
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
