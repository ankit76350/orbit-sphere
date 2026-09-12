package com.orbitastra.backend.dto.academics.academicterm.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One reporting period added to a year. Endpoint #1.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>No {@code academicYear}.</b> It comes from the {@code {year}} path segment. A term cannot
 * be moved between years, so accepting it in the body would be offering a field that can never
 * change.
 *
 * <p><b>No {@code resultsLocked} and no {@code active}.</b> Both start at their defaults —
 * {@code false} and {@code true} — and both are events with their own endpoints (#5, #6, #7, #8)
 * rather than fields to set at create. A term created already locked is a state nothing asked for.
 *
 * <h2>termCode is given, not derived</h2>
 *
 * <p>The caller names the code. It was derived from {@code name} until 2026-09-12, which tied two
 * fields that do not move together: a school renaming "Term 1" to "First Term" would have had to
 * accept a code of {@code FIRST_TERM} on a term six documents across three modules already
 * reference as {@code TERM1}. The code is the stable half and the name is the display half, so
 * the code is the one a school states outright.
 *
 * <p><b>The shape is still fixed, and tighter than the derivation's was.</b> Uppercase letters
 * and digits, nothing else — {@code TERM1}, not {@code TERM_1}. {@code TextHelper.toCode} produced
 * underscores because it had to put <i>something</i> where a space had been; a code stated
 * outright has no such gap to fill, and one separator nobody needs is one more way for two
 * schools to write the same term differently.
 *
 * <p>Validated rather than normalized, so "term 1" is a 400 naming the field instead of a silent
 * rewrite into something the caller never typed and will not recognise when it comes back.
 *
 * <h2>weightPercent, and why null is a real answer</h2>
 *
 * <p>Null means <b>this school does not weight the annual result</b>, which is a normal way to
 * run a school — not a missing value. What is refused is the <i>mixture</i>: one term weighted
 * and another not, which computes to nothing. A wrong total is only reported, because getting
 * from 20/80 to 30/70 passes through 30/80.
 */
public record AcademicTermCreateRequest(

        /** Free text — "Term 1", "Semester 2", "Annual". Displayed, and renameable later. */
        @NotBlank @Size(max = 120) String name,

        /**
         * The stable key, unique within the year — {@code TERM1}, {@code SEM2}, {@code Q3}.
         * Uppercase letters and digits only. Never changes once records reference it.
         */
        @NotBlank
        @Size(max = 40)
        @Pattern(
                regexp = "^[A-Z0-9]+$",
                message = "must be uppercase letters and digits only, like TERM1")
        String termCode,

        /** Order inside the year, unique within it. 1-based; nothing enforces density. */
        @NotNull @Min(1) Integer sequence,

        /** First day, inclusive. Must fall inside the academic year. */
        @NotNull LocalDate startDate,

        /** Last day, inclusive — a term ending 30 September includes the 30th. */
        @NotNull LocalDate endDate,

        /** This term's share of the annual result, or null when the school does not weight. */
        @DecimalMin("0") @DecimalMax("100") BigDecimal weightPercent) {
}
