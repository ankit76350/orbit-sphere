package com.orbitastra.backend.controllers.academics.examination;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.examschedule.request.ExamScheduleCreateRequest;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamPapersAddedResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.ExamScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The papers of an exam — the datesheet. Endpoints #8–#13 of
 * {@code controllers/academics/examination/README.md}; #8 is built.
 */
@RestController
@RequestMapping("/schools/current/academic-years/{year}")
@RequiredArgsConstructor
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #8 — <b>add a paper</b>: one subject of one class, for one or more sections at the
     * same date and time. One paper per section. 201.
     *
     * <pre>
     * 400 INVALID_TIME_RANGE             startTime is not before endTime
     * 400 PASSING_MARKS_ABOVE_MAXIMUM    passingMarks is more than maximumMarks
     * 400 DUPLICATE_SECTION_IN_REQUEST   a section is in sectionNos twice
     * 404 ACADEMIC_YEAR_NOT_FOUND        no year of that name in this school
     * 404 EXAM_NOT_FOUND                 no exam of that id in this year
     * 409 EXAM_NOT_EDITABLE              the exam is COMPLETED or CANCELLED
     * 409 PAPER_OUTSIDE_EXAM             the date is not inside the exam's dates
     * 404 NOT_A_WORKING_DAY              the date is a holiday or weekly off — the message names it
     * 404 CLASS_NOT_FOUND                no class of that id in this year
     * 400 SECTION_NOT_IN_CLASS           a section the class does not have — names them
     * 409 SECTION_NOT_ACTIVE             a section that has been switched off
     * 400 SUBJECT_NOT_TAUGHT_IN_SECTION  the subject is not taught in a section — names them
     * 409 PAPER_ALREADY_SCHEDULED        a section already has this paper in this exam — names them
     * 404 ROOM_NOT_FOUND                 facilityResourceDocsId sent, and not this school's
     * 409 ROOM_NOT_IN_USE                the room is under maintenance, closed or decommissioned
     * 409 ROOM_CLASH                     the room holds another sitting then — names it
     * 404 STAFF_NOT_FOUND                an invigilator who is not this school's staff
     * 409 INVIGILATOR_CLASH              an invigilator is on duty at another sitting then — names it
     * 400 VALIDATION_FAILED              a required field is missing, or maximumMarks not above 0
     * 409 SCHOOL_NOT_EDITABLE            the school is suspended or closed
     * 409 ACADEMIC_YEAR_NOT_RUNNING      the year is not the one the school is running
     * 400 TENANT_NOT_RESOLVED            no idtoken cookie
     * </pre>
     */
    @PostMapping("/exams/{examDocsId}/schedules")
    public ResponseEntity<ExamPapersAddedResponse> add(@PathVariable String year,
            @PathVariable String examDocsId, @Valid @RequestBody ExamScheduleCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        ExamPapersAddedResponse response = examScheduleService.addPapers(year, examDocsId, request);

        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/exams/"
                        + examDocsId + "/schedules"))
                .body(response);
    }
}
