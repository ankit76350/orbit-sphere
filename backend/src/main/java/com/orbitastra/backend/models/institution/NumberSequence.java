package com.orbitastra.backend.models.institution;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;

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

    // Which run of numbering this row counts, and the partner of resetPolicy below: that field
    // says HOW the scope is worked out, this one is the answer.
    //
    //     resetPolicy NEVER          -> scopeKey "GLOBAL"     one count, forever
    //     resetPolicy ACADEMIC_YEAR  -> scopeKey "2026-2027"  a fresh count each year
    //     resetPolicy CALENDAR_YEAR  -> scopeKey "2026"
    //     resetPolicy MONTHLY        -> scopeKey "2026-09"
    //
    // A NEW ROW per scope, rather than resetting one row: the 2026-2027 row keeps saying it
    // issued 900 admissions long after 2027-2028 has started at 1. Overwriting nextValue back to
    // 1 would destroy that, and "how many did we admit last year" is a question somebody asks.
    //
    // DELIBERATELY NOT AN ENUM, unlike almost everything else typed in this codebase. "2026-2027"
    // is data, not a constant: the set of values is decided by the calendar, so an enum would
    // need a new constant every April and a deploy to go with it — and the year that deploy was
    // late, no school could admit a student. Compare FeatureCode, which IS an enum because a
    // plan cannot grant a capability the software does not have, so that set is closed by us.
    // The test is always: does our code decide the values, or does time?
    //
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

    // How the scopeKey above is worked out — the kind, where scopeKey is the instance. This is
    // the enum half of the pair.
    //
    // NOTHING READS IT YET. No code resets a sequence or opens a new scope, so every row is
    // NEVER + "GLOBAL" in practice. It is the field that says what the intended behaviour is
    // when somebody comes to build it.
    //
    // Example: SequenceResetPolicy.ACADEMIC_YEAR
    @NotNull
    @Builder.Default
    private SequenceResetPolicy resetPolicy = SequenceResetPolicy.NEVER;

    // When this row's scope last rolled over, so it cannot be done twice in one day.
    // Unused, like resetPolicy — nothing rolls anything over yet.
    // Example: 2026-04-01T00:00:00Z
    private Instant lastResetAt;
}
