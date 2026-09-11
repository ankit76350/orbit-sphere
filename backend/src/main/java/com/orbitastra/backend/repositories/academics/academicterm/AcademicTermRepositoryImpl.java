package com.orbitastra.backend.repositories.academics.academicterm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermSearchRequest;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link AcademicTermRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls {@code search} — which is the
 * warning the module plan carries from the plans module.
 */
@RequiredArgsConstructor
public class AcademicTermRepositoryImpl implements AcademicTermRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<AcademicTerm> search(String schoolId, String academicYear,
            AcademicTermSearchRequest request, Pageable pageable) {

        //! step 1 - build the filter: the tenant and the year, then whichever were sent
        Criteria criteria = buildCriteria(schoolId, academicYear, request);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading terms (how many match)
        long total = mongo.count(countQuery, AcademicTerm.class);

        //! step 5 - run the page
        // TODO: reading terms (one page of them)
        List<AcademicTerm> rows = mongo.find(pageQuery, AcademicTerm.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, String academicYear,
            AcademicTermSearchRequest request) {

        //! step 1 - the tenant and the parent key, always, and never from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        //! step 2 - the headline filter, and the one school_year_term_active_dates_idx covers
        if (request.active() != null) {
            filters.add(Criteria.where("active").is(request.active()));
        }

        //! step 3 - the search, across BOTH things a term is known by. A person looking for a
        //! term types either its name or its code, and which one they remember is not something
        //! this endpoint gets to decide.
        //!
        //! Quoted before it is compiled, so a caller typing "Term (1)" searches for those
        //! characters rather than injecting a regex group - and so a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(needle, "i"),
                    Criteria.where("termCode").regex(needle, "i")));
        }

        //! step 4 - the frozen ones, which is what #5 and #6 write
        if (request.resultsLocked() != null) {
            filters.add(Criteria.where("resultsLocked").is(request.resultsLocked()));
        }

        //! step 5 - weighted or not. Asked with `exists` rather than `ne: null`, because a term
        //! written before the field existed has no key at all and must read as unweighted -
        //! the same reason #28 asks `sections.0` rather than a stored count.
        if (request.weighted() != null) {
            filters.add(Criteria.where("weightPercent").exists(request.weighted()));
        }

        //! step 6 - the term covering one date. Both ends inclusive, because that is what every
        //! date range in this system means: a term ending 30 September includes the 30th.
        if (request.coversDate() != null) {
            filters.add(Criteria.where("startDate").lte(request.coversDate()));
            filters.add(Criteria.where("endDate").gte(request.coversDate()));
        }

        //! step 7 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
