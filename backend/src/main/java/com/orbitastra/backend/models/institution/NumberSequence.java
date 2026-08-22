package com.orbitastra.backend.models.new_new.institution;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.new_new.institution.enums.SequenceResetPolicy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Generates one type of school-scoped human-readable business number.
 *
 * <p>The unique key is {@code schoolId + sequenceType + scopeKey}. A global
 * sequence uses scopeKey {@code "GLOBAL"}; an academic-year sequence uses the
 * immutable academic-year name such as {@code "2026-2027"}.
 *
 * <p>{@code nextValue} means the next unused numeric value. Allocation must use
 * one atomic MongoDB {@code findAndModify} operation that returns the old value
 * while applying {@code $inc}; never read and update in separate operations.
 * Formatting tokens are interpreted by the service and stored output numbers
 * remain immutable business identifiers.
 */
@Document(collection = "number_sequences")
@CompoundIndex(
        name = "school_sequence_type_scope_uniq",
        def = "{'schoolId': 1, 'sequenceType': 1, 'scopeKey': 1}",
        unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NumberSequence extends SchoolBase {

    // Example: NumberSequenceType.STUDENT_ADMISSION
    @NotNull
    private NumberSequenceType sequenceType;

    // Example: "2026-2027" or "GLOBAL"
    @NotBlank
    private String scopeKey;

    // Example: "ADM/{YYYY}/"
    private String prefixTemplate;

    // Example: ""
    private String suffixTemplate;

    // Example: 1
    @NotNull
    @Builder.Default
    private Long nextValue = 1L;

    // Example: 6
    @NotNull
    @Builder.Default
    private Integer paddingWidth = 6;

    // Example: SequenceResetPolicy.ACADEMIC_YEAR
    @NotNull
    @Builder.Default
    private SequenceResetPolicy resetPolicy = SequenceResetPolicy.NEVER;

    // Example: 2026-04-01T00:00:00Z
    private Instant lastResetAt;
}
