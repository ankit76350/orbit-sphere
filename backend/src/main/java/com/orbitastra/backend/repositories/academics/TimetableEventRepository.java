package com.orbitastra.backend.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.TimetableEvent;

@Repository
public interface TimetableEventRepository extends MongoRepository<TimetableEvent, String> {
    List<TimetableEvent> findBySchoolId(String schoolId);
    List<TimetableEvent> findByClassNameAndSchoolId(String className, String schoolId);
    List<TimetableEvent> findByClassId(String classId);
    List<TimetableEvent> findByClassIdAndSchoolId(String classId, String schoolId);
}
