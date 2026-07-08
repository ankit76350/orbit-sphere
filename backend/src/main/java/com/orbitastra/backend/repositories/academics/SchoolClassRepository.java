package com.orbitastra.backend.repositories.academics;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.SchoolClass;

@Repository
public interface SchoolClassRepository extends MongoRepository<SchoolClass, String> {
    List<SchoolClass> findBySchoolId(String schoolId);
    Optional<SchoolClass> findByNameAndSchoolId(String name, String schoolId);
    List<SchoolClass> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);
    Optional<SchoolClass> findByNameAndSchoolIdAndAcademicYear(String name, String schoolId, String academicYear);
}
