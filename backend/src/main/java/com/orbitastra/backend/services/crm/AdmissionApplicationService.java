package com.orbitastra.backend.services.crm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;

/**
 * Admission applications — the form a family fills in. Endpoint #17 of the plan in
 * {@code controllers/crm/README.md}; only #17 is built.
 *
 * <p><b>This is the first thing in the module that needs a cycle to be OPEN</b>, which is what #3
 * made possible. Before it, no cycle could leave DRAFT and nothing could be applied to.
 *
 * <p><b>An application does not need an inquiry.</b> The family that walks in with a completed
 * form never enquired, so {@code inquiryDocsId} is nullable — and that is why the whole pipeline
 * is testable without a single lead in the database.
 */
@Service
@RequiredArgsConstructor
public class AdmissionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionApplicationService.class);

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * How many answers a form may carry.
     *
     * <p><b>Nothing validates the answers themselves</b> — there is no form definition to check
     * them against — so the only thing that can be bounded is how many there are. Without this the
     * field is an unbounded map a caller controls.
     */
    private static final int MAX_FORM_ANSWERS = 200;

    private final AdmissionApplicationRepository applications;
    private final InquiryRepository inquiries;
    private final SchoolClassRepository schoolClasses;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;

    /**
     * Endpoint #17 — starts an application against an open cycle.
     *
     * <p>Creates in {@code DRAFT}. Submitting it is #19, which is not built, so nothing can move
     * past DRAFT yet.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND   no cycle with that id in this school
     * 409 CYCLE_NOT_OPEN              the cycle is not taking applications
     * 404 INQUIRY_NOT_FOUND           an inquiry that is not this school's
     * 409 APPLICATION_ALREADY_EXISTS  that inquiry already applied to that cycle
     * 409 CLASS_NOT_IN_CYCLE_YEAR     the class is not of the cycle's year
     * 409 CLASS_NOT_IN_CAPACITY       the cycle's seat table does not list that class
     * 400 TOO_MANY_FORM_ANSWERS       more answers than the cap
     * </pre>
     */
    public AdmissionApplicationResponse createApplication(
            AdmissionApplicationCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[createApplication] Step 1: Checking the cycle is open");

        //! step 2 - the cycle, and it has to be taking applications. THIS IS THE MODULE'S GATE 4:
        //! every other module asks "is this the running year"; admissions asks "is this cycle
        //! open", because a cycle for a year that has not started is the normal case here.
        AdmissionCycle cycle = helper.loadOpenCycle(school, request.admissionCycleDocsId());

        //! step 3 - the class has to be one of the CYCLE'S year, not merely of the school.
        String classId = request.appliedClassDocsId().trim();
        log.info("[createApplication] Step 2: Checking class {} belongs to '{}'",
                classId, cycle.getAcademicYear());

        // TODO: read school class
        SchoolClass applied = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(),
                        cycle.getAcademicYear())
                .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                        "Class '" + classId + "' is not a class of '" + cycle.getAcademicYear()
                                + "', which is the year this cycle admits into."));

        //! step 4 - and it has to have seats. #3 refuses to open a cycle with an empty table, but
        //! a table can list some classes and not others, and applying for a class with no seats is
        //! an application that could never be offered anything.
        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();
        boolean hasSeats = seats.stream()
                .anyMatch(seat -> classId.equals(seat.getClassDocsId()));
        if (!hasSeats) {
            throw ApiException.conflict("CLASS_NOT_IN_CAPACITY",
                    "'" + cycle.getName() + "' has no seats set up for " + applied.getName()
                            + ". A class that is not in the seat table cannot be applied for — "
                            + "add it with the seat table endpoint first.");
        }

        //! step 5 - the inquiry, when one was named. Optional: a family that walks in with a
        //! completed form never enquired, and refusing them would be refusing the common case.
        String inquiryId = TextHelper.blankToNull(request.inquiryDocsId());
        Inquiry inquiry = null;
        if (inquiryId != null) {
            log.info("[createApplication] Step 3: Reading inquiry {}", inquiryId);

            // TODO: read inquiry
            inquiry = inquiries.findByIdAndSchoolId(inquiryId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                            "No inquiry with id '" + inquiryId + "' in this school."));

            //! ONE INQUIRY, ONE APPLICATION PER CYCLE. school_cycle_inquiry_uniq enforces it and
            //! would otherwise fire as a duplicate key error the handler turns into a 500. An
            //! inquiry is about ONE prospective child - it carries their name and date of birth -
            //! so two applications from it in one round is two applications for one child.
            // TODO: check admission application exists
            if (applications.existsBySchoolIdAndAdmissionCycleDocsIdAndInquiryDocsId(
                    school.getId(), cycle.getId(), inquiryId)) {
                throw ApiException.conflict("APPLICATION_ALREADY_EXISTS",
                        "That inquiry has already produced an application in '" + cycle.getName()
                                + "'. One inquiry is one child applying once per round — a second "
                                + "child needs their own inquiry.");
            }
        }

        //! step 6 - the guardians. Copied from the inquiry when there is one, then OVERRIDDEN by
        //! whatever the form carried: the parent filling it in is the one who signs, and editing
        //! the inquiry afterwards must not rewrite an application the school has acted on.
        List<InquiryGuardian> guardians = new ArrayList<>();
        for (AdmissionApplicationCreateRequest.Guardian sent : request.guardians()) {
            InquiryGuardian one = InquiryGuardian.builder()
                    .fullName(sent.fullName().trim())
                    .relation(sent.relation())
                    .phoneNumber(TextHelper.blankToNull(sent.phoneNumber()))
                    .emailAddress(TextHelper.blankToNull(sent.emailAddress()))
                    .address(TextHelper.blankToNull(sent.address()))
                    .occupation(TextHelper.blankToNull(sent.occupation()))
                    .primaryContact(sent.primaryContact() != null && sent.primaryContact())
                    .build();
            guardians.add(one);
        }

        //! step 7 - the answers. Nothing checks WHAT they are, so the only thing that can be
        //! checked is how many, which stops the field being an unbounded map a caller controls.
        var answers = request.formAnswers() == null
                ? new HashMap<String, Object>()
                : new HashMap<>(request.formAnswers());
        if (answers.size() > MAX_FORM_ANSWERS) {
            throw ApiException.badRequest("TOO_MANY_FORM_ANSWERS",
                    "That form carries " + answers.size() + " answers and the limit is "
                            + MAX_FORM_ANSWERS + ".");
        }

        //! step 8 - the number. Generated, never supplied: nobody picks their own application
        //! number, and a caller-supplied one lets two families collide.
        String applicationNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_APPLICATION, "APP/{YYYY}/{MM}/");
        log.info("[createApplication] Step 4: Allocated application number {}", applicationNo);

        //! step 9 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        AdmissionApplication application = AdmissionApplication.builder()
                .schoolId(school.getId())
                .applicationNo(applicationNo)
                .admissionCycleDocsId(cycle.getId())
                .inquiryDocsId(inquiryId)
                .appliedClassDocsId(classId)
                .applicantName(request.applicantName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .guardians(guardians)
                .status(AdmissionApplicationStatus.DRAFT)
                .formAnswers(answers)
                .evidenceDocumentDocsIds(new ArrayList<>())
                .build();

        //! step 10 - save
        // TODO: insert admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[createApplication] Step 5: Saved application {} as a DRAFT", saved.getId());

        //! step 11 - the lead, if there was one, has started an application.
        //! Done AFTER the application is saved, not before: if the insert fails, an inquiry that
        //! says APPLICATION_STARTED with no application is a worse lie than one that is late.
        if (inquiry != null) {
            inquiry.setStatus(InquiryStatus.APPLICATION_STARTED);

            // TODO: update inquiry
            inquiries.save(inquiry);
            log.info("[createApplication] Step 6: Moved inquiry {} to APPLICATION_STARTED",
                    inquiry.getId());
        }

        return AdmissionApplicationResponse.fromApplication(saved, applied.getName(),
                "'" + saved.getApplicantName() + "' has a DRAFT application for "
                        + applied.getName() + ". Submitting it is #19, which is not built, so it "
                        + "cannot move past DRAFT yet. " + NO_AUTHORIZATION_YET);
    }
}
