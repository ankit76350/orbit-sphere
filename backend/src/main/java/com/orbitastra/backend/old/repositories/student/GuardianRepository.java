package com.orbitastra.backend.old.repositories.student;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.old.student.Guardian;

@Repository
public interface GuardianRepository extends MongoRepository<Guardian, String> {
    List<Guardian> findBySchoolId(String schoolId);
    List<Guardian> findBySchoolIdAndName(String schoolId, String name);
}
