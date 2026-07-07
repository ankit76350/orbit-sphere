package com.orbitastra.backend.repositories.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.core.Holiday;

@Repository
public interface HolidayRepository extends MongoRepository<Holiday, String> {
    List<Holiday> findBySchoolId(String schoolId);
    Optional<Holiday> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    boolean existsBySchoolIdAndAcademicYear(String schoolId, String academicYear);
}
