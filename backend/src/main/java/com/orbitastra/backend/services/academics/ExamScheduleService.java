package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.academics.examschedule.request.ExamScheduleCreateRequest;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamPapersAddedResponse;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamScheduleResponse;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.facilities.FacilityResource;
import com.orbitastra.backend.models.facilities.enums.FacilityResourceStatus;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.facilities.facilityresource.FacilityResourceRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamScheduleServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The papers of an exam — the datesheet. One paper is one subject (and component) for one section
 * on one date and time. Endpoint #8 of the plan in {@code controllers/academics/examination}; the
 * rest are not built.
 *
 * <p>The year, the class and the holiday check are the timetable's own utils, the same as
 * attendance and exams use, because they answer the same questions about the same year.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final CurrentSchoolResolver currentSchool;
    private final ExamRepository exams;
    private final ExamScheduleRepository examSchedules;
    private final FacilityResourceRepository facilityResources;
    private final StaffRepository staff;
    private final DailyTimetableServiceUtils utils;

    /**
     * Endpoint #8 — <b>add a paper</b>: one subject of one class, for one or more sections at the
     * same date and time. One paper row is saved for each section, all in one transaction.
     *
     * <p>Everything is checked before anything is saved: the exam can still change, the date is
     * inside the exam and a working day, every section exists and is taught the subject, none of
     * them has this paper already, and the room and the invigilators are free at that time.
     * Several sections of one paper may share a room — that is the same sitting.
     *
     * <p>A paper starts as {@code DRAFT}, or as {@code PUBLISHED} when the exam's datesheet has
     * already gone out. Gates 1, 2 and 4 run in the controller.
     */
    @Transactional
    public ExamPapersAddedResponse addPapers(String academicYear, String examDocsId,
            ExamScheduleCreateRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String examId = examDocsId == null ? "" : examDocsId.trim();
        String classId = request.classDocsId().trim();
        String componentName = TextHelper.blankToNull(request.componentName()) == null ? null
                : request.componentName().trim();
        log.info("[addPapers] Step 1: Adding {} for {} section(s) to exam {} in school {}",
                request.subjectCode(), request.sectionNos().size(), examId, school.getId());

        //! Step 2 - The checks that need nothing from the database: times in order, passing marks
        //! not above the maximum, and each section sent once.
        if (!request.startTime().isBefore(request.endTime())) {
            throw ApiException.badRequest("INVALID_TIME_RANGE",
                    "startTime (" + request.startTime() + ") must be before endTime ("
                            + request.endTime() + ").");
        }
        if (request.passingMarks() != null
                && request.passingMarks().compareTo(request.maximumMarks()) > 0) {
            throw ApiException.badRequest("PASSING_MARKS_ABOVE_MAXIMUM",
                    "passingMarks (" + request.passingMarks() + ") is more than maximumMarks ("
                            + request.maximumMarks() + ").");
        }
        Set<String> seenSections = new LinkedHashSet<>();
        for (String sent : request.sectionNos()) {
            if (!seenSections.add(sent.trim().toUpperCase())) {
                throw ApiException.badRequest("DUPLICATE_SECTION_IN_REQUEST",
                        "Section '" + sent.trim() + "' is in sectionNos twice. Send each section once.");
            }
        }

        //! Step 3 - The year, then the exam inside it. A completed or cancelled exam takes no papers.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(examId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND",
                        "No exam with id '" + examId + "' in '" + year.getName() + "'."));

        if (exam.getStatus() == ExamStatus.COMPLETED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw ApiException.conflict("EXAM_NOT_EDITABLE",
                    "This exam is " + exam.getStatus() + ", so no paper can be added to it.");
        }

        //! Step 4 - The date must be inside the exam's dates, and a working day.
        if (request.examDate().isBefore(exam.getStartDate())
                || request.examDate().isAfter(exam.getEndDate())) {
            throw ApiException.conflict("PAPER_OUTSIDE_EXAM",
                    "'" + exam.getName() + "' runs from " + Dates.readable(exam.getStartDate())
                            + " to " + Dates.readable(exam.getEndDate())
                            + ", so its papers have to fall between those dates.");
        }
        String holiday = utils.holidayNameFor(year, request.examDate());
        if (holiday != null) {
            throw ApiException.notFound("NOT_A_WORKING_DAY",
                    Dates.readable(request.examDate()) + " is " + holiday
                            + " for this school, so no paper can be held that day.");
        }

        //! Step 5 - The class, then each section in it — spelled the class's own way, and in use.
        SchoolClass schoolClass = utils.loadClassForYear(school, year.getName(), classId);
        List<ClassSection> classSections = schoolClass.getSections() == null ? List.of()
                : schoolClass.getSections();

        List<String> sectionNos = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String sent : request.sectionNos()) {
            ClassSection section = classSections.stream()
                    .filter(one -> one.getSectionNo().equalsIgnoreCase(sent.trim()))
                    .findFirst()
                    .orElse(null);
            if (section == null) {
                missing.add(sent.trim());
                continue;
            }
            if (Boolean.FALSE.equals(section.getActive())) {
                throw ApiException.conflict("SECTION_NOT_ACTIVE",
                        "Section '" + section.getSectionNo() + "' of '" + schoolClass.getName()
                                + "' is not active.");
            }
            sectionNos.add(section.getSectionNo());
        }
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("SECTION_NOT_IN_CLASS",
                    "'" + schoolClass.getName() + "' has no section " + String.join(", ", missing)
                            + ".");
        }

        //! Step 6 - The subject must be taught in every section: an active subject of the whole
        //! class, or of that one section.
        List<ClassSubject> classSubjects = schoolClass.getSubjects() == null ? List.of()
                : schoolClass.getSubjects();
        String subjectCode = request.subjectCode().trim();
        String subjectName = null;
        List<String> notTaught = new ArrayList<>();
        for (String sectionNo : sectionNos) {
            ClassSubject taught = classSubjects.stream()
                    .filter(one -> one.getSubjectCode().equalsIgnoreCase(subjectCode))
                    .filter(one -> one.getSectionNo() == null
                            || one.getSectionNo().equalsIgnoreCase(sectionNo))
                    .filter(one -> !Boolean.FALSE.equals(one.getActive()))
                    .findFirst()
                    .orElse(null);
            if (taught == null) {
                notTaught.add(sectionNo);
            } else {
                subjectName = taught.getName();
            }
        }
        if (!notTaught.isEmpty()) {
            throw ApiException.badRequest("SUBJECT_NOT_TAUGHT_IN_SECTION",
                    "'" + subjectCode + "' is not taught in " + schoolClass.getName() + " section "
                            + String.join(", ", notTaught) + ".");
        }
        //! Store the subject code the class's own way, so every paper of it matches.
        String storedSubjectCode = classSubjects.stream()
                .map(ClassSubject::getSubjectCode)
                .filter(code -> code.equalsIgnoreCase(subjectCode))
                .findFirst()
                .orElse(subjectCode);

        //! Step 7 - No section may have this paper in this exam already — the same subject and the
        //! same part, ignoring case. A subject with no part named is one whole paper.
        List<String> alreadyScheduled = new ArrayList<>();
        for (String sectionNo : sectionNos) {
            // TODO: check exam schedule exists (this paper for this section)
            boolean exists = componentName == null
                    ? examSchedules.existsBySchoolIdAndExamDocsIdAndClassDocsIdAndSectionNoAndSubjectCodeAndComponentNameIsNull(
                            school.getId(), examId, schoolClass.getId(), sectionNo, storedSubjectCode)
                    : examSchedules.existsBySchoolIdAndExamDocsIdAndClassDocsIdAndSectionNoAndSubjectCodeAndComponentNameIgnoreCase(
                            school.getId(), examId, schoolClass.getId(), sectionNo, storedSubjectCode,
                            componentName);
            if (exists) {
                alreadyScheduled.add(sectionNo);
            }
        }
        if (!alreadyScheduled.isEmpty()) {
            throw ApiException.conflict("PAPER_ALREADY_SCHEDULED",
                    ExamScheduleServiceUtils.paperLabel(storedSubjectCode, componentName)
                            + " is already in this exam for " + schoolClass.getName() + " section "
                            + String.join(", ", alreadyScheduled) + ".");
        }

        //! Step 8 - The room, when sent: this school's, in use, and not holding another sitting then.
        String roomId = TextHelper.blankToNull(request.facilityResourceDocsId());
        String roomName = null;
        if (roomId != null) {
            // TODO: read facility resource
            FacilityResource room = facilityResources.findByIdAndSchoolId(roomId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND",
                            "No room with id '" + roomId + "' in this school."));
            if (room.getStatus() != null && room.getStatus() != FacilityResourceStatus.IN_USE) {
                throw ApiException.conflict("ROOM_NOT_IN_USE",
                        "Room '" + room.getName() + "' is " + room.getStatus() + ".");
            }
            roomName = room.getName();

            // TODO: read exam schedules (papers in this room that day)
            for (ExamSchedule other : examSchedules
                    .findBySchoolIdAndFacilityResourceDocsIdAndExamDateAndStatusNot(school.getId(),
                            roomId, request.examDate(), ExamScheduleStatus.CANCELLED)) {
                if (ExamScheduleServiceUtils.overlaps(other, request.startTime(), request.endTime())
                        && !ExamScheduleServiceUtils.sameSitting(other, examId, storedSubjectCode,
                                componentName, request.examDate(), request.startTime(),
                                request.endTime())) {
                    throw ApiException.conflict("ROOM_CLASH",
                            "Room '" + room.getName() + "' already holds "
                                    + ExamScheduleServiceUtils.paperLabel(other.getSubjectCode(),
                                            other.getComponentName())
                                    + " for section " + other.getSectionNo() + " from "
                                    + other.getStartTime()
                                    + " to " + other.getEndTime() + " that day.");
                }
            }
        }

        //! Step 9 - The invigilators, when sent: each this school's staff, sent once, and not on
        //! duty at another sitting then.
        List<String> invigilatorIds = new ArrayList<>();
        if (request.invigilatorDocsIds() != null) {
            for (String id : request.invigilatorDocsIds()) {
                if (!invigilatorIds.contains(id.trim())) {
                    invigilatorIds.add(id.trim());
                }
            }
        }
        Map<String, String> invigilatorNames = new LinkedHashMap<>();
        if (!invigilatorIds.isEmpty()) {
            // TODO: read staff (the invigilators)
            for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), invigilatorIds)) {
                invigilatorNames.put(person.getId(), person.getFullName());
            }
            List<String> unknown = invigilatorIds.stream()
                    .filter(id -> !invigilatorNames.containsKey(id))
                    .toList();
            if (!unknown.isEmpty()) {
                throw ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id " + String.join(", ", unknown) + " in this school.");
            }

            // TODO: read exam schedules (papers these staff are on that day)
            for (ExamSchedule other : examSchedules
                    .findBySchoolIdAndInvigilatorDocsIdsInAndExamDateAndStatusNot(school.getId(),
                            invigilatorIds, request.examDate(), ExamScheduleStatus.CANCELLED)) {
                if (ExamScheduleServiceUtils.overlaps(other, request.startTime(), request.endTime())
                        && !ExamScheduleServiceUtils.sameSitting(other, examId, storedSubjectCode,
                                componentName, request.examDate(), request.startTime(),
                                request.endTime())) {
                    String busy = other.getInvigilatorDocsIds().stream()
                            .filter(invigilatorIds::contains)
                            .findFirst()
                            .orElse("");
                    throw ApiException.conflict("INVIGILATOR_CLASH",
                            invigilatorNames.getOrDefault(busy, busy) + " is already on duty at "
                                    + ExamScheduleServiceUtils.paperLabel(other.getSubjectCode(),
                                            other.getComponentName())
                                    + " for section " + other.getSectionNo() + " from "
                                    + other.getStartTime() + " to " + other.getEndTime()
                                    + " that day.");
                }
            }
        }

        //! Step 10 - Build one paper per section. A paper added after the datesheet went out is
        //! published straight away.
        ExamScheduleStatus status = exam.getStatus() == ExamStatus.PUBLISHED
                || exam.getStatus() == ExamStatus.IN_PROGRESS
                ? ExamScheduleStatus.PUBLISHED : ExamScheduleStatus.DRAFT;
        List<ExamSchedule> toSave = new ArrayList<>();
        for (String sectionNo : sectionNos) {
            ExamSchedule paper = ExamSchedule.builder()
                    .schoolId(school.getId())
                    .academicYear(year.getName())
                    .examDocsId(examId)
                    .classDocsId(schoolClass.getId())
                    .sectionNo(sectionNo)
                    .subjectCode(storedSubjectCode)
                    .componentName(componentName)
                    .examDate(request.examDate())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .maximumMarks(request.maximumMarks())
                    .passingMarks(request.passingMarks())
                    .facilityResourceDocsId(roomId)
                    .invigilatorDocsIds(new ArrayList<>(invigilatorIds))
                    .status(status)
                    .build();
            toSave.add(paper);
        }

        //! Step 11 - Save them all, in one transaction. The unique index on exam, class, section,
        //! subject and component stops two requests adding the same paper twice.
        // TODO: insert exam schedules
        List<ExamSchedule> saved = examSchedules.saveAll(toSave);
        log.info("[addPapers] Step 2: Saved {} paper(s) of {} to exam '{}'", saved.size(),
                storedSubjectCode, exam.getName());

        List<ExamScheduleResponse.Invigilator> invigilators = invigilatorIds.stream()
                .map(id -> new ExamScheduleResponse.Invigilator(id, invigilatorNames.get(id)))
                .toList();
        String finalSubjectName = subjectName;
        String finalRoomName = roomName;
        List<ExamScheduleResponse> papers = saved.stream()
                .map(one -> ExamScheduleResponse.of(one, schoolClass.getName(), finalSubjectName,
                        finalRoomName, invigilators))
                .toList();

        return new ExamPapersAddedResponse(exam.getId(), exam.getName(), papers.size(), papers,
                roomId == null
                        ? "No room yet — give each paper a room before the datesheet goes out."
                        : "Add the exam's other papers the same way.");
    }
}
