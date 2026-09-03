package com.orbitastra.backend.repositories.plans;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.plans.catalogue.PlanSearchRequest;
import com.orbitastra.backend.models.plans.PlanDefinition;

/**
 * The part of {@link PlanDefinitionRepository} that cannot be a derived query method.
 *
 * <p>#8's filters are each optional, so no method name could express it: a name per combination,
 * and none of them matching a request that sends no filters at all.
 *
 * <p>Spring Data finds the implementation by name — {@code PlanDefinitionRepositoryImpl}. Renaming
 * that class breaks the wiring at startup, silently, so do not.
 */
public interface PlanDefinitionRepositoryCustom {

    /** Filters, searches, sorts and pages, all in the database. */
    Page<PlanDefinition> search(PlanSearchRequest request, Pageable pageable);
}
