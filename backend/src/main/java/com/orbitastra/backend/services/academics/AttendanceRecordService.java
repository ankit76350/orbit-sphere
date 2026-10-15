package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.AcademicYearWindow;
import com.orbitastra.backend.common.time.AcademicYearWindow.Bound;
import com.orbitastra.backend.common.time.AcademicYearWindow.Refusal;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceDayResponse;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.attendance.AttendanceSessionRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepository;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepositoryCustom.SessionStatusCount;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepositoryCustom.SectionCount;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attendance records — the reads over registers and marks. Endpoint #8 of the plan in
 * {@code controllers/academics/attendance}; #9 to #11 are not built.
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
    private final DailyTimetableServiceUtils utils;
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
}
