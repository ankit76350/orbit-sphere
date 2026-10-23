package com.orbitastra.backend.repositories.academics.gradingscheme;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeSearchRequest;
import com.orbitastra.backend.models.academics.grading.GradingScheme;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link GradingSchemeRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls {@code search} — the warning
 * this module's plan carries from the plans module.
 */
@RequiredArgsConstructor
public class GradingSchemeRepositoryImpl implements GradingSchemeRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<GradingScheme> search(String schoolId, GradingSchemeSearchRequest request,
            Pageable pageable) {

        //! step 1 - build the filter: the tenant, then whichever filters were sent
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading grading schemes (how many match)
        long total = mongo.count(countQuery, GradingScheme.class);

        //! step 5 - run the page
        // TODO: reading grading schemes (one page of them)
        List<GradingScheme> rows = mongo.find(pageQuery, GradingScheme.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, GradingSchemeSearchRequest request) {

        //! step 1 - the tenant, always, and never from the caller. There is no second boundary
        //! key here: a scheme belongs to the school rather than to one of its years, which is
        //! what makes this filter one line where the term equivalent is two.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - offered for new work, or retired. Absent returns BOTH, which is not the same
        //! as false: a retired scheme still resolves every report card that used it.
        if (request.active() != null) {
            filters.add(Criteria.where("active").is(request.active()));
        }

        //! step 3 - the scale. The one filter that answers a real question: "what can I grade an
        //! exam out of 100 with" excludes DESCRIPTOR, which cannot be resolved by value at all.
        if (request.scaleType() != null) {
            filters.add(Criteria.where("scaleType").is(request.scaleType()));
        }

        //! step 4 - the search, across `name` and nothing else. A term is searched by name OR
        //! code because it has both; a scheme has no code, which is the visible cost of keying
        //! on the name.
        //!
        //! Quoted before it is compiled, so a caller typing "CBSE (2026)" searches for those
        //! characters rather than injecting a regex group - and so a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(Criteria.where("name").regex(needle, "i"));
        }

        //! step 5 - AND them. One filter is still an andOperator of one, which is what keeps this
        //! from needing a branch for "the caller sent nothing".
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
