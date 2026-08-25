package com.orbitastra.backend.repositories.identity;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.identity.Role;

public interface RoleRepository extends MongoRepository<Role, String> {

    List<Role> findBySchoolId(String schoolId);

    boolean existsBySchoolId(String schoolId);
}
