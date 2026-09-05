package com.orbitastra.backend.repositories.plans;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.common.mongo.CriteriaText;
import com.orbitastra.backend.dto.plans.catalogue.PlanSearchRequest;
import com.orbitastra.backend.models.plans.PlanDefinition;

import lombok.RequiredArgsConstructor;

/**
 * The dynamic plan query behind #8.
 *
 * <p><b>Everything happens in the database.</b> Filtering, searching, sorting and paging are all
 * on the query, so only one page of documents is read however many plan versions exist. The same
 * arrangement as the school list, down to the shared {@link CriteriaText}.
 *
 * <p>Two round trips: one for the page, one for the total behind {@code totalElements}. The count
 * is built from the same criteria object rather than a second hand-written copy, which is the
 * only way the two cannot drift apart.
 */
@RequiredArgsConstructor
public class PlanDefinitionRepositoryImpl implements PlanDefinitionRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<PlanDefinition> search(PlanSearchRequest request, Pageable pageable) {
        //! step 1 - build the filter from whichever parameters were sent
        Criteria criteria = buildCriteria(request);

        //! step 2 - build the count query. It carries the filter and NOTHING else: given the
        //! page's skip and limit it would only ever count one page.
        Query countQuery = new Query(criteria);

        //! step 3 - build the page query, which is the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading plans (how many match)
        long total = mongo.count(countQuery, PlanDefinition.class);

        //! step 5 - run the page. Only these rows are read, however many versions exist.
        // TODO: reading plans (one page of them)
        List<PlanDefinition> rows = mongo.find(pageQuery, PlanDefinition.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /** The filters, combined with AND. An absent one adds nothing, so no filters means all. */
    private Criteria buildCriteria(PlanSearchRequest request) {
        //! step 1 - start with nothing, and add only the filters that were actually sent
        List<Criteria> filters = new ArrayList<>();

        //! step 2 - status, which ORs within itself
        if (request.statuses() != null && !request.statuses().isEmpty()) {
            filters.add(Criteria.where("status").in(request.statuses()));
        }

        //! step 3 - the plan family, matched exactly
        if (hasText(request.planCode())) {
            // Exact: this is how somebody asks for every version of one plan.
            filters.add(CriteriaText.exactIgnoreCase("planCode", normalizeCode(request.planCode())));
        }

        //! step 4 - the display name, matched loosely
        if (hasText(request.name())) {
            filters.add(CriteriaText.containsIgnoreCase("name", request.name()));
        }

        //! step 5 - public or quote-only
        if (request.publiclyAvailable() != null) {
            filters.add(Criteria.where("publiclyAvailable").is(request.publiclyAvailable()));
        }

        //! step 6 - the free-text search, across the code and the name
        if (hasText(request.search())) {
            filters.add(new Criteria().orOperator(
                    CriteriaText.containsIgnoreCase("planCode", request.search()),
                    CriteriaText.containsIgnoreCase("name", request.search())));
        }

        //! step 7 - AND them together. No filters at all means everything.
        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters);
    }

    /**
     * The same shaping a code gets when a plan is created, so a filter typed as
     * {@code premium-plus} finds {@code PREMIUM_PLUS}.
     *
     * <p>Deliberately not calling the validator: this is a filter, and a code of the wrong shape
     * should simply match nothing rather than turn a list request into an error.
     */
    private String normalizeCode(String raw) {
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
