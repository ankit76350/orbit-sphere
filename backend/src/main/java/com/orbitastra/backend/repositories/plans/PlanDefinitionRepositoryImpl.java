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
        Query query = new Query(buildCriteria(request));

        // Taken before the skip and limit are applied, or it would count one page.
        long total = mongo.count(Query.of(query).limit(-1).skip(-1), PlanDefinition.class);

        List<PlanDefinition> rows = mongo.find(query.with(pageable), PlanDefinition.class);
        return new PageImpl<>(rows, pageable, total);
    }

    /** The filters, combined with AND. An absent one adds nothing, so no filters means all. */
    private Criteria buildCriteria(PlanSearchRequest request) {
        List<Criteria> filters = new ArrayList<>();

        if (request.statuses() != null && !request.statuses().isEmpty()) {
            filters.add(Criteria.where("status").in(request.statuses()));
        }
        if (hasText(request.planCode())) {
            // Exact: this is how somebody asks for every version of one plan.
            filters.add(CriteriaText.exactIgnoreCase("planCode", normalizeCode(request.planCode())));
        }
        if (hasText(request.name())) {
            filters.add(CriteriaText.containsIgnoreCase("name", request.name()));
        }
        if (request.publiclyAvailable() != null) {
            filters.add(Criteria.where("publiclyAvailable").is(request.publiclyAvailable()));
        }
        if (hasText(request.search())) {
            filters.add(new Criteria().orOperator(
                    CriteriaText.containsIgnoreCase("planCode", request.search()),
                    CriteriaText.containsIgnoreCase("name", request.search())));
        }

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
