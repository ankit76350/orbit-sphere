package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.models.core.enums.HolidayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One reason a day is closed, as sent by a caller.
 *
 * <p><b>Flat on the way in, nested on the way out.</b> Storage groups reasons under a date —
 * see {@link com.orbitastra.backend.models.core.embedded.HolidayDetail} — but a caller adding
 * Holi should not have to know whether that Sunday already exists in the calendar, fetch it,
 * append to its array and send the whole thing back. They send one reason with its date, and the
 * service merges it into the day.
 *
 * <p>So sending two of these with the same date is not a duplicate: it is a day with two
 * reasons, which is exactly the case the storage shape exists for. What is refused is the same
 * <i>type</i> twice on one day — a second WEEKLY_OFF on the same Sunday is a mistake, not a
 * second reason.
 *
 * <p>Every non-working day is a <b>dated</b> entry, including a weekly off. There is no "weekly
 * off day" setting anywhere in this system: schools here may run on Sunday with the off day on
 * any other weekday, so nothing may infer a closure from the day of the week.
 */
public record HolidayRequest(

        /** Example: "Diwali" */
        @NotBlank @Size(max = 120) String name,

        /** Example: "School closed for the Diwali festival" */
        @Size(max = 300) String description,

        /** Example: HolidayType.FESTIVAL */
        @NotNull HolidayType type,

        /** Must fall inside the academic year. Example: 2026-11-08 */
        @NotNull LocalDate date) {

    public HolidayEvent toEvent() {
        return HolidayEvent.builder()
                .name(name.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .type(type)
                .build();
    }
}
