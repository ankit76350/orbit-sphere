package com.orbitastra.backend.repositories.crm.inquiry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import lombok.RequiredArgsConstructor;

/**
 * #13's query and #10's {@code $push}. The query mirrors
 * {@code AdmissionApplicationRepositoryImpl} and {@code AdmissionReviewRepositoryImpl}, which
 * answer the same shape of question; the push mirrors
 * {@code DailyTimetableRepositoryImpl.pushEntry}.
 */
@RequiredArgsConstructor
public class InquiryRepositoryImpl implements InquiryRepositoryCustom {

    /**
     * The statuses a lead is finished in.
     *
     * <p><b>Overdue means "past its date AND still worth chasing"</b>, and these are the two that
     * are not. A lead somebody closed last month has a follow-up date in the past too, and nobody
     * needs ringing about it.
     *
     * <p>{@code APPLICATION_SUBMITTED} is deliberately NOT here. The family sent a form, which is
     * the best possible outcome — but the lead is still live until somebody closes it, and a
     * counsellor who promised to ring them back should still see that promise.
     */
    private static final List<InquiryStatus> FINISHED = List.of(
            InquiryStatus.LOST, InquiryStatus.CLOSED);

    private final MongoTemplate mongo;

    @Override
    public Page<Inquiry> search(String schoolId, InquirySearchRequest request, Pageable pageable) {

        //! step 1 - the filter: the school, then whatever was asked for
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page, the same filter plus paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        // TODO: reading inquiries (how many match)
        long total = mongo.count(countQuery, Inquiry.class);

        // TODO: reading inquiries (one page of them)
        List<Inquiry> rows = mongo.find(pageQuery, Inquiry.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, InquirySearchRequest request) {

        //! step 1 - the school, always, and never from the caller. The tenant boundary.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the two the index leads with, in its order: status, then the counsellor.
        //! school_inquiry_pipeline_idx is {schoolId, status, assignedCounselorDocsId,
        //! nextFollowUpAt}, which is this endpoint's whole reason for existing.
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }
        if (request.assignedCounselorDocsId() != null
                && !request.assignedCounselorDocsId().isBlank()) {
            filters.add(Criteria.where("assignedCounselorDocsId")
                    .is(request.assignedCounselorDocsId().trim()));
        }

        //! step 3 - one intake's leads. A school runs more than one year at a time.
        if (request.academicYear() != null && !request.academicYear().isBlank()) {
            filters.add(Criteria.where("academicYear").is(request.academicYear().trim()));
        }

        //! step 4 - what is late.
        //!
        //! TWO CONDITIONS, NOT ONE, exactly as #28's `overdue` and #32's `expired` are. A past
        //! date is not enough: a lead closed last month has one, and nobody needs chasing about
        //! it. So overdue is "past its date AND not finished", and `overdue=false` is the mirror —
        //! everything not late, including the finished ones and the ones with no date at all.
        //!
        //! A LEAD WITH NO nextFollowUpAt IS NEVER OVERDUE. `$lt` does not match a missing field,
        //! so that falls out of the query rather than needing a rule — and it is the honest
        //! answer: nobody promised to ring them by any particular day.
        if (request.overdue() != null) {
            Instant now = Instant.now();
            Criteria late = new Criteria().andOperator(
                    Criteria.where("nextFollowUpAt").lt(now),
                    Criteria.where("status").nin(FINISHED));

            filters.add(request.overdue() ? late : new Criteria().norOperator(late));
        }

        //! step 5 - the child's name or the inquiry number, anywhere, ignoring case. Two fields
        //! because a desk looks a lead up by either: the parent gives a name on the phone, the
        //! note on the pad carries a number.
        //!
        //! QUOTED, so a caller cannot send a regular expression. Without it "(" is a 500 and ".*"
        //! matches everything — the same lesson #24's search records.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("prospectiveStudentName").regex(needle, "i"),
                    Criteria.where("inquiryNo").regex(needle, "i")));
        }

        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }

    @Override
    public long pushFollowUp(String schoolId, String inquiryId, InquiryFollowUp entry,
            Instant nextFollowUpAt, InquiryStatus status, Long expectedVersion) {

        //! step 1 - the lead, SCOPED BY SCHOOL IN THE QUERY. An id from another school is a real
        //! id, and writing to it without the school would put an entry on another tenant's lead.
        Criteria criteria = Criteria.where("_id").is(inquiryId).and("schoolId").is(schoolId);

        //! step 2 - the version, IN THE QUERY. The service checks it as well, and no sequential
        //! test can tell the two apart — this one is for the race. Two counsellors logging against
        //! one lead in the same instant: the second matches no document and is told so, instead of
        //! both writing and one of them believing they saw the timeline they were adding to.
        if (expectedVersion != null) {
            criteria = criteria.and("version").is(expectedVersion);
        }

        //! step 3 - the entry, and the two fields on the PARENT that a follow-up moves.
        //!
        //! nextFollowUpAt IS ALWAYS SET, INCLUDING TO NULL, which is #10's rule rather than this
        //! method's: the field means "the next call is due at", and once this call has been made
        //! with no new date promised there is no next call due. Leaving the old one would keep
        //! showing a family as overdue on the day somebody rang them.
        Update update = new Update()
                .push("followUps", entry)
                .set("nextFollowUpAt", nextFollowUpAt)
                //! ONLY WHEN IT MOVED. A call that changed nothing must leave the lead alone.
                .set("updatedAt", Instant.now())
                //! BELT AND BRACES, as timetable #3 has it. Spring Data adds its own $inc for a
                //! versioned entity when the update does not carry one — and does not double up
                //! when it does. Explicit anyway: the optimistic check above depends on this
                //! moving, and a future switch to a raw MongoCollection call would lose it with
                //! nothing to notice.
                .inc("version", 1);

        if (status != null) {
            update = update.set("status", status);
        }

        // TODO: update inquiry (append one follow-up)
        return mongo.updateFirst(new Query(criteria), update, Inquiry.class).getModifiedCount();
    }

    @Override
    public long moveStatus(String schoolId, String inquiryId, InquiryStatus status,
            String lostReason, InquiryFollowUp entry, Long expectedVersion) {

        //! step 1 - the lead, SCOPED BY SCHOOL IN THE QUERY, and the version when one was sent.
        //! The same two locks pushFollowUp has, and for the same two reasons: the tenant boundary
        //! belongs in the query, and the version guard is what settles a race the service's own
        //! check cannot see.
        Criteria criteria = Criteria.where("_id").is(inquiryId).and("schoolId").is(schoolId);

        if (expectedVersion != null) {
            criteria = criteria.and("version").is(expectedVersion);
        }

        //! step 2 - the move, the entry that records it, and the end of the chasing.
        //!
        //! nextFollowUpAt IS CLEARED, ALWAYS. #12's moves are the ones that stop a lead being
        //! chased — nobody owes a call to a family that has gone elsewhere — and leaving the date
        //! would keep it on #13's overdue worklist for ever.
        Update update = new Update()
                .set("status", status)
                .push("followUps", entry)
                .unset("nextFollowUpAt")
                .set("updatedAt", Instant.now())
                //! BELT AND BRACES, as in pushFollowUp above. Spring Data adds its own $inc for a
                //! versioned entity and does not double up; explicit because the optimistic check
                //! depends on it moving.
                .inc("version", 1);

        //! ONLY WHEN THERE IS ONE, so a move that is not a loss cannot wipe a reason written
        //! earlier. Not reachable today — LOST is terminal — and cheaper than the bug would be.
        if (lostReason != null) {
            update = update.set("lostReason", lostReason);
        }

        // TODO: update inquiry (move its status)
        return mongo.updateFirst(new Query(criteria), update, Inquiry.class).getModifiedCount();
    }
}
