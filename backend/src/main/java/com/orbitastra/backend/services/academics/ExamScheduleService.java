package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.Comparator;
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
import com.orbitastra.backend.dto.academics.examschedule.request.ExamDatesheetRequest;
import com.orbitastra.backend.dto.academics.examschedule.request.ExamScheduleCreateRequest;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamDatesheetResponse;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamScheduleDetailResponse;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamPapersAddedResponse;
import com.orbitastra.backend.dto.academics.examschedule.response.ExamScheduleResponse;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.examination.Exam;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.MarkEntryStatus;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.embedded.ExamPaperAttendance;
import com.orbitastra.backend.models.academics.examination.embedded.ExamRoomAllocation;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.room.Room;
import com.orbitastra.backend.models.room.enums.RoomStatus;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.exam.ExamRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.studentmark.StudentMarkRepository;
import com.orbitastra.backend.repositories.academics.examschedule.ExamScheduleRepository;
import com.orbitastra.backend.repositories.room.room.RoomRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamAttendanceServiceUtils;
import com.orbitastra.backend.services.academics.utils.ExamScheduleServiceUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.orbitastra.backend.dto.academics.examschedule.request.ExamScheduleCompleteRequest;
import com.orbitastra.backend.dto.academics.examattendance.response.ExamHallRegisterResponse;
import java.time.LocalDate;

/**
 * The papers of an exam — the datesheet. One paper is one subject (and component) for one section
 * on one date and time. Endpoints #8, #11, #12 and #16 of the plan in {@code controllers/academics/examination};
 * the rest are not built.
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
    private final SchoolClassRepository schoolClasses;
    private final StudentMarkRepository studentMarks;
    private final RoomRepository rooms;
    private final StaffRepository staff;
    private final DailyTimetableServiceUtils utils;
    private final ExamAttendanceServiceUtils hallUtils;

    /**
     * Endpoint #8 — <b>add a paper</b>: one subject of one class, for one or more sections at the
     * same date and time. One paper row is saved for each section, all in one transaction.
     *
     * <p>Everything is checked before anything is saved: the exam can still change, the date is
     * inside the exam and a working day, every section exists and is taught the subject, none of
     * them has this paper already or another paper at that time, and every room and invigilator is
     * free at that time.
     *
     * <p><b>A paper can use several rooms</b> — 200 in the hall, 50 in Room 12 — each with its
     * seat count — for all the sections sent together — and its own invigilators. <b>A room can hold
     * more than one exam at a time</b> — a
     * big hall with space left — as long as everyone in it then fits its capacity. A person is on
     * duty in one room at a time; the same room is the same place.
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

        //! Step 7b - A section sits one paper at a time: none of them may already have another paper,
        //! in any exam, at an overlapping time that day.
        // TODO: read exam schedules (these sections' papers that day)
        for (ExamSchedule other : examSchedules
                .findBySchoolIdAndClassDocsIdAndSectionNoInAndExamDateAndStatusNot(school.getId(),
                        schoolClass.getId(), sectionNos, request.examDate(),
                        ExamScheduleStatus.CANCELLED)) {
            if (ExamScheduleServiceUtils.overlaps(other, request.startTime(), request.endTime())) {
                throw ApiException.conflict("SECTION_CLASH",
                        schoolClass.getName() + " section " + other.getSectionNo()
                                + " already sits "
                                + ExamScheduleServiceUtils.paperLabel(other.getSubjectCode(),
                                        other.getComponentName())
                                + " from " + other.getStartTime() + " to " + other.getEndTime()
                                + " that day. A section can sit one paper at a time.");
            }
        }

        //! Step 8 - The rooms, when sent: each room once, each person on duty in one room only.
        List<ExamScheduleCreateRequest.Room> sentRooms = request.rooms() == null ? List.of()
                : request.rooms();
        List<String> roomIds = new ArrayList<>();
        Map<String, String> roomOfInvigilator = new LinkedHashMap<>();
        for (ExamScheduleCreateRequest.Room sent : sentRooms) {
            String roomId = sent.roomDocsId().trim();
            if (roomIds.contains(roomId)) {
                throw ApiException.badRequest("DUPLICATE_ROOM_IN_REQUEST",
                        "Room '" + roomId + "' is in rooms twice. Send each room once, with all its seats.");
            }
            roomIds.add(roomId);
            for (String id : sent.invigilatorDocsIds() == null ? List.<String>of()
                    : sent.invigilatorDocsIds()) {
                String other = roomOfInvigilator.putIfAbsent(id.trim(), roomId);
                if (other != null && !other.equals(roomId)) {
                    throw ApiException.badRequest("INVIGILATOR_IN_TWO_ROOMS",
                            "Staff member '" + id.trim() + "' is on duty in two rooms of this paper. "
                                    + "A person can watch one room at a time.");
                }
            }
        }

        //! Step 9 - Each room is this school's and in use.
        Map<String, Room> roomById = new LinkedHashMap<>();
        Map<String, Integer> roomTotal = new LinkedHashMap<>();
        if (!roomIds.isEmpty()) {
            // TODO: read rooms
            for (Room room : rooms.findBySchoolIdAndIdIn(school.getId(), roomIds)) {
                roomById.put(room.getId(), room);
            }
            List<String> unknownRooms = roomIds.stream().filter(id -> !roomById.containsKey(id)).toList();
            if (!unknownRooms.isEmpty()) {
                throw ApiException.notFound("ROOM_NOT_FOUND",
                        "No room with id " + String.join(", ", unknownRooms) + " in this school.");
            }
            for (Room room : roomById.values()) {
                if (room.getStatus() != null && room.getStatus() != RoomStatus.IN_USE) {
                    throw ApiException.conflict("ROOM_NOT_IN_USE",
                            "Room '" + room.getName() + "' is " + room.getStatus() + ".");
                }
            }

            //! Step 10 - A room may hold more than one exam at a time — a big hall with space left —
            //! as long as everyone sitting in it at an overlapping time fits its capacity. Every
            //! overlapping paper already in the room counts, whatever its exam or sitting.
            Map<String, ExamSchedule> otherSittingIn = new LinkedHashMap<>();
            //! Another sitting in the room with no seat count — then nobody knows how full it is.
            Map<String, ExamSchedule> otherSittingWithoutSeats = new LinkedHashMap<>();
            //! A room's seat count is for a whole sitting — the same exam, subject, part, date and
            //! times — and every paper of the sitting carries it, so it counts once per room.
            Map<String, Integer> seatsPerSittingInRoom = new LinkedHashMap<>();
            //! What this same sitting already has in a room, from sections added earlier.
            Map<String, Integer> seatsOfThisSitting = new LinkedHashMap<>();
            // TODO: read exam schedules (papers in these rooms that day)
            for (ExamSchedule other : examSchedules.findPapersInRooms(school.getId(), roomIds,
                    request.examDate(), ExamScheduleStatus.CANCELLED)) {
                if (!ExamScheduleServiceUtils.overlaps(other, request.startTime(), request.endTime())) {
                    continue;
                }
                boolean sameSitting = ExamScheduleServiceUtils.sameSitting(other, examId,
                        storedSubjectCode, componentName, request.examDate(), request.startTime(),
                        request.endTime());
                for (ExamRoomAllocation used : other.getRooms()) {
                    if (!roomById.containsKey(used.getRoomDocsId())) {
                        continue;
                    }
                    String part = other.getComponentName() == null ? ""
                            : other.getComponentName().toLowerCase();
                    String countKey = used.getRoomDocsId() + "|" + other.getExamDocsId() + "|"
                            + other.getSubjectCode() + "|" + part + "|" + other.getExamDate() + "|"
                            + other.getStartTime() + "|" + other.getEndTime();
                    int seats = used.getSeatCount() == null ? 0 : used.getSeatCount();
                    seatsPerSittingInRoom.merge(countKey, seats, Math::max);
                    if (sameSitting) {
                        if (used.getSeatCount() != null) {
                            seatsOfThisSitting.merge(used.getRoomDocsId(), seats, Math::max);
                        }
                    } else {
                        otherSittingIn.putIfAbsent(used.getRoomDocsId(), other);
                        if (used.getSeatCount() == null) {
                            otherSittingWithoutSeats.putIfAbsent(used.getRoomDocsId(), other);
                        }
                    }
                }
            }
            Map<String, Integer> seatsAlreadyTaken = new LinkedHashMap<>();
            seatsPerSittingInRoom.forEach((key, seats) -> seatsAlreadyTaken
                    .merge(key.substring(0, key.indexOf('|')), seats, Integer::sum));
            for (ExamScheduleCreateRequest.Room sent : sentRooms) {
                Room room = roomById.get(sent.roomDocsId().trim());
                ExamSchedule other = otherSittingIn.get(room.getId());

                //! Sharing with another exam is only safe when we know how many the room seats.
                if (other != null && room.getCapacity() == null) {
                    throw ApiException.conflict("ROOM_CLASH",
                            "Room '" + room.getName() + "' already holds "
                                    + ExamScheduleServiceUtils.paperLabel(other.getSubjectCode(),
                                            other.getComponentName())
                                    + " for section " + other.getSectionNo() + " from "
                                    + other.getStartTime() + " to " + other.getEndTime()
                                    + " that day, and its capacity is not recorded, so it cannot be "
                                    + "shared. Record the room's capacity to seat two exams in it.");
                }

                //! Sharing is also only safe when every sitting in the room says how many it seats.
                if (other != null && (sent.seatCount() == null
                        || otherSittingWithoutSeats.containsKey(room.getId()))) {
                    ExamSchedule unknown = sent.seatCount() == null ? other
                            : otherSittingWithoutSeats.get(room.getId());
                    throw ApiException.conflict("ROOM_CLASH",
                            "Room '" + room.getName() + "' already holds "
                                    + ExamScheduleServiceUtils.paperLabel(unknown.getSubjectCode(),
                                            unknown.getComponentName())
                                    + " for section " + unknown.getSectionNo() + " from "
                                    + unknown.getStartTime() + " to " + unknown.getEndTime()
                                    + " that day, and "
                                    + (sent.seatCount() == null ? "no seatCount was sent for it here"
                                            : "that paper has no seat count")
                                    + ", so it cannot be shared. Give the seats to seat two exams in it.");
                }

                //! No seats sent and nobody else in the room — nothing to check, and nothing to store.
                if (sent.seatCount() == null) {
                    roomTotal.put(room.getId(), seatsOfThisSitting.get(room.getId()));
                    continue;
                }
                int wanted = seatsAlreadyTaken.getOrDefault(room.getId(), 0)
                        + sent.seatCount();
                //! Sections joining a sitting already in this room add to its seats; the new papers
                //! carry the sitting's new total, so it still counts once.
                roomTotal.put(room.getId(), seatsOfThisSitting.getOrDefault(room.getId(), 0)
                        + sent.seatCount());
                if (room.getCapacity() != null && wanted > room.getCapacity()) {
                    throw ApiException.conflict("ROOM_OVER_CAPACITY",
                            "Room '" + room.getName() + "' seats " + room.getCapacity() + ", but "
                                    + wanted + " would sit there (" + sent.seatCount()
                                    + " for these " + sectionNos.size() + " section(s)"
                                    + (seatsAlreadyTaken.containsKey(room.getId())
                                            ? ", plus " + seatsAlreadyTaken.get(room.getId())
                                                    + " already in it at that time" : "")
                                    + ").");
                }
            }
        }

        //! Step 11 - The invigilators: each this school's staff, and in one place at a time — not on
        //! duty in another room at an overlapping time. The same room is the same place, so one
        //! person may watch two exams sharing a hall.
        Map<String, String> staffNames = new LinkedHashMap<>();
        if (!roomOfInvigilator.isEmpty()) {
            // TODO: read staff (the invigilators)
            for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), roomOfInvigilator.keySet())) {
                staffNames.put(person.getId(), person.getFullName());
            }
            List<String> unknown = roomOfInvigilator.keySet().stream()
                    .filter(id -> !staffNames.containsKey(id))
                    .toList();
            if (!unknown.isEmpty()) {
                throw ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id " + String.join(", ", unknown) + " in this school.");
            }

            // TODO: read exam schedules (papers these staff are on that day)
            for (ExamSchedule other : examSchedules.findPapersWithInvigilators(school.getId(),
                    roomOfInvigilator.keySet(), request.examDate(), ExamScheduleStatus.CANCELLED)) {
                if (!ExamScheduleServiceUtils.overlaps(other, request.startTime(), request.endTime())) {
                    continue;
                }
                for (ExamRoomAllocation used : other.getRooms()) {
                    for (String id : used.getInvigilatorDocsIds()) {
                        String wantedRoom = roomOfInvigilator.get(id);
                        if (wantedRoom == null || wantedRoom.equals(used.getRoomDocsId())) {
                            continue;
                        }
                        throw ApiException.conflict("INVIGILATOR_CLASH",
                                staffNames.getOrDefault(id, id) + " is already on duty at "
                                        + ExamScheduleServiceUtils.paperLabel(other.getSubjectCode(),
                                                other.getComponentName())
                                        + " for section " + other.getSectionNo() + " from "
                                        + other.getStartTime() + " to " + other.getEndTime()
                                        + " that day, in another room. A person can be in one "
                                        + "place at a time.");
                    }
                }
            }
        }

        //! Step 12 - Build one paper per section, each with the same rooms. A paper added after the datesheet went out is
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
                    .rooms(sentRooms.stream()
                            .map(sent -> ExamRoomAllocation.builder()
                                    .roomDocsId(sent.roomDocsId().trim())
                                    .seatCount(roomTotal.get(sent.roomDocsId().trim()))
                                    .invigilatorDocsIds(sent.invigilatorDocsIds() == null
                                            ? new ArrayList<>()
                                            : new ArrayList<>(new LinkedHashSet<>(sent.invigilatorDocsIds()
                                                    .stream().map(String::trim).toList())))
                                    .build())
                            .toList())
                    .status(status)
                    .build();
            toSave.add(paper);
        }

        //! Step 13 - Save them all, in one transaction. The unique index on exam, class, section,
        //! subject and component stops two requests adding the same paper twice.
        // TODO: insert exam schedules
        List<ExamSchedule> saved = examSchedules.saveAll(toSave);
        log.info("[addPapers] Step 2: Saved {} paper(s) of {} to exam '{}'", saved.size(),
                storedSubjectCode, exam.getName());

        Map<String, String> roomNames = new LinkedHashMap<>();
        roomById.forEach((id, room) -> roomNames.put(id, room.getName()));
        String finalSubjectName = subjectName;
        List<ExamScheduleResponse> papers = saved.stream()
                .map(one -> ExamScheduleResponse.of(one, schoolClass.getName(), finalSubjectName,
                        roomNames, staffNames))
                .toList();
        int seats = sentRooms.stream()
                .map(ExamScheduleCreateRequest.Room::seatCount)
                .filter(count -> count != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new ExamPapersAddedResponse(exam.getId(), exam.getName(), papers.size(), papers,
                sentRooms.isEmpty()
                        ? "No rooms yet — give each paper its rooms before the datesheet goes out."
                        : (seats == 0 ? "No seat counts yet" : seats + " seat(s)") + " for these "
                                + sectionNos.size() + " section(s) across " + sentRooms.size()
                                + " room(s). Add the exam's other papers the same way.");
    }

    /**
     * Endpoint #11 — <b>the datesheet</b>: every paper of an exam, by date, start time, class and
     * section, each with its class, subject, rooms and invigilators named.
     *
     * <p>Filters, all optional: one class, one section (ignoring case), one day, one status.
     * Cancelled papers are shown, marked, unless {@code status} leaves them out. Not paged — a
     * datesheet is read whole. <b>No gates.</b> A read.
     */
    public ExamDatesheetResponse getDatesheet(String academicYear, String examDocsId,
            ExamDatesheetRequest request) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String examId = examDocsId == null ? "" : examDocsId.trim();
        log.info("[getDatesheet] Step 1: Reading the datesheet of exam {} for school {}", examId,
                school.getId());

        //! Step 2 - The year, then the exam inside it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam
        Exam exam = exams.findByIdAndSchoolIdAndAcademicYear(examId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND",
                        "No exam with id '" + examId + "' in '" + year.getName() + "'."));

        //! Step 3 - The papers that match, by date and start time.
        // TODO: read exam schedules (the datesheet)
        List<ExamSchedule> papers = examSchedules.findDatesheet(school.getId(), examId,
                request.classDocsId(), request.sectionNo(), request.date(), request.status());

        //! Step 4 - The names behind the ids: the classes (and their subjects), the rooms and the
        //! staff — one read each, for the whole datesheet.
        Map<String, SchoolClass> classById = new LinkedHashMap<>();
        Map<String, String> roomNames = new LinkedHashMap<>();
        Map<String, String> staffNames = new LinkedHashMap<>();
        if (!papers.isEmpty()) {
            List<String> classIds = papers.stream().map(ExamSchedule::getClassDocsId).distinct().toList();
            // TODO: read school classes (the classes on the datesheet)
            for (SchoolClass one : schoolClasses.findBySchoolIdAndIdIn(school.getId(), classIds)) {
                classById.put(one.getId(), one);
            }

            Set<String> roomIds = new LinkedHashSet<>();
            Set<String> staffIds = new LinkedHashSet<>();
            for (ExamSchedule paper : papers) {
                for (ExamRoomAllocation room : paper.getRooms()) {
                    roomIds.add(room.getRoomDocsId());
                    staffIds.addAll(room.getInvigilatorDocsIds());
                }
            }
            if (!roomIds.isEmpty()) {
                // TODO: read rooms (the rooms on the datesheet)
                for (Room room : rooms.findBySchoolIdAndIdIn(school.getId(), roomIds)) {
                    roomNames.put(room.getId(), room.getName());
                }
            }
            if (!staffIds.isEmpty()) {
                // TODO: read staff (the invigilators on the datesheet)
                for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), staffIds)) {
                    staffNames.put(person.getId(), person.getFullName());
                }
            }
        }

        //! Step 5 - One row per paper: within a day and a start time, by class name, then section,
        //! then subject.
        List<ExamScheduleResponse> rows = papers.stream()
                .sorted(Comparator.comparing(ExamSchedule::getExamDate)
                        .thenComparing(ExamSchedule::getStartTime)
                        .thenComparing(paper -> classById.containsKey(paper.getClassDocsId())
                                ? classById.get(paper.getClassDocsId()).getName() : "")
                        .thenComparing(ExamSchedule::getSectionNo)
                        .thenComparing(ExamSchedule::getSubjectCode))
                .map(paper -> {
                    SchoolClass schoolClass = classById.get(paper.getClassDocsId());
                    String subjectName = schoolClass == null || schoolClass.getSubjects() == null ? null
                            : schoolClass.getSubjects().stream()
                                    .filter(one -> one.getSubjectCode().equalsIgnoreCase(paper.getSubjectCode()))
                                    .map(ClassSubject::getName)
                                    .findFirst()
                                    .orElse(null);
                    return ExamScheduleResponse.of(paper,
                            schoolClass == null ? null : schoolClass.getName(), subjectName,
                            roomNames, staffNames);
                })
                .toList();

        log.info("[getDatesheet] Step 2: Exam '{}' has {} matching paper(s)", exam.getName(),
                rows.size());

        return new ExamDatesheetResponse(exam.getId(), exam.getName(), exam.getStartDate(),
                exam.getEndDate(), rows.size(), rows);
    }

    /**
     * Endpoint #12 — <b>one paper in full</b>: the paper with its names, who is expected in the
     * hall and how they were marked, and the marks entered for it.
     *
     * <p><b>Expected</b> is the section's children on the paper's date who are ELIGIBLE candidates
     * of the exam, plus anyone who already has a hall entry for this paper (a child debarred after
     * they sat it). Debarred children and children who are not candidates are counted, not listed.
     * <b>No gates.</b> A read.
     */
    public ExamScheduleDetailResponse getPaper(String academicYear, String examScheduleDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[getPaper] Step 1: Reading paper {} for school {}", paperId, school.getId());

        //! Step 2 - The year, then the paper inside it, then its exam.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));

        // TODO: read exam
        String examName = exams.findByIdAndSchoolIdAndAcademicYear(paper.getExamDocsId(),
                school.getId(), year.getName()).map(Exam::getName).orElse(null);

        //! Step 3 - The names behind the class, subject, rooms and invigilators.
        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findBySchoolIdAndIdIn(school.getId(), List.of(paper.getClassDocsId()))
                .stream().findFirst().orElse(null);
        String subjectName = schoolClass == null || schoolClass.getSubjects() == null ? null
                : schoolClass.getSubjects().stream()
                        .filter(one -> one.getSubjectCode().equalsIgnoreCase(paper.getSubjectCode()))
                        .map(ClassSubject::getName)
                        .findFirst()
                        .orElse(null);

        Set<String> roomIds = new LinkedHashSet<>();
        Set<String> staffIds = new LinkedHashSet<>();
        for (ExamRoomAllocation room : paper.getRooms()) {
            roomIds.add(room.getRoomDocsId());
            staffIds.addAll(room.getInvigilatorDocsIds());
        }
        Map<String, String> roomNames = new LinkedHashMap<>();
        if (!roomIds.isEmpty()) {
            // TODO: read rooms
            for (Room room : rooms.findBySchoolIdAndIdIn(school.getId(), roomIds)) {
                roomNames.put(room.getId(), room.getName());
            }
        }
        Map<String, String> staffNames = new LinkedHashMap<>();
        if (!staffIds.isEmpty()) {
            // TODO: read staff (the invigilators)
            for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), staffIds)) {
                staffNames.put(person.getId(), person.getFullName());
            }
        }

        //! Step 4 - Who was in the section on the paper's date, and which of them are candidates.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        Set<String> sectionChildren = roster.sectionRecords().keySet();
        Map<String, ExamCandidate> candidateOf = roster.candidates();

        //! Step 5 - The hall counts.
        int eligible = 0;
        int debarred = 0;
        int expected = 0;
        Map<ExamAttendanceStatus, Integer> marked = new LinkedHashMap<>();
        for (ExamCandidate one : candidateOf.values()) {
            ExamPaperAttendance entry = one.getPapers().stream()
                    .filter(p -> paperId.equals(p.getExamScheduleDocsId()))
                    .findFirst()
                    .orElse(null);
            boolean inSection = sectionChildren.contains(one.getStudentDocsId());
            if (inSection && one.getStatus() == ExamCandidateStatus.ELIGIBLE) {
                eligible++;
            }
            if (inSection && one.getStatus() == ExamCandidateStatus.DEBARRED && entry == null) {
                debarred++;
            }
            if (entry != null) {
                expected++;
                marked.merge(entry.getAttendanceStatus(), 1, Integer::sum);
            } else if (inSection && one.getStatus() == ExamCandidateStatus.ELIGIBLE) {
                expected++;
            }
        }
        int notCandidates = (int) sectionChildren.stream().filter(id -> !candidateOf.containsKey(id)).count();
        int present = marked.getOrDefault(ExamAttendanceStatus.PRESENT, 0);
        int markedTotal = marked.values().stream().mapToInt(Integer::intValue).sum();

        ExamScheduleDetailResponse.Hall hall = new ExamScheduleDetailResponse.Hall(
                sectionChildren.size(), eligible, debarred, notCandidates, expected, present,
                marked.getOrDefault(ExamAttendanceStatus.ABSENT, 0),
                marked.getOrDefault(ExamAttendanceStatus.UNFAIR_MEANS, 0),
                marked.getOrDefault(ExamAttendanceStatus.EXEMPT, 0),
                expected - markedTotal);

        //! Step 6 - The marks, per status, every status listed.
        // TODO: count student marks (per status, this paper)
        Map<String, Long> markCounts = studentMarks.countByStatusForPaper(school.getId(), paperId);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (MarkEntryStatus status : MarkEntryStatus.values()) {
            byStatus.put(status.name(), markCounts.getOrDefault(status.name(), 0L));
        }
        long entered = byStatus.values().stream().mapToLong(Long::longValue).sum();

        log.info("[getPaper] Step 2: Paper {} — {} expected, {} marked, {} mark(s) entered", paperId,
                expected, markedTotal, entered);

        return new ExamScheduleDetailResponse(
                ExamScheduleResponse.of(paper, schoolClass == null ? null : schoolClass.getName(),
                        subjectName, roomNames, staffNames),
                examName, hall,
                new ExamScheduleDetailResponse.Marks(byStatus, entered, Math.max(0, present - entered)));
    }

    /**
     * Endpoint #16 — <b>the paper is over</b>: {@code PUBLISHED} → {@code COMPLETED}, and its hall
     * is closed — #15 refuses it from here. Refused while any child on the register is unmarked;
     * {@code EXEMPT} counts as marked, and a debarred child, who is not on the register, does not
     * hold it up. {@code version} is required.
     *
     * <p>Gates 1, 2 and 4 run in the controller.
     */
    public ExamHallRegisterResponse completePaper(String academicYear, String examScheduleDocsId,
            ExamScheduleCompleteRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String paperId = examScheduleDocsId == null ? "" : examScheduleDocsId.trim();
        log.info("[completePaper] Step 1: Closing the hall of paper {} for school {}", paperId, school.getId());

        //! Step 2 - The year, the paper, and its version.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        // TODO: read exam schedule
        ExamSchedule paper = examSchedules
                .findByIdAndSchoolIdAndAcademicYear(paperId, school.getId(), year.getName())
                .orElseThrow(() -> ApiException.notFound("EXAM_SCHEDULE_NOT_FOUND",
                        "No paper with id '" + paperId + "' in '" + year.getName() + "'."));
        if (!request.version().equals(paper.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This paper's register was saved by someone else. Please read it again before " + "closing it.");
        }

        //! Step 3 - Only a published paper whose day has come can be closed.
        if (paper.getStatus() == ExamScheduleStatus.COMPLETED) {
            throw ApiException.conflict("PAPER_ALREADY_COMPLETED",
                    "This paper is already completed. Reopen it to correct the hall.");
        }
        if (paper.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw ApiException.conflict("PAPER_NOT_OPEN",
                    "This paper is " + paper.getStatus() + ". Only a PUBLISHED paper can be completed.");
        }
        LocalDate today = Dates.todayIn(school.getDefaultTimeZone());
        if (today.isBefore(paper.getExamDate())) {
            throw ApiException.conflict("PAPER_NOT_YET_SAT",
                    "This paper is on " + Dates.readable(paper.getExamDate())
                            + ". It can be completed from that day.");
        }

        //! Step 4 - Everyone on the register needs a mark first. Debarred children are not on it.
        ExamAttendanceServiceUtils.HallRoster roster = hallUtils.rosterOf(school, year.getName(), paper);
        ExamHallRegisterResponse register = hallUtils.registerOf(school, year.getName(), paper, roster);
        if (register.counts().notMarked() > 0) {
            throw ApiException.conflict("UNMARKED_STUDENTS",
                    register.counts().notMarked() + " of " + register.counts().expected()
                            + " child(ren) on the register have no hall mark yet. Mark every child — "
                            + "EXEMPT for one who does not take this paper — before completing it.");
        }

        //! Step 5 - Close it. A save racing a hall save fails on the version.
        paper.setStatus(ExamScheduleStatus.COMPLETED);
        // TODO: update exam schedule (completed)
        ExamSchedule saved = examSchedules.save(paper);
        log.info("[completePaper] Step 2: Paper {} completed with {} marked", saved.getId(),
                register.counts().marked());

        //! Step 6 - The register as it now stands — the same answer #14 gives.
        return hallUtils.registerOf(school, year.getName(), saved, roster);
    }
}
