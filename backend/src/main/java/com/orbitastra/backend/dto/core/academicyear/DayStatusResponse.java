package com.orbitastra.backend.dto.core.academicyear;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.core.embedded.HolidayDetail;

/**
 * Whether the school is open on one date, and if not, why. The answer to G9.
 *
 * <p>This is the question attendance, timetables, transport and fee due dates all ask, so the
 * shape matters more than most. It is {@link HolidayView} plus a {@code closed} flag, as the
 * plan in {@code controllers/core/README.md} asked for.
 *
 * <p><b>An open day is a real answer, not a missing one.</b> A working day comes back {@code 200}
 * with {@code closed: false} and an empty {@code events} list — never a {@code 404}. A 404 would
 * make every caller treat "the school is open" and "something went wrong" as the same reply,
 * which is precisely the bug this endpoint exists to prevent. The only {@code 404} here is a
 * year that does not exist.
 *
 * <p>{@code events} is named to match {@link HolidayView#events()} rather than something like
 * "reasons", because it is the same list of the same things. Two names for one structure is a
 * thing every client then has to know.
 *
 * <p><b>{@code dayOfWeek} is information, never input.</b> It is derived from the date so a
 * person reading the answer can sanity-check it. Nothing in this system decides a closure from
 * the weekday, and a caller must not either: schools here may run on a Sunday and take the
 * weekly off on another day. Every closure is a dated entry, which is why #23 exists to generate
 * them. If you find yourself writing {@code dayOfWeek == SUNDAY} anywhere, that is the bug.
 */
public record DayStatusResponse(
        String academicYearName,
        LocalDate date,
        DayOfWeek dayOfWeek,
        boolean closed,
        List<HolidayView.EventView> events) {

    /** The school is shut that day, for the reasons on the calendar entry. */
    public static DayStatusResponse closed(String academicYearName, HolidayDetail detail) {
        HolidayView view = HolidayView.fromDetail(detail);

        return new DayStatusResponse(
                academicYearName, view.date(), view.dayOfWeek(), true, view.events());
    }

    /** Nothing on the calendar for that date, so the school is open. */
    public static DayStatusResponse open(String academicYearName, LocalDate date) {
        return new DayStatusResponse(
                academicYearName, date, date.getDayOfWeek(), false, List.of());
    }
}
