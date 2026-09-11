package com.orbitastra.backend.repositories.people.staff;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.people.staff.Staff;

/**
 * Staff, so a section can name its class teacher and a subject its teachers.
 *
 * <p><b>Created 2026-09-11 for endpoint #17</b>, which accepts a {@code classTeacherDocsId}.
 * Until this existed there was no way to check the id was real or belonged to the caller's
 * school, so the field would have accepted another tenant's staff id — the bug {@code SchoolBase}
 * exists to prevent, discovered when a timetable printed a teacher who works somewhere else.
 *
 * <p>The second of the three repositories the module's README listed as missing;
 * {@code AffiliationProgrammeRepository} was the first. {@code grading_schemes} is still absent
 * and blocks the same check on #22's {@code gradingSchemeDocsId}.
 *
 * <p><b>This is the {@code people} module's first repository.</b> That module has 26 models and
 * no API; nothing here reads staff for its own sake, only to refuse a bad reference.
 */
public interface StaffRepository extends MongoRepository<Staff, String> {

    /**
     * One staff member, scoped to their school.
     *
     * <p>{@code schoolId} is in the query even though the id is globally unique. Looking up by id
     * alone would find another school's staff and quietly accept them as a class teacher.
     */
    Optional<Staff> findByIdAndSchoolId(String id, String schoolId);
}
