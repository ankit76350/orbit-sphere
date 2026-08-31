package com.orbitastra.backend.repositories.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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
            filters.add(exactIgnoreCase("countryCode", request.countryCode().trim()));
        }
        if (hasText(request.city())) {
            filters.add(exactIgnoreCase("city", request.city().trim()));
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
            String term = escapeRegex(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("schoolName").regex(term, "i"),
                    Criteria.where("subdomain").regex(term, "i")));
        }

        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters);
    }

    /** An exact match that ignores case, anchored so it cannot also match a longer value. */
    private Criteria exactIgnoreCase(String field, String value) {
        return Criteria.where(field).regex("^" + escapeRegex(value) + "$", "i");
    }

    /**
     * Makes caller input safe to put inside a regular expression.
     *
     * <p><b>This is not optional.</b> The search term goes into a Mongo regex, so without it a
     * caller can send a pattern rather than a word: {@code .*} matches everything, and a
     * nested-quantifier pattern can pin a database thread for a very long time on very little
     * input. Escaping turns every metacharacter back into the literal the caller typed, which is
     * what somebody searching for "st." meant anyway.
     */
    private String escapeRegex(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if ("\\.[]{}()*+-?^$|/".indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
