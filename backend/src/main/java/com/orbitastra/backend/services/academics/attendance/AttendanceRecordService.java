package com.orbitastra.backend.services.academics.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.AcademicYearWindow;
import com.orbitastra.backend.common.time.AcademicYearWindow.Bound;
import com.orbitastra.backend.common.time.AcademicYearWindow.Refusal;
import com.orbitastra.backend.dto.academics.attendance.request.AttendanceMarksRequest;
import com.orbitastra.backend.dto.academics.attendance.response.AbsenteesResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceDayResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceRegisterResponse;
import com.orbitastra.backend.dto.academics.attendance.response.SectionAttendanceResponse;
import com.orbitastra.backend.dto.academics.attendance.response.StudentAttendanceResponse;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;
import com.orbitastra.backend.models.academics.enums.AttendanceSource;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.attendance.AttendanceSessionRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepositoryCustom.SessionStatusCount;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepositoryCustom.StudentStatusCount;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepositoryCustom.SectionCount;
import com.orbitastra.backend.services.academics.attendance.utils.AttendanceServiceUtils;
import com.orbitastra.backend.services.academics.timetable.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attendance records — the marks, and the reads over them. Endpoints #2 and #8 to #11 of the plan
 * in {@code controllers/academics/attendance}.
 *
 * <p>Every report counts <b>DAILY registers only</b> (open item 8) and leaves cancelled registers
 * out. The year and holiday reads are the timetable's own utils, the same ones attendance #1 uses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceRecordService {

    private final SchoolClassRepository schoolClasses;
    private final AttendanceSessionRepository sessions;
    private final StudentAttendanceRecordRepository marks;
    private final StudentAcademicRecordRepository academicRecords;
    private final StudentRepository students;
    private final DailyTimetableServiceUtils utils;
    private final AttendanceServiceUtils attendanceUtils;
    private final AcademicYearWindow yearWindow;
    private final CurrentSchoolResolver currentSchool;

    /**
     * Endpoint #8 — <b>the office's morning question</b>: on one date, which sections have taken
     * attendance and which have not.
     *
     * <p>One row per active section of every active class. A section is <b>taken</b> when its
     * DAILY register exists and is not cancelled; period registers are counted beside it. Each
     * row carries the headcount for that date and how many are marked.
     *
     * <p><b>Four reads for the whole school</b> — the classes, the day's registers, one count of
     * children per section, one count of marks — never one per section.
     *
     * <p><b>No gates.</b> A read.
     */
    public AttendanceDayResponse getDay(String academicYear, LocalDate date) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[getDay] Step 1: Reading attendance for {} in school {}", date, school.getId());

        //! Step 2 - The year, and the date has to be inside it and a working day.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        yearWindow.requireDates(year, Bound.INSIDE_THE_YEAR,
                Refusal.conflict("DATE_OUTSIDE_ACADEMIC_YEAR"), Map.of("date", date));

        String holiday = utils.holidayNameFor(year, date);
        if (holiday != null) {
            throw ApiException.notFound("NOT_A_WORKING_DAY",
                    date + " is " + holiday + " for this school, so no attendance is taken that "
                            + "day.");
        }

        //! Step 3 - Every class of the year, for the sections and their names.
        // TODO: read school classes (every class of the year)
        List<SchoolClass> classes = new ArrayList<>(
                schoolClasses.findBySchoolIdAndAcademicYear(school.getId(), year.getName()));
        classes.sort(Comparator.comparing(SchoolClass::getName,
                Comparator.nullsLast(String::compareTo)));

        //! Step 4 - Every register of the date. The DAILY one per section is what "taken" means;
        //! PERIOD registers that are not cancelled are only counted.
        // TODO: read attendance sessions (every register of the date)
        Map<String, AttendanceSession> dailyBySection = new LinkedHashMap<>();
        Map<String, Integer> periodsBySection = new LinkedHashMap<>();
        for (AttendanceSession one : sessions.findBySchoolIdAndAcademicYearAndAttendanceDate(
                school.getId(), year.getName(), date)) {
            String key = one.getClassDocsId() + "|" + one.getSectionNo();
            if (one.getSessionType() == AttendanceSessionType.DAILY) {
                dailyBySection.put(key, one);
            } else if (one.getStatus() != AttendanceSessionStatus.CANCELLED) {
                periodsBySection.merge(key, 1, Integer::sum);
            }
        }

        //! Step 5 - How many children were in each section that day, in one aggregation.
        // TODO: read student academic records (how many in each section on this date)
        Map<String, Long> headcount = new LinkedHashMap<>();
        for (SectionCount one : academicRecords.countInSectionsOn(school.getId(), year.getName(),
                date)) {
            headcount.put(one.classDocsId() + "|" + one.sectionNo(), one.enrolled());
        }

        //! Step 6 - The marks of every DAILY register, counted per register and status in one
        //! aggregation.
        Map<String, Map<String, Long>> marksBySession = new LinkedHashMap<>();
        List<String> dailyIds = dailyBySection.values().stream().map(AttendanceSession::getId)
                .toList();
        if (!dailyIds.isEmpty()) {
            // TODO: read student attendance records (count per register and status)
            for (SessionStatusCount one : marks.countBySessionAndStatus(school.getId(), dailyIds)) {
                marksBySession.computeIfAbsent(one.attendanceSessionDocsId(),
                        k -> new LinkedHashMap<>())
                        .merge(String.valueOf(one.status()), one.count(), Long::sum);
            }
        }

        //! Step 7 - One row per active section of every active class.
        List<AttendanceDayResponse.SectionRow> rows = new ArrayList<>();
        int taken = 0;
        int locked = 0;

        for (SchoolClass schoolClass : classes) {
            if (Boolean.FALSE.equals(schoolClass.getActive())) {
                continue;
            }
            for (ClassSection section : schoolClass.getSections() == null
                    ? List.<ClassSection>of() : schoolClass.getSections()) {
                if (Boolean.FALSE.equals(section.getActive())) {
                    continue;
                }
                String key = schoolClass.getId() + "|" + section.getSectionNo();
                AttendanceSession daily = dailyBySection.get(key);
                boolean isTaken = daily != null
                        && daily.getStatus() != AttendanceSessionStatus.CANCELLED;

                Map<String, Long> counts = new LinkedHashMap<>();
                for (AttendanceStatus status : AttendanceStatus.values()) {
                    counts.put(status.name(), 0L);
                }
                if (isTaken) {
                    marksBySession.getOrDefault(daily.getId(), Map.of())
                            .forEach((status, count) -> counts.merge(status, count, Long::sum));
                }
                long marked = counts.values().stream().mapToLong(Long::longValue).sum();
                long students = headcount.getOrDefault(key, 0L);

                if (isTaken) {
                    taken++;
                    if (daily.getStatus() == AttendanceSessionStatus.LOCKED) {
                        locked++;
                    }
                }

                rows.add(new AttendanceDayResponse.SectionRow(
                        schoolClass.getId(),
                        schoolClass.getName(),
                        section.getSectionNo(),
                        students,
                        isTaken,
                        daily == null ? null : daily.getId(),
                        daily == null ? null : daily.getStatus(),
                        marked,
                        Math.max(0, students - marked),
                        counts,
                        periodsBySection.getOrDefault(key, 0)));
            }
        }

        log.info("[getDay] Step 2: {} of {} section(s) have taken attendance on {}", taken,
                rows.size(), date);

        return new AttendanceDayResponse(year.getName(), date, rows.size(), taken,
                rows.size() - taken, locked, rows);
    }

    /**
     * Endpoint #2 — <b>mark the children</b> in one register: one, several or all at once.
     *
     * <p><b>Each mark is saved on its own.</b> Sending a child who already has a mark corrects it;
     * children not sent are left alone. <b>The register's version is checked and moved on</b> with
     * every save, so a save from a stale copy of the register is refused rather than silently
     * overwriting someone else's marks.
     *
     * <p><b>Everything is checked before anything is written</b> — a child not in the section that
     * day, or a missing reason, refuses the whole request and names the child. Nothing is half
     * saved.
     *
     * <p>Answers with the register as it now stands (the same answer #6 gives). Gates 1, 2 and 4
     * run in the controller.
     */
    @Transactional
    public AttendanceRegisterResponse markRegister(String academicYear, String attendanceSessionDocsId, AttendanceMarksRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String sessionId = attendanceSessionDocsId == null ? "" : attendanceSessionDocsId.trim();
        log.info("[markRegister] Step 1: Marking {} child(ren) in register {} of school {}", request.marks().size(), sessionId, school.getId());

        //! Step 2 - There has to be something to mark.
        if (request.marks().isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_MARK", "Send at least one mark.");
        }

        //! Step 3 - Each child once, and a reason with every mark that is not PRESENT. Checked
        //! before anything is read.
        Set<String> seen = new HashSet<>();
        List<String> missingReason = new ArrayList<>();
        for (AttendanceMarksRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            if (!seen.add(childId)) {
                throw ApiException.badRequest("DUPLICATE_STUDENT_IN_REQUEST", "Student '" + childId + "' is in marks twice. Send each child once.");
            }
            if (mark.status() != AttendanceStatus.PRESENT
                    && TextHelper.blankToNull(mark.reason()) == null) {
                missingReason.add(childId);
            }
        }

        //! Step 4 - The year, then the register inside it. Only an OPEN register takes marks, and
        //! only from someone who has seen it as it now stands.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read attendance session
        AttendanceSession session = sessions
                .findByIdAndSchoolIdAndAcademicYear(sessionId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("ATTENDANCE_SESSION_NOT_FOUND", "No attendance register with id '" + sessionId + "' in '" + year.getName() + "'."));

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "This register is " + session.getStatus() + ", so it takes no marks.");
        }

        if (!request.version().equals(session.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This register was changed by someone else. Please read it again before "
                            + "saving marks.");
        }

        //! Step 5 - Who was in the section on the register's date. Each mark is stored against
        //! the placement the child held that day.
        // TODO: read student academic records (who was in this section on this date)
        Map<String, StudentAcademicRecord> placementOf = new LinkedHashMap<>();
        for (StudentAcademicRecord one : academicRecords.findInSectionOn(school.getId(),
                year.getName(), session.getClassDocsId(), session.getSectionNo(),
                session.getAttendanceDate())) {
            placementOf.putIfAbsent(one.getStudentDocsId(), one);
        }

        //! Step 6 - Every child sent has to be in the register. All of them are named at once.
        List<String> notInRegister = new ArrayList<>();
        for (AttendanceMarksRequest.Mark mark : request.marks()) {
            if (!placementOf.containsKey(mark.studentDocsId().trim())) {
                notInRegister.add(mark.studentDocsId().trim());
            }
        }
        if (!notInRegister.isEmpty()) {
            throw ApiException.badRequest("STUDENT_NOT_IN_REGISTER",
                    "Not in section " + session.getSectionNo() + " on "
                            + session.getAttendanceDate() + ": " + String.join(", ", notInRegister)
                            + ". Nothing was saved.");
        }
        if (!missingReason.isEmpty()) {
            throw ApiException.badRequest("REASON_REQUIRED",
                    "A reason is needed for every mark that is not PRESENT. Missing for: "
                            + String.join(", ", missingReason) + ". Nothing was saved.");
        }

        //! Step 7 - The marks already in the register, so a child sent again is corrected rather
        //! than given a second mark.
        // TODO: read student attendance records (the marks in this register)
        Map<String, StudentAttendanceRecord> existing = new LinkedHashMap<>();
        for (StudentAttendanceRecord one : marks.findBySchoolIdAndAttendanceSessionDocsId(
                school.getId(), session.getId())) {
            existing.put(one.getStudentDocsId(), one);
        }

        //! Step 8 - Build every mark. A child with a mark already gets it corrected; the departure
        //! time is not part of this request, so a stored one is left as it is.
        List<StudentAttendanceRecord> toSave = new ArrayList<>();
        for (AttendanceMarksRequest.Mark mark : request.marks()) {
            String childId = mark.studentDocsId().trim();
            StudentAttendanceRecord row = existing.getOrDefault(childId,
                    StudentAttendanceRecord.builder()
                            .schoolId(school.getId())
                            .academicYear(year.getName())
                            .studentDocsId(childId)
                            .attendanceSessionDocsId(session.getId())
                            .attendanceDate(session.getAttendanceDate())
                            .build());
            row.setStudentAcademicRecordDocsId(placementOf.get(childId).getId());
            row.setStatus(mark.status());
            row.setReason(mark.status() == AttendanceStatus.PRESENT
                    ? TextHelper.blankToNull(mark.reason())
                    : mark.reason().trim());
            row.setArrivalAt(mark.arrivalAt());
            row.setSource(AttendanceSource.MANUAL);
            toSave.add(row);
        }

        //! Step 9 - Save them all, in one transaction. The unique index on register and child is
        //! what stops two requests giving one child two marks.
        // TODO: insert or update student attendance records
        marks.saveAll(toSave);
        log.info("[markRegister] Step 2: Saved {} mark(s)", toSave.size());

        //! Step 10 - Move the register's version on, in the same transaction, so the next save
        //! from a stale copy is refused. Two saves racing past Step 4 together meet here: the
        //! second fails on the version and its marks are rolled back.
        // TODO: update attendance session (move its version on)
        AttendanceSession saved = sessions.save(session);

        //! Step 11 - The register as it now stands — the same answer #6 gives.
        return attendanceUtils.registerOf(school, saved);
    }

    /**
     * Endpoint #9 — <b>one child's attendance</b> between two dates: every mark, newest first, and
     * the totals.
     *
     * <p><b>The totals count DAILY registers only</b> (open item 8) — period marks are listed but
     * leave the percentage alone. <b>LATE counts as attended</b> (open item 7) and is also counted
     * on its own. Marks in a cancelled register are left out entirely.
     *
     * <p>{@code from} and {@code to} default to the year's own start and end. <b>Three reads</b>:
     * the child, their marks, the registers behind them.
     *
     * <p><b>No gates.</b> A read.
     */
    public StudentAttendanceResponse getStudentAttendance(String academicYear, String studentDocsId,
            LocalDate from, LocalDate to) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String childId = studentDocsId == null ? "" : studentDocsId.trim();
        log.info("[getStudentAttendance] Step 1: Reading attendance of student {} in school {}",
                childId, school.getId());

        //! Step 2 - The year, and the range: the year's own dates when none are sent.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        LocalDate start = from != null ? from : year.getStartDate();
        LocalDate end = to != null ? to : year.getEndDate();

        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "from (" + start + ") is after to (" + end + ").");
        }

        //! Step 3 - The child, scoped by school. Another school's child is a 404.
        // TODO: read student
        Student child = students.findByIdAndSchoolId(childId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + childId + "' in this school."));

        //! Step 4 - Their marks in the range, newest first.
        // TODO: read student attendance records (one child's marks in a range)
        List<StudentAttendanceRecord> taken = marks.findForStudent(school.getId(), child.getId(),
                year.getName(), start, end);

        //! Step 5 - The registers behind those marks, in one read, to know each one's kind and
        //! status. A mark in a cancelled register is left out.
        Map<String, AttendanceSession> sessionById = new LinkedHashMap<>();
        if (!taken.isEmpty()) {
            // TODO: read attendance sessions (the registers behind these marks)
            for (AttendanceSession one : sessions.findBySchoolIdAndIdIn(school.getId(),
                    taken.stream().map(StudentAttendanceRecord::getAttendanceSessionDocsId)
                            .distinct().toList())) {
                sessionById.put(one.getId(), one);
            }
        }

        //! Step 6 - One row per mark, and the DAILY totals.
        Map<String, Long> dailyCounts = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            dailyCounts.put(status.name(), 0L);
        }
        List<StudentAttendanceResponse.Row> rows = new ArrayList<>();
        long periodMarks = 0;

        for (StudentAttendanceRecord mark : taken) {
            AttendanceSession session = sessionById.get(mark.getAttendanceSessionDocsId());
            if (session == null || session.getStatus() == AttendanceSessionStatus.CANCELLED) {
                continue;
            }
            if (session.getSessionType() == AttendanceSessionType.DAILY) {
                dailyCounts.merge(mark.getStatus().name(), 1L, Long::sum);
            } else {
                periodMarks++;
            }
            rows.add(new StudentAttendanceResponse.Row(
                    mark.getAttendanceDate(),
                    session.getSessionType(),
                    session.getPeriodCode(),
                    session.getClassDocsId(),
                    session.getSectionNo(),
                    mark.getStatus(),
                    mark.getReason(),
                    mark.getArrivalAt(),
                    session.getId(),
                    session.getStatus()));
        }

        long marked = dailyCounts.values().stream().mapToLong(Long::longValue).sum();
        long attended = dailyCounts.get(AttendanceStatus.PRESENT.name())
                + dailyCounts.get(AttendanceStatus.LATE.name());
        Double percentage = marked == 0 ? null
                : Math.round(attended * 1000.0 / marked) / 10.0;

        log.info("[getStudentAttendance] Step 2: {} mark(s), {} of {} day(s) attended", rows.size(),
                attended, marked);

        return new StudentAttendanceResponse(child.getId(), child.getFullName(),
                child.getAdmissionNo(), year.getName(), start, end, dailyCounts, marked, attended,
                percentage, periodMarks, rows);
    }

    /**
     * Endpoint #10 — <b>the monthly register</b> of one section: one row per child who was in it
     * at any time between two dates, with their counts and percentage.
     *
     * <p>The same rules as #9 — <b>DAILY registers only</b>, <b>LATE counts as attended</b>,
     * cancelled registers left out. Each child is counted only against the registers held while
     * they were in the section, so one who joined mid-month is not shown as absent before it.
     *
     * <p>{@code from} and {@code to} default to the year's own dates. <b>Five reads</b> — the
     * class, the placements, the registers, <b>one aggregation</b> of every mark, the names.
     *
     * <p><b>No gates.</b> A read.
     */
    public SectionAttendanceResponse getSectionAttendance(String academicYear, String classDocsId,
            String sectionNo, LocalDate from, LocalDate to) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String classId = classDocsId == null ? "" : classDocsId.trim();
        String section = sectionNo == null ? "" : sectionNo.trim();
        log.info("[getSectionAttendance] Step 1: Reading attendance of class {} section {} in "
                + "school {}", classId, section, school.getId());

        //! Step 2 - The year, and the range: the year's own dates when none are sent.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        LocalDate start = from != null ? from : year.getStartDate();
        LocalDate end = to != null ? to : year.getEndDate();

        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "from (" + start + ") is after to (" + end + ").");
        }

        //! Step 3 - The class of this year, and the section inside it.
        SchoolClass schoolClass = utils.loadClassForYear(school, year.getName(), classId);
        boolean hasSection = (schoolClass.getSections() == null ? List.<ClassSection>of()
                : schoolClass.getSections()).stream()
                .anyMatch(one -> section.equals(one.getSectionNo()));
        if (!hasSection) {
            throw ApiException.badRequest("SECTION_NOT_IN_CLASS",
                    "Class '" + schoolClass.getName() + "' has no section '" + section + "'.");
        }

        //! Step 4 - Every child in the section at any time in the range. A child can hold more
        //! than one placement in it (left and came back); the earliest start and latest end are
        //! kept, and the days are counted against the registers inside any of them.
        // TODO: read student academic records (who was in this section during the range)
        List<StudentAcademicRecord> placements = academicRecords.findInSectionBetween(
                school.getId(), year.getName(), classId, section, start, end);
        Map<String, List<StudentAcademicRecord>> placementsOf = new LinkedHashMap<>();
        for (StudentAcademicRecord one : placements) {
            placementsOf.computeIfAbsent(one.getStudentDocsId(), k -> new ArrayList<>()).add(one);
        }

        //! Step 5 - The section's DAILY registers in the range, cancelled ones left out.
        // TODO: read attendance sessions (one section's daily registers in a range)
        List<AttendanceSession> registers = sessions.findDailyForSection(school.getId(),
                year.getName(), classId, section, start, end);

        //! Step 6 - Every mark in those registers, counted per child and status in ONE aggregation.
        Map<String, Map<String, Long>> countsOf = new LinkedHashMap<>();
        if (!registers.isEmpty()) {
            // TODO: read student attendance records (count per child and status)
            for (StudentStatusCount one : marks.countByStudentAndStatus(school.getId(),
                    registers.stream().map(AttendanceSession::getId).toList())) {
                countsOf.computeIfAbsent(one.studentDocsId(), k -> new LinkedHashMap<>())
                        .merge(String.valueOf(one.status()), one.count(), Long::sum);
            }
        }

        //! Step 7 - The children's names, in one read.
        Map<String, Student> children = new LinkedHashMap<>();
        if (!placementsOf.isEmpty()) {
            // TODO: read students (the children of this section)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(),
                    placementsOf.keySet())) {
                children.put(one.getId(), one);
            }
        }

        //! Step 8 - One row per child.
        List<SectionAttendanceResponse.StudentRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<StudentAcademicRecord>> entry : placementsOf.entrySet()) {
            List<StudentAcademicRecord> held = entry.getValue();

            //! The registers held while this child was in the section.
            long registerDays = registers.stream()
                    .filter(register -> held.stream().anyMatch(one ->
                            !register.getAttendanceDate().isBefore(one.getEffectiveFrom())
                                    && (one.getEffectiveUntil() == null
                                            || !register.getAttendanceDate().isAfter(one.getEffectiveUntil()))))
                    .count();

            Map<String, Long> counts = new LinkedHashMap<>();
            for (AttendanceStatus status : AttendanceStatus.values()) {
                counts.put(status.name(), 0L);
            }
            countsOf.getOrDefault(entry.getKey(), Map.of())
                    .forEach((status, count) -> counts.merge(status, count, Long::sum));

            long marked = counts.values().stream().mapToLong(Long::longValue).sum();
            long attended = counts.get(AttendanceStatus.PRESENT.name())
                    + counts.get(AttendanceStatus.LATE.name());
            Double percentage = marked == 0 ? null
                    : Math.round(attended * 1000.0 / marked) / 10.0;

            LocalDate placedFrom = held.stream().map(StudentAcademicRecord::getEffectiveFrom)
                    .min(Comparator.naturalOrder()).orElse(null);
            LocalDate placedUntil = held.stream().anyMatch(one -> one.getEffectiveUntil() == null)
                    ? null
                    : held.stream().map(StudentAcademicRecord::getEffectiveUntil)
                            .max(Comparator.naturalOrder()).orElse(null);
            String rollNo = held.get(held.size() - 1).getRollNo();

            Student child = children.get(entry.getKey());
            rows.add(new SectionAttendanceResponse.StudentRow(
                    entry.getKey(),
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    rollNo,
                    placedFrom,
                    placedUntil,
                    registerDays,
                    marked,
                    attended,
                    Math.max(0, registerDays - marked),
                    counts,
                    percentage));
        }
        rows.sort(Comparator
                .comparing(SectionAttendanceResponse.StudentRow::rollNo,
                        AttendanceServiceUtils.ROLL_ORDER)
                .thenComparing(SectionAttendanceResponse.StudentRow::fullName,
                        Comparator.nullsLast(String::compareTo)));

        log.info("[getSectionAttendance] Step 2: {} child(ren) across {} register(s)", rows.size(),
                registers.size());

        return new SectionAttendanceResponse(year.getName(), schoolClass.getId(),
                schoolClass.getName(), section, start, end, registers.size(), rows.size(), rows);
    }

    /**
     * Endpoint #11 — <b>the absentees of one date</b>, for follow-up: every child marked absent
     * in a DAILY register, and whether their guardian has been told.
     *
     * <p><b>DAILY registers only</b>, so a child absent from three periods is one row, not three;
     * cancelled registers are left out. {@code guardianNotified} narrows to those told — or, with
     * {@code false}, to the follow-up list. The notification flags are read here, never written.
     *
     * <p><b>Four reads</b> — the absences, their registers, the children, the classes.
     *
     * <p><b>No gates.</b> A read.
     */
    public AbsenteesResponse getAbsentees(String academicYear, LocalDate date,
            Boolean guardianNotified) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[getAbsentees] Step 1: Reading the absentees of {} in school {}", date,
                school.getId());

        //! Step 2 - The year, and the date has to be inside it and a working day.
        AcademicYear year = utils.loadYearByName(school, academicYear);
        yearWindow.requireDates(year, Bound.INSIDE_THE_YEAR,
                Refusal.conflict("DATE_OUTSIDE_ACADEMIC_YEAR"), Map.of("date", date));

        String holiday = utils.holidayNameFor(year, date);
        if (holiday != null) {
            throw ApiException.notFound("NOT_A_WORKING_DAY",
                    date + " is " + holiday + " for this school, so nobody was absent.");
        }

        //! Step 3 - Every absence marked that day.
        // TODO: read student attendance records (the absences of one date)
        List<StudentAttendanceRecord> absences = marks.findAbsencesOn(school.getId(),
                year.getName(), date, guardianNotified);

        //! Step 4 - Their registers, to keep DAILY ones that are not cancelled.
        Map<String, AttendanceSession> sessionById = new LinkedHashMap<>();
        if (!absences.isEmpty()) {
            // TODO: read attendance sessions (the registers behind these absences)
            for (AttendanceSession one : sessions.findBySchoolIdAndIdIn(school.getId(),
                    absences.stream().map(StudentAttendanceRecord::getAttendanceSessionDocsId)
                            .distinct().toList())) {
                sessionById.put(one.getId(), one);
            }
        }
        List<StudentAttendanceRecord> kept = absences.stream()
                .filter(one -> {
                    AttendanceSession session = sessionById.get(one.getAttendanceSessionDocsId());
                    return session != null
                            && session.getSessionType() == AttendanceSessionType.DAILY
                            && session.getStatus() != AttendanceSessionStatus.CANCELLED;
                })
                .toList();

        //! Step 5 - The children and the classes, one read each.
        Map<String, Student> children = new LinkedHashMap<>();
        Map<String, String> classNames = new LinkedHashMap<>();
        if (!kept.isEmpty()) {
            // TODO: read students (the absent children)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(),
                    kept.stream().map(StudentAttendanceRecord::getStudentDocsId).distinct().toList())) {
                children.put(one.getId(), one);
            }
            // TODO: read school classes (the classes of the absent children)
            for (SchoolClass one : schoolClasses.findBySchoolIdAndIdIn(school.getId(),
                    kept.stream().map(one -> sessionById.get(one.getAttendanceSessionDocsId())
                            .getClassDocsId()).distinct().toList())) {
                classNames.put(one.getId(), one.getName());
            }
        }

        //! Step 6 - One row per absent child, and the totals.
        List<AbsenteesResponse.Row> rows = new ArrayList<>();
        int authorised = 0;
        int notNotified = 0;
        for (StudentAttendanceRecord mark : kept) {
            AttendanceSession session = sessionById.get(mark.getAttendanceSessionDocsId());
            Student child = children.get(mark.getStudentDocsId());
            boolean notified = Boolean.TRUE.equals(mark.getGuardianNotified());
            if (mark.getStatus() == AttendanceStatus.AUTHORISED_ABSENCE) {
                authorised++;
            }
            if (!notified) {
                notNotified++;
            }
            rows.add(new AbsenteesResponse.Row(
                    mark.getStudentDocsId(),
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    session.getClassDocsId(),
                    classNames.get(session.getClassDocsId()),
                    session.getSectionNo(),
                    mark.getStatus(),
                    mark.getReason(),
                    notified,
                    mark.getGuardianAcknowledgedAt(),
                    session.getId(),
                    session.getStatus()));
        }
        rows.sort(Comparator
                .comparing(AbsenteesResponse.Row::className, Comparator.nullsLast(String::compareTo))
                .thenComparing(AbsenteesResponse.Row::sectionNo, Comparator.nullsLast(String::compareTo))
                .thenComparing(AbsenteesResponse.Row::fullName, Comparator.nullsLast(String::compareTo)));

        log.info("[getAbsentees] Step 2: {} absent on {}, {} guardian(s) not told", rows.size(),
                date, notNotified);

        return new AbsenteesResponse(year.getName(), date, guardianNotified, rows.size(),
                authorised, rows.size() - authorised, notNotified, rows);
    }
}
