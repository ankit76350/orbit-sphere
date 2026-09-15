package com.orbitastra.backend.services.people;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.people.staff.request.StaffCreateRequest;
import com.orbitastra.backend.dto.people.staff.response.StaffCreatedResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.institution.NumberSequenceService;
import com.orbitastra.backend.services.people.utils.StaffServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The people a school employs — endpoints #1 to #8 of the plan in
 * {@code controllers/people/staff/README.md}. #1 is built.
 *
 * <p><b>The person and the job are two documents, and that is the whole design.</b> {@code Staff}
 * has no status, no department, no designation and no joining date. Every field on it is a fact
 * about a human being; everything about the employment lives on {@code EmploymentRecord}, which
 * #16 writes. Somebody created here and not yet hired is a real state, not a half-finished one.
 *
 * <p><b>This is the second half of the module's phase 1.</b> #9 and #13 built the seat; this
 * builds the person. #16 is what joins them, and five other modules are waiting on the
 * {@code staffDocsId} this hands back.
 *
 * <p><b>The gates run in the controller</b>, not here.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final CurrentSchoolResolver currentSchool;
    private final StaffRepository staff;
    private final NumberSequenceService numberSequences;
    private final StaffServiceUtils utils;

    private static final String NO_AUTHORIZATION_YET =
            "This module has no authorization yet, so treat every field on it as readable by "
                    + "anybody who can reach the API.";

    /**
     * Endpoint #1 — create a person.
     *
     * <p><b>{@code employeeNo} is generated, never accepted.</b> Two schools' numbering
     * conventions must not be able to collide inside one tenant, and nobody should pick their own
     * staff number. The sequence is allocated atomically, so two simultaneous creates cannot be
     * handed the same one.
     *
     * <p><b>A phone number and an email address each identify one person within a school.</b> The
     * module plan said the opposite about email — "two staff genuinely may share a family address"
     * — and that was overruled on 2026-09-15. Both are now refused as duplicates.
     */
    public StaffCreatedResponse createStaff(StaffCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the phone and the email, normalised BEFORE they are checked.
        //!
        //! NORMALISE FIRST OR THE CHECK IS A LIE. "+91 98765-43210" and "+919876543210" are one
        //! number and "Anita@X.com" and "anita@x.com" are one address - checking the raw strings
        //! would pass both, store both, and leave the school with a duplicate the index would
        //! have refused had it ever been built.
        String phoneNumber = utils.normalisePhone(request.phoneNumber());
        String emailAddress = TextHelper.lowercaseOrNull(request.emailAddress());

        //! step 3 - and each has to be free.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the indexes.
        //! school_staff_phone_uniq and school_staff_email_uniq are declared on the model but built
        //! on demand (app.mongo.sync-indexes), so a database that has never synced carries no such
        //! constraint at all - where they ARE built, this turns a duplicate-key 500 into a 409
        //! that says which field and what to do.
        //!
        //! BOTH INDEXES ARE PARTIAL, so a person with no phone and no email is always allowed:
        //! only a value that is actually there has to be unique.
        if (phoneNumber != null) {
            // TODO: check staff exists
            if (staff.existsBySchoolIdAndPhoneNumber(school.getId(), phoneNumber)) {
                throw ApiException.conflict("STAFF_PHONE_TAKEN",
                        "Somebody in this school already has the phone number " + phoneNumber
                                + ". A number identifies one person here — check whether they are "
                                + "already on the books before entering them again.");
            }
        }

        if (emailAddress != null) {
            // TODO: check staff exists
            if (staff.existsBySchoolIdAndEmailAddress(school.getId(), emailAddress)) {
                throw ApiException.conflict("STAFF_EMAIL_TAKEN",
                        "Somebody in this school already has the email address " + emailAddress
                                + ". An address identifies one person here — check whether they "
                                + "are already on the books before entering them again.");
            }
        }

        //! step 4 - take an employee number. ATOMIC, so two requests can never be handed the same
        //! one - see NumberSequenceService. The counter itself was created when the school was
        //! provisioned, with no template; the first caller's template is written onto it and every
        //! later number in the school's life reads the same shape.
        //!
        //! "EMP/{YYYY}/{MM}/" with the counter's width of 6 gives EMP/2026/09/000001.
        //!
        //! THE SHAPE IS FIXED ONCE PER SCHOOL, not once per release. A school that already
        //! allocated a number under an older template keeps it - changing this line does not
        //! restyle numbers that have already been written down, and it must not.
        String employeeNo = numberSequences.next(school.getId(),
                NumberSequenceType.EMPLOYEE_NUMBER, "EMP/{YYYY}/{MM}/");

        //! step 5 - insert.
        //!
        //! schoolId IS SET EXPLICITLY, and that is not boilerplate. SchoolBase declares it
        //! @NotBlank but nothing validates a document on save: a person written without it is
        //! stored, invisible to every tenant-scoped query, and found only by reading the raw
        //! collection. That exact bug shipped in this project's term create on 2026-09-11.
        //!
        //! NO active, NO status, NO department, NO joining date - there are none on the document.
        //! A person is not employed by existing; #16 writes the job.
        // TODO: create staff
        Staff saved = staff.save(Staff.builder()
                .schoolId(school.getId())
                .employeeNo(employeeNo)
                .fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .nationalityCode(TextHelper.uppercaseOrNull(request.nationalityCode()))
                .preferredLanguage(TextHelper.blankToNull(request.preferredLanguage()))
                .phoneNumber(phoneNumber)
                .emailAddress(emailAddress)
                .currentAddress(utils.toAddress(request.currentAddress()))
                .permanentAddress(utils.toAddress(request.permanentAddress()))
                .emergencyContact(utils.toEmergencyContact(request.emergencyContact()))
                .profileImageDocsId(TextHelper.blankToNull(request.profileImageDocsId()))
                .build());

        return StaffCreatedResponse.fromStaff(saved,
                "This person exists but is not employed yet — #16 writes the job, and needs "
                        + "staffDocsId " + saved.getId() + " and a positionDocsId. "
                        + NO_AUTHORIZATION_YET);
    }
}
