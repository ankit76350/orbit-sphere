package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;

import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;

/**
 * One holiday as the API returns it.
 *
 * <p>{@code dayOfWeek} is derived and included because it is the first thing a person checks
 * when reading a calendar back — "is that Sunday right?" — and working it out from a date in a
 * table is exactly the kind of arithmetic a response should save the reader.
 *
 * <p>It is <b>derived, never stored</b>. Nothing in this system infers a closure from the day of
 * the week; this is a convenience for whoever is looking at the list.
 */
public record HolidayView(
        String name,
        String description,
        HolidayType type,
        LocalDate date,
        DayOfWeek dayOfWeek) {

    public static HolidayView fromDetail(HolidayDetail detail) {
        return new HolidayView(
                detail.getName(),
                detail.getDescription(),
                detail.getType(),
                detail.getDate(),
                detail.getDate().getDayOfWeek());
    }
}
