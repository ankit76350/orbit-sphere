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
 * <p><b>No {@code termCode}.</b> It is derived from {@code name} the way {@code planCode} is —
 * trimmed, uppercased, runs of non-alphanumerics to {@code _} — so "Term 1" becomes
 * {@code TERM_1}. Accepting both would let a school create a term named "Term 1" coded
 * {@code SEMESTER_2}, and the code is what six documents across three modules store.
 *
 * <p><b>No {@code academicYear}.</b> It comes from the {@code {year}} path segment. A term cannot
 * be moved between years, so accepting it in the body would be offering a field that can never
 * change.
 *
 * <p><b>No {@code resultsLocked} and no {@code active}.</b> Both start at their defaults —
 * {@code false} and {@code true} — and both are events with their own endpoints (#5, #6, #7, #8)
 * rather than fields to set at create. A term created already locked is a state nothing asked for.
 *
 * <h2>weightPercent, and why null is a real answer</h2>
 *
 * <p>Null means <b>this school does not weight the annual result</b>, which is a normal way to
 * run a school — not a missing value. What is refused is the <i>mixture</i>: one term weighted
 * and another not, which computes to nothing. A wrong total is only reported, because getting
 * from 20/80 to 30/70 passes through 30/80.
 */
public record AcademicTermCreateRequest(

        /** Free text — "Term 1", "Semester 2", "Annual". The code is derived from it. */
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
