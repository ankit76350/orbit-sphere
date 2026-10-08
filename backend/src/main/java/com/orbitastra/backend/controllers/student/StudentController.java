package com.orbitastra.backend.controllers.student;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.student.student.request.StudentCreateRequest;
import com.orbitastra.backend.dto.student.student.request.StudentGuardianLinkRequest;
import com.orbitastra.backend.dto.student.student.request.StudentMatchRequest;
import com.orbitastra.backend.dto.student.student.request.StudentSearchRequest;
import com.orbitastra.backend.dto.student.student.request.StudentUpdateRequest;
import com.orbitastra.backend.dto.student.student.response.StudentResponse;
import com.orbitastra.backend.dto.student.student.response.StudentRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.student.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The children a school has accepted. Endpoints #1, #2, #4, #5, #6 and #11 of the plan in this
 * package's README; the rest are not built.
 *
 * <p><b>Phase 5 was four endpoints</b> — what {@code controllers/README.md} calls "the minimum,
 * not the module". They existed to unblock {@code crm} #33, the handover where an applicant
 * becomes a child on a register, and nothing else. <b>#2 is the first thing added beyond it</b>,
 * on 2026-10-07: a front desk mishears a name, and a roll nobody can correct is a roll that gets
 * worse every week. The status graph (#3), the guardian endpoints (#7 to #13) and the whole
 * academic record (#14 onwards) still wait.
 *
 * <p><b>School surface only.</b> The school comes from the {@code idtoken} cookie and never from
 * the URL. There is no platform route into a school's students and there will not be one: a
 * {@code Student} carries a child's date of birth and their guardians' phone numbers, and an
 * operator route into that would be a standing invitation to read one school's children while
 * holding another's session.
 *
 * <p><b>Two gates on a write, none on a read.</b> Gate 4 asks whether a named academic year is the
 * school's working one, and nothing here carries a year to ask it about — a school admits a child
 * in January for a year that starts in June, so requiring the running year would refuse the whole
 * handover. A read runs no gate at all: a suspended school still reads its own students, because
 * they are still its students.
 *
 * <p><b>There is no {@code DELETE}.</b> A child who leaves is moved through the status graph with
 * a reason — #3, which is not built — and a record typed in by mistake is a correction, not a
 * removal. A school's register is not something you delete rows out of.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/students")
public class StudentController {

    private final StudentService studentService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — <b>admit a child</b>.
     *
     * <p><b>The write every other module is waiting for.</b> Attendance, marks, fees and transport
     * are all keyed on a {@code studentDocsId}, and nothing else in this product makes one.
     *
     * <p><b>{@code admissionNo} is generated, never sent.</b> Nobody picks their own admission
     * number, and a caller-supplied one lets two children collide inside a school.
     *
     * <p><b>The guardians are matched, not blindly created.</b> A phone number identifies one
     * person per school and two siblings share a father, so a contact already on the books is
     * linked rather than written again — and the answer says, per guardian, which happened.
     *
     * <p><b>No class and no academic year.</b> The child exists; placing them is #14, separately
     * and often months later.
     *
     * <p><b>{@code admissionApplicationDocsId} is checked when it is sent.</b> It is the one field
     * here naming another module's document, and an id that is not a real form in this school is a
     * 404 rather than a link written to nothing.
     *
     * <pre>
     * 400 VALIDATION_FAILED            a field missing or over its length
     * 400 PRIMARY_CONTACT_REQUIRED     none of the guardians is the primary contact, or two are
     * 400 DUPLICATE_GUARDIAN_IN_REQUEST  two guardians on one form share a phone or an email
     * 404 ADMISSION_APPLICATION_NOT_FOUND  admissionApplicationDocsId names no form in this school
     * 409 APPLICATION_ALREADY_ENROLLED  that admission application already became a child
     * 409 SCHOOL_NOT_EDITABLE          the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED          no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<StudentResponse> admit(
            @Valid @RequestBody StudentCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: a school admits a child for a year it has not started.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        StudentResponse response = studentService.createStudent(request);

        return ResponseEntity
                .created(URI.create("/schools/current/students/" + response.studentDocsId()))
                .body(response);
    }

        /**
     * Endpoint #4 — <b>the roll</b>.
     *
     * <p><b>Filtered by status, gender, whether they have been placed and whether they came from
     * admissions.</b> The class filters the plan asked for are not here: a class lives on the
     * academic record, which #14 writes and which does not exist yet. Taking them now would mean
     * a filter that silently matched nothing.
     *
     * <p><b>A row carries no guardians</b> — fifty children with two contacts each is a hundred
     * families' phone numbers crossing the wire to draw a list that shows none of them. #5 opens
     * one child and has them all.
     *
     * <p><b>Sortable by {@code fullName}, {@code admissionNo}, {@code admissionDate} and
     * {@code createdAt}, and nothing else.</b> The allowlist is a security control: an open sort
     * field lets a caller order by a date of birth and read it back out of the ordering.
     *
     * <pre>
     * 400 INVALID_SORT_FIELD   a sort field outside the allowlist
     * 400 INVALID_PAGE_SIZE    a page bigger than the cap
     * 400 TENANT_NOT_RESOLVED  no idtoken cookie
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<StudentRowResponse>> list(StudentSearchRequest request) {

        //! NO GATES. A read — a suspended school still reads its own roll.
        return ResponseEntity.ok(studentService.listStudents(request));
    }

    /**
     * Endpoint #5 — <b>one child in full</b>.
     *
     * <p><b>With the guardians resolved into people</b>, in one read rather than one per contact.
     * That is the whole difference from a row: the student document stores ids and flags, and a
     * page that showed an id where a mother's name belongs would be no use to anybody.
     *
     * <p><b>An id from another school is a 404, not a 403.</b> A 403 would confirm the child
     * exists, and behind it are a date of birth and a family's phone numbers.
     *
     * <pre>
     * 404 STUDENT_NOT_FOUND    no child of that id in this school
     * 400 TENANT_NOT_RESOLVED  no idtoken cookie
     * </pre>
     */
    @GetMapping("/{studentDocsId}")
    public ResponseEntity<StudentResponse> getOne(@PathVariable String studentDocsId) {

        //! NO GATES. A read.
        return ResponseEntity.ok(studentService.getOneStudent(studentDocsId));
    }

    /**
     * Endpoint #2 — <b>correct a child's details</b>.
     *
     * <p><b>The front desk mishears things.</b> A name spelled as it sounded, a date of birth with
     * the year transposed, a number taken down wrong. None of those is an event in the child's
     * life, so none of them gets a verb — they are corrections, and a {@code PATCH} is what a
     * correction is.
     *
     * <p><b>Not the photo.</b> {@code profilePhotoDocumentId} names a {@code DocumentRecord}, and
     * a file is uploaded rather than typed — it came off this endpoint on 2026-10-07 and belongs
     * with the documents module.
     *
     * <p><b>{@code ""} clears a field, absent leaves it alone.</b> That is the whole reason this
     * is not a {@code PUT}: a form that sent every field would wipe whatever it did not know
     * about.
     *
     * <p><b>Never the admission number, the status or the guardians.</b> The number is printed on
     * certificates; the status is seven moves with preconditions, which is #3; the guardians are
     * their own documents shared between siblings, which is #11 to #13. None of the three is
     * built, and a {@code PATCH} that quietly did any of them would be the wrong endpoint doing
     * it.
     *
     * <p><b>A blank name is refused rather than obeyed</b> — it is the one field whose empty
     * string would mean "remove the thing this child is found by".
     *
     * <p><b>{@code version} is required</b>, so {@code 409 CONCURRENT_MODIFICATION} is reachable:
     * two clerks correcting one child's record is exactly what it exists for.
     *
     * <pre>
     * 404 STUDENT_NOT_FOUND        no child of that id in this school
     * 400 NOTHING_TO_UPDATE        the body moves nothing
     * 400 STUDENT_NAME_REQUIRED    fullName sent as "" or spaces
     * 400 VALIDATION_FAILED        a field over its length, a future date, no version
     * 409 CONCURRENT_MODIFICATION  somebody wrote to this child first
     * 409 SCHOOL_NOT_EDITABLE      the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED      no idtoken cookie
     * </pre>
     */
    @PatchMapping("/{studentDocsId}")
    public ResponseEntity<StudentResponse> correct(@PathVariable String studentDocsId,
            @Valid @RequestBody StudentUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: correcting a name has nothing to do with which year is running.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(studentService.updateStudent(studentDocsId, request));
    }

    /**
     * Endpoint #6 — <b>is this child already here?</b>
     *
     * <p><b>Asked before every admission</b>, which is why #1 does not refuse duplicates itself:
     * refusing there would mean deciding that two children sharing a surname and a phone number
     * are one child, <i>which siblings are not</i>. The judgement belongs to the person at the
     * desk; this shows them what they need to make it.
     *
     * <p><b>It searches the guardians as well as the child.</b> A seven year old has no phone —
     * the number the school holds is their mother's — so a check against the child's own details
     * would miss nearly every child it exists for.
     *
     * <p><b>One of phone, admission number or name is required; several match any of them.</b>
     *
     * <p><b>It is declared above {@code /{studentDocsId}} on purpose.</b> {@code /search} is a
     * literal path and a child's id is a variable one; Spring prefers the literal either way, and
     * the order is here so a reader does not have to know that.
     *
     * <pre>
     * 400 NOTHING_TO_SEARCH_FOR  none of the three was sent
     * 400 VALIDATION_FAILED      a field over its length
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping("/search")
    public ResponseEntity<List<StudentRowResponse>> findKnownChild(
            @Valid StudentMatchRequest request) {

        //! NO GATES. Reads run none — and this one least of all: a school that cannot be edited
        //! still needs to know whether it already has this child, or the desk duplicates them.
        return ResponseEntity.ok(studentService.findKnownChild(request));
    }


    /**
     * Endpoint #11 - Add a guardian to a student.
     *
     * <p>The guardian can be an existing person or a new person.
     *
     * <p>If guardianDocsId is provided, the existing guardian is linked.
     * If it is not provided, a new guardian is created.
     *
     * <p>If the phone or email already belongs to another guardian, the request is rejected.
     *
     * <p>The relation and contact settings belong to this student.
     * They are not changed on the guardian's main record.
     *
     * <p>If primaryContact is true, any existing primary guardian for this student
     * is changed to non-primary.
     *
     * <p>The version belongs to the student because the guardian link is stored
     * inside the student's document.
     *
     * <pre>
     * 404 STUDENT_NOT_FOUND
     *     Student does not exist in this school.
     *
     * 404 GUARDIAN_NOT_FOUND
     *     The given guardianDocsId does not exist in this school.
     *
     * 400 VALIDATION_FAILED
     *     Required data is missing or a field is too long.
     *
     * 409 GUARDIAN_PHONE_TAKEN
     *     The phone number is already used by another guardian.
     *
     * 409 GUARDIAN_EMAIL_TAKEN
     *     The email is already used by another guardian.
     *
     * 409 GUARDIAN_ALREADY_LINKED
     *     This guardian is already linked to the student.
     *
     * 409 TOO_MANY_GUARDIANS
     *     The student already has the maximum number of guardians.
     *
     * 409 CONCURRENT_MODIFICATION
     *     The student was changed by someone else before this request.
     *
     * 409 SCHOOL_NOT_EDITABLE
     *     The school is suspended or closed.
     *
     * 400 TENANT_NOT_RESOLVED
     *     The school could not be identified from the request.
     * </pre>
     */
    @PostMapping("/{studentDocsId}/guardians")
    public ResponseEntity<StudentResponse> addGuardian(@PathVariable String studentDocsId,
            @Valid @RequestBody StudentGuardianLinkRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: a contact has nothing to do with which year is running.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(studentService.linkGuardian(studentDocsId, request));
    }
}
