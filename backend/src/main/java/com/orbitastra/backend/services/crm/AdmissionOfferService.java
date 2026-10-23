package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.Map;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferUpdateRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferWithdrawRequest;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferSummaryResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.common.time.AcademicYearWindow;
import com.orbitastra.backend.common.time.AcademicYearWindow.Bound;
import com.orbitastra.backend.common.time.AcademicYearWindow.Refusal;
import com.orbitastra.backend.common.time.SchoolZone;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionResponse;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.crm.utils.AdmissionOfferServiceUtils;
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
 * <p><b>It gained a {@code utils} file when #30 and #31 arrived</b>, which is what the note here
 * predicted while it had one endpoint and nothing that could repeat. Three things moved: the guard
 * both writes start with, the answer both end with, and the sentence that answer carries.
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
     * <p><b>{@code OFFERED} JOINED THIS SET ON 2026-09-30, when the one-offer rule went.</b> A
     * form that is {@code OFFERED} has a letter out, and whether another may be written now
     * depends on what became of that one — which is a question about the OFFER, not the form. The
     * check at step 10 answers it: a {@code DRAFT} or {@code ACCEPTED} letter blocks, an
     * {@code ISSUED} one is superseded, and a {@code DECLINED}, {@code EXPIRED} or
     * {@code WITHDRAWN} one stands aside.
     *
     * <p><b>{@code OFFER_ACCEPTED} is still absent, and more strongly than before:</b> the family
     * has said yes to something specific, and that is the one answer a school does not get to
     * replace. It is the same refusal step 10 gives for an {@code ACCEPTED} letter, said by the
     * form instead — and it is the one that actually fires, because accepting moves the form.
     */
    private static final Set<AdmissionApplicationStatus> OFFERABLE = EnumSet.of(
            AdmissionApplicationStatus.APPROVED,
            AdmissionApplicationStatus.WAITLISTED,
            AdmissionApplicationStatus.OFFERED);

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
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;
    private final AdmissionOfferServiceUtils utils;
    private final AcademicYearWindow yearWindow;
    private final SchoolZone schoolZone;

    /**
     * Endpoint #29 — the school offers a seat.
     *
     * <p><b>ONE LIVE OFFER PER APPLICATION — not one offer.</b> A family holds one letter at a
     * time, and that is what this endpoint keeps. It is not the same as one row: a family who
     * declined can be offered again, and a school re-offering on different terms leaves the old
     * letter behind as the record of what it replaced. So the question is which answers are still
     * open, never how many rows exist.
     *
     * <p><b>A {@code DRAFT} or an {@code ACCEPTED} letter blocks.</b> A draft is a decision
     * somebody is still writing and a second one alongside is two drafts of one answer; an
     * accepted one is a seat the family has taken, and offering again would be the school going
     * back on it. {@code DECLINED}, {@code EXPIRED} and {@code WITHDRAWN} do not block — each
     * already says what became of it.
     *
     * <p><b>An {@code ISSUED} one is superseded rather than refused.</b> The school is re-offering
     * on different terms — a different grade, a longer deadline — so the old letter is marked
     * {@code SUPERSEDED} and the new one is the live answer. This is what finally writes
     * {@code SUPERSEDED}, which the enum had carried unused since it was written, and it is why
     * {@code revisionNo} counts: it is what tells the second letter from the first.
     *
     * <p><b>This reverses 2026-09-23, and it is worth saying why.</b> That build refused a second
     * offer outright, on the reasoning that two documents for one seat is two things to keep in
     * step. What it actually did was close the admission for good the moment a family declined —
     * the state a school most needs to recover from. Keeping both is the lesser cost, because only
     * one of them is ever live.
     *
     * <p><b>Nothing is ever stored {@code EXPIRED}</b>, which matters here: a lapsed offer is an
     * {@code ISSUED} one whose {@code expiresAt} has passed, so it takes the supersede path and
     * not the leave-alone one. That is the right half — it was live until this moment.
     *
     * <p><b>The offered class is not always the applied class.</b> A school assesses a child and
     * offers a different grade; the offer carries its own class for exactly that.
     *
     * <p><b>Any class in the year may be offered, seats or no seats</b> — changed 2026-09-30. The
     * seat table is what #17 checks when a family APPLIES; an offer is the school's own decision
     * afterwards, and a school that assesses a child and offers a grade outside the intake plan
     * should not be stopped by a plan it is allowed to depart from. The year is the only line
     * left: a class of another year is a seat in a round this one does not admit into. Counting
     * offers against seats stays #7's job, and it never capped them anyway — schools deliberately
     * over-offer, sixty letters for forty places, because a fifth of families go elsewhere.
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
                            + (application.getStatus()
                                    == AdmissionApplicationStatus.OFFER_ACCEPTED
                                    ? " The family has taken that seat, and that is the one answer "
                                            + "the school does not get to replace."
                                    : ""));
        }

        //! step 4 - the round, which is where the seat table and the published deadline live. It
        //! THROWS rather than reading tolerantly: an offer is a promise about a seat in a round,
        //! and a round nobody can find has no seats and no deadline to promise anything against.
        AdmissionCycle cycle = helper.loadCycle(school, application.getAdmissionCycleDocsId());

        //! step 5 - the class being offered. NOT NECESSARILY THE ONE APPLIED FOR. One question is
        //! left — does it exist in the CYCLE'S year — and it lives in utils, because #29b asks the
        //! same one when the school corrects the grade. The seat check that used to sit beside it
        //! went on 2026-09-30; see the javadoc.
        SchoolClass offered = utils.offerableClass(school, cycle, request.offeredClassDocsId());

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

        //! step 8 - the deposit invoice, when the caller names one. In utils, because #29b asks
        //! the same question — and it refuses EVERYTHING today, which is the honest state rather
        //! than a bug: nothing writes fee_invoices. See the utils method for why it will not fit
        //! even when the finance module exists.
        String depositInvoiceId = TextHelper.blankToNull(request.depositInvoiceDocsId());
        utils.requireInvoice(school, depositInvoiceId);

        //! step 9 - when it runs out, and THERE IS NO LONGER A DEFAULT. It used to fall back to
        //! the cycle's enrollmentDeadlineAt, and that field went on 2026-09-28: an admission cycle
        //! now carries only the window applications are made in, which is not the date an offer
        //! expires on. Guessing one from applicationCloseAt would be worse than none — offers go
        //! out after applications close, so every offer would be born expired.
        //!
        //! WHICH MEANS AN OFFER SENT WITHOUT A DATE IS A SEAT HELD FOR EVER. Making expiresAt
        //! @NotNull on this request is the fix if that matters, and it is a one-line change.
        Instant expiresAt = request.expiresAt();

        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw ApiException.badRequest("OFFER_EXPIRY_IN_THE_PAST",
                    "That offer would expire on " + expiresAt + ", which has already passed. "
                            + "An offer nobody could accept is not an offer.");
        }

        //! AND IT CANNOT OUTLAST THE YEAR THE SEAT IS IN. A seat held open past the end of the
        //! year it belongs to is a seat in a year that has finished: the child would be starting
        //! a school year that is already over, and the place could not be given to anybody else
        //! in time either. The cycle's own dates say nothing about this — they are the window
        //! applications were taken in, which closed long before a letter went out.
        //!
        //! THE SAME COMPONENT EVERY OTHER DATE-VERSUS-YEAR RULE USES, with this module's own code
        //! and its own bound: an offer may expire before the year begins — a school can give a
        //! family a fortnight in the spring to accept a September place — but never after it ends.
        if (expiresAt != null) {
            yearWindow.requireDates(school, cycle.getAcademicYear(), schoolZone.of(school),
                    Bound.NOT_AFTER_THE_END,
                    Refusal.badRequest("OFFER_EXPIRY_OUTSIDE_ACADEMIC_YEAR"),
                    Map.of("expiresAt", expiresAt));
        }

        //! step 10 - what became of this application's last offer, if it had one.
        //!
        //! IT USED TO BE ONE OFFER PER APPLICATION, FULL STOP — changed 2026-09-30. Every status
        //! counted, so a family who declined could never be offered anything again and a withdrawn
        //! letter closed the admission for good. What the rule was protecting is real though: two
        //! documents both claiming to be the school's answer to one family. So the rule is now
        //! about which answers are still OPEN rather than about how many rows exist.
        //!
        //! A DRAFT OR AN ACCEPTED OFFER BLOCKS. A draft is a letter somebody is still writing, and
        //! a second one alongside is two drafts of one decision; an accepted one is a seat the
        //! family has taken, and offering again would be the school going back on it.
        //!
        //! AN ISSUED ONE IS SUPERSEDED. The school is re-offering on different terms — a different
        //! grade, a longer deadline — and the old letter becomes the record that it was replaced
        //! rather than a second live answer. This is what finally writes SUPERSEDED, which the
        //! enum has carried unused since it was written.
        //!
        //! DECLINED, EXPIRED AND WITHDRAWN ARE LEFT EXACTLY AS THEY ARE. Those already say what
        //! became of them, and rewriting a declined letter as superseded would lose the fact that
        //! the family said no.
        //!
        //! NOTHING IS EVER STORED EXPIRED, which is worth knowing here: a lapsed offer is an
        //! ISSUED one whose expiresAt has passed, so it takes the supersede path rather than the
        //! leave-alone one. That is the right half — it was live until this moment.
        // TODO: read admission offers
        List<AdmissionOffer> existing = admissionOffers
                .findBySchoolIdAndAdmissionApplicationDocsIdOrderByRevisionNoAsc(
                        school.getId(), application.getId());

        AdmissionOffer blocking = existing.stream()
                .filter(one -> one.getStatus() == AdmissionOfferStatus.DRAFT
                        || one.getStatus() == AdmissionOfferStatus.ACCEPTED)
                .findFirst()
                .orElse(null);

        if (blocking != null) {
            throw ApiException.conflict("OFFER_ALREADY_ISSUED",
                    "'" + application.getApplicantName() + "' already has offer "
                            + blocking.getOfferNo() + ", which is " + blocking.getStatus()
                            + (blocking.getStatus() == AdmissionOfferStatus.ACCEPTED
                                    ? ". The family has taken that seat — offering again would be "
                                            + "the school going back on it."
                                    : ". That letter is still being written; finish or withdraw it "
                                            + "rather than starting a second.")
                            + " A DECLINED, EXPIRED or WITHDRAWN offer does not stand in the way, "
                            + "and an ISSUED one is superseded by the new letter.");
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
                //! COUNTED NOW, AND IT USED TO BE PINNED AT 1. Until 2026-09-30 a second offer
                //! was refused outright, so `existing` was always empty and the constant said WHY
                //! the number was always 1. A family can be re-offered now, and the revision is
                //! what tells the second letter from the first.
                .revisionNo(existing.size() + 1)
                .admissionApplicationDocsId(application.getId())
                .offeredClassDocsId(offered.getId())
                .status(AdmissionOfferStatus.ISSUED)
                .offeredAt(Instant.now())
                .expiresAt(expiresAt)
                .depositInvoiceDocsId(depositInvoiceId)
                .issuedByDocsId(issuedById)
                .build();

        //! step 13 - the letter this one replaces, if there is a live one. FIRST, so that the
        //! moment between the two writes has the old offer already stood down rather than two
        //! letters both claiming to be the school's answer.
        //!
        //! ONLY AN ISSUED ONE. A DECLINED, EXPIRED or WITHDRAWN offer already says what became of
        //! it, and rewriting a declined letter as superseded would lose the fact that the family
        //! said no. DRAFT and ACCEPTED never reach here — step 10 refuses them.
        for (AdmissionOffer older : existing) {
            if (older.getStatus() == AdmissionOfferStatus.ISSUED) {
                older.setStatus(AdmissionOfferStatus.SUPERSEDED);

                // TODO: update admission offer
                admissionOffers.save(older);
                log.info("[issueOffer] Step 3: Offer {} superseded by the new letter",
                        older.getOfferNo());
            }
        }

        //! step 14 - save the offer
        // TODO: insert admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[issueOffer] Step 3: Offer {} issued for application {}",
                saved.getId(), application.getId());

        //! step 15 - the form follows. A CONSEQUENCE, not a request: #20 names statuses and this
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
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }


    /**
     * Endpoint #29b — correcting the one offer letter this admission has.
     *
     * <p><b>It exists because the one-offer rule opened a hole.</b> A school issues one letter per
     * admission; when that letter lapsed, nothing could extend it and #29 could not issue another,
     * so a family that missed the deadline could not be given a seat by any route. That was a dead
     * end in something already shipped rather than a feature nobody had built — which is why this
     * came before the rest of the plan.
     *
     * <p><b>Extending a lapsed offer works, and that is the point.</b> Nothing writes
     * {@code EXPIRED} — a date in the past is what it means — so a lapsed offer is still stored as
     * {@code ISSUED} and is still an offer this can reach. The design decision that looked like an
     * omission is what makes the fix possible.
     *
     * <p><b>A PATCH, not a verb.</b> The module gives verbs to <i>events</i> — starting, finishing,
     * answering — and correcting a letter is none of those: it is fields being set, which is
     * exactly what #27 is for reviews.
     *
     * <p><b>Only an {@code ISSUED} offer.</b> Once a family has answered, changing the deadline or
     * the grade underneath them rewrites what they agreed to without telling them — and a
     * {@code WITHDRAWN} one is over.
     *
     * <p><b>It does not touch the application, and it cannot set a status.</b> {@code status} and
     * {@code response} belong to #30 and #31; an edit that could set them would be a second way to
     * answer on a family's behalf.
     */
    public AdmissionOfferResponse updateOffer(String admissionOfferId,
            AdmissionOfferUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionOfferId == null ? "" : admissionOfferId.trim();
        log.info("[updateOffer] Step 1: Correcting offer {} for school {}", id, school.getId());

        //! step 2 - the offer, scoped by school in the QUERY.
        AdmissionOffer offer = utils.loadOffer(school, id);

        //! step 3 - is the body carrying anything at all.
        //!
        //! FIRST, BEFORE THE VERSION AND BEFORE THE STATUS — the same order #27 settled on. It is
        //! the only check about the REQUEST rather than about the world, and a body that asks for
        //! nothing is meaningless whatever state the offer is in.
        boolean movesSomething = request.expiresAt() != null
                || request.offeredClassDocsId() != null
                || request.depositInvoiceDocsId() != null;

        if (!movesSomething) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "This request changes nothing. Send an expiresAt, an offeredClassDocsId or a "
                            + "depositInvoiceDocsId.");
        }

        //! step 4 - somebody else may have answered or withdrawn it while this caller was reading.
        if (!request.version().equals(offer.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "Offer " + offer.getOfferNo() + " changed since you read it — it is "
                            + offer.getStatus() + " now. Read it again before correcting it, so "
                            + "you are not editing a letter the family has already answered.");
        }

        //! step 5 - only a letter still out can be corrected.
        //!
        //! A LAPSED ONE IS STILL ISSUED and is therefore editable — which is the whole reason this
        //! endpoint exists. An ANSWERED one is not: changing the deadline or the grade underneath
        //! a family rewrites what they agreed to without telling them.
        if (offer.getStatus() != AdmissionOfferStatus.ISSUED) {
            throw ApiException.conflict("OFFER_NOT_OPEN",
                    "Offer " + offer.getOfferNo() + " is " + offer.getStatus()
                            + ", so there is nothing to correct."
                            + (offer.getStatus() == AdmissionOfferStatus.ACCEPTED
                                    || offer.getStatus() == AdmissionOfferStatus.DECLINED
                                    ? " The family answered on " + offer.getRespondedAt()
                                            + ", and changing the letter under them would rewrite "
                                            + "what they agreed to."
                                    : ""));
        }

        //! step 6 - a new deadline, and it has to be one somebody could still meet. EXTENDING IS
        //! THE COMMON CASE; bringing it forward is allowed, because a school shortening a window
        //! it published is its own business — but not to a moment already gone.
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("OFFER_EXPIRY_IN_THE_PAST",
                    "That would set the offer to expire on " + request.expiresAt()
                            + ", which has already passed. Extending a lapsed offer is what this "
                            + "endpoint is for, and extending it into the past is not an "
                            + "extension.");
        }

        //! step 7 - the round this offer belongs to, READ ONCE FOR BOTH questions below. An
        //! offer does not carry its cycle, so the round comes from the application — and both a
        //! new deadline and a new grade have to be checked against it. Reading it twice would be
        //! two queries for one answer.
        //!
        //! ONLY WHEN SOMETHING NEEDS IT: a correction that moves neither is not made to pay for a
        //! round it never looks at.
        AdmissionCycle cycle = null;

        if (request.expiresAt() != null || request.offeredClassDocsId() != null) {
            // TODO: read admission application
            AdmissionApplication form = applications
                    .findByIdAndSchoolId(offer.getAdmissionApplicationDocsId(), school.getId())
                    .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                            "The application this offer is for is gone, so there is no round to "
                                    + "check it against."));

            cycle = helper.loadCycle(school, form.getAdmissionCycleDocsId());
        }

        //! step 7a - AND THE NEW DEADLINE CANNOT OUTLAST THE YEAR THE SEAT IS IN, exactly as #29
        //! refuses it on the way out. A correction is the easier way to get this wrong: extending
        //! a lapsed offer "by a few months" is the ordinary use of this endpoint, and a few months
        //! from March is the year after next.
        if (request.expiresAt() != null) {
            yearWindow.requireDates(school, cycle.getAcademicYear(), schoolZone.of(school),
                    Bound.NOT_AFTER_THE_END,
                    Refusal.badRequest("OFFER_EXPIRY_OUTSIDE_ACADEMIC_YEAR"),
                    Map.of("expiresAt", request.expiresAt()));
        }

        //! step 7b - a different grade, when the school corrects what it offered. THE SAME TWO
        //! QUESTIONS #29 ASKS, which is why they live in utils.
        SchoolClass offered = null;

        if (request.offeredClassDocsId() != null) {
            offered = utils.offerableClass(school, cycle, request.offeredClassDocsId());
        }

        //! step 8 - the deposit invoice, when one is named. Refuses everything today; see utils.
        String depositInvoiceId = TextHelper.blankToNull(request.depositInvoiceDocsId());
        utils.requireInvoice(school, depositInvoiceId);

        //! step 9 - build the change. ONLY WHAT WAS SENT, so extending a deadline does not clear
        //! the grade somebody corrected yesterday.
        if (request.expiresAt() != null) {
            offer.setExpiresAt(request.expiresAt());
        }
        if (offered != null) {
            offer.setOfferedClassDocsId(offered.getId());
        }
        if (depositInvoiceId != null) {
            offer.setDepositInvoiceDocsId(depositInvoiceId);
        }

        //! NOT offeredAt. The letter was issued when it was issued; a correction is not a reissue,
        //! and moving that date would lose how long the family has actually had it.

        //! step 10 - save
        // TODO: update admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[updateOffer] Step 2: Offer {} corrected, expires {}",
                saved.getId(), saved.getExpiresAt());

        //! step 11 - the form, for the answer.
        // TODO: read admission application
        AdmissionApplication application = applications
                .findByIdAndSchoolId(saved.getAdmissionApplicationDocsId(), school.getId())
                .orElse(null);

        return utils.answerFor(school, saved, application,
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
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
        AdmissionOffer offer = utils.loadOffer(school, id);

        //! step 3 - somebody else may have answered or withdrawn it while this caller was reading.
        if (!request.version().equals(offer.getVersion())) {
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

        //! THE SENTENCE IS BUILT HERE AND HANDED IN, rather than worked out inside answerFor.
        //! Both live in utils, and a method there may not call another there — so the service is
        //! what puts them together.
        return utils.answerFor(school, saved, application,
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
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
        AdmissionOffer offer = utils.loadOffer(school, id);

        //! step 3 - somebody else may have moved it while this caller was reading.
        if (!request.version().equals(offer.getVersion())) {
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

        return utils.answerFor(school, saved, application,
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
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
}
