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
        Query query = new Query(buildCriteria(request));

        // The count must not see the skip and limit, so it is taken before they are applied.
        long total = mongo.count(Query.of(query).limit(-1).skip(-1), School.class);

        List<School> rows = mongo.find(query.with(pageable), School.class);
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * The filters, combined with AND. Absent ones add nothing.
     *
     * <p>An empty criteria list means "everything", which is the right answer to a request that
     * asked for no filters at all.
     */
    private Criteria buildCriteria(SchoolSearchRequest request) {
        List<Criteria> filters = new ArrayList<>();

        if (request.statuses() != null && !request.statuses().isEmpty()) {
            // OR within the field: "show me the live ones" is one question, not two requests.
            filters.add(Criteria.where("status").in(request.statuses()));
        }
        if (hasText(request.countryCode())) {
            filters.add(CriteriaText.exactIgnoreCase("countryCode", request.countryCode().trim()));
        }
        if (hasText(request.city())) {
            filters.add(CriteriaText.exactIgnoreCase("city", request.city().trim()));
        }
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
        if (hasText(request.search())) {
            filters.add(new Criteria().orOperator(
                    CriteriaText.containsIgnoreCase("schoolName", request.search()),
                    CriteriaText.containsIgnoreCase("subdomain", request.search())));
        }

        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
