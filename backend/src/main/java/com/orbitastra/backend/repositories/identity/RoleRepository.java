package com.orbitastra.backend.repositories.identity;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.identity.Role;

/**
 * One document per school holds all of that school's roles, so every method here is keyed on
 * schoolId alone and returns at most one document. It was one document per role until
 * 2026-09-05 — see Role.
 */
public interface RoleRepository extends MongoRepository<Role, String>, RoleRepositoryCustom {
    // RoleRepository
    //         ↓
    // RoleRepositoryCustom
    //         ↓
    // RoleRepositoryImpl


    /** The school's roles document, or empty when it has never been provisioned. */
    Optional<Role> findBySchoolId(String schoolId);

    boolean existsBySchoolId(String schoolId);

    /**
     * Does this school have a role with that key?
     *
     * <p>Spring Data turns the nested property into {@code {'schoolId': .., 'roles.roleKey': ..}},
     * which is exactly what the {@code school_role_key_idx} multikey index serves. Endpoint #3
     * asks this for {@code SCHOOL_ADMIN} before it will activate a school.
     */
    boolean existsBySchoolIdAndRolesRoleKey(String schoolId, String roleKey);
}
