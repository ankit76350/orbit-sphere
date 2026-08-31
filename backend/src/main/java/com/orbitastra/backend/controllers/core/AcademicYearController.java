package com.orbitastra.backend.controllers.core;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
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
}
