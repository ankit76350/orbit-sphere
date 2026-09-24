package com.orbitastra.backend.controllers.crm;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryCreateRequest;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.crm.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The lead, before there is an application. Endpoint #8 of the plan in this package's README; #9 to
 * #16 are not built.
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
}
