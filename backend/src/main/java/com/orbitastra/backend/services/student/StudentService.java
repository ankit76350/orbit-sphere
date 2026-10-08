package com.orbitastra.backend.services.student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.student.guardian.response.GuardianResponse;
import com.orbitastra.backend.dto.student.student.request.StudentCreateRequest;
import com.orbitastra.backend.dto.student.student.request.StudentGuardianLinkRequest;
import com.orbitastra.backend.dto.student.student.request.StudentMatchRequest;
import com.orbitastra.backend.dto.student.student.request.StudentSearchRequest;
import com.orbitastra.backend.dto.student.student.request.StudentUpdateRequest;
import com.orbitastra.backend.dto.student.student.response.StudentResponse;
import com.orbitastra.backend.dto.student.student.response.StudentRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.student.guardian.GuardianRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.institution.NumberSequenceService;
import com.orbitastra.backend.services.student.helper.StudentHelper;
import com.orbitastra.backend.services.student.utils.StudentServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The child, once the school has accepted them. Endpoints #1, #4, #5 and #6 of the plan in
 * {@code controllers/student/README.md}.
 *
 * <p><b>These four and no others, on purpose.</b> They are what
 * {@code controllers/README.md} calls phase 5 — "the minimum, not the module". They exist to
 * unblock {@code crm} #33, which is the day a lead becomes a child on a register, and nothing
 * else. The status graph, correcting a profile and managing guardian links all wait, because #33
 * needs none of them.
 *
 * <p><b>The one hard thing in here is guardian matching</b>, and it is hard because of a fact
 * about the database rather than a decision: a guardian's phone number is unique per school, and
 * two siblings share a father. #1 therefore has to find the person who is already here and link
 * them. Getting that wrong is not untidy — the second child in a family fails to be admitted at
 * all, with a duplicate key error that surfaces as a 500.
 *
 * <p><b>A child is created without a class, and that is the recorded flow</b>, not a shortcut:
 * inquiry → admission → student → academic record. A school knows it has admitted a child in
 * January and does not know which section they are in until June. Placing them is #14.
 *
 * <p><b>No gate 4 anywhere in this module.</b> A school admits a child for a year that has not
 * started, so requiring the year to be the running one would refuse the entire admissions
 * handover. Reads run no gates at all: a suspended school still reads its own students, because
 * they are still its students.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    /**
     * The most children #6 will answer with.
     *
     * <p>A cap and not a page, for the same reason {@code crm} #15 is capped: the answer to "is
     * this child already here" is one child, or two for a name a family shares, or none.
     * <b>Thirty means the question was wrong</b> — somebody searched for "a" — and that is worth
     * seeing in one screen rather than paging through.
     */
    private static final int MOST_MATCHES = 25;

    /**
     * Ten digits is a whole Indian mobile number.
     *
     * <p>A query at least this long is compared on its <b>last ten</b>, so a country code or a
     * trunk 0 on either side stops mattering. Shorter than this has to match the whole number:
     * comparing "543210" by its tail matches every number ending in those six digits, and a false
     * "we already have this child" is the worst answer #6 can give — the school merges two
     * children, or skips admitting one who was never here.
     */
    private static final int FULL_PHONE_DIGITS = 10;

    /**
     * The most contacts one child can have.
     *
     * <p>The same cap #1 puts on the list it accepts, enforced by #11 too — because #11 is how
     * that list grows after the child is admitted, and a cap only one of the two doors respects is
     * not a cap.
     */
    private static final int MOST_GUARDIANS = 10;

    /**
     * What #4 may be ordered by, and nothing else.
     *
     * <p><b>An allowlist is a security control, not a convenience.</b> An open sort field lets a
     * caller order the roll by anything the document holds — a date of birth, an address — and
     * read the values back out of the ordering without the endpoint ever returning them.
     *
     * <p>Keyed lower case, because a caller typing {@code ?sort=fullname} means the same thing as
     * {@code ?sort=fullName}.
     */
    private static final Map<String, String> SORTABLE_STUDENT_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_STUDENT_FIELDS.put("fullname", "fullName");
        SORTABLE_STUDENT_FIELDS.put("admissionno", "admissionNo");
        SORTABLE_STUDENT_FIELDS.put("admissiondate", "admissionDate");
        SORTABLE_STUDENT_FIELDS.put("createdat", "createdAt");
    }

    /** The same set written out, so the refusal can list what is allowed. */
    private static final String SORTABLE_STUDENT_FIELD_NAMES =
            SORTABLE_STUDENT_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker under every other one.
     *
     * <p><b>Two keys, because the first is not unique.</b> Two children genuinely share a name —
     * it is an ordinary thing on a school roll — so {@code fullName} alone ties, and a tie with no
     * tiebreaker puts one child on two pages while another appears on none.
     * {@code admissionNo} is unique per school by index and settles it.
     */
    private static final Sort STUDENT_ORDER =
            Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("admissionNo"));

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final AdmissionApplicationRepository admissionApplications;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final StudentServiceUtils utils;
    private final StudentHelper helper;

    /**
     * Endpoint #1 — <b>admit a child</b>.
     *
     * <p><b>The write the rest of the product is waiting for.</b> Attendance, marks, fees and
     * transport are all keyed on a {@code studentDocsId}, and nothing else in this system makes
     * one.
     *
     * <p><b>The admission number is generated, never sent.</b> A caller-supplied one lets two
     * children collide inside a school, and nobody picks their own.
     *
     * <p><b>A guardian whose number is already taken is REFUSED, not quietly linked</b> — see
     * {@link StudentServiceUtils#linkGuardians}. That is the one genuinely difficult thing here,
     * and it changed on 2026-10-07: linking is right for a sibling's father and alarming for
     * everybody else, so the caller says which by sending {@code guardianDocsId} or not.
     *
     * <p><b>No class, no section, no academic year.</b> The child exists; where they sit is #14.
     *
     * <p><b>Both callers behave the same.</b> The controller and {@code crm} #33 both reach this,
     * and a taken guardian number is a refusal on either — #33 takes this very request shape, so
     * whoever is enrolling can see the refusal and link an existing person by id in the same
     * breath. There was a flag for the difference until 2026-10-07; there is no difference left.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4 — a school admits in January for a year starting in June.
     */
     public StudentResponse createStudent(StudentCreateRequest request) {

        //! Step 1 - Get the current school.
        School school = currentSchool.requireUsable();
        log.info("[createStudent] Step 1: Admitting '{}' into school {}", request.fullName(), school.getId());

        //! Step 2 - If an admission application is provided, validate it.
        //! Make sure it belongs to this school and has not already created a student.
        String fromApplicationId = TextHelper.blankToNull(request.admissionApplicationDocsId());

        if (fromApplicationId != null) {

                // TODO: Read and validate the admission application.
                AdmissionApplication form = admissionApplications
                        .findByIdAndSchoolId(fromApplicationId, school.getId())
                        .orElseThrow(() -> ApiException.notFound(
                                "ADMISSION_APPLICATION_NOT_FOUND",
                                "No admission application with id '"
                                        + fromApplicationId + "' found in this school."));

                //! One admission application can create only one student.
                //! Check this before saving to return a clear error.
                // TODO: Check if this application already created a student.
                Student already = students
                        .findBySchoolIdAndAdmissionApplicationDocsId(
                                school.getId(), fromApplicationId)
                        .orElse(null);

                if (already != null) {
                throw ApiException.conflict(
                        "APPLICATION_ALREADY_ENROLLED",
                        "Admission application " + form.getApplicationNo()
                                + " has already created student "
                                + already.getFullName()
                                + " (" + already.getAdmissionNo() + ").");
                }

                log.info("[createStudent] Step 2: Application {} is valid and has no student yet",
                        form.getApplicationNo());
        }

        //! Step 3 - Prepare the student's guardians.
        //! Existing guardians are linked using guardianDocsId.
        //! A guardian phone number already used by someone else is rejected.
        log.info("[createStudent] Step 3: Preparing the child's guardians");

        StudentServiceUtils.PreparedGuardians people = utils.linkGuardians(school, request.guardians(), true);

        //! Step 4 - Generate a unique admission number for the student.
        String admissionNo = numberSequences.next(school.getId(), NumberSequenceType.STUDENT_ADMISSION,"ADM/{YYYY}/{MM}/");

        log.info("[createStudent] Step 4: Generated admission number {}", admissionNo);

        //! Step 5 - Create the student object.
        //! If admissionDate is not provided, use today's date.
        Student child = Student.builder()
                .schoolId(school.getId())
                .admissionNo(admissionNo)
                .admissionApplicationDocsId(fromApplicationId)
                .fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .nationalityCode(request.nationalityCode())
                .preferredLanguage(request.preferredLanguage())
                .phoneNumber(TextHelper.blankToNull(request.phoneNumber()))
                .emailAddress(TextHelper.lowercaseOrNull(request.emailAddress()))
                .guardians(people.links())
                .admissionDate(request.admissionDate() == null
                        ? LocalDate.now()
                        : request.admissionDate())
                .build();

        //! Step 6 - Save the student in the database.
        // TODO: Insert student
        Student saved = students.save(child);
        log.info("[createStudent] Step 6: Saved the student (id={}) with {} guardian(s)", saved.getId(), people.links().size());

        //! Step 7 - Return the created student and guardian information.
        //! The response also includes the next step.
        return StudentResponse.of(
                saved,
                people.answers(),
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
        }

    /**
     * Endpoint #2 — <b>correct a child's details</b>.
     *
     * <p><b>The front desk mishears things.</b> A name spelled as it sounded, a date of birth with
     * the year transposed, a number taken down wrong — none of those is an event in the child's
     * life, so none of them gets a verb. They are corrections, and a {@code PATCH} is what a
     * correction is.
     *
     * <p><b>{@code ""} clears, absent leaves alone.</b> The project-wide rule, and the reason this
     * is not a {@code PUT}: a form that sent every field would wipe whatever it did not know
     * about.
     *
     * <p><b>No status, no guardians, no admission number.</b> See {@code StudentUpdateRequest} for
     * each. The short version: a status is seven moves with preconditions (#3), guardians are
     * their own documents shared between siblings (#11 to #13), and an admission number is printed
     * on things.
     *
     * <p><b>A blank name is refused rather than obeyed.</b> It is the only field here whose empty
     * string would otherwise mean "remove the thing a child is found by".
     *
     * <p><b>Gates 1 and 2.</b> No gate 4 — correcting a child's name has nothing to do with which
     * year is running.
     */
     public StudentResponse updateStudent( String studentDocsId, StudentUpdateRequest request) {

        //! Step 1 - Get the current school and make sure it can be used.
        School school = currentSchool.requireUsable();

        //! Step 2 - Make sure the request contains at least one field to update.
        //! If nothing is provided, return an error.
        if (request.isEmpty()) {
                throw ApiException.badRequest(
                        "NOTHING_TO_UPDATE",
                        "Send at least one field to update.");
        }

        log.info("[updateStudent] Step 1: Correcting student {} of school {}", studentDocsId, school.getId());

        //! Step 3 - Get the student from the current school.
        //! This also makes sure the student belongs to this school.
        Student child = utils.loadStudent(school, studentDocsId);

        //! Step 4 - Check that the student was not changed by someone else.
        //! The version must match the version sent by the caller.
        if (!request.version().equals(child.getVersion())) {
                throw ApiException.conflict(
                        "CONCURRENT_MODIFICATION",
                        "'" + child.getFullName()
                                + "' was changed by someone else. Please read the student again "
                                + "before updating.");
        }

        //! Step 5 - Update the student's name.
        //! An empty name is not allowed.
        if (request.fullName() != null) {
                String newName = request.fullName().trim();

                if (newName.isEmpty()) {
                throw ApiException.badRequest(
                        "STUDENT_NAME_REQUIRED",
                        "Student name cannot be empty.");
                }

                child.setFullName(newName);
        }

        //! Step 6 - Update date of birth and gender if provided.
        if (request.dateOfBirth() != null) {
                child.setDateOfBirth(request.dateOfBirth());
        }

        if (request.gender() != null) {
                child.setGender(request.gender());
        }

        //! Step 7 - Update nationality and preferred language if provided.
        if (request.nationalityCode() != null) {
                child.setNationalityCode(request.nationalityCode());
        }

        if (request.preferredLanguage() != null) {
                child.setPreferredLanguage(request.preferredLanguage());
        }

        //! Step 8 - Update phone number and email if provided.
        //! An empty phone or email clears the existing value.
        if (request.phoneNumber() != null) {
                child.setPhoneNumber(
                        TextHelper.blankToNull(request.phoneNumber()));
        }

        if (request.emailAddress() != null) {
                child.setEmailAddress(
                        TextHelper.lowercaseOrNull(request.emailAddress()));
        }

        //! Step 9 - Save the updated student.
        // TODO: Update student
        Student saved = students.save(child);

        log.info("[updateStudent] Step 2: Saved the correction to '{}' (version {})",
                saved.getFullName(), saved.getVersion());

        //! Step 10 - Get the student's guardians and return the updated student.
        //! Guardian information is not changed by this API.
        List<GuardianResponse> contacts = utils.guardiansOf(school, saved);

        return StudentResponse.of(
                saved,
                contacts,
                "'" + saved.getFullName()
                        + "' is updated successfully. Guardians were not changed. "
                        + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #11 — <b>put a guardian on a child</b>.
     *
     * <p><b>It creates or links, and the caller does not have to know which in advance.</b> The
     * plan made {@code guardianDocsId} required — link only — and that is not how a desk works:
     * somebody adding a father types his name and his number, and whether the school already holds
     * him is the thing they are about to find out.
     *
     * <p>So: send an id and that person is linked; leave it out and a new guardian is written,
     * <b>unless the number is already somebody's</b> — which is the same refusal #1 gives, naming
     * the holder and quoting the id to send back. That round trip is the whole flow: type a
     * number, be told whose it is, decide, link.
     *
     * <p><b>The identity rule is not written twice.</b> This hands the one row to the same
     * {@link StudentServiceUtils#linkGuardians} that #1 uses, with the "exactly one primary" check
     * off — that question is about a child's whole list, and this can only see the row being
     * added.
     *
     * <p><b>{@code primaryContact: true} clears it on the child's other guardians</b>, in the same
     * write, and the answer says who was demoted. Two primaries is not a state worth being able to
     * reach — and refusing instead would make "this is the person to ring now" impossible to say.
     *
     * <p><b>The version is the STUDENT'S.</b> The link lives in the child's document, so that is
     * what this write touches and what a concurrent change would collide with.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4.
     */
     public StudentResponse linkGuardian(String studentDocsId, StudentGuardianLinkRequest request) {

        //! Step 1 - Get the current school.
        School school = currentSchool.requireUsable();

        log.info("[linkGuardian] Step 1: Adding a guardian to student {} of school {}", studentDocsId, school.getId());

        //! Step 2 - Get the student from the current school.
        Student child = utils.loadStudent(school, studentDocsId);

        //! Step 3 - Check that the student was not changed by someone else.
        //! The version must match before adding a guardian.
        if (!request.version().equals(child.getVersion())) {
                throw ApiException.conflict(
                        "CONCURRENT_MODIFICATION",
                        "'" + child.getFullName()
                                + "' was changed by someone else. Please read the student again "
                                + "before adding a guardian.");
        }

        List<GuardianLink> links = child.getGuardians() == null
                ? new ArrayList<>()
                : new ArrayList<>(child.getGuardians());

        //! Step 4 - Check the maximum number of guardians allowed.
        if (links.size() >= MOST_GUARDIANS) {
                throw ApiException.conflict(
                        "TOO_MANY_GUARDIANS",
                        "'" + child.getFullName() + "' already has the maximum number of guardians.");
        }

        //! Step 5 - A guardian ID or a guardian name is required.
        //! The ID is used to link an existing guardian.
        //! The name is used to create a new guardian.
        String namedId = TextHelper.blankToNull(request.guardianDocsId());
        String newName = TextHelper.blankToNull(request.fullName());

        if (namedId == null && newName == null) {
                throw ApiException.badRequest(
                        "GUARDIAN_NAME_REQUIRED",
                        "Send guardianDocsId to link an existing guardian, " + "or fullName to create a new guardian.");
        }

        //! Step 6 - Find the existing guardian or create a new one.
        //! The same guardian logic used during student creation is used here.
        StudentServiceUtils.PreparedGuardians prepared =
                utils.linkGuardians(
                        school,
                        List.of(new StudentCreateRequest.GuardianRequest(
                                namedId,
                                newName == null ? "" : newName,
                                request.relation(),
                                request.phoneNumber(),
                                request.emailAddress(),
                                request.alternatePhoneNumber(),
                                request.address(),
                                request.occupation(),
                                request.preferredLanguage(),
                                Boolean.TRUE.equals(request.primaryContact()),
                                Boolean.TRUE.equals(request.emergencyContact()),
                                Boolean.TRUE.equals(request.pickupAuthorized()),
                                Boolean.TRUE.equals(request.portalAccess()))),
                        false);

        GuardianLink fresh = prepared.links().get(0);

        //! Step 7 - Check that this guardian is not already linked to the student.
        for (GuardianLink existing : links) {
                if (fresh.getGuardianDocsId().equals(existing.getGuardianDocsId())) {
                throw ApiException.conflict(
                        "GUARDIAN_ALREADY_LINKED",
                        "'" + prepared.answers().get(0).fullName()
                                + "' is already a guardian of '"
                                + child.getFullName() + "'.");
                }
        }

        //! Step 8 - If the new guardian is the primary contact,
        //! remove the primary contact flag from the existing primary guardian.
        String demoted = null;

        if (Boolean.TRUE.equals(fresh.getPrimaryContact())) {
                for (GuardianLink existing : links) {
                if (Boolean.TRUE.equals(existing.getPrimaryContact())) {
                        demoted = existing.getGuardianDocsId();
                        existing.setPrimaryContact(false);
                }
                }
        }

        //! Step 9 - Add the new guardian and save the student.
        links.add(fresh);
        child.setGuardians(links);

        // TODO: Update student
        Student saved = students.save(child);

        log.info("[linkGuardian] Step 2: '{}' now has {} guardian(s)", saved.getFullName(), links.size());

        //! Step 10 - Return the updated student with all guardian details.
        List<GuardianResponse> contacts = utils.guardiansOf(school, saved);

        boolean wasExisting = Boolean.TRUE.equals(prepared.answers().get(0).matched());

        return StudentResponse.of(
                saved,
                contacts,
                (wasExisting
                        ? "Linked an existing guardian to '"
                                + saved.getFullName() + "'."
                        : "Created a new guardian and linked them to '"
                                + saved.getFullName() + "'.")
                        + (demoted == null
                                ? ""
                                : " The previous primary guardian is no longer "
                                        + "the primary contact.")
                        + " " + NO_AUTHORIZATION_YET);
        }

    /**
     * Endpoint #4 — <b>the roll</b>.
     *
     * <p><b>A row is thinner than a child.</b> The guardians are left off: a page of fifty
     * children with two contacts each is a hundred people's phone numbers and addresses carried
     * across to draw a list that shows none of them. #5 opens one child and has them all.
     *
     * <p><b>The class filters are not here, and the reason is not this endpoint's fault</b> — a
     * class is on the academic record, and #14 writes one. See {@code StudentSearchRequest}.
     * {@code placed} is what stands in for them, and it is the question actually worth asking at
     * the start of a term.
     *
     * <p><b>The sort allowlist is a security control.</b> See {@code SORTABLE_STUDENT_FIELDS}.
     *
     * <p><b>No gates.</b> A read.
     */
    public PageResponse<StudentRowResponse> listStudents(StudentSearchRequest request) {

        //! step 1 - the paging and the order, checked before anything is read. A cheap check with
        //! no database behind it goes first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_STUDENT_FIELDS, SORTABLE_STUDENT_FIELD_NAMES, STUDENT_ORDER);

        //! step 2 - who is asking. require, not requireUsable: a suspended school still reads its
        //! own roll.
        School school = currentSchool.require();
        log.info("[listStudents] Step 1: Reading the roll of school {}", school.getId());

        //! step 3 - one page, filtered and ordered in the database rather than in Java.
        // TODO: read students
        return PageResponse.from(
                students.search(school.getId(), request, pageable),
                StudentRowResponse::fromStudent);
    }

    /**
     * Endpoint #5 — <b>one child in full</b>.
     *
     * <p><b>The guardians are resolved, which is the whole reason this is not a row.</b> A student
     * document stores ids and flags; a page showing {@code 67aa15d9…} where a mother's name belongs
     * would be no use to anybody. They come back in <b>one</b> read, not one per contact.
     *
     * <p><b>An id from another school is a 404.</b> Not a 403 — that would confirm the child
     * exists, and what is behind it is a date of birth and a family's phone numbers.
     *
     * <p><b>No gates.</b> A read.
     */
    public StudentResponse getOneStudent(String studentDocsId) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[getStudent] Step 1: Reading student {} of school {}",
                studentDocsId, school.getId());

        //! step 2 - the child, scoped by school in the QUERY. An id from another school is a real
        //! id, and reading it would hand over another school's family details.
        Student child = utils.loadStudent(school, studentDocsId);

        //! step 3 - the people behind the links, as one read.
        List<GuardianResponse> contacts = utils.guardiansOf(school, child);
        log.info("[getStudent] Step 2: Found {} guardian(s) for '{}'",
                contacts.size(), child.getFullName());

        //! step 4 - no nextStep: a read changed nothing, so there is nothing to do next.
        return StudentResponse.of(child, contacts, null);
    }

    /**
     * Endpoint #6 — <b>is this child already here?</b>
     *
     * <p><b>The call made before every admission</b>, and the reason #1 does not refuse duplicates
     * itself: refusing there would mean deciding that two children with one surname and one phone
     * number are the same child, <i>which siblings are not</i>. The judgement belongs to the person
     * at the desk, and this is what shows them enough to make it.
     *
     * <p><b>It searches the guardians too, and that is the point.</b> A seven year old has no phone
     * — the number a school holds is their mother's — so a check against the child's own contact
     * details would miss nearly every child it exists to find.
     *
     * <p><b>One of the three is required, and sending more than one matches any of them.</b> A
     * family that gave a number last year and quotes an admission number this year is the same
     * family.
     *
     * <p><b>A list, not a page.</b> See {@code MOST_MATCHES}.
     *
     * <p><b>No gates, and this one least of all.</b> A school that cannot be edited still needs to
     * know whether it already has this child, because the alternative is a desk creating
     * duplicates blind.
     */
    public List<StudentRowResponse> findKnownChild(StudentMatchRequest request) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();

        //! step 2 - what was actually asked. THE DIGITS ONLY for the phone, because that is what
        //! makes "+91 98765 43210" and "9876543210" the same question.
        String digits = helper.digitsOf(request.phone());
        String admissionNo = TextHelper.blankToNull(request.admissionNo());
        String name = TextHelper.blankToNull(request.name());

        //! step 3 - a search for nothing is not a search. It would be the whole roll, which is
        //! #4's job, and a caller who sent a blank phone probably believes they sent a real one.
        if (digits.isEmpty() && admissionNo == null && name == null) {
            throw ApiException.badRequest("NOTHING_TO_SEARCH_FOR",
                    "Send a phone number, an admission number or a name — or several, which "
                            + "matches any of them. A search for none of those would be every "
                            + "child in the school, and #4 is what lists those.");
        }
        log.info("[findKnownChild] Step 1: Looking for a child already in school {}",
                school.getId());

        //! step 4 - A FULL-LENGTH NUMBER IS COMPARED ON ITS LAST TEN DIGITS, so a country code or
        //! a trunk 0 on either side stops mattering. A shorter one has to match the whole number —
        //! see FULL_PHONE_DIGITS for what goes wrong otherwise.
        boolean wholeNumber = digits.length() < FULL_PHONE_DIGITS;
        String needle = wholeNumber ? digits
                : digits.substring(digits.length() - FULL_PHONE_DIGITS);

        //! step 5 - whose number is this? Asked of the guardians FIRST, because the answer is how
        //! a child is actually found. One read, and the ids go into the next one rather than a
        //! query per person.
        Collection<String> guardianDocsIds = List.of();
        if (!needle.isEmpty()) {
            // TODO: read guardians (whose number is this)
            List<Guardian> people = guardians.findByLoosePhone(school.getId(), needle,
                    wholeNumber, true, MOST_MATCHES);
            List<String> ids = new ArrayList<>();
            for (Guardian person : people) {
                ids.add(person.getId());
            }
            guardianDocsIds = ids;
            log.info("[findKnownChild] Step 2: That number belongs to {} guardian(s)", ids.size());
        }

        //! step 6 - the children: their own number, their guardians' children, the admission
        //! number, or the name. One query with all four OR-ed, not four queries.
        // TODO: read students (does this school already have this child)
        List<Student> found = students.findKnownChild(school.getId(),
                needle.isEmpty() ? null : needle, wholeNumber, admissionNo, name,
                guardianDocsIds, MOST_MATCHES);
        log.info("[findKnownChild] Step 3: Found {} child(ren) that might be this one",
                found.size());

        //! step 7 - thin rows, the same ones #4's roll draws.
        return found.stream().map(StudentRowResponse::fromStudent).toList();
    }
}
