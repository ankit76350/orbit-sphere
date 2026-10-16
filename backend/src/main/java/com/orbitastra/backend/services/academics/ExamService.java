package com.orbitastra.backend.services.academics;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.academics.exam.request.ExamCreateRequest;
import com.orbitastra.backend.dto.academics.exam.response.ExamResponse;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exams — one exam event, such as "Half Yearly Examination, Term 1". Endpoint #1 of the plan in
 * {@code controllers/academics/examination}; the rest are not built.
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
    private final DailyTimetableServiceUtils utils;

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

        return ExamResponse.of(saved, term.getName(), schemeName,
                "Next, add the exam's papers — POST /exams/" + saved.getId()
                        + "/schedules (#8, not built yet).");
    }
}
