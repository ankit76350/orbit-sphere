package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationUpdateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationWithdrawRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationDecisionRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationSearchRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationDetailResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationEnrollResponse;
import com.orbitastra.backend.dto.student.student.request.StudentCreateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.AdmissionApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The forms families fill in. Endpoints #17 to #25 and #33 of the plan in this package's README;
 * #17, #19, #20, #24 and #25 are built.
 *
 * <p><b>Its own controller, not part of the cycle's.</b> Five collections get five controllers —
 * the call this module's plan made after watching {@code people} grow to fifteen endpoints across
 * two documents in one file.
 *
 * <p><b>An application is addressed by its own id</b>, not nested under the cycle. An admission
 * officer opens one from a worklist or a search far more often than by walking down from a cycle,
 * and a nested address would make the common call carry an id nobody had.
 *
 * <p><b>There is no {@code DELETE}.</b> An application the family pulled out of is {@code WITHDRAWN}
 * — #21. Admissions is the record of what a school decided about a child, and the decision not to
 * go ahead is part of it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/applications")
public class AdmissionApplicationController {

    private final AdmissionApplicationService admissionApplicationService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Writes run gates 1 and 2. <b>Gate 4 never runs in this module</b> — a cycle for a year
     * that has not started is the normal case, so asking "is this the running year" would refuse
     * the work the module exists to do. What takes its place is the cycle's own status: an
     * application can only go into an {@code OPEN} cycle.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #17 — starts an application against an open cycle.
     *
     * <p><b>The inquiry is optional</b>, and that is the point: the family that walks in with a
     * completed form never enquired. It is also why the whole pipeline is testable without a
     * single lead in the database.
     *
     * <p><b>The class must be in the cycle's seat table</b>, not just in its year. A class with no
     * seats is a class nothing could ever be offered in.
     *
     * <p>Creates in {@code DRAFT}. Submitting is #19.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND   no cycle with that id in this school
     * 409 CYCLE_NOT_OPEN              the cycle is not taking applications
     * 404 INQUIRY_NOT_FOUND           an inquiry that is not this school's
     * 409 APPLICATION_ALREADY_EXISTS  that inquiry already applied to that cycle
     * 409 CLASS_NOT_IN_CYCLE_YEAR     the class is not of the cycle's year
     * 409 CLASS_NOT_IN_CAPACITY       the cycle's seat table does not list that class
     * 400 TOO_MANY_FORM_ANSWERS       more answers than the cap
     * 400 VALIDATION_FAILED           a missing cycle, class, name, date of birth, gender or guardian
     * </pre>
     */
    @PostMapping
    public ResponseEntity<AdmissionApplicationResponse> create(
            @Valid @RequestBody AdmissionApplicationCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The cycle's own status is what decides, and the service asks it.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        AdmissionApplicationResponse response =
                admissionApplicationService.createApplication(request);

        return ResponseEntity
                .created(URI.create("/schools/current/applications/"
                        + response.admissionApplicationId()))
                .body(response);
    }

    /**
     * Endpoint #18 — correcting a form the family has not sent yet.
     *
     * <p><b>Families fill a form over several sittings.</b> Before this, #17 created one and
     * nothing could change it — a typo in a child's name meant starting again.
     *
     * <p><b>{@code DRAFT} and nothing else</b>, which is the line this module is built around.
     * After #19 the applicant and guardian fields stop being a draft and become a record of what
     * the family declared; a school that could rewrite them afterwards could not answer "what did
     * they tell us".
     *
     * <p><b>A {@code PATCH}, so only what you send moves</b> — and lists and maps are
     * <b>replaced</b>, not merged. A guardian has no id to merge by, and merging answers would
     * leave no way to remove one typed by mistake.
     *
     * <p><b>The cycle cannot be changed, and neither can the inquiry or the status.</b> The round
     * decides the year, the seat table and the window #19 checks, so moving a form elsewhere is a
     * different application. Re-pointing the inquiry would leave the old lead claiming a form it no
     * longer has. And {@code DRAFT → SUBMITTED} is #19, which freezes the snapshot as it goes.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND        no application with that id in this school
     * 400 NOTHING_TO_UPDATE            a body that changes nothing
     * 409 APPLICATION_NOT_EDITABLE     anything past DRAFT — the snapshot is frozen
     * 409 CLASS_NOT_IN_CYCLE_YEAR      a class the round does not admit into
     * 409 CLASS_NOT_IN_CAPACITY        a class the round has no seats for
     * 400 BLANK_APPLICANT_NAME         a name sent as empty rather than left out
     * 400 TOO_MANY_FORM_ANSWERS        more than 200 answers
     * 409 CONCURRENT_MODIFICATION      somebody moved it while you were reading
     * 400 VALIDATION_FAILED            no guardians in a guardian list, or a date in the future
     * 409 SCHOOL_NOT_EDITABLE          gate 1
     * 409 SUBSCRIPTION_NOT_USABLE      gate 2
     * </pre>
     */
    @PatchMapping("/{admissionApplicationId}")
    public ResponseEntity<AdmissionApplicationResponse> correct(
            @PathVariable String admissionApplicationId,
            @Valid @RequestBody AdmissionApplicationUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The application's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionApplicationService.updateApplication(admissionApplicationId, request));
    }

    /**
     * Endpoint #19 — the family submits the form. <b>The snapshot freezes here.</b>
     *
     * <p><b>No body.</b> Everything it needs is already on the form; this endpoint is an event,
     * not a field edit. That is why it is a {@code POST} to its own address rather than a
     * {@code PATCH} that sets {@code status} — the move has its own preconditions and its own side
     * effects, and a single "set the status" endpoint would be nine endpoints wearing one name.
     *
     * <p><b>Only a DRAFT can be submitted</b>, and that is checked before the cycle is: somebody
     * pressing submit twice should be told the form is already in, not that the round has since
     * closed.
     *
     * <p><b>The cycle is asked the same two questions #17 asks it.</b> The status says whether
     * anybody opened the round; the published dates say what the school promised families. A form
     * started before the deadline and submitted after it is a late application.
     *
     * <p><b>It does not re-check the seat table.</b> Submitting is the family's act, and the
     * school emptying its own seat table afterwards must not refuse it. Capacity is decided when a
     * seat is offered.
     *
     * <p><b>A named inquiry moves to {@code APPLICATION_SUBMITTED}</b>, and is skipped when the
     * lead is gone — a deleted note about how the form arrived cannot be allowed to block the form.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND           no application with that id in this school
     * 409 INVALID_APPLICATION_TRANSITION  anything that is not a DRAFT, re-submitting included
     * 404 ADMISSION_CYCLE_NOT_FOUND       the round it names is gone
     * 409 CYCLE_NOT_OPEN                  the round is not taking applications
     * 409 APPLICATIONS_NOT_OPEN_YET       the round is OPEN, but its published start has not come
     * 409 APPLICATIONS_CLOSED             its published close has passed and nobody closed it
     * 409 SCHOOL_NOT_EDITABLE             gate 1
     * 409 SUBSCRIPTION_NOT_USABLE         gate 2
     * </pre>
     */
    @PostMapping("/{admissionApplicationId}/submit")
    public ResponseEntity<AdmissionApplicationResponse> submit(
            @PathVariable String admissionApplicationId) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The cycle's own status and window decide, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionApplicationService.submitApplication(admissionApplicationId));
    }

    /**
     * Endpoint #21 — the family pulls out.
     *
     * <p><b>The family's act, not the school's.</b> #20 records what the school decided; this
     * records that the family stopped. A school that refused a child and a family that went
     * elsewhere are very different numbers at the end of a season, and one endpoint for both would
     * lose which happened.
     *
     * <p><b>From anywhere before {@code ENROLLED}</b> — a draft nobody sent, a form under review,
     * an approved applicant, one holding an offer. The status graph has said so since before any
     * of this was built.
     *
     * <p><b>A reason is required</b>, and it goes in {@code withdrawalReason} — <i>not</i>
     * {@code decisionNote}, which is the school's own word about what it decided. Two facts, two
     * fields.
     *
     * <p><b>It stamps {@code withdrawnAt} and not {@code decidedAt}</b>, because the school did not
     * decide anything.
     *
     * <p><b>It touches nothing but the application.</b> A form withdrawn while it holds a live
     * offer leaves that offer {@code ISSUED} — so #32's chase list will still show it. The
     * endpoint that ends the offer is #30 with {@code DECLINED}, or #31; doing both is two calls,
     * and each records a different fact.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND           no application with that id in this school
     * 409 INVALID_APPLICATION_TRANSITION  ENROLLED, or already WITHDRAWN
     * 409 CONCURRENT_MODIFICATION         somebody moved it while you were reading
     * 400 VALIDATION_FAILED               no reason, or a blank one
     * 409 SCHOOL_NOT_EDITABLE             gate 1
     * 409 SUBSCRIPTION_NOT_USABLE         gate 2
     * </pre>
     */
    @PostMapping("/{admissionApplicationId}/withdraw")
    public ResponseEntity<AdmissionApplicationResponse> withdraw(
            @PathVariable String admissionApplicationId,
            @Valid @RequestBody AdmissionApplicationWithdrawRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The application's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionApplicationService.withdrawApplication(admissionApplicationId, request));
    }

    /**
     * Endpoint #20 — what the school decided.
     *
     * <p><b>It names the status the application moves to</b>, exactly as #3 does for a cycle —
     * {@code APPROVED}, {@code REJECTED}, {@code WAITLISTED},
     * {@code ADDITIONAL_INFORMATION_REQUIRED}, or back to {@code UNDER_REVIEW}. One set of words on
     * the way in and the same set on the way out.
     *
     * <p><b>The transition table is what refuses {@code ENROLLED}</b>, not the shape of the
     * vocabulary. {@code OFFERED}, {@code OFFER_ACCEPTED} and {@code ENROLLED} are #29's, #30's and
     * #33's consequences; {@code WITHDRAWN} is #21's; {@code DRAFT} and {@code SUBMITTED} are the
     * family's side. Asking for any of them here is a refusal.
     *
     * <p><b>It does not need a review to exist.</b> Small schools decide in a conversation, so a
     * {@code SUBMITTED} form can be decided without ever having been assigned to anybody — which
     * is why this is not gated on {@code UNDER_REVIEW}.
     *
     * <p><b>But it will not APPROVE one while somebody is still assessing it.</b> If any review of
     * the form is {@code PENDING} or {@code IN_PROGRESS}, {@code APPROVED} is
     * {@code 409 REVIEWS_STILL_OUTSTANDING} and the refusal names the rounds. A {@code CANCELLED}
     * review does not hold it up — work the school called off is a settled answer — and a form
     * with no reviews at all is unaffected, which is what keeps the paragraph above true.
     *
     * <p><b>Only {@code APPROVED}.</b> Refusing, waitlisting and asking for more are all answers a
     * head can give over an incomplete picture — asking for more is often exactly <i>why</i> a
     * review is still open. Admitting a child is the one decision that claims every assessment was
     * seen.
     *
     * <p><b>A refusal and a request for more both have to say why</b>, and the reason is kept on
     * the application rather than logged and dropped.
     *
     * <p><b>{@code APPROVED} cannot be decided again.</b> The next thing that happens to an
     * approved applicant is an offer (#29); changing your mind is withdrawing it (#31).
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND           no application with that id in this school
     * 409 INVALID_APPLICATION_TRANSITION  not a move that form can make from where it is
     * 409 REVIEWS_STILL_OUTSTANDING       APPROVED while a review is PENDING or IN_PROGRESS
     * 409 CONCURRENT_MODIFICATION         somebody decided it while you were reading
     * 400 VALIDATION_FAILED               no status, no note, or one that is not on the enum
     * 409 SCHOOL_NOT_EDITABLE             gate 1
     * 409 SUBSCRIPTION_NOT_USABLE         gate 2
     * </pre>
     */
    @PostMapping("/{admissionApplicationId}/decision")
    public ResponseEntity<AdmissionApplicationResponse> decide(
            @PathVariable String admissionApplicationId,
            @Valid @RequestBody AdmissionApplicationDecisionRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. The application's own status is what decides, and the service asks.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionApplicationService.decide(admissionApplicationId, request));
    }

    /**
     * Endpoint #24 — one page of this school's applications. <b>The pipeline.</b>
     *
     * <p>Filter by {@code admissionCycleDocsId}, {@code appliedClassDocsId}, {@code status} and
     * {@code assignedAdmissionOfficerDocsId}; search the applicant's name or the application
     * number; split walk-ins from leads with {@code fromInquiry}. Every one is optional.
     *
     * <p>The first four are the order {@code school_cycle_class_status_idx} is built in, which is
     * not a coincidence — it is the worklist an admission officer opens.
     *
     * <p><b>A row is thinner than what #17 returns</b>: no guardians, no form answers, no evidence
     * ids. All three are on #25.
     *
     * <p><b>No gates.</b> This is a read — a suspended school still sees who applied to it.
     *
     * <pre>
     * 400 INVALID_PAGE           a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above 100
     * 400 INVALID_SORT_FIELD     a field that is not on the allowlist
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdmissionApplicationSummaryResponse>> list(
            AdmissionApplicationSearchRequest request) {

        //! NO GATES. Reads run none: gate 1 would stop a suspended school reading applications it
        //! already took, and gate 2 would make a lapsed subscription hide its own pipeline.
        return ResponseEntity.ok(admissionApplicationService.listApplications(request));
    }

    /**
     * Endpoint #25 — one application in full.
     *
     * <p>Everything a #24 row leaves off: the guardians, the form answers, the evidence ids, the
     * withdrawal and the resulting student — <b>plus its reviews and its offers</b>, which are not
     * fields on the application but rows in two other collections.
     *
     * <p><b>Those two arrays are empty today.</b> #26 and #27 create reviews, #29 to #31 create
     * offers, and none are built. The reads are real and scoped; there is simply nothing yet.
     *
     * <p><b>The class and the cycle come back named.</b> A name that could not be found is left
     * off rather than guessed — an application pointing at a round or a class the school no longer
     * has is a real problem, and inventing a label would hide it.
     *
     * <p><b>No gates.</b> A read. A suspended school still opens the forms families sent it.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND   no application with that id IN THIS SCHOOL
     * 400 TENANT_NOT_RESOLVED     no idtoken cookie
     * </pre>
     */
    @GetMapping("/{admissionApplicationId}")
    public ResponseEntity<AdmissionApplicationDetailResponse> get(
            @PathVariable String admissionApplicationId) {

        //! NO GATES, same as #24. The school is scoped inside the query rather than checked after
        //! the read, so another school's real id answers 404 instead of handing the form over.
        return ResponseEntity.ok(
                admissionApplicationService.getApplication(admissionApplicationId));
    }

    /**
     * Endpoint #33 — <b>the applicant becomes a student</b>.
     *
     * <p><b>The point of this module and of {@code student} together.</b> Everything before it is
     * a school making up its mind; this is where a family stops being an enquiry and a child goes
     * onto a register that attendance, marks, fees and transport all read.
     *
     * <p><b>It takes the same body {@code student} #1 takes.</b> Enrolling is admitting a child
     * who happens to have applied, so it asks for a child rather than a second shape meaning the
     * same thing — the caller fills it from the form and corrects whatever is wrong on the way
     * past.
     *
     * <p><b>That is the only chance to correct anything.</b> #18 edits an application only while
     * it is a {@code DRAFT} — the snapshot freezes at #19 — so by the time a family holds an
     * accepted offer, a typo on the form cannot be fixed any other way.
     *
     * <p><b>{@code admissionApplicationDocsId} comes from the path</b>, whatever the body says.
     * The URL names the form being enrolled, and two sources for one fact is one too many.
     *
     * <p><b>It does not write back to the application.</b> The form keeps saying what the family
     * declared; the student says what the school admitted. After a corrected enrolment #25 and the
     * child disagree, on purpose — the frozen snapshot is the record of what was sent.
     *
     * <p><b>A guardian whose number is already taken is refused here too</b>, exactly as on
     * {@code student} #1 — the caller links an existing person by sending their
     * {@code guardianDocsId}. One rule on both doors.
     *
     * <p><b>It writes four documents in one transaction</b> — a student (and their guardians), the
     * application, and the lead the family started as. A child created with the application left
     * unlinked would be a child nobody can find their way back to.
     *
     * <p><b>The child is made by {@code student} #1, not here.</b> That is where guardian matching
     * lives, and matching is the difficult part: a sibling already at the school shares a father,
     * and writing him down twice fails on a unique index.
     *
     * <p><b>Seats are enforced here and nowhere else.</b> #29 lets a school over-offer on purpose
     * — sixty letters for forty places — so the cap bites at the last possible moment, which is
     * this one.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4: a school enrols in January for a year starting in June.
     *
     * <pre>
     * 404 APPLICATION_NOT_FOUND          no form of that id in this school
     * 409 ALREADY_ENROLLED               that form already became a child
     * 409 INVALID_APPLICATION_TRANSITION the form is not OFFER_ACCEPTED
     * 409 OFFER_NOT_ACCEPTED              no offer on it was accepted
     * 409 SEATS_EXHAUSTED                the class's open seats are all taken
     * 400 PRIMARY_CONTACT_REQUIRED       cannot happen — #33 always marks one. See the service.
     * 409 SCHOOL_NOT_EDITABLE            the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED            no idtoken cookie
     * </pre>
     */
    @PostMapping("/{admissionApplicationId}/enroll")
    public ResponseEntity<AdmissionApplicationEnrollResponse> enroll(
            @PathVariable String admissionApplicationId,
            //! THE SAME BODY student #1 TAKES. Enrolling is admitting a child who happens to have
            //! applied, so it asks for a child — not a second shape that means the same thing.
            //! The caller fills it from the form and corrects whatever is wrong on the way past.
            @Valid @RequestBody StudentCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. A child is enrolled for a year the school has not started yet.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(
                admissionApplicationService.enrollApplicant(admissionApplicationId, request));
    }
}
