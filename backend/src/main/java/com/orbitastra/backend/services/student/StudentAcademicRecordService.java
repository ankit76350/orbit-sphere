package com.orbitastra.backend.services.student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordCreateRequest;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordTransferRequest;
import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordUpdateRequest;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordHistoryResponse;
import com.orbitastra.backend.dto.student.academicrecord.response.StudentAcademicRecordTransferResponse;
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
 * <p>Endpoints #14, #15, #17 and #20 of the plan in {@code controllers/student}. The close
 * (#16), the roster (#21) and the strength table (#22) are not built.
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
                                    + year + "'. Transferring them is #17, which closes this one and "
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
                        + " Transferring them is #17 and closing the record is #16; neither is built. "
                        + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #20 — <b>a child's whole history, newest year first</b>.
     *
     * <p><b>Terminal records included</b>, which is the point. The question is where this child
     * has been; a closed record is most of the answer. Only a read asking "where are they now"
     * filters on {@code ACTIVE}, and this is not one.
     *
     * <h2>The sort is the index order, deliberately</h2>
     *
     * <p>{@code school_student_academic_record_history_idx} is
     * {@code {schoolId, studentDocsId, academicYear: -1, effectiveFrom: -1}}, and asking for
     * exactly that order lets Mongo walk the index and skip the sort stage. Reversing either key
     * would turn this into a scan with an in-memory sort for no visible difference until a school
     * has eight years of history.
     *
     * <h2>Two reads, never one per row</h2>
     *
     * <p>A record carries {@code classDocsId} and nothing readable. Resolving a name per row is
     * the N+1 the plan names for #21 — the distinct ids go into <b>one</b> query instead, so a
     * child with eight years costs two reads and not nine.
     *
     * <h2>An empty list is a real answer</h2>
     *
     * <p>A child admitted in January and not yet placed has no records. That is the state the roll
     * shows as {@code placed: false}, not a 404 — the <i>child</i> is what has to exist here, and
     * that is checked.
     *
     * <p><b>No gates.</b> A read, and a suspended school still has to answer where its children
     * sat.
     */
    public StudentAcademicRecordHistoryResponse getStudentAcademicRecords(String studentDocsId,
            String academicYear) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String childId = studentDocsId == null ? "" : studentDocsId.trim();
        String year = academicYear == null || academicYear.isBlank()
                ? null
                : academicYear.trim();
        log.info("[historyOf] Step 1: Reading the records of student {} in school {}",
                childId, school.getId());

        //! Step 2 - Find the student in the current school.
        // TODO: read student
        Student child = students.findByIdAndSchoolId(childId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + childId + "' in this school."));

        //! Step 3 - Fetch the student's academic records.
        // TODO: read student academic records (this child's history)
        List<StudentAcademicRecord> rows = year == null
                ? academicRecords
                        .findBySchoolIdAndStudentDocsIdOrderByAcademicYearDescEffectiveFromDescIdDesc(
                                school.getId(), child.getId())
                : academicRecords
                        .findBySchoolIdAndStudentDocsIdAndAcademicYearOrderByEffectiveFromDescIdDesc(
                                school.getId(), child.getId(), year);

        //! Step 4 - Fetch the class names for the academic records.
        Map<String, String> classNames = new LinkedHashMap<>();
        List<String> classIds = new ArrayList<>();

        for (StudentAcademicRecord one : rows) {
            if (one.getClassDocsId() != null && !classNames.containsKey(one.getClassDocsId())) {
                classNames.put(one.getClassDocsId(), null);
                classIds.add(one.getClassDocsId());
            }
        }

        if (!classIds.isEmpty()) {
            // TODO: read school classes (the names behind this history's class ids)
            for (SchoolClass one : schoolClasses.findBySchoolIdAndIdIn(school.getId(), classIds)) {
                classNames.put(one.getId(), one.getName());
            }
        }

        //! Step 5 - Build and return the academic records response.
        String pointer = child.getCurrentAcademicRecordDocsId();
        List<StudentAcademicRecordHistoryResponse.Row> history = new ArrayList<>();

        for (StudentAcademicRecord one : rows) {
            history.add(new StudentAcademicRecordHistoryResponse.Row(
                    one.getId(),
                    one.getAcademicYear(),
                    one.getClassDocsId(),
                    classNames.get(one.getClassDocsId()),
                    one.getSectionNo(),
                    one.getRollNo(),
                    one.getEffectiveFrom(),
                    one.getEffectiveUntil(),
                    one.getStatus(),
                    one.getPreviousAcademicRecordDocsId(),
                    one.getId() != null && one.getId().equals(pointer),
                    one.getVersion()));
        }

        log.info("[historyOf] Step 2: '{}' has {} record(s)", child.getFullName(), history.size());

        return new StudentAcademicRecordHistoryResponse(
                child.getId(),
                child.getFullName(),
                child.getAdmissionNo(),
                year,
                history.size(),
                history);
    }

    /**
     * Endpoint #17 — <b>transfer a child to another class or section</b>.
     *
     * <p>In one transaction: <b>close the open record as {@code TRANSFERRED}</b>, insert a new
     * {@code ACTIVE} one pointing back through {@code previousAcademicRecordDocsId}, and repoint
     * the child.
     *
     * <h2>Why this is not a {@code PATCH} of the class</h2>
     *
     * <p>The mechanical reason is the index: two {@code ACTIVE} records for one child in one year
     * are forbidden, so the close and the open cannot be two requests — between them the child is
     * either in two places or in none.
     *
     * <p>The reason that matters is that <b>editing the class in place erases where the child sat
     * for the first half of the year</b>, which is exactly what that half's attendance and marks
     * are attached to. A transfer is a new fact. Correcting a placement that was simply typed wrongly
     * is #15.
     *
     * <h2>{@code TRANSFERRED}, not {@code COMPLETED}</h2>
     *
     * <p>The plan says close it as {@code COMPLETED}. <b>The enum disagrees</b>, and it is right:
     * {@code TRANSFERRED} is documented on {@code AcademicRecordStatus} as <i>"placement ended
     * because the student changed class or section"</i>, which is this and only this.
     * {@code COMPLETED} is what #16 writes when a year ends normally — a record closed by a
     * February transfer is not a year anybody completed.
     *
     * <h2>The order of the writes is load-bearing</h2>
     *
     * <p>The old record is <b>saved closed before the new one is checked or inserted</b>. Two
     * things depend on it: the unique index stops seeing it as {@code ACTIVE}, so the insert does
     * not collide — and a child keeping their roll number while changing section is not refused by
     * their own old record still holding it.
     *
     * <h2>No open record is not a refusal</h2>
     *
     * <p>There is nothing to close, so the transfer is a first placement and the response says which
     * happened. A caller asking to transfer a child who was never placed means to put them somewhere,
     * and refusing would leave them to work out that they wanted #14 instead.
     */
    @Transactional
    public StudentAcademicRecordTransferResponse transferStudent(String academicYear, StudentAcademicRecordTransferRequest request) {

        //! Step 1 - Get the current school and validate the academic year and student IDs.
        School school = currentSchool.requireUsable();
        String year = academicYear == null ? "" : academicYear.trim();
        String childId = request.studentDocsId().trim();
        log.info("[transferStudent] Step 1: Transferring student {} within year {} of school {}", childId, year, school.getId());

        //! Step 2 - Check whether the academic year exists in the current school.
        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! Step 3 - Find the student and verify that they belong to the current school.
        // TODO: read student
        Student child = students.findByIdAndSchoolId(childId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + childId + "' in this school."));

        //! Step 4 - Check whether the student is eligible for class transfer.
        if (CANNOT_BE_PLACED.contains(child.getStatus())) {
            throw ApiException.conflict("STUDENT_NOT_PLACEABLE",
                    "'" + child.getFullName() + "' is " + child.getStatus()
                            + " and cannot be transferred.");
        }

        //! Step 5 - Check whether the student record has been modified by someone else.
        if (!request.version().equals(child.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + child.getFullName() + "' was changed by someone else. Please read the "
                            + "student again before transferring them.");
        }

        //! Step 6 - Find the student's current academic record for the selected year.
        // TODO: read student academic record (what is this child holding now)
        StudentAcademicRecord open = academicRecords
                .findBySchoolIdAndAcademicYearAndStudentDocsIdAndStatus(
                        school.getId(), year, child.getId(), AcademicRecordStatus.ACTIVE)
                .orElse(null);

       //! Step 7 - Determine the class to which the student will be assigned.
        String classId = request.classDocsId() == null || request.classDocsId().isBlank() ? (open == null ? null : open.getClassDocsId()) : request.classDocsId().trim();

        if (classId == null) {
            throw ApiException.badRequest("CLASS_REQUIRED",
                    "'" + child.getFullName() + "' has no open record for '" + year
                            + "', so there is no current class to transfer within. Send classDocsId.");
        }

        //! Step 8 - Find the class and verify that it belongs to the selected academic year.
        // TODO: read school class
        List<SchoolClass> matches = schoolClasses.findBySchoolIdAndIdIn(
                school.getId(), List.of(classId));

        if (matches.isEmpty()) {
            throw ApiException.notFound("CLASS_NOT_FOUND",
                    "No class with id '" + classId + "' in this school.");
        }

        SchoolClass schoolClass = matches.get(0);

        if (!year.equals(schoolClass.getAcademicYear())) {
            throw ApiException.badRequest("CLASS_NOT_IN_YEAR",
                    "Class '" + schoolClass.getName() + "' belongs to '"
                            + schoolClass.getAcademicYear() + "', not '" + year + "'.");
        }

       //! Step 9 - Find the section and check whether it is active.
        String sectionNo = request.sectionNo().trim();
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

        //! Step 10 - Check whether the student is already in the selected class and section.
        if (open != null && classId.equals(open.getClassDocsId())
                && sectionNo.equals(open.getSectionNo())) {
            throw ApiException.conflict("ALREADY_IN_THAT_SECTION",
                    "'" + child.getFullName() + "' is already in " + schoolClass.getName() + " "
                            + sectionNo + " for '" + year + "'. Correcting a roll number or a "
                            + "date is #15.");
        }

        LocalDate from = request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom();

        //! Step 11 - Close the student's existing academic record, if available.
        StudentAcademicRecordTransferResponse.Side transferredFrom = null;

        if (open != null) {
            open.setStatus(AcademicRecordStatus.TRANSFERRED);
            open.setEffectiveUntil(from);

            // TODO: update student academic record (close the one being transferred out of)
            StudentAcademicRecord closed = academicRecords.save(open);
            log.info("[transferStudent] Step 2: Closed record {} as TRANSFERRED", closed.getId());

            //! THE OLD CLASS'S NAME. One read, and only when it is a different class from the one
            //! being transferred into — a section transfer inside one class already has it in hand.
            String fromClassName = classId.equals(closed.getClassDocsId())
                    ? schoolClass.getName()
                    : schoolClasses
                            .findBySchoolIdAndIdIn(school.getId(), List.of(closed.getClassDocsId()))
                            .stream()
                            .findFirst()
                            .map(SchoolClass::getName)
                            .orElse(null);

            transferredFrom = new StudentAcademicRecordTransferResponse.Side(
                    closed.getId(),
                    closed.getClassDocsId(),
                    fromClassName,
                    closed.getSectionNo(),
                    closed.getRollNo(),
                    closed.getEffectiveFrom(),
                    closed.getEffectiveUntil(),
                    closed.getStatus(),
                    closed.getPreviousAcademicRecordDocsId(),
                    closed.getVersion());
        }

        //! Step 12 - Check whether the roll number is available in the new section.
        String rollNo = request.rollNo() == null || request.rollNo().isBlank()
                ? null
                : request.rollNo().trim();

        if (rollNo != null) {
            // TODO: read student academic record (who holds this roll number in the new section)
            StudentAcademicRecord holder = academicRecords
                    .findBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
                            school.getId(), year, classId, sectionNo, rollNo,
                            AcademicRecordStatus.ACTIVE)
                    .orElse(null);

            if (holder != null) {
                // TODO: read student (who holds this roll number)
                String heldBy = students
                        .findByIdAndSchoolId(holder.getStudentDocsId(), school.getId())
                        .map(Student::getFullName)
                        .orElse(null);

                throw ApiException.conflict("ROLL_NUMBER_TAKEN",
                        "Roll number '" + rollNo + "' is already assigned in section '" + sectionNo
                                + "' of class '" + schoolClass.getName() + "' for academic year '"
                                + year + "'" + (heldBy == null ? "." : " by '" + heldBy + "'."));
            }
        }

        //! Step 13 - Create a new academic record linked to the previous record.
        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .schoolId(school.getId())
                .academicYear(year)
                .studentDocsId(child.getId())
                .classDocsId(classId)
                .sectionNo(sectionNo)
                .rollNo(rollNo)
                .effectiveFrom(from)
                .status(AcademicRecordStatus.ACTIVE)
                .previousAcademicRecordDocsId(open == null ? null : open.getId())
                .build();

        // TODO: insert student academic record
        StudentAcademicRecord saved = academicRecords.save(record);
        log.info("[transferStudent] Step 3: Opened record {}", saved.getId());

        //! Step 14 - Update the student's current academic record reference.
        child.setCurrentAcademicRecordDocsId(saved.getId());

        // TODO: update student (point them at their new record)
        students.save(child);

        return new StudentAcademicRecordTransferResponse(
                child.getId(),
                child.getFullName(),
                child.getAdmissionNo(),
                year,
                transferredFrom,
                new StudentAcademicRecordTransferResponse.Side(
                        saved.getId(),
                        saved.getClassDocsId(),
                        schoolClass.getName(),
                        saved.getSectionNo(),
                        saved.getRollNo(),
                        saved.getEffectiveFrom(),
                        saved.getEffectiveUntil(),
                        saved.getStatus(),
                        saved.getPreviousAcademicRecordDocsId(),
                        saved.getVersion()),
                transferredFrom != null,
                (transferredFrom == null
                        ? "'" + child.getFullName() + "' had no open record for '" + year
                                + "', so this was a first placement rather than a transfer."
                        : "'" + child.getFullName() + "' transferred from " + transferredFrom.className() + " "
                                + transferredFrom.sectionNo() + " to " + schoolClass.getName() + " "
                                + sectionNo + " on " + from + ". The old record is TRANSFERRED, "
                                + "not deleted — it is where that half of the year's attendance "
                                + "is attached.")
                        + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #15 — <b>correct the open record</b>.
     *
     * <p>The typo endpoint: a roll number entered wrongly, a start date a day out. <b>It changes
     * what the record always should have said</b>, which is why it writes no history and why it
     * is the only academic-record write that touches one document.
     *
     * <h2>What it deliberately cannot change</h2>
     *
     * <p><b>Not the class or the section</b> — that is #17, which closes this record and opens
     * another, because a child did not <i>turn out</i> to be in 7B all along. <b>Not the
     * status</b> — that is #16, which also clears the child's pointer. <b>Not
     * {@code effectiveUntil}</b>, which belongs to a closed record and this one is open by
     * definition.
     *
     * <h2>The open record, found rather than named</h2>
     *
     * <p>Addressed by the child and the year, the same as #17. The cost is that <b>a closed record
     * cannot be corrected at all</b>: a typo in last term's roll number is unreachable from here.
     *
     * <h2>The version is the RECORD'S</h2>
     *
     * <p>Unlike #17, which guards the child because it repoints them. This write touches the
     * record and nothing else, so that is what it guards.
     *
     * <p><b>No gate 4</b>, the same as the rest of this collection.
     */
    @Transactional
    public StudentAcademicRecordResponse updateCurrentRecord(String academicYear,
            StudentAcademicRecordUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, not require: this is a write.
        School school = currentSchool.requireUsable();
        String year = academicYear == null ? "" : academicYear.trim();
        String childId = request.studentDocsId().trim();
        log.info("[updateCurrentRecord] Step 1: Correcting the open record of student {} in year " + "{} of school {}", childId, year, school.getId());

        //! Step 2 - Reject the request if no field is provided for correction.

        if (request.sectionNo() == null && request.rollNo() == null
                && request.effectiveFrom() == null) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send sectionNo, rollNo or effectiveFrom. Changing the class is #17, and "
                            + "closing the record is #16.");
        }

        //! Step 3 - Check that the academic year exists in the school.
        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! Step 4 - Find the student belonging to the school.
        // TODO: read student
        Student child = students.findByIdAndSchoolId(childId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "No student with id '" + childId + "' in this school."));

        //! Step 5 - Check that the student has a current academic record and the provided record ID matches it.
        String recordId = request.academicRecordDocsId().trim();
        String currentId = child.getCurrentAcademicRecordDocsId();

        if (currentId == null) {
            throw ApiException.notFound("NO_ACTIVE_RECORD",
                    "'" + child.getFullName() + "' has no current academic record. Placing them "
                            + "is #14.");
        }

        if (!recordId.equals(currentId)) {
            throw ApiException.conflict("RECORD_NOT_CURRENT",
                    "This academic record cannot be edited: it is not the current record of '"
                            + child.getFullName() + "'. Only the current record can be corrected.");
        }

        //! Step 6 - Find the academic record and check that it belongs to the student, matches the academic year, and is ACTIVE.
        // TODO: read student academic record (the current one)
        StudentAcademicRecord record = academicRecords
                .findByIdAndSchoolIdAndStudentDocsId(recordId, school.getId(), child.getId())
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_RECORD_NOT_FOUND",
                        "'" + child.getFullName() + "' has no academic record with id '"
                                + recordId + "'."));

        if (!year.equals(record.getAcademicYear())) {
            throw ApiException.badRequest("RECORD_NOT_IN_YEAR",
                    "This academic record belongs to '" + record.getAcademicYear()
                            + "', not to '" + year + "'.");
        }

        if (record.getStatus() != AcademicRecordStatus.ACTIVE) {
            throw ApiException.conflict("RECORD_NOT_OPEN",
                    "This academic record cannot be edited: it is " + record.getStatus()
                            + ", not ACTIVE.");
        }

        //! Step 7 - Check the record version to prevent concurrent updates.
        if (!request.version().equals(record.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "The academic record of '" + child.getFullName() + "' was changed by someone "
                            + "else. Please read it again before correcting it.");
        }

        //! Step 8 - Find the class linked to the academic record.
        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findBySchoolIdAndIdIn(school.getId(), List.of(record.getClassDocsId()))
                .stream()
                .findFirst()
                .orElse(null);

        //! Step 9 - Validate the section and check that it is active.
        String section = record.getSectionNo();
        boolean movedSection = false;

        if (request.sectionNo() != null) {
            String wanted = request.sectionNo().trim();

            if (wanted.isEmpty()) {
                throw ApiException.badRequest("SECTION_REQUIRED",
                        "A record cannot be left without a section. Send one, or leave the field "
                                + "out to keep the current one.");
            }

            if (!wanted.equals(record.getSectionNo())) {
                if (schoolClass == null) {
                    throw ApiException.notFound("CLASS_NOT_FOUND",
                            "The class this record names no longer exists, so its sections cannot "
                                    + "be checked.");
                }

                ClassSection target = (schoolClass.getSections() == null
                        ? List.<ClassSection>of()
                        : schoolClass.getSections()).stream()
                        .filter(one -> wanted.equals(one.getSectionNo()))
                        .findFirst()
                        .orElseThrow(() -> ApiException.badRequest("SECTION_NOT_IN_CLASS",
                                "Class '" + schoolClass.getName() + "' has no section '" + wanted
                                        + "'."));

                if (Boolean.FALSE.equals(target.getActive())) {
                    throw ApiException.conflict("SECTION_NOT_ACTIVE",
                            "Section '" + wanted + "' of '" + schoolClass.getName()
                                    + "' is not active.");
                }

                section = wanted;
                movedSection = true;
            }
        }

        //! Step 10 - Update the roll number and check for duplicates in the target section.
        boolean movedRoll = false;

        if (request.rollNo() != null) {
            String wanted = request.rollNo().isBlank() ? null : request.rollNo().trim();

            if (!java.util.Objects.equals(wanted, record.getRollNo())) {
                //! THEIR OWN NUMBER DOES NOT BLOCK THEM — the holder is only a conflict when it
                //! is a different record. That matters when the correction is spacing or case.
                if (wanted != null) {
                    //! AGAINST THE SECTION IT WILL END UP IN, which is the corrected one when
                    //! both fields are sent together. Checking the old section would let a number
                    //! through that collides the moment the section lands.
                    // TODO: read student academic record (who holds this roll number)
                    StudentAcademicRecord holder = academicRecords
                            .findBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
                                    school.getId(), year, record.getClassDocsId(),
                                    section, wanted, AcademicRecordStatus.ACTIVE)
                            .orElse(null);

                    if (holder != null && !holder.getId().equals(record.getId())) {
                        // TODO: read student (who holds this roll number)
                        String heldBy = students
                                .findByIdAndSchoolId(holder.getStudentDocsId(), school.getId())
                                .map(Student::getFullName)
                                .orElse(null);

                        throw ApiException.conflict("ROLL_NUMBER_TAKEN",
                                "Roll number '" + wanted + "' is already assigned in section '"
                                        + section + "' for academic year '" + year
                                        + "'" + (heldBy == null ? "." : " by '" + heldBy + "'."));
                    }
                }

                record.setRollNo(wanted);
                movedRoll = true;
            }
        }

        //! Step 11 - Check the existing roll number if the section changes.
        if (movedSection && record.getRollNo() != null) {
            // TODO: read student academic record (is the kept roll number free in the new section)
            StudentAcademicRecord holder = academicRecords
                    .findBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
                            school.getId(), year, record.getClassDocsId(), section,
                            record.getRollNo(), AcademicRecordStatus.ACTIVE)
                    .orElse(null);

            if (holder != null && !holder.getId().equals(record.getId())) {
                // TODO: read student (who holds this roll number)
                String heldBy = students
                        .findByIdAndSchoolId(holder.getStudentDocsId(), school.getId())
                        .map(Student::getFullName)
                        .orElse(null);

                throw ApiException.conflict("ROLL_NUMBER_TAKEN",
                        "Roll number '" + record.getRollNo() + "' is already assigned in section '"
                                + section + "' for academic year '" + year + "'"
                                + (heldBy == null ? "." : " by '" + heldBy + "'."));
            }
        }

        record.setSectionNo(section);

        //! Step 12 - Update the start date if a new date is provided.
        boolean movedDate = false;

        if (request.effectiveFrom() != null
                && !request.effectiveFrom().equals(record.getEffectiveFrom())) {
            record.setEffectiveFrom(request.effectiveFrom());
            movedDate = true;
        }

        //! Step 13 - Reject the request if no values have actually changed.
        if (!movedSection && !movedRoll && !movedDate) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "The record already says that.");
        }

        // TODO: update student academic record
        StudentAcademicRecord saved = academicRecords.save(record);
        log.info("[updateCurrentRecord] Step 2: Corrected record {}", saved.getId());

        //! Step 14 - Save the corrected academic record.
        List<String> changed = new ArrayList<>();
        if (movedSection) {
            changed.add("the section");
        }
        if (movedRoll) {
            changed.add("the roll number");
        }
        if (movedDate) {
            changed.add("the start date");
        }

        //! Step 15 - Return the updated record with a message describing the changes.
        return StudentAcademicRecordResponse.of(saved, child,
                schoolClass == null ? null : schoolClass.getName(),
                "Corrected " + String.join(" and ", changed) + " on the open record of '"
                        + child.getFullName() + "'. THE CLASS IS UNCHANGED, and no history was "
                        + "written — if this child actually moved rather than was mistyped, that "
                        + "is #17. " + NO_AUTHORIZATION_YET);
    }
}
