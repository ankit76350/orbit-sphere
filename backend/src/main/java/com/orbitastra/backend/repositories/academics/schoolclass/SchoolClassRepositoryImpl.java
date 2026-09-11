package com.orbitastra.backend.repositories.academics.schoolclass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassSearchRequest;
import com.orbitastra.backend.models.academics.structure.SchoolClass;

import lombok.RequiredArgsConstructor;

/**
 * The dynamic class query behind #28.
 *
 * <p><b>Everything happens in the database.</b> Filtering, sorting and paging are all on the
 * query, so one page of documents is read however many classes a year holds. The same
 * arrangement as the plan catalogue, the school list and the two subscription lists.
 *
 * <p>Two round trips: one for the page, one for the total behind {@code totalElements}. The count
 * carries the filter and nothing else — given the page's skip and limit it would only ever count
 * one page — and it is built from the same {@link Criteria} object rather than a second
 * hand-written copy, which is the only way the two cannot drift apart.
 *
 * <h2>The index behind it</h2>
 *
 * <p><b>No index was added for #28</b>, and {@code explain} says what the existing ones do:
 *
 * <pre>
 * bare list            IXSCAN school_year_class_name_uniq          + SORT
 * ?active=true         IXSCAN school_year_class_active_order_idx   + SORT
 * ?search=grade        IXSCAN school_year_class_name_uniq          + SORT   (4 returned, 11 keys)
 * ?hasSections=false   IXSCAN school_year_class_name_uniq          + SORT
 * ?sort=name           IXSCAN school_year_class_name_uniq          + SORT
 * </pre>
 *
 * <p><b>Every filter is an index scan and every sort is a blocking one.</b> The scan is never a
 * COLLSCAN because {@code schoolId} and {@code academicYear} are always pinned, and both existing
 * indexes begin with that pair. The sort cannot come from an index because the order ends in the
 * {@code _id} tiebreaker, which neither index carries — see the note on {@code CLASS_ORDER} in
 * the service for why keeping it is the right trade on tens of documents.
 *
 * <p>{@code search} and {@code affiliationProgrammeDocsId} have nothing behind them and do not
 * want anything: both are applied <i>after</i> the query is pinned to one school and one year, so
 * they filter tens of documents. A case-insensitive contains regex cannot use an index in any
 * case. See the request DTO.
 */
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<SchoolClass> search(String schoolId, String academicYear,
            SchoolClassSearchRequest request, Pageable pageable) {

        //! step 1 - build the filter: the tenant and the year, then whichever were sent
        Criteria criteria = buildCriteria(schoolId, academicYear, request);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading classes (how many match)
        long total = mongo.count(countQuery, SchoolClass.class);

        //! step 5 - run the page. Only these rows are read, however many classes the year has.
        // TODO: reading classes (one page of them)
        List<SchoolClass> rows = mongo.find(pageQuery, SchoolClass.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * #28's filters, combined with AND. An absent one adds nothing.
     *
     * <p>The tenant and the year go in first and unconditionally, so the list is never wider than
     * one year of one school whatever else the caller sent. Everything after that narrows.
     *
     * <p><b>The {@code $and} list always has at least two entries</b>, so unlike #30's
     * cross-school query there is no empty-{@code $and} case to guard — Mongo rejects
     * {@code {$and: []}} outright.
     */
    private Criteria buildCriteria(String schoolId, String academicYear,
            SchoolClassSearchRequest request) {

        //! step 1 - the tenant and the parent key, always, and never from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        //! step 2 - the headline filter, and the one the index covers
        if (request.active() != null) {
            filters.add(Criteria.where("active").is(request.active()));
        }

        //! step 3 - the name search. Quoted before it is compiled, so a caller typing "Grade
        //! (7)" searches for those characters rather than injecting a regex group — and so that
        //! a stray "(" is an empty result instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            filters.add(Criteria.where("name")
                    .regex(Pattern.quote(request.search().trim()), "i"));
        }

        //! step 4 - one board programme
        if (request.affiliationProgrammeDocsId() != null
                && !request.affiliationProgrammeDocsId().isBlank()) {

            filters.add(Criteria.where("affiliationProgrammeDocsId")
                    .is(request.affiliationProgrammeDocsId().trim()));
        }

        //! step 5 - the two setup filters. Asked of the array's FIRST ELEMENT rather than a
        //! stored count: `sections.0` exists only when the list has something in it, so there is
        //! no second field to keep in step with the list. `$size: 0` would also work for the
        //! empty case but not for the non-empty one without a $expr.
        //!
        //! A document written before either field existed has no array at all, which correctly
        //! reads as "no sections" here.
        if (request.hasSections() != null) {
            filters.add(Criteria.where("sections.0").exists(request.hasSections()));
        }
        if (request.hasSubjects() != null) {
            filters.add(Criteria.where("subjects.0").exists(request.hasSubjects()));
        }

        //! step 6 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
