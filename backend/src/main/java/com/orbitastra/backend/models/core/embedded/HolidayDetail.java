package com.orbitastra.backend.models.core.embedded;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One non-working day, and every reason it is one.
 *
 * <p>Restructured on 2026-08-31. It used to be a flat row — name, description, type, date — one
 * per reason, which meant a date could appear several times in the calendar and there was no
 * single answer to "is the school open on the 8th".
 *
 * <p>Now the <b>date is the key</b> and the reasons hang off it. A Sunday that is also Holi is
 * one closed day with two {@link HolidayEvent}s, not two days that happen to share a date.
 *
 * <p>That shape is what the rest of the system actually asks for. Attendance, timetables,
 * transport and fee due dates all want the same thing — <i>is this date a working day</i> — and
 * that is now one lookup returning one entry, rather than a scan that must not stop at the first
 * match.
 *
 * <p><b>A date appears at most once</b> in {@code AcademicYear.holidays}. Two entries for one
 * date would put the question back where it started, so the services merge into the existing
 * entry rather than adding a second.
 *
 * <p>{@code events} is never empty. A date row with no reason on it is a day marked closed for
 * no stated cause, which reads as data corruption to whoever finds it — so removing the last
 * event removes the day.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDetail {

    // The day itself. Unique within an academic year's calendar. Example: 2026-11-08
    @NotNull
    private LocalDate date;

    // Why the school is closed. At least one, and more where a day has more than one reason:
    // a weekly off that is also a festival keeps both.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<HolidayEvent> events = new ArrayList<>();
}
