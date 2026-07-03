package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.Parent;

@Repository
public interface ParentRepository extends MongoRepository<Parent, String> {
    Optional<Parent> findByEmail(String email);
    Optional<Parent> findByPhone(String phone);
    List<Parent> findBySchoolId(String schoolId);
}
