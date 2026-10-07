package com.orbitastra.backend.services.student;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.student.guardian.request.GuardianCreateRequest;
import com.orbitastra.backend.dto.student.guardian.response.GuardianDetailResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.repositories.student.guardian.GuardianRepository;
import com.orbitastra.backend.services.student.helper.StudentHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The people a school contacts about its children. Endpoint #7 of the plan in
 * {@code controllers/student/README.md}; #8 to #13 are not built.
 *
 * <p><b>Its own service because {@code guardians} is its own collection</b>, and because a guardian
 * outlives any one child: the same person belongs to siblings, and correcting their number changes
 * it for all of them. Folding this into {@code StudentService} would put a shared document's rules
 * inside the thing that merely happens to create most of them.
 *
 * <p><b>A guardian is one real person per school, and the database decided that</b>, not this
 * service. {@code school_guardian_phone_uniq} and {@code school_guardian_email_uniq} are unique and
 * partial, so a value that is there has to be the only one. Everything interesting in this module
 * follows from that single fact.
 *
 * <p><b>#1 and #7 obey it in opposite ways, deliberately.</b> #1 is describing a family and so
 * matches an existing contact and links them; #7 is asserting a new person and so refuses. See
 * {@code GuardianCreateRequest} for the full reasoning — it is the one thing about this module
 * worth reading before using it.
 *
 * <p><b>No gate 4.</b> A contact has nothing to do with which academic year is running.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    private final GuardianRepository guardians;
    private final CurrentSchoolResolver currentSchool;
    private final StudentHelper helper;

    /**
     * Endpoint #7 — <b>add a guardian who is not being created with a child</b>.
     *
     * <p><b>For the guardian who turns up on their own:</b> a grandmother added before the child
     * she will collect, an emergency number the office wants on file, a family entered ahead of an
     * admission.
     *
     * <p><b>No relation and no flags.</b> "Father", "primary contact", "may collect" are facts
     * about a person <i>and a child together</i>, so they live on the link — #11. A guardian made
     * here belongs to nobody yet, which is a normal state rather than a half-finished one.
     *
     * <p><b>It refuses a taken number where #1 matches one.</b> The difference is what the caller
     * is saying: #1 describes a family, this asserts a new person. Handing back an existing row
     * would look like a successful create and leave somebody believing a guardian exists that
     * does not.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4.
     */
    public GuardianDetailResponse createGuardian(GuardianCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this writes.
        School school = currentSchool.requireUsable();
        log.info("[createGuardian] Step 1: Adding guardian '{}' to school {}",
                request.fullName(), school.getId());

        //! step 2 - the phone and the email, normalised BEFORE they are checked.
        //!
        //! NORMALISE FIRST OR THE CHECK IS A LIE. "+91 98765-43210" and "+919876543210" are one
        //! number and "Anita@X.com" and "anita@x.com" are one address — checking the raw strings
        //! would pass both, store both, and leave the school with a duplicate the index was
        //! supposed to stop. Through the module helper, because #1 has to normalise the same way
        //! or the two disagree about who is already here.
        String phoneNumber = helper.normalisePhone(request.phoneNumber());
        String emailAddress = TextHelper.lowercaseOrNull(request.emailAddress());

        //! step 3 - and each has to be free.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the indexes. Both are
        //! declared on the model and built on demand (app.mongo.sync-indexes), so a database that
        //! has never synced carries no such constraint at all — measured 2026-10-06, the
        //! guardians collection has only _id_. Where they ARE built, this turns a duplicate-key
        //! 500 into a 409 that says which field and who holds it.
        //!
        //! THE REFUSAL NAMES THE PERSON, so the caller can go and look at them: either they have
        //! the wrong number, or they meant to edit that contact rather than add one.
        if (phoneNumber != null) {
            // TODO: read guardian (is this number already somebody's)
            Guardian holder = guardians.findBySchoolIdAndPhoneNumber(school.getId(), phoneNumber)
                    .orElse(null);
            if (holder != null) {
                throw ApiException.conflict("GUARDIAN_PHONE_TAKEN",
                        "'" + holder.getFullName() + "' already has the phone number "
                                + phoneNumber + " in this school. A number identifies one person "
                                + "here — correct that guardian with #8, or attach them to a "
                                + "child "
                                + "with #11, neither of which is built yet.");
            }
        }

        if (emailAddress != null) {
            // TODO: read guardian (is this address already somebody's)
            Guardian holder = guardians.findBySchoolIdAndEmailAddress(school.getId(), emailAddress)
                    .orElse(null);
            if (holder != null) {
                throw ApiException.conflict("GUARDIAN_EMAIL_TAKEN",
                        "'" + holder.getFullName() + "' already has the email address "
                                + emailAddress + " in this school. An address identifies one "
                                + "person here, the same as a number.");
            }
        }

        //! step 4 - build the person.
        //!
        //! schoolId IS SET BY HAND, and that is not boilerplate. SchoolBase marks it required but
        //! nothing checks a document on the way to the database: a contact saved without it is
        //! stored, invisible to every query this school makes, and found only by reading the raw
        //! collection.
        Guardian person = Guardian.builder()
                .schoolId(school.getId())
                .fullName(request.fullName().trim())
                .phoneNumber(phoneNumber)
                .alternatePhoneNumber(helper.normalisePhone(request.alternatePhoneNumber()))
                .emailAddress(emailAddress)
                .address(TextHelper.blankToNull(request.address()))
                .occupation(TextHelper.blankToNull(request.occupation()))
                .preferredLanguage(request.preferredLanguage())
                .build();

        //! step 5 - save it. Built above, written here: two steps, so what is being stored can be
        //! read before the line that stores it.
        // TODO: insert guardian
        Guardian saved = guardians.save(person);
        log.info("[createGuardian] Step 2: Saved the guardian (id={})", saved.getId());

        return GuardianDetailResponse.of(saved,
                "'" + saved.getFullName() + "' is on file and attached to nobody. Linking them to "
                        + "a child is #11, which is not built — until then this guardian is "
                        + "findable and unused. " + NO_AUTHORIZATION_YET);
    }
}
