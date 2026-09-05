package com.orbitastra.backend.repositories.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.common.mongo.CriteriaText;
import com.orbitastra.backend.dto.core.platform.SchoolSearchRequest;
import com.orbitastra.backend.models.core.School;

import lombok.RequiredArgsConstructor;

/**
 * The dynamic school query behind G1.
 *
 * <p><b>Everything happens in the database.</b> Filtering, searching, sorting and paging are all
 * on the query, and only one page of documents is ever read. Loading the collection and filtering
 * in Java would work on a demo and fall over on the first real operator with a thousand tenants.
 *
 * <p>Two round trips per request: one for the page, one for the total. That is the price of
 * reporting {@code totalElements}, and it is why the count query is built from the same criteria
 * rather than a second, hand-written copy that could drift out of agreement with it.
 *
 * <p>The text matching lives in {@link CriteriaText}, shared with the plan list. Escaping
 * caller input before it reaches a regex is a security rule, and a second copy of it is a second
 * thing to remember to fix.
 */
@RequiredArgsConstructor
public class SchoolRepositoryImpl implements SchoolRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<School> search(SchoolSearchRequest request, Pageable pageable) {
        //! step 1 - build the filter from whichever parameters were sent
        Criteria criteria = buildCriteria(request);

        //! step 2 - build the count query. It carries the filter and NOTHING else: given the
        //! page's skip and limit it would only ever count one page.
        Query countQuery = new Query(criteria);

        //! step 3 - build the page query, which is the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading schools (how many match)
        long total = mongo.count(countQuery, School.class);

        //! step 5 - run the page. Only these rows are ever read, however many tenants exist.
        // TODO: reading schools (one page of them)
        List<School> rows = mongo.find(pageQuery, School.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * The filters, combined with AND. Absent ones add nothing.
     *
     * <p>An empty criteria list means "everything", which is the right answer to a request that
     * asked for no filters at all.
     */
    private Criteria buildCriteria(SchoolSearchRequest request) {
        //! step 1 - start with nothing, and add only the filters that were actually sent
        List<Criteria> filters = new ArrayList<>();

        //! step 2 - status, which is the one field that ORs within itself
        if (request.statuses() != null && !request.statuses().isEmpty()) {
            // OR within the field: "show me the live ones" is one question, not two requests.
            filters.add(Criteria.where("status").in(request.statuses()));
        }
        //! step 3 - where the school is, matched exactly but ignoring case
        if (hasText(request.countryCode())) {
            filters.add(CriteriaText.exactIgnoreCase("countryCode", request.countryCode().trim()));
        }
        if (hasText(request.city())) {
            filters.add(CriteriaText.exactIgnoreCase("city", request.city().trim()));
        }

        //! step 4 - when it was created, either end optional
        if (request.createdFrom() != null || request.createdTo() != null) {
            Criteria created = Criteria.where("createdAt");
            if (request.createdFrom() != null) {
                created = created.gte(request.createdFrom());
            }
            if (request.createdTo() != null) {
                created = created.lte(request.createdTo());
            }
            filters.add(created);
        }
        //! step 5 - the free-text search, across the name and the subdomain
        if (hasText(request.search())) {
            filters.add(new Criteria().orOperator(
                    CriteriaText.containsIgnoreCase("schoolName", request.search()),
                    CriteriaText.containsIgnoreCase("subdomain", request.search())));
        }

        //! step 6 - AND them together. No filters at all means everything, which is the right
        //! answer to a request that asked for nothing in particular.
        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
