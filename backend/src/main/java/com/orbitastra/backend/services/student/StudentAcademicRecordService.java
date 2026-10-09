package com.orbitastra.backend.services.student;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordCreateRequest;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;
import com.orbitastra.backend.models.student.enums.StudentStatus;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Where a child sits: the class and section they hold for one academic year.
 *
 * <p>Endpoint #14 of the plan in {@code controllers/student}. #15 to #22 — the reads, the close,
 * the move, the roster — are not built.
 *
 * <p><b>This is the one everything else waits for.</b> Attendance is taken against a section, a
 * mark sheet lists one, and a timetable is drawn for one. None of them can exist until a child is
 * in a section, and until now nothing put them there — which is why every student on the roll
 * reads back as {@code placed: false}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAcademicRecordService {

    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    /**
     * The statuses a child cannot be placed from.
     *
     * <p><b>They have left.</b> A placement says where somebody sits now, so writing one for a
     * child who withdrew in March is describing a desk nobody is at.
     *
     * <p><b>{@code INACTIVE} and {@code SUSPENDED} are deliberately not here.</b> Both are states
     * a child comes back from — a suspension ends, and a school that marks a child inactive over
     * a long absence still needs them on a roster when they return. Refusing those would mean
     * re-admitting a child to put them back in their own class.
     */
    private static final Set<StudentStatus> CANNOT_BE_PLACED = EnumSet.of(StudentStatus.WITHDRAWN, StudentStatus.TRANSFERRED, StudentStatus.GRADUATED);

    private final StudentAcademicRecordRepository academicRecords;
    private final StudentRepository students;
    private final SchoolClassRepository schoolClasses;
    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;

    /**
     * Endpoint #14 — <b>put a child in a class and a section</b>.
     *
     * <h2>Two documents, one transaction</h2>
     *
     * <p>The record is inserted and then {@code Student.currentAcademicRecordDocsId} is pointed at
     * it. <b>Either both happen or neither does.</b> A record with no pointer is a child who is
     * placed but reads back as unplaced everywhere; a pointer with no record is worse — it names a
     * document that is not there.
     *
     * <h2>The uniqueness is the database's, and the check here is for the message</h2>
     *
     * <p>{@code school_year_student_active_academic_record_uniq} is unique on
     * {@code {schoolId, academicYear, studentDocsId, status}}, partial on {@code ACTIVE}. Two
     * callers racing both read nothing and both insert; the second gets a duplicate-key error
     * whatever this method does.
     *
     * <p>So the read before it is <b>not</b> the enforcement. It is there so the ordinary case —
     * somebody placing a child who is already placed — answers {@code 409} naming the class they
     * are already in, instead of a 500 from a Mongo error.
     *
     * <h2>No gate 4, and it is load-bearing</h2>
     *
     * <p>Every write in {@code academics} refuses a year that is not running. <b>This one must
     * not</b>, because the handover it exists for runs the other way: a child admitted in January
     * is placed into a year that starts in June. Refusing until June would mean no class list
     * could be built before term began.
     *
     * <p>The cost is written down in the plan's open item 3: nothing stops a record being created
     * for a year that finished three years ago. The year has to exist and the class has to belong
     * to it; "this year is over" is not asked.
     */
    @Transactional
    public StudentAcademicRecordResponse assignStudentToClass(String academicYear,
            StudentAcademicRecordCreateRequest request) {

        //! step 1 - who is asking. requireUsable, not require: this is a write.
        School school = currentSchool.requireUsable();
        String year = academicYear == null ? "" : academicYear.trim();
        log.info("[placeStudent] Step 1: Placing student {} in year {} of school {}", request.studentDocsId(), year, school.getId());

        //! step 2 - the year in the path has to be one this school actually has. The NAME is the
        //! key here, not an id — AcademicYear.name is what every other collection references.
        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND", "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the child, scoped by school IN THE QUERY and never checked after. An id from
        //! another school is a real id, and placing it would write a record about somebody else's
        //! student. A 404 rather than a 403, which would confirm they exist.
        String childId = request.studentDocsId().trim();
        // TODO: read student
        Student child = students.findByIdAndSchoolId(childId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + childId + "' in this school."));

        //! step 4 - a child who has left cannot be given a desk. INACTIVE and SUSPENDED are not
        //! refused: both are states a child comes back from, and refusing them would mean
        //! re-admitting a child to put them back in their own class.
        if (CANNOT_BE_PLACED.contains(child.getStatus())) {
            throw ApiException.conflict("STUDENT_NOT_PLACEABLE",
                    "'" + child.getFullName() + "' is " + child.getStatus()
                            + " and cannot be placed in a class.");
        }

        //! Step 5 - Find the class and verify that it belongs to the selected academic year.
        String classId = request.classDocsId().trim();
        // TODO: read school class
        List<SchoolClass> matches = schoolClasses.findBySchoolIdAndIdIn(school.getId(), List.of(classId));

        if (matches.isEmpty()) {
            throw ApiException.notFound("CLASS_NOT_FOUND", "No class with id '" + classId + "' in this school.");
        }

        SchoolClass schoolClass = matches.get(0);

        if (!year.equals(schoolClass.getAcademicYear())) {
            throw ApiException.badRequest("CLASS_NOT_IN_YEAR",
                    "Class '" + schoolClass.getName() + "' belongs to '"
                            + schoolClass.getAcademicYear() + "', not '" + year + "'.");
        }

        //! Step 6 - Find the requested section within the selected class.
        String sectionNo = request.sectionNo().trim();
        ClassSection section = (schoolClass.getSections() == null ? List.<ClassSection>of()
                : schoolClass.getSections()).stream()
                .filter(one -> sectionNo.equals(one.getSectionNo()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("SECTION_NOT_IN_CLASS",
                        "Class '" + schoolClass.getName() + "' has no section '" + sectionNo
                                + "'."));

        //! Step 7 - Check whether the section is active.
        if (Boolean.FALSE.equals(section.getActive())) {
            throw ApiException.conflict("SECTION_NOT_ACTIVE",
                    "Section '" + sectionNo + "' of '" + schoolClass.getName()
                            + "' is not active.");
        }

        //! Step 8 - Check whether the student is already assigned to a class for this academic year.
        // TODO: read student academic record (is this child already placed this year)
        academicRecords.findBySchoolIdAndAcademicYearAndStudentDocsIdAndStatus(
                school.getId(), year, child.getId(), AcademicRecordStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw ApiException.conflict("STUDENT_ALREADY_PLACED",
                            "'" + child.getFullName() + "' already has an active record for '"
                                    + year + "'. Moving them is #17, which closes this one and "
                                    + "opens another.");
                });

        //! Step 9 - Validate the roll number and check whether it is already assigned.
        String rollNo = request.rollNo() == null || request.rollNo().isBlank() ? null : request.rollNo().trim();

        if (rollNo != null) {
                //! Check whether the roll number is already assigned to another student.
                // TODO: read student academic record (who holds this roll number in that section)
                StudentAcademicRecord holder = academicRecords
                        .findBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
                                school.getId(),
                                year,
                                classId,
                                sectionNo,
                                rollNo,
                                AcademicRecordStatus.ACTIVE)
                        .orElse(null);

                if (holder != null) {
                        //! Find the name of the student who holds this roll number.
                        // TODO: read student (who holds this roll number)
                        String heldBy = students
                                .findByIdAndSchoolId(holder.getStudentDocsId(), school.getId())
                                .map(Student::getFullName)
                                .orElse(null);

                        //! Return an error if the roll number is already assigned.
                        throw ApiException.conflict(
                                "ROLL_NUMBER_TAKEN",
                                "Roll number '" + rollNo + "' is already assigned in section '"
                                        + sectionNo + "' of class '" + schoolClass.getName()
                                        + "' for academic year '" + year + "'"
                                        + (heldBy == null ? "." : " by '" + heldBy + "'."));
                }
        }

        //! Step 10 - Create the student's academic record with the class, section, and academic year details.
        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .schoolId(school.getId())
                .academicYear(year)
                .studentDocsId(child.getId())
                .classDocsId(classId)
                .sectionNo(sectionNo)
                .rollNo(rollNo)
                .effectiveFrom(request.effectiveFrom() == null
                        ? LocalDate.now()
                        : request.effectiveFrom())
                .status(AcademicRecordStatus.ACTIVE)
                .build();

        // TODO: insert student academic record
        StudentAcademicRecord saved = academicRecords.save(record);
        log.info("[placeStudent] Step 2: Wrote academic record {}", saved.getId());

        //! Step 11 - Update the student's current academic record reference.
        child.setCurrentAcademicRecordDocsId(saved.getId());

        // TODO: update student (point them at their current record)
        students.save(child);
        log.info("[placeStudent] Step 3: '{}' now points at record {}",
                child.getFullName(), saved.getId());

        return StudentAcademicRecordResponse.of(saved, child, schoolClass.getName(),
                "'" + child.getFullName() + "' is in " + schoolClass.getName() + " "
                        + sectionNo + " for " + year
                        + (rollNo == null
                                ? ", with no roll number — none was sent, and #14 does not "
                                        + "generate one yet."
                                : ", roll number " + rollNo + ".")
                        + " Moving them is #17 and closing the record is #16; neither is built. "
                        + NO_AUTHORIZATION_YET);
    }
}
