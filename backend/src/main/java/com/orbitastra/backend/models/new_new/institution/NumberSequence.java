package com.orbitastra.backend.models.new_new.institution;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.new_new.institution.enums.SequenceResetPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Generates school-scoped business numbers. The next value must be allocated
 * atomically with MongoDB findAndModify and $inc; never read and then update it
 * in separate operations.
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
    private NumberSequenceType sequenceType;

    // Example: "AY_2026_2027"
    private String scopeKey;

    // Example: "ADM/{YYYY}/"
    private String prefixTemplate;

    // Example: ""
    private String suffixTemplate;

    // Example: 1
    @Builder.Default
    private Long nextValue = 1L;

    // Example: 6
    @Builder.Default
    private Integer paddingWidth = 6;

    // Example: SequenceResetPolicy.ACADEMIC_YEAR
    @Builder.Default
    private SequenceResetPolicy resetPolicy = SequenceResetPolicy.NEVER;

    // Example: 2026-04-01T00:00:00Z
    private Instant lastResetAt;
}
