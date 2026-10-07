package com.orbitastra.backend.services.student.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.student.guardian.response.GuardianResponse;
import com.orbitastra.backend.dto.student.student.request.StudentCreateRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.repositories.student.guardian.GuardianRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.student.helper.StudentHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The reads and the preparation behind {@code StudentService}.
 *
 * <p>Everything here is a method the service used to hold. The service file holds its endpoints
 * and nothing else, so a repository read, a response builder and a one-caller guard all live here
 * regardless of how many callers they have.
 *
 * <p><b>Nothing here calls anything else here.</b> Two public methods that need the same small
 * rule share a {@code private static} instead, which is how the flat rule and "no duplicated
 * logic" both hold at once.
 *
 * <p><b>The module got a {@code helper/} on 2026-10-07</b>, when {@code GuardianService} arrived
 * and made the phone rule genuinely shared. {@code normalisePhone} moved there out of a
 * {@code private static} here: #1 matching a guardian and #7 refusing a duplicate write into the
 * same unique index, so the two have to agree character for character about the stored form of a
 * number.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentServiceUtils {

    /**
     * Ten digits is a whole Indian mobile number — the same rule and the same number
     * {@code StudentService} and {@code GuardianService} keep, because all three ask one question
     * about one index.
     */
    private static final int FULL_PHONE_DIGITS = 10;

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final StudentHelper helper;

    /**
     * One child, or a 404.
     *
     * <p><b>The school is in the query, never checked after the read.</b> An id from another
     * school is a real id; reading it without the school would find it and hand over a child's
     * date of birth, their address and their guardians' phone numbers. A check written after the
     * read is one {@code if} away from being forgotten.
     *
     * <p><b>A 404 and not a 403</b>, for the same reason as everywhere else: a 403 would confirm
     * the child exists.
     *
     * <p>It trims and tolerates null, so a blank piece of the path is a 404 about an empty id
     * rather than a 500 about a null.
     *
     * Used by:
     * - getStudent()
     */
    public Student loadStudent(School school, String studentDocsId) {
        String id = studentDocsId == null ? "" : studentDocsId.trim();

        // TODO: read student
        return students.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + id + "' in this school."));
    }

    /**
     * Turns the guardians on an admission form into links on the child, finding the people who are
     * already here instead of writing them again.
     *
     * <p><b>This is the whole difficulty of #1.</b> A phone number identifies one person within a
     * school — {@code school_guardian_phone_uniq} is unique and the database will not be talked out
     * of it — and <b>two siblings share a father</b>. Writing a guardian row per child fails with a
     * duplicate key error the first time a second child in a family is admitted, which is a 500 on
     * an ordinary Tuesday morning at the front desk.
     *
     * <p><b>Changed 2026-10-07: a taken number is refused, not matched.</b> It used to link
     * whoever held the number, silently — so typing "ANKIT KUMAR" on a number the school already
     * had for "Hero" returned a child whose father was <i>Hero</i>, a different person, with no
     * warning. That is the right answer for a sibling and an alarming one for everybody else, and
     * <b>this method cannot tell which it is looking at</b>. So the caller says:
     *
     * <ul>
     *   <li><b>{@code guardianDocsId} sent</b> — that person is linked as they are, and the typed
     *       name and number are ignored. This is how a sibling's father is attached.</li>
     *   <li><b>left out</b> — the phone and the email must be free, or
     *       {@code 409 GUARDIAN_PHONE_TAKEN} / {@code GUARDIAN_EMAIL_TAKEN}, <b>naming the holder
     *       and quoting their id</b> so the caller can send it back if it really is them.</li>
     * </ul>
     *
     * <p><b>{@code matchByNumber} is the one exception, and only {@code crm} #33 passes it.</b> An
     * admission form's guardians were typed by the family months ago; refusing at the handover
     * would strand a family who hold an accepted offer, so the enrolment links what it finds.
     *
     * <p><b>The refusal compares digits</b> — the loose rule #7, #8 and #9 use, so the check a
     * desk makes before admitting and the refusal they get here agree about one number.
     *
     * <p><b>A guardian with no phone and no email is always a new row.</b> Nothing identifies them,
     * so a family that leaves both blank will slowly collect duplicates. Known, accepted, and said
     * out loud rather than solved badly.
     *
     * Used by:
     * - createStudent()
     */
    public PreparedGuardians linkGuardians(School school,
            List<StudentCreateRequest.GuardianRequest> asked, boolean matchByNumber) {

        //! step 1 - two people on one form cannot be the same person. The second would silently
        //! match the first and the child would end up with one contact listed twice, which reads
        //! as "we have their mother and father" and is not.
        Set<String> phonesOnTheForm = new HashSet<>();
        Set<String> emailsOnTheForm = new HashSet<>();
        int primaries = 0;

        for (StudentCreateRequest.GuardianRequest one : asked) {
            String phone = helper.normalisePhone(one.phoneNumber());
            String email = TextHelper.lowercaseOrNull(one.emailAddress());

            if (phone != null && !phonesOnTheForm.add(phone)) {
                throw ApiException.badRequest("DUPLICATE_GUARDIAN_IN_REQUEST",
                        "Two guardians on this form have the phone number " + phone + ". A number "
                                + "belongs to one person in a school, so the second would be "
                                + "saved as the first — give each contact their own number, or "
                                + "leave the second one's blank.");
            }
            if (email != null && !emailsOnTheForm.add(email)) {
                throw ApiException.badRequest("DUPLICATE_GUARDIAN_IN_REQUEST",
                        "Two guardians on this form have the email address " + email + ". An "
                                + "address belongs to one person here, the same as a number.");
            }
            if (Boolean.TRUE.equals(one.primaryContact())) {
                primaries++;
            }
        }

        //! step 2 - exactly one person to ring. None means "ring the family" has no answer; two
        //! means it has two, which is the same problem wearing a different face.
        if (primaries != 1) {
            throw ApiException.badRequest("PRIMARY_CONTACT_REQUIRED",
                    primaries == 0
                            ? "No guardian on this form is marked as the primary contact. The "
                                    + "school needs one person to ring first — set "
                                    + "'primaryContact': true on exactly one of them."
                            : primaries + " guardians are marked as the primary contact. Only one "
                                    + "person can be rung first — set 'primaryContact': true on "
                                    + "exactly one of them.");
        }

        //! step 3 - find or write each one, in the order they were sent so the answer lines up
        //! with the form somebody filled in.
        List<GuardianLink> links = new ArrayList<>();
        List<GuardianResponse> answers = new ArrayList<>();

        for (StudentCreateRequest.GuardianRequest one : asked) {
            String phone = helper.normalisePhone(one.phoneNumber());
            String email = TextHelper.lowercaseOrNull(one.emailAddress());

            //! step 3a - DID THE CALLER NAME AN EXISTING PERSON? That is the only way a
            //! guardian gets linked through #1 now. "Yes, this is that person" is a decision, and
            //! this method cannot make it: linking is right for a sibling's father and alarming
            //! for a family that mistyped a digit, and the two look identical from here.
            Optional<Guardian> found = Optional.empty();
            String namedId = TextHelper.blankToNull(one.guardianDocsId());

            if (namedId != null) {
                // TODO: read guardian (the person the caller asked to link)
                found = guardians.findByIdAndSchoolId(namedId, school.getId());
                if (found.isEmpty()) {
                    throw ApiException.notFound("GUARDIAN_NOT_FOUND",
                            "No guardian with id '" + namedId + "' in this school, so '"
                                    + one.fullName() + "' cannot be linked to them. Find the "
                                    + "right one with #9, or leave guardianDocsId out and a new "
                                    + "guardian is written.");
                }
            } else if (matchByNumber) {
                //! THE OLD BEHAVIOUR, AND NOW ONLY FOR crm #33. An admission form's guardians
                //! were typed by the family months ago; refusing at the handover would strand a
                //! family who hold an accepted offer, so the enrolment links what it finds.
                if (phone != null) {
                    // TODO: read guardian (is this person already in the school)
                    found = guardians.findBySchoolIdAndPhoneNumber(school.getId(), phone);
                }
                if (found.isEmpty() && email != null) {
                    // TODO: read guardian (is this person already in the school)
                    found = guardians.findBySchoolIdAndEmailAddress(school.getId(), email);
                }
            } else {
                //! step 3b - NOBODY WAS NAMED, so the number and the address have to be FREE.
                //!
                //! CHANGED 2026-10-07. This used to link whoever held the number, silently:
                //! typing "ANKIT KUMAR" on a number the school already had for "Hero" returned a
                //! child whose father was Hero. Correct for a sibling, alarming for everybody
                //! else, and indistinguishable from here — so the caller decides now.
                //!
                //! LOOSE ON THE PHONE, the same rule #7, #8 and #9 use, so the check a desk makes
                //! before admitting and the refusal they get here agree about one number.
                if (phone != null) {
                    String digits = helper.digitsOf(phone);
                    boolean wholeNumber = digits.length() < FULL_PHONE_DIGITS;
                    String needle = wholeNumber ? digits
                            : digits.substring(digits.length() - FULL_PHONE_DIGITS);

                    // TODO: read guardian (is this number already somebody's)
                    Guardian holder = guardians
                            .findByLoosePhone(school.getId(), needle, wholeNumber, false, 1)
                            .stream().findFirst().orElse(null);
                    if (holder != null) {
                        throw ApiException.conflict("GUARDIAN_PHONE_TAKEN",
                                "'" + holder.getFullName() + "' already has the phone number "
                                        + holder.getPhoneNumber() + " in this school, which is "
                                        + "the same number as " + phone + ". If that is the same "
                                        + "person, send their guardianDocsId "
                                        + holder.getId() + " to link them deliberately. If it is "
                                        + "not, the number is wrong on one of the two.");
                    }
                }

                if (email != null) {
                    // TODO: read guardian (is this address already somebody's)
                    Guardian holder = guardians
                            .findBySchoolIdAndEmailAddress(school.getId(), email).orElse(null);
                    if (holder != null) {
                        throw ApiException.conflict("GUARDIAN_EMAIL_TAKEN",
                                "'" + holder.getFullName() + "' already has the email address "
                                        + email + " in this school. If that is the same person, "
                                        + "send their guardianDocsId " + holder.getId()
                                        + " to link them deliberately.");
                    }
                }
            }

            Guardian person;
            boolean matched = found.isPresent();

            if (matched) {
                //! step 3c - LINK THEM AND LEAVE THEM ALONE. Not even the name is updated: a new
                //! spelling on this form is not proof the old one was wrong, and correcting a
                //! guardian is #8 — which changes them for every child they belong to, and is not
                //! something admitting one child should do by accident.
                person = found.get();
                log.info("[linkGuardians] Linking '{}' (id {}) as asked, rather than writing a "
                        + "second record", person.getFullName(), person.getId());
            } else {
                //! step 3d - nobody here is them, so write them. Two steps: build it, then save
                //! it, with schoolId set by hand — SchoolBase declares it required but nothing
                //! checks a document on the way to the database, and a guardian written without it
                //! is invisible to every query this school makes.
                Guardian fresh = Guardian.builder()
                        .schoolId(school.getId())
                        .fullName(one.fullName().trim())
                        .phoneNumber(phone)
                        .alternatePhoneNumber(helper.normalisePhone(one.alternatePhoneNumber()))
                        .emailAddress(email)
                        .address(TextHelper.blankToNull(one.address()))
                        .occupation(TextHelper.blankToNull(one.occupation()))
                        .preferredLanguage(one.preferredLanguage())
                        .build();

                // TODO: insert guardian
                person = guardians.save(fresh);
                log.info("[linkGuardians] '{}' is new to this school, so saved them as a new "
                        + "guardian (id {})", person.getFullName(), person.getId());
            }

            //! step 3e - the role, which belongs to this child and not to the person. The same
            //! man is "father, primary contact, allowed to collect" for one child and only an
            //! emergency number for their cousin, so the flags live on the link.
            GuardianLink link = GuardianLink.builder()
                    .guardianDocsId(person.getId())
                    .relation(one.relation())
                    .primaryContact(Boolean.TRUE.equals(one.primaryContact()))
                    .emergencyContact(Boolean.TRUE.equals(one.emergencyContact()))
                    .pickupAuthorized(Boolean.TRUE.equals(one.pickupAuthorized()))
                    .portalAccess(Boolean.TRUE.equals(one.portalAccess()))
                    .build();

            links.add(link);
            answers.add(GuardianResponse.of(person, link, matched));
        }

        return new PreparedGuardians(links, answers);
    }

    /**
     * The people behind a child's guardian links, as one read.
     *
     * <p><b>One query for the whole child, not one per contact.</b> A child with three guardians
     * is one read; asking per link is the N+1 this project keeps naming, and it is worse here than
     * it looks because a page of children would multiply it.
     *
     * <p><b>A link whose person has been deleted is skipped rather than drawn as a blank.</b> It
     * should not be possible — nothing deletes a guardian today — but a row of empty fields where
     * a mother's name belongs is the kind of thing somebody reports as a bug in the screen.
     *
     * Used by:
     * - getStudent()
     */
    public List<GuardianResponse> guardiansOf(School school, Student child) {
        List<GuardianLink> links = child.getGuardians();
        if (links == null || links.isEmpty()) {
            return List.of();
        }

        List<String> ids = new ArrayList<>();
        for (GuardianLink link : links) {
            if (link.getGuardianDocsId() != null) {
                ids.add(link.getGuardianDocsId());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }

        // TODO: read guardians (the people this child's links point at)
        List<Guardian> people = guardians.findBySchoolIdAndIdIn(school.getId(), ids);

        Map<String, Guardian> byId = new HashMap<>();
        for (Guardian person : people) {
            byId.put(person.getId(), person);
        }

        //! THE ORDER IS THE CHILD'S, not the database's. The first guardian on the form is the one
        //! the family wrote first, and a page that reordered them would look wrong to the person
        //! who filled it in.
        List<GuardianResponse> answers = new ArrayList<>();
        for (GuardianLink link : links) {
            Guardian person = byId.get(link.getGuardianDocsId());
            if (person != null) {
                answers.add(GuardianResponse.of(person, link));
            }
        }
        return answers;
    }

    /**
     * What to tell the caller after a child has been admitted.
     *
     * <p>On the response rather than only in a README, because the next step is a different
     * endpoint in a different module and nothing about the answer says so.
     *
     * Used by:
     * - createStudent()
     */
    public String nextStepFor(Student child) {
        return "'" + child.getFullName() + "' is admitted as " + child.getAdmissionNo()
                + " and has no class yet. Placing a child in a class and section is student #14, "
                + "which is not built — so this child will read as not placed until it is.";
    }

    /**
     * The guardians of one child, ready to be written and ready to be answered with.
     *
     * <p>Two lists rather than one because they are two different things: {@code links} goes onto
     * the student document, and {@code answers} is what the caller reads — the same people, plus
     * whether each was found or created.
     */
    public record PreparedGuardians(List<GuardianLink> links, List<GuardianResponse> answers) {
    }
}
