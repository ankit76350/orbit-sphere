package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferCreateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferRespondRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferSearchRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferWithdrawRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferSummaryResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionResponse;
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

    /**
     * The fields #32 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>An allowlist is a security control, not a convenience</b> — ordering is a read, and
     * sorting by a field walks its values out of the database a page at a time.
     *
     * <p>{@code withdrawalReason} is deliberately absent: it is something a school wrote about one
     * family, and paging through it sorted would hand over every reason a seat was taken back. So
     * are the three document ids, which sort by nothing meaningful.
     */
    private static final Map<String, String> SORTABLE_OFFER_FIELDS = new LinkedHashMap<>();

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_OFFER_FIELD_NAMES;

    /**
     * The default order: soonest to lapse first, then by id.
     *
     * <p><b>A chase list is sorted by what runs out next</b>, which is the whole of what #32 is
     * for.
     *
     * <p><b>{@code id} is the tiebreaker, and it has to be something.</b> An offer's unique
     * business key is its number, but a fallback must be total and {@code offerNo} is only unique
     * per school — the document id is the one total order available. Without it, two offers
     * sharing an expiry can swap places between pages and one row is shown twice while another is
     * never shown.
     *
     * <p><b>An offer with no expiry sorts FIRST</b>, because Mongo puts a missing field before
     * every value in an ascending sort — which is the wrong end of a chase list and is not worth an
     * aggregation to fix. {@code expired=true} is the filter that answers "what has lapsed", and it
     * excludes them.
     */
    private static final Sort OFFER_ORDER =
            Sort.by(Sort.Order.asc("expiresAt"), Sort.Order.asc("id"));

    static {
        SORTABLE_OFFER_FIELDS.put("expiresat", "expiresAt");
        SORTABLE_OFFER_FIELDS.put("offeredat", "offeredAt");
        SORTABLE_OFFER_FIELDS.put("respondedat", "respondedAt");
        SORTABLE_OFFER_FIELDS.put("status", "status");
        SORTABLE_OFFER_FIELDS.put("offerno", "offerNo");
        SORTABLE_OFFER_FIELDS.put("createdat", "createdAt");
        SORTABLE_OFFER_FIELDS.put("updatedat", "updatedAt");
        SORTABLE_OFFER_FIELD_NAMES = String.join(", ", SORTABLE_OFFER_FIELDS.values());
    }

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
     * Endpoint #30 — the family answers.
     *
     * <p><b>This is why the offer half exists.</b> Approving is the school saying yes; this is the
     * family saying yes, and without it a school cannot tell an approved child who is coming from
     * one who went elsewhere.
     *
     * <p><b>Only an {@code ISSUED} offer can be answered, and only before it lapses.</b> An offer
     * past its {@code expiresAt} is refused here even though its stored status still says
     * {@code ISSUED} — {@code EXPIRED} is what a date in the past MEANS, and nothing writes it.
     * Accepting a seat the school withdrew the offer of last week is not an answer, it is a
     * misunderstanding.
     *
     * <p><b>{@code ACCEPTED} moves the application to {@code OFFER_ACCEPTED}; {@code DECLINED}
     * moves nothing.</b> A declined offer is not a rejected applicant — the school decided to
     * admit this child and the family chose otherwise, and those are different facts.
     */
    public AdmissionOfferResponse respond(String admissionOfferId,
            AdmissionOfferRespondRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionOfferId == null ? "" : admissionOfferId.trim();
        log.info("[respond] Step 1: Recording {} on offer {} for school {}",
                request.response(), id, school.getId());

        //! step 2 - the offer, scoped by school in the QUERY.
        AdmissionOffer offer = loadOffer(school, id);

        //! step 3 - somebody else may have answered or withdrawn it while this caller was reading.
        if (request.version() != null && !request.version().equals(offer.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "Offer " + offer.getOfferNo() + " changed since you read it — it is "
                            + offer.getStatus() + " now. Read it again before recording an "
                            + "answer.");
        }

        //! step 4 - only one status can be answered, and each refusal says which end it hit.
        if (offer.getStatus() != AdmissionOfferStatus.ISSUED) {
            throw ApiException.conflict("OFFER_NOT_OPEN",
                    "Offer " + offer.getOfferNo() + " is " + offer.getStatus()
                            + ", so there is nothing for the family to answer."
                            + (offer.getStatus() == AdmissionOfferStatus.ACCEPTED
                                    || offer.getStatus() == AdmissionOfferStatus.DECLINED
                                    ? " They answered on " + offer.getRespondedAt()
                                            + ", and an answer is not changed by sending another."
                                    : ""));
        }

        //! step 5 - and not after it has lapsed.
        //!
        //! THE DATE, NOT THE STATUS. Nothing writes EXPIRED — it is what expiresAt in the past
        //! MEANS — so a lapsed offer is still stored as ISSUED and only the clock can tell. #32
        //! asks the same question to build the chase list, and this is the other half of it: a
        //! school that never chased cannot let the answer arrive a month late.
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("OFFER_EXPIRED",
                    "Offer " + offer.getOfferNo() + " lapsed on " + offer.getExpiresAt()
                            + ", so it can no longer be answered. Nothing writes EXPIRED — a date "
                            + "in the past is what it means — so the offer still reads ISSUED and "
                            + "only the clock says otherwise.");
        }

        //! step 6 - build the change.
        AdmissionResponse answer = request.response();
        offer.setResponse(answer);
        offer.setRespondedAt(Instant.now());
        offer.setStatus(answer == AdmissionResponse.ACCEPTED
                ? AdmissionOfferStatus.ACCEPTED
                : AdmissionOfferStatus.DECLINED);

        //! A SIGNATURE IS KEPT WHEN ONE IS SENT. Not validated: it points at document_records,
        //! which has no repository and no service — and refusing every value on a field the
        //! family's acceptance carries would block the answer itself. #29's deposit invoice IS
        //! refused, because that one is optional to the act of offering rather than part of it.
        String signature = TextHelper.blankToNull(request.acceptanceSignatureDocsId());
        if (signature != null) {
            offer.setAcceptanceSignatureDocsId(signature);
        }

        //! step 7 - save the offer
        // TODO: update admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[respond] Step 2: Offer {} is {}", saved.getId(), saved.getStatus());

        //! step 8 - the form, which is read whichever way the family answered because the answer
        //! is reported against the applicant's name.
        // TODO: read admission application
        AdmissionApplication application = applications
                .findByIdAndSchoolId(saved.getAdmissionApplicationDocsId(), school.getId())
                .orElse(null);

        //! step 9 - and it follows ONLY on acceptance.
        //!
        //! A DECLINED OFFER IS NOT A REJECTED APPLICANT. The school decided to admit this child;
        //! the family chose another school. Moving the form to REJECTED or WITHDRAWN would record
        //! a decision nobody made, and WITHDRAWN is #21's, which the family drives.
        if (answer == AdmissionResponse.ACCEPTED && application != null
                && application.getStatus() != AdmissionApplicationStatus.OFFER_ACCEPTED) {

            AdmissionApplicationStatus from = application.getStatus();
            application.setStatus(AdmissionApplicationStatus.OFFER_ACCEPTED);

            // TODO: update admission application
            applications.save(application);
            log.info("[respond] Step 3: Application {} moved {} -> OFFER_ACCEPTED",
                    application.getId(), from);
        }

        return answerFor(school, saved, application);
    }

    /**
     * Endpoint #31 — the school takes the offer back.
     *
     * <p><b>A reason is required</b>, and it is the part of an admissions record worth the most: a
     * seat promised to a family and then taken away is exactly what somebody will ask about later.
     *
     * <p><b>This is what a {@code DELETE} would have been.</b> The offer stays and says it was
     * withdrawn and why.
     *
     * <p><b>It does not touch the application.</b> Withdrawing the offer does not un-approve the
     * child — the school may still admit them, and #20 is where that would be recorded. A form
     * left at {@code OFFERED} with a {@code WITHDRAWN} offer is the honest state, and with one
     * offer per admission it is also a dead end until something can edit an offer.
     */
    public AdmissionOfferResponse withdraw(String admissionOfferId,
            AdmissionOfferWithdrawRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionOfferId == null ? "" : admissionOfferId.trim();
        log.info("[withdraw] Step 1: Withdrawing offer {} for school {}", id, school.getId());

        //! step 2 - the offer, scoped by school in the QUERY.
        AdmissionOffer offer = loadOffer(school, id);

        //! step 3 - somebody else may have moved it while this caller was reading.
        if (request.version() != null && !request.version().equals(offer.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "Offer " + offer.getOfferNo() + " changed since you read it — it is "
                            + offer.getStatus() + " now. Read it again before withdrawing, so you "
                            + "are not taking back a seat the family has just accepted.");
        }

        //! step 4 - only an offer that is still out can be taken back.
        //!
        //! AN ACCEPTED ONE IS REFUSED, and that is the interesting line. The family has the seat;
        //! taking it away is a bigger act than withdrawing a letter nobody answered, and it is not
        //! this endpoint's to do quietly. A LAPSED one is refused too — there is nothing to take
        //! back from a family who can no longer accept.
        if (offer.getStatus() != AdmissionOfferStatus.ISSUED) {
            throw ApiException.conflict("OFFER_NOT_OPEN",
                    "Offer " + offer.getOfferNo() + " is " + offer.getStatus()
                            + ", so there is nothing to take back."
                            + (offer.getStatus() == AdmissionOfferStatus.ACCEPTED
                                    ? " The family accepted it on " + offer.getRespondedAt()
                                            + " and holds the seat; taking that away is a decision "
                                            + "about the APPLICATION (#20), not a tidy-up of the "
                                            + "letter."
                                    : ""));
        }

        //! step 5 - build the change. THE REASON IS KEPT, not logged and dropped.
        offer.setStatus(AdmissionOfferStatus.WITHDRAWN);
        offer.setWithdrawalReason(request.withdrawalReason().trim());

        //! NOT respondedAt. The family did not answer — the school changed its mind — and stamping
        //! it would make a withdrawal look like a decline in every list that reads that field.

        //! step 6 - save
        // TODO: update admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[withdraw] Step 2: Offer {} is WITHDRAWN", saved.getId());

        //! step 7 - the form, for the answer only. NOT CHANGED: withdrawing an offer does not
        //! un-approve a child.
        // TODO: read admission application
        AdmissionApplication application = applications
                .findByIdAndSchoolId(saved.getAdmissionApplicationDocsId(), school.getId())
                .orElse(null);

        return answerFor(school, saved, application);
    }

    /**
     * Endpoint #32 — <b>what is expiring</b>. The chase list.
     *
     * <p><b>Soonest to lapse first</b>, which is the whole of what a chase list is. Filter by
     * {@code status} and {@code expiringBefore} and you have this week's phone calls; those two
     * are exactly {@code school_offer_status_expiry_idx}, which exists for this.
     *
     * <p><b>{@code expired=true} is the sharper question</b> — past its date <i>and</i> still
     * {@code ISSUED}, because an offer a family accepted last month also has a past date.
     *
     * <p><b>No gates.</b> A read — a suspended school still needs to know what it promised.
     */
    public PageResponse<AdmissionOfferSummaryResponse> listOffers(
            AdmissionOfferSearchRequest request) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();

        //! step 2 - the page, the sort and the allowlist
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_OFFER_FIELDS, SORTABLE_OFFER_FIELD_NAMES, OFFER_ORDER);

        // TODO: read admission offers
        Page<AdmissionOffer> found = admissionOffers.search(school.getId(), request, pageable);
        log.info("[listOffers] Step 1: Found {} offer(s) in total", found.getTotalElements());

        //! step 3 - the applicants and the classes, ONE QUERY EACH FOR THE WHOLE PAGE. A chase
        //! list of raw ids is not a list anybody can work from: the point of it is to ring people.
        List<String> applicationIds = found.getContent().stream()
                .map(AdmissionOffer::getAdmissionApplicationDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        // TODO: read admission applications
        Map<String, AdmissionApplication> forms = applicationIds.isEmpty()
                ? Map.of()
                : applications.findBySchoolIdAndIdIn(school.getId(), applicationIds).stream()
                        .collect(Collectors.toMap(AdmissionApplication::getId, one -> one,
                                (first, second) -> first));

        List<String> classIds = found.getContent().stream()
                .map(AdmissionOffer::getOfferedClassDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! THE CLASSES ARE READ WITHOUT A YEAR, unlike everywhere else in this module. A page can
        //! hold offers from several rounds and a round admits into its own year, so there is no
        //! single year to scope by — the school is the scope, and the id is already the school's
        //! because the offer that names it is.
        // TODO: read school classes
        Map<String, String> classNames = classIds.isEmpty()
                ? Map.of()
                : schoolClasses.findBySchoolIdAndIdIn(school.getId(), classIds).stream()
                        .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName,
                                (first, second) -> first));

        //! step 4 - thin rows. The reason and the document ids are on the offer, not on a list.
        return PageResponse.from(found, one -> {
            AdmissionApplication form = forms.get(one.getAdmissionApplicationDocsId());
            return AdmissionOfferSummaryResponse.fromOffer(one,
                    form == null ? null : form.getApplicationNo(),
                    form == null ? null : form.getApplicantName(),
                    one.getOfferedClassDocsId() == null ? null
                            : classNames.get(one.getOfferedClassDocsId()));
        });
    }

    /**
     * One offer of this school, or a 404.
     *
     * <p>Private and inline rather than in a {@code utils} file: it is two lines and a throw, and
     * the folder rules put shared READS there — this is the guard #30 and #31 both start with.
     *
     * Used by:
     * - respond()
     * - withdraw()
     */
    private AdmissionOffer loadOffer(School school, String admissionOfferId) {
        // TODO: read admission offer
        return admissionOffers.findByIdAndSchoolId(admissionOfferId, school.getId())
                .orElseThrow(() -> ApiException.notFound("OFFER_NOT_FOUND",
                        "No admission offer with id '" + admissionOfferId + "' in this school."));
    }

    /**
     * The whole offer, with the names its ids stand for.
     *
     * <p>The tail #30 and #31 share. The class and the issuer are read here rather than carried
     * from the write, because neither endpoint has them in hand the way #29 does.
     *
     * Used by:
     * - respond()
     * - withdraw()
     */
    private AdmissionOfferResponse answerFor(School school, AdmissionOffer offer,
            AdmissionApplication application) {

        //! BOTH READS ARE TOLERANT. A class that was removed, or a staff member who has left, must
        //! not stop a family's answer being recorded — the answer is the fact, the names are the
        //! decoration.
        String offeredClassName = null;
        if (offer.getOfferedClassDocsId() != null) {
            // TODO: read school class
            offeredClassName = schoolClasses
                    .findBySchoolIdAndIdIn(school.getId(), List.of(offer.getOfferedClassDocsId()))
                    .stream()
                    .findFirst()
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        String issuedByName = null;
        if (offer.getIssuedByDocsId() != null) {
            // TODO: read staff
            issuedByName = staff.findByIdAndSchoolId(offer.getIssuedByDocsId(), school.getId())
                    .map(Staff::getFullName)
                    .orElse(null);
        }

        return AdmissionOfferResponse.fromOffer(offer,
                application == null ? null : application.getApplicationNo(),
                application == null ? null : application.getApplicantName(),
                offeredClassName, issuedByName,
                nextStepFor(offer) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * What happens to this offer next, in plain words.
     *
     * <p>Private and inline: one caller, and the folder rules keep single-use logic where it is
     * used. It moves to {@code utils} when #30 also answers with it.
     */
    private static String nextStepFor(AdmissionOffer offer) {
        return switch (offer.getStatus()) {
            case ISSUED -> "It is out with the family. #30 records their answer — ACCEPTED or "
                    + "DECLINED — and #31 takes it back if the school changes its mind."
                    //! A LAPSED ONE STILL READS ISSUED, because nothing writes EXPIRED. The clock
                    //! is the only thing that knows, so the answer has to do the comparison.
                    + (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(Instant.now())
                            ? " IT HAS ALREADY LAPSED — it ran out on " + offer.getExpiresAt()
                                    + " — so #30 will refuse it. The stored status still says "
                                    + "ISSUED because nothing writes EXPIRED; a date in the past "
                                    + "is what that means, and #32 is how a school finds them."
                            : " It lapses on " + offer.getExpiresAt() + ", after which #30 "
                                    + "refuses — which is what EXPIRED means, rather than a call "
                                    + "anybody makes.");
            case ACCEPTED -> "The family accepted, and the application is OFFER_ACCEPTED. #33 "
                    + "turns the applicant into a student, and it is not built — it needs the "
                    + "student module. An answer is not changed by sending another.";
            case DECLINED -> "The family went elsewhere. THE APPLICATION IS NOT REJECTED — the "
                    + "school decided to admit this child and they chose otherwise — so it stays "
                    + "where it is. There is one offer letter per admission and this one is "
                    + "answered, so nothing here can offer the seat to them again.";
            case WITHDRAWN -> "The school took it back, and the reason is on the offer. The "
                    + "application is untouched: withdrawing an offer does not un-approve a child, "
                    + "and #20 is where a change of mind about the CHILD would be recorded.";
            //! NEITHER IS REACHABLE. DRAFT has no endpoint that writes it, EXPIRED is what a date
            //! means rather than a status anything sets, and SUPERSEDED stopped being written when
            //! the one-offer rule replaced revisions. All three are answered rather than left to
            //! fall through, because a switch that cannot fail is one fewer thing to get wrong.
            case DRAFT -> "Nothing writes DRAFT — #29 issues directly. If you are reading this, "
                    + "something wrote it straight to the database.";
            case EXPIRED -> "Nothing writes EXPIRED either: it is what a past expiresAt MEANS, and "
                    + "#32 is how a school finds the offers it applies to.";
            case SUPERSEDED -> "Nothing writes SUPERSEDED any more. It belonged to the revision "
                    + "model that one-offer-per-admission replaced on 2026-09-23.";
        };
    }
}
