package com.orbitastra.backend.services.academics.examination;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.current.CurrentUserResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.studentmark.request.StudentMarksEnterRequest;
import com.orbitastra.backend.dto.academics.studentmark.request.StudentMarksPublishRequest;
import com.orbitastra.backend.dto.academics.studentmark.response.ExamMarkSheetResponse;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.StudentMark;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.services.academics.grading.helper.GradingHelper;
import com.orbitastra.backend.services.academics.timetable.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.examination.utils.ExamAttendanceServiceUtils;
import com.orbitastra.backend.services.academics.examination.utils.StudentMarkServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Marks — what each child scored on each paper they sat. Endpoints #18, #19 and #22 of the plan
 * in {@code controllers/academics/examination}; #20, #21, #24 and #25 are not built, and #23 is not
 * being built — a mark is DRAFT or PUBLISHED, nothing between.
 *
 * <p>A mark exists only for a child who sat: absent, unfair means and exempt stay on the
 * candidate's hall entry, never on a mark.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentMarkService {

    private final CurrentSchoolResolver currentSchool;
    private final CurrentUserResolver currentUser;
    private final ExamScheduleRepository examSchedules;
    private final StudentMarkRepository studentMarks;
    private final GradingHelper gradingHelper;
    private final DailyTimetableServiceUtils utils;
    private final ExamAttendanceServiceUtils hallUtils;
    private final StudentMarkServiceUtils markUtils;

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

        //! Step 2 - The year, then the paper inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));

        //! Step 3 - The paper's exam, class, subject and scheme; who sat it; then the sheet.
        StudentMarkServiceUtils.PaperContext context = markUtils.contextOf(school, year.getName(), paper);
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        ExamMarkSheetResponse sheet = markUtils.markSheetOf(school, paper, roster, context);

        log.info("[getMarkSheet] Step 2: Paper {} — {} on the sheet, {} present, {} mark(s) entered",
                paperId, sheet.counts().listed(), sheet.counts().present(), sheet.counts().entered());
        return sheet;
    }

    /**
     * Endpoint #19 — <b>enter marks</b> on one paper: one child, several or all. Re-sending a
     * child corrects their mark; grace already given is kept.
     *
     * <p>Each mark is either {@code obtainedMarks} — 0 to the paper's maximum, the grade then
     * worked out from the paper's scheme — or a {@code gradeCode} on its own, which must be a band
     * of that scheme. Only a child marked PRESENT in the hall gets a mark, only on a COMPLETED
     * paper, only while the paper's marks are still DRAFT (not PUBLISHED), and never once results are
     * locked.
     * Everything is checked before anything is written; the paper's {@code version} is required.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public ExamMarkSheetResponse enterMarks(String academicYear, String examScheduleDocsId,
            StudentMarksEnterRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[enterMarks] Step 1: Entering {} mark(s) on paper {} for school {}",
                request.marks().size(), paperId, school.getId());

        //! Step 2 - The request on its own: each child once, and marks or a grade — one, not both.
        Set<String> seen = new LinkedHashSet<>();
        List<String> twice = new ArrayList<>();
        for (StudentMarksEnterRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            if (!seen.add(childId)) {
                twice.add(childId);
            }
            boolean hasMarks = mark.obtainedMarks() != null;
            boolean hasGrade = mark.gradeCode() != null && !mark.gradeCode().isBlank();
            if (hasMarks && hasGrade) {
                throw ApiException.badRequest("MARKS_AND_GRADE_BOTH_SENT",
                        "Child " + childId + " has both obtainedMarks and gradeCode. Send the marks — "
                                + "the grade is worked out from them — or, for a grade-only subject, "
                                + "the grade alone.");
            }
            if (!hasMarks && !hasGrade) {
                throw ApiException.badRequest("MARK_REQUIRED",
                        "Child " + childId + " has neither obtainedMarks nor gradeCode.");
            }
        }
        if (!twice.isEmpty()) {
            throw ApiException.badRequest("DUPLICATE_STUDENT_IN_REQUEST",
                    "Each child once: " + String.join(", ", twice) + " is in marks more than once.");
        }

        //! Step 3 - The year, the paper, and its version.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));
        if (!request.version().equals(paper.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This paper was saved by someone else. Please read its mark sheet again before "
                            + "entering marks.");
        }

        //! Step 4 - Hall attendance comes first: only a completed paper takes marks.
        if (paper.getStatus() != ExamScheduleStatus.COMPLETED) {
            throw ApiException.conflict("PAPER_NOT_COMPLETED",
                    "This paper is " + paper.getStatus() + ". Complete its hall register first — "
                            + "marks are entered once the paper is COMPLETED.");
        }

        //! Step 5 - Results locked for the year or the exam's term stop every change to marks.
        StudentMarkServiceUtils.PaperContext context = markUtils.contextOf(school, year.getName(), paper);
        markUtils.requireResultsOpen(school, year, context);

        //! Step 6 - The paper's marks move together: once they are PUBLISHED, none changes here.
        Map<String, StudentMark> markOf = new LinkedHashMap<>();
        // TODO: read student marks (this paper)
        for (StudentMark one : studentMarks.findBySchoolIdAndExamScheduleDocsId(school.getId(), paperId)) {
            markOf.put(one.getStudentDocsId(), one);
        }
        MarkEntryStatus moved = markOf.values().stream()
                .map(StudentMark::getStatus)
                .filter(status -> status != MarkEntryStatus.DRAFT)
                .findFirst()
                .orElse(null);
        if (moved != null) {
            throw ApiException.conflict("MARKS_NOT_EDITABLE",
                    "This paper's marks are " + moved + ", so they can no longer be changed here. "
                            + "A revaluation takes them back to DRAFT first.");
        }

        //! Step 7 - Only a child marked PRESENT at this paper gets a mark.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        List<String> notPresent = new ArrayList<>();
        Map<String, ExamCandidate> candidateOf = new LinkedHashMap<>();
        for (String childId : seen) {
            ExamCandidate candidate = roster.candidates().get(childId);
            ExamPaperAttendance entry = candidate == null || candidate.getPapers() == null ? null
                    : candidate.getPapers().stream()
                            .filter(p -> paperId.equals(p.getExamScheduleDocsId()))
                            .findFirst()
                            .orElse(null);
            if (entry == null) {
                notPresent.add(childId + " (no hall entry)");
            } else if (entry.getAttendanceStatus() != ExamAttendanceStatus.PRESENT) {
                notPresent.add(childId + " (" + entry.getAttendanceStatus() + ")");
            } else {
                candidateOf.put(childId, candidate);
            }
        }
        if (!notPresent.isEmpty()) {
            throw ApiException.badRequest("STUDENT_NOT_PRESENT",
                    "Only a child marked PRESENT at this paper gets a mark: "
                            + String.join(", ", notPresent) + ".");
        }

        //! Step 8 - Each mark within 0 to the paper's maximum; each grade a band of the scheme.
        GradingScheme scheme = context.scheme();
        BigDecimal maximum = paper.getMaximumMarks();
        for (StudentMarksEnterRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            if (mark.obtainedMarks() != null && (mark.obtainedMarks().signum() < 0
                    || mark.obtainedMarks().compareTo(maximum) > 0)) {
                throw ApiException.badRequest("MARKS_OUT_OF_RANGE",
                        "Child " + childId + " has " + mark.obtainedMarks().stripTrailingZeros().toPlainString()
                                + ", but this paper is out of " + maximum.stripTrailingZeros().toPlainString()
                                + ". A mark is 0 to " + maximum.stripTrailingZeros().toPlainString() + ".");
            }
            if (mark.gradeCode() != null && !mark.gradeCode().isBlank()) {
                String wanted = mark.gradeCode().trim();
                boolean known = scheme != null && scheme.getGradeBands().stream()
                        .anyMatch(band -> band.getGradeCode().equalsIgnoreCase(wanted));
                if (!known) {
                    throw ApiException.badRequest("GRADE_NOT_IN_SCHEME", scheme == null
                            ? "Child " + childId + " was given grade '" + wanted + "', but this paper has no "
                                    + "grading scheme — neither its subject nor its exam names one."
                            : "Child " + childId + " was given grade '" + wanted + "', which is not a band of '"
                                    + scheme.getName() + "'. Its grades are: " + String.join(", ",
                                            scheme.getGradeBands().stream().map(GradeBand::getGradeCode).toList())
                                    + ".");
                }
            }
        }

        //! Step 9 - Build each child's mark: replaced when there is one, added when not. The grade
        //! is worked out from obtained + grace, scaled onto the scheme. A scheme that cannot resolve
        //! the value — a descriptor scheme, or a gap between bands — leaves the grade empty rather
        //! than refusing the mark.
        Instant now = Instant.now();
        String enteredBy = currentUser.staffDocsIdOrNull();
        List<StudentMark> toSave = new ArrayList<>();
        for (StudentMarksEnterRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            StudentMark stored = markOf.get(childId);
            if (stored == null) {
                stored = StudentMark.builder()
                        .schoolId(school.getId())
                        .academicYear(year.getName())
                        .studentDocsId(childId)
                        .examDocsId(paper.getExamDocsId())
                        .examScheduleDocsId(paperId)
                        .studentAcademicRecordDocsId(candidateOf.get(childId).getStudentAcademicRecordDocsId())
                        .status(MarkEntryStatus.DRAFT)
                        .build();
            }
            String grade;
            if (mark.obtainedMarks() != null) {
                stored.setObtainedMarks(mark.obtainedMarks());
                grade = null;
                if (scheme != null && scheme.getScaleType() != GradingScaleType.DESCRIPTOR
                        && maximum.signum() > 0) {
                    BigDecimal effective = mark.obtainedMarks().add(stored.getGraceMarks() == null
                            ? BigDecimal.ZERO : stored.getGraceMarks());
                    BigDecimal onScale = effective.min(maximum)
                            .multiply(scheme.getMaximumValue())
                            .divide(maximum, 2, RoundingMode.HALF_UP);
                    try {
                        grade = gradingHelper.resolveBand(scheme.getScaleType(), scheme.getMaximumValue(),
                                scheme.getGradeBands(), onScale).getGradeCode();
                    } catch (ApiException noBand) {
                        log.info("[enterMarks] Step 2: No grade for {} on '{}' — {}", onScale,
                                scheme.getName(), noBand.getMessage());
                    }
                }
            } else {
                String wanted = mark.gradeCode().trim();
                stored.setObtainedMarks(null);
                grade = scheme.getGradeBands().stream()
                        .map(GradeBand::getGradeCode)
                        .filter(code -> code.equalsIgnoreCase(wanted))
                        .findFirst()
                        .orElse(wanted);
            }
            stored.setGradeCode(grade);
            stored.setRemarks(mark.remarks() == null || mark.remarks().isBlank() ? null : mark.remarks().trim());
            stored.setEnteredAt(now);
            stored.setEnteredByDocsId(enteredBy);
            toSave.add(stored);
        }

        //! Step 10 - Save the marks and move the paper's version on, together.
        // TODO: insert / update student marks
        studentMarks.saveAll(toSave);
        // TODO: update exam schedule (move its version on)
        ExamSchedule savedPaper = examSchedules.save(paper);
        log.info("[enterMarks] Step 3: Saved {} mark(s) on paper {}; its version is now {}",
                toSave.size(), paperId, savedPaper.getVersion());

        //! Step 11 - The mark sheet as it now stands — the same answer #18 gives.
        return markUtils.markSheetOf(school, savedPaper, roster, context);
    }

    /**
     * Endpoint #22 — <b>publish the paper's marks</b>: every mark on the paper moves {@code DRAFT} →
     * {@code PUBLISHED}, together, with when and by whom. They are final from here — report cards
     * read them, and #19 no longer changes them. Refused while any child who sat has no mark. No
     * row is made for a child who did not sit — absent, unfair means and exempt stay on the hall
     * entry. {@code version} is required.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public ExamMarkSheetResponse publishMarks(String academicYear, String examScheduleDocsId,
            StudentMarksPublishRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[publishMarks] Step 1: Publishing the marks of paper {} for school {}", paperId,
                school.getId());

        //! Step 2 - The year, the paper, and its version.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));
        if (!request.version().equals(paper.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This paper was saved by someone else. Please read its mark sheet again before "
                            + "publishing.");
        }

        //! Step 3 - Marks exist only once the hall is closed.
        if (paper.getStatus() != ExamScheduleStatus.COMPLETED) {
            throw ApiException.conflict("PAPER_NOT_COMPLETED",
                    "This paper is " + paper.getStatus() + ". Complete its hall register and enter "
                            + "its marks first.");
        }

        //! Step 4 - Results locked for the year or the exam's term stop every change to marks.
        StudentMarkServiceUtils.PaperContext context = markUtils.contextOf(school, year.getName(), paper);
        markUtils.requireResultsOpen(school, year, context);

        //! Step 5 - The paper's marks move together, so they must all still be DRAFT.
        // TODO: read student marks (this paper)
        List<StudentMark> marks = studentMarks.findBySchoolIdAndExamScheduleDocsId(school.getId(), paperId);
        MarkEntryStatus moved = marks.stream()
                .map(StudentMark::getStatus)
                .filter(status -> status != MarkEntryStatus.DRAFT)
                .findFirst()
                .orElse(null);
        if (moved != null) {
            throw ApiException.conflict("MARKS_NOT_EDITABLE",
                    "This paper's marks are already " + moved + ", so there is nothing to publish.");
        }

        //! Step 6 - Every child who sat needs a mark, and there has to be at least one.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        ExamMarkSheetResponse sheet = markUtils.markSheetOf(school, paper, roster, context);
        if (sheet.counts().notEntered() > 0) {
            throw ApiException.conflict("UNENTERED_MARKS",
                    sheet.counts().notEntered() + " of " + sheet.counts().present()
                            + " child(ren) who sat have no mark yet. Enter every mark before publishing.");
        }
        if (marks.isEmpty()) {
            throw ApiException.conflict("NO_MARKS_TO_PUBLISH",
                    "Nobody sat this paper, so there are no marks to publish.");
        }

        //! Step 7 - Publish every row — when, and by whom when the caller is signed in as staff —
        //! and move the paper's version on, together.
        Instant now = Instant.now();
        String publishedBy = currentUser.staffDocsIdOrNull();
        for (StudentMark one : marks) {
            one.setStatus(MarkEntryStatus.PUBLISHED);
            one.setPublishedAt(now);
            one.setPublishedByDocsId(publishedBy);
        }
        // TODO: update student marks (published)
        studentMarks.saveAll(marks);
        // TODO: update exam schedule (move its version on)
        ExamSchedule savedPaper = examSchedules.save(paper);
        log.info("[publishMarks] Step 2: Published {} mark(s) on paper {}; its version is now {}",
                marks.size(), paperId, savedPaper.getVersion());

        //! Step 8 - The mark sheet as it now stands — the same answer #18 gives.
        return markUtils.markSheetOf(school, savedPaper, roster, context);
    }
}
