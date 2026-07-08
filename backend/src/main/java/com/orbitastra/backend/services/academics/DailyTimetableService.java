package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.dto.academics.DaySchedule;
import com.orbitastra.backend.dto.academics.TimetableCreateRequest;
import com.orbitastra.backend.dto.academics.TimetableCreateRequest.ClassSectionTimetable;
import com.orbitastra.backend.dto.academics.TimetableCreationResult;
import com.orbitastra.backend.dto.academics.TimetableCreationResult.SkippedDate;
import com.orbitastra.backend.dto.academics.TimetablePeriod;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.DailyTimetable;
import com.orbitastra.backend.models.academics.DailyTimetable.TimetableEntry;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.academics.enums.SlotType;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.academics.DailyTimetableRepository;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyTimetableService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int MAX_CONFLICTS_REPORTED = 15;

    private final DailyTimetableRepository dailyTimetableRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StaffRepository staffRepository;
    private final AcademicYearRepository academicYearRepository;

    /**
     * Creates (or extends) the daily timetables of a school over a date range.
     * Holidays and weekly offs from the school's academic years are skipped
     * and reported back. Nothing is saved unless the whole batch is conflict
     * free.
     */
    public TimetableCreationResult createTimetable(TimetableCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        require(request.getSchoolId(), "schoolId");
        if (request.getStartDate() == null) {
            if (request.getEndDate() != null) {
                throw new IllegalArgumentException(
                        "Please fill the start date ('Timetable starts from') — 'Runs until' alone is not enough.");
            }
            throw new IllegalArgumentException("Please fill the start date ('Timetable starts from').");
        }
        String schoolId = request.getSchoolId();
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate;
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("'Runs until' (" + endDate
                    + ") cannot be before 'Timetable starts from' (" + startDate + ").");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "The date range is too long — it cannot be more than " + MAX_RANGE_DAYS + " days (one year).");
        }

        // ---- the whole range must lie inside ONE academic year of the school ----
        AcademicYear academicYear;
        if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
            // The frontend names the year explicitly; validate the dates against it.
            academicYear = academicYearRepository.findBySchoolIdAndName(schoolId, request.getAcademicYear())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Academic year '" + request.getAcademicYear() + "' not found for this school."));
            if (startDate.isBefore(academicYear.getStartDate()) || startDate.isAfter(academicYear.getEndDate())) {
                throw new IllegalArgumentException("'Timetable starts from' (" + startDate
                        + ") is outside your academic year '" + academicYear.getName() + "', which runs "
                        + academicYear.getStartDate() + " to " + academicYear.getEndDate() + ".");
            }
        } else {
            academicYear = academicYearRepository.findBySchoolId(schoolId).stream()
                    .filter(y -> y.getStartDate() != null && y.getEndDate() != null
                            && !startDate.isBefore(y.getStartDate()) && !startDate.isAfter(y.getEndDate()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("'Timetable starts from' (" + startDate
                            + ") does not fall in any academic year of this school. "
                            + "Create the academic year (e.g. '2026-2027') with its start/end dates first."));
        }

        if (endDate.isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException("'Runs until' (" + endDate + ") goes beyond the academic year '"
                    + academicYear.getName() + "', which ends on " + academicYear.getEndDate()
                    + ". A timetable cannot cross academic years — create the next academic year and a separate "
                    + "timetable for it.");
        }

        List<ClassSectionTimetable> sections = request.getClassTimetables();
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Please add the timetable of at least one class section.");
        }

        // ---- validate every class section and its periods ----
        Map<String, SchoolClass> classCache = new HashMap<>();
        Set<String> seenClassSections = new HashSet<>();
        Set<String> teacherIds = new HashSet<>();
        for (int i = 0; i < sections.size(); i++) {
            ClassSectionTimetable entry = sections.get(i);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Timetable entry " + (i + 1) + " is empty — please fill it in or remove it.");
            }
            if (entry.getClassId() == null || entry.getClassId().isBlank()) {
                throw new IllegalArgumentException("Timetable entry " + (i + 1) + ": please choose a class.");
            }
            if (entry.getSection() == null || entry.getSection().isBlank()) {
                throw new IllegalArgumentException("Timetable entry " + (i + 1) + ": please choose a section.");
            }
            validateClassSection(schoolId, entry.getClassId(), entry.getSection(), classCache);
            // From here on, messages can address the section by its real name.
            String label = classLabel(entry.getClassId(), classCache) + " Section " + entry.getSection();
            if (!seenClassSections.add(entry.getClassId() + "::" + entry.getSection().toLowerCase())) {
                throw new IllegalArgumentException(label
                        + " was added more than once — each class section can only appear once per request.");
            }
            validatePeriods(entry.getPeriods(), label);
            entry.getPeriods().stream()
                    .filter(p -> p.getTeacherId() != null)
                    .forEach(p -> teacherIds.add(p.getTeacherId()));
        }
        Map<String, Staff> staffCache = validateTeachers(schoolId, teacherIds);
        raiseIfConflicts(findTemplateTeacherConflicts(sections, classCache, staffCache));

        // ---- resolve dates: skip holidays (incl. dated weekly offs) of THIS academic year ----
        Map<LocalDate, String> holidayByDate = new HashMap<>();
        if (academicYear.getHolidays() != null) {
            for (HolidayDetail detail : academicYear.getHolidays()) {
                if (detail == null || detail.getDate() == null) {
                    continue;
                }
                String reason = detail.getType() == HolidayType.WEEKLY_OFF
                        ? detail.getName() + " (weekly off)"
                        : detail.getName();
                holidayByDate.put(detail.getDate(), reason);
            }
        }

        List<LocalDate> workingDates = new ArrayList<>();
        List<SkippedDate> skipped = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (holidayByDate.containsKey(date)) {
                skipped.add(new SkippedDate(date, holidayByDate.get(date)));
            } else {
                workingDates.add(date);
            }
        }
        if (workingDates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Every date in the range is a holiday or weekly off — nothing to create.");
        }

        // ---- template of one day's new entries (same for every date) ----
        List<TimetableEntry> template = new ArrayList<>();
        for (ClassSectionTimetable entry : sections) {
            for (TimetablePeriod period : entry.getPeriods()) {
                boolean isBreak = period.getType() == SlotType.BREAK;
                template.add(TimetableEntry.builder()
                        .classId(entry.getClassId())
                        .section(entry.getSection())
                        .type(isBreak ? SlotType.BREAK : SlotType.LESSON)
                        .subject(isBreak && isBlank(period.getSubject()) ? "Break" : period.getSubject())
                        .teacherId(period.getTeacherId())
                        .startTime(period.getStartTime())
                        .endTime(period.getEndTime())
                        .build());
            }
        }

        // ---- conflict check against already stored day documents ----
        Map<LocalDate, DailyTimetable> existingByDate = dailyTimetableRepository
                .findBySchoolIdAndDateIn(schoolId, workingDates).stream()
                .collect(Collectors.toMap(DailyTimetable::getDate, d -> d));
        List<String> conflicts = new ArrayList<>();
        for (LocalDate date : workingDates) {
            DailyTimetable existing = existingByDate.get(date);
            if (existing == null || existing.getEntries() == null) {
                continue;
            }
            for (TimetableEntry candidate : template) {
                for (TimetableEntry stored : existing.getEntries()) {
                    if (stored.getStartTime() == null || stored.getEndTime() == null
                            || !overlaps(candidate.getStartTime(), candidate.getEndTime(),
                                    stored.getStartTime(), stored.getEndTime())) {
                        continue;
                    }
                    if (candidate.getTeacherId() != null && candidate.getTeacherId().equals(stored.getTeacherId())) {
                        conflicts.add("Teacher " + teacherLabel(candidate.getTeacherId(), staffCache)
                                + " is already scheduled for '" + stored.getSubject() + "' (class "
                                + classLabel(stored.getClassId(), classCache) + ", section " + stored.getSection()
                                + ") on " + date + " " + stored.getStartTime() + "-" + stored.getEndTime());
                    } else if (candidate.getClassId().equals(stored.getClassId())
                            && candidate.getSection().equalsIgnoreCase(stored.getSection())) {
                        conflicts.add("Class " + classLabel(stored.getClassId(), classCache) + " section "
                                + stored.getSection() + " already has '" + stored.getSubject() + "' on " + date
                                + " " + stored.getStartTime() + "-" + stored.getEndTime());
                    }
                }
            }
        }
        raiseIfConflicts(conflicts);

        // ---- write: one document per school per date, linked to its academic year ----
        List<DailyTimetable> toSave = new ArrayList<>();
        for (LocalDate date : workingDates) {
            DailyTimetable doc = existingByDate.get(date);
            if (doc == null) {
                doc = DailyTimetable.builder().schoolId(schoolId).date(date).entries(new ArrayList<>()).build();
            } else if (doc.getEntries() == null) {
                doc.setEntries(new ArrayList<>());
            }
            doc.setAcademicYear(academicYear.getName());
            for (TimetableEntry entry : template) {
                doc.getEntries().add(TimetableEntry.builder()
                        .id(new ObjectId().toHexString())
                        .classId(entry.getClassId())
                        .section(entry.getSection())
                        .type(entry.getType())
                        .subject(entry.getSubject())
                        .teacherId(entry.getTeacherId())
                        .startTime(entry.getStartTime())
                        .endTime(entry.getEndTime())
                        .build());
            }
            toSave.add(doc);
        }
        dailyTimetableRepository.saveAll(toSave);

        return TimetableCreationResult.builder()
                .daysCreated(workingDates.size())
                .totalEntries(workingDates.size() * template.size())
                .dates(workingDates)
                .skipped(skipped)
                .build();
    }

    /** The whole school's timetable for one date (empty entries if none stored). */
    public DailyTimetable getDay(String schoolId, LocalDate date) {
        return dailyTimetableRepository.findBySchoolIdAndDate(schoolId, date)
                .orElse(DailyTimetable.builder().schoolId(schoolId).date(date).entries(List.of()).build());
    }

    public List<DailyTimetable> getRange(String schoolId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return dailyTimetableRepository
                .findBySchoolIdAndDateRange(schoolId, startDate, endDate);
    }

    /** All day documents belonging to one academic year (referenced by its name, e.g. "2026-2027"). */
    public List<DailyTimetable> getByAcademicYear(String schoolId, String academicYearName) {
        return dailyTimetableRepository.findBySchoolIdAndAcademicYear(schoolId, academicYearName);
    }

    /** One class section's lessons per date within a range. */
    public List<DaySchedule> getSectionSchedule(String schoolId, String classId, String section,
            LocalDate startDate, LocalDate endDate) {
        return filterRange(schoolId, startDate, endDate,
                e -> classId.equals(e.getClassId()) && section.equalsIgnoreCase(e.getSection()));
    }

    /** One teacher's lessons per date within a range. */
    public List<DaySchedule> getTeacherSchedule(String schoolId, String teacherId,
            LocalDate startDate, LocalDate endDate) {
        return filterRange(schoolId, startDate, endDate, e -> teacherId.equals(e.getTeacherId()));
    }

    /**
     * Replaces one class section's timetable on ONE date. The new periods are
     * checked against the other sections of that day (teacher clashes); the
     * section's old entries are discarded. Creates the day document if the
     * date has none yet.
     */
    public DailyTimetable updateSectionForDate(String schoolId, LocalDate date, String classId, String section,
            List<TimetablePeriod> periods) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        Map<String, SchoolClass> classCache = new HashMap<>();
        validateClassSection(schoolId, classId, section, classCache);
        String label = classLabel(classId, classCache) + " Section " + section;
        validatePeriods(periods, label);
        Set<String> teacherIds = new HashSet<>();
        periods.stream().filter(p -> p.getTeacherId() != null).forEach(p -> teacherIds.add(p.getTeacherId()));
        Map<String, Staff> staffCache = validateTeachers(schoolId, teacherIds);

        AcademicYear academicYear = requireWorkingDate(schoolId, date);

        DailyTimetable doc = dailyTimetableRepository.findBySchoolIdAndDate(schoolId, date)
                .orElse(DailyTimetable.builder().schoolId(schoolId).date(date).entries(new ArrayList<>()).build());
        doc.setAcademicYear(academicYear.getName());
        if (doc.getEntries() == null) {
            doc.setEntries(new ArrayList<>());
        }

        // the section's own old entries are being replaced, so they cannot clash
        List<TimetableEntry> others = doc.getEntries().stream()
                .filter(e -> !(classId.equals(e.getClassId()) && section.equalsIgnoreCase(e.getSection())))
                .collect(Collectors.toList());

        List<String> conflicts = new ArrayList<>();
        for (TimetablePeriod period : periods) {
            if (period.getTeacherId() == null) {
                continue;
            }
            for (TimetableEntry other : others) {
                if (period.getTeacherId().equals(other.getTeacherId())
                        && other.getStartTime() != null && other.getEndTime() != null
                        && overlaps(period.getStartTime(), period.getEndTime(),
                                other.getStartTime(), other.getEndTime())) {
                    conflicts.add("Teacher " + teacherLabel(period.getTeacherId(), staffCache)
                            + " is already scheduled for '" + other.getSubject() + "' (class "
                            + classLabel(other.getClassId(), classCache) + ", section " + other.getSection()
                            + ") on " + date + " " + other.getStartTime() + "-" + other.getEndTime());
                }
            }
        }
        raiseIfConflicts(conflicts);

        others.addAll(buildEntries(classId, section, periods));
        doc.setEntries(others);
        return dailyTimetableRepository.save(doc);
    }

    /**
     * Replaces the WHOLE timetable of one date for the school: every existing
     * entry of that day is discarded and the given class sections become the
     * day's complete timetable. Creates the day document if none exists yet.
     */
    public DailyTimetable updateDay(String schoolId, LocalDate date, List<ClassSectionTimetable> sections) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Please add the timetable of at least one class section"
                    + " (to remove the whole day use DELETE instead).");
        }
        Map<String, SchoolClass> classCache = new HashMap<>();
        Set<String> seenClassSections = new HashSet<>();
        Set<String> teacherIds = new HashSet<>();
        for (int i = 0; i < sections.size(); i++) {
            ClassSectionTimetable entry = sections.get(i);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Timetable entry " + (i + 1) + " is empty — please fill it in or remove it.");
            }
            if (entry.getClassId() == null || entry.getClassId().isBlank()) {
                throw new IllegalArgumentException("Timetable entry " + (i + 1) + ": please choose a class.");
            }
            if (entry.getSection() == null || entry.getSection().isBlank()) {
                throw new IllegalArgumentException("Timetable entry " + (i + 1) + ": please choose a section.");
            }
            validateClassSection(schoolId, entry.getClassId(), entry.getSection(), classCache);
            String label = classLabel(entry.getClassId(), classCache) + " Section " + entry.getSection();
            if (!seenClassSections.add(entry.getClassId() + "::" + entry.getSection().toLowerCase())) {
                throw new IllegalArgumentException(label
                        + " was added more than once — each class section can only appear once per request.");
            }
            validatePeriods(entry.getPeriods(), label);
            entry.getPeriods().stream()
                    .filter(p -> p.getTeacherId() != null)
                    .forEach(p -> teacherIds.add(p.getTeacherId()));
        }
        Map<String, Staff> staffCache = validateTeachers(schoolId, teacherIds);
        raiseIfConflicts(findTemplateTeacherConflicts(sections, classCache, staffCache));

        AcademicYear academicYear = requireWorkingDate(schoolId, date);

        DailyTimetable doc = dailyTimetableRepository.findBySchoolIdAndDate(schoolId, date)
                .orElse(DailyTimetable.builder().schoolId(schoolId).date(date).build());
        doc.setAcademicYear(academicYear.getName());
        List<TimetableEntry> entries = new ArrayList<>();
        for (ClassSectionTimetable entry : sections) {
            entries.addAll(buildEntries(entry.getClassId(), entry.getSection(), entry.getPeriods()));
        }
        doc.setEntries(entries);
        return dailyTimetableRepository.save(doc);
    }

    /** Deletes the whole timetable of one date. */
    public void deleteDay(String schoolId, LocalDate date) {
        DailyTimetable doc = dailyTimetableRepository.findBySchoolIdAndDate(schoolId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No timetable stored for school " + schoolId + " on " + date));
        dailyTimetableRepository.delete(doc);
    }

    /** Removes one class section's entries from every day document in the range. */
    public long clearSection(String schoolId, String classId, String section,
            LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        long removed = 0;
        List<DailyTimetable> docs = dailyTimetableRepository
                .findBySchoolIdAndDateRange(schoolId, startDate, endDate);
        List<DailyTimetable> toSave = new ArrayList<>();
        List<DailyTimetable> toDelete = new ArrayList<>();
        for (DailyTimetable doc : docs) {
            if (doc.getEntries() == null) {
                continue;
            }
            int before = doc.getEntries().size();
            doc.getEntries().removeIf(
                    e -> classId.equals(e.getClassId()) && section.equalsIgnoreCase(e.getSection()));
            if (doc.getEntries().size() == before) {
                continue;
            }
            removed += before - doc.getEntries().size();
            if (doc.getEntries().isEmpty()) {
                toDelete.add(doc);
            } else {
                toSave.add(doc);
            }
        }
        dailyTimetableRepository.saveAll(toSave);
        dailyTimetableRepository.deleteAll(toDelete);
        return removed;
    }

    /** Removes a single entry from one date's document. */
    public void deleteEntry(String schoolId, LocalDate date, String entryId) {
        DailyTimetable doc = dailyTimetableRepository.findBySchoolIdAndDate(schoolId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No timetable stored for school " + schoolId + " on " + date));
        if (doc.getEntries() == null || !doc.getEntries().removeIf(e -> entryId.equals(e.getId()))) {
            throw new ResourceNotFoundException("Entry " + entryId + " not found in the timetable of " + date);
        }
        if (doc.getEntries().isEmpty()) {
            dailyTimetableRepository.delete(doc);
        } else {
            dailyTimetableRepository.save(doc);
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * The date must fall inside an academic year of the school and must not be
     * one of that year's holidays. Returns the academic year.
     */
    private AcademicYear requireWorkingDate(String schoolId, LocalDate date) {
        AcademicYear academicYear = academicYearRepository.findBySchoolId(schoolId).stream()
                .filter(y -> y.getStartDate() != null && y.getEndDate() != null
                        && !date.isBefore(y.getStartDate()) && !date.isAfter(y.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The date " + date
                        + " does not fall in any academic year of this school. "
                        + "Create the academic year (e.g. '2026-2027') with its start/end dates first."));
        if (academicYear.getHolidays() != null) {
            for (HolidayDetail detail : academicYear.getHolidays()) {
                if (detail != null && date.equals(detail.getDate())) {
                    throw new IllegalArgumentException(date + " is a holiday ('" + detail.getName()
                            + "') in academic year '" + academicYear.getName()
                            + "' — a timetable cannot be set on a holiday. "
                            + "Remove the holiday first if school runs on this date.");
                }
            }
        }
        return academicYear;
    }

    /** Builds stored entries (with fresh ids) for one class section's periods. */
    private static List<TimetableEntry> buildEntries(String classId, String section, List<TimetablePeriod> periods) {
        List<TimetableEntry> entries = new ArrayList<>();
        for (TimetablePeriod period : periods) {
            boolean isBreak = period.getType() == SlotType.BREAK;
            entries.add(TimetableEntry.builder()
                    .id(new ObjectId().toHexString())
                    .classId(classId)
                    .section(section)
                    .type(isBreak ? SlotType.BREAK : SlotType.LESSON)
                    .subject(isBreak && isBlank(period.getSubject()) ? "Break" : period.getSubject())
                    .teacherId(period.getTeacherId())
                    .startTime(period.getStartTime())
                    .endTime(period.getEndTime())
                    .build());
        }
        return entries;
    }

    private interface EntryFilter { boolean test(TimetableEntry e); }

    private List<DaySchedule> filterRange(String schoolId, LocalDate startDate, LocalDate endDate,
            EntryFilter filter) {
        validateRange(startDate, endDate);
        List<DaySchedule> result = new ArrayList<>();
        for (DailyTimetable doc : dailyTimetableRepository
                .findBySchoolIdAndDateRange(schoolId, startDate, endDate)) {
            if (doc.getEntries() == null) {
                continue;
            }
            List<TimetableEntry> entries = doc.getEntries().stream()
                    .filter(filter::test)
                    .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                    .collect(Collectors.toList());
            if (!entries.isEmpty()) {
                result.add(new DaySchedule(doc.getDate(), doc.getDate().getDayOfWeek(), entries));
            }
        }
        return result;
    }

    private static void validatePeriods(List<TimetablePeriod> periods, String label) {
        if (periods == null || periods.isEmpty()) {
            throw new IllegalArgumentException(label + ": please add at least one period.");
        }
        for (int i = 0; i < periods.size(); i++) {
            TimetablePeriod period = periods.get(i);
            String where = label + ", period " + (i + 1);
            if (period == null) {
                throw new IllegalArgumentException(where + " is empty — please fill it in or remove it.");
            }
            if (!isBlank(period.getSubject())) {
                where += " ('" + period.getSubject() + "')";
            }
            if (period.getType() == SlotType.BREAK) {
                if (period.getTeacherId() != null) {
                    throw new IllegalArgumentException(where
                            + " is a break — a break cannot have a teacher. Please remove the teacher.");
                }
            } else {
                if (isBlank(period.getSubject())) {
                    throw new IllegalArgumentException(where + ": please enter the subject.");
                }
                if (period.getTeacherId() == null) {
                    throw new IllegalArgumentException(where + ": please choose a teacher.");
                }
            }
            if (period.getStartTime() == null) {
                throw new IllegalArgumentException(where + ": please set the start time.");
            }
            if (period.getEndTime() == null) {
                throw new IllegalArgumentException(where + ": please set the end time.");
            }
            if (!period.getStartTime().isBefore(period.getEndTime())) {
                throw new IllegalArgumentException(where + ": the end time (" + period.getEndTime()
                        + ") must be after the start time (" + period.getStartTime() + ").");
            }
        }
        // All periods of one entry target the same class section, so any
        // overlap inside the entry is invalid regardless of type or teacher.
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                TimetablePeriod a = periods.get(i);
                TimetablePeriod b = periods.get(j);
                if (overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    throw new IllegalArgumentException(label + ": '" + a.getSubject() + "' (" + a.getStartTime()
                            + "-" + a.getEndTime() + ") and '" + b.getSubject() + "' (" + b.getStartTime() + "-"
                            + b.getEndTime() + ") overlap — two periods of the same section cannot run at the same time.");
                }
            }
        }
    }

    /** Same teacher at overlapping times in two different class-section entries of one request. */
    private List<String> findTemplateTeacherConflicts(List<ClassSectionTimetable> sections,
            Map<String, SchoolClass> classCache, Map<String, Staff> staffCache) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            for (int j = i + 1; j < sections.size(); j++) {
                ClassSectionTimetable a = sections.get(i);
                ClassSectionTimetable b = sections.get(j);
                for (TimetablePeriod pa : a.getPeriods()) {
                    for (TimetablePeriod pb : b.getPeriods()) {
                        if (pa.getTeacherId() != null && pa.getTeacherId().equals(pb.getTeacherId())
                                && overlaps(pa.getStartTime(), pa.getEndTime(), pb.getStartTime(), pb.getEndTime())) {
                            conflicts.add("Teacher " + teacherLabel(pa.getTeacherId(), staffCache)
                                    + " is booked in two places in this request: '" + pa.getSubject() + "' ("
                                    + describe(a, classCache) + " " + pa.getStartTime() + "-" + pa.getEndTime()
                                    + ") and '" + pb.getSubject() + "' (" + describe(b, classCache) + " "
                                    + pb.getStartTime() + "-" + pb.getEndTime() + ")");
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

    private void validateClassSection(String schoolId, String classId, String section,
            Map<String, SchoolClass> classCache) {
        SchoolClass schoolClass = classCache.computeIfAbsent(classId,
                id -> schoolClassRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id)));
        if (!schoolId.equals(schoolClass.getSchoolId())) {
            throw new IllegalArgumentException(
                    "Class '" + schoolClass.getName() + "' does not belong to this school.");
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

    /** Validates all teachers and returns them keyed by id, for name lookups in error messages. */
    private Map<String, Staff> validateTeachers(String schoolId, Set<String> teacherIds) {
        Map<String, Staff> staffById = new HashMap<>();
        staffRepository.findAllById(teacherIds).forEach(staff -> staffById.put(staff.getId(), staff));
        for (String teacherId : teacherIds) {
            Staff staff = staffById.get(teacherId);
            if (staff == null) {
                throw new ResourceNotFoundException("Teacher (staff) not found with id: " + teacherId);
            }
            if (!schoolId.equals(staff.getSchoolId())) {
                throw new IllegalArgumentException(
                        "Teacher '" + staff.getName() + "' does not belong to this school.");
            }
        }
        return staffById;
    }

    /** Class name for error messages; falls back to the id if the class cannot be resolved. */
    private String classLabel(String classId, Map<String, SchoolClass> classCache) {
        SchoolClass schoolClass = classCache.get(classId);
        if (schoolClass == null && classId != null) {
            schoolClass = schoolClassRepository.findById(classId).orElse(null);
            if (schoolClass != null) {
                classCache.put(classId, schoolClass);
            }
        }
        return schoolClass != null && schoolClass.getName() != null ? "'" + schoolClass.getName() + "'" : classId;
    }

    /** Teacher name for error messages; falls back to the id if the staff cannot be resolved. */
    private String teacherLabel(String teacherId, Map<String, Staff> staffCache) {
        Staff staff = staffCache.get(teacherId);
        if (staff == null && teacherId != null) {
            staff = staffRepository.findById(teacherId).orElse(null);
            if (staff != null) {
                staffCache.put(teacherId, staff);
            }
        }
        return staff != null && staff.getName() != null ? "'" + staff.getName() + "'" : teacherId;
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

    private static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate (" + endDate + ") must not be before startDate (" + startDate + ").");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed " + MAX_RANGE_DAYS + " days.");
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
