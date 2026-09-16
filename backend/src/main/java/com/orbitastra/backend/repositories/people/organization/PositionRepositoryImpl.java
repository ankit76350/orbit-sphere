package com.orbitastra.backend.repositories.people.organization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.people.organization.request.PositionSearchRequest;
import com.orbitastra.backend.models.people.organization.Position;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link PositionRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls it.
 */
@RequiredArgsConstructor
public class PositionRepositoryImpl implements PositionRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<Position> search(String schoolId, PositionSearchRequest request,
            Pageable pageable) {

        //! step 1 - the filter: the tenant, then whichever were sent. `vacant` is not among them
        //! and cannot be - see the interface.
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        // TODO: reading positions (how many match)
        long total = mongo.count(new Query(criteria), Position.class);

        //! step 3 - the page, the same filter plus the paging and sorting
        // TODO: reading positions (one page of them)
        List<Position> rows = mongo.find(new Query(criteria).with(pageable), Position.class);

        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public List<Position> searchAll(String schoolId, PositionSearchRequest request, Sort sort) {

        //! The same filter, no paging - the read behind ?vacant=, which has to see the whole
        //! matching set before it can tell which of it is vacant.
        // TODO: reading positions (all of them, to compute vacancy)
        return mongo.find(new Query(buildCriteria(schoolId, request)).with(sort), Position.class);
    }

    private Criteria buildCriteria(String schoolId, PositionSearchRequest request) {

        //! step 1 - the tenant, always, and never from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the owning unit, and the leading field of
        //! school_department_position_active_idx after the school
        if (request.departmentDocsId() != null && !request.departmentDocsId().isBlank()) {
            filters.add(Criteria.where("departmentDocsId").is(request.departmentDocsId().trim()));
        }

        //! step 3 - in use, or retired
        if (request.active() != null) {
            filters.add(Criteria.where("active").is(request.active()));
        }

        //! step 4 - teaching or not. Asked with `ne` on the false side rather than `is(false)`,
        //! because a seat written before the field had a default carries no key at all and is
        //! NOT a teaching position - `is(false)` would drop it from both answers, which is how a
        //! row disappears from a list no matter what you filter by.
        if (request.teaching() != null) {
            filters.add(request.teaching()
                    ? Criteria.where("teachingPosition").is(true)
                    : Criteria.where("teachingPosition").ne(true));
        }

        //! step 5 - the search. A seat is known by its title and nothing else now that
        //! positionCode is gone, so there is one field to match.
        //!
        //! Quoted before it is compiled, so a caller typing "Teacher (Senior)" searches for those
        //! characters rather than injecting a regex group - and a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            filters.add(Criteria.where("title")
                    .regex(Pattern.quote(request.search().trim()), "i"));
        }

        //! step 6 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
