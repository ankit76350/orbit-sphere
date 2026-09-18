package com.orbitastra.backend.repositories.academics.attendance;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.attendance.AttendanceSession;

/**
 * Attendance sessions, read by the one thing outside attendance that has to ask about them.
 *
 * <p><b>This exists for timetable #5.</b> Removing a period that a session already points at would
 * leave that session naming nothing, and dangling links are the thing this project has refused
 * everywhere else — see open item 4 of
 * {@code controllers/academics/timetable/README.md}, which proposed exactly this check.
 *
 * <p><b>Nothing writes this collection yet</b>, so the refusal cannot fire through the API today.
 * It is still the right check: it costs one query per removal, and it becomes live the moment the
 * attendance module is built rather than needing to be remembered then.
 *
 * <p>The attendance module will own this file when it is built; it is here now because the
 * repository folder rules put a repository under the module of the document it reads, not under
 * the module that happens to call it first.
 */
public interface AttendanceSessionRepository extends MongoRepository<AttendanceSession, String> {

    /**
     * Whether any session of this school is taken against that period.
     *
     * <p><b>Scoped by school as well as by entry id</b>, for the reason every query in this project
     * is: another school's real entry id is still a real id, and a check that answered "yes" across
     * the tenant boundary would refuse a removal this school is entitled to make.
     */
    boolean existsBySchoolIdAndTimetableEntryId(String schoolId, String timetableEntryId);
}
