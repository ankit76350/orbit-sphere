package com.orbitastra.backend.repositories.people.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.people.organization.Department;

/**
 * The org units a school is structured into.
 *
 * <p><b>Every query carries {@code schoolId}.</b> A {@code departmentCode} is unique only within
 * one school, so a lookup by code alone would find another school's unit — the bug tenant-scoped
 * lookups exist across this project to prevent.
 */
public interface DepartmentRepository
        extends MongoRepository<Department, String>, DepartmentRepositoryCustom {

    /** Whether this school already uses a code. The uniqueness rule, asked before an insert. */
    boolean existsBySchoolIdAndDepartmentCode(String schoolId, String departmentCode);

    /** One department of this school, by its document id. */
    Optional<Department> findByIdAndSchoolId(String id, String schoolId);

    /**
     * The units directly under one parent, by name.
     *
     * <p><b>Direct children only, not the subtree.</b> #52 answers "what is this unit made of";
     * the whole nesting is #12 with {@code ?tree=true}, which builds it from one flat read.
     */
    List<Department> findBySchoolIdAndParentDepartmentDocsIdOrderByNameAsc(String schoolId,
            String parentDepartmentDocsId);
}
