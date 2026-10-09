package com.orbitastra.backend.dto.student.academicrecord.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #14 needs to put a child in a class and a section.
 *
 * <p><b>The year is the path</b>, so it is not here. Everything else that identifies the placement
 * is: who, which class, which section.
 *
 * <h2>Three ids, and each is checked against the one before it</h2>
 *
 * <p>A class belongs to a year and a section belongs to a class, so the three cannot be validated
 * independently. {@code CLASS_NOT_IN_YEAR} and {@code SECTION_NOT_IN_CLASS} exist because
 * "not found" would be true and useless — the caller pasted a real id from the wrong place, and
 * needs to be told which wrong place.
 *
 * <h2>No {@code status}, and that is a change from the plan</h2>
 *
 * <p>The plan's table says {@code ACTIVE} or {@code PLANNED}, defaulting to {@code ACTIVE}.
 * <b>{@code PLANNED} was removed from {@code AcademicRecordStatus} on 2026-10-09</b>, along with
 * {@code WITHDRAWN}; what remains is {@code ACTIVE}, {@code COMPLETED}, {@code TRANSFERRED} and
 * {@code CANCELLED}. The last three are terminal states that #16 and #17 put a record into, and a
 * caller creating a {@code COMPLETED} placement is describing something that never happened.
 *
 * <p>So a record created here is always {@code ACTIVE}, and the field is not offered rather than
 * offered with one legal value.
 */
public record StudentAcademicRecordCreateRequest(

        /**
         * The child being placed.
         *
         * <p>Must be this school's, and must not have left: {@code WITHDRAWN},
         * {@code TRANSFERRED} and {@code GRADUATED} are refused, because a placement is a
         * statement about where somebody sits <i>now</i>.
         */
        @NotBlank @Size(max = 60) String studentDocsId,

        /**
         * The class, which must belong to the year in the path.
         *
         * <p>A class id is globally unique, so one pasted from another year is a <i>real</i> id —
         * and silently accepting it would put a child in a class that no longer runs.
         */
        @NotBlank @Size(max = 60) String classDocsId,

        /**
         * The section within that class.
         *
         * <p><b>Always {@code sectionNo}</b>, which is what the field is called on
         * {@code ClassSection} — it is a short identifier like "A", not a number and not a name.
         */
        @NotBlank @Size(max = 20) String sectionNo,

        /**
         * The child's roll number in that section. <b>Optional, and not generated.</b>
         *
         * <p>The plan says it is generated when absent, and it is not, because the generator
         * cannot do it yet: {@code NumberSequenceService.next} allocates against
         * {@code GLOBAL_SCOPE} while {@code school_year_class_section_active_roll_uniq} scopes the
         * value to {@code {year, class, section}}. One roll-number counter per school is not what
         * that index describes. The plan calls this out as a prerequisite and offers
         * caller-supplied as the alternative; this is that alternative.
         *
         * <p><b>Absent means no roll number</b>, which is a real state rather than a gap: the
         * unique index is partial on {@code rollNo: {$type: 'string'}}, so any number of records
         * may have none, and #21's roster is specified to sort the ones without by name.
         */
        @Size(max = 20) String rollNo,

        /**
         * The day the placement starts. <b>Defaults to today.</b>
         *
         * <p><b>A future date is allowed</b>, and that is the point of taking the field: a child
         * admitted in January into a June year should carry the June date, not the day somebody
         * typed it in.
         *
         * <p>Such a record is {@code ACTIVE} from the moment it is written. That is the reading
         * open item 4 recommends — <b>{@code ACTIVE} means "this is the placement", not "this
         * placement is in effect today"</b> — and it is the one #21's roster will have to follow.
         */
        LocalDate effectiveFrom) {
}
