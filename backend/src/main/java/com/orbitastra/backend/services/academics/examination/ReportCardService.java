package com.orbitastra.backend.services.academics.examination;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.academics.reportcard.request.ReportCardSaveRequest;
import com.orbitastra.backend.dto.academics.reportcard.response.ReportCardDraftResponse;
import com.orbitastra.backend.dto.academics.reportcard.response.ReportCardResponse;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.enums.ReportCardStatus;
import com.orbitastra.backend.models.academics.enums.ResultStatus;
import com.orbitastra.backend.models.academics.enums.SubjectType;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.ReportCard;
import com.orbitastra.backend.models.academics.examination.StudentMark;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.examination.embedded.ReportCardComponentResult;
import com.orbitastra.backend.models.academics.examination.embedded.ReportCardSubjectResult;
import com.orbitastra.backend.models.academics.grading.GradingScheme;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.attendance.AttendanceSessionRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepositoryCustom.StudentStatusCount;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examcandidate.ExamCandidateRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.academics.gradingscheme.GradingSchemeRepository;
import com.orbitastra.backend.repositories.academics.reportcard.ReportCardRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.services.academics.examination.utils.ExamCandidateServiceUtils;
import com.orbitastra.backend.services.academics.examination.utils.ReportCardServiceUtils;
import com.orbitastra.backend.services.academics.grading.helper.GradingHelper;
import com.orbitastra.backend.services.academics.timetable.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Report cards — a child's results for one term, snapshotted so a later change to marks, names or
 * schemes never changes an issued card. Made <b>one child at a time</b>: #29b works the card out
 * from the marks and the register, the school checks and edits it, and #29 saves what it sends.
 * Both are addressed by the student record — the child's placement this year. Endpoints #29 and #29b of the plan in {@code controllers/academics/examination}; #30–#35 are not
 * built.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCardService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final ExamScheduleRepository examSchedules;
    private final ExamCandidateRepository examCandidates;
    private final StudentMarkRepository studentMarks;
    private final GradingSchemeRepository gradingSchemes;
    private final StudentAcademicRecordRepository academicRecords;
    private final AttendanceSessionRepository sessions;
    private final StudentAttendanceRecordRepository attendanceMarks;
    private final ReportCardRepository reportCards;
    private final GradingHelper gradingHelper;
    private final DailyTimetableServiceUtils utils;
    private final ReportCardServiceUtils cardUtils;

    /**
     * Endpoint #29b — <b>one child's report card, worked out but not saved</b>, for the school to
     * check and edit before #29 saves it.
     *
     * <p>From every exam of the term (cancelled ones left out) and the section of the student record
     * in the path: every paper as a component of its subject — exam name and weight, the effective mark and
     * grace, or why there is none — then subject totals, grades and pass/fail, the card's total,
     * percentage, grade and result, and the attendance days from the daily register.
     *
     * <p><b>Nothing here refuses because the numbers are not final</b> — a paper not completed, marks
     * not published, weights that do not add up are returned as {@code warnings}, so the form can
     * still be filled. Weights that do not add up are left out (raw totals).
     *
     * <p><b>No gates.</b> A read.
     */
    public ReportCardDraftResponse draftCard(String academicYear, String studentAcademicRecordDocsId,
            String termDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String termId = termDocsId == null ? "" : termDocsId.trim();
        String recordId = studentAcademicRecordDocsId == null ? "" : studentAcademicRecordDocsId.trim();
        log.info("[draftCard] Step 1: Working out the card of student record {} for term {} in school {}",
                recordId, termId, school.getId());

        //! Step 2 - The year, the term, and the student record — its class, section and roll.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        ReportCardServiceUtils.CardOwner owner = cardUtils.ownerOf(school, year.getName(), termId, recordId);
        String childId = owner.student().getId();
        AcademicTerm term = owner.term();
        SchoolClass schoolClass = owner.schoolClass();
        StudentAcademicRecord record = owner.record();
        String sectionNo = record.getSectionNo();
        List<String> warnings = new ArrayList<>();
        if (Boolean.TRUE.equals(year.getResultsLocked()) || Boolean.TRUE.equals(term.getResultsLocked())) {
            warnings.add("Results are locked for " + (Boolean.TRUE.equals(year.getResultsLocked())
                    ? year.getName() : "term '" + term.getName() + "'") + " — saving will be refused.");
        }

        //! Step 3 - The term's exams, cancelled ones left out. Weights: none, or all adding to 100.
        // TODO: read exams (this term's)
        List<Exam> termExams = exams.findBySchoolIdAndAcademicYearAndTermDocsId(school.getId(), year.getName(), termId)
                .stream()
                .filter(one -> one.getStatus() != ExamStatus.CANCELLED)
                .sorted(Comparator.comparing(Exam::getStartDate))
                .toList();
        if (termExams.isEmpty()) {
            warnings.add("Term '" + term.getName() + "' has no exams, so the card has no subjects.");
        }
        List<Exam> weighted = termExams.stream().filter(one -> one.getWeightPercent() != null).toList();
        BigDecimal weightSum = weighted.stream().map(Exam::getWeightPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean isWeighted = !weighted.isEmpty() && weighted.size() == termExams.size()
                && weightSum.compareTo(HUNDRED) == 0;
        if (!weighted.isEmpty() && !isWeighted) {
            warnings.add("The term's weights do not add up — " + termExams.stream()
                    .map(one -> one.getName() + " " + (one.getWeightPercent() == null ? "unweighted"
                            : one.getWeightPercent().stripTrailingZeros().toPlainString() + "%"))
                    .collect(Collectors.joining(", "))
                    + ". Raw totals are used instead; fix the exams' weights for a weighted card.");
        }
        Map<String, Exam> examById = new LinkedHashMap<>();
        termExams.forEach(one -> examById.put(one.getId(), one));

        //! Step 4 - The section's papers across those exams, cancelled ones left out.
        List<ExamSchedule> papers = List.of();
        if (!examById.isEmpty()) {
            // TODO: read exam schedules (the section's papers in the term's exams)
            papers = examSchedules.findBySchoolIdAndExamDocsIdInAndClassDocsIdAndStatusNot(school.getId(),
                    examById.keySet(), schoolClass.getId(), ExamScheduleStatus.CANCELLED).stream()
                    .filter(one -> sectionNo.equalsIgnoreCase(one.getSectionNo()))
                    .sorted(Comparator.comparing(ExamSchedule::getExamDate).thenComparing(ExamSchedule::getStartTime))
                    .toList();
        }
        if (!examById.isEmpty() && papers.isEmpty()) {
            warnings.add("The term's exams have no papers for " + schoolClass.getName() + " " + sectionNo + ".");
        }
        List<String> paperIds = papers.stream().map(ExamSchedule::getId).toList();

        //! Step 5 - The child's candidacies, and every mark on those papers.
        Map<String, ExamCandidate> candidateOf = new LinkedHashMap<>();
        Map<String, StudentMark> markOf = new LinkedHashMap<>();
        if (!paperIds.isEmpty()) {
            // TODO: read exam candidates (this child, in the term's exams)
            for (ExamCandidate one : examCandidates.findBySchoolIdAndExamDocsIdInAndStudentDocsIdIn(school.getId(),
                    examById.keySet(), List.of(childId))) {
                candidateOf.put(one.getExamDocsId(), one);
            }
            // TODO: read student marks (the section's papers)
            for (StudentMark one : studentMarks.findBySchoolIdAndExamScheduleDocsIdIn(school.getId(), paperIds)) {
                markOf.put(one.getExamScheduleDocsId() + "|" + one.getStudentDocsId(), one);
            }
        }

        //! Step 6 - Is every paper final? Completed, every mark published — or nobody sat. Not final
        //! is a warning here; the school decides.
        for (ExamSchedule paper : papers) {
            List<StudentMark> marks = markOf.values().stream()
                    .filter(one -> paper.getId().equals(one.getExamScheduleDocsId())).toList();
            String why = paper.getStatus() != ExamScheduleStatus.COMPLETED ? "hall not completed"
                    : marks.stream().anyMatch(one -> one.getStatus() != MarkEntryStatus.PUBLISHED) ? "marks not published"
                    : null;
            if (why != null) {
                warnings.add(examById.get(paper.getExamDocsId()).getName() + " · " + paper.getSubjectCode()
                        + (paper.getComponentName() == null ? "" : " (" + paper.getComponentName() + ")") + " — " + why + ".");
            }
        }

        //! Step 7 - The grading schemes: each subject's own, each exam's, in one read.
        Set<String> schemeIds = new LinkedHashSet<>();
        termExams.stream().map(Exam::getGradingSchemeDocsId).filter(Objects::nonNull).forEach(schemeIds::add);
        List<ClassSubject> classSubjects = schoolClass.getSubjects() == null ? List.of() : schoolClass.getSubjects();
        classSubjects.stream().map(ClassSubject::getGradingSchemeDocsId).filter(Objects::nonNull).forEach(schemeIds::add);
        Map<String, GradingScheme> schemeById = new LinkedHashMap<>();
        if (!schemeIds.isEmpty()) {
            // TODO: read grading schemes (the subjects' and the exams')
            for (GradingScheme one : gradingSchemes.findBySchoolIdAndIdIn(school.getId(), schemeIds)) {
                schemeById.put(one.getId(), one);
            }
        }
        //! The card's own grade comes from the latest exam that names a scheme.
        GradingScheme cardScheme = termExams.stream()
                .sorted(Comparator.comparing(Exam::getStartDate).reversed())
                .map(one -> one.getGradingSchemeDocsId() == null ? null : schemeById.get(one.getGradingSchemeDocsId()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        //! Step 8 - The components: one per paper, grouped by subject, in datesheet order.
        boolean withheld = false;
        boolean incomplete = false;
        Map<String, List<ReportCardComponentResult>> componentsOf = new LinkedHashMap<>();
        Map<String, BigDecimal[]> rawOf = new LinkedHashMap<>();
        Map<String, String> gradeOnlyOf = new LinkedHashMap<>();
        Map<String, Map<String, BigDecimal[]>> perExamOf = new LinkedHashMap<>();
        for (ExamSchedule paper : papers) {
            Exam exam = examById.get(paper.getExamDocsId());
            ExamCandidate candidate = candidateOf.get(paper.getExamDocsId());
            ExamPaperAttendance entry = candidate == null || candidate.getPapers() == null ? null
                    : candidate.getPapers().stream()
                            .filter(p -> paper.getId().equals(p.getExamScheduleDocsId()))
                            .findFirst()
                            .orElse(null);
            StudentMark mark = markOf.get(paper.getId() + "|" + childId);
            boolean debarred = entry == null && candidate != null
                    && paper.getStatus() == ExamScheduleStatus.COMPLETED
                    && ExamCandidateServiceUtils.everDebarred(candidate);
            ExamAttendanceStatus hall = entry == null ? null : entry.getAttendanceStatus();

            BigDecimal effective = null;
            if (mark != null && mark.getObtainedMarks() != null) {
                effective = mark.getObtainedMarks().add(mark.getGraceMarks() == null ? BigDecimal.ZERO
                        : mark.getGraceMarks());
            }
            if (mark != null && mark.getObtainedMarks() == null && mark.getGradeCode() != null) {
                gradeOnlyOf.put(paper.getSubjectCode(), mark.getGradeCode());
            }
            componentsOf.computeIfAbsent(paper.getSubjectCode(), k -> new ArrayList<>())
                    .add(ReportCardComponentResult.builder()
                            .examDocsId(exam.getId())
                            .examName(exam.getName())
                            .weightPercent(isWeighted ? exam.getWeightPercent() : null)
                            .componentName(paper.getComponentName())
                            .maximumMarks(paper.getMaximumMarks())
                            .obtainedMarks(effective)
                            .graceMarks(mark == null ? null : mark.getGraceMarks())
                            .attendanceStatus(hall)
                            .debarred(debarred)
                            .build());

            //! What this paper does to the result, and to the subject's sums.
            if (hall == ExamAttendanceStatus.UNFAIR_MEANS || debarred) {
                withheld = true;
            } else if (hall == ExamAttendanceStatus.EXEMPT) {
                continue;
            } else if (effective == null && hall != ExamAttendanceStatus.ABSENT
                    && !gradeOnlyOf.containsKey(paper.getSubjectCode())) {
                incomplete = true;
            }
            if (gradeOnlyOf.containsKey(paper.getSubjectCode())) {
                continue;
            }
            //! [maximum, obtained, passing maximum, passing obtained] — absent and debarred count 0.
            BigDecimal got = effective == null ? BigDecimal.ZERO : effective;
            BigDecimal[] raw = rawOf.computeIfAbsent(paper.getSubjectCode(),
                    k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            raw[0] = raw[0].add(paper.getMaximumMarks());
            raw[1] = raw[1].add(got);
            if (paper.getPassingMarks() != null) {
                raw[2] = raw[2].add(paper.getPassingMarks());
                raw[3] = raw[3].add(got);
            }
            BigDecimal[] perExam = perExamOf.computeIfAbsent(paper.getSubjectCode(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(exam.getId(), k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            perExam[0] = perExam[0].add(paper.getMaximumMarks());
            perExam[1] = perExam[1].add(got);
        }

        //! Step 9 - The subjects, and the card's totals. Activity and grade-only subjects stay out
        //! of the total; a subject passes when its marks reach the sum of its papers' pass marks.
        List<ReportCardSubjectResult> subjects = new ArrayList<>();
        BigDecimal totalMaximum = BigDecimal.ZERO;
        BigDecimal totalObtained = BigDecimal.ZERO;
        boolean failed = false;
        for (Map.Entry<String, List<ReportCardComponentResult>> one : componentsOf.entrySet()) {
            String code = one.getKey();
            ClassSubject classSubject = classSubjects.stream()
                    .filter(s -> s.getSubjectCode().equalsIgnoreCase(code))
                    .filter(s -> sectionNo.equalsIgnoreCase(s.getSectionNo()))
                    .findFirst()
                    .orElse(classSubjects.stream()
                            .filter(s -> s.getSubjectCode().equalsIgnoreCase(code) && s.getSectionNo() == null)
                            .findFirst()
                            .orElse(null));
            boolean gradeOnly = gradeOnlyOf.containsKey(code);
            SubjectType type = classSubject == null ? null : classSubject.getSubjectType();
            boolean counts = !gradeOnly && type != SubjectType.ACTIVITY;

            BigDecimal[] raw = rawOf.get(code);
            BigDecimal maximum = null;
            BigDecimal obtained = null;
            BigDecimal percentage = null;
            Boolean passed = null;
            if (!gradeOnly && raw != null && raw[0].signum() > 0) {
                if (isWeighted) {
                    //! Weighted: each exam's share of the subject, by the exam's weight, out of 100.
                    BigDecimal weights = BigDecimal.ZERO;
                    BigDecimal weightedSum = BigDecimal.ZERO;
                    for (Map.Entry<String, BigDecimal[]> part : perExamOf.get(code).entrySet()) {
                        if (part.getValue()[0].signum() == 0) {
                            continue;
                        }
                        BigDecimal weight = examById.get(part.getKey()).getWeightPercent();
                        weights = weights.add(weight);
                        weightedSum = weightedSum.add(weight.multiply(part.getValue()[1])
                                .divide(part.getValue()[0], 6, RoundingMode.HALF_UP));
                    }
                    percentage = weights.signum() == 0 ? null
                            : weightedSum.multiply(HUNDRED).divide(weights, 2, RoundingMode.HALF_UP);
                    maximum = HUNDRED;
                    obtained = percentage;
                } else {
                    maximum = raw[0];
                    obtained = raw[1];
                    percentage = raw[1].multiply(HUNDRED).divide(raw[0], 2, RoundingMode.HALF_UP);
                }
                passed = raw[2].signum() > 0 ? raw[3].compareTo(raw[2]) >= 0 : null;
            }
            if (counts && Boolean.FALSE.equals(passed)) {
                failed = true;
            }
            if (counts && maximum != null) {
                totalMaximum = totalMaximum.add(maximum);
                totalObtained = totalObtained.add(obtained);
            }

            //! The subject's grade: its own scheme, else the latest of its exams' schemes.
            String schemeId = classSubject != null && classSubject.getGradingSchemeDocsId() != null
                    ? classSubject.getGradingSchemeDocsId()
                    : one.getValue().stream()
                            .map(c -> examById.get(c.getExamDocsId()).getGradingSchemeDocsId())
                            .filter(Objects::nonNull)
                            .reduce((first, second) -> second)
                            .orElse(null);
            GradingScheme scheme = schemeId == null ? null : schemeById.get(schemeId);
            String grade = gradeOnly ? gradeOnlyOf.get(code) : null;
            if (!gradeOnly && percentage != null && scheme != null
                    && scheme.getScaleType() != GradingScaleType.DESCRIPTOR) {
                try {
                    grade = gradingHelper.resolveBand(scheme.getScaleType(), scheme.getMaximumValue(),
                            scheme.getGradeBands(), percentage.multiply(scheme.getMaximumValue())
                                    .divide(HUNDRED, 2, RoundingMode.HALF_UP)).getGradeCode();
                } catch (ApiException noBand) {
                    log.info("[draftCard] Step 2: No grade for {}% in {} on '{}'", percentage, code, scheme.getName());
                }
            }
            subjects.add(ReportCardSubjectResult.builder()
                    .subjectCode(code)
                    .subjectName(classSubject == null ? code : classSubject.getName())
                    .subjectType(type)
                    .countsTowardTotal(counts)
                    .components(one.getValue())
                    .maximumMarks(maximum)
                    .obtainedMarks(obtained)
                    .percentage(percentage)
                    .gradeCode(grade)
                    .gradingSchemeDocsId(scheme == null ? null : scheme.getId())
                    .passed(gradeOnly ? null : passed)
                    .build());
        }

        BigDecimal overall = totalMaximum.signum() > 0
                ? totalObtained.multiply(HUNDRED).divide(totalMaximum, 2, RoundingMode.HALF_UP) : null;
        String overallGrade = null;
        if (overall != null && cardScheme != null && cardScheme.getScaleType() != GradingScaleType.DESCRIPTOR) {
            try {
                overallGrade = gradingHelper.resolveBand(cardScheme.getScaleType(), cardScheme.getMaximumValue(),
                        cardScheme.getGradeBands(), overall.multiply(cardScheme.getMaximumValue())
                                .divide(HUNDRED, 2, RoundingMode.HALF_UP)).getGradeCode();
            } catch (ApiException noBand) {
                log.info("[draftCard] Step 3: No overall grade for {}% on '{}'", overall, cardScheme.getName());
            }
        }
        ResultStatus result = withheld ? ResultStatus.WITHHELD
                : subjects.isEmpty() || incomplete ? ResultStatus.INCOMPLETE
                : failed ? ResultStatus.FAIL
                : ResultStatus.PASS;

        //! Step 10 - Attendance: the section's DAILY registers in the term while the child was in it,
        //! and the days attended — attendance #10's counting (LATE attended, cancelled registers out).
        // TODO: read attendance sessions (the section's daily registers in the term)
        List<AttendanceSession> registers = sessions.findDailyForSection(school.getId(), year.getName(),
                schoolClass.getId(), sectionNo, term.getStartDate(), term.getEndDate());
        // TODO: read student academic records (who was in the section during the term)
        List<StudentAcademicRecord> held = academicRecords.findInSectionBetween(school.getId(), year.getName(),
                schoolClass.getId(), sectionNo, term.getStartDate(), term.getEndDate()).stream()
                .filter(one -> childId.equals(one.getStudentDocsId()))
                .toList();
        int workingDays = (int) registers.stream()
                .filter(register -> held.stream().anyMatch(p ->
                        !register.getAttendanceDate().isBefore(p.getEffectiveFrom())
                                && (p.getEffectiveUntil() == null
                                        || !register.getAttendanceDate().isAfter(p.getEffectiveUntil()))))
                .count();
        long presentDays = 0;
        if (!registers.isEmpty()) {
            // TODO: read student attendance records (count per child and status)
            for (StudentStatusCount one : attendanceMarks.countByStudentAndStatus(school.getId(),
                    registers.stream().map(AttendanceSession::getId).toList())) {
                if (childId.equals(one.studentDocsId()) && (AttendanceStatus.PRESENT.name().equals(String.valueOf(one.status()))
                        || AttendanceStatus.LATE.name().equals(String.valueOf(one.status())))) {
                    presentDays += one.count();
                }
            }
        }

        //! Step 11 - What saving will do, from the child's latest card for the term.
        ReportCard latest = cardUtils.latestCardOf(school, year.getName(), termId, childId);
        String saveWill = latest == null ? "CREATE"
                : latest.getStatus() == ReportCardStatus.PUBLISHED ? "REFUSE_PUBLISHED"
                : latest.getStatus() == ReportCardStatus.REVOKED ? "NEW_VERSION"
                : "REPLACE_DRAFT";
        if (latest != null && latest.getStatus() == ReportCardStatus.PUBLISHED) {
            warnings.add("This child's card for the term is already PUBLISHED — revoke it before saving a new one.");
        }

        log.info("[draftCard] Step 4: {} subject(s), {}% {} {}; {} warning(s); saving will {}", subjects.size(),
                overall, overallGrade, result, warnings.size(), saveWill);

        return new ReportCardDraftResponse(termId, term.getName(), record.getId(), childId, owner.student().getFullName(),
                owner.student().getAdmissionNo(), schoolClass.getId(), schoolClass.getName(), sectionNo,
                record.getRollNo(), new ArrayList<>(examById.keySet()), termExams.stream().map(Exam::getName).toList(),
                isWeighted, cardScheme == null ? null : cardScheme.getId(), cardScheme == null ? null : cardScheme.getName(),
                subjects, totalMaximum.signum() > 0 ? totalMaximum : null, totalMaximum.signum() > 0 ? totalObtained : null,
                overall, overallGrade, result, workingDays, (int) presentDays, warnings,
                latest == null ? null : new ReportCardDraftResponse.Existing(latest.getId(), latest.getReportVersion(),
                        latest.getStatus()),
                saveWill);
    }

    /**
     * Endpoint #29 — <b>save one child's report card</b> for a term: what is sent is what is stored,
     * as a DRAFT. Usually the draft #29b worked out, edited where the school wanted.
     *
     * <p>Who the card belongs to is the path's student record — its class, section and roll number —
     * and the term's name, never the body's. A DRAFT or REVIEWED card is replaced
     * in place (back to DRAFT); after a REVOKED one, the next version; a PUBLISHED one is refused.
     * The numbers are checked only for sense — no subject or card above its maximum, no attendance
     * above the working days.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public ReportCardResponse saveCard(String academicYear, String studentAcademicRecordDocsId,
            ReportCardSaveRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String termId = request.termDocsId().trim();
        String recordId = studentAcademicRecordDocsId == null ? "" : studentAcademicRecordDocsId.trim();
        log.info("[saveCard] Step 1: Saving the card of student record {} for term {} in school {}", recordId,
                termId, school.getId());

        //! Step 2 - The year, the term, and the student record — its class, section and roll.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        ReportCardServiceUtils.CardOwner owner = cardUtils.ownerOf(school, year.getName(), termId, recordId);
        String childId = owner.student().getId();

        //! Step 3 - Results locked for the year or the term stop every change to results.
        if (Boolean.TRUE.equals(year.getResultsLocked())) {
            throw ApiException.conflict("RESULTS_LOCKED",
                    "The results of " + year.getName() + " are locked, so no card can be saved. "
                            + "Unlock the year's results first.");
        }
        if (Boolean.TRUE.equals(owner.term().getResultsLocked())) {
            throw ApiException.conflict("RESULTS_LOCKED",
                    "The results of term '" + owner.term().getName() + "' are locked, so no card can be saved. "
                            + "Unlock the term's results first.");
        }

        //! Step 4 - The numbers must make sense, even after editing.
        List<String> wrong = new ArrayList<>();
        Set<String> codes = new LinkedHashSet<>();
        for (ReportCardSaveRequest.Subject subject : request.subjectResults()) {
            if (!codes.add(subject.subjectCode().trim().toUpperCase())) {
                wrong.add(subject.subjectCode() + " is on the card twice");
            }
            if (subject.obtainedMarks() != null && subject.maximumMarks() != null
                    && subject.obtainedMarks().compareTo(subject.maximumMarks()) > 0) {
                wrong.add(subject.subjectCode() + " has " + subject.obtainedMarks().stripTrailingZeros().toPlainString() + " of "
                        + subject.maximumMarks().stripTrailingZeros().toPlainString());
            }
            for (ReportCardSaveRequest.Component part : subject.components() == null
                    ? List.<ReportCardSaveRequest.Component>of() : subject.components()) {
                if (part.obtainedMarks() != null && part.maximumMarks() != null
                        && part.obtainedMarks().compareTo(part.maximumMarks()) > 0) {
                    wrong.add(subject.subjectCode() + " · " + part.examName() + " has " + part.obtainedMarks().stripTrailingZeros().toPlainString()
                            + " of " + part.maximumMarks().stripTrailingZeros().toPlainString());
                }
            }
        }
        if (request.totalObtainedMarks() != null && request.totalMaximumMarks() != null
                && request.totalObtainedMarks().compareTo(request.totalMaximumMarks()) > 0) {
            wrong.add("the total is " + request.totalObtainedMarks().stripTrailingZeros().toPlainString() + " of " + request.totalMaximumMarks().stripTrailingZeros().toPlainString());
        }
        if (request.attendancePresentDays() != null && request.attendanceWorkingDays() != null
                && request.attendancePresentDays() > request.attendanceWorkingDays()) {
            wrong.add("attendance is " + request.attendancePresentDays() + " of " + request.attendanceWorkingDays()
                    + " days");
        }
        if (!wrong.isEmpty()) {
            throw ApiException.badRequest("CARD_NUMBERS_DO_NOT_ADD_UP",
                    "The card cannot be saved: " + String.join("; ", wrong) + ".");
        }

        //! Step 5 - Which card to write: a DRAFT or REVIEWED one is replaced in place; after a
        //! REVOKED one, the next version; a PUBLISHED one is refused.
        ReportCard latest = cardUtils.latestCardOf(school, year.getName(), termId, childId);
        if (latest != null && latest.getStatus() == ReportCardStatus.PUBLISHED) {
            throw ApiException.conflict("CARD_ALREADY_PUBLISHED",
                    owner.student().getFullName() + "'s card for term '" + owner.term().getName()
                            + "' is already published (version " + latest.getReportVersion()
                            + "). Revoke it before saving a new one.");
        }
        boolean replace = latest != null && (latest.getStatus() == ReportCardStatus.DRAFT
                || latest.getStatus() == ReportCardStatus.REVIEWED);
        ReportCard card = replace ? latest : ReportCard.builder()
                .schoolId(school.getId())
                .academicYear(year.getName())
                .studentDocsId(childId)
                .termDocsId(termId)
                .reportVersion(latest == null ? 1 : latest.getReportVersion() + 1)
                .build();

        //! Step 6 - Fill the card: who it belongs to from the server, everything else as sent.
        StudentAcademicRecord record = owner.record();
        card.setStudentAcademicRecordDocsId(record.getId());
        card.setTermName(owner.term().getName());
        card.setClassDocsId(owner.schoolClass().getId());
        card.setClassName(owner.schoolClass().getName());
        card.setSectionNo(record.getSectionNo());
        card.setRollNo(record.getRollNo());
        card.setExamDocsIds(request.examDocsIds() == null ? new ArrayList<>()
                : new ArrayList<>(request.examDocsIds().stream().map(String::trim).toList()));
        card.setGradingSchemeDocsId(TextHelper.blankToNull(request.gradingSchemeDocsId()));
        card.setSubjectResults(request.subjectResults().stream()
                .map(subject -> ReportCardSubjectResult.builder()
                        .subjectCode(subject.subjectCode().trim())
                        .subjectName(subject.subjectName().trim())
                        .subjectType(subject.subjectType())
                        .countsTowardTotal(subject.countsTowardTotal())
                        .components(subject.components() == null ? new ArrayList<>() : subject.components().stream()
                                .map(part -> ReportCardComponentResult.builder()
                                        .examDocsId(part.examDocsId().trim())
                                        .examName(part.examName().trim())
                                        .weightPercent(part.weightPercent())
                                        .componentName(TextHelper.blankToNull(part.componentName()))
                                        .maximumMarks(part.maximumMarks())
                                        .obtainedMarks(part.obtainedMarks())
                                        .graceMarks(part.graceMarks())
                                        .attendanceStatus(part.attendanceStatus())
                                        .debarred(Boolean.TRUE.equals(part.debarred()))
                                        .build())
                                .toList())
                        .maximumMarks(subject.maximumMarks())
                        .obtainedMarks(subject.obtainedMarks())
                        .percentage(subject.percentage())
                        .gradeCode(TextHelper.blankToNull(subject.gradeCode()))
                        .gradingSchemeDocsId(TextHelper.blankToNull(subject.gradingSchemeDocsId()))
                        .passed(subject.passed())
                        .teacherRemark(TextHelper.blankToNull(subject.teacherRemark()))
                        .build())
                .toList());
        card.setTotalMaximumMarks(request.totalMaximumMarks());
        card.setTotalObtainedMarks(request.totalObtainedMarks());
        card.setOverallPercentage(request.overallPercentage());
        card.setOverallGradeCode(TextHelper.blankToNull(request.overallGradeCode()));
        card.setResultStatus(request.resultStatus());
        card.setAttendanceWorkingDays(request.attendanceWorkingDays());
        card.setAttendancePresentDays(request.attendancePresentDays());
        card.setClassTeacherRemark(TextHelper.blankToNull(request.classTeacherRemark()));
        card.setPrincipalRemark(TextHelper.blankToNull(request.principalRemark()));
        card.setClassRank(null);
        card.setRankedStudentCount(null);
        card.setStatus(ReportCardStatus.DRAFT);
        card.setReviewedAt(null);
        card.setReviewedByDocsId(null);

        //! Step 7 - Save it.
        // TODO: insert / update report card
        ReportCard saved = reportCards.save(card);
        log.info("[saveCard] Step 2: {} card {} (version {}) for student {}", replace ? "Replaced" : "Created",
                saved.getId(), saved.getReportVersion(), childId);

        return ReportCardResponse.of(saved, owner.student(),
                (replace ? "The DRAFT card was replaced." : "Card version " + saved.getReportVersion() + " is saved as DRAFT.")
                        + " Review it, then publish the section's cards together.");
    }
}
