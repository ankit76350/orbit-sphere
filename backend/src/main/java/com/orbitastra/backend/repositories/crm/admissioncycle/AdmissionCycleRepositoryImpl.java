package com.orbitastra.backend.repositories.crm.admissioncycle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionCycle;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link AdmissionCycleRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data finds a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application still compiles and still starts — it fails only when somebody calls
 * {@code search}.
 */
@RequiredArgsConstructor
public class AdmissionCycleRepositoryImpl implements AdmissionCycleRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<AdmissionCycle> search(String schoolId, AdmissionCycleSearchRequest request,
            Pageable pageable) {

        //! step 1 - build the filter: the school, then whichever filters were sent
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - how many match in total, for the page count
        // TODO: reading admission cycles (how many match)
        long total = mongo.count(countQuery, AdmissionCycle.class);

        //! step 5 - the page itself
        // TODO: reading admission cycles (one page of them)
        List<AdmissionCycle> rows = mongo.find(pageQuery, AdmissionCycle.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, AdmissionCycleSearchRequest request) {

        //! step 1 - the school, always, and never from the caller. This is the tenant boundary.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - one year, or every year. Leaving it out is the normal case here, unlike in
        //! academics: a school works on two years at once during admissions, so "show me both"
        //! has to be the default rather than something you ask for.
        if (request.academicYear() != null && !request.academicYear().isBlank()) {
            filters.add(Criteria.where("academicYear").is(request.academicYear().trim()));
        }

        //! step 3 - one status. DRAFT to find the rounds nobody has opened yet, OPEN to find the
        //! ones taking applications right now.
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        //! step 4 - the search, across `name` and nothing else. A cycle has no code, so the name
        //! is all there is to match on.
        //!
        //! QUOTED BEFORE IT IS COMPILED, so a caller typing "Main (2026)" searches for those
        //! characters rather than injecting a regex group - and a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(Criteria.where("name").regex(needle, "i"));
        }

        //! step 5 - the four dates, as "which rounds were taking applications on this day". Both
        //! ends are optional and either can be sent alone.
        //!
        //! A MISSING DATE ON THE DOCUMENT DOES NOT MATCH. A cycle with no applicationCloseAt is
        //! not "open forever" - it is a cycle whose calendar was never filled in, and answering
        //! a date question with it would be inventing the answer.
        if (request.openOn() != null) {
            filters.add(Criteria.where("applicationOpenAt").lte(request.openOn()));
            filters.add(Criteria.where("applicationCloseAt").gte(request.openOn()));
        }

        //! step 6 - AND them. One filter is still an andOperator of one, which is what keeps this
        //! from needing a branch for "the caller sent nothing".
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
