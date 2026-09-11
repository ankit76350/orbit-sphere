package com.orbitastra.backend.services.academics;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.institution.affiliationprogramme.AffiliationProgrammeRepository;

import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year. Endpoint #12 of the plan in
 * {@code controllers/academics/structure/README.md}.
 *
 * <p>School surface, so the tenant comes from CurrentSchoolResolver and never from the URL. There
 * is no platform surface for classes: no operator of ours decides that a school teaches Grade 7.
 *
 * <p><b>A class is addressed by its document id.</b> Twelve other documents already store
 * {@code classDocsId} and not one stores a class code, so the id is what the whole system means
 * when it says "which class". {@code sectionNo} and {@code subjectCode} are codes only because
 * they are <i>embedded</i> and have no id to be referenced by — a top-level document does not
 * need one.
 *
 * <p><b>Which is why the name is editable.</b> An academic year <i>is</i> its name to every other
 * collection, so it can never be renamed; a class is its id, so a rename joins nothing and breaks
 * nothing. The name only has to stay unique inside the year.
 *
 * <p><b>Sections and subjects are embedded, so they have no service of their own.</b> When #17
 * and #22 arrive they belong here, writing positionally into the class document, the way holidays
 * belong to AcademicYearService.
 */
@Service
@RequiredArgsConstructor
public class SchoolClassService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    private final SchoolClassRepository schoolClasses;
    private final AcademicYearRepository academicYears;
    private final AffiliationProgrammeRepository affiliationProgrammes;
    private final CurrentSchoolResolver currentSchool;

    //! Endpoint 12 — create a class ---------------------------------------------------

    /**
     * Creates a class for one academic year, with no sections and no subjects.
     *
     * <p>The gates run in the controller, not here — see
     * {@code memory/backend/code-writing-rules}. What is left in this method is the year, the
     * name and the reference.
     *
     * <p><b>An index had to be repointed before this could be built.</b>
     * {@code school_year_class_code_uniq} indexed a {@code classCode} that no model ever
     * declared, so every document would have indexed a <i>missing</i> value, they would all have
     * collided, and a school could have held exactly one class per academic year — the second
     * insert a duplicate-key error. It is now {@code school_year_class_name_uniq} on
     * {@code name}: a real guarantee, on a field that exists.
     */
    @Transactional
    public SchoolClassResponse createClass(String academicYear, SchoolClassCreateRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();
        String year = academicYear.trim();

        //! step 2 - the year in the path has to be a year this school actually has. A class
        //! written against a year that does not exist is an orphan the moment it is saved, and
        //! nothing downstream would report it: every consumer joins on the string.
        //!
        //! UNREACHABLE THROUGH HTTP TODAY, and worth saying so rather than letting somebody
        //! discover it. Gate 4 in the controller loads the same year to read isThisYearRunning,
        //! and throws the same ACADEMIC_YEAR_NOT_FOUND first - so mutating this check to
        //! `if (false)` leaves every test passing. It stays because a service must not depend on
        //! a controller having run a gate: #35 and #36 copy a structure between two years and
        //! will call in here with a year the gate never saw.
        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the name has to be free in this year. Two classes both called "Grade 7"
        //! would leave every screen showing the same class twice with nothing to tell them
        //! apart. school_year_class_name_uniq guarantees it; this check is what turns a
        //! duplicate-key error into a message that names the class.
        String name = request.name().trim();
        // TODO: check school class exists
        if (schoolClasses.existsBySchoolIdAndAcademicYearAndName(school.getId(), year, name)) {
            throw ApiException.conflict("CLASS_NAME_TAKEN",
                    "'" + year + "' already has a class called '" + name + "'.");
        }

        //! step 4 - the programme, when one was named. Checked with schoolId in the query, not
        //! by id alone: an id from another school exists, and looking it up without the tenant
        //! would accept it.
        String programmeId = TextHelper.blankToNull(request.affiliationProgrammeDocsId());
        if (programmeId != null) {
            // TODO: read affiliation programme
            affiliationProgrammes.findByIdAndSchoolId(programmeId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("AFFILIATION_PROGRAMME_NOT_FOUND",
                            "No affiliation programme with id '" + programmeId
                                    + "' in this school."));
        }

        //! step 5 - build the document, with nothing taught in it yet
        // Sections and subjects are never set here. Both are their own resources with their own
        // endpoints (#17 and #22), and mixing them into creation meant one request that could
        // fail for two unrelated reasons - a bad class name or a stray subject - with the caller
        // having to work out which.
        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(school.getId())
                .academicYear(year)
                .name(name)
                .displayOrder(request.displayOrder())
                .affiliationProgrammeDocsId(programmeId)
                .sections(new ArrayList<>())
                .subjects(new ArrayList<>())
                .active(true)
                .build();

        //! step 6 - save
        // TODO: insert school class
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SchoolClassResponse.fromSchoolClass(saved,
                "'" + saved.getName() + "' has no sections and no subjects yet. Add a section "
                        + "next — nothing can be placed in this class until one exists, because "
                        + "a student record stores sectionNo. " + NO_AUTHORIZATION_YET);
    }
}
