package com.orbitastra.backend.repositories.plans;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.plans.PlanDefinition;

/**
 * The plan catalogue. <b>Not school-scoped</b> — a PlanDefinition is platform configuration
 * shared by every tenant, and it is the only document in this module with no {@code schoolId}.
 *
 * <p>Every lookup is by {@code planCode} plus {@code planVersion}, because that pair is the
 * plan's business identity: {@code PREMIUM} version 1 and {@code PREMIUM} version 2 are two
 * documents with two prices, and a school is on exactly one of them. There is a unique index on
 * the pair.
 */
public interface PlanDefinitionRepository extends MongoRepository<PlanDefinition, String> {

    Optional<PlanDefinition> findByPlanCodeAndPlanVersion(String planCode, Integer planVersion);

    boolean existsByPlanCode(String planCode);

    /** Every version of one plan. Used by the version list and by "make a new version". */
    List<PlanDefinition> findByPlanCodeOrderByPlanVersionDesc(String planCode);
}
