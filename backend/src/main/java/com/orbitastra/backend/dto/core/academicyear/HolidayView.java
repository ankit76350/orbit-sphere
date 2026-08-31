package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;

/**
 * One closed day and every reason for it, as the API returns it.
 *
 * <p>Mirrors the storage shape rather than flattening back out: the caller asked "what does this
 * calendar look like", and a day with two reasons should read as one day with two reasons.
 *
 * <p>{@code dayOfWeek} is derived and included because it is the first thing a person checks
 * when reading a calendar back — "is that Sunday right?" — and working it out from a date in a
 * table is exactly the arithmetic a response should save the reader. It is <b>derived, never
 * stored</b>; nothing in this system infers a closure from the day of the week.
 */
public record HolidayView(
        LocalDate date,
        DayOfWeek dayOfWeek,
        List<EventView> events) {

    /** One reason, without repeating the date it already sits under. */
    public record EventView(String name, String description, HolidayType type) {
    }

    public static HolidayView fromDetail(HolidayDetail detail) {
        List<EventView> events = detail.getEvents() == null ? List.of()
                : detail.getEvents().stream()
                        .map(e -> new EventView(e.getName(), e.getDescription(), e.getType()))
                        .toList();

        return new HolidayView(detail.getDate(), detail.getDate().getDayOfWeek(), events);
    }
}
