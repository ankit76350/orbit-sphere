package com.orbitastra.backend.services.academics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.examattendance.response.ExamHallRegisterResponse;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.examination.embedded.ExamRoomAllocation;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.room.Room;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.room.room.RoomRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.academics.utils.AttendanceServiceUtils;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamAttendanceServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The exam hall — who sat each paper. Endpoint #14 of the plan in
 * {@code controllers/academics/examination}; #15–#17 are not built.
 *
 * <p>Hall entries live inside each candidate ({@code ExamCandidate.papers}); there is no hall
 * collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final ExamScheduleRepository examSchedules;
    private final SchoolClassRepository schoolClasses;
    private final RoomRepository rooms;
    private final StudentRepository students;
    private final DailyTimetableServiceUtils utils;
    private final ExamAttendanceServiceUtils hallUtils;

    /**
     * Endpoint #14 — <b>the hall register</b> of one paper: every child expected, in roll order,
     * each with their hall entry or none.
     *
     * <p><b>Expected</b> is a child in the paper's class and section on its date who is an
     * {@code ELIGIBLE} candidate of the exam — plus anyone who already has an entry on this paper
     * (debarred after they sat it, or moved section since). A debarred child is not listed, only
     * counted. Nothing is created ahead of time: an unmarked child is shown as unmarked.
     *
     * <p><b>No gates.</b> A read.
     */
    public ExamHallRegisterResponse getHallRegister(String academicYear, String examScheduleDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[getHallRegister] Step 1: Reading the hall register of paper {} for school {}",
                paperId, school.getId());

        //! Step 2 - The year, then the paper inside it, then its exam's name.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));

        // TODO: read exam
        String examName = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(),
                school.getId(), year.getName()).map(Exam::getName).orElse(null);

        //! Step 3 - The names behind the class, the subject and the rooms.
        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(paper.getClassDocsId(), school.getId(),
                        year.getName())
                .orElse(null);
        String subjectName = schoolClass == null || schoolClass.getSubjects() == null ? null
                : schoolClass.getSubjects().stream()
                        .filter(one -> one.getSubjectCode().equalsIgnoreCase(paper.getSubjectCode()))
                        .map(ClassSubject::getName)
                        .findFirst()
                        .orElse(null);

        List<String> roomIds = paper.getRooms().stream().map(ExamRoomAllocation::getRoomDocsId).toList();
        Map<String, String> roomNames = new LinkedHashMap<>();
        if (!roomIds.isEmpty()) {
            // TODO: read rooms
            for (Room room : rooms.findBySchoolIdAndIdIn(school.getId(), roomIds)) {
                roomNames.put(room.getId(), room.getName());
            }
        }

        //! Step 4 - Who the hall is about: the section that day, and the candidates among them.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        Map<String, StudentAcademicRecord> sectionRecords = roster.sectionRecords();

        //! Step 5 - Pick the rows. A child with an entry here is always listed; an eligible child in
        //! the section is listed; a debarred one with no entry is only counted.
        List<ExamCandidate> listed = new ArrayList<>();
        int debarred = 0;
        for (ExamCandidate one : roster.candidates().values()) {
            boolean hasEntry = one.getPapers() != null && one.getPapers().stream()
                    .anyMatch(entry -> paperId.equals(entry.getExamScheduleDocsId()));
            boolean inSection = sectionRecords.containsKey(one.getStudentDocsId());
            if (hasEntry || (inSection && one.getStatus() == ExamCandidateStatus.ELIGIBLE)) {
                listed.add(one);
            } else if (inSection && one.getStatus() == ExamCandidateStatus.DEBARRED) {
                debarred++;
            }
        }
        int notCandidates = (int) sectionRecords.keySet().stream()
                .filter(id -> !roster.candidates().containsKey(id))
                .count();

        //! Step 6 - The children's names.
        Map<String, Student> children = new LinkedHashMap<>();
        if (!listed.isEmpty()) {
            // TODO: read students (the children on the register)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(),
                    listed.stream().map(ExamCandidate::getStudentDocsId).toList())) {
                children.put(one.getId(), one);
            }
        }

        //! Step 7 - One row each. Late and left early are worked out from the paper's times in the
        //! school's own time zone; they are not stored.
        ZoneId zone = school.getDefaultTimeZone() == null ? ZoneOffset.UTC
                : school.getDefaultTimeZone().toZoneId();
        Instant startsAt = paper.getExamDate().atTime(paper.getStartTime()).atZone(zone).toInstant();
        Instant endsAt = paper.getExamDate().atTime(paper.getEndTime()).atZone(zone).toInstant();

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (ExamAttendanceStatus status : ExamAttendanceStatus.values()) {
            byStatus.put(status.name(), 0);
        }
        List<ExamHallRegisterResponse.Row> rows = new ArrayList<>();
        for (ExamCandidate one : listed) {
            ExamPaperAttendance entry = one.getPapers().stream()
                    .filter(p -> paperId.equals(p.getExamScheduleDocsId()))
                    .findFirst()
                    .orElse(null);
            ExamHallRegisterResponse.Entry shown = null;
            if (entry != null) {
                byStatus.merge(entry.getAttendanceStatus().name(), 1, Integer::sum);
                shown = new ExamHallRegisterResponse.Entry(entry.getId(), entry.getAttendanceStatus(),
                        entry.getAnswerCopyNo(), entry.getAdditionalAnswerCopyNos(),
                        entry.getRoomDocsId(), roomNames.get(entry.getRoomDocsId()), entry.getSeatNo(),
                        entry.getReportedAt(), entry.getSubmittedAt(),
                        entry.getReportedAt() != null && entry.getReportedAt().isAfter(startsAt),
                        entry.getSubmittedAt() != null && entry.getSubmittedAt().isBefore(endsAt),
                        entry.getRemarks(), entry.getMarkedAt(), entry.getMarkedByDocsId());
            }
            Student child = children.get(one.getStudentDocsId());
            StudentAcademicRecord record = sectionRecords.get(one.getStudentDocsId());
            rows.add(new ExamHallRegisterResponse.Row(one.getStudentDocsId(),
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    record == null ? null : record.getRollNo(),
                    one.getId(), one.getStatus(), record != null, shown));
        }
        rows.sort(Comparator
                .comparing(ExamHallRegisterResponse.Row::rollNo, AttendanceServiceUtils.ROLL_ORDER)
                .thenComparing(ExamHallRegisterResponse.Row::fullName,
                        Comparator.nullsLast(String::compareToIgnoreCase)));

        int marked = byStatus.values().stream().mapToInt(Integer::intValue).sum();
        log.info("[getHallRegister] Step 2: Paper {} — {} expected, {} marked, {} debarred", paperId,
                rows.size(), marked, debarred);

        return new ExamHallRegisterResponse(paper.getId(), paper.getExamDocsId(), examName,
                paper.getClassDocsId(), schoolClass == null ? null : schoolClass.getName(),
                paper.getSectionNo(), paper.getSubjectCode(), subjectName, paper.getComponentName(),
                paper.getExamDate(), paper.getStartTime(), paper.getEndTime(), paper.getStatus(),
                roomIds.stream().map(id -> new ExamHallRegisterResponse.Room(id, roomNames.get(id))).toList(),
                new ExamHallRegisterResponse.Counts(rows.size(), marked, rows.size() - marked, byStatus,
                        debarred, notCandidates),
                rows, paper.getVersion());
    }
}
