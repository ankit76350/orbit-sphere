package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.academics.examcandidate.request.ExamCandidatesAddRequest;
import com.orbitastra.backend.dto.academics.examcandidate.response.ExamCandidateResponse;
import com.orbitastra.backend.dto.academics.examcandidate.response.ExamCandidatesAddedResponse;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examcandidate.ExamCandidateRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * An exam's candidates — the children allowed to sit it. Endpoint #43 of the plan in
 * {@code controllers/academics/examination}; #44–#49 are not built.
 *
 * <p>The year and the class are read through the timetable's utils, the same as the papers are.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamCandidateService {

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final ExamCandidateRepository examCandidates;
    private final StudentAcademicRecordRepository academicRecords;
    private final StudentRepository students;
    private final DailyTimetableServiceUtils utils;

    /**
     * Endpoint #43 — <b>add the children who may sit</b>, one section at a time. With no
     * {@code studentDocsIds}, everyone in the section on the exam's first day; with them, only those
     * children. A child already on the list is skipped and listed, never an error, so running it
     * again for a section adds only the new joiners. Each new candidate is {@code ELIGIBLE}.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    public ExamCandidatesAddedResponse addCandidates(String academicYear, String examDocsId,
            ExamCandidatesAddRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String examId = examDocsId == null ? "" : examDocsId.trim();
        log.info("[addCandidates] Step 1: Adding candidates to exam {} of {} for school {}", examId,
                academicYear, school.getId());

        //! Step 2 - The year, then the exam inside it. A finished or cancelled exam takes nobody new.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(examId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND",
                        "No exam with id '" + examId + "' in '" + year.getName() + "'."));
        if (exam.getStatus() == ExamStatus.COMPLETED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw ApiException.conflict("EXAM_NOT_EDITABLE",
                    "This exam is " + exam.getStatus() + ", so nobody can be added to it.");
        }

        //! Step 3 - The class, then the section in it — spelled the class's own way, and in use.
        SchoolClass schoolClass = utils.loadClassForYear(school, year.getName(),
                request.classDocsId().trim());
        String sent = request.sectionNo().trim();
        ClassSection section = (schoolClass.getSections() == null ? List.<ClassSection>of()
                : schoolClass.getSections()).stream()
                .filter(one -> one.getSectionNo().equalsIgnoreCase(sent))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("SECTION_NOT_IN_CLASS",
                        "'" + schoolClass.getName() + "' has no section " + sent + "."));
        if (Boolean.FALSE.equals(section.getActive())) {
            throw ApiException.conflict("SECTION_NOT_ACTIVE",
                    "Section '" + section.getSectionNo() + "' of '" + schoolClass.getName()
                            + "' is not active.");
        }
        String sectionNo = section.getSectionNo();

        //! Step 4 - Who is in the section. One record per child — on the day of a transfer a child
        //! can hold two, and the later one wins.
        Map<String, StudentAcademicRecord> recordByChild = new LinkedHashMap<>();
        boolean wholeSection = request.studentDocsIds() == null || request.studentDocsIds().isEmpty();
        List<StudentAcademicRecord> placements;
        if (wholeSection) {
            //! The whole section: everyone in it on the exam's first day.
            // TODO: read student academic records (who is in this section on the first day)
            placements = academicRecords.findInSectionOn(school.getId(), year.getName(),
                    schoolClass.getId(), sectionNo, exam.getStartDate());
        } else {
            //! Named children: each in the section on some day of the exam, so a child who joins
            //! mid-exam can be added.
            // TODO: read student academic records (who is in this section during the exam)
            placements = academicRecords.findInSectionBetween(school.getId(), year.getName(),
                    schoolClass.getId(), sectionNo, exam.getStartDate(), exam.getEndDate());
        }
        for (StudentAcademicRecord one : placements) {
            StudentAcademicRecord kept = recordByChild.get(one.getStudentDocsId());
            if (kept == null || one.getEffectiveFrom().isAfter(kept.getEffectiveFrom())) {
                recordByChild.put(one.getStudentDocsId(), one);
            }
        }

        //! Step 5 - Named children must all be in the section. Sending one twice is the same as once.
        List<String> wanted;
        if (wholeSection) {
            wanted = new ArrayList<>(recordByChild.keySet());
        } else {
            wanted = new ArrayList<>(new LinkedHashSet<>(request.studentDocsIds().stream()
                    .map(String::trim)
                    .toList()));
            List<String> notInSection = wanted.stream()
                    .filter(id -> !recordByChild.containsKey(id))
                    .toList();
            if (!notInSection.isEmpty()) {
                throw ApiException.badRequest("STUDENT_NOT_IN_SECTION",
                        "Not in " + schoolClass.getName() + " " + sectionNo + " between "
                                + Dates.readable(exam.getStartDate()) + " and "
                                + Dates.readable(exam.getEndDate()) + ": "
                                + String.join(", ", notInSection) + ".");
            }
        }

        //! Step 6 - Who is already a candidate. They are left as they are — debarred stays debarred.
        Map<String, ExamCandidate> existing = new LinkedHashMap<>();
        if (!wanted.isEmpty()) {
            // TODO: read exam candidates (these children, in this exam)
            for (ExamCandidate one : examCandidates.findBySchoolIdAndExamDocsIdAndStudentDocsIdIn(
                    school.getId(), exam.getId(), wanted)) {
                existing.put(one.getStudentDocsId(), one);
            }
        }

        //! Step 7 - Build a candidate for everyone else. Only the record id is copied; the class and
        //! section are always read from the record.
        List<ExamCandidate> toSave = new ArrayList<>();
        for (String childId : wanted) {
            if (existing.containsKey(childId)) {
                continue;
            }
            toSave.add(ExamCandidate.builder()
                    .schoolId(school.getId())
                    .academicYear(year.getName())
                    .studentDocsId(childId)
                    .examDocsId(exam.getId())
                    .studentAcademicRecordDocsId(recordByChild.get(childId).getId())
                    .status(ExamCandidateStatus.ELIGIBLE)
                    .build());
        }

        //! Step 8 - Save them.
        List<ExamCandidate> saved = new ArrayList<>();
        if (!toSave.isEmpty()) {
            // TODO: insert exam candidates
            saved = examCandidates.saveAll(toSave);
        }
        log.info("[addCandidates] Step 2: Added {} candidate(s) to exam {}; {} already on it",
                saved.size(), exam.getId(), existing.size());

        //! Step 9 - The children's names, for both lists, sorted by name.
        Set<String> childIds = new LinkedHashSet<>(wanted);
        Map<String, Student> children = new LinkedHashMap<>();
        if (!childIds.isEmpty()) {
            // TODO: read students (the children added or already there)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(), childIds)) {
                children.put(one.getId(), one);
            }
        }
        Comparator<ExamCandidateResponse> byName = Comparator.comparing(
                ExamCandidateResponse::fullName, Comparator.nullsLast(String::compareToIgnoreCase));
        List<ExamCandidateResponse> added = saved.stream()
                .map(one -> ExamCandidateResponse.of(one, children.get(one.getStudentDocsId()),
                        recordByChild.get(one.getStudentDocsId()).getRollNo()))
                .sorted(byName)
                .toList();
        List<ExamCandidateResponse> already = existing.values().stream()
                .map(one -> ExamCandidateResponse.of(one, children.get(one.getStudentDocsId()),
                        recordByChild.get(one.getStudentDocsId()).getRollNo()))
                .sorted(byName)
                .toList();

        String nextStep;
        if (wanted.isEmpty()) {
            nextStep = "Nobody is in " + schoolClass.getName() + " " + sectionNo + " on "
                    + Dates.readable(exam.getStartDate()) + ", the exam's first day. Place the "
                    + "children in the section first, or name them.";
        } else if (added.isEmpty()) {
            nextStep = "Everyone was already a candidate. Nothing changed.";
        } else {
            nextStep = added.size() + " child(ren) may now sit. Debar anyone who may not — fees due, "
                    + "attendance short — before the first paper.";
        }

        return new ExamCandidatesAddedResponse(exam.getId(), exam.getName(), schoolClass.getId(),
                schoolClass.getName(), sectionNo, added.size(), already.size(), added, already,
                nextStep);
    }
}
