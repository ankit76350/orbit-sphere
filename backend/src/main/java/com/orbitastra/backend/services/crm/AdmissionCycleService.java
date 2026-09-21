package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.common.time.SchoolZone;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCreateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Admission cycles — one round of admissions for one academic year. Endpoint #1 of the plan in
 * {@code controllers/crm/README.md}; only #1 is built.
 *
 * <p>School surface, so the school comes from CurrentSchoolResolver and never from the URL. There
 * is no platform surface for cycles: no operator of ours decides when a school admits students.
 *
 * <p><b>This is the first call anybody makes in this module.</b> Every application has to name a
 * cycle, so nothing else here works until one exists.
 *
 * <p><b>The academic year does not have to be the running one.</b> Every other module refuses a
 * write against a year the school is not currently running. This module is the opposite case: a
 * school sets up its 2027-2028 admissions in the middle of 2026-2027, and often runs two cycles at
 * once — late admissions into this year while next year's cycle is open. The controller therefore
 * runs gates 1 and 2 and not gate 4. The full reasoning is in the README.
 *
 * <p><b>There is no utils file for this module yet.</b> Everything #1 does is used by #1 only, and
 * this project's rule is that logic with one caller stays inline under its own step. When #2 and
 * #3 arrive, the year check and the date ordering will have a second caller and move to
 * {@code utils/AdmissionCycleServiceUtils.java} then.
 */
@Service
@RequiredArgsConstructor
public class AdmissionCycleService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionCycleService.class);

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    private final AdmissionCycleRepository admissionCycles;
    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolZone schoolZone;

    /**
     * Endpoint #1 — sets up a new admission cycle for one academic year.
     *
     * <p>The cycle is created <b>empty and not started</b>: status DRAFT, and no seats. Seats are
     * added by #4 and the cycle is opened by #3.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND   no year with that name in this school
     * 409 CYCLE_NAME_TAKEN          that year already has a cycle with that name
     * 400 CYCLE_DATES_OUT_OF_ORDER  the dates given are not in a sensible order
     * </pre>
     */
    public AdmissionCycleResponse createCycle(AdmissionCycleCreateRequest request) {
        log.info("[createCycle] Step 1: Finding out which school is asking");
        //! step 1 - who is asking. requireUsable and not require, because this is a write.
        //!
        //! NOT REACHABLE AS A REFUSAL, and kept anyway. Mutation M12 swapped this for require()
        //! and every test still passed, because the controller has already run
        //! gate.requireActiveSchool - which is stricter than this check, not weaker: it allows
        //! only ACTIVE, while requireUsable also allows PROVISIONING. So the gate always says no
        //! first. This stays because every other service in the project does the same, and
        //! because it is what protects the method if it is ever called from somewhere that
        //! forgot the gate.
        School school = currentSchool.requireUsable();

        //! step 2 - the year has to be one this school actually has.
        //! We do NOT check that it is the year the school is running. Admissions are set up for a
        //! year that has not started yet, so that check would refuse the normal case.
        String year = request.academicYear() == null ? "" : request.academicYear().trim();
        log.info("[createCycle] Step 2: Checking the school has an academic year called '{}'", year);

        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the name has to be free inside that year. A school can run more than one
        //! round for a year, so the name is the only thing staff have to tell two cycles apart.
        //! school_academic_year_cycle_name_uniq also stops it; this check is what turns a
        //! duplicate key error into a message that says which cycle already has the name.
        String name = request.name().trim();
        log.info("[createCycle] Step 3: Checking '{}' does not already have a cycle called '{}'",
                year, name);

        // TODO: check admission cycle exists
        if (admissionCycles.existsBySchoolIdAndAcademicYearAndName(school.getId(), year, name)) {
            throw ApiException.conflict("CYCLE_NAME_TAKEN",
                    "'" + year + "' already has an admission cycle called '" + name + "'.");
        }

        //! step 4 - the four dates have to run forwards.
        //! All four are optional, because a school often creates the cycle before it has settled
        //! its calendar. We only compare the ones that were actually sent, so a school can give
        //! just the application window now and fill the rest in later through #2.
        log.info("[createCycle] Step 4: Checking the dates that were given run in order");
        SchoolTimeZone zone = schoolZone.of(school);
        Instant[] dates = {
                request.inquiryOpenAt(),
                request.applicationOpenAt(),
                request.applicationCloseAt(),
                request.enrollmentDeadlineAt()
        };
        String[] dateNames = {
                "enquiries open",
                "applications open",
                "applications close",
                "the enrollment deadline"
        };

        Instant earlier = null;
        String earlierName = null;
        for (int i = 0; i < dates.length; i++) {
            if (dates[i] == null) {
                continue;
            }
            if (earlier != null && dates[i].isBefore(earlier)) {
                throw ApiException.badRequest("CYCLE_DATES_OUT_OF_ORDER",
                        "The dates are in the wrong order: " + dateNames[i] + " is "
                                + Dates.readable(dates[i], zone) + ", which is before "
                                + earlierName + " at " + Dates.readable(earlier, zone) + ".");
            }
            earlier = dates[i];
            earlierName = dateNames[i];
        }

        //! step 5 - build the cycle, with nothing set up in it yet.
        //! Seats are never set here. They are their own endpoint (#4), and mixing them in meant
        //! one request that could fail for two unrelated reasons - a name that is taken, or a bad
        //! seat row - with the caller having to work out which.
        //! schoolId is set by hand. Nothing fills it in for us, and a cycle saved without it
        //! belongs to no school and is invisible to every read.
        log.info("[createCycle] Step 5: Getting the new cycle ready to save");
        AdmissionCycle cycle = AdmissionCycle.builder()
                .schoolId(school.getId())
                .academicYear(year)
                .name(name)
                .status(AdmissionCycleStatus.DRAFT)
                .inquiryOpenAt(request.inquiryOpenAt())
                .applicationOpenAt(request.applicationOpenAt())
                .applicationCloseAt(request.applicationCloseAt())
                .enrollmentDeadlineAt(request.enrollmentDeadlineAt())
                .capacities(new ArrayList<>())
                .notes(TextHelper.blankToNull(request.notes()))
                .build();

        //! step 6 - save
        // TODO: insert admission cycle
        AdmissionCycle saved = admissionCycles.save(cycle);
        log.info("[createCycle] Step 6: Saved the cycle (id={}) as a DRAFT", saved.getId());

        return AdmissionCycleResponse.fromCycle(saved,
                "'" + saved.getName() + "' is a DRAFT with no seats set up yet. Set the seats per "
                        + "class next, then open the cycle — applications can only be taken once "
                        + "it is OPEN. " + NO_AUTHORIZATION_YET);
    }
}
