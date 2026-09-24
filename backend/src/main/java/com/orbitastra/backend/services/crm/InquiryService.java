package com.orbitastra.backend.services.crm;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryCreateRequest;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The lead half of admissions. Endpoint #8 of the plan in this package's README; #9 to #16 are not
 * built.
 *
 * <p><b>The module's other four collections were built first, and that was deliberate.</b> An
 * application does <i>not</i> need an inquiry — {@code inquiryDocsId} is nullable, for the family
 * that walks in with a completed form — so the whole pipeline is testable end to end without a
 * single lead in the database. Leads were the one block nothing else depended on.
 *
 * <p><b>Which left two write paths nothing could reach.</b> #17 moves a named lead to
 * {@code APPLICATION_STARTED} and #19 to {@code APPLICATION_SUBMITTED}, and until #8 existed there
 * was no way to create an inquiry through the API at all — those branches were only reachable by
 * writing to Mongo directly, which is how the suites have been exercising them.
 *
 * <p><b>No {@code utils} file.</b> One public method cannot repeat a read; it gets one when #9
 * arrives and the two share a load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    private final InquiryRepository inquiries;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;

    /**
     * Endpoint #8 — the front desk captures a lead.
     *
     * <p><b>Almost everything is optional, and that is the point.</b> A phone call is "a mother
     * rang about her son for next year" — a name, a year, and nothing else. An endpoint that
     * demanded a date of birth and a guardian's email would refuse the commonest lead there is,
     * and the front desk would stop using it.
     *
     * <p><b>It creates {@code NEW} and nothing else.</b> Every other status is somebody having
     * done something — #10 logs a call, #12 moves it, #17 and #19 move it as a side effect of the
     * family applying. A lead captured is a lead nobody has acted on yet.
     *
     * <p><b>It does NOT check for duplicates.</b> #15 is what asks "is this family already known",
     * and it is asked <i>before</i> this — by the person at the desk, who can see the answer and
     * decide. Refusing here would be this endpoint guessing that two children with one phone
     * number are the same enquiry, when a family with two children is exactly that.
     */
    public InquiryResponse createInquiry(InquiryCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[createInquiry] Step 1: Capturing a lead for school {}", school.getId());

        //! step 2 - the year has to exist. NOT the running one: a lead is about an intake that has
        //! not started, which is the normal case rather than the edge — the same reading #1 makes
        //! about a cycle.
        String year = request.academicYear().trim();

        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the class they are interested in, when they named one. OPTIONAL: most phone
        //! calls do not. It has to be a class of THAT year — a lead about a class the year does
        //! not have is a lead nobody can act on.
        String classId = TextHelper.blankToNull(request.interestedClassDocsId());
        SchoolClass interested = null;

        if (classId != null) {
            // TODO: read school class
            interested = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(), year)
                    .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                            "Class '" + classId + "' is not a class of '" + year + "', which is "
                                    + "the year this lead is about."));
        }

        //! step 4 - the counsellor, when the desk hands it straight to somebody. OPTIONAL because
        //! most leads are captured first and assigned after — #11 is what assigns one later.
        String counselorId = TextHelper.blankToNull(request.assignedCounselorDocsId());
        Staff counselor = null;

        if (counselorId != null) {
            // TODO: read staff
            counselor = staff.findByIdAndSchoolId(counselorId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + counselorId + "' in this school, so the "
                                    + "lead cannot be given to them."));
        }

        //! step 5 - the number. Generated, never supplied: nobody picks their own inquiry number.
        String inquiryNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_INQUIRY, "INQ/{YYYY}/{MM}/");
        log.info("[createInquiry] Step 2: Allocated inquiry number {}", inquiryNo);

        //! step 6 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        //!
        //! GUARDIANS ARE OPTIONAL AND SO IS EVERY FIELD ON ONE. A walk-in who gives a child's name
        //! and a phone number is a real lead; a record that insisted on a relation would refuse it.
        //! #15 finds a family again by phone or email, and it can only find the ones who left one.
        Inquiry inquiry = Inquiry.builder()
                .schoolId(school.getId())
                .inquiryNo(inquiryNo)
                .prospectiveStudentName(request.prospectiveStudentName().trim())
                .academicYear(year)
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .interestedClassDocsId(interested == null ? null : interested.getId())
                .status(InquiryStatus.NEW)
                .assignedCounselorDocsId(counselor == null ? null : counselor.getId())
                .source(TextHelper.blankToNull(request.source()))
                .sourceDetails(TextHelper.blankToNull(request.sourceDetails()))
                .notes(TextHelper.blankToNull(request.notes()))
                .guardians(request.guardians() == null ? new ArrayList<>()
                        : request.guardians().stream()
                                .map(one -> InquiryGuardian.builder()
                                        .fullName(TextHelper.blankToNull(one.fullName()))
                                        .relation(one.relation())
                                        .phoneNumber(TextHelper.blankToNull(one.phoneNumber()))
                                        .emailAddress(TextHelper.blankToNull(one.emailAddress()))
                                        .address(TextHelper.blankToNull(one.address()))
                                        .occupation(TextHelper.blankToNull(one.occupation()))
                                        .primaryContact(one.primaryContact() != null
                                                && one.primaryContact())
                                        .build())
                                .collect(Collectors.toCollection(ArrayList::new)))
                .followUps(new ArrayList<>())
                .build();

        //! step 7 - save
        // TODO: insert inquiry
        Inquiry saved = inquiries.save(inquiry);
        log.info("[createInquiry] Step 3: Captured lead {} as {}", saved.getId(), inquiryNo);

        return InquiryResponse.fromInquiry(saved,
                interested == null ? null : interested.getName(),
                counselor == null ? null : counselor.getFullName(),
                nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * What to do with this lead next, in plain words.
     *
     * <p>Private and inline: one caller, and the folder rules keep single-use logic where it is
     * used. It moves to {@code utils} when #9 also answers with it.
     */
    private static String nextStepFor(Inquiry inquiry) {
        String assigned = inquiry.getAssignedCounselorDocsId() == null
                ? " Nobody is looking after it yet — #11 gives it to a counsellor, and is not "
                        + "built."
                : " It is somebody's to chase.";

        return "Captured as NEW."
                + assigned
                + " #10 logs what happens on a call and #12 moves it along; neither is built, so "
                + "this lead cannot move from NEW yet. What CAN happen to it is an application: "
                + "#17 takes one naming this lead and moves it to APPLICATION_STARTED, and #19 to "
                + "APPLICATION_SUBMITTED — those are the only two statuses anything writes today.";
    }
}
