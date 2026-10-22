package com.orbitastra.backend.services.academics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.current.CurrentUserResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.academics.examattendance.request.ExamHallMarksRequest;
import com.orbitastra.backend.dto.academics.examattendance.response.ExamHallRegisterResponse;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.examination.embedded.ExamRoomAllocation;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examcandidate.ExamCandidateRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamAttendanceServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The exam hall — who sat each paper. Endpoints #14 and #15 of the plan in
 * {@code controllers/academics/examination}. Completing a paper (#16) is the paper's own
 * endpoint, in {@code ExamScheduleService}; #17 is not built.
 *
 * <p>Hall entries live inside each candidate ({@code ExamCandidate.papers}); there is no hall
 * collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamAttendanceService {

    private final CurrentSchoolResolver currentSchool;
    private final CurrentUserResolver currentUser;
    private final ExamRepository exams;
    private final ExamScheduleRepository examSchedules;
    private final ExamCandidateRepository examCandidates;
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

        //! Step 2 - The year, then the paper inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));

        //! Step 3 - Who the hall is about, then the register built from it.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        ExamHallRegisterResponse register = hallUtils.registerOf(school, year.getName(), paper, roster);

        log.info("[getHallRegister] Step 2: Paper {} — {} expected, {} marked, {} debarred", paperId,
                register.counts().expected(), register.counts().marked(), register.counts().debarred());
        return register;
    }

    /**
     * Endpoint #15 — <b>mark the hall</b>: present, absent, unfair means or exempt, the booklets,
     * the room and seat, the times. One child, several or all; re-sending a child corrects them.
     *
     * <p><b>Everything is checked before anything is written</b>, and one request is one
     * transaction: every candidate it touches, the paper's version, and the exam moving to
     * {@code IN_PROGRESS} on the first save. A candidate changed in between fails the whole request.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public ExamHallRegisterResponse markHall(String academicYear, String examScheduleDocsId,
            ExamHallMarksRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[markHall] Step 1: Marking {} child(ren) at paper {} for school {}",
                request.marks().size(), paperId, school.getId());

        //! Step 2 - The request on its own: each child once, booklets only where a booklet is
        //! handed out, and the times the right way round.
        Set<String> seenChildren = new LinkedHashSet<>();
        List<String> twice = new ArrayList<>();
        for (ExamHallMarksRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            if (!seenChildren.add(childId)) {
                twice.add(childId);
            }
            boolean writes = mark.attendanceStatus() == ExamAttendanceStatus.PRESENT
                    || mark.attendanceStatus() == ExamAttendanceStatus.UNFAIR_MEANS;
            boolean hasBooklet = mark.answerCopyNo() != null && !mark.answerCopyNo().isBlank();
            boolean hasExtra = mark.additionalAnswerCopyNos() != null
                    && !mark.additionalAnswerCopyNos().isEmpty();
            if (writes && !hasBooklet) {
                throw ApiException.badRequest("ANSWER_COPY_REQUIRED",
                        "Child " + childId + " is " + mark.attendanceStatus()
                                + ", so their answer booklet number is needed.");
            }
            if (!writes && (hasBooklet || hasExtra)) {
                throw ApiException.badRequest("ANSWER_COPY_NOT_ALLOWED",
                        "Child " + childId + " is " + mark.attendanceStatus()
                                + ", so they had no booklet. Leave the booklet numbers out.");
            }
            if (mark.reportedAt() != null && mark.submittedAt() != null
                    && mark.submittedAt().isBefore(mark.reportedAt())) {
                throw ApiException.badRequest("INVALID_TIME_RANGE",
                        "Child " + childId + " handed in before they came in.");
            }
        }
        if (!twice.isEmpty()) {
            throw ApiException.badRequest("DUPLICATE_STUDENT_IN_REQUEST",
                    "Each child once: " + String.join(", ", twice) + " is in marks more than once.");
        }

        //! Step 3 - A booklet is used once — inside this request first.
        Map<String, String> bookletOwner = new LinkedHashMap<>();
        for (ExamHallMarksRequest.Mark mark : request.marks()) {
            List<String> numbers = new ArrayList<>();
            if (mark.answerCopyNo() != null && !mark.answerCopyNo().isBlank()) {
                numbers.add(mark.answerCopyNo().trim());
            }
            if (mark.additionalAnswerCopyNos() != null) {
                mark.additionalAnswerCopyNos().forEach(one -> numbers.add(one.trim()));
            }
            for (String number : numbers) {
                String owner = bookletOwner.putIfAbsent(number, mark.studentDocsId().trim());
                if (owner != null) {
                    throw ApiException.conflict("ANSWER_COPY_TAKEN",
                            "Booklet " + number + " is given twice in this request — to " + owner
                                    + (owner.equals(mark.studentDocsId().trim()) ? " both times"
                                            : " and " + mark.studentDocsId().trim()) + ".");
                }
            }
        }

        //! Step 4 - The year, the paper, and its version.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));
        if (!request.version().equals(paper.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This paper's register was saved by someone else. Please read it again before "
                            + "marking.");
        }

        //! Step 5 - Only a published paper, on or after its day, takes hall marks.
        if (paper.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw ApiException.conflict("PAPER_NOT_OPEN",
                    "This paper is " + paper.getStatus() + ". Only a PUBLISHED paper takes hall marks.");
        }
        LocalDate today = Dates.todayIn(school.getDefaultTimeZone());
        if (today.isBefore(paper.getExamDate())) {
            throw ApiException.conflict("PAPER_NOT_YET_SAT",
                    "This paper is on " + Dates.readable(paper.getExamDate())
                            + ". Its hall can be marked from that day.");
        }

        //! Step 6 - Each room must be one of the paper's. A paper with one room fills it in.
        Set<String> paperRooms = new LinkedHashSet<>();
        for (ExamRoomAllocation room : paper.getRooms()) {
            paperRooms.add(room.getRoomDocsId());
        }
        Map<String, String> roomOf = new LinkedHashMap<>();
        for (ExamHallMarksRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            String room = mark.roomDocsId() == null || mark.roomDocsId().isBlank() ? null
                    : mark.roomDocsId().trim();
            if (room != null && !paperRooms.contains(room)) {
                throw ApiException.badRequest("ROOM_NOT_IN_PAPER",
                        "Room " + room + " is not one of this paper's rooms (child " + childId + ").");
            }
            boolean writes = mark.attendanceStatus() == ExamAttendanceStatus.PRESENT
                    || mark.attendanceStatus() == ExamAttendanceStatus.UNFAIR_MEANS;
            if (room == null && writes && paperRooms.size() == 1) {
                room = paperRooms.iterator().next();
            }
            if (room == null && writes && paperRooms.size() > 1) {
                throw ApiException.badRequest("ROOM_REQUIRED",
                        "This paper is written in " + paperRooms.size() + " rooms, so say which one "
                                + "child " + childId + " sat in.");
            }
            roomOf.put(childId, room);
        }

        //! Step 7 - Only the children #14 lists. A debarred child gets no new entry; one who sat
        //! before being debarred can still have their entry corrected.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        List<String> notInPaper = new ArrayList<>();
        List<String> debarred = new ArrayList<>();
        for (String childId : seenChildren) {
            ExamCandidate candidate = roster.candidates().get(childId);
            boolean hasEntry = candidate != null && candidate.getPapers() != null
                    && candidate.getPapers().stream().anyMatch(p -> paperId.equals(p.getExamScheduleDocsId()));
            boolean inSection = roster.sectionRecords().containsKey(childId);
            if (candidate == null || (!inSection && !hasEntry)) {
                notInPaper.add(childId);
            } else if (candidate.getStatus() == ExamCandidateStatus.DEBARRED && !hasEntry) {
                debarred.add(childId + " (" + candidate.getDebarmentReason() + ")");
            }
        }
        if (!notInPaper.isEmpty()) {
            throw ApiException.badRequest("STUDENT_NOT_IN_PAPER",
                    "Not on this paper's register — not a candidate, or not in "
                            + paper.getSectionNo() + " that day: " + String.join(", ", notInPaper) + ".");
        }
        if (!debarred.isEmpty()) {
            throw ApiException.conflict("STUDENT_DEBARRED",
                    "Debarred from this exam, so not on the register: " + String.join(", ", debarred)
                            + ". Let them back in first.");
        }

        //! Step 8 - A booklet is used once in the whole exam — against every other entry, on any
        //! paper, the child's own included. Only the entry being replaced does not count.
        if (!bookletOwner.isEmpty()) {
            List<String> taken = new ArrayList<>();
            Set<String> holderIds = new LinkedHashSet<>();
            // TODO: read exam candidates (holding any of these booklets)
            List<ExamCandidate> holders = examCandidates.findHoldingBooklets(school.getId(),
                    paper.getExamDocsId(), bookletOwner.keySet());
            for (ExamCandidate holder : holders) {
                for (ExamPaperAttendance entry : holder.getPapers()) {
                    boolean beingReplaced = paperId.equals(entry.getExamScheduleDocsId())
                            && seenChildren.contains(holder.getStudentDocsId());
                    if (beingReplaced) {
                        continue;
                    }
                    List<String> held = new ArrayList<>();
                    if (entry.getAnswerCopyNo() != null) {
                        held.add(entry.getAnswerCopyNo());
                    }
                    if (entry.getAdditionalAnswerCopyNos() != null) {
                        held.addAll(entry.getAdditionalAnswerCopyNos());
                    }
                    for (String number : held) {
                        if (bookletOwner.containsKey(number)) {
                            taken.add(number + "|" + holder.getStudentDocsId() + "|"
                                    + (paperId.equals(entry.getExamScheduleDocsId()) ? "this paper"
                                            : "another paper of this exam"));
                            holderIds.add(holder.getStudentDocsId());
                        }
                    }
                }
            }
            if (!taken.isEmpty()) {
                Map<String, String> names = new LinkedHashMap<>();
                // TODO: read students (who already holds the booklets)
                for (Student one : students.findBySchoolIdAndIdIn(school.getId(), holderIds)) {
                    names.put(one.getId(), one.getFullName());
                }
                throw ApiException.conflict("ANSWER_COPY_TAKEN",
                        "A booklet is used once in the whole exam. Already given: " + taken.stream()
                                .map(one -> {
                                    String[] part = one.split("\\|");
                                    return part[0] + " to " + names.getOrDefault(part[1], part[1])
                                            + " on " + part[2];
                                })
                                .collect(Collectors.joining("; ")) + ".");
            }
        }

        //! Step 9 - Write each child's entry for this paper: replaced when there is one, added
        //! when not. When and by whom are the server's.
        Instant now = Instant.now();
        String markedBy = currentUser.staffDocsIdOrNull();
        List<ExamCandidate> toSave = new ArrayList<>();
        for (ExamHallMarksRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            ExamCandidate candidate = roster.candidates().get(childId);
            if (candidate.getPapers() == null) {
                candidate.setPapers(new ArrayList<>());
            }
            ExamPaperAttendance old = candidate.getPapers().stream()
                    .filter(p -> paperId.equals(p.getExamScheduleDocsId()))
                    .findFirst()
                    .orElse(null);
            ExamPaperAttendance entry = ExamPaperAttendance.builder()
                    .id(old != null ? old.getId() : new ObjectId().toHexString())
                    .examScheduleDocsId(paperId)
                    .attendanceStatus(mark.attendanceStatus())
                    .answerCopyNo(mark.answerCopyNo() == null || mark.answerCopyNo().isBlank() ? null
                            : mark.answerCopyNo().trim())
                    .additionalAnswerCopyNos(mark.additionalAnswerCopyNos() == null ? new ArrayList<>()
                            : new ArrayList<>(mark.additionalAnswerCopyNos().stream().map(String::trim).toList()))
                    .roomDocsId(roomOf.get(childId))
                    .seatNo(mark.seatNo() == null || mark.seatNo().isBlank() ? null : mark.seatNo().trim())
                    .reportedAt(mark.reportedAt())
                    .submittedAt(mark.submittedAt())
                    .remarks(mark.remarks() == null || mark.remarks().isBlank() ? null : mark.remarks().trim())
                    .evidenceDocumentDocsIds(old != null && old.getEvidenceDocumentDocsIds() != null
                            ? old.getEvidenceDocumentDocsIds() : new ArrayList<>())
                    .markedAt(now)
                    .markedByDocsId(markedBy)
                    .build();
            if (old != null) {
                candidate.getPapers().set(candidate.getPapers().indexOf(old), entry);
            } else {
                candidate.getPapers().add(entry);
            }
            toSave.add(candidate);
        }

        //! Step 10 - Save everything together. A candidate or paper changed in between fails it all.
        // TODO: update exam candidates (their hall entries for this paper)
        examCandidates.saveAll(toSave);
        // TODO: update exam schedule (move its version on)
        ExamSchedule savedPaper = examSchedules.save(paper);

        //! The first hall save starts the exam.
        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(), school.getId(),
                year.getName()).orElse(null);
        if (exam != null && exam.getStatus() == ExamStatus.PUBLISHED) {
            exam.setStatus(ExamStatus.IN_PROGRESS);
            // TODO: update exam (in progress)
            exams.save(exam);
            log.info("[markHall] Step 2: Exam {} is now IN_PROGRESS", exam.getId());
        }
        log.info("[markHall] Step 3: Marked {} child(ren) at paper {}; its version is now {}",
                toSave.size(), paperId, savedPaper.getVersion());

        //! Step 11 - The register as it now stands — the same answer #14 gives.
        ExamAttendanceServiceUtils.HallRoster after = hallUtils.rosterOf(school, year.getName(), savedPaper);
        return hallUtils.registerOf(school, year.getName(), savedPaper, after);
    }
}
