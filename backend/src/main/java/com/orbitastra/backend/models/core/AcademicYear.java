package com.orbitastra.backend.models.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One named academic year belonging to one school.
 *
 * <p>Other collections reference this document by the immutable {@code name}
 * stored in their {@code academicYear} field, never by AcademicYear.id.
 * Therefore, a created name must not be changed. Every lookup must combine the
 * inherited {@code schoolId} with the academic-year name.
 *
 * <p>The holiday calendar is embedded because its dates belong exclusively to
 * this academic year. Date ordering, overlap prevention, duplicate holidays,
 * and lock/unlock authorization are service and request-DTO rules.
 */
@Document(collection = "academic_years")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_dates_idx",
                def = "{'schoolId': 1, 'startDate': 1, 'endDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends SchoolBase {

    // Immutable reference used by child documents. Example: "2026-2027"
    @NotBlank
    @Setter(AccessLevel.NONE)
    private String name;

    // First school day boundary. Example: 2026-04-01
    @NotNull
    private LocalDate startDate;

    // Last school day boundary. Example: 2027-03-31
    @NotNull
    private LocalDate endDate;

    // Embedded dated holidays and weekly offs. Example: [{ "name": "Diwali", "date": "2026-11-08" }]
    @Builder.Default
    private List<HolidayDetail> holidays = new ArrayList<>();

    // Controls whether new enrollments may be assigned to this year. Example: true
    @NotNull
    @Builder.Default
    private Boolean enrollmentEnabled = false;

    // Prevents result changes after publication/finalization. Example: false
    @NotNull
    @Builder.Default
    private Boolean resultsLocked = false;

    /**
     * Whether the school has switched over to operating in this year. Example: true
     *
     * <p><b>Added on request, 2026-09-09, and it argues with a decision already recorded in this
     * codebase.</b> {@link com.orbitastra.backend.dto.core.academicyear.response.AcademicYearResponse}
     * derives a {@code current} flag from the dates and says plainly why nothing like it is
     * stored: <i>"two sources for 'which year is it' is two sources that can disagree, and the
     * dates are already authoritative"</i>. That is a fair objection, and #18 and #19 back it up
     * by refusing years whose ranges overlap — so at most one year can contain any given date,
     * and "which year is it" already has exactly one answer.
     *
     * <p><b>What the dates still cannot say</b> is whether the school has <i>started using</i>
     * the year that the calendar says has begun. A school sets next year up in February and its
     * {@code startDate} arrives on 1 April, while the previous year's results, attendance and
     * fees are still being closed out for weeks afterwards. The dates make the new year live at
     * midnight, on their own, silently. This flag makes it a deliberate act instead — one that
     * somebody performs, that can be delayed, and that can be undone.
     *
     * <p><b>It does not replace the dates and must never be read alone.</b> That is the whole
     * force of the objection above: a flag left true after its year ended would keep a finished
     * year live indefinitely. {@code ActionGate.requireRunningAcademicYear} therefore requires
     * <b>both</b> — the flag true <i>and</i> today inside the dates — so the stored value can
     * only ever narrow what the dates already permit, never widen it. Read that way, it cannot
     * disagree with the dates in the direction that matters.
     *
     * <p><b>Only one year per school should be true.</b> Nothing in this document enforces that;
     * it is a service rule wherever this comes to be set, and it wants a partial unique index on
     * {@code {schoolId: 1, isThisYearRunning: 1}} filtered to true — the same shape that keeps
     * one {@code current} subscription per school honest.
     *
     * <p><b>Defaults to false</b>, so creating a year never silently makes it the live one, and
     * the 18 documents that predate this field read as null — which the gate treats as not
     * running. Failing closed is the right way round for a gate.
     */
    @NotNull
    @Builder.Default
    private Boolean isThisYearRunning = false;
}
