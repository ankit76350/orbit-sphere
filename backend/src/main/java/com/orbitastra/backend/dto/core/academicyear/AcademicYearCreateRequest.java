package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A new academic year. Endpoint #18.
 *
 * <p><b>{@code name} can never be changed after this.</b> There is no rename endpoint and there
 * must never be one. Other collections do not reference a year by id — they store this string in
 * their own {@code academicYear} field. FeeInvoice, TransportTrip, FeedbackCampaign,
 * FacilityInspection and dozens more. {@code "2026-2027"} <i>is</i> the join key.
 *
 * <p>Which means there is no referential integrity to lean on. A rename would not fail and would
 * not cascade; it would leave every one of those strings naming a year that no longer answers to
 * it, and every row would still look perfectly valid. You would find out when a fee report came
 * back empty.
 *
 * <p>No shape is enforced on the name. A school may use "2026-2027", "2026-27" or "AY2026-27",
 * and imposing one would be this platform deciding something that is not its business. It only
 * has to be unique within the school and never change.
 *
 * <p><b>Holidays are not accepted here.</b> A year is created with an empty calendar, and the
 * calendar is filled through its own endpoints — #20 to #23. Accepting them at creation meant
 * one request that could fail for two unrelated reasons, a bad date range or a stray holiday,
 * leaving the caller to work out which. It also made a school importing a spreadsheet choose
 * between one enormous request and the endpoints that exist for exactly that.
 */
public record AcademicYearCreateRequest(

        /** Unique within the school, immutable. Example: "2026-2027" */
        @NotBlank @Size(max = 40) String name,

        /** Example: 2026-04-01 */
        @NotNull LocalDate startDate,

        /** Example: 2027-03-31 */
        @NotNull LocalDate endDate) {
}
