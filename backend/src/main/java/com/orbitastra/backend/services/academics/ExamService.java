package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.exam.request.ExamCreateRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamSearchRequest;
import com.orbitastra.backend.dto.academics.exam.request.ExamUpdateRequest;
import com.orbitastra.backend.dto.academics.exam.response.ExamDetailResponse;
import com.orbitastra.backend.dto.academics.exam.response.ExamResponse;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exams — one exam event, such as "Half Yearly Examination, Term 1". Endpoints #1, #2, #6 and #7
 * of the plan in {@code controllers/academics/examination}; the rest are not built.
 *
 * <p>The year is read through the timetable's utils, the same as attendance does, because it is
 * the same question about the same year.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final AcademicTermRepository academicTerms;
    private final GradingSchemeRepository gradingSchemes;
    private final ExamScheduleRepository examSchedules;
    private final StudentMarkRepository studentMarks;
    private final DailyTimetableServiceUtils utils;

    /**
     * What {@code ?sort=} accepts, keyed by the lower-cased name a caller types. Only these, so a
     * caller cannot sort on a field the database would have to read every row to order by.
     */
    private static final Map<String, String> SORTABLE_EXAM_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_EXAM_FIELDS.put("startdate", "startDate");
        SORTABLE_EXAM_FIELDS.put("enddate", "endDate");
        SORTABLE_EXAM_FIELDS.put("name", "name");
        SORTABLE_EXAM_FIELDS.put("createdat", "createdAt");
        SORTABLE_EXAM_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_EXAM_FIELD_NAMES =
            String.join(", ", SORTABLE_EXAM_FIELDS.values());

    /**
     * The default order: the exams in the order they happen. The name comes last because it is
     * unique in the year, so no two rows tie and paging never shows a row twice.
     */
    private static final Sort EXAM_ORDER = Sort.by(Sort.Order.asc("startDate"),
            Sort.Order.asc("name"));

    /**
     * Endpoint #1 — <b>create an exam</b>. It starts as {@code DRAFT}.
     *
     * <p>The name must be free in the year, ignoring upper and lower case. The term must be an
     * active term of this year, and the exam's dates must fall inside it. A grading scheme is
     * optional; when one is sent it must be this school's and active.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    public ExamResponse createExam(String academicYear, ExamCreateRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String name = request.name().trim();
        String termId = request.termDocsId().trim();
        String schemeId = TextHelper.blankToNull(request.gradingSchemeDocsId());
        log.info("[createExam] Step 1: Creating exam '{}' in {} for school {}", name,
                academicYear, school.getId());

        //! Step 2 - The dates must be in the right order. Checked before anything is read.
        if (request.startDate().isAfter(request.endDate())) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "startDate (" + Dates.readable(request.startDate()) + ") is after endDate ("
                            + Dates.readable(request.endDate()) + ").");
        }

        //! Step 3 - The year must exist.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! Step 4 - The term must belong to this year and still be in use.
        // TODO: read academic term
        AcademicTerm term = academicTerms
                .findByIdAndSchoolIdAndAcademicYear(termId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("TERM_NOT_FOUND",
                        "No term with id '" + termId + "' in '" + year.getName() + "'."));

        if (Boolean.FALSE.equals(term.getActive())) {
            throw ApiException.conflict("TERM_NOT_ACTIVE",
                    "Term '" + term.getName() + "' has been retired, so no new exam can be added "
                            + "to it.");
        }

        //! Step 5 - The exam's dates must fall inside the term.
        if (request.startDate().isBefore(term.getStartDate())
                || request.endDate().isAfter(term.getEndDate())) {
            throw ApiException.conflict("EXAM_DATES_OUTSIDE_TERM",
                    "Term '" + term.getName() + "' runs from " + Dates.readable(term.getStartDate())
                            + " to " + Dates.readable(term.getEndDate())
                            + ", so the exam has to fall between those dates.");
        }

        //! Step 6 - When a grading scheme is sent, it must be this school's and still in use.
        String schemeName = null;
        if (schemeId != null) {
            // TODO: read grading scheme
            GradingScheme scheme = gradingSchemes.findByIdAndSchoolId(schemeId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("GRADING_SCHEME_NOT_FOUND",
                            "No grading scheme with id '" + schemeId + "' in this school."));

            if (Boolean.FALSE.equals(scheme.getActive())) {
                throw ApiException.conflict("GRADING_SCHEME_NOT_ACTIVE",
                        "Grading scheme '" + scheme.getName() + "' has been retired, so it cannot "
                                + "be used for a new exam.");
            }
            schemeName = scheme.getName();
        }

        //! Step 7 - The name must be free in this year, ignoring upper and lower case.
        log.info("[createExam] Step 2: Making sure no other exam in {} is called '{}'",
                year.getName(), name);
        // TODO: read exam (same name in this year)
        exams.findFirstBySchoolIdAndAcademicYearAndNameIgnoreCase(school.getId(), year.getName(),
                name).ifPresent(other -> {
                    throw ApiException.conflict("EXAM_NAME_TAKEN",
                            "This year already has an exam called '" + other.getName() + "'"
                                    + (other.getStatus() == ExamStatus.CANCELLED ? " (cancelled)" : "")
                                    + ". Choose another name.");
                });

        //! Step 8 - Build the exam. It always starts as DRAFT.
        Exam exam = Exam.builder()
                .schoolId(school.getId())
                .academicYear(year.getName())
                .name(name)
                .termDocsId(term.getId())
                .examType(request.examType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .weightPercent(request.weightPercent())
                .gradingSchemeDocsId(schemeId)
                .status(ExamStatus.DRAFT)
                .build();

        //! Step 9 - Save it.
        // TODO: insert exam
        Exam saved = exams.save(exam);
        log.info("[createExam] Step 3: Saved exam '{}' (id {})", saved.getName(), saved.getId());

        return ExamResponse.of(saved, term.getName(), schemeName, 0,
                "Next, add the exam's papers — POST /exams/" + saved.getId()
                        + "/schedules (#8, not built yet).");
    }

    /**
     * Endpoint #6 — <b>the year's exams</b>, filtered and paged, each with its term, its grading
     * scheme and how many papers it has.
     *
     * <p>Every filter is optional: {@code termDocsId}, {@code examType}, {@code status} and
     * {@code search} (part of the name, ignoring case). Ordered by start date unless
     * {@code ?sort=} says otherwise.
     *
     * <p><b>No gates.</b> A read. Four reads: the year, the page, the year's terms and the page's
     * schemes for their names, and the paper counts.
     */
    public PageResponse<ExamResponse> listExams(String academicYear, ExamSearchRequest request) {

        //! Step 1 - Check the paging and the order before anything is read.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_EXAM_FIELDS, SORTABLE_EXAM_FIELD_NAMES, EXAM_ORDER);

        //! Step 2 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[listExams] Step 1: Listing the exams of {} for school {}", academicYear,
                school.getId());

        //! Step 3 - The year must exist, so an unknown year is a 404 rather than an empty page.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! Step 4 - One page of the year's exams.
        // TODO: search exams
        Page<Exam> page = exams.search(school.getId(), year.getName(), request, pageable);

        //! Step 5 - The names behind the term and scheme ids, and each exam's paper count.
        //! The year has only a few terms, so they are all read at once.
        // TODO: read academic terms
        Map<String, String> termNames = academicTerms
                .findBySchoolIdAndAcademicYearOrderBySequenceAsc(school.getId(), year.getName())
                .stream()
                .collect(Collectors.toMap(AcademicTerm::getId, AcademicTerm::getName));

        List<String> schemeIds = page.getContent().stream()
                .map(Exam::getGradingSchemeDocsId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> schemeNames = new LinkedHashMap<>();
        if (!schemeIds.isEmpty()) {
            // TODO: read grading schemes (the ones on this page)
            for (GradingScheme scheme : gradingSchemes.findBySchoolIdAndIdIn(school.getId(),
                    schemeIds)) {
                schemeNames.put(scheme.getId(), scheme.getName());
            }
        }

        // TODO: count exam schedules (papers per exam)
        Map<String, Long> paperCounts = examSchedules.countPapersByExam(school.getId(),
                page.getContent().stream().map(Exam::getId).toList());

        log.info("[listExams] Step 2: Found {} exam(s) in all; returning {}",
                page.getTotalElements(), page.getNumberOfElements());

        return PageResponse.from(page, exam -> ExamResponse.of(exam,
                termNames.get(exam.getTermDocsId()),
                schemeNames.get(exam.getGradingSchemeDocsId()),
                paperCounts.getOrDefault(exam.getId(), 0L),
                null));
    }

    /**
     * Endpoint #7 — <b>one exam in full</b>: the exam with its term and scheme named, its papers
     * per status and the marks entered against them per status.
     *
     * <p>Every status is listed in both counts, with 0 where there are none, so a screen can show a
     * fixed set of numbers. <b>No gates.</b> A read.
     */
    public ExamDetailResponse getExam(String academicYear, String examDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String examId = examDocsId == null ? "" : examDocsId.trim();
        log.info("[getExam] Step 1: Reading exam {} of {} for school {}", examId, academicYear,
                school.getId());

        //! Step 2 - The year, then the exam inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(examId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND",
                        "No exam with id '" + examId + "' in '" + year.getName() + "'."));

        //! Step 3 - The names behind the term and the scheme. A term or scheme that has gone is
        //! shown by its id alone rather than failing the read.
        // TODO: read academic term
        String termName = academicTerms
                .findByIdAndSchoolIdAndAcademicYear(exam.getTermDocsId(), school.getId(),
                        year.getName())
                .map(AcademicTerm::getName)
                .orElse(null);

        String schemeName = null;
        if (exam.getGradingSchemeDocsId() != null) {
            // TODO: read grading scheme
            schemeName = gradingSchemes
                    .findByIdAndSchoolId(exam.getGradingSchemeDocsId(), school.getId())
                    .map(GradingScheme::getName)
                    .orElse(null);
        }

        //! Step 4 - How far along it is: papers per status, then marks per status.
        // TODO: count exam schedules (per status)
        Map<String, Long> paperCounts = examSchedules.countByStatusForExam(school.getId(), examId);
        // TODO: count student marks (per status)
        Map<String, Long> markCounts = studentMarks.countByStatusForExam(school.getId(), examId);

        //! Step 5 - Every status, in order, with 0 where there are none.
        Map<String, Long> papersByStatus = new LinkedHashMap<>();
        for (ExamScheduleStatus status : ExamScheduleStatus.values()) {
            papersByStatus.put(status.name(), paperCounts.getOrDefault(status.name(), 0L));
        }
        Map<String, Long> marksByStatus = new LinkedHashMap<>();
        for (MarkEntryStatus status : MarkEntryStatus.values()) {
            marksByStatus.put(status.name(), markCounts.getOrDefault(status.name(), 0L));
        }

        long paperTotal = papersByStatus.values().stream().mapToLong(Long::longValue).sum();
        long markTotal = marksByStatus.values().stream().mapToLong(Long::longValue).sum();
        long livePapers = paperTotal - papersByStatus.get(ExamScheduleStatus.CANCELLED.name());

        log.info("[getExam] Step 2: Exam '{}' has {} paper(s) and {} mark(s)", exam.getName(),
                paperTotal, markTotal);

        return new ExamDetailResponse(
                ExamResponse.of(exam, termName, schemeName, livePapers, null),
                paperTotal, papersByStatus, markTotal, marksByStatus);
    }

    /**
     * Endpoint #2 — <b>fix an exam</b>: its name, term, dates, weight or grading scheme. A field
     * left out is left as it is; {@code version} is required.
     *
     * <p><b>The term only changes while the exam has no papers</b>, because its papers sit inside
     * the exam's dates. New dates must stay inside the term and still hold every paper. A
     * completed or cancelled exam cannot be changed.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    public ExamResponse updateExam(String academicYear, String examDocsId,
            ExamUpdateRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String examId = examDocsId == null ? "" : examDocsId.trim();
        log.info("[updateExam] Step 1: Changing exam {} of {} for school {}", examId,
                academicYear, school.getId());

        //! Step 2 - There has to be something to change, and a name cannot be blanked.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send at least one of name, termDocsId, startDate, endDate, weightPercent or "
                            + "gradingSchemeDocsId.");
        }
        if (request.name() != null && request.name().isBlank()) {
            throw ApiException.badRequest("EXAM_NAME_REQUIRED",
                    "A name can be changed, but not removed.");
        }

        //! Step 3 - The year, then the exam inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(examId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND",
                        "No exam with id '" + examId + "' in '" + year.getName() + "'."));

        //! Step 4 - Make sure nobody else changed it since it was read, and that it can still change.
        if (!request.version().equals(exam.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This exam was changed by someone else. Please read it again before changing it.");
        }
        if (exam.getStatus() == ExamStatus.COMPLETED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw ApiException.conflict("EXAM_NOT_EDITABLE",
                    "This exam is " + exam.getStatus() + ", so it can no longer be changed.");
        }

        //! Step 5 - The term. Moving to another term is only allowed while the exam has no papers.
        String sentTermId = TextHelper.blankToNull(request.termDocsId());
        boolean termChanges = sentTermId != null && !sentTermId.equals(exam.getTermDocsId());
        if (termChanges) {
            // TODO: check exam schedules exist
            if (examSchedules.existsBySchoolIdAndExamDocsId(school.getId(), examId)) {
                throw ApiException.conflict("EXAM_HAS_PAPERS",
                        "This exam already has papers, so it cannot move to another term.");
            }
        }
        String termId = termChanges ? sentTermId : exam.getTermDocsId();
        // TODO: read academic term
        AcademicTerm term = academicTerms
                .findByIdAndSchoolIdAndAcademicYear(termId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("TERM_NOT_FOUND",
                        "No term with id '" + termId + "' in '" + year.getName() + "'."));
        if (termChanges && Boolean.FALSE.equals(term.getActive())) {
            throw ApiException.conflict("TERM_NOT_ACTIVE",
                    "Term '" + term.getName() + "' has been retired, so no exam can move to it.");
        }

        //! Step 6 - The dates, as a pair: one sent alone keeps the other, then both are checked.
        LocalDate start = request.startDate() != null ? request.startDate() : exam.getStartDate();
        LocalDate end = request.endDate() != null ? request.endDate() : exam.getEndDate();
        boolean datesChange = !start.equals(exam.getStartDate()) || !end.equals(exam.getEndDate());

        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "startDate (" + Dates.readable(start) + ") is after endDate ("
                            + Dates.readable(end) + ").");
        }
        if ((datesChange || termChanges)
                && (start.isBefore(term.getStartDate()) || end.isAfter(term.getEndDate()))) {
            throw ApiException.conflict("EXAM_DATES_OUTSIDE_TERM",
                    "Term '" + term.getName() + "' runs from " + Dates.readable(term.getStartDate())
                            + " to " + Dates.readable(term.getEndDate())
                            + ", so the exam has to fall between those dates.");
        }

        //! Step 7 - New dates must still hold every paper. Cancelled papers do not count.
        if (datesChange) {
            // TODO: read exam schedules (papers outside the new dates)
            List<ExamSchedule> outside = examSchedules.findPapersOutside(school.getId(), examId,
                    ExamScheduleStatus.CANCELLED, start, end);
            if (!outside.isEmpty()) {
                throw ApiException.conflict("PAPERS_OUTSIDE_DATES",
                        outside.size() + " paper(s) fall outside " + Dates.readable(start) + " to "
                                + Dates.readable(end) + ": " + outside.stream()
                                        .map(one -> one.getSubjectCode() + " " + one.getSectionNo()
                                                + " on " + Dates.readable(one.getExamDate()))
                                        .collect(Collectors.joining(", "))
                                + ". Move those papers first.");
            }
        }

        //! Step 8 - The grading scheme. An empty string removes it; an id must be this school's
        //! and still in use.
        String schemeId = exam.getGradingSchemeDocsId();
        String schemeName = null;
        if (request.gradingSchemeDocsId() != null) {
            schemeId = TextHelper.blankToNull(request.gradingSchemeDocsId());
        }
        if (schemeId != null) {
            String lookFor = schemeId;
            // TODO: read grading scheme
            GradingScheme scheme = gradingSchemes.findByIdAndSchoolId(lookFor, school.getId())
                    .orElseThrow(() -> ApiException.notFound("GRADING_SCHEME_NOT_FOUND",
                            "No grading scheme with id '" + lookFor + "' in this school."));
            boolean schemeChanges = !lookFor.equals(exam.getGradingSchemeDocsId());
            if (schemeChanges && Boolean.FALSE.equals(scheme.getActive())) {
                throw ApiException.conflict("GRADING_SCHEME_NOT_ACTIVE",
                        "Grading scheme '" + scheme.getName() + "' has been retired, so it "
                                + "cannot be chosen now.");
            }
            schemeName = scheme.getName();
        }

        //! Step 9 - A new name must be free in the year, ignoring case. The exam may keep its own.
        String name = request.name() != null ? request.name().trim() : exam.getName();
        if (!name.equalsIgnoreCase(exam.getName())) {
            log.info("[updateExam] Step 2: Making sure no other exam in {} is called '{}'",
                    year.getName(), name);
            // TODO: read exam (same name in this year)
            exams.findFirstBySchoolIdAndAcademicYearAndNameIgnoreCase(school.getId(),
                    year.getName(), name).ifPresent(other -> {
                        throw ApiException.conflict("EXAM_NAME_TAKEN",
                                "This year already has an exam called '" + other.getName() + "'"
                                        + (other.getStatus() == ExamStatus.CANCELLED ? " (cancelled)" : "")
                                        + ". Choose another name.");
                    });
        }

        //! Step 10 - Apply the changes.
        exam.setName(name);
        exam.setTermDocsId(term.getId());
        exam.setStartDate(start);
        exam.setEndDate(end);
        if (request.weightPercent() != null) {
            exam.setWeightPercent(request.weightPercent());
        }
        exam.setGradingSchemeDocsId(schemeId);

        //! Step 11 - Save it. A save racing another change fails on the version.
        // TODO: update exam
        Exam saved = exams.save(exam);
        log.info("[updateExam] Step 3: Saved exam '{}' (id {})", saved.getName(), saved.getId());

        // TODO: count exam schedules (papers of this exam)
        long paperCount = examSchedules.countPapersByExam(school.getId(), List.of(saved.getId()))
                .getOrDefault(saved.getId(), 0L);

        return ExamResponse.of(saved, term.getName(), schemeName, paperCount, null);
    }
}
