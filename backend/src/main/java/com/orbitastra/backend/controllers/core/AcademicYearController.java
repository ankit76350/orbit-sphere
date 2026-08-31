package com.orbitastra.backend.controllers.core;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
import com.orbitastra.backend.dto.core.academicyear.GenerateWeeklyOffRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayCalendarResponse;
import com.orbitastra.backend.dto.core.academicyear.HolidayRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayUpdateRequest;
import com.orbitastra.backend.dto.core.academicyear.WeeklyOffGenerateResponse;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.services.core.AcademicYearService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The school's academic years. Endpoints #18 and #19 of the plan in this package's README.
 *
 * <p>School surface, so the tenant comes from CurrentSchoolResolver and never from the URL.
 * There is no platform surface for academic years: a year belongs to one school's calendar and
 * no platform operator should be setting one.
 *
 * <p><b>A year is addressed by name, not by id</b> — {@code /academic-years/2026-2027}. That is
 * deliberate. Every other collection in this system stores the year's <i>name</i> as a string in
 * its own {@code academicYear} field, so the name is what the whole system already means when it
 * says "which year". Using it in the URL keeps one vocabulary, and a URL that cannot change is a
 * daily reminder that the thing it names cannot either.
 *
 * <p><b>There is no rename endpoint and there must never be one.</b> Nothing references a year
 * by id, so a rename would not fail and would not cascade — it would leave every stored
 * {@code "2026-2027"} pointing at a year that no longer answers to it, with every row still
 * looking valid. Nobody would notice until a report came back empty.
 *
 * <p>There is also no {@code DELETE}. "Is this year used anywhere?" cannot be a foreign-key
 * check when the references are strings; it is a query across every collection that carries an
 * {@code academicYear} field. Until that is cheap, a year created by mistake is hidden through
 * {@code recordState}, not removed.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/academic-years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    /**
     * Endpoint #18 — creates an academic year.
     *
     * <p>The name must be unique within the school and can never be changed afterwards.
     * Holidays are optional and can be supplied here or added later.
     *
     * <p>Refuses a range that overlaps an existing year: two years covering one date would give
     * every "which year is this?" lookup two answers.
     */
    @PostMapping
    public ResponseEntity<AcademicYearResponse> create(
            @Valid @RequestBody AcademicYearCreateRequest request) {

        AcademicYearResponse response = academicYearService.createAcademicYear(request);
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + response.name()))
                .body(response);
    }

    /**
     * Updates the start and/or end date of an academic year.
    */
    @PatchMapping("/{name}/dates")
    public ResponseEntity<AcademicYearResponse> updateDates(
            @PathVariable String name,
            @Valid @RequestBody AcademicYearDatesRequest request) {

        return ResponseEntity.ok(academicYearService.updateDates(name, request));
    }

    //! the holiday calendar — #20 to #23 ----------------------------------------------

    /**
     * Replaces the complete holiday calendar for an academic year.
     */
    @PutMapping("/{name}/holidays")
    public ResponseEntity<HolidayCalendarResponse> replaceCalendar(
            @PathVariable String name,
            @Valid @RequestBody List<HolidayRequest> holidays) {

        return ResponseEntity.ok(academicYearService.replaceCalendar(name, holidays));
    }

    /**
     * Adds a holiday to the academic year's calendar.
     * Allows multiple holiday reasons on the same date.
     */
    @PostMapping("/{name}/holidays")
    public ResponseEntity<HolidayCalendarResponse> addHoliday(
            @PathVariable String name,
            @Valid @RequestBody HolidayRequest holiday) {

        return ResponseEntity.ok(academicYearService.addHoliday(name, holiday));
    }

    /**
     * Updates a holiday reason for a specific date.
     * The date itself cannot be changed.
     */
    @PatchMapping("/{name}/holidays/{date}")
    public ResponseEntity<HolidayCalendarResponse> updateHoliday(
            @PathVariable String name,
            @PathVariable LocalDate date,
            @RequestParam(required = false) HolidayType type,
            @Valid @RequestBody HolidayUpdateRequest request) {

        return ResponseEntity.ok(academicYearService.updateHoliday(name, date, type, request));
    }

    /**
     * Removes a holiday reason from a specific date.
     * Without a type, removes all holidays for that date.
     */
    @DeleteMapping("/{name}/holidays/{date}")
    public ResponseEntity<HolidayCalendarResponse> removeHoliday(
            @PathVariable String name,
            @PathVariable LocalDate date,
            @RequestParam(required = false) HolidayType type) {

        return ResponseEntity.ok(academicYearService.removeHoliday(name, date, type));
    }

    /**
     * Generates weekly off days for the academic year.
     * Skips dates that already have a weekly off.
     */
    @PostMapping("/{name}/holidays/generate-weekly-off")
    public ResponseEntity<WeeklyOffGenerateResponse> generateWeeklyOff(
            @PathVariable String name,
            @Valid @RequestBody GenerateWeeklyOffRequest request) {

        return ResponseEntity.ok(academicYearService.generateWeeklyOff(name, request));
    }

    /**
     * Removes all holidays of the specified type from the academic year.
     */
    @DeleteMapping("/{name}/holidays")
    public ResponseEntity<HolidayCalendarResponse> removeHolidaysByType(
            @PathVariable String name,
            @RequestParam HolidayType type) {

        return ResponseEntity.ok(academicYearService.removeHolidaysByType(name, type));
    }
}
