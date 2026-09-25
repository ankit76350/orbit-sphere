package com.orbitastra.backend.repositories.crm.inquiry;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.inquiry.request.InquiryMatchRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

/**
 * The parts of #13 and #10 Spring Data cannot derive from a method name.
 *
 * <p>#13's five optional filters, its two-condition "overdue" question and its two-field search do
 * not fit a derived query. #10 needs a {@code $push} rather than a save, which no derived method
 * can express at all. Both are built by hand in the implementation beside this.
 */
public interface InquiryRepositoryCustom {

    /**
     * One page of leads.
     *
     * <p><b>{@code schoolId} is a parameter, never a field on the request.</b> The caller says what
     * to filter; the tenant is added by the service from the resolved school.
     */
    Page<Inquiry> search(String schoolId, InquirySearchRequest request, Pageable pageable);

    /**
     * Endpoint #10's write — <b>one entry appended, in one atomic update</b>.
     *
     * <p><b>A {@code $push}, never a re-save.</b> Reading a lead, adding to its list and saving the
     * whole document back would overwrite every entry anybody else logged in between — and a
     * timeline is exactly the kind of list two counsellors write to at once. The same call
     * {@code timetable} #3 makes, for the same reason.
     *
     * <p><b>The chase date is rewritten every time, to whatever is passed</b>, including
     * {@code null}. That is #10's rule rather than this method's: see the endpoint. Passing it
     * here rather than reading it off the entry keeps the two independent — the entry records what
     * was <i>promised</i>, the parent carries what is <i>due</i>.
     *
     * <p><b>{@code status} is only written when it is not null</b>, because a call that moved
     * nothing must leave the lead where it is.
     *
     * <p><b>The version guards the QUERY, not just a check before it.</b> The service checks it too
     * and no sequential test can tell the two apart — this one is for the race: two counsellors
     * logging against one lead in the same instant, where the second matches no document and is
     * told so.
     *
     * @return how many documents moved — {@code 0} means the lead is gone, or somebody else got
     *         there first, and the caller is what tells those apart
     */
    long pushFollowUp(String schoolId, String inquiryId, InquiryFollowUp entry,
            Instant nextFollowUpAt, InquiryStatus status, Long expectedVersion);

    /**
     * Endpoint #12's write — <b>the move, its reason and its timeline entry, in one update</b>.
     *
     * <p><b>The same {@code $push} as #10's, with two differences.</b> The status always moves —
     * that is the whole request — and {@code lostReason} is written beside it, so a lead never
     * exists in a state where it is {@code LOST} and nobody can say why.
     *
     * <p><b>{@code nextFollowUpAt} is cleared, always.</b> #12's moves are the ones that end the
     * chasing: nobody owes a call to a family that has gone elsewhere, and a lead whose file has
     * been closed is not waiting for one either. Leaving the date would keep it on #13's overdue
     * worklist for ever.
     *
     * <p><b>{@code lostReason} is only set when there is one</b>, so a move to {@code CLOSED} does
     * not clear a reason written earlier — not that it can happen today, since {@code LOST} is
     * terminal.
     *
     * @return how many documents moved — {@code 0} means the lead is gone, or somebody else got
     *         there first, and the caller is what tells those apart
     */
    long moveStatus(String schoolId, String inquiryId, InquiryStatus status, String lostReason,
            InquiryFollowUp entry, Long expectedVersion);

    /**
     * Endpoint #15's query — <b>is this family already known?</b>
     *
     * <p><b>The only query in this module that reaches into an embedded array to match.</b>
     * Everything else filters on a top-level field; this asks whether any guardian on any lead
     * carries this phone number or this address.
     *
     * <p><b>An OR, not an AND.</b> A family that left a phone number last year and an email this
     * year is the same family, and requiring both would miss exactly the case worth catching.
     *
     * <p><b>It does NOT use {@code elemMatch}</b>, and that is deliberate rather than an
     * oversight. {@code elemMatch} asks whether ONE guardian satisfies every condition; the
     * question here is whether ANY guardian matches EITHER — a mother's phone and a father's email
     * on one lead is still that family. Dotted paths match across the array, which is the
     * behaviour wanted.
     *
     * @param phoneDigits the digits to match on — the query's last ten when it gave ten or more,
     *                    otherwise all of them. The service decides which; see it for why
     * @param wholeNumber whether the stored number's digits must be exactly these, rather than
     *                    merely ending with them. True for a short query, which would otherwise
     *                    match the tail of every long number
     * @param email       the address, trimmed, or {@code null}. <b>Not lowercased</b> — the
     *                    {@code i} flag on the query makes the case of both sides irrelevant, and
     *                    lowercasing as well was a second mechanism doing one job
     * @param limit       how many to return at most — see the service for why there is a cap
     */
    List<Inquiry> findFamily(String schoolId, String phoneDigits, boolean wholeNumber,
            String email, int limit);
}
