package com.orbitastra.backend.controllers.crm;

import java.net.URI;

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
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryCreateRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryFollowUpRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryStatusRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryUpdateRequest;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryDetailResponse;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryResponse;
import com.orbitastra.backend.dto.crm.inquiry.response.InquirySummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The lead, before there is an application. Endpoints #8, #9, #10, #12, #13 and #14 of the plan
 * in this package's README; #11, #15 and #16 are not built.
 *
 * <p><b>Its own controller, because {@code inquiries} is its own collection.</b> Five collections
 * get five controllers — the call this module's plan made after watching {@code people} grow to
 * fifteen endpoints across two documents in one file. This is the fifth and last.
 *
 * <p><b>It was built last of the five, and the order was deliberate.</b> An application does not
 * need an inquiry — {@code inquiryDocsId} is nullable, for the family that walks in with a
 * completed form — so the pipeline was testable end to end without a single lead. Leads were the
 * one block nothing else depended on.
 *
 * <p><b>There is no {@code DELETE}.</b> A lead that came to nothing is {@code LOST}, with a reason
 * — #12's job. Admissions keeps what it heard.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Writes run gates 1 and 2. <b>Gate 4 never runs in this module</b> — see the README. A
     * lead is captured about a year the school is <i>not</i> running, which is the normal case.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #8 — the front desk captures a lead.
     *
     * <p><b>Almost everything is optional, and that is the point.</b> A phone call is "a mother
     * rang about her son for next year" — a name, a year, and nothing else. Demanding a date of
     * birth and a guardian's email would refuse the commonest lead there is.
     *
     * <p><b>Two things are required</b>: the child's name, and the academic year the lead is about.
     * The year must exist but <b>need not be the running one</b> — a lead is about the future.
     *
     * <p><b>It creates {@code NEW} and nothing else.</b> Every other status is somebody having done
     * something.
     *
     * <p><b>It does not check for duplicates.</b> #15 asks "is this family already known", and it
     * is asked <i>before</i> this by the person at the desk. Refusing here would mean guessing that
     * two children sharing a phone number are one enquiry — which a family with two children is
     * not.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND     no year of that name in this school
     * 409 CLASS_NOT_IN_CYCLE_YEAR     a class that is not of that year
     * 404 STAFF_NOT_FOUND             a counsellor who is not this school's staff
     * 400 VALIDATION_FAILED           no name, no year, or a date of birth in the future
     * 409 SCHOOL_NOT_EDITABLE         gate 1
     * 409 SUBSCRIPTION_NOT_USABLE     gate 2
     * </pre>
     */
    @PostMapping
    public ResponseEntity<InquiryResponse> capture(
            @Valid @RequestBody InquiryCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. A lead is about a year the school has not started, which is why.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        InquiryResponse response = inquiryService.createInquiry(request);

        return ResponseEntity
                .created(URI.create("/schools/current/inquiries/" + response.inquiryId()))
                .body(response);
    }

    /**
     * Endpoint #9 — <b>correct what the front desk wrote down</b>.
     *
     * <p><b>There is no status gate, and that is the decision this endpoint turns on.</b> #18
     * refuses anything but a {@code DRAFT} application, because #19 freezes a snapshot of what the
     * family declared and a school that could rewrite it afterwards could not answer what they
     * actually said. <b>Nobody declares a lead.</b> Somebody took a phone call and wrote down what
     * they heard, and the commonest thing that happens to a phone call is mishearing it — so a
     * {@code LOST} lead can still have a misspelt name put right.
     *
     * <p><b>Correcting a lead never touches an application.</b> #17 <i>copies</i> the guardians
     * onto the form when it starts one, so the two have been separate records ever since. A lead
     * at {@code APPLICATION_SUBMITTED} is editable and the form it produced is still frozen.
     *
     * <p><b>A blank string clears an optional field</b> — {@code ""} is how a caller says "they no
     * longer have a class in mind" — <b>and an absent field leaves it alone.</b> A <i>required</i>
     * field refuses a blank instead. {@code dateOfBirth} and {@code gender} cannot be cleared at
     * all: neither is a string, so neither has a blank to send.
     *
     * <p><b>Moving the year re-checks the class</b>, including one the caller never mentioned. A
     * lead's interested class must be a class of the year it is about, and a year that moved and
     * left an unrelated class behind would break that silently.
     *
     * <p><b>What another endpoint owns is not a field here</b>: {@code status} and
     * {@code lostReason} are #12's, and {@code nextFollowUpAt} with {@code followUps} are #10's.
     * Events get verbs in this module; field edits get this.
     *
     * <pre>
     * 404 INQUIRY_NOT_FOUND          no lead of that id in this school
     * 400 NOTHING_TO_UPDATE          a body that asks for nothing
     * 409 CONCURRENT_MODIFICATION    somebody moved it since you read it
     * 400 BLANK_STUDENT_NAME         "" where a name is required
     * 400 BLANK_ACADEMIC_YEAR        "" where a year is required
     * 404 ACADEMIC_YEAR_NOT_FOUND    a year this school does not have
     * 409 CLASS_NOT_IN_CYCLE_YEAR    a class that is not of the lead's year, sent or stored
     * 400 VALIDATION_FAILED          a date of birth in the future, or a field over its length
     * 409 SCHOOL_NOT_EDITABLE        gate 1
     * 409 SUBSCRIPTION_NOT_USABLE    gate 2
     * </pre>
     */
    @PatchMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> correct(@PathVariable String inquiryId,
            @Valid @RequestBody InquiryUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. A lead is about a year the school has not started, which is why.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(inquiryService.updateInquiry(inquiryId, request));
    }

    /**
     * Endpoint #10 — <b>log one interaction</b>.
     *
     * <p><b>This is the endpoint the lead half was waiting for.</b> #13 sorts a worklist by
     * {@code nextFollowUpAt} and #14 renders a timeline, and until this existed every lead in the
     * database had an empty timeline and no chase date — both reads were correct and had nothing
     * to show.
     *
     * <p><b>A {@code $push}, never a re-save</b>, so two counsellors logging at once do not
     * overwrite each other. The same call {@code timetable} #3 makes.
     *
     * <p><b>The chase date is rewritten every time, including to nothing.</b> The field means "the
     * next call is due at" — once this call has been made and no new date promised, there is no
     * next call due, and leaving the old one would keep showing a family as overdue on the day
     * somebody rang them. The entry keeps what was promised, so nothing is lost.
     *
     * <p><b>Moving the status is optional</b>, and most calls move nothing. When one does, it
     * walks the transition table, with three destinations refused on top of it:
     * {@code LOST} needs a reason (#12), and {@code APPLICATION_STARTED} and
     * {@code APPLICATION_SUBMITTED} are facts about an application (#17 and #19).
     *
     * <p><b>The note is the only required field.</b> A timeline entry that says nothing is a row
     * that makes a lead look worked when nobody did anything.
     *
     * <p><b>A finished lead can still be logged against.</b> It cannot be moved anywhere — the
     * table says so — but somebody ringing back a family that gave up is exactly the call worth
     * recording.
     *
     * <pre>
     * 404 INQUIRY_NOT_FOUND               no lead of that id in this school
     * 404 STAFF_NOT_FOUND                 a counsellor who is not this school's staff
     * 409 INQUIRY_STATUS_NOT_BY_HAND      APPLICATION_STARTED or APPLICATION_SUBMITTED
     * 409 LOST_NEEDS_A_REASON             LOST, which is #12's
     * 409 INQUIRY_TRANSITION_NOT_ALLOWED  a move the table does not have
     * 409 CONCURRENT_MODIFICATION         somebody moved it since you read it
     * 400 VALIDATION_FAILED               no note, a blank one, or a field over its length
     * 409 SCHOOL_NOT_EDITABLE             gate 1
     * 409 SUBSCRIPTION_NOT_USABLE         gate 2
     * </pre>
     */
    @PostMapping("/{inquiryId}/follow-ups")
    public ResponseEntity<InquiryDetailResponse> logFollowUp(@PathVariable String inquiryId,
            @Valid @RequestBody InquiryFollowUpRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. A lead is about a year the school has not started, which is why.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        InquiryDetailResponse response = inquiryService.logFollowUp(inquiryId, request);

        return ResponseEntity
                .created(URI.create("/schools/current/inquiries/" + response.inquiryId()))
                .body(response);
    }

    /**
     * Endpoint #12 — <b>move the lead, and say why when it is being given up on</b>.
     *
     * <p><b>#10 can move a lead too, and the split is the point.</b> #10 logs a call that
     * <i>happened to</i> move it; this is the move on its own — a school writing a family off in
     * January because nobody has answered since October, where there was no call and pretending
     * there was one would put a fiction in the timeline.
     *
     * <p><b>This is the only thing that may set {@code LOST}</b>, because it is the only one with
     * somewhere to put the reason. #10 refuses that status and names this endpoint.
     *
     * <p><b>A {@code lostReason} is required on a loss and refused on anything else</b>, rather
     * than quietly dropped — a reason attached to a move that is not a loss is a caller who has
     * misunderstood something.
     *
     * <p><b>Moving to where it already is is refused here and accepted by #10.</b> #10's status is
     * a detail of a call that did happen, so echoing the current one is harmless; this endpoint's
     * whole job is the move, and a request that moves nothing has asked for nothing.
     *
     * <p><b>Every move lands on the timeline</b>, with or without a note, and <b>every move ends
     * the chasing</b> — {@code nextFollowUpAt} is cleared, because nobody owes a call to a family
     * that has gone elsewhere.
     *
     * <p><b>It still cannot set the two the application half owns</b>, and refuses them with the
     * same code #10 does.
     *
     * <pre>
     * 404 INQUIRY_NOT_FOUND               no lead of that id in this school
     * 404 STAFF_NOT_FOUND                 a counsellor who is not this school's staff
     * 409 INQUIRY_STATUS_NOT_BY_HAND      APPLICATION_STARTED or APPLICATION_SUBMITTED
     * 400 LOST_REASON_REQUIRED            LOST with no reason
     * 400 LOST_REASON_NOT_ALLOWED         a reason on a move that is not a loss
     * 409 INQUIRY_TRANSITION_NOT_ALLOWED  a move the table does not have, or one that moves nothing
     * 409 CONCURRENT_MODIFICATION         somebody moved it since you read it
     * 400 VALIDATION_FAILED               no status, or a field over its length
     * 409 SCHOOL_NOT_EDITABLE             gate 1
     * 409 SUBSCRIPTION_NOT_USABLE         gate 2
     * </pre>
     */
    @PostMapping("/{inquiryId}/status")
    public ResponseEntity<InquiryDetailResponse> moveStatus(@PathVariable String inquiryId,
            @Valid @RequestBody InquiryStatusRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — NOT RUN. A lead is about a year the school has not started, which is why.
        School school = currentSchool.requireUsable();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(inquiryService.moveStatus(inquiryId, request));
    }

    /**
     * Endpoint #13 — <b>the counsellor's worklist</b>. Whose, what state, what is overdue.
     *
     * <p><b>Soonest to chase first.</b> Filter by {@code status} and add {@code overdue=true} and
     * you have the calls that are already late.
     *
     * <p><b>It filtered by counsellor until 2026-09-24</b>, and does not any more: a lead is no
     * longer owned by anybody. That field and #11 were removed together.
     *
     * <p><b>{@code overdue} is two conditions, not one</b>: past its follow-up date <b>and</b> not
     * {@code LOST} or {@code CLOSED}. A lead somebody gave up on last month has a past date too,
     * and nobody owes it a phone call. The flag is reported on <i>every</i> row, whether or not
     * the filter asked for it.
     *
     * <p><b>A lead with no follow-up date is never overdue</b>, and sorts to the front by default
     * because Mongo puts a missing field first. On a chase list that is arguably the wrong end —
     * but a lead nobody has promised to ring is also the one most likely to be forgotten.
     *
     * <p><b>A row is thinner than the lead</b>: no notes, no source detail, no timeline. Those are
     * paragraphs a counsellor wrote about one family, and a page of twenty would carry every word
     * of them to draw a list that shows none. <b>The phone number is on it</b> — the point of a
     * worklist is to pick the phone up.
     *
     * <p><b>There is no "mine".</b> Nothing here knows who is calling yet, so the counsellor has to
     * be named in the query.
     *
     * <p><b>No gates.</b> A read — a suspended school still owes these families a call back.
     *
     * <pre>
     * 400 INVALID_PAGE           a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above 100
     * 400 INVALID_SORT_FIELD     a field that is not on the allowlist
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<InquirySummaryResponse>> list(InquirySearchRequest request) {

        //! NO GATES. Reads run none: a school that cannot be edited still has families waiting on
        //! a call back, and hiding the worklist would lose them.
        return ResponseEntity.ok(inquiryService.listInquiries(request));
    }

    /**
     * Endpoint #14 — <b>one lead with its whole timeline</b>.
     *
     * <p><b>Everything a #13 row leaves off</b>: the notes, where the lead came from, and every
     * follow-up in the order it happened with whoever logged it named.
     *
     * <p><b>Oldest first.</b> A timeline that reads backwards is worse than no timeline — the
     * question being asked of it is "what have we already told this family".
     *
     * <p><b>Nothing is resolved by refusing.</b> A class that was removed, or a counsellor who has
     * left, leaves the name off and the lead readable. What was said to that family still happened.
     *
     * <p><b>An id from another school is a 404</b>, not a 403. It is a real id; saying which would
     * confirm another tenant's lead exists.
     *
     * <p><b>No gates.</b> A read.
     *
     * <pre>
     * 404 INQUIRY_NOT_FOUND      no lead of that id in this school
     * 400 TENANT_NOT_RESOLVED    no idtoken cookie
     * </pre>
     */
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDetailResponse> getOne(@PathVariable String inquiryId) {

        //! NO GATES. A read.
        return ResponseEntity.ok(inquiryService.getInquiry(inquiryId));
    }
}
