package com.orbitastra.backend.services.academics.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;

import lombok.RequiredArgsConstructor;

/**
 * The timetable module's helper — the rules MongoDB cannot express about a day's periods.
 *
 * <p><b>Its own class rather than a section of {@link AcademicsHelper} or {@link GradingHelper}.</b>
 * Those take terms and bands; these take periods, and a helper validating three unrelated documents
 * would be a folder rather than a class.
 *
 * <p><b>Each check stands on its own and calls nothing else here</b>, per the service code-writing
 * rules. The two exceptions decide nothing: {@code sameSection} compares two entries and
 * {@code overlaps} compares two windows.
 *
 * <p><b>Every method takes {@link TimetableEntry}, the stored type</b>, rather than the request —
 * the same shape {@link GradingHelper} settled on. #1 builds its documents first and then validates
 * exactly what it will save, so the single-entry writes that follow can re-run these checks against
 * a stored day merged with the one entry they are changing.
 *
 * <p><b>Overlap is why this class is worth unit-testing.</b> A day that validates but leaves one
 * teacher in two rooms is invisible over HTTP and obvious in a table of cases — the argument that
 * put {@code GradingHelper.resolveBand} here rather than inline, where eight mutations were each
 * caught by a different assertion.
 */
@Component
@RequiredArgsConstructor
public class TimetableHelper {

    /** Whether two periods belong to the same section of the same class. */
    private static boolean sameSection(TimetableEntry a, TimetableEntry b) {
        //! CASE-FOLDED, since 2026-09-17. It compared exactly until then, while the section was
        //! RESOLVED against the class case-insensitively and the period-code key folded case too
        //! - three readings of one field. A day carrying "A" and "a" therefore passed the overlap
        //! check as two sections when the class holds one, and 10-A was given two periods at
        //! 09:30. Measured, not theorised: the request answered 201.
        return a.getClassDocsId().equals(b.getClassDocsId())
                && a.getSectionNo().equalsIgnoreCase(b.getSectionNo());
    }

    /**
     * Whether two periods share any minute.
     *
     * <p><b>End-exclusive, so touching periods do NOT overlap.</b> 09:00 to 09:45 beside 09:45 to
     * 10:30 is a normal school day, not a clash. That is the opposite call from a grade band, whose
     * bounds are inclusive at both ends and whose neighbours therefore must not touch — and the
     * difference is real: a band covers the value 90, while a period does not occupy the instant it
     * ends.
     */
    private static boolean overlaps(TimetableEntry a, TimetableEntry b) {
        return a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
    }

    /**
     * A period ends after it starts.
     *
     * <p><b>Equal times are refused</b>, unlike a grade band where equal bounds are legal. A band
     * covering exactly 100 is odd but meaningful; a period from 09:00 to 09:00 is nothing
     * happening, and every overlap check below would silently pass it.
     *
     * <p>Checked before every other rule here: an inverted period compared for overlap reports a
     * clash, which is true and says the wrong thing about which period is wrong.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateTimes(List<TimetableEntry> entries) {
        for (TimetableEntry entry : entries) {
            if (!entry.getStartTime().isBefore(entry.getEndTime())) {
                throw ApiException.badRequest("INVALID_PERIOD_TIMES",
                        "Period '" + entry.getPeriodCode() + "' of section "
                                + entry.getSectionNo() + " runs from " + entry.getStartTime()
                                + " to " + entry.getEndTime()
                                + ", which is not a length of time.");
            }
        }
    }

    /**
     * A lesson carries a subject and a teacher; anything else may carry a teacher but no subject.
     *
     * <p>Bean validation cannot express "required unless a sibling field says otherwise", which is
     * why {@code TimetableEntryRequest} leaves both optional. The rule lives here, where
     * {@code slotType} is visible — the same split {@link GradingHelper#validateBandBounds} makes
     * for a band's bounds.
     *
     * <h2>A teacher on a break is allowed — changed 2026-09-17</h2>
     *
     * <p>This refused a teacher on any non-lesson until then, on the grounds that dropping the
     * field silently "would make a caller believe somebody was supervising lunch". <b>That had it
     * backwards.</b> Somebody <i>does</i> supervise lunch, somebody runs the assembly, somebody
     * takes the activity — and a school that records who is the one able to answer for it. The
     * field was being refused for the exact case it is most useful in.
     *
     * <p><b>A subject is still refused</b>, and that half was always right: a break has nothing
     * being taught in it, so a {@code subjectCode} there is either a mistake or a lesson wearing
     * the wrong slot type.
     *
     * <p><b>A teacher named on a break still counts against them.</b>
     * {@link #validateNoTeacherOverlap} looks at every entry carrying a teacher whatever its slot
     * type, so somebody supervising lunch cannot also be teaching period 4 — which is the point of
     * recording it.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateSlotFields(List<TimetableEntry> entries) {
        for (TimetableEntry entry : entries) {
            boolean lesson = entry.getSlotType() == TimetableSlotType.LESSON;
            boolean hasSubject = entry.getSubjectCode() != null;
            boolean hasTeacher = entry.getTeacherDocsId() != null;

            if (lesson && !(hasSubject && hasTeacher)) {
                throw ApiException.badRequest("SLOT_FIELDS_REQUIRED",
                        "Period '" + entry.getPeriodCode() + "' of section " + entry.getSectionNo()
                                + " is a LESSON, so it needs "
                                + (hasSubject ? "a teacherDocsId."
                                        : hasTeacher ? "a subjectCode."
                                                : "both a subjectCode and a teacherDocsId."));
            }

            //! THE SUBJECT ONLY. A teacher is welcome on a break, an assembly or an activity -
            //! somebody supervises it, and recording who is what makes their day add up.
            if (!lesson && hasSubject) {
                throw ApiException.badRequest("SLOT_FIELDS_NOT_ALLOWED",
                        "Period '" + entry.getPeriodCode() + "' of section " + entry.getSectionNo()
                                + " is a " + entry.getSlotType() + ", which teaches nothing — so "
                                + "it cannot carry a subjectCode. A teacherDocsId is fine: "
                                + "somebody supervises it. Use slotLabel for what it is called.");
            }
        }
    }

    /**
     * No section names one period code twice in a day.
     *
     * <p>Case-folded: a day holding both {@code P3} and {@code p3} for one section is a data-entry
     * mistake in every school. <b>Scoped per section</b>, because every section has its own P03 and
     * that is normal.
     *
     * <p>There is no index behind this — a period is two levels inside one document — so this check
     * is the only enforcement, which is the opposite situation from a term code, where the check
     * exists to turn a duplicate-key 500 into a readable 409.
     *
     * Used by:
     * - createTimetables()
     */
    public void validatePeriodCodesUnique(List<TimetableEntry> entries) {
        //! A LIST as the key, not a joined string: sectionNo is free text, so any delimiter could
        //! appear inside it and two different sections could collide on one key.
        Set<List<String>> seen = new HashSet<>();

        for (TimetableEntry entry : entries) {
            List<String> code = List.of(
                    entry.getClassDocsId(),
                    entry.getSectionNo().toUpperCase(Locale.ROOT),
                    entry.getPeriodCode().trim().toUpperCase(Locale.ROOT));

            if (!seen.add(code)) {
                throw ApiException.conflict("PERIOD_CODE_TAKEN",
                        "Section " + entry.getSectionNo() + " names period '"
                                + entry.getPeriodCode() + "' more than once in this day. A period "
                                + "code identifies one slot on a printed timetable, so two of them "
                                + "describe no day at all.");
            }
        }
    }

    /**
     * One section is in one place at a time.
     *
     * <p><b>Refused, never warned.</b> A section with two periods at 09:00 is thirty children who
     * cannot be in both, and no school means it — the opposite call from grading's gap, where a
     * hole in the scale is reported and left to the school to judge.
     *
     * <p>Compared within each section rather than across the whole day: two sections sharing a time
     * is the normal case, not a clash.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateNoSectionOverlap(List<TimetableEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                TimetableEntry a = entries.get(i);
                TimetableEntry b = entries.get(j);

                if (sameSection(a, b) && overlaps(a, b)) {
                    throw ApiException.conflict("SECTION_PERIOD_OVERLAP",
                            "Section " + a.getSectionNo() + " has '" + a.getPeriodCode() + "' ("
                                    + a.getStartTime() + " to " + a.getEndTime() + ") and '"
                                    + b.getPeriodCode() + "' (" + b.getStartTime() + " to "
                                    + b.getEndTime() + ") at the same time. A section cannot be in "
                                    + "two places at once.");
                }
            }
        }
    }

    /**
     * One teacher is in one place at a time.
     *
     * <p><b>The clash a school notices last and feels first.</b> Two sections can share a time
     * legitimately; the same teacher in both cannot, and nothing about the document makes that
     * visible — the two entries sit far apart in a list of four hundred.
     *
     * <p>Only entries naming a teacher are compared, so breaks and assemblies are skipped rather
     * than colliding with one another on a null.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateNoTeacherOverlap(List<TimetableEntry> entries) {
        List<TimetableEntry> taught = new ArrayList<>();
        for (TimetableEntry entry : entries) {
            if (entry.getTeacherDocsId() != null) {
                taught.add(entry);
            }
        }

        for (int i = 0; i < taught.size(); i++) {
            for (int j = i + 1; j < taught.size(); j++) {
                TimetableEntry a = taught.get(i);
                TimetableEntry b = taught.get(j);

                if (a.getTeacherDocsId().equals(b.getTeacherDocsId()) && overlaps(a, b)) {
                    throw ApiException.conflict("TEACHER_PERIOD_OVERLAP",
                            "One teacher is given section " + a.getSectionNo() + " '"
                                    + a.getPeriodCode() + "' (" + a.getStartTime() + " to "
                                    + a.getEndTime() + ") and section " + b.getSectionNo() + " '"
                                    + b.getPeriodCode() + "' (" + b.getStartTime() + " to "
                                    + b.getEndTime() + ") at the same time.");
                }
            }
        }
    }

    /**
     * Two sections are not sent to one room at once.
     *
     * <p>Only entries naming a room are compared — most name none, because in most Indian schools a
     * section has one classroom all day and the timetable moves teachers, not children.
     *
     * <p><b>This checks the timetable against itself and nothing else.</b> A room booked for the
     * same hour through {@code ResourceBooking} is a clash this cannot see; whose job that is has
     * not been settled, and the plan's open item 3 says so rather than leaving it implied.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateNoRoomOverlap(List<TimetableEntry> entries) {
        List<TimetableEntry> roomed = new ArrayList<>();
        for (TimetableEntry entry : entries) {
            if (entry.getFacilityResourceDocsId() != null) {
                roomed.add(entry);
            }
        }

        for (int i = 0; i < roomed.size(); i++) {
            for (int j = i + 1; j < roomed.size(); j++) {
                TimetableEntry a = roomed.get(i);
                TimetableEntry b = roomed.get(j);

                if (a.getFacilityResourceDocsId().equals(b.getFacilityResourceDocsId())
                        && overlaps(a, b)) {

                    throw ApiException.conflict("ROOM_PERIOD_OVERLAP",
                            "Sections " + a.getSectionNo() + " and " + b.getSectionNo()
                                    + " are both sent to the same room at " + a.getStartTime()
                                    + ". Thirty children end up standing in a corridor, and nobody "
                                    + "finds out until they do.");
                }
            }
        }
    }

    /**
     * The section named by a period exists and is still taught in.
     *
     * <p><b>A retired section is not a place children can be sent next week.</b> That is the
     * opposite call from a retired grading scheme, which must keep resolving report cards already
     * issued — nothing is being reprinted here, the day has not happened yet.
     *
     * <p>Matched case-insensitively: a school that wrote "a" on one screen and "A" on another
     * means the same section, and refusing that would be a rule about typing rather than about
     * timetables.
     *
     * <p><b>It returns the class's own spelling of the section</b>, which the caller stores in
     * place of whatever was sent. A section is its {@code sectionNo} — the identity an
     * {@code AttendanceSession} and every later read carry — so storing "a" on a day whose class
     * calls it "A" leaves two spellings of one section in the database and a lookup by either
     * finding half the periods.
     *
     * @return the section's {@code sectionNo} exactly as the class stores it
     *
     * Used by:
     * - createTimetables()
     */
    public String requireActiveSection(SchoolClass schoolClass, String sectionNo) {
        ClassSection found = null;

        if (schoolClass.getSections() != null) {
            for (ClassSection one : schoolClass.getSections()) {
                if (one.getSectionNo() != null && one.getSectionNo().equalsIgnoreCase(sectionNo)) {
                    found = one;
                    break;
                }
            }
        }

        if (found == null || !Boolean.TRUE.equals(found.getActive())) {
            throw ApiException.conflict("SECTION_NOT_IN_CLASS",
                    "Section '" + sectionNo + "' is not an active section of '"
                            + schoolClass.getName() + "'.");
        }
        return found.getSectionNo();
    }

    /**
     * The subject named by a lesson is one that section actually studies.
     *
     * <h2>A subject belongs to the class or to one section, and the difference is the rule</h2>
     *
     * <p>A {@link ClassSubject} created with <b>no {@code sectionNo}</b> is class-wide: every
     * section of that class takes it, and Maths is the usual example. One created <b>with a
     * {@code sectionNo}</b> belongs to that section alone — 10-C does German, 10-A does not.
     *
     * <p>So a section studies a subject when the stored row is class-wide <b>or</b> names that
     * section. This is the same reading {@code GET /classes/{id}/subjects?sectionNo=} answers with,
     * and it has to be: a timetable built from that list must not then be refused by this check.
     *
     * <p><b>Getting it wrong in either direction is a real failure.</b> Matching only the section's
     * own subjects would refuse Maths for every section in the school; ignoring {@code sectionNo}
     * altogether would let 10-A be timetabled for German it does not take, and nobody would notice
     * until a child sat an exam in it.
     *
     * <p><b>Retired subjects do not count</b>, for the same reason a retired section does not.
     *
     * <p><b>Nothing is thrown for a period that names no subject.</b> A break has none, and
     * whether it is allowed to is {@link #validateSlotFields}'s question rather than this one.
     *
     * Used by:
     * - createTimetables()
     */
    public void validateSubjectForSection(SchoolClass schoolClass, String subjectCode,
            String sectionNo) {

        if (subjectCode == null) {
            return;
        }

        if (studies(schoolClass.getSubjects(), subjectCode, sectionNo) == null) {
            throw ApiException.conflict("SUBJECT_NOT_IN_SECTION",
                    "Section " + sectionNo + " of '" + schoolClass.getName()
                            + "' does not study '" + subjectCode + "'. A subject belongs to the "
                            + "whole class only when it was created without a sectionNo; one "
                            + "created with a sectionNo belongs to that section alone.");
        }
    }

    /**
     * What that subject is called, for a period being read back.
     *
     * <h2>Why this is a helper and not four lines inside the service</h2>
     *
     * <p>The rule here is the folder rule's exception: single-use logic normally stays inline, and
     * this method's {@code Used by:} names one caller. <b>What it would cost to inline is a second
     * copy of the class-wide rule</b> — a subject with no {@code sectionNo} belongs to every
     * section, one with a {@code sectionNo} to that section alone — living in the service beside
     * the copy {@link #validateSubjectForSection} enforces. Two copies of one rule drift, and the
     * one that drifts silently is the read: a period would be shown a name for a subject the write
     * path says that section does not study.
     *
     * <p><b>It reads, it does not refuse.</b> #1 already established that the section studies this
     * subject; a code that no longer matches means the subject was retired or renamed <i>after</i>
     * the day was written, and a read of last Tuesday must not fail because of that. The name is
     * simply absent, and the code is still in the response.
     *
     * @return the subject's name, or null when no active subject of that section carries the code
     *
     * Used by:
     * - getTimetable()
     */
    public String subjectNameFor(SchoolClass schoolClass, String subjectCode, String sectionNo) {
        if (schoolClass == null || subjectCode == null) {
            return null;
        }

        ClassSubject subject = studies(schoolClass.getSubjects(), subjectCode, sectionNo);
        return subject == null ? null : subject.getName();
    }

    /**
     * The stored subject row a section studies under that code, or null.
     *
     * <p><b>Private, and that is the point.</b> {@link #validateSubjectForSection} and
     * {@link #subjectNameFor} are its only callers — the write path asking whether the section
     * studies this, the read path asking what it is called — and the service rule here is that a
     * helper never calls another helper. A private static is how logic is shared inside this class
     * without breaking it, and it is what keeps the class-wide rule in one place for both.
     */
    private static ClassSubject studies(List<ClassSubject> subjects, String subjectCode,
            String sectionNo) {

        if (subjects == null) {
            return null;
        }

        for (ClassSubject subject : subjects) {
            if (!Boolean.TRUE.equals(subject.getActive())) {
                continue;
            }
            if (subject.getSubjectCode() == null
                    || !subject.getSubjectCode().equalsIgnoreCase(subjectCode)) {
                continue;
            }

            //! CLASS-WIDE, so every section takes it. Blank is treated as absent: a stored empty
            //! string means the same thing as no section and must not behave differently.
            boolean classWide = subject.getSectionNo() == null || subject.getSectionNo().isBlank();
            if (classWide || subject.getSectionNo().equalsIgnoreCase(sectionNo)) {
                return subject;
            }
        }
        return null;
    }
}
