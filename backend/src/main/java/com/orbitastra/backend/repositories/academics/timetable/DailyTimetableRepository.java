package com.orbitastra.backend.repositories.academics.timetable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.timetable.DailyTimetable;

/**
 * One school's timetable, one document per date.
 *
 * <p><b>A day is addressed by its date, not by its id</b>, which is unusual in this project and
 * right here for one reason: a caller always knows the date and never knows the id. A teacher's app
 * asks what is on today. {@code school_timetable_date_uniq} is what makes the date sufficient.
 *
 * <p><b>Every query carries {@code schoolId}</b>, for the reason every repository in this project
 * does: another school's real id — or its date — is still real.
 */
public interface DailyTimetableRepository extends MongoRepository<DailyTimetable, String> {

    /** One day of one school. */
    Optional<DailyTimetable> findBySchoolIdAndDate(String schoolId, LocalDate date);

    /**
     * Which of these dates the school already has a timetable for.
     *
     * <p><b>One query for the whole range, not one per date.</b> A fortnight answered by fourteen
     * {@code existsBy...} calls is the N+1 that makes a longer range slower than a shorter one for
     * no reason the caller can see.
     *
     * <p>Returns the documents rather than a count, because the refusal names the colliding dates —
     * "three of these already exist" tells a caller nothing it can act on.
     */
    List<DailyTimetable> findBySchoolIdAndDateIn(String schoolId, Collection<LocalDate> dates);
}
