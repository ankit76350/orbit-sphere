package com.orbitastra.backend.repositories.identity;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.identity.Role;

public interface RoleRepository extends MongoRepository<Role, String> {

    List<Role> findBySchoolId(String schoolId);

    boolean existsBySchoolId(String schoolId);

    /**
     * For the activation readiness check. A school with no SCHOOL_ADMIN role cannot be given a
     * first administrator, so activating it produces a tenant nobody can log into.
     */
    boolean existsBySchoolIdAndRoleKey(String schoolId, String roleKey);
}
