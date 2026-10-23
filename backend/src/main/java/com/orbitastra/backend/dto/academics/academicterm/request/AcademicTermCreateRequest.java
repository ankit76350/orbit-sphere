package com.orbitastra.backend.dto.academics.academicterm.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * <h2>No termCode</h2>
 *
 * <p><b>Removed 2026-10-16.</b> Every other collection references a term by {@code termDocsId},
 * so a second key nothing joins on was one more thing to keep unique. The {@code name} is the
 * term's only natural key, and it is unique within the year.
 *
 * <h2>weightPercent, and why null is a real answer</h2>
 *
 * <p>Null means <b>this school does not weight the annual result</b>, which is a normal way to
 * run a school — not a missing value. What is refused is the <i>mixture</i>: one term weighted
 * and another not, which computes to nothing. A wrong total is only reported, because getting
 * from 20/80 to 30/70 passes through 30/80.
 */
public record AcademicTermCreateRequest(

        /**
         * Free text — "Term 1", "Semester 2", "Annual". Displayed, and renameable later.
         *
         * <p><b>Unique within the school's year</b>, case-insensitively, the same as
         * {@code sequence}. A report card names the term, so two "Term 1"s are ambiguous in
         * front of a parent.
         */
        @NotBlank @Size(max = 120) String name,

        /** Order inside the year, unique within it. 1-based; nothing enforces density. */
        @NotNull @Min(1) Integer sequence,

        /** First day, inclusive. Must fall inside the academic year. */
        @NotNull LocalDate startDate,

        /** Last day, inclusive — a term ending 30 September includes the 30th. */
        @NotNull LocalDate endDate,

        /** This term's share of the annual result, or null when the school does not weight. */
        @DecimalMin("0") @DecimalMax("100") BigDecimal weightPercent) {
}
