package com.orbitastra.backend.repositories.academics;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.TimetableSlot;

@Repository
public interface TimetableSlotRepository extends MongoRepository<TimetableSlot, String> {
    List<TimetableSlot> findBySchoolId(String schoolId);

    List<TimetableSlot> findBySchoolIdAndClassId(String schoolId, String classId);

    List<TimetableSlot> findBySchoolIdAndClassIdAndSectionIgnoreCase(String schoolId, String classId, String section);

    List<TimetableSlot> findBySchoolIdAndTeacherId(String schoolId, String teacherId);

    List<TimetableSlot> findBySchoolIdAndTeacherIdIn(String schoolId, Collection<String> teacherIds);

    long deleteBySchoolIdAndClassIdAndSectionIgnoreCase(String schoolId, String classId, String section);
}
