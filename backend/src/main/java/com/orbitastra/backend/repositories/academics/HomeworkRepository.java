package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.Homework;

@Repository
public interface HomeworkRepository extends MongoRepository<Homework, String> {
    List<Homework> findBySchoolId(String schoolId);
    List<Homework> findByClassDocsId(String classDocsId);
    List<Homework> findBySchoolIdAndAcademicYear(String schoolId, String academicYear);

    @Query("{ 'studentAssignments.studentDocsId': ?0 }")
    List<Homework> findByStudentAssignmentsStudentDocsId(String studentDocsId);

    @Query("{ 'schoolId': ?0, 'studentAssignments.studentDocsId': ?1 }")
    List<Homework> findBySchoolIdAndStudentAssignmentsStudentDocsId(String schoolId, String studentDocsId);
}
