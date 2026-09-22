package com.orbitastra.backend.dto.crm.admissionapplication.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.GuardianRelation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * A new admission application. Endpoint #17.
 *
 * <h2>The inquiry is optional, and that is the point</h2>
 *
 * <p>A family that walks in with a completed form never enquired. So {@code inquiryDocsId} is
 * nullable, and the whole pipeline is testable without a single lead in the database — which is
 * why the inquiry endpoints are late in the build order rather than first.
 *
 * <h2>More is required here than on an inquiry</h2>
 *
 * <p>An inquiry may be a name and a phone number; an application is a formal document. So
 * {@code dateOfBirth}, {@code gender} and at least one guardian are required here where the
 * inquiry left them optional.
 *
 * <h2>The guardians are copied, then owned by the application</h2>
 *
 * <p>When an inquiry is named its guardians are copied in — but what is sent here wins. The parent
 * filling the form is the one who signs it, and editing the inquiry afterwards must not rewrite an
 * application the school has already acted on.
 */
public record AdmissionApplicationCreateRequest(

        /**
         * Which round this is an application to. Example: "67aa15d9dc3f7d0011111111"
         *
         * <p><b>Must be OPEN.</b> That check is this module's replacement for gate 4 — every other
         * module refuses a write against a year that is not running, and admissions cannot,
         * because a cycle for next year is the normal case.
         */
        @NotBlank @Size(max = 60) String admissionCycleDocsId,

        /**
         * The lead this came from, when there was one. Example: "67aa15d9dc3f7d0022222222"
         *
         * <p><b>Nullable on purpose</b> — the family that walks in with a form. When given, the
         * inquiry must be this school's, and it may produce only one application per cycle.
         */
        @Size(max = 60) String inquiryDocsId,

        /**
         * Which class is being applied for. Example: "67aa15d9dc3f7d0033333333"
         *
         * <p>Must belong to the <b>cycle's</b> academic year, and be in the cycle's seat table — a
         * class with no seats is a class nothing can be offered in.
         */
        @NotBlank @Size(max = 60) String appliedClassDocsId,

        /** The child. Example: "Aarav Sharma" */
        @NotBlank @Size(max = 160) String applicantName,

        /** Required here, unlike on an inquiry. Example: "2020-04-11" */
        @NotNull @Past LocalDate dateOfBirth,

        /** Required here, unlike on an inquiry. */
        @NotNull Gender gender,

        /** At least one. The first is the family's point of contact unless one is marked primary. */
        @NotEmpty @Size(max = 10) List<@Valid Guardian> guardians,

        /**
         * The extra answers this school asks for. Optional, and <b>nothing checks them</b>.
         *
         * <p>There is no form definition model — the fields that named one were deleted on
         * 2026-09-21 — so the answers are stored as sent. Nothing can check they match the
         * questions, that the required ones are there, or that a number is a number.
         */
        Map<String, Object> formAnswers) {

    /** One parent or guardian on the form. */
    public record Guardian(
            @NotBlank @Size(max = 160) String fullName,
            @NotNull GuardianRelation relation,
            @Size(max = 40) String phoneNumber,
            @Size(max = 160) String emailAddress,
            @Size(max = 400) String address,
            @Size(max = 120) String occupation,
            Boolean primaryContact) {
    }
}
