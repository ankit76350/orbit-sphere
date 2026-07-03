package com.orbitastra.backend.repositories.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.enums.StudentStatus;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByAdmissionNo(String admissionNo);
    List<Student> findBySchoolId(String schoolId);
    long countBySchoolId(String schoolId);
    List<Student> findByParentId(String parentId);
    List<Student> findByStatus(StudentStatus status);
}
