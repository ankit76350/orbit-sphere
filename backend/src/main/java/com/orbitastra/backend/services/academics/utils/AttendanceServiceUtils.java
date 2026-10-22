package com.orbitastra.backend.services.academics.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.dto.academics.attendance.response.AttendanceRegisterResponse;
import com.orbitastra.backend.dto.academics.attendance.response.AttendanceSessionResponse;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;
import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.attendance.StudentAttendanceRecordRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * The reads both attendance services share.
 */
@Component
@RequiredArgsConstructor
public class AttendanceServiceUtils {

    public static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * <b>Roll numbers in the order a teacher reads them</b>: numbers by value — 7 before 41 —
     * then anything that is not a plain number, as text, then none at all.
     *
     * Used by:
     * - registerOf()
     * - AttendanceRecordService.getSectionAttendance()
     * - ExamCandidateService.listCandidates()
     * - ExamAttendanceServiceUtils.registerOf()
     * - StudentMarkService.getMarkSheet()
     */
    public static final Comparator<String> ROLL_ORDER = Comparator.nullsLast((a, b) -> {
        boolean aNumber = a.matches("\\d+");
        boolean bNumber = b.matches("\\d+");
        if (aNumber && bNumber) {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        }
        if (aNumber != bNumber) {
            return aNumber ? -1 : 1;
        }
        return a.compareToIgnoreCase(b);
    });

    private final StudentAcademicRecordRepository academicRecords;
    private final StudentAttendanceRecordRepository marks;
    private final StudentRepository students;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;

    /**
     * <b>One register as the answer shows it</b>: the header, and every child who was in the
     * section on its date with their mark or none yet. A child who has a mark but is no longer in
     * the section for that date is still listed, with {@code onRoster: false}, so no mark is ever
     * hidden.
     *
     * <p><b>Four reads</b> — the children's placements, the marks, the names, the class and
     * teacher — never one per child. The register itself is passed in, already loaded.
     *
     * Used by:
     * - AttendanceSessionService.getRegister()
     * - AttendanceSessionService.lockRegister()
     * - AttendanceSessionService.unlockRegister()
     * - AttendanceRecordService.markRegister()
     */
    public AttendanceRegisterResponse registerOf(School school, AttendanceSession session) {

        //! Step 1 - Who was in the section on that date.
        // TODO: read student academic records (who was in this section on this date)
        List<StudentAcademicRecord> placements = academicRecords.findInSectionOn(school.getId(),
                session.getAcademicYear(), session.getClassDocsId(), session.getSectionNo(),
                session.getAttendanceDate());

        //! Step 2 - The marks already in the register.
        // TODO: read student attendance records (the marks in this register)
        List<StudentAttendanceRecord> taken = marks.findBySchoolIdAndAttendanceSessionDocsId(
                school.getId(), session.getId());
        Map<String, StudentAttendanceRecord> markByStudent = new LinkedHashMap<>();
        for (StudentAttendanceRecord one : taken) {
            markByStudent.put(one.getStudentDocsId(), one);
        }

        //! Step 3 - The children's names, for everybody on the roster and everybody marked.
        Set<String> childIds = new HashSet<>(markByStudent.keySet());
        for (StudentAcademicRecord one : placements) {
            childIds.add(one.getStudentDocsId());
        }
        Map<String, Student> children = new LinkedHashMap<>();
        if (!childIds.isEmpty()) {
            // TODO: read students (the children in this register)
            for (Student one : students.findBySchoolIdAndIdIn(school.getId(), childIds)) {
                children.put(one.getId(), one);
            }
        }

        //! Step 4 - One row per child on the roster, then any marked child who is not on it. A
        //! stored mark is shown as status, reason, source and times; no mark is null.
        Function<StudentAttendanceRecord, AttendanceRegisterResponse.Mark> markOf =
                record -> record == null ? null : new AttendanceRegisterResponse.Mark(
                        record.getStatus(), record.getReason(), record.getSource(),
                        record.getArrivalAt(), record.getDepartureAt());

        List<AttendanceRegisterResponse.StudentRow> rows = new ArrayList<>();
        Set<String> onRoster = new HashSet<>();
        for (StudentAcademicRecord placement : placements) {
            //! A child on the day of a transfer can hold two placements that both cover it; one
            //! row per child.
            if (!onRoster.add(placement.getStudentDocsId())) {
                continue;
            }
            Student child = children.get(placement.getStudentDocsId());
            rows.add(new AttendanceRegisterResponse.StudentRow(
                    placement.getStudentDocsId(),
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    placement.getRollNo(),
                    placement.getId(),
                    true,
                    markOf.apply(markByStudent.get(placement.getStudentDocsId()))));
        }
        for (StudentAttendanceRecord one : taken) {
            if (onRoster.contains(one.getStudentDocsId())) {
                continue;
            }
            Student child = children.get(one.getStudentDocsId());
            rows.add(new AttendanceRegisterResponse.StudentRow(
                    one.getStudentDocsId(),
                    child == null ? null : child.getFullName(),
                    child == null ? null : child.getAdmissionNo(),
                    null,
                    one.getStudentAcademicRecordDocsId(),
                    false,
                    markOf.apply(one)));
        }
        rows.sort(Comparator
                .comparing(AttendanceRegisterResponse.StudentRow::rollNo, ROLL_ORDER)
                .thenComparing(AttendanceRegisterResponse.StudentRow::fullName,
                        Comparator.nullsLast(String::compareTo)));

        //! Step 5 - The counts.
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status.name(), 0L);
        }
        long marked = 0;
        for (AttendanceRegisterResponse.StudentRow row : rows) {
            if (row.mark() != null) {
                counts.merge(row.mark().status().name(), 1L, Long::sum);
                marked++;
            }
        }
        long unmarked = rows.size() - marked;

        //! Step 6 - The class and teacher names for the header.
        // TODO: read school class
        String className = schoolClasses
                .findBySchoolIdAndIdIn(school.getId(), List.of(session.getClassDocsId()))
                .stream().findFirst().map(SchoolClass::getName).orElse(null);
        String teacherName = null;
        if (session.getTeacherDocsId() != null) {
            // TODO: read staff
            teacherName = staff.findByIdAndSchoolId(session.getTeacherDocsId(), school.getId())
                    .map(Staff::getFullName).orElse(null);
        }

        return new AttendanceRegisterResponse(
                AttendanceSessionResponse.of(session, className, teacherName, false, null),
                rows.size(),
                marked,
                unmarked,
                counts,
                rows,
                (rows.isEmpty()
                        ? "Nobody was placed in this section on " + session.getAttendanceDate()
                                + ". Placing a child is student #14. "
                        : unmarked == 0
                                ? "Every child is marked. "
                                : unmarked + " of " + rows.size() + " not marked yet — mark them "
                                        + "with attendance #2. ")
                        + NO_AUTHORIZATION_YET);
    }
}
