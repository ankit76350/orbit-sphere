package com.orbitastra.backend.repositories.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.core.AcademicYear;

@Repository
public interface AcademicYearRepository extends MongoRepository<AcademicYear, String> {
    List<AcademicYear> findBySchoolId(String schoolId);

    Optional<AcademicYear> findBySchoolIdAndName(String schoolId, String name);

    boolean existsBySchoolIdAndName(String schoolId, String name);
}
