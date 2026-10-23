package com.orbitastra.backend.repositories.people.department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.people.department.Position;

/**
 * The approved seats inside a school's departments.
 *
 * <p><b>A position has no business code.</b> {@code positionCode} was removed on 2026-09-15, so a
 * seat is addressed by its document id — which is what {@code EmploymentRecord} stores as
 * {@code positionDocsId}, and what every reference to a seat already used.
 *
 * <p><b>Every query carries {@code schoolId}</b>, for the reason every repository in this project
 * does: another school's real id is a real id.
 */
public interface PositionRepository
        extends MongoRepository<Position, String>, PositionRepositoryCustom {

    /** One seat of this school, by its document id. */
    Optional<Position> findByIdAndSchoolId(String id, String schoolId);

    /**
     * Whether a department already holds a seat with this title.
     *
     * <p><b>Retired seats count.</b> {@code school_department_title_uniq} does not filter on
     * {@code active}, so a check that skipped them would accept a write the index then refuses —
     * a 500 where a 409 was meant.
     */
    boolean existsBySchoolIdAndDepartmentDocsIdAndTitleIgnoreCase(String schoolId,
            String departmentDocsId, String title);

    /**
     * Whether a department holds at least one teaching seat.
     *
     * <p><b>Replaced a {@code findAll()} on 2026-09-15.</b> The warning behind this used to read
     * every position of every school into memory and filter in Java — it was the only
     * {@code findAll()} in the whole services layer, it grew with the platform rather than with
     * the department, and it pulled other tenants' rows across a boundary they should never cross.
     *
     * <p><b>Retired seats count as teaching.</b> The question is "does this unit teach at all",
     * and a seat retired last term still says it does — the warning exists to catch a school that
     * has <i>never</i> flagged one.
     */
    boolean existsBySchoolIdAndDepartmentDocsIdAndTeachingPositionIsTrue(String schoolId,
            String departmentDocsId);

    /**
     * Every seat in one department, by title.
     *
     * <p><b>Retired seats included.</b> #52 shows what a unit is made of, and a retired seat is
     * still part of that — records made against it still name it. The response marks each one
     * rather than hiding it.
     */
    List<Position> findBySchoolIdAndDepartmentDocsIdOrderByTitleAsc(String schoolId,
            String departmentDocsId);

    /**
     * How many seats in one department are still active.
     *
     * <p><b>A count, not an exists</b>, because the refusal it feeds names the number. #10 turns
     * this into "four seats are still active here" — which tells a school what to do next, where
     * "not empty" leaves it guessing how much work that is.
     *
     * <p>Retired seats are excluded on purpose: they are what a school is left with once it has
     * closed a unit properly, so counting them would make the refusal impossible to satisfy.
     */
    long countBySchoolIdAndDepartmentDocsIdAndActiveIsTrue(String schoolId,
            String departmentDocsId);
}
