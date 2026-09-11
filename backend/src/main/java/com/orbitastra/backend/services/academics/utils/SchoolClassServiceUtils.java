package com.orbitastra.backend.services.academics.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * The two lookups every academic-structure endpoint needs.
 *
 * <p>Lifted out of {@code SchoolClassService} on 2026-09-11, when the same year check stood in
 * <b>five</b> of its methods and the same class lookup in <b>three</b>. Six endpoints written one
 * after another had each repeated them, and the copies had already started to differ: #13 gave a
 * {@code CLASS_NOT_FOUND} for an unknown year where #12 gave {@code ACADEMIC_YEAR_NOT_FOUND},
 * because one of the copies was missing. One home each is what stops that recurring.
 *
 * <p><b>These are not validators.</b> {@code CoreHelper} refuses bad input; nothing here refuses
 * anything about a request. Both 404s are the answer to "which one did you mean", not a rule
 * about what is allowed.
 *
 * <p>A {@code @Component} rather than a static utility because both need repositories.
 *
 * <h2>Why two methods and not one</h2>
 *
 * <p>The obvious shape is a single {@code loadClass} that checks the year on its way through —
 * and it is the wrong one, twice over.
 *
 * <p><b>The service rules forbid it.</b> A utils method may never call another utils method, so
 * the year check would have to be duplicated inside the class lookup rather than reused. The
 * rule exists so a reader can see what a service method does without following a chain.
 *
 * <p><b>And three endpoints need only the year.</b> #12 creates a class, #28 lists them and #17
 * checks the year before it reads the class — a combined method would make them pay for a lookup
 * they do not want, or force a second method anyway.
 *
 * <p>So the service calls both, in that order, and the order is visible at the call site. It has
 * to be that order: reversed, an unknown year answers {@code CLASS_NOT_FOUND}, which is true but
 * says the wrong thing about what is wrong.
 */
@Component
@RequiredArgsConstructor
public class SchoolClassServiceUtils {

    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;

    //! the year — used by every endpoint in the module ---------------------------------

    /**
     * Refuses unless the school has a year by that name.
     *
     * <p>A class written against a year that does not exist is an orphan the moment it is saved,
     * and nothing downstream would report it: every consumer joins on the string, so there is no
     * foreign key to fail.
     *
     * <p><b>Unreachable through HTTP on the writes, and the only thing that answers on the
     * reads.</b> Gate 4 loads the same year in the controller and throws the same 404 first — so
     * on #12, #13 and #17 this never fires, and mutating it away leaves every test passing. It is
     * here because a service must not depend on a controller having run a gate: #35 and #36 will
     * copy a structure between two years and call in with a year no gate saw. On #28, #29 and
     * #30 no gate runs at all, and this is what answers.
     *
     * @return the trimmed year name, which is what every later query should use
     *
     * Used by:
     * - addSection()
     * - createClass()
     * - getClass()
     * - listClasses()
     * - listSections()
     * - updateClass()
     */
    public String requireAcademicYear(School school, String academicYear) {
        String year = academicYear == null ? "" : academicYear.trim();

        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }
        return year;
    }

    //! the class — used by every endpoint that names one --------------------------------

    /**
     * One class by its document id, scoped to the school and the year.
     *
     * <p><b>All three parts, and none of them optional.</b> The id alone is globally unique, so
     * querying by it alone would read another school's class — the tenant boundary
     * {@code SchoolBase} exists to enforce. The year is in the query too, so an id pasted from
     * last year's URL answers 404 rather than quietly reading last year's structure.
     *
     * <p><b>Call {@link #requireAcademicYear} first.</b> This method does not, because a utils
     * method may not call another, and because an unknown year reaching here answers
     * {@code CLASS_NOT_FOUND} — true, but the wrong thing to say about what is wrong.
     *
     * Used by:
     * - addSection()
     * - getClass()
     * - listSections()
     * - updateClass()
     */
    public SchoolClass loadClass(School school, String year, String classId) {
        // TODO: read school class
        return schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(
                        classId == null ? "" : classId.trim(), school.getId(), year)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "No class with id '" + classId + "' in '" + year + "'."));
    }
}
