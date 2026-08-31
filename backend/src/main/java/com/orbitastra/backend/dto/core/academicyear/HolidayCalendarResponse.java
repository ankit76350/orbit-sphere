package com.orbitastra.backend.dto.core.academicyear;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.models.core.enums.HolidayType;

/**
 * A year's whole holiday calendar. Shared by every calendar endpoint.
 *
 * <p>One record for all six rather than one per verb. Every calendar operation — replace, add,
 * edit, remove one, remove a type — leaves the caller wanting the same thing: what does the
 * calendar look like now. {@code changeSummary} is what distinguishes them, so a `200` with an
 * unchanged count is still legible.
 *
 * <p><b>Two counts, because a day and a reason are not the same thing.</b>
 * {@code closedDayCount} is how many days the school is shut — the number attendance and fees
 * care about. {@code eventCount} is how many reasons are recorded across them, and is larger
 * whenever a weekly off falls on a festival. Reporting only one of them would make a Sunday that
 * is also Holi look like either a lost entry or an extra closed day.
 *
 * <p>{@code countsByType} counts <b>events</b>, not days, for the same reason: "how many
 * festivals" should not be reduced by the ones that happened to land on a Sunday.
 *
 * <p>Days come back <b>sorted by date</b>. They are stored in whatever order they were added,
 * and an unsorted calendar is unreadable.
 */
public record HolidayCalendarResponse(
        String academicYearName,
        LocalDate startDate,
        LocalDate endDate,
        int closedDayCount,
        int eventCount,
        Map<HolidayType, Integer> countsByType,
        List<HolidayView> holidays,

        /**
         * What the call just did to the calendar. A <b>write</b> field.
         *
         * <p>Left out of the JSON when it is null, which is what G8 passes. A read changed
         * nothing, so it has nothing to summarise. Every calendar write sets it, so none of
         * #20 to #23 or the two DELETEs change.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String changeSummary) {

    /**
     * The same calendar, for a read. Used by G8.
     *
     * <p>No {@code changeSummary}: nothing just happened. The field drops out of the JSON rather
     * than coming back null.
     */
    public static HolidayCalendarResponse fromAcademicYear(AcademicYear year) {
        return fromAcademicYear(year, null);
    }

    public static HolidayCalendarResponse fromAcademicYear(AcademicYear year, String changeSummary) {
        List<HolidayDetail> stored = year.getHolidays() == null ? List.of() : year.getHolidays();

        List<HolidayView> sorted = stored.stream()
                .sorted(Comparator.comparing(HolidayDetail::getDate))
                .map(HolidayView::fromDetail)
                .toList();

        List<HolidayEvent> allEvents = stored.stream()
                .flatMap(d -> d.getEvents() == null ? List.<HolidayEvent>of().stream()
                        : d.getEvents().stream())
                .toList();

        // LinkedHashMap so the enum's own order survives into the JSON, which makes two
        // responses comparable by eye.
        Map<HolidayType, Integer> counts = new LinkedHashMap<>();
        for (HolidayType type : HolidayType.values()) {
            long n = allEvents.stream().filter(e -> e.getType() == type).count();
            if (n > 0) {
                counts.put(type, (int) n);
            }
        }

        return new HolidayCalendarResponse(
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                stored.size(),
                allEvents.size(),
                counts,
                sorted,
                changeSummary);
    }
}
