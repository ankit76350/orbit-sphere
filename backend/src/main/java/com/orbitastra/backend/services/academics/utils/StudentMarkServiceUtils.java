package com.orbitastra.backend.services.academics.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.studentmark.response.ExamMarkSheetResponse;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.StudentMark;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the mark endpoints share. A method here never calls another method here — a caller that
 * needs the mark sheet reads the paper's context and the hall roster first and hands them in.
 */
@Component
@RequiredArgsConstructor
public class StudentMarkServiceUtils {

    private final ExamRepository exams;
    private final AcademicTermRepository academicTerms;
    private final SchoolClassRepository schoolClasses;
    private final GradingSchemeRepository gradingSchemes;
    private final StudentMarkRepository studentMarks;
    private final StudentRepository students;

    /**
     * What a paper's marks are read against.
     *
     * @param exam        the paper's exam, or null if it has gone
     * @param schoolClass the paper's class, or null if it has gone
     * @param subject     the class's subject row for this paper — this section's before a
     *                    class-wide one — or null
     * @param scheme      the grading scheme the grades come from, or null when none is named
     * @param schemeFrom  SUBJECT or EXAM — where the scheme was found
     */
    public record PaperContext(Exam exam, SchoolClass schoolClass, ClassSubject subject,
            GradingScheme scheme, String schemeFrom) {
    }

    /**
     * The exam, class, subject and grading scheme behind one paper. The scheme follows the model
     * README's order: the class subject's, else the exam's, else none.
     *
     * Used by:
     * - StudentMarkService.getMarkSheet()
     * - StudentMarkService.enterMarks()
     * - StudentMarkService.publishMarks()
     */
    public PaperContext contextOf(School school, String academicYear, ExamSchedule paper) {
        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(), school.getId(),
                academicYear).orElse(null);

        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(paper.getClassDocsId(), school.getId(), academicYear)
                .orElse(null);
        List<ClassSubject> subjects = schoolClass == null || schoolClass.getSubjects() == null
                ? List.of() : schoolClass.getSubjects();
        //! This section's own subject row wins over a class-wide one.
        ClassSubject subject = subjects.stream()
                .filter(one -> one.getSubjectCode().equalsIgnoreCase(paper.getSubjectCode()))
                .filter(one -> paper.getSectionNo().equalsIgnoreCase(one.getSectionNo()))
                .findFirst()
                .orElse(subjects.stream()
                        .filter(one -> one.getSubjectCode().equalsIgnoreCase(paper.getSubjectCode()))
                        .filter(one -> one.getSectionNo() == null)
                        .findFirst()
                        .orElse(null));

        String schemeId = null;
        String schemeFrom = null;
        if (subject != null && subject.getGradingSchemeDocsId() != null) {
            schemeId = subject.getGradingSchemeDocsId();
            schemeFrom = "SUBJECT";
        } else if (exam != null && exam.getGradingSchemeDocsId() != null) {
            schemeId = exam.getGradingSchemeDocsId();
            schemeFrom = "EXAM";
        }
        GradingScheme scheme = null;
        if (schemeId != null) {
            // TODO: read grading scheme
            scheme = gradingSchemes.findByIdAndSchoolId(schemeId, school.getId()).orElse(null);
        }
        return new PaperContext(exam, schoolClass, subject, scheme, scheme == null ? null : schemeFrom);
    }

    /**
     * Refuses when results are locked — for the whole year, or for the term the paper's exam
     * belongs to. The year is the stronger lock, so it is checked first. Every change to a mark
     * runs this.
     *
     * Used by:
     * - StudentMarkService.enterMarks()
     * - StudentMarkService.publishMarks()
     */
    public void requireResultsOpen(School school, AcademicYear year, PaperContext context) {
        if (Boolean.TRUE.equals(year.getResultsLocked())) {
            throw ApiException.conflict("RESULTS_LOCKED",
                    "The results of " + year.getName() + " are locked, so no mark can change. "
                            + "Unlock the year's results first.");
        }
        if (context.exam() == null) {
            return;
        }
        // TODO: read academic term (the exam's)
        AcademicTerm term = academicTerms.findByIdAndSchoolIdAndAcademicYear(
                context.exam().getTermDocsId(), school.getId(), year.getName()).orElse(null);
        if (term != null && Boolean.TRUE.equals(term.getResultsLocked())) {
            throw ApiException.conflict("RESULTS_LOCKED",
                    "The results of term '" + term.getName() + "' are locked, so no mark can change. "
                            + "Unlock the term's results first.");
        }
    }

    /**
     * One paper's mark sheet, from a context and a hall roster already read: every child with a
     * hall entry on the paper, in roll order, those PRESENT with their mark or none yet.
     * {@code effectiveMarks} is obtained plus grace.
     *
     * Used by:
     * - StudentMarkService.getMarkSheet()
     * - StudentMarkService.enterMarks()
     * - StudentMarkService.publishMarks()
     */
    public ExamMarkSheetResponse markSheetOf(School school, ExamSchedule paper,
            ExamAttendanceServiceUtils.HallRoster roster, PaperContext context) {
        String paperId = paper.getId();

        //! Who sat it: the roster's children with an entry on this paper.
        Map<String, ExamPaperAttendance> entryOf = new LinkedHashMap<>();
        for (ExamCandidate one : roster.candidates().values()) {
            if (one.getPapers() == null) {
                continue;
            }
            one.getPapers().stream()
                    .filter(entry -> paperId.equals(entry.getExamScheduleDocsId()))
                    .findFirst()
                    .ifPresent(entry -> entryOf.put(one.getStudentDocsId(), entry));
        }

        //! The marks on this paper, and the children's names.
        Map<String, StudentMark> markOf = new LinkedHashMap<>();
        // TODO: read student marks (this paper)
        for (StudentMark mark : studentMarks.findBySchoolIdAndExamScheduleDocsId(school.getId(), paperId)) {
            markOf.put(mark.getStudentDocsId(), mark);
        }
        Map<String, Student> children = new LinkedHashMap<>();
        if (!entryOf.isEmpty()) {
            // TODO: read students (the children on the sheet)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(), entryOf.keySet())) {
                children.put(one.getId(), one);
            }
        }

        //! One row per child with a hall entry.
        Map<String, Integer> byHall = new LinkedHashMap<>();
        for (ExamAttendanceStatus status : ExamAttendanceStatus.values()) {
            byHall.put(status.name(), 0);
        }
        Map<String, Integer> byMark = new LinkedHashMap<>();
        for (MarkEntryStatus status : MarkEntryStatus.values()) {
            byMark.put(status.name(), 0);
        }
        int present = 0;
        int entered = 0;
        List<ExamMarkSheetResponse.Row> rows = new ArrayList<>();
        for (Map.Entry<String, ExamPaperAttendance> one : entryOf.entrySet()) {
            String childId = one.getKey();
            ExamAttendanceStatus hall = one.getValue().getAttendanceStatus();
            byHall.merge(hall.name(), 1, Integer::sum);
            boolean sat = hall == ExamAttendanceStatus.PRESENT;
            StudentMark stored = markOf.get(childId);
            ExamMarkSheetResponse.Mark shown = null;
            if (stored != null) {
                byMark.merge(stored.getStatus().name(), 1, Integer::sum);
                BigDecimal effective = stored.getObtainedMarks() == null ? null
                        : stored.getObtainedMarks().add(stored.getGraceMarks() == null
                                ? BigDecimal.ZERO : stored.getGraceMarks());
                shown = new ExamMarkSheetResponse.Mark(stored.getId(), stored.getObtainedMarks(),
                        stored.getGraceMarks(), effective, stored.getGradeCode(), stored.getStatus(),
                        stored.getRemarks(), stored.getEnteredAt(), stored.getEnteredByDocsId(),
                        stored.getVersion());
            }
            if (sat) {
                present++;
                if (stored != null) {
                    entered++;
                }
            }
            Student child = children.get(childId);
            StudentAcademicRecord record = roster.sectionRecords().get(childId);
            rows.add(new ExamMarkSheetResponse.Row(childId,
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    record == null ? null : record.getRollNo(),
                    hall, sat, shown));
        }
        rows.sort(Comparator
                .comparing(ExamMarkSheetResponse.Row::rollNo, AttendanceServiceUtils.ROLL_ORDER)
                .thenComparing(ExamMarkSheetResponse.Row::fullName,
                        Comparator.nullsLast(String::compareToIgnoreCase)));

        GradingScheme scheme = context.scheme();
        ExamMarkSheetResponse.Scheme shownScheme = scheme == null ? null
                : new ExamMarkSheetResponse.Scheme(scheme.getId(), scheme.getName(), scheme.getScaleType(),
                        scheme.getGradeBands().stream().map(GradeBand::getGradeCode).toList(),
                        context.schemeFrom());

        return new ExamMarkSheetResponse(paper.getId(), paper.getExamDocsId(),
                context.exam() == null ? null : context.exam().getName(), paper.getClassDocsId(),
                context.schoolClass() == null ? null : context.schoolClass().getName(),
                paper.getSectionNo(), paper.getSubjectCode(),
                context.subject() == null ? null : context.subject().getName(),
                paper.getComponentName(), paper.getExamDate(), paper.getMaximumMarks(),
                paper.getPassingMarks(), paper.getStatus(), shownScheme,
                new ExamMarkSheetResponse.Counts(rows.size(), present, entered, present - entered,
                        byHall, byMark),
                rows, paper.getVersion());
    }
}
