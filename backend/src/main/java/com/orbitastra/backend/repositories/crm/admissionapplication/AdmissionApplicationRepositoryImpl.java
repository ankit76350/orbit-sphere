package com.orbitastra.backend.repositories.crm.admissionapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
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
}
