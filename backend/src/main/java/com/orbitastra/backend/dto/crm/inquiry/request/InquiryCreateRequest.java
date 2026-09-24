package com.orbitastra.backend.dto.crm.inquiry.request;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.GuardianRelation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * What #8 carries — a lead, captured at the front desk.
 *
 * <p><b>Almost everything is optional, and that is the endpoint's whole character.</b> A phone
 * call is "a mother rang about her son for next year" — a name, a year, and nothing else. An
 * endpoint that demanded a date of birth and a guardian's email would be refusing the commonest
 * lead there is, and the front desk would stop using it.
 *
 * <p><b>Two things ARE required.</b> The child's name, because a lead about nobody is not a lead;
 * and the academic year, because a lead is always <i>for</i> an intake — "next year" is the first
 * thing anybody says — and without it the worklist cannot be split by the round it belongs to.
 *
 * <p><b>Guardians are optional too, and that is deliberate rather than lax.</b> The plan says at
 * least one is "strongly wanted but not required": a walk-in who gives a child's name and leaves
 * is a real lead, and the school would rather have the row than nothing. #15 is what finds a
 * family again by phone or email, and it can only find the ones who left one.
 */
public record InquiryCreateRequest(

        @NotBlank @Size(max = 160) String prospectiveStudentName,

        /** Must exist in this school. **Need not be the running one** — a lead is about the future. */
        @NotBlank @Size(max = 20) String academicYear,

        @Past LocalDate dateOfBirth,

        Gender gender,

        /** A class of that year, when the family has one in mind. */
        @Size(max = 60) String interestedClassDocsId,

        @Size(max = 10) List<@Valid Guardian> guardians,

        /** This school's staff, when the lead is handed to somebody at capture. */
        @Size(max = 60) String assignedCounselorDocsId,

        /** Free text — "WALK_IN", "PHONE", "REFERRAL". Nothing validates it. */
        @Size(max = 60) String source,

        @Size(max = 200) String sourceDetails,

        @Size(max = 2000) String notes) {

    /**
     * One guardian on a lead.
     *
     * <p><b>Its own record rather than the application's</b>, and that is not duplication for its
     * own sake: on an application a guardian's {@code fullName} and {@code relation} are required,
     * because the family filled a form in. On a lead they are not — the front desk writes down a
     * phone number and a first name, and a record that refused that would refuse the call.
     */
    public record Guardian(
            @Size(max = 160) String fullName,
            GuardianRelation relation,
            @Size(max = 40) String phoneNumber,
            @Size(max = 160) String emailAddress,
            @Size(max = 400) String address,
            @Size(max = 120) String occupation,
            Boolean primaryContact) {
    }
}
