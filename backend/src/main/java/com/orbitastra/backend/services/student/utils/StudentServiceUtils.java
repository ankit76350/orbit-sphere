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
 * <p><b>There is no {@code helper/} file in this module, and that is not an omission.</b> A helper
 * is for what the main service and the utils <i>both</i> use, and there is no such thing here yet
 * — the module has one service. {@code services/people} is laid out the same way for the same
 * reason. The day a second service appears, whatever the two share moves into one.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentServiceUtils {

    // What people type into phone numbers and nobody stores: spaces (including the non-breaking
    // one that comes from pasting out of a spreadsheet), brackets, hyphens and dots.
    private static final String PHONE_NOISE = "[\\s\\u00A0()\\-.]";

    private final StudentRepository students;
    private final GuardianRepository guardians;

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
     * <p><b>What it does, per guardian:</b> look them up by phone; if that finds nobody, look them
     * up by email; if that finds nobody either, write a new one. A match <b>links the person and
     * leaves their stored name alone</b> — a new spelling on this form is not evidence that the
     * old one was wrong, and a school that renamed somebody's father by admitting their sister
     * would have no way of noticing.
     *
     * <p><b>The match is on the number after the spaces and brackets come off, not on the digits.</b>
     * So "+91 98765 43210" and "+919876543210" are one person. <b>A number given with a country
     * code one time and without it the next is still two rows</b> — "+919876543210" and
     * "9876543210" are different strings, and that is exactly what the unique index thinks too.
     * Matching more loosely than the index would mean this method and the database disagreed about
     * who is who, which is worse than the duplicate.
     *
     * <p><b>A guardian with no phone and no email is always a new row.</b> Nothing identifies them,
     * so a family that leaves both blank will slowly collect duplicates. Known, accepted, and said
     * out loud rather than solved badly.
     *
     * Used by:
     * - createStudent()
     */
    public PreparedGuardians linkGuardians(School school,
            List<StudentCreateRequest.GuardianRequest> asked) {

        //! step 1 - two people on one form cannot be the same person. The second would silently
        //! match the first and the child would end up with one contact listed twice, which reads
        //! as "we have their mother and father" and is not.
        Set<String> phonesOnTheForm = new HashSet<>();
        Set<String> emailsOnTheForm = new HashSet<>();
        int primaries = 0;

        for (StudentCreateRequest.GuardianRequest one : asked) {
            String phone = stripPhone(one.phoneNumber());
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
            String phone = stripPhone(one.phoneNumber());
            String email = TextHelper.lowercaseOrNull(one.emailAddress());

            //! step 3a - is this person already here? The phone first, because that is what the
            //! unique index means by "the same person", then the email for the family that gives
            //! an address and no number.
            Optional<Guardian> found = Optional.empty();
            if (phone != null) {
                // TODO: read guardian (is this person already in the school)
                found = guardians.findBySchoolIdAndPhoneNumber(school.getId(), phone);
            }
            if (found.isEmpty() && email != null) {
                // TODO: read guardian (is this person already in the school)
                found = guardians.findBySchoolIdAndEmailAddress(school.getId(), email);
            }

            Guardian person;
            boolean matched = found.isPresent();

            if (matched) {
                //! step 3b - LINK THEM AND LEAVE THEM ALONE. Not even the name is updated: a new
                //! spelling on this form is not proof the old one was wrong, and overwriting it is
                //! how one child's father quietly becomes another's.
                person = found.get();
                log.info("[linkGuardians] Found '{}' already in the school, so linking them "
                        + "instead of making a second record (id {})",
                        person.getFullName(), person.getId());
            } else {
                //! step 3c - nobody here is them, so write them. Two steps: build it, then save
                //! it, with schoolId set by hand — SchoolBase declares it required but nothing
                //! checks a document on the way to the database, and a guardian written without it
                //! is invisible to every query this school makes.
                Guardian fresh = Guardian.builder()
                        .schoolId(school.getId())
                        .fullName(one.fullName().trim())
                        .phoneNumber(phone)
                        .alternatePhoneNumber(stripPhone(one.alternatePhoneNumber()))
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

            //! step 3d - the role, which belongs to this child and not to the person. The same
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
     * Just the digits somebody typed, for comparing two spellings of one phone number.
     *
     * <p>Different from what is <i>stored</i>, which keeps a leading "+". This is for asking a
     * question about a number, not for writing one down.
     *
     * Used by:
     * - findKnownChild()
     */
    public String digitsOf(String typed) {
        if (typed == null) {
            return "";
        }

        StringBuilder digits = new StringBuilder();
        for (char each : typed.toCharArray()) {
            if (each >= '0' && each <= '9') {
                digits.append(each);
            }
        }
        return digits.toString();
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
     * Strips what people type into a phone number and nobody stores.
     *
     * <p>Private and static, so {@link #linkGuardians} can use it in three places without this
     * class reaching into itself. A number made of nothing but punctuation — "( )", "--" — comes
     * back as null rather than as an empty string, because a blank number stored behind a request
     * that looked filled in is worse than no number.
     */
    private static String stripPhone(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.replaceAll(PHONE_NOISE, "");
        return stripped.isEmpty() ? null : stripped;
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
