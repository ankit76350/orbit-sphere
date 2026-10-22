package com.orbitastra.backend.services.academics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
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
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.academics.utils.AttendanceServiceUtils;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamAttendanceServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Marks — what each child scored on each paper they sat. Endpoint #18 of the plan in
 * {@code controllers/academics/examination}; #19–#25 are not built.
 *
 * <p>A mark exists only for a child who sat: absent, unfair means and exempt stay on the
 * candidate's hall entry, never on a mark.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentMarkService {

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final ExamScheduleRepository examSchedules;
    private final SchoolClassRepository schoolClasses;
    private final GradingSchemeRepository gradingSchemes;
    private final StudentMarkRepository studentMarks;
    private final StudentRepository students;
    private final DailyTimetableServiceUtils utils;
    private final ExamAttendanceServiceUtils hallUtils;

    /**
     * Endpoint #18 — <b>the mark sheet</b> of one paper: every child with a hall entry, in roll
     * order. A child marked PRESENT carries their mark, or "not entered"; anyone else carries only
     * their hall status and cannot be marked. Also names the grading scheme the paper's grades
     * come from — the class subject's, else the exam's.
     *
     * <p><b>No gates.</b> A read.
     */
    public ExamMarkSheetResponse getMarkSheet(String academicYear, String examScheduleDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[getMarkSheet] Step 1: Reading the mark sheet of paper {} for school {}", paperId,
                school.getId());

        //! Step 2 - The year, the paper inside it, and its exam.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(), school.getId(),
                year.getName()).orElse(null);

        //! Step 3 - The class and the subject. The subject's own grading scheme wins — one set for
        //! this section before one for the whole class — and the exam's is the fallback.
        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(paper.getClassDocsId(), school.getId(), year.getName())
                .orElse(null);
        List<ClassSubject> subjects = schoolClass == null || schoolClass.getSubjects() == null
                ? List.of() : schoolClass.getSubjects();
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
        ExamMarkSheetResponse.Scheme scheme = null;
        if (schemeId != null) {
            // TODO: read grading scheme
            GradingScheme found = gradingSchemes.findByIdAndSchoolId(schemeId, school.getId()).orElse(null);
            if (found != null) {
                scheme = new ExamMarkSheetResponse.Scheme(found.getId(), found.getName(),
                        found.getScaleType(),
                        found.getGradeBands().stream().map(GradeBand::getGradeCode).toList(),
                        schemeFrom);
            }
        }

        //! Step 4 - Who sat it: the hall's roster, kept to the children with an entry on this paper.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
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

        //! Step 5 - The marks on this paper, and the children's names.
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

        //! Step 6 - One row per child with a hall entry. Effective marks are obtained plus grace.
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

        log.info("[getMarkSheet] Step 2: Paper {} — {} on the sheet, {} present, {} mark(s) entered",
                paperId, rows.size(), present, entered);

        return new ExamMarkSheetResponse(paper.getId(), paper.getExamDocsId(),
                exam == null ? null : exam.getName(), paper.getClassDocsId(),
                schoolClass == null ? null : schoolClass.getName(), paper.getSectionNo(),
                paper.getSubjectCode(), subject == null ? null : subject.getName(),
                paper.getComponentName(), paper.getExamDate(), paper.getMaximumMarks(),
                paper.getPassingMarks(), paper.getStatus(), scheme,
                new ExamMarkSheetResponse.Counts(rows.size(), present, entered, present - entered,
                        byHall, byMark),
                rows, paper.getVersion());
    }
}
