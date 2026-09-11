package com.orbitastra.backend.services.academics;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassUpdateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.institution.affiliationprogramme.AffiliationProgrammeRepository;

import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year. Endpoints #12 and #13 of the plan in
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

    //! Endpoint 13 — edit a class -----------------------------------------------------

    /**
     * Changes a class's display name, sort order, or the programme it runs under.
     *
     * <p><b>Nothing structural is touched.</b> No section, no subject, no {@code active} flag —
     * #15 and #16 own that, and #17 to #27 own the embedded lists. This edits three fields and
     * refuses to be a way of doing anything else.
     *
     * <p><b>The rename is the reason this endpoint is small.</b> It is allowed at all only
     * because a class is addressed and referenced by its document id: twelve documents store
     * {@code classDocsId} and none stores a name or a code, so a rename joins nothing. Compare
     * {@code AcademicYear}, which has no rename endpoint and must never have one.
     *
     * <p><b>A class may keep its own name.</b> The uniqueness check compares ids, not just
     * names, so sending the name unchanged beside a new sort order is not a conflict with itself.
     */
    @Transactional
    public SchoolClassResponse updateClass(String academicYear, String classId,
            SchoolClassUpdateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();
        String year = academicYear.trim();

        //! step 2 - refuse a request that asks for nothing, before reading anything. A PATCH
        //! that changes nothing and answers 200 lets a client with a broken form look healthy.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send name, displayOrder, or affiliationProgrammeDocsId.");
        }

        //! step 3 - the year has to exist, for the same reason #12 checks it and with the same
        //! caveat: gate 4 in the controller answers this first, so it is unreachable through
        //! HTTP. It is here because WITHOUT it the two endpoints in this service disagree — a
        //! bad year gives #12 an ACADEMIC_YEAR_NOT_FOUND and #13 a CLASS_NOT_FOUND, which is
        //! true but says the wrong thing about what is wrong. Found by removing gate 4.
        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 4 - the class, scoped to the school AND the year. The id alone is globally
        //! unique, so querying by it alone would find another school's class, and an id pasted
        //! from last year's URL would edit last year's structure.
        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classId.trim(), school.getId(), year)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "No class with id '" + classId + "' in '" + year + "'."));

        //! step 5 - a new name has to be usable, and free. Comparing ids rather than names is
        //! what lets a class keep the name it already has.
        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("CLASS_NAME_REQUIRED",
                        "A class name cannot be removed. Send a new one, or omit the field.");
            }
            // TODO: read school class
            schoolClasses.findBySchoolIdAndAcademicYearAndName(school.getId(), year, newName)
                    .filter(other -> !other.getId().equals(schoolClass.getId()))
                    .ifPresent(other -> {
                        throw ApiException.conflict("CLASS_NAME_TAKEN",
                                "'" + year + "' already has a class called '" + newName + "'.");
                    });
            schoolClass.setName(newName);
        }

        //! step 6 - the sort order. Cannot be cleared: absent and null are the same thing on a
        //! record, and 0 is a real position rather than "no position". See the request DTO.
        if (request.displayOrder() != null) {
            schoolClass.setDisplayOrder(request.displayOrder());
        }

        //! step 7 - the programme. "" detaches it; a value is checked with schoolId in the
        //! query, because another school's id is real and would otherwise be accepted.
        if (request.affiliationProgrammeDocsId() != null) {
            String programmeId = TextHelper.blankToNull(request.affiliationProgrammeDocsId());
            if (programmeId != null) {
                // TODO: read affiliation programme
                affiliationProgrammes.findByIdAndSchoolId(programmeId, school.getId())
                        .orElseThrow(() -> ApiException.notFound("AFFILIATION_PROGRAMME_NOT_FOUND",
                                "No affiliation programme with id '" + programmeId
                                        + "' in this school."));
            }
            schoolClass.setAffiliationProgrammeDocsId(programmeId);
        }

        //! step 8 - save
        // TODO: update school class
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SchoolClassResponse.fromSchoolClass(saved,
                "'" + saved.getName() + "' updated. Sections and subjects are untouched — they "
                        + "have their own endpoints, none of which is built. "
                        + NO_AUTHORIZATION_YET);
    }
}
