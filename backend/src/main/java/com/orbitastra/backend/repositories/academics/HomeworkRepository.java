package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.Homework;

@Repository
public interface HomeworkRepository extends MongoRepository<Homework, String> {
    List<Homework> findBySchoolId(String schoolId);
    List<Homework> findByClassId(String classId);

    @Query("{ 'studentAssignments.studentId': ?0 }")
    List<Homework> findByStudentAssignmentsStudentId(String studentId);

    @Query("{ 'schoolId': ?0, 'studentAssignments.studentId': ?1 }")
    List<Homework> findBySchoolIdAndStudentAssignmentsStudentId(String schoolId, String studentId);
}
