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

import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import lombok.RequiredArgsConstructor;

/**
 * #13's query. Mirrors {@code AdmissionApplicationRepositoryImpl} and
 * {@code AdmissionReviewRepositoryImpl}, which answer the same shape of question.
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
}
