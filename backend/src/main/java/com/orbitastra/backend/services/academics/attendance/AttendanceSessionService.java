package com.orbitastra.backend.services.academics.attendance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.current.CurrentUserResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.AcademicYearWindow;
import com.orbitastra.backend.common.time.AcademicYearWindow.Bound;
import com.orbitastra.backend.common.time.AcademicYearWindow.Refusal;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionCreateRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionLockRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionUnlockRequest;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionSearchRequest;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceRegisterResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceSessionResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceSessionRowResponse;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;
import com.orbitastra.backend.models.academics.attendance.embedded.AttendanceUnlock;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.attendance.AttendanceSessionRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepositoryCustom.SessionStatusCount;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.timetable.DailyTimetableRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.services.academics.attendance.utils.AttendanceServiceUtils;
import com.orbitastra.backend.services.academics.timetable.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attendance registers — one per section, per date, per kind. Endpoints #1, #3, #4, #6 and #7 of the plan
 * in {@code controllers/academics/attendance}; the rest are not built.
 *
 * <p>The year, class and holiday reads are the timetable's own utils, because they answer the
 * same questions about the same year: a register is opened on a day the timetable would also call
 * a working day.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceSessionService {

    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /** #7's order: newest date first, DAILY before PERIOD, then by period code. */
    private static final Sort SESSION_ORDER = Sort.by(Sort.Order.desc("attendanceDate"),
            Sort.Order.asc("sessionType"), Sort.Order.asc("periodCode"), Sort.Order.asc("id"));

    private final AttendanceSessionRepository sessions;
    private final StudentAttendanceRecordRepository marks;
    private final StudentAcademicRecordRepository academicRecords;
    private final SchoolClassRepository schoolClasses;
    private final DailyTimetableRepository timetables;
    private final StaffRepository staff;
    private final DailyTimetableServiceUtils utils;
    private final AttendanceServiceUtils attendanceUtils;
    private final AcademicYearWindow yearWindow;
    private final CurrentSchoolResolver currentSchool;
    private final CurrentUserResolver currentUser;

    /**
     * Endpoint #1 — <b>open a register</b> for one section on one date.
     *
     * <p>{@code DAILY} is taken once for the day; {@code PERIOD} is one timetable period, and its
     * timetable entry and teacher are copied from that period.
     *
     * <p><b>Opening a register that exists is refused and names it</b>, so a second teacher's
     * screen goes to the one already open. <b>A CANCELLED register is opened again</b> instead —
     * the unique index would refuse a new one, and a register cancelled by mistake is the likeliest
     * thing to be re-opened. Its marks are kept.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    public AttendanceSessionResponse openSession(String academicYear,
            AttendanceSessionCreateRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String classId = request.classDocsId().trim();
        String sectionNo = request.sectionNo().trim();
        String periodCode = TextHelper.blankToNull(request.periodCode());
        String timetableSent = TextHelper.blankToNull(request.timetableDocsId());
        String teacherSent = TextHelper.blankToNull(request.teacherDocsId());
        log.info("[openSession] Step 1: Opening a {} register for class {} section {} on {} in "
                + "school {}", request.sessionType(), classId, sectionNo,
                request.attendanceDate(), school.getId());

        //! Step 2 - Validate the session type and period code.
        if (request.sessionType() == AttendanceSessionType.PERIOD && periodCode == null) {
            throw ApiException.badRequest("PERIOD_CODE_REQUIRED",
                    "A PERIOD register needs periodCode — the period it is taken for.");
        }
        if (request.sessionType() == AttendanceSessionType.DAILY && periodCode != null) {
            throw ApiException.badRequest("PERIOD_CODE_NOT_ALLOWED",
                    "A DAILY register is taken once for the day, so it has no periodCode. Send "
                            + "sessionType PERIOD to take attendance for one period.");
        }
        //! A DAILY register is not tied to the timetable, so it takes no timetable id either.
        if (request.sessionType() == AttendanceSessionType.DAILY && timetableSent != null) {
            throw ApiException.badRequest("TIMETABLE_DOCS_ID_NOT_ALLOWED",
                    "A DAILY register is not taken against the timetable, so it has no "
                            + "timetableDocsId. Leave it out.");
        }

        //! Step 3 - Check that the academic year exists and the attendance date is within it.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        yearWindow.requireDates(year, Bound.INSIDE_THE_YEAR,
                Refusal.conflict("DATE_OUTSIDE_ACADEMIC_YEAR"),
                Map.of("attendanceDate", request.attendanceDate()));

        //! Step 4 - Check that the date is a working day.
        String holiday = utils.holidayNameFor(year, request.attendanceDate());
        if (holiday != null) {
            throw ApiException.notFound("NOT_A_WORKING_DAY",
                    request.attendanceDate() + " is " + holiday + " for this school, so no "
                            + "attendance is taken that day.");
        }

        //! Step 5 - Find the class and validate the section.
        SchoolClass schoolClass = utils.loadClassForYear(school, year.getName(), classId);
        ClassSection section = (schoolClass.getSections() == null ? List.<ClassSection>of()
                : schoolClass.getSections()).stream()
                .filter(one -> sectionNo.equals(one.getSectionNo()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("SECTION_NOT_IN_CLASS",
                        "Class '" + schoolClass.getName() + "' has no section '" + sectionNo
                                + "'."));

        if (Boolean.FALSE.equals(section.getActive())) {
            throw ApiException.conflict("SECTION_NOT_ACTIVE",
                    "Section '" + sectionNo + "' of '" + schoolClass.getName()
                            + "' is not active.");
        }

        //! Step 6 - For PERIOD attendance, find the period in the day's timetable.
        TimetableEntry period = null;
        DailyTimetable day = null;

        if (request.sessionType() == AttendanceSessionType.PERIOD) {
            // TODO: read daily timetable
            day = timetables
                    .findBySchoolIdAndDate(school.getId(), request.attendanceDate())
                    .filter(one -> year.getName().equals(one.getAcademicYear()))
                    .orElseThrow(() -> ApiException.notFound("PERIOD_NOT_IN_TIMETABLE",
                            "No timetable has been written for " + request.attendanceDate()
                                    + ", so period '" + periodCode + "' cannot be found."));

            //! The timetable id, when sent, has to be this date's timetable. Sent from a screen that
            //! read the day, so a different id means the screen was looking at another day.
            if (timetableSent != null && !timetableSent.equals(day.getId())) {
                throw ApiException.badRequest("TIMETABLE_NOT_FOR_DATE",
                        "Timetable '" + timetableSent + "' is not the timetable for "
                                + request.attendanceDate() + ". The timetable for that date is '"
                                + day.getId() + "'.");
            }

            period = (day.getEntries() == null ? List.<TimetableEntry>of() : day.getEntries())
                    .stream()
                    .filter(one -> classId.equals(one.getClassDocsId())
                            && sectionNo.equals(one.getSectionNo())
                            && periodCode.equals(one.getPeriodCode()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.notFound("PERIOD_NOT_IN_TIMETABLE",
                            "Section '" + sectionNo + "' of '" + schoolClass.getName()
                                    + "' has no period '" + periodCode + "' on "
                                    + request.attendanceDate() + "."));
        }

        //! Step 7 - Find the teacher or use the default teacher.
        String teacherId = teacherSent != null
                ? teacherSent
                : (period != null ? period.getTeacherDocsId() : section.getClassTeacherDocsId());

        String teacherName = null;
        if (teacherId != null) {
            // TODO: read staff
            Staff teacher = staff.findByIdAndSchoolId(teacherId, school.getId()).orElse(null);
            if (teacher == null && teacherSent != null) {
                throw ApiException.notFound("TEACHER_NOT_FOUND",
                        "No staff member with id '" + teacherSent + "' in this school.");
            }
            teacherName = teacher == null ? null : teacher.getFullName();
        }

        //! Step 8 - Check whether the attendance register already exists.
        // TODO: read attendance session (the same register)
        AttendanceSession existing = sessions
                .findBySchoolIdAndAcademicYearAndAttendanceDateAndClassDocsIdAndSectionNoAndSessionTypeAndPeriodCode(
                        school.getId(), year.getName(), request.attendanceDate(), classId,
                        sectionNo, request.sessionType(), periodCode)
                .orElse(null);

        if (existing != null && existing.getStatus() != AttendanceSessionStatus.CANCELLED) {
            throw ApiException.conflict("SESSION_ALREADY_OPEN",
                    "This register already exists (id '" + existing.getId() + "', "
                            + existing.getStatus() + "). Open that one instead of a new one.");
        }

        //! Step 9 - Create a new register or reopen the cancelled register.
        boolean reopened = existing != null;
        AttendanceSession session = reopened ? existing : AttendanceSession.builder()
                .schoolId(school.getId())
                .academicYear(year.getName())
                .attendanceDate(request.attendanceDate())
                .classDocsId(classId)
                .sectionNo(sectionNo)
                .sessionType(request.sessionType())
                .periodCode(periodCode)
                .build();

        session.setStatus(AttendanceSessionStatus.OPEN);
        session.setLockedAt(null);
        session.setLockedByDocsId(null);
        session.setTeacherDocsId(teacherId);
        session.setTimetableDocsId(day == null ? null : day.getId());

        //! Step 10 - Save the register and return the response.
        // TODO: insert attendance session (or update the cancelled one being re-opened)
        AttendanceSession saved = sessions.save(session);
        log.info("[openSession] Step 2: {} register {}", reopened ? "Re-opened" : "Opened",
                saved.getId());

        return AttendanceSessionResponse.of(saved, schoolClass.getName(), teacherName, reopened,
                (reopened
                        ? "A cancelled register for this section and date was opened again, with "
                                + "its marks kept. "
                        : "The register is open. ")
                        + "Mark the children with attendance #2. "
                        + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #7 — <b>registers, one page at a time</b>, filtered by date, class, section, kind
     * or status. Each row carries how many marks of each status it holds, from <b>one aggregation
     * for the page</b>; the class and teacher names are one read each.
     *
     * <p><b>No gates.</b> A read.
     */
    public PageResponse<AttendanceSessionRowResponse> listSessions(String academicYear,
            AttendanceSessionSearchRequest request) {

        //! Step 1 - The paging, checked first. There is no caller sort; the order is fixed.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), null,
                Map.of(), "", SESSION_ORDER);

        //! Step 2 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[listSessions] Step 1: Listing attendance registers of year {} in school {}",
                academicYear, school.getId());

        //! Step 3 - The year has to be this school's.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! Step 4 - One page of registers.
        // TODO: search attendance sessions
        Page<AttendanceSession> page = sessions.search(school.getId(), year.getName(), request,
                pageable);
        List<AttendanceSession> rows = page.getContent();

        //! Step 5 - The marks of every register on the page, counted in one aggregation.
        Map<String, Map<String, Long>> countsBySession = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            // TODO: read student attendance records (count per register and status)
            for (SessionStatusCount one : marks.countBySessionAndStatus(school.getId(),
                    rows.stream().map(AttendanceSession::getId).toList())) {
                countsBySession.computeIfAbsent(one.attendanceSessionDocsId(), k -> new LinkedHashMap<>())
                        .merge(String.valueOf(one.status()), one.count(), Long::sum);
            }
        }

        //! Step 6 - The class and teacher names on the page, one read each.
        Map<String, String> classNames = new LinkedHashMap<>();
        Set<String> classIds = new HashSet<>();
        Set<String> teacherIds = new HashSet<>();
        for (AttendanceSession one : rows) {
            classIds.add(one.getClassDocsId());
            if (one.getTeacherDocsId() != null) {
                teacherIds.add(one.getTeacherDocsId());
            }
        }
        if (!classIds.isEmpty()) {
            // TODO: read school classes (the classes on this page)
            for (SchoolClass one : schoolClasses.findBySchoolIdAndIdIn(school.getId(), classIds)) {
                classNames.put(one.getId(), one.getName());
            }
        }
        Map<String, String> teacherNames = new LinkedHashMap<>();
        if (!teacherIds.isEmpty()) {
            // TODO: read staff (the teachers on this page)
            for (Staff one : staff.findBySchoolIdAndIdIn(school.getId(), teacherIds)) {
                teacherNames.put(one.getId(), one.getFullName());
            }
        }

        log.info("[listSessions] Step 2: Found {} register(s), showing {}", page.getTotalElements(),
                rows.size());

        return PageResponse.from(page, one -> {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (AttendanceStatus status : AttendanceStatus.values()) {
                counts.put(status.name(), 0L);
            }
            countsBySession.getOrDefault(one.getId(), Map.of())
                    .forEach((status, count) -> counts.merge(status, count, Long::sum));

            return new AttendanceSessionRowResponse(
                    one.getId(),
                    one.getAttendanceDate(),
                    one.getClassDocsId(),
                    classNames.get(one.getClassDocsId()),
                    one.getSectionNo(),
                    one.getSessionType(),
                    one.getPeriodCode(),
                    one.getTeacherDocsId(),
                    one.getTeacherDocsId() == null ? null : teacherNames.get(one.getTeacherDocsId()),
                    one.getStatus(),
                    counts.values().stream().mapToLong(Long::longValue).sum(),
                    counts,
                    one.getVersion());
        });
    }

    /**
     * Endpoint #6 — <b>one register in full</b>: the header, and every child who belongs in it
     * with their mark or none yet. What the teacher's screen opens.
     *
     * <p><b>Who belongs is who was in the section on the register's date</b> — read with
     * {@code StudentAcademicRecordRepository.findInSectionOn}, the same question the roster asks.
     * A child who has a mark but is no longer in the section for that date is still listed, with
     * {@code onRoster: false}, so no mark is ever hidden.
     *
     * <p><b>Four reads</b> — the register, the children's placements, their names, the marks —
     * never one per child.
     *
     * <p><b>No gates.</b> A read.
     */
    public AttendanceRegisterResponse getRegister(String academicYear, String attendanceSessionDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String sessionId = attendanceSessionDocsId == null ? "" : attendanceSessionDocsId.trim();
        log.info("[getRegister] Step 1: Reading register {} in school {}", sessionId,
                school.getId());

        //! Step 2 - The year, then the register inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read attendance session
        AttendanceSession session = sessions
                .findByIdAndSchoolIdAndAcademicYear(sessionId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("ATTENDANCE_SESSION_NOT_FOUND",
                        "No attendance register with id '" + sessionId + "' in '" + year.getName()
                                + "'."));

        //! Step 3 - The register as the answer shows it: every child who was in the section that
        //! day, with their mark or none yet.
        return attendanceUtils.registerOf(school, session);
    }

    /**
     * Endpoint #3 — <b>lock a register</b>: it is final, and takes no more marks.
     *
     * <p><b>Refused while any child is unmarked</b> — every child who was in the section that day
     * must have a mark first, so a lock never hides a gap. A register with nobody in it can be
     * locked.
     *
     * <p><b>The version is the register's</b>, as last read: a register changed since is refused,
     * so nobody locks one they have not seen. Records when, and — when the caller is signed in as
     * staff — by whom.
     *
     * <p>Answers with the register as it now stands. Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public AttendanceRegisterResponse lockRegister(String academicYear, String attendanceSessionDocsId,
            AttendanceSessionLockRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String sessionId = attendanceSessionDocsId == null ? "" : attendanceSessionDocsId.trim();
        log.info("[lockRegister] Step 1: Locking register {} of school {}", sessionId,
                school.getId());

        //! Step 2 - The year, then the register inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read attendance session
        AttendanceSession session = sessions
                .findByIdAndSchoolIdAndAcademicYear(sessionId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("ATTENDANCE_SESSION_NOT_FOUND",
                        "No attendance register with id '" + sessionId + "' in '" + year.getName()
                                + "'."));

        //! Step 3 - Only an OPEN register can be locked.
        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw ApiException.conflict("SESSION_NOT_OPEN",
                    "This register is " + session.getStatus() + ", so it cannot be locked.");
        }

        //! Step 4 - Check the register was not changed by somebody else since it was read.
        if (!request.version().equals(session.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This register was changed by someone else. Please read it again before "
                            + "locking it.");
        }

        //! Step 5 - Every child who was in the section that day needs a mark first.
        // TODO: read student academic records (who was in this section on this date)
        Set<String> onRoster = new HashSet<>();
        for (StudentAcademicRecord one : academicRecords.findInSectionOn(school.getId(),
                year.getName(), session.getClassDocsId(), session.getSectionNo(),
                session.getAttendanceDate())) {
            onRoster.add(one.getStudentDocsId());
        }

        // TODO: read student attendance records (the marks in this register)
        for (StudentAttendanceRecord one : marks.findBySchoolIdAndAttendanceSessionDocsId(
                school.getId(), session.getId())) {
            onRoster.remove(one.getStudentDocsId());
        }

        if (!onRoster.isEmpty()) {
            throw ApiException.conflict("UNMARKED_STUDENTS",
                    onRoster.size() + " child(ren) in this register have no mark yet. Mark every "
                            + "child before locking it.");
        }

        //! Step 6 - Lock it: when, and by whom when the caller is signed in as staff.
        session.setStatus(AttendanceSessionStatus.LOCKED);
        session.setLockedAt(Instant.now());
        session.setLockedByDocsId(currentUser.staffDocsIdOrNull());

        // TODO: update attendance session (lock it)
        AttendanceSession saved = sessions.save(session);
        log.info("[lockRegister] Step 2: Locked register {}", sessionId);

        //! Step 7 - The register as it now stands — the same answer #6 gives.
        return attendanceUtils.registerOf(school, saved);
    }

    /**
     * Endpoint #4 — <b>open a locked register again</b>, to correct it.
     *
     * <p><b>A reason is required, and it is saved</b> on the register's unlock history together
     * with when, by whom, and the lock it undid — re-opening a final register is exactly what
     * somebody asks about later. Unlocking clears {@code lockedAt} and {@code lockedByDocsId}; the
     * history is what keeps them.
     *
     * <p>Only a {@code LOCKED} register can be unlocked. The version is the register's, as last
     * read. Answers with the register as it now stands. Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public AttendanceRegisterResponse unlockRegister(String academicYear, String attendanceSessionDocsId,
            AttendanceSessionUnlockRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String sessionId = attendanceSessionDocsId == null ? "" : attendanceSessionDocsId.trim();
        String reason = TextHelper.blankToNull(request.reason());
        log.info("[unlockRegister] Step 1: Unlocking register {} of school {}", sessionId,
                school.getId());

        //! Step 2 - A reason is required, checked before anything is read.
        if (reason == null) {
            throw ApiException.badRequest("REASON_REQUIRED",
                    "Say why this register is being opened again. The reason is saved with it.");
        }

        //! Step 3 - The year, then the register inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read attendance session
        AttendanceSession session = sessions
                .findByIdAndSchoolIdAndAcademicYear(sessionId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("ATTENDANCE_SESSION_NOT_FOUND",
                        "No attendance register with id '" + sessionId + "' in '" + year.getName()
                                + "'."));

        //! Step 4 - Only a LOCKED register can be unlocked.
        if (session.getStatus() != AttendanceSessionStatus.LOCKED) {
            throw ApiException.conflict("SESSION_NOT_LOCKED",
                    "This register is " + session.getStatus() + ", so there is nothing to unlock.");
        }

        //! Step 5 - Check the register was not changed by somebody else since it was read.
        if (!request.version().equals(session.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This register was changed by someone else. Please read it again before "
                            + "unlocking it.");
        }

        //! Step 6 - Record the unlock — the reason, when, by whom, and the lock it undoes — then
        //! open the register again.
        AttendanceUnlock unlock = AttendanceUnlock.builder()
                .reason(reason)
                .unlockedAt(Instant.now())
                .unlockedByDocsId(currentUser.staffDocsIdOrNull())
                .previousLockedAt(session.getLockedAt())
                .previousLockedByDocsId(session.getLockedByDocsId())
                .build();

        List<AttendanceUnlock> history = session.getUnlocks() == null
                ? new ArrayList<>()
                : new ArrayList<>(session.getUnlocks());
        history.add(unlock);

        session.setUnlocks(history);
        session.setStatus(AttendanceSessionStatus.OPEN);
        session.setLockedAt(null);
        session.setLockedByDocsId(null);

        // TODO: update attendance session (unlock it)
        AttendanceSession saved = sessions.save(session);
        log.info("[unlockRegister] Step 2: Unlocked register {} — unlock number {}", sessionId,
                history.size());

        //! Step 7 - The register as it now stands — the same answer #6 gives.
        return attendanceUtils.registerOf(school, saved);
    }
}
