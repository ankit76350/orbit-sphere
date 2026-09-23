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
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
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
     * <p><b>{@code OFFERED} is here because SUPERSEDING NEEDS IT.</b> The plan said "approved or
     * waitlisted" and also "a later one supersedes the last" — and those two cannot both be true,
     * because the first offer moves the form to {@code OFFERED} and a second would then be refused.
     * Building it is what found that; the plan is corrected in the README rather than quietly.
     *
     * <p><b>{@code OFFER_ACCEPTED} is deliberately absent.</b> The family has said yes to something
     * specific, and issuing a new revision on top would rewrite what they agreed to without
     * telling them. That is a withdrawal (#31) followed by a new offer, which leaves both in the
     * record.
     */
    private static final Set<AdmissionApplicationStatus> OFFERABLE = EnumSet.of(
            AdmissionApplicationStatus.APPROVED,
            AdmissionApplicationStatus.WAITLISTED,
            AdmissionApplicationStatus.OFFERED);

    /**
     * The offer statuses a new revision pushes aside.
     *
     * <p>Only the ones still <i>live</i>. A {@code DECLINED}, {@code EXPIRED}, {@code WITHDRAWN} or
     * already-{@code SUPERSEDED} revision is finished, and marking it superseded a second time
     * would overwrite what actually happened to it with a tidier story.
     *
     * <p>{@code ACCEPTED} is not here either — an accepted offer is unreachable from this endpoint,
     * because its application is {@code OFFER_ACCEPTED} and that is not {@code OFFERABLE}.
     */
    private static final Set<AdmissionOfferStatus> STILL_LIVE = EnumSet.of(
            AdmissionOfferStatus.DRAFT,
            AdmissionOfferStatus.ISSUED);

    private final AdmissionOfferRepository admissionOffers;
    private final AdmissionApplicationRepository applications;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;

    /**
     * Endpoint #29 — the school offers a seat.
     *
     * <p><b>A later offer supersedes the last</b>, and every revision is kept. "What did we
     * originally offer this family" is a question schools get asked, and an endpoint that
     * overwrote the previous row could not answer it.
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
            throw ApiException.conflict("APPLICATION_NOT_APPROVED",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so there is no seat to offer. #20 is what approves or waitlists a "
                            + "form, and an offer follows that decision rather than making it."
                            + (application.getStatus() == AdmissionApplicationStatus.OFFER_ACCEPTED
                                    ? " This family has already accepted an offer — changing it "
                                            + "means withdrawing that one (#31) and issuing "
                                            + "another, so both stay in the record."
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

        //! step 8 - when it runs out. The caller's date, or the round's published deadline —
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

        //! step 9 - what this application has been offered before. ONE READ ANSWERING BOTH
        //! QUESTIONS: the highest revision so far, and which rows a new one pushes aside.
        // TODO: read admission offers
        List<AdmissionOffer> existing = admissionOffers
                .findBySchoolIdAndAdmissionApplicationDocsIdOrderByRevisionNoAsc(
                        school.getId(), application.getId());

        int nextRevision = existing.stream()
                .map(AdmissionOffer::getRevisionNo)
                .filter(each -> each != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        //! step 10 - the number. Generated, never supplied: nobody picks their own offer number.
        String offerNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_OFFER, "OFFER/{YYYY}/{MM}/");
        log.info("[issueOffer] Step 2: Allocated offer number {} as revision {}",
                offerNo, nextRevision);

        //! step 11 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        //!
        //! ISSUED, NOT DRAFT. Issuing is the endpoint, so offeredAt is stamped here. DRAFT is on
        //! the enum and no endpoint writes it — the same honest gap as EXPIRED, which is what a
        //! date in the past MEANS rather than a call anybody makes.
        AdmissionOffer offer = AdmissionOffer.builder()
                .schoolId(school.getId())
                .offerNo(offerNo)
                .revisionNo(nextRevision)
                .admissionApplicationDocsId(application.getId())
                .offeredClassDocsId(offered.getId())
                .status(AdmissionOfferStatus.ISSUED)
                .offeredAt(Instant.now())
                .expiresAt(expiresAt)
                .depositInvoiceDocsId(TextHelper.blankToNull(request.depositInvoiceDocsId()))
                .issuedByDocsId(issuedById)
                .build();

        //! step 12 - save the offer
        // TODO: insert admission offer
        AdmissionOffer saved = admissionOffers.save(offer);
        log.info("[issueOffer] Step 3: Offer {} issued for application {}",
                saved.getId(), application.getId());

        //! step 13 - and the ones it replaces. AFTER the insert, so a failure to allocate a number
        //! or write the row leaves the previous offer standing rather than superseded by nothing.
        List<AdmissionOffer> pushedAside = existing.stream()
                .filter(one -> STILL_LIVE.contains(one.getStatus()))
                .toList();

        if (!pushedAside.isEmpty()) {
            pushedAside.forEach(one -> one.setStatus(AdmissionOfferStatus.SUPERSEDED));

            // TODO: update admission offers
            admissionOffers.saveAll(pushedAside);
            log.info("[issueOffer] Step 4: Superseded {} earlier offer(s)", pushedAside.size());
        }

        //! step 14 - the form follows. A CONSEQUENCE, not a request: #20 names statuses and this
        //! one does not, because moving to OFFERED is what issuing an offer MEANS.
        if (application.getStatus() != AdmissionApplicationStatus.OFFERED) {
            AdmissionApplicationStatus from = application.getStatus();
            application.setStatus(AdmissionApplicationStatus.OFFERED);

            // TODO: update admission application
            applications.save(application);
            log.info("[issueOffer] Step 5: Application {} moved {} -> OFFERED",
                    application.getId(), from);
        }

        return AdmissionOfferResponse.fromOffer(saved, application.getApplicationNo(),
                application.getApplicantName(), offered.getName(),
                issuedBy == null ? null : issuedBy.getFullName(),
                existing.size() + 1,
                nextStepFor(saved, pushedAside.size()) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * What happens to this offer next, in plain words.
     *
     * <p>Private and inline: one caller, and the folder rules keep single-use logic where it is
     * used. It moves to {@code utils} when #30 also answers with it.
     */
    private static String nextStepFor(AdmissionOffer offer, int superseded) {
        String supersedeNote = superseded == 0 ? ""
                : " The " + (superseded == 1 ? "offer" : superseded + " offers")
                        + " before it " + (superseded == 1 ? "is" : "are")
                        + " SUPERSEDED and stay in the record, so what was first offered is still "
                        + "answerable.";

        return "The family answers with #30, which is not built — so this offer cannot move past "
                + "ISSUED yet." + supersedeNote
                + (offer.getExpiresAt() == null
                        ? " It has no expiry date, because the round has no enrollment deadline "
                                + "and none was sent: nothing will ever make it lapse."
                        : " It lapses on " + offer.getExpiresAt() + ", which is what EXPIRED "
                                + "means — a date in the past, not a call anybody makes.");
    }
}
