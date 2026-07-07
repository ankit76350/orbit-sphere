package com.orbitastra.backend.services.academics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.dto.academics.BulkTimetableRequest;
import com.orbitastra.backend.dto.academics.SchoolTimetableRequest;
import com.orbitastra.backend.dto.academics.SchoolTimetableRequest.ClassSectionTimetable;
import com.orbitastra.backend.dto.academics.TimetableOccurrence;
import com.orbitastra.backend.dto.academics.TimetablePeriod;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.academics.TimetableSlot;
import com.orbitastra.backend.models.academics.enums.SlotType;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.TimetableSlotRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimetableSlotService {

    private static final Set<DayOfWeek> DEFAULT_DAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    private static final int MAX_SLOTS_PER_REQUEST = 2000;
    private static final int MAX_CONFLICTS_REPORTED = 15;
    private static final int MAX_SCHEDULE_RANGE_DAYS = 370;

    private final TimetableSlotRepository timetableSlotRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StaffRepository staffRepository;

    public TimetableSlot createSlot(TimetableSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        slot.setId(null);
        validateSlotFields(slot);
        validateClassSection(slot.getSchoolId(), slot.getClassId(), slot.getSection(), new HashMap<>());
        if (slot.getTeacherId() != null) {
            validateTeachers(slot.getSchoolId(), Set.of(slot.getTeacherId()));
        }
        raiseIfConflicts(findDbConflicts(List.of(slot), null));
        return timetableSlotRepository.save(slot);
    }

    /**
     * Single class section. Delegates to the school-wide implementation with
     * one entry.
     */
    public List<TimetableSlot> createBulkTimetable(BulkTimetableRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        SchoolTimetableRequest schoolRequest = SchoolTimetableRequest.builder()
                .schoolId(request.getSchoolId())
                .days(request.getDays())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .classTimetables(List.of(ClassSectionTimetable.builder()
                        .classId(request.getClassId())
                        .section(request.getSection())
                        .periods(request.getPeriods())
                        .build()))
                .build();
        return createSchoolTimetable(schoolRequest);
    }

    /**
     * Creates the weekly timetables for many class sections of one school at
     * once. Nothing is saved unless the entire batch is free of conflicts.
     */
    public List<TimetableSlot> createSchoolTimetable(SchoolTimetableRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        require(request.getSchoolId(), "schoolId");
        require(request.getEffectiveFrom(), "effectiveFrom");
        String schoolId = request.getSchoolId();
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo (" + request.getEffectiveTo()
                    + ") must not be before effectiveFrom (" + request.getEffectiveFrom() + ").");
        }

        List<ClassSectionTimetable> entries = request.getClassTimetables();
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one entry in classTimetables is required.");
        }

        Set<DayOfWeek> days = (request.getDays() == null || request.getDays().isEmpty())
                ? DEFAULT_DAYS
                : EnumSet.copyOf(request.getDays());

        Map<String, SchoolClass> classCache = new HashMap<>();
        Set<String> seenClassSections = new HashSet<>();
        Set<String> teacherIds = new HashSet<>();
        int totalPeriods = 0;
        for (int i = 0; i < entries.size(); i++) {
            ClassSectionTimetable entry = entries.get(i);
            String label = "classTimetables[" + (i + 1) + "]";
            if (entry == null) {
                throw new IllegalArgumentException(label + " is empty.");
            }
            require(entry.getClassId(), label + ".classId");
            require(entry.getSection(), label + ".section");
            if (!seenClassSections.add(entry.getClassId() + "::" + entry.getSection().toLowerCase())) {
                throw new IllegalArgumentException(label + ": class " + entry.getClassId() + " section '"
                        + entry.getSection() + "' appears more than once in the request.");
            }
            validateClassSection(schoolId, entry.getClassId(), entry.getSection(), classCache);
            validatePeriods(entry.getPeriods(), label);
            totalPeriods += entry.getPeriods().size();
            entry.getPeriods().stream()
                    .filter(p -> p.getTeacherId() != null)
                    .forEach(p -> teacherIds.add(p.getTeacherId()));
        }
        validateTeachers(schoolId, teacherIds);

        // A teacher clash between two entries repeats on every requested day,
        // so it is reported once at the template level.
        raiseIfConflicts(findTemplateTeacherConflicts(entries, classCache));

        long totalSlots = (long) days.size() * totalPeriods;
        if (totalSlots > MAX_SLOTS_PER_REQUEST) {
            throw new IllegalArgumentException("This request would create " + totalSlots
                    + " weekly slots; the maximum per request is " + MAX_SLOTS_PER_REQUEST + ".");
        }

        List<TimetableSlot> slots = new ArrayList<>();
        for (ClassSectionTimetable entry : entries) {
            for (DayOfWeek day : days) {
                for (TimetablePeriod period : entry.getPeriods()) {
                    boolean isBreak = period.getType() == SlotType.BREAK;
                    slots.add(TimetableSlot.builder()
                            .schoolId(schoolId)
                            .classId(entry.getClassId())
                            .section(entry.getSection())
                            .type(isBreak ? SlotType.BREAK : SlotType.LESSON)
                            .subject(isBreak && isBlank(period.getSubject()) ? "Break" : period.getSubject())
                            .teacherId(period.getTeacherId())
                            .dayOfWeek(day)
                            .startTime(period.getStartTime())
                            .endTime(period.getEndTime())
                            .effectiveFrom(request.getEffectiveFrom())
                            .effectiveTo(request.getEffectiveTo())
                            .build());
                }
            }
        }

        raiseIfConflicts(findDbConflicts(slots, null));
        return timetableSlotRepository.saveAll(slots);
    }

    public List<TimetableSlot> getAllSlots() {
        return timetableSlotRepository.findAll();
    }

    public TimetableSlot getSlotById(String id) {
        return timetableSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + id));
    }

    public List<TimetableSlot> getSlotsBySchool(String schoolId) {
        return timetableSlotRepository.findBySchoolId(schoolId);
    }

    /** The weekly template of a class (all sections). */
    public List<TimetableSlot> getSlotsByClass(String schoolId, String classId) {
        return timetableSlotRepository.findBySchoolIdAndClassId(schoolId, classId);
    }

    /** Concrete lessons of a class between two dates, computed from the template. */
    public List<TimetableOccurrence> getClassSchedule(String schoolId, String classId,
            LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return expand(timetableSlotRepository.findBySchoolIdAndClassId(schoolId, classId), startDate, endDate);
    }

    /** Concrete lessons of a teacher between two dates, computed from the template. */
    public List<TimetableOccurrence> getTeacherSchedule(String schoolId, String teacherId,
            LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return expand(timetableSlotRepository.findBySchoolIdAndTeacherId(schoolId, teacherId), startDate, endDate);
    }

    public TimetableSlot updateSlot(String id, TimetableSlot details) {
        TimetableSlot slot = getSlotById(id);

        if (details.getSchoolId() != null) {
            slot.setSchoolId(details.getSchoolId());
        }
        if (details.getClassId() != null) {
            slot.setClassId(details.getClassId());
        }
        if (details.getSection() != null) {
            slot.setSection(details.getSection());
        }
        if (details.getType() != null) {
            slot.setType(details.getType());
            if (details.getType() == SlotType.BREAK) {
                slot.setTeacherId(null);
            }
        }
        if (details.getSubject() != null) {
            slot.setSubject(details.getSubject());
        }
        if (details.getTeacherId() != null) {
            slot.setTeacherId(details.getTeacherId());
        }
        if (details.getDayOfWeek() != null) {
            slot.setDayOfWeek(details.getDayOfWeek());
        }
        if (details.getStartTime() != null) {
            slot.setStartTime(details.getStartTime());
        }
        if (details.getEndTime() != null) {
            slot.setEndTime(details.getEndTime());
        }
        if (details.getEffectiveFrom() != null) {
            slot.setEffectiveFrom(details.getEffectiveFrom());
        }
        if (details.getEffectiveTo() != null) {
            slot.setEffectiveTo(details.getEffectiveTo());
        }

        validateSlotFields(slot);
        validateClassSection(slot.getSchoolId(), slot.getClassId(), slot.getSection(), new HashMap<>());
        if (slot.getTeacherId() != null) {
            validateTeachers(slot.getSchoolId(), Set.of(slot.getTeacherId()));
        }
        raiseIfConflicts(findDbConflicts(List.of(slot), id));
        return timetableSlotRepository.save(slot);
    }

    public void deleteSlot(String id) {
        TimetableSlot slot = getSlotById(id);
        timetableSlotRepository.delete(slot);
    }

    /** Removes the whole weekly timetable of one class section. */
    public long clearSectionTimetable(String schoolId, String classId, String section) {
        return timetableSlotRepository.deleteBySchoolIdAndClassIdAndSectionIgnoreCase(schoolId, classId, section);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private List<TimetableOccurrence> expand(List<TimetableSlot> slots, LocalDate startDate, LocalDate endDate) {
        Map<DayOfWeek, List<TimetableSlot>> byDay = slots.stream()
                .filter(s -> s.getDayOfWeek() != null && s.getEffectiveFrom() != null
                        && s.getStartTime() != null && s.getEndTime() != null)
                .collect(Collectors.groupingBy(TimetableSlot::getDayOfWeek));

        List<TimetableOccurrence> occurrences = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (TimetableSlot slot : byDay.getOrDefault(date.getDayOfWeek(), List.of())) {
                if (date.isBefore(slot.getEffectiveFrom())
                        || (slot.getEffectiveTo() != null && date.isAfter(slot.getEffectiveTo()))) {
                    continue;
                }
                occurrences.add(TimetableOccurrence.builder()
                        .date(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .slotId(slot.getId())
                        .schoolId(slot.getSchoolId())
                        .classId(slot.getClassId())
                        .section(slot.getSection())
                        .type(slot.getType())
                        .subject(slot.getSubject())
                        .teacherId(slot.getTeacherId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build());
            }
        }
        occurrences.sort(Comparator.comparing(TimetableOccurrence::getDate)
                .thenComparing(TimetableOccurrence::getStartTime));
        return occurrences;
    }

    private static void validatePeriods(List<TimetablePeriod> periods, String label) {
        if (periods == null || periods.isEmpty()) {
            throw new IllegalArgumentException(label + ": at least one period is required.");
        }
        for (int i = 0; i < periods.size(); i++) {
            TimetablePeriod period = periods.get(i);
            if (period == null) {
                throw new IllegalArgumentException(label + ".periods[" + (i + 1) + "] is empty.");
            }
            if (period.getType() == SlotType.BREAK) {
                if (period.getTeacherId() != null) {
                    throw new IllegalArgumentException(label + ".periods[" + (i + 1)
                            + "]: a BREAK period must not have a teacherId.");
                }
            } else {
                require(period.getSubject(), label + ".periods[" + (i + 1) + "].subject");
                require(period.getTeacherId(), label + ".periods[" + (i + 1) + "].teacherId");
            }
            require(period.getStartTime(), label + ".periods[" + (i + 1) + "].startTime");
            require(period.getEndTime(), label + ".periods[" + (i + 1) + "].endTime");
            if (!period.getStartTime().isBefore(period.getEndTime())) {
                throw new IllegalArgumentException(label + ".periods[" + (i + 1) + "] ('" + period.getSubject()
                        + "'): startTime must be before endTime.");
            }
        }
        // All periods of one entry target the same class section, so any
        // overlap inside the entry is invalid regardless of teacher.
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                TimetablePeriod a = periods.get(i);
                TimetablePeriod b = periods.get(j);
                if (overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    throw new IllegalArgumentException(label + ": periods " + (i + 1) + " ('" + a.getSubject() + "' "
                            + a.getStartTime() + "-" + a.getEndTime() + ") and " + (j + 1) + " ('" + b.getSubject()
                            + "' " + b.getStartTime() + "-" + b.getEndTime() + ") overlap with each other.");
                }
            }
        }
    }

    /**
     * Detects the same teacher booked at overlapping times in two different
     * class-section entries of the same request.
     */
    private List<String> findTemplateTeacherConflicts(List<ClassSectionTimetable> entries,
            Map<String, SchoolClass> classCache) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                ClassSectionTimetable a = entries.get(i);
                ClassSectionTimetable b = entries.get(j);
                for (TimetablePeriod pa : a.getPeriods()) {
                    for (TimetablePeriod pb : b.getPeriods()) {
                        if (pa.getTeacherId() != null && pa.getTeacherId().equals(pb.getTeacherId())
                                && overlaps(pa.getStartTime(), pa.getEndTime(), pb.getStartTime(), pb.getEndTime())) {
                            conflicts.add("Teacher " + pa.getTeacherId() + " is booked in two places in this request: '"
                                    + pa.getSubject() + "' (" + describe(a, classCache) + " " + pa.getStartTime() + "-"
                                    + pa.getEndTime() + ") and '" + pb.getSubject() + "' (" + describe(b, classCache)
                                    + " " + pb.getStartTime() + "-" + pb.getEndTime() + ")");
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    private static String describe(ClassSectionTimetable entry, Map<String, SchoolClass> classCache) {
        SchoolClass schoolClass = classCache.get(entry.getClassId());
        String className = schoolClass != null && schoolClass.getName() != null ? schoolClass.getName()
                : entry.getClassId();
        return "class " + className + " section " + entry.getSection();
    }

    private void validateSlotFields(TimetableSlot slot) {
        require(slot.getSchoolId(), "schoolId");
        require(slot.getClassId(), "classId");
        require(slot.getSection(), "section");
        if (slot.getType() == SlotType.BREAK) {
            if (slot.getTeacherId() != null) {
                throw new IllegalArgumentException("A BREAK slot must not have a teacherId.");
            }
            if (isBlank(slot.getSubject())) {
                slot.setSubject("Break");
            }
        } else {
            slot.setType(SlotType.LESSON);
            require(slot.getSubject(), "subject");
            require(slot.getTeacherId(), "teacherId");
        }
        require(slot.getDayOfWeek(), "dayOfWeek");
        require(slot.getStartTime(), "startTime");
        require(slot.getEndTime(), "endTime");
        require(slot.getEffectiveFrom(), "effectiveFrom");
        if (!slot.getStartTime().isBefore(slot.getEndTime())) {
            throw new IllegalArgumentException("startTime must be before endTime.");
        }
        if (slot.getEffectiveTo() != null && slot.getEffectiveTo().isBefore(slot.getEffectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo (" + slot.getEffectiveTo()
                    + ") must not be before effectiveFrom (" + slot.getEffectiveFrom() + ").");
        }
        if (!schoolRepository.existsById(slot.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + slot.getSchoolId());
        }
    }

    /**
     * Checks that the class exists, belongs to the school, and that the given
     * section is one of the class's sections.
     */
    private void validateClassSection(String schoolId, String classId, String section,
            Map<String, SchoolClass> classCache) {
        SchoolClass schoolClass = classCache.computeIfAbsent(classId,
                id -> schoolClassRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id)));
        if (!schoolId.equals(schoolClass.getSchoolId())) {
            throw new IllegalArgumentException("Class " + classId + " does not belong to school " + schoolId + ".");
        }
        List<String> sections = schoolClass.getSections();
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Class '" + schoolClass.getName()
                    + "' has no sections defined. Add sections to the class before creating its timetable.");
        }
        if (sections.stream().noneMatch(s -> s.equalsIgnoreCase(section))) {
            throw new IllegalArgumentException("Section '" + section + "' does not exist in class '"
                    + schoolClass.getName() + "'. Available sections: " + sections);
        }
    }

    private void validateTeachers(String schoolId, Set<String> teacherIds) {
        Map<String, Staff> staffById = new HashMap<>();
        staffRepository.findAllById(teacherIds).forEach(staff -> staffById.put(staff.getId(), staff));
        for (String teacherId : teacherIds) {
            Staff staff = staffById.get(teacherId);
            if (staff == null) {
                throw new ResourceNotFoundException("Teacher (staff) not found with id: " + teacherId);
            }
            if (!schoolId.equals(staff.getSchoolId())) {
                throw new IllegalArgumentException(
                        "Teacher " + teacherId + " does not belong to school " + schoolId + ".");
            }
        }
    }

    /**
     * Checks every candidate slot against what is already stored. Two slots
     * clash when they are on the same weekday, their times overlap, their
     * validity windows overlap, and they share either the teacher (one person
     * cannot be in two rooms) or the class section (one room cannot host two
     * lessons).
     */
    private List<String> findDbConflicts(List<TimetableSlot> candidates, String excludeSlotId) {
        String schoolId = candidates.get(0).getSchoolId();
        Set<String> teacherIds = new HashSet<>();
        Set<String> classIds = new HashSet<>();
        for (TimetableSlot candidate : candidates) {
            if (candidate.getTeacherId() != null) {
                teacherIds.add(candidate.getTeacherId());
            }
            classIds.add(candidate.getClassId());
        }

        Map<String, TimetableSlot> existingById = new HashMap<>();
        timetableSlotRepository.findBySchoolIdAndTeacherIdIn(schoolId, teacherIds)
                .forEach(s -> existingById.put(s.getId(), s));
        for (String classId : classIds) {
            timetableSlotRepository.findBySchoolIdAndClassId(schoolId, classId)
                    .forEach(s -> existingById.put(s.getId(), s));
        }
        if (excludeSlotId != null) {
            existingById.remove(excludeSlotId);
        }
        if (existingById.isEmpty()) {
            return List.of();
        }

        Map<DayOfWeek, List<TimetableSlot>> existingByDay = existingById.values().stream()
                .filter(s -> s.getDayOfWeek() != null && s.getEffectiveFrom() != null
                        && s.getStartTime() != null && s.getEndTime() != null)
                .collect(Collectors.groupingBy(TimetableSlot::getDayOfWeek));

        List<String> conflicts = new ArrayList<>();
        for (TimetableSlot candidate : candidates) {
            for (TimetableSlot existing : existingByDay.getOrDefault(candidate.getDayOfWeek(), List.of())) {
                if (!overlaps(candidate.getStartTime(), candidate.getEndTime(),
                        existing.getStartTime(), existing.getEndTime())) {
                    continue;
                }
                if (!validityOverlaps(candidate.getEffectiveFrom(), candidate.getEffectiveTo(),
                        existing.getEffectiveFrom(), existing.getEffectiveTo())) {
                    continue;
                }
                String window = existing.getEffectiveFrom() + ".."
                        + (existing.getEffectiveTo() == null ? "ongoing" : existing.getEffectiveTo());
                if (candidate.getTeacherId() != null && candidate.getTeacherId().equals(existing.getTeacherId())) {
                    conflicts.add("Teacher " + candidate.getTeacherId() + " already teaches '"
                            + existing.getSubject() + "' (class " + existing.getClassId() + ", section "
                            + existing.getSection() + ") every " + existing.getDayOfWeek() + " "
                            + existing.getStartTime() + "-" + existing.getEndTime() + " (valid " + window + ")");
                } else if (candidate.getClassId().equals(existing.getClassId())
                        && candidate.getSection().equalsIgnoreCase(existing.getSection())) {
                    conflicts.add("Class " + candidate.getClassId() + " section " + candidate.getSection()
                            + " already has '" + existing.getSubject() + "' with teacher "
                            + existing.getTeacherId() + " every " + existing.getDayOfWeek() + " "
                            + existing.getStartTime() + "-" + existing.getEndTime() + " (valid " + window + ")");
                }
            }
        }
        return conflicts;
    }

    private void raiseIfConflicts(List<String> conflicts) {
        if (conflicts.isEmpty()) {
            return;
        }
        String shown = conflicts.stream()
                .limit(MAX_CONFLICTS_REPORTED)
                .collect(Collectors.joining("; "));
        String suffix = conflicts.size() > MAX_CONFLICTS_REPORTED
                ? " ... and " + (conflicts.size() - MAX_CONFLICTS_REPORTED) + " more conflict(s)"
                : "";
        throw new ConflictException(
                conflicts.size() + " scheduling conflict(s) found, nothing was saved: " + shown + suffix);
    }

    private static boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private static boolean validityOverlaps(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB) {
        return (toA == null || !toA.isBefore(fromB)) && (toB == null || !toB.isBefore(fromA));
    }

    private static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate (" + endDate + ") must not be before startDate (" + startDate + ").");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_SCHEDULE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Schedule range must not exceed " + MAX_SCHEDULE_RANGE_DAYS + " days.");
        }
    }

    private static void require(Object value, String fieldName) {
        if (value == null || (value instanceof String && ((String) value).isBlank())) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
