package com.orbitastra.backend.repositories.academics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.academics.DailyTimetable;

@Repository
public interface DailyTimetableRepository extends MongoRepository<DailyTimetable, String> {
    Optional<DailyTimetable> findBySchoolIdAndDate(String schoolId, LocalDate date);

    List<DailyTimetable> findBySchoolIdAndDateIn(String schoolId, Collection<LocalDate> dates);

    // Derived queries cannot express gte+lte on the same field, hence @Query.
    @Query(value = "{ 'schoolId': ?0, 'date': { '$gte': ?1, '$lte': ?2 } }", sort = "{ 'date': 1 }")
    List<DailyTimetable> findBySchoolIdAndDateRange(String schoolId, LocalDate startDate, LocalDate endDate);
}
