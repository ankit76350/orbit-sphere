package com.orbitastra.backend.repositories.crm.admissionapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionApplication;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link AdmissionApplicationRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data finds a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application still compiles and still starts — it fails only when somebody calls {@code search}.
 */
@RequiredArgsConstructor
public class AdmissionApplicationRepositoryImpl implements AdmissionApplicationRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<AdmissionApplication> search(String schoolId,
            AdmissionApplicationSearchRequest request, Pageable pageable) {

        //! step 1 - the filter: the school, then whatever was asked for
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page, the same filter plus paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        // TODO: reading admission applications (how many match)
        long total = mongo.count(countQuery, AdmissionApplication.class);

        // TODO: reading admission applications (one page of them)
        List<AdmissionApplication> rows = mongo.find(pageQuery, AdmissionApplication.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, AdmissionApplicationSearchRequest request) {

        //! step 1 - the school, always, and never from the caller. The tenant boundary.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the three the index is built for, in its order: cycle, class, status.
        //! school_cycle_class_status_idx exists for exactly this worklist.
        if (request.admissionCycleDocsId() != null && !request.admissionCycleDocsId().isBlank()) {
            filters.add(Criteria.where("admissionCycleDocsId")
                    .is(request.admissionCycleDocsId().trim()));
        }
        if (request.appliedClassDocsId() != null && !request.appliedClassDocsId().isBlank()) {
            filters.add(Criteria.where("appliedClassDocsId")
                    .is(request.appliedClassDocsId().trim()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        //! step 3 - whose worklist. Nothing assigns an officer yet (#22 is not built), so this
        //! returns nothing for any id until it is - which is the truth, not a bug.
        if (request.assignedAdmissionOfficerDocsId() != null
                && !request.assignedAdmissionOfficerDocsId().isBlank()) {
            filters.add(Criteria.where("assignedAdmissionOfficerDocsId")
                    .is(request.assignedAdmissionOfficerDocsId().trim()));
        }

        //! step 4 - came from a lead, or walked in.
        //! $type rather than $ne null: a MISSING field and a field holding null both have to read
        //! as "no inquiry", and $ne null does not exclude a missing one.
        if (request.fromInquiry() != null) {
            filters.add(request.fromInquiry()
                    ? Criteria.where("inquiryDocsId").type(2)
                    : new Criteria().orOperator(
                            Criteria.where("inquiryDocsId").exists(false),
                            Criteria.where("inquiryDocsId").is(null)));
        }

        //! step 5 - the search, across the name AND the number. A school looks a child up by
        //! either: a parent gives a name on the phone, the file carries a number.
        //!
        //! QUOTED BEFORE IT IS COMPILED, so "APP/2026/09" searches for those characters rather
        //! than being read as a pattern - and a stray "(" is an empty result, not a 500.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("applicantName").regex(needle, "i"),
                    Criteria.where("applicationNo").regex(needle, "i")));
        }

        //! step 6 - AND them. One filter is still an andOperator of one.
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }

    /**
     * #7's counts, in one grouped aggregation.
     *
     * <p><b>The only aggregation in this module.</b> Everything else is a find or a count, because
     * everything else answers a question about rows rather than about totals — and a summary of
     * twenty classes is exactly the shape that becomes twenty queries if it is written the obvious
     * way.
     */
    @Override
    public List<ClassStatusCount> countByClassAndStatus(String schoolId,
            String admissionCycleDocsId) {

        //! THE SCHOOL IS IN THE MATCH, always, and never from the caller. The tenant boundary —
        //! and a count is a read like any other: totals leak as surely as rows do.
        //!
        //! MUTATION CANNOT TELL THIS FROM MATCHING ON THE CYCLE ALONE — proven 2026-09-24, and
        //! the scope stays anyway. A cycle id is a globally unique ObjectId, and #7 loads the
        //! cycle through CrmHelper.loadCycle first, which IS school-scoped — so a foreign id is a
        //! 404 before this runs, and a real one only ever names this school's applications. The
        //! line is defence in depth rather than a reachable boundary, which is exactly the kind
        //! that rots quietly when somebody "simplifies" it. This comment is why it is still here.
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("admissionCycleDocsId").is(admissionCycleDocsId)),
                Aggregation.group("appliedClassDocsId", "status").count().as("count"),
                Aggregation.project("count")
                        .and("_id.appliedClassDocsId").as("classDocsId")
                        .and("_id.status").as("status"));

        // TODO: read admission applications (grouped counts)
        AggregationResults<ClassStatusCount> results =
                mongo.aggregate(aggregation, AdmissionApplication.class, ClassStatusCount.class);

        return results.getMappedResults();
    }
}
