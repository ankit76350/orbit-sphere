package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassSearchRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SectionCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SubjectCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SubjectUpdateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassUpdateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassDetailResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SectionListResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SubjectListResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.institution.affiliationprogramme.AffiliationProgrammeRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.utils.SchoolClassServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year, with their sections and subjects. Endpoints #12,
 * #13, #17, #22, #28, #29 and #30 of the plan in
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

    /**
     * The fields #28 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>An allow-list, not a pass-through.</b> Anything else is a 400 naming what is
     * accepted, so nobody can order by a field with nothing behind it and nobody can learn the
     * document's shape by guessing names. Keys are lowercase because the lookup is.
     *
     * <p>A {@link LinkedHashMap} so the refusal lists them in this order rather than a hash
     * order that changes between runs.
     */
    /**
     * The fields #28 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>An allow-list, not a pass-through.</b> Anything else is a 400 naming what is
     * accepted, so nobody can order by a field with nothing behind it and nobody can learn the
     * document's shape by guessing names. Keys are lowercase because the lookup is.
     */
    private static final Map<String, String> SORTABLE_CLASS_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_CLASS_FIELDS.put("name", "name");
        SORTABLE_CLASS_FIELDS.put("createdat", "createdAt");
        SORTABLE_CLASS_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same names as they should be typed, for the refusal message. */
    private static final String SORTABLE_CLASS_FIELD_NAMES =
            SORTABLE_CLASS_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order for #28.
     *
     * <p><b>{@code name}, since 2026-09-11.</b> It was a school-defined {@code displayOrder}
     * until that field was removed, and the loss is real: alphabetical puts "Grade 10" before
     * "Grade 2", and an order like "Nursery, LKG, UKG, 1, 2, 3" cannot be expressed at all.
     *
     * <p>What it buys is that the order needs no tiebreaker — {@code name} is unique within the
     * year, so it is a total order and pagination is stable — and that
     * {@code school_year_class_name_uniq} serves it, so the sort comes from an index rather than
     * a blocking in-memory pass.
     */
    private static final Sort CLASS_ORDER = Sort.by(Sort.Order.asc("name"));

    private final SchoolClassRepository schoolClasses;
    private final AffiliationProgrammeRepository affiliationProgrammes;
    private final StaffRepository staff;
    private final GradingSchemeRepository gradingSchemes;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolClassServiceUtils utils;

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
        //! step 2 - the year in the path has to be a year this school actually has
        String year = utils.requireAcademicYear(school, academicYear);

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

        //! step 2 - refuse a request that asks for nothing, before reading anything. A PATCH
        //! that changes nothing and answers 200 lets a client with a broken form look healthy.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send name or affiliationProgrammeDocsId.");
        }

        //! step 3 - the year has to exist. Before the class, so a bad year says so rather than
        //! answering CLASS_NOT_FOUND - which is true and says the wrong thing about what is
        //! wrong. Found by removing gate 4 and watching the two endpoints' codes diverge.
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 3 - the class, scoped to the school AND the year
        SchoolClass schoolClass = utils.loadClass(school, year, classId);

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

        //! step 6 - the programme. "" detaches it; a value is checked with schoolId in the
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

        //! step 7 - save
        // TODO: update school class
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SchoolClassResponse.fromSchoolClass(saved,
                "'" + saved.getName() + "' updated. Sections and subjects are untouched — they "
                        + "have their own endpoints, none of which is built. "
                        + NO_AUTHORIZATION_YET);
    }

    //! Endpoint 28 — list the year's classes -----------------------------------------

    /**
     * One year's classes, filtered, sorted and paged.
     *
     * <p><b>Read-only, so no gates and no {@code @Transactional}.</b> Looking at a structure is
     * not an action on it, and a school that has stopped paying still has to be able to read its
     * own records — the rule the whole project follows, set out in {@code controllers/core}.
     *
     * <p><b>Which makes this the one place the year check actually fires.</b> On #12 and #13 gate
     * 4 loads the year first and throws the same 404, so their own checks are unreachable through
     * HTTP. No gate runs here, so this is the check that answers
     * {@code 404 ACADEMIC_YEAR_NOT_FOUND} — and an unknown year is a 404 rather than an empty
     * page, because "that year does not exist" and "that year has no classes" are different
     * answers and a school acting on the second would wait for classes that can never appear.
     *
     * <p><b>The embedded lists are not returned, only their sizes.</b> A twelve-class year with
     * four sections and ten subjects each is 168 embedded rows in one response nobody reads. #29
     * is for one class in full.
     */
    public PageResponse<SchoolClassResponse> listClasses(String academicYear,
            SchoolClassSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_CLASS_FIELDS, SORTABLE_CLASS_FIELD_NAMES, CLASS_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure.
        School school = currentSchool.require();
        //! step 2 - the year in the path has to be a year this school actually has
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 4 - one page, filtered and ordered in the database. The tenant and the year are
        //! passed separately from the request because they are the boundary, not filters.
        // TODO: read classes
        return PageResponse.from(
                schoolClasses.search(school.getId(), year, request, pageable),
                // The single-argument factory, so no `nextStep` appears: a read changed nothing,
                // and a "nextStep": null on every row of a list is noise a client then has to
                // decide whether to trust.
                SchoolClassResponse::fromSchoolClass);
    }

    //! Endpoint 17 — add a section to a class -----------------------------------------

    /**
     * Adds one section to a class.
     *
     * <p><b>The endpoint the student module is waiting for.</b>
     * {@code StudentAcademicRecord.sectionNo} has nothing to point at until this runs, and six of
     * {@code models/academics}' own documents are behind the same wall.
     *
     * <p><b>A section is embedded, so the document written is the class.</b> There is no section
     * collection, no section id and no {@code schoolId} on a section — it inherits all three from
     * its parent. That is also why {@code sectionNo} can never change: eight collections store it
     * as a plain string, and an embedded value has no id for them to reference instead.
     *
     * <p><b>Mongo cannot enforce uniqueness inside an array</b>, so the check in step 5 is the
     * only thing standing between a class and two sections both called "A". There is no index to
     * fall back on here, unlike the class name.
     */
    @Transactional
    public SectionListResponse addSection(String academicYear, String classId,
            SectionCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();
        //! step 2 - the year in the path has to be a year this school actually has
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 3 - the class, scoped to the school AND the year
        SchoolClass schoolClass = utils.loadClass(school, year, classId);

        //! step 4 - the value to store, exactly as typed. sectionNo is the display value as well
        //! as the reference, so it is trimmed and nothing else: a school naming its sections by
        //! colour is not making a mistake.
        String sectionNo = request.sectionNo().trim();

        //! step 5 - and it has to be free in this class. Checked case-insensitively: "A" and "a"
        //! in one class is a typo every time, and the two would be indistinguishable on screen.
        //! This check is the ONLY guard — Mongo cannot make an array's contents unique.
        List<ClassSection> sections = schoolClass.getSections() == null
                ? new ArrayList<>()
                : schoolClass.getSections();

        boolean taken = sections.stream()
                .anyMatch(existing -> existing.getSectionNo() != null
                        && existing.getSectionNo().equalsIgnoreCase(sectionNo));

        if (taken) {
            throw ApiException.conflict("SECTION_ALREADY_EXISTS",
                    "'" + schoolClass.getName() + "' already has a section '" + sectionNo
                            + "'. Section numbers are unique within a class, and cannot be "
                            + "changed once records reference them.");
        }

        //! step 6 - the class teacher, when one was named. Checked with schoolId in the query,
        //! not by id alone: another school's staff id is real, and a lookup without the tenant
        //! would accept them as this class's teacher.
        String teacherId = TextHelper.blankToNull(request.classTeacherDocsId());
        if (teacherId != null) {
            // TODO: read staff
            staff.findByIdAndSchoolId(teacherId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + teacherId + "' in this school."));
        }

        //! step 7 - append it, and set the list back on the document.
        //!
        //! MEASURED 2026-09-11, because the obvious reason is the wrong one. An ABSENT
        //! `sections` field does NOT read as null: Spring Data supplies an empty list for a
        //! missing collection property, so both the guard above and this line are no-ops for it.
        //! What they are for is a field stored as an explicit `null`, which does read as null -
        //! without them that is a 500. Verified both ways against the live database; three
        //! comments in this repository claimed the absent case, and none of them was right.
        sections.add(ClassSection.builder()
                .sectionNo(sectionNo)
                .classTeacherDocsId(teacherId)
                .capacity(request.capacity())
                // Stated here as well as on the model. ClassSection carries
                // @Builder.Default active = true, so this line is redundant TODAY - removing it
                // changes nothing, which a mutation test confirmed by surviving. It stays
                // because a section starting active is this endpoint's decision, not the
                // model's default's to make, and the two can be changed independently.
                .active(true)
                .build());
        schoolClass.setSections(sections);

        //! step 8 - save
        // TODO: update school class (append a section)
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SectionListResponse.fromSchoolClass(saved,
                "Section '" + sectionNo + "' added to '" + saved.getName() + "'. A student can "
                        + "be placed in it as soon as the student module exists — nothing else "
                        + "in this class is affected. " + NO_AUTHORIZATION_YET);
    }

    //! Endpoint 29 — one class in full ------------------------------------------------

    /**
     * One class with its sections and its subjects.
     *
     * <p><b>One document, one query, no joins</b> — which is the entire reason sections and
     * subjects are embedded rather than collections of their own.
     *
     * <p><b>Read-only: no gates, no {@code @Transactional}.</b> Looking at a class is not an
     * action on it, and a school that has stopped paying still has to be able to read its own
     * records. Which makes the year check below the only thing that answers
     * {@code 404 ACADEMIC_YEAR_NOT_FOUND} here — on the writes, gate 4 answers first.
     */
    public SchoolClassDetailResponse getClass(String academicYear, String classId) {
        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure, which is the whole point of reads running no gates.
        School school = currentSchool.require();

        //! step 2 - the year, then the class. In that order, and never the other way: an unknown
        //! year answering CLASS_NOT_FOUND is true and says the wrong thing about what is wrong.
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 3 - one document, and everything this returns is already in it
        return SchoolClassDetailResponse.fromSchoolClass(utils.loadClass(school, year, classId));
    }

    //! Endpoint 30 — just the sections -------------------------------------------------

    /**
     * A class's sections, optionally narrowed to the active ones.
     *
     * <p><b>Why this exists beside #29.</b> A "move this student" dropdown wants four fields per
     * section, and #29 hands back the class with every subject assignment behind it. The document
     * read is identical — a section is embedded, so there is nothing cheaper to read — but the
     * response is a tenth of the size, and that is the part that crosses the network.
     *
     * <p><b>{@code ?active=true} is what that dropdown actually sends.</b> A retired section
     * still holds its {@code sectionNo} and still appears in the unfiltered list, because
     * records reference it — but nobody should be placed in one.
     *
     * <p>Read-only, like #29, and for the same reasons.
     *
     * @param active {@code null} for every section, which is not the same as {@code false}
     */
    public SectionListResponse listSections(String academicYear, String classId, Boolean active) {
        //! step 1 - who is asking. `require`, as #29.
        School school = currentSchool.require();

        //! step 2 - the year, then the class, in that order
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 3 - the same document #29 reads. What differs is the response, not the query.
        return SectionListResponse.forRead(utils.loadClass(school, year, classId), active);
    }

    //! Endpoint 22 — assign a subject to a class --------------------------------------

    /**
     * Assigns a subject to a class, or to one section of it.
     *
     * <p><b>The row key is the pair {@code (subjectCode, sectionNo)}.</b> A class-wide assignment
     * has a null {@code sectionNo}; a per-section one names it. Both live in the same embedded
     * list, so the uniqueness check is on the pair and not the code.
     *
     * <p><b>A subject is class-wide OR per-section, never both — settled here.</b> The plan left
     * it open and this is where it had to be answered, because this is the only endpoint that can
     * create the mixture. See {@code SubjectCreateRequest} for the reasoning; the short version
     * is that a section studies its own rows <i>plus</i> the class's, so both would give one
     * section the subject twice with nothing to say which row wins.
     *
     * <p><b>Three references, and all three can finally be checked.</b>
     * {@code GradingSchemeRepository} was built with this endpoint — the last of the three the
     * module's README listed as missing — so a teacher, a grading scheme and the section are all
     * verified to exist and to belong to this school.
     */
    @Transactional
    public SubjectListResponse addSubject(String academicYear, String classId,
            SubjectCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year, then the class, in that order
        String year = utils.requireAcademicYear(school, academicYear);

        //! step 3 - the class the subject is being added to
        SchoolClass schoolClass = utils.loadClass(school, year, classId);

        //! step 4 - the code to store. Uppercased and normalized, unlike sectionNo: a subject
        //! has `name` for display, which leaves the code free to be a code.
        String subjectCode = TextHelper.toCode(request.subjectCode(), 40);
        if (subjectCode.isEmpty()) {
            throw ApiException.conflict("SUBJECT_CODE_INVALID",
                    "A subject code must contain at least one letter or digit. Received: '"
                            + request.subjectCode() + "'.");
        }

        //! step 5 - a named section has to be one this class actually has. Assigning a subject to
        //! a section that does not exist is a typo that would sit there until somebody built a
        //! timetable and found a subject taught to nobody.
        List<ClassSection> sections = schoolClass.getSections() == null
                ? List.of()
                : schoolClass.getSections();
        String sectionNo = TextHelper.blankToNull(request.sectionNo());

        if (sectionNo != null) {
            String wanted = sectionNo;
            boolean exists = sections.stream()
                    .anyMatch(one -> one.getSectionNo() != null
                            && one.getSectionNo().equalsIgnoreCase(wanted));
            if (!exists) {
                throw ApiException.notFound("SECTION_NOT_FOUND",
                        "'" + schoolClass.getName() + "' has no section '" + sectionNo
                                + "'. Add it first, or leave sectionNo out to assign the subject "
                                + "to the whole class.");
            }
            // Stored as the class spells it, not as the caller typed it: sectionNo is the
            // display value, and two spellings of one section would read as two sections.
            sectionNo = sections.stream()
                    .filter(one -> one.getSectionNo() != null
                            && one.getSectionNo().equalsIgnoreCase(wanted))
                    .map(ClassSection::getSectionNo)
                    .findFirst()
                    .orElse(sectionNo);
        }

        //! step 6 - the pair must be free, and the subject must not already be assigned the
        //! OTHER way round. Mongo cannot make an array's contents unique, so these two checks
        //! are the only guard there is.
        List<ClassSubject> subjects = schoolClass.getSubjects() == null
                ? new ArrayList<>()
                : schoolClass.getSubjects();

        for (ClassSubject existing : subjects) {
            if (existing.getSubjectCode() == null
                    || !existing.getSubjectCode().equalsIgnoreCase(subjectCode)) {
                continue;
            }

            boolean existingIsClassWide = existing.getSectionNo() == null;
            boolean addingClassWide = sectionNo == null;

            if (existingIsClassWide && addingClassWide) {
                throw ApiException.conflict("SUBJECT_ALREADY_ASSIGNED",
                        "'" + schoolClass.getName() + "' already teaches " + subjectCode
                                + " to the whole class.");
            }

            if (!existingIsClassWide && !addingClassWide
                    && existing.getSectionNo().equalsIgnoreCase(sectionNo)) {
                throw ApiException.conflict("SUBJECT_ALREADY_ASSIGNED",
                        "Section " + sectionNo + " of '" + schoolClass.getName()
                                + "' already studies " + subjectCode + ".");
            }

            // The mixture. Refused rather than allowed with an undefined precedence - see the
            // request DTO for why, and why relaxing this needs a rule rather than a deletion.
            if (existingIsClassWide != addingClassWide) {
                throw ApiException.conflict("SUBJECT_ASSIGNMENT_CONFLICT",
                        addingClassWide
                                ? subjectCode + " is already assigned to individual sections of '"
                                        + schoolClass.getName() + "', so it cannot also be "
                                        + "assigned to the whole class. A section studies its own "
                                        + "subjects and the class's, so it would get this one "
                                        + "twice."
                                : subjectCode + " is already assigned to the whole of '"
                                        + schoolClass.getName() + "', so section " + sectionNo
                                        + " already studies it. Remove the class-wide assignment "
                                        + "first if each section needs its own teacher.");
            }
        }

        //! step 7 - the teachers. Each checked with schoolId in the query: another school's
        //! staff id is real, and a lookup without the tenant would accept them.
        List<String> teachers = new ArrayList<>();
        for (String raw : request.teacherDocsIds() == null ? List.<String>of()
                : request.teacherDocsIds()) {

            String teacherId = TextHelper.blankToNull(raw);
            if (teacherId == null) {
                continue;
            }
            if (teachers.contains(teacherId)) {
                throw ApiException.badRequest("DUPLICATE_TEACHER",
                        "'" + teacherId + "' appears twice in teacherDocsIds. A list that "
                                + "quietly loses an entry is one nobody notices.");
            }
            // TODO: read staff
            staff.findByIdAndSchoolId(teacherId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + teacherId + "' in this school."));
            teachers.add(teacherId);
        }

        //! step 8 - the grading scheme, when one was named
        String schemeId = TextHelper.blankToNull(request.gradingSchemeDocsId());
        if (schemeId != null) {
            // TODO: read grading scheme
            gradingSchemes.findByIdAndSchoolId(schemeId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("GRADING_SCHEME_NOT_FOUND",
                            "No grading scheme with id '" + schemeId + "' in this school."));
        }

        //! step 9 - append it, and set the list back for the explicit-null case
        subjects.add(ClassSubject.builder()
                .subjectCode(subjectCode)
                .name(request.name().trim())
                .shortName(TextHelper.blankToNull(request.shortName()))
                .subjectType(request.subjectType())
                .sectionNo(sectionNo)
                .teacherDocsIds(teachers)
                .gradingSchemeDocsId(schemeId)
                .active(true)
                .build());
        schoolClass.setSubjects(subjects);

        //! step 10 - save
        // TODO: update school class (append a subject)
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SubjectListResponse.fromSchoolClass(saved,
                subjectCode + " added to '" + saved.getName() + "'"
                        + (sectionNo == null ? " for every section" : " for section " + sectionNo)
                        + ". " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #24 — change one assignment's name, short name, type or grading scheme.
     *
     * <p><b>The row is found by the pair, never by the code alone.</b> {@code MATHEMATICS} may be
     * on the class twice — once class-wide, once for a section — and an edit that matched the
     * first row it found would silently rewrite whichever happened to be stored first.
     */
    public SubjectListResponse updateSubject(String academicYear, String classId,
            String subjectCodeInPath, String sectionNoInQuery, SubjectUpdateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing, before reading anything. A PATCH that
        //! changes nothing and answers 200 lets a client with a broken form look healthy.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send name, shortName, subjectType or gradingSchemeDocsId.");
        }

        //! step 3 - the year, then the class, in that order. A bad year says so rather than
        //! answering CLASS_NOT_FOUND, which is true and says the wrong thing about what is wrong.
        //!
        //! THIS CHECK CANNOT FIRE THROUGH THE CONTROLLER, and it is kept anyway. Gate 4 loads the
        //! year by name and 404s first, so the two are indistinguishable from outside - measured
        //! 2026-09-11 by removing each in turn: with gate 4 alone, or this alone, an unknown year
        //! is ACADEMIC_YEAR_NOT_FOUND; with NEITHER it becomes CLASS_NOT_FOUND, which is the
        //! wrong answer. A mutation that deletes this line therefore passes every black-box test.
        //! Do not "clean it up": it is what makes the service right when called from anywhere
        //! that is not this controller.
        String year = utils.requireAcademicYear(school, academicYear);
        SchoolClass schoolClass = utils.loadClass(school, year, classId);

        //! step 4 - the key, normalised the same way #22 stored it. Without this, the URL that
        //! created a subject would not find it: #22 stores "maths-2" as MATHS_2.
        String subjectCode = TextHelper.toCode(subjectCodeInPath, 40);
        String sectionNo = TextHelper.blankToNull(sectionNoInQuery);

        //! step 5 - the one row that pair names. Blank ?sectionNo= is the same as leaving it off:
        //! the class-wide row, which is the ordinary case and should not need the parameter.
        List<ClassSubject> subjects = schoolClass.getSubjects() == null
                ? new ArrayList<>()
                : schoolClass.getSubjects();

        ClassSubject subject = subjects.stream()
                .filter(one -> one.getSubjectCode() != null
                        && one.getSubjectCode().equalsIgnoreCase(subjectCode)
                        && (sectionNo == null
                                ? one.getSectionNo() == null
                                : one.getSectionNo() != null
                                        && one.getSectionNo().equalsIgnoreCase(sectionNo)))
                .findFirst()
                .orElseThrow(() -> {
                    // The subject may well be on the class the OTHER way round — #22 forbids
                    // both at once, so at most one exists. "Not found" is true and unhelpful on
                    // its own; saying which one does exist is the difference between a caller
                    // fixing the URL and a caller thinking the assignment is gone.
                    String otherWayRound = subjects.stream()
                            .filter(one -> one.getSubjectCode() != null
                                    && one.getSubjectCode().equalsIgnoreCase(subjectCode))
                            .findFirst()
                            .map(one -> one.getSectionNo() == null
                                    ? " It is assigned to the whole class — drop ?sectionNo= to"
                                            + " edit that row."
                                    : " It is assigned to section " + one.getSectionNo()
                                            + " — use ?sectionNo=" + one.getSectionNo() + ".")
                            .orElse("");

                    return ApiException.notFound("SUBJECT_NOT_FOUND",
                            "'" + schoolClass.getName() + "' has no " + subjectCode
                                    + (sectionNo == null
                                            ? " assigned to the whole class."
                                            : " assigned to section " + sectionNo + ".")
                                    + otherWayRound);
                });

        //! step 6 - the name. Blank is refused rather than clearing, because the model requires
        //! one and it is the only thing on the row a person reads.
        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("SUBJECT_NAME_REQUIRED",
                        "A subject name cannot be removed. Send a new one, or omit the field.");
            }
            subject.setName(newName);
        }

        //! step 7 - the short name. "" removes it; absent leaves it alone.
        if (request.shortName() != null) {
            subject.setShortName(TextHelper.blankToNull(request.shortName()));
        }

        //! step 8 - the type. An enum, so Jackson has already refused anything outside the five,
        //! and there is no way to send a blank one - which is right: the model requires it.
        if (request.subjectType() != null) {
            subject.setSubjectType(request.subjectType());
        }

        //! step 9 - the grading scheme. "" hands the decision back to the exam; a value is
        //! checked with schoolId in the query, because another school's id is real.
        if (request.gradingSchemeDocsId() != null) {
            String schemeId = TextHelper.blankToNull(request.gradingSchemeDocsId());
            if (schemeId != null) {
                // TODO: read grading scheme
                gradingSchemes.findByIdAndSchoolId(schemeId, school.getId())
                        .orElseThrow(() -> ApiException.notFound("GRADING_SCHEME_NOT_FOUND",
                                "No grading scheme with id '" + schemeId + "' in this school."));
            }
            subject.setGradingSchemeDocsId(schemeId);
        }

        //! step 10 - save. The row was edited in place, so the list is already the new one.
        schoolClass.setSubjects(subjects);
        // TODO: update school class (edit one subject)
        SchoolClass saved = schoolClasses.save(schoolClass);

        return SubjectListResponse.fromSchoolClass(saved,
                subjectCode + (sectionNo == null ? " (whole class)" : " (section " + sectionNo + ")")
                        + " updated in '" + saved.getName() + "'. The code, the section and the "
                        + "teachers are untouched — the first two are the key, and the teachers "
                        + "are #25. " + NO_AUTHORIZATION_YET);
    }
}
