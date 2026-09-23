package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferCreateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.finance.billing.FeeInvoice;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
import com.orbitastra.backend.repositories.finance.feeinvoice.FeeInvoiceRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The seat a school formally offers, and what happens to it. Endpoint #29 of the plan in this
 * package's README; #30, #31 and #32 are not built.
 *
 * <p><b>This is the half of the module that was nearly deleted.</b> The argument against it was
 * that approving an application is the school saying yes, so an offer is ceremony on top of a
 * decision already made. The argument that kept it is that <b>approving is not the FAMILY saying
 * yes</b>: a family applies to five schools, three approve, and one child arrives. Without a
 * recorded answer a school cannot tell an approved child who is coming from one who went
 * elsewhere, and its seat counts are fiction.
 *
 * <p><b>No {@code utils} file, and that is the folder rule rather than an omission.</b> One public
 * method cannot repeat a read, so there is nothing two callers share. It gets one when #30 arrives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionOfferService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    /**
     * The application statuses an offer can be issued against.
     *
     * <p><b>{@code APPROVED} is the obvious one and {@code WAITLISTED} is the plan's</b> — a seat
     * comes free and the school offers it to a child it had held back. The graph draws
     * {@code WAITLISTED → APPROVED}, but going through #20 first would record a decision the school
     * never made separately from the offer.
     *
     * <p><b>{@code OFFERED} is NOT here, and that is the whole of the one-offer rule.</b> A form
     * that is {@code OFFERED} already has its offer, so there is nothing for this endpoint to do
     * with it — a second one is refused by the status and by the offer check below, which say the
     * same thing from two directions.
     *
     * <p><b>{@code OFFER_ACCEPTED} is absent for the same reason</b>, and more strongly: the family
     * has said yes to something specific.
     */
    private static final Set<AdmissionApplicationStatus> OFFERABLE = EnumSet.of(
            AdmissionApplicationStatus.APPROVED,
            AdmissionApplicationStatus.WAITLISTED);

    /**
     * The revision every offer has, because there is only ever one.
     *
     * <p><b>Pinning it to 1 lines the rule up with the declared index.</b>
     * {@code school_application_offer_revision_uniq} is unique on
     * {@code (schoolId, admissionApplicationDocsId, revisionNo)}, so a fixed revision makes that
     * index mean "one offer per application" — a second write would collide rather than race.
     *
     * <p><b>But the index is DECLARED, not present — measured 2026-09-23.</b> This project keeps
     * Mongo's auto-index-creation off (it cost six minutes a boot) and builds the indexes on
     * demand, so a development database has only {@code _id_} and a duplicate inserted straight
     * into Mongo is accepted. <b>Until the indexes are synced, the check below is the only thing
     * enforcing this rule</b> — which is worth knowing rather than assuming the database has your
     * back.
     */
    private static final int THE_ONLY_REVISION = 1;

    private final AdmissionOfferRepository admissionOffers;
    private final AdmissionApplicationRepository applications;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final FeeInvoiceRepository feeInvoices;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;

    /**
     * Endpoint #29 — the school offers a seat.
     *
     * <p><b>ONE OFFER PER APPLICATION, and that is the rule this endpoint exists to keep.</b> A
     * school issues one offer letter for one admission; if it expires the school extends it, and if
     * anything else changes the school edits it. There is no second document and no revision
     * history — a family holds one letter, and the record should say the same thing they are
     * holding.
     *
     * <p><b>An earlier build of this superseded instead</b>, which is what the plan asked for: a
     * later offer marked the previous one {@code SUPERSEDED} and both were kept. That was replaced
     * on 2026-09-23 because two documents for one seat is two things to keep in step, and the
     * question it answered — "what did we originally offer" — is one this module has never been
     * asked. {@code SUPERSEDED} is now unreachable, like {@code DRAFT}.
     *
     * <p><b>The offered class is not always the applied class.</b> A school assesses a child and
     * offers a different grade; the offer carries its own class for exactly that.
     *
     * <p><b>It does not cap the number of offers against the seat table</b>, and that is a decision.
     * Schools deliberately over-offer — sixty offers for forty places, because a fifth of families
     * go elsewhere — so a hard refusal at {@code totalSeats} would refuse the normal case. What it
     * DOES refuse is a class the round has no seats for at all, which is #17's rule and is
     * nonsense rather than strategy. Counting offers against seats is #7's job.
     */
    public AdmissionOfferResponse issueOffer(String admissionApplicationId,
            AdmissionOfferCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String applicationId = admissionApplicationId == null ? "" : admissionApplicationId.trim();
        log.info("[issueOffer] Step 1: Offering on application {} for school {}",
                applicationId, school.getId());

        //! step 2 - the form, scoped by school in the QUERY. Inline rather than borrowed from the
        //! application service's utils: one caller, and a main service uses its OWN utils.
        // TODO: read admission application
        AdmissionApplication application = applications
                .findByIdAndSchoolId(applicationId, school.getId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No admission application with id '" + applicationId
                                + "' in this school."));

        //! step 3 - a seat is offered to somebody the school has said yes to.
        if (!OFFERABLE.contains(application.getStatus())) {
            throw ApiException.conflict("APPLICATION_NOT_ELIGIBLE_FOR_OFFER",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so there is no seat to offer. #20 is what approves or waitlists a "
                            + "form, and an offer follows that decision rather than making it."
                            + (application.getStatus() == AdmissionApplicationStatus.OFFERED
                                    || application.getStatus()
                                            == AdmissionApplicationStatus.OFFER_ACCEPTED
                                    ? " This form already has its offer, and there is only ever "
                                            + "one: extending or correcting it is an edit to that "
                                            + "letter rather than a second one."
                                    : ""));
        }

        //! step 4 - the round, which is where the seat table and the published deadline live. It
        //! THROWS rather than reading tolerantly: an offer is a promise about a seat in a round,
        //! and a round nobody can find has no seats and no deadline to promise anything against.
        AdmissionCycle cycle = helper.loadCycle(school, application.getAdmissionCycleDocsId());

        //! step 5 - the class being offered. NOT NECESSARILY THE ONE APPLIED FOR, and read in the
        //! CYCLE'S year, which is the only year this round admits into.
        String offeredClassId = request.offeredClassDocsId().trim();

        // TODO: read school class
        SchoolClass offered = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(offeredClassId, school.getId(),
                        cycle.getAcademicYear())
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "No class with id '" + offeredClassId + "' in '"
                                + cycle.getAcademicYear() + "', which is the year '"
                                + cycle.getName() + "' admits into."));

        //! step 6 - and the round has to have seats set up for it. THE SAME RULE #17 APPLIES to
        //! the class applied for: a class that is not in the seat table is a class this round is
        //! not admitting into, so a seat in it is not the school's to offer.
        //!
        //! IT IS NOT A COUNT. Over-offering is deliberate — see the method's note.
        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();

        if (seats.stream().noneMatch(seat -> offeredClassId.equals(seat.getClassDocsId()))) {
            throw ApiException.conflict("CLASS_NOT_IN_CAPACITY",
                    "'" + cycle.getName() + "' has no seats set up for " + offered.getName()
                            + ", so there is none to offer. Add it to the seat table with #4 "
                            + "first.");
        }

        //! step 7 - who issued it, when the caller says. OPTIONAL because nothing knows who is
        //! calling yet — but an id that IS sent has to be real, or the offer names a person who
        //! does not work here.
        String issuedById = TextHelper.blankToNull(request.issuedByDocsId());
        Staff issuedBy = null;

        if (issuedById != null) {
            // TODO: read staff
            issuedBy = staff.findByIdAndSchoolId(issuedById, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + issuedById + "' in this school, so the "
                                    + "offer cannot say they issued it."));
        }

        //! step 8 - the deposit invoice, when the caller names one. AN ID NOTHING VERIFIES IS AN
        //! ID THAT CAN BE ANYTHING, and "13212313" was accepted and stored until this check
        //! existed. An offer pointing at an invoice that is not there tells a family to settle a
        //! bill nobody can find.
        //!
        //! IT REFUSES EVERYTHING TODAY, and that is the honest state rather than a bug: nothing
        //! writes fee_invoices — the finance module has models and no service — so there is no
        //! real id to send. The field is therefore unusable until that module exists, which is
        //! worth knowing rather than papering over by accepting any string.
        //!
        //! AND IT WILL STILL NOT FIT WHEN IT DOES. FeeInvoice extends AcademicStudentSchoolBase,
        //! which requires a studentDocsId — and an applicant is not a student until #33 enrolls
        //! them. An admission DEPOSIT invoice for somebody who is not yet a student is a shape
        //! that collection does not currently have.
        String depositInvoiceId = TextHelper.blankToNull(request.depositInvoiceDocsId());

        if (depositInvoiceId != null) {
            // TODO: read fee invoice
            FeeInvoice deposit = feeInvoices
                    .findByIdAndSchoolId(depositInvoiceId, school.getId())
                    .orElse(null);

            if (deposit == null) {
                throw ApiException.notFound("FEE_INVOICE_NOT_FOUND",
                        "No fee invoice with id '" + depositInvoiceId + "' in this school, so the "
                                + "offer cannot point at it. Nothing writes fee_invoices yet — the "
                                + "finance module has models and no service — so there is no id "
                                + "this will accept today. Leave the field out.");
            }
        }

        //! step 9 - when it runs out. The caller's date, or the round's published deadline —
        //! WHICH IS THE POINT OF THE DEFAULT: the school already told families that date, and an
        //! offer with no deadline is a seat held for ever.
        Instant expiresAt = request.expiresAt() != null
                ? request.expiresAt()
                : cycle.getEnrollmentDeadlineAt();

        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw ApiException.badRequest("OFFER_EXPIRY_IN_THE_PAST",
                    "That offer would expire on " + expiresAt + ", which has already passed"
                            + (request.expiresAt() == null
                                    ? " — it is '" + cycle.getName() + "'s enrollment deadline, "
                                            + "used because the request named no date. Send one, "
                                            + "or move the round's deadline with #2."
                                    : ".")
                            + " An offer nobody could accept is not an offer.");
        }

        //! step 10 - has this application been offered anything already. ONE OFFER PER
        //! APPLICATION: a school issues one letter, and everything that happens afterwards —
        //! extending it, correcting it, withdrawing it — is an edit to that letter.
        //!
        //! EVERY STATUS COUNTS, not only the live ones. A WITHDRAWN or DECLINED offer is still
        //! this application's offer, and its status is the record of what became of it; a second
        //! row would leave two documents claiming to be the school's answer to one family.
        //!
        //! THE STATUS CHECK ABOVE ALREADY REFUSES THE COMMON CASE, because issuing moves the form
        //! to OFFERED. This one catches what that cannot: a form moved back by #20 after an offer
        //! went out, which leaves an offer standing against a form that is APPROVED again.
        // TODO: read admission offers
        List<AdmissionOffer> existing = admissionOffers
                .findBySchoolIdAndAdmissionApplicationDocsIdOrderByRevisionNoAsc(
                        school.getId(), application.getId());

        if (!existing.isEmpty()) {
            AdmissionOffer already = existing.get(0);
            throw ApiException.conflict("OFFER_ALREADY_ISSUED",
                    "'" + application.getApplicantName() + "' already has offer "
                            + already.getOfferNo() + ", which is " + already.getStatus()
                            + ". There is one offer letter per admission: extend it or correct it "
                            + "rather than issuing a second, so the record says what the family is "
                            + "holding.");
        }

        //! step 11 - the number. Generated, never supplied: nobody picks their own offer number.
        String offerNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_OFFER, "OFFER/{YYYY}/{MM}/");
        log.info("[issueOffer] Step 2: Allocated offer number {}", offerNo);

        //! step 12 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        //!
        //! ISSUED, NOT DRAFT. Issuing is the endpoint, so offeredAt is stamped here. DRAFT is on
        //! the enum and no endpoint writes it — the same honest gap as EXPIRED, which is what a
        //! date in the past MEANS rather than a call anybody makes.
        AdmissionOffer offer = AdmissionOffer.builder()
                .schoolId(school.getId())
                .offerNo(offerNo)
                //! PINNED, AND MUTATION CANNOT TELL THIS FROM `existing.size() + 1` — proven
                //! 2026-09-23. A second offer is refused at step 10, so `existing` is always
                //! empty by the time anything is built and both expressions give 1. The constant
                //! is kept because it says WHY the number is 1, which the arithmetic does not.
                .revisionNo(THE_ONLY_REVISION)
                .admissionApplicationDocsId(application.getId())
                .offeredClassDocsId(offered.getId())
                .status(AdmissionOfferStatus.ISSUED)
                .offeredAt(Instant.now())
                .expiresAt(expiresAt)
                .depositInvoiceDocsId(depositInvoiceId)
                .issuedByDocsId(issuedById)
                .build();

        //! step 13 - save the offer
        // TODO: insert admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[issueOffer] Step 3: Offer {} issued for application {}",
                saved.getId(), application.getId());

        //! step 14 - the form follows. A CONSEQUENCE, not a request: #20 names statuses and this
        //! one does not, because moving to OFFERED is what issuing an offer MEANS.
        if (application.getStatus() != AdmissionApplicationStatus.OFFERED) {
            AdmissionApplicationStatus from = application.getStatus();
            application.setStatus(AdmissionApplicationStatus.OFFERED);

            // TODO: update admission application
            applications.save(application);
            log.info("[issueOffer] Step 4: Application {} moved {} -> OFFERED",
                    application.getId(), from);
        }

        return AdmissionOfferResponse.fromOffer(saved, application.getApplicationNo(),
                application.getApplicantName(), offered.getName(),
                issuedBy == null ? null : issuedBy.getFullName(),
                nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * What happens to this offer next, in plain words.
     *
     * <p>Private and inline: one caller, and the folder rules keep single-use logic where it is
     * used. It moves to {@code utils} when #30 also answers with it.
     */
    private static String nextStepFor(AdmissionOffer offer) {
        return "The family answers with #30, which is not built — so this offer cannot move past "
                + "ISSUED yet. It is the ONLY offer this application will have: extending or "
                + "correcting it is an edit to this letter, and there is no endpoint for that yet."
                + (offer.getExpiresAt() == null
                        ? " It has no expiry date, because the round has no enrollment deadline "
                                + "and none was sent: nothing will ever make it lapse."
                        : " It lapses on " + offer.getExpiresAt() + ", which is what EXPIRED "
                                + "means — a date in the past, not a call anybody makes.");
    }
}
