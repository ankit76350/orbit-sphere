package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;

import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One non-working day, as sent by a caller.
 *
 * <p>A separate record from HolidayDetail rather than binding the embedded model directly.
 * The model is what is stored; this is what the API accepts, and keeping them apart means a
 * field added to the model does not silently become settable from outside.
 *
 * <p>Every non-working day is a <b>dated</b> entry, including a weekly off. There is no
 * "weekly off day" setting anywhere in this system, deliberately: schools in this market may
 * run on Sunday with the off day on any other weekday, so nothing may infer a closure from the
 * day of the week. That is why a year needs roughly 52 WEEKLY_OFF rows and why endpoint #23
 * exists to generate them.
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

    public HolidayDetail toDetail() {
        return HolidayDetail.builder()
                .name(name.trim())
                .description(description == null || description.isBlank() ? null : description.trim())
                .type(type)
                .date(date)
                .build();
    }
}
