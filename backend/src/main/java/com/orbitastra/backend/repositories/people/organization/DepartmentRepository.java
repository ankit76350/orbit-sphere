package com.orbitastra.backend.repositories.people.organization;

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
public interface DepartmentRepository extends MongoRepository<Department, String> {

    /** Whether this school already uses a code. The uniqueness rule, asked before an insert. */
    boolean existsBySchoolIdAndDepartmentCode(String schoolId, String departmentCode);

    /** One department of this school, by its document id. */
    Optional<Department> findByIdAndSchoolId(String id, String schoolId);
}
