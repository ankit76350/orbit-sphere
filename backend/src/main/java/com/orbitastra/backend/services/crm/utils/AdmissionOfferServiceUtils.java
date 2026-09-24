package com.orbitastra.backend.services.crm.utils;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.crm.admissionoffer.response.AdmissionOfferResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.finance.billing.FeeInvoice;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
import com.orbitastra.backend.repositories.finance.feeinvoice.FeeInvoiceRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * What {@link com.orbitastra.backend.services.crm.AdmissionOfferService} does more than once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>This file did not exist while the service had one endpoint</b>, and that was right —
 * nothing in a single method can repeat. #30, #31 and #32 gave it four, and #30 and #31 share three
 * things: the guard they both start with, the answer they both end with, and the sentence that
 * answer carries.
 *
 * <p><b>{@code answerFor} does NOT call {@code nextStepFor}, and it easily could have.</b> They
 * were one method in the service and the obvious move was to bring the call across with them — but
 * that is a utils method calling a utils method, which is the one thing this folder forbids. The
 * service composes them instead: it builds the sentence and hands it in. Two arguments in place of
 * a hidden dependency.
 */
@Component
@RequiredArgsConstructor
public class AdmissionOfferServiceUtils {

    private final AdmissionOfferRepository admissionOffers;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final FeeInvoiceRepository feeInvoices;

    /**
     * One offer of this school, or a 404.
     *
     * <p><b>Scoped by school in the QUERY, never by id alone.</b> An id from another school is a
     * real id: answering or withdrawing against it would record this school's family accepting
     * another school's seat.
     *
     * <p><b>It THROWS, unlike the two tolerant reads below.</b> The offer is the thing being
     * changed; the names are decoration on the answer.
     *
     * Used by:
     * - respond()
     * - withdraw()
     */
    public AdmissionOffer loadOffer(School school, String admissionOfferId) {
        String id = admissionOfferId == null ? "" : admissionOfferId.trim();

        // TODO: read admission offer
        return admissionOffers.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("OFFER_NOT_FOUND",
                        "No admission offer with id '" + id + "' in this school."));
    }

    /**
     * The whole offer, with the names its ids stand for.
     *
     * <p>The tail #30 and #31 share. The class and the issuer are read here rather than carried
     * from the write, because neither endpoint has them in hand the way #29 does — #29 reads both
     * to refuse a bad id and has the documents already.
     *
     * <p><b>{@code nextStep} is a PARAMETER, not something this works out.</b> Building it is
     * {@code nextStepFor} below, and a method here calling another here is what the folder rules
     * forbid — so the service calls both and passes the sentence in.
     *
     * Used by:
     * - respond()
     * - withdraw()
     */
    public AdmissionOfferResponse answerFor(School school, AdmissionOffer offer,
            AdmissionApplication application, String nextStep) {

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
                offeredClassName, issuedByName, nextStep);
    }

    /**
     * The class a round may offer a seat in, or a refusal.
     *
     * <p><b>Two questions, and both have to be asked.</b> Does the class exist in the <i>cycle's</i>
     * academic year — which is the only year that round admits into — and does the round have seats
     * set up for it. The second is #17's rule applied to the offered class: a class that is not in
     * the seat table is one this round is not admitting into, so a seat in it is not the school's
     * to offer.
     *
     * <p><b>It is NOT a count.</b> Over-offering is deliberate — sixty offers for forty places,
     * because a fifth of families go elsewhere — so nothing here compares the number of offers to
     * {@code totalSeats}. That is #7's job.
     *
     * Used by:
     * - issueOffer()
     * - updateOffer()
     */
    public SchoolClass offerableClass(School school, AdmissionCycle cycle, String classDocsId) {
        String classId = classDocsId == null ? "" : classDocsId.trim();

        // TODO: read school class
        SchoolClass offered = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(),
                        cycle.getAcademicYear())
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "No class with id '" + classId + "' in '" + cycle.getAcademicYear()
                                + "', which is the year '" + cycle.getName() + "' admits into."));

        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();

        if (seats.stream().noneMatch(seat -> classId.equals(seat.getClassDocsId()))) {
            throw ApiException.conflict("CLASS_NOT_IN_CAPACITY",
                    "'" + cycle.getName() + "' has no seats set up for " + offered.getName()
                            + ", so there is none to offer. Add it to the seat table with #4 "
                            + "first.");
        }

        return offered;
    }

    /**
     * Refuses a deposit invoice id that is not this school's.
     *
     * <p><b>An id nothing verifies is an id that can be anything</b>, and {@code "13212313"} was
     * stored happily until this check existed.
     *
     * <p><b>It refuses everything today</b>, which is the honest state rather than a bug: nothing
     * writes {@code fee_invoices} — the finance module has models and no service — so there is no
     * real id to send.
     *
     * <p><b>Returns nothing.</b> Neither caller wants the invoice; they want to know it is there.
     *
     * Used by:
     * - issueOffer()
     * - updateOffer()
     */
    public void requireInvoice(School school, String depositInvoiceDocsId) {
        if (depositInvoiceDocsId == null) {
            return;
        }

        // TODO: read fee invoice
        FeeInvoice deposit = feeInvoices
                .findByIdAndSchoolId(depositInvoiceDocsId, school.getId())
                .orElse(null);

        if (deposit == null) {
            throw ApiException.notFound("FEE_INVOICE_NOT_FOUND",
                    "No fee invoice with id '" + depositInvoiceDocsId + "' in this school, so the "
                            + "offer cannot point at it. Nothing writes fee_invoices yet — the "
                            + "finance module has models and no service — so there is no id this "
                            + "will accept today. Leave the field out.");
        }
    }

    /**
     * What happens to this offer next, in plain words.
     *
     * <p><b>It reads nothing.</b> Every other method in this file is a query; this one is a
     * {@code switch} over seven statuses. It is here because three endpoints answer with it, not
     * because it is a lookup — the folder rule counts callers, not queries.
     *
     * Used by:
     * - issueOffer()
     * - respond()
     * - withdraw()
     */
    public String nextStepFor(AdmissionOffer offer) {
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
            //! NONE OF THE THREE IS REACHABLE. DRAFT has no endpoint that writes it, EXPIRED is
            //! what a date means rather than a status anything sets, and SUPERSEDED stopped being
            //! written when the one-offer rule replaced revisions. All three are answered rather
            //! than left to fall through, because a switch that cannot fail is one fewer thing to
            //! get wrong.
            case DRAFT -> "Nothing writes DRAFT — #29 issues directly. If you are reading this, "
                    + "something wrote it straight to the database.";
            case EXPIRED -> "Nothing writes EXPIRED either: it is what a past expiresAt MEANS, and "
                    + "#32 is how a school finds the offers it applies to.";
            case SUPERSEDED -> "Nothing writes SUPERSEDED any more. It belonged to the revision "
                    + "model that one-offer-per-admission replaced on 2026-09-23.";
        };
    }
}
