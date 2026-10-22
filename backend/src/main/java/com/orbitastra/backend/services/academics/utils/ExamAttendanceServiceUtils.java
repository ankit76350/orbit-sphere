package com.orbitastra.backend.services.academics.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

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
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.room.Room;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examcandidate.ExamCandidateRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.room.room.RoomRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the hall endpoints share. A method here never calls another method here — a caller that
 * needs the register reads the roster first and hands it in.
 */
@Component
@RequiredArgsConstructor
public class ExamAttendanceServiceUtils {

    private final StudentAcademicRecordRepository academicRecords;
    private final ExamCandidateRepository examCandidates;
    private final ExamRepository exams;
    private final SchoolClassRepository schoolClasses;
    private final RoomRepository rooms;
    private final StudentRepository students;

    /**
     * Who a paper's hall is about.
     *
     * @param sectionRecords the children in the paper's section on its date, one record each
     * @param candidates     the exam's candidates among them, plus anyone already marked at this
     *                       paper even if they have left the section — keyed by child
     */
    public record HallRoster(Map<String, StudentAcademicRecord> sectionRecords,
            Map<String, ExamCandidate> candidates) {
    }

    /**
     * The section on the paper's date, from academic records, and which of those children are
     * candidates of the exam — plus anyone with a hall entry on this paper already. On the day of a
     * transfer a child can hold two records; the later one is kept.
     *
     * Used by:
     * - ExamScheduleService.getPaper()
     * - ExamAttendanceService.getHallRegister()
     * - ExamAttendanceService.markHall()
     * - ExamScheduleService.completePaper()
     * - StudentMarkService.getMarkSheet()
     */
    public HallRoster rosterOf(School school, String academicYear, ExamSchedule paper) {
        Map<String, StudentAcademicRecord> sectionRecords = new LinkedHashMap<>();
        // TODO: read student academic records (who was in this section on this date)
        for (StudentAcademicRecord record : academicRecords.findInSectionOn(school.getId(),
                academicYear, paper.getClassDocsId(), paper.getSectionNo(), paper.getExamDate())) {
            StudentAcademicRecord kept = sectionRecords.get(record.getStudentDocsId());
            if (kept == null || record.getEffectiveFrom().isAfter(kept.getEffectiveFrom())) {
                sectionRecords.put(record.getStudentDocsId(), record);
            }
        }

        Map<String, ExamCandidate> candidates = new LinkedHashMap<>();
        if (!sectionRecords.isEmpty()) {
            // TODO: read exam candidates (the section's children)
            for (ExamCandidate one : examCandidates.findBySchoolIdAndExamDocsIdAndStudentDocsIdIn(
                    school.getId(), paper.getExamDocsId(), sectionRecords.keySet())) {
                candidates.put(one.getStudentDocsId(), one);
            }
        }
        //! Anyone already marked at this paper counts too, even if they have left the section.
        // TODO: read exam candidates (marked at this paper)
        for (ExamCandidate one : examCandidates.findMarkedAtPaper(school.getId(), paper.getId())) {
            candidates.putIfAbsent(one.getStudentDocsId(), one);
        }

        return new HallRoster(sectionRecords, candidates);
    }

    /**
     * The hall register of one paper, from a roster already read: the paper, its rooms, the counts,
     * and every child expected in roll order with their entry or none. A child with an entry here
     * is always listed; an eligible child in the section is listed; a debarred one with no entry is
     * only counted. {@code late} and {@code leftEarly} are worked out in the school's time zone.
     *
     * Used by:
     * - ExamAttendanceService.getHallRegister()
     * - ExamAttendanceService.markHall()
     * - ExamScheduleService.completePaper()
     */
    public ExamHallRegisterResponse registerOf(School school, String academicYear,
            ExamSchedule paper, HallRoster roster) {
        String paperId = paper.getId();

        //! The names behind the exam, the class, the subject and the rooms.
        // TODO: read exam
        String examName = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(),
                school.getId(), academicYear).map(Exam::getName).orElse(null);

        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(paper.getClassDocsId(), school.getId(), academicYear)
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

        //! Pick the rows.
        Map<String, StudentAcademicRecord> sectionRecords = roster.sectionRecords();
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

        //! The children's names.
        Map<String, Student> children = new LinkedHashMap<>();
        if (!listed.isEmpty()) {
            // TODO: read students (the children on the register)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(),
                    listed.stream().map(ExamCandidate::getStudentDocsId).toList())) {
                children.put(one.getId(), one);
            }
        }

        //! One row each. Late and left early are worked out from the paper's times in the school's
        //! own time zone; they are not stored.
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
