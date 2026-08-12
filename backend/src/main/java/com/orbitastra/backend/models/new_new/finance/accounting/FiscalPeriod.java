package com.orbitastra.backend.models.new_new.finance.accounting;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.FiscalPeriodStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One stretch of time in the books, usually a month, that can be opened and
 * closed for posting.
 *
 * <p>This is a finance period, not an academic one. A financial year often does
 * not line up with the academic year, so this collection is kept apart from
 * AcademicYear and AcademicTerm and uses its own {@code fiscalYearKey}.
 *
 * <p>Closing a period is what stops last month's numbers from moving after a
 * report has gone out. Once the status is CLOSED, nothing more may be posted into
 * it; a late correction goes into the next open period instead.
 *
 * <p>The service checks that periods do not overlap, that dates run in order, and
 * that reopening a closed period is approved and leaves a reason behind.
 */
@Document(collection = "fiscal_periods")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_fiscal_period_code_uniq",
                def = "{'schoolId': 1, 'periodCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_fiscal_year_period_status_idx",
                def = "{'schoolId': 1, 'fiscalYearKey': 1, 'status': 1, 'startDate': 1}"),
        @CompoundIndex(
                name = "school_fiscal_period_dates_idx",
                def = "{'schoolId': 1, 'startDate': 1, 'endDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalPeriod extends SchoolBase {

    // Stable key for the period. Example: "2026-04"
    @NotBlank
    private String periodCode;

    // Financial year this period belongs to. Example: "2026-2027"
    @NotBlank
    private String fiscalYearKey;

    // Name shown to accountants. Example: "April 2026"
    @NotBlank
    private String name;

    // Example: 2026-04-01
    @NotNull
    private LocalDate startDate;

    // Example: 2026-04-30
    @NotNull
    private LocalDate endDate;

    // Position of the period inside its financial year. Example: 1
    @NotNull
    private Integer periodNo;

    // Example: FiscalPeriodStatus.OPEN
    @NotNull
    @Builder.Default
    private FiscalPeriodStatus status = FiscalPeriodStatus.FUTURE;

    // Links to the staff identity that closed the period.
    // Example: "67aa15d9dc3f7d0044444444"
    private String closedByDocsId;

    // Example: 2026-05-05T11:00:00Z
    private Instant closedAt;

    // Links to the staff identity that opened it again.
    // Example: "67aa15d9dc3f7d0055555555"
    private String reopenedByDocsId;

    // Example: 2026-05-12T06:30:00Z
    private Instant reopenedAt;

    // Needed whenever a closed period is opened again.
    // Example: "A April fee receipt was found after the month was closed."
    private String reopenReason;
}
