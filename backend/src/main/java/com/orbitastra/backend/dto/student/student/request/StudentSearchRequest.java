package com.orbitastra.backend.dto.student.student.request;

import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

/**
 * What #4 filters the roll by.
 *
 * <p><b>Every field is optional, and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?placed=} left off returns children with and without a class,
 * which is a different question from "show me who has not been placed yet".
 *
 * <h2>The class filter the plan asked for is not here, and that is not an oversight</h2>
 *
 * <p>The plan's filter set is {@code status}, {@code academicYear} + {@code classDocsId} +
 * {@code sectionNo}, {@code name} and {@code admissionNo}. <b>The three in the middle are not
 * fields on a student at all</b> — they live on {@code StudentAcademicRecord}, and #14 is what
 * writes one: there is no repository, no endpoint and no {@code student_academic_records} document
 * anywhere in the database yet.
 *
 * <p>Taking them now would mean a filter that silently matches nothing, which is worse than one
 * that is documented as absent — a parameter that looks like it works and returns an empty page
 * sends somebody looking for the bug in their own code. They arrive with #14, and the plan already
 * says how: page on {@code student_academic_records} first, then read {@code students} by id,
 * because the narrowing filter lives on the first collection and paging the second gives pages
 * that shrink after filtering.
 *
 * <p><b>{@code placed} is what stands in for them today</b>, and it is a real question rather than
 * a consolation: at the start of a term the thing a school needs is the list of children nobody
 * has put in a section yet.
 */
public record StudentSearchRequest(

        /**
         * Case-insensitive, matches anywhere in {@code fullName} OR {@code admissionNo}.
         *
         * <p>Both, because which one somebody has in front of them is not this endpoint's to
         * decide — an office looks a child up by name and a fee receipt by number.
         */
        String search,

        /** One of the seven. Absent returns every status, including children who have left. */
        StudentStatus status,

        /** Absent returns every gender. */
        Gender gender,

        /**
         * True for children who have been put in a class, false for those who have not.
         *
         * <p><b>The start-of-term question.</b> A child admitted in January has no academic record
         * until somebody places them, and this is the only way to ask who is still waiting.
         */
        Boolean placed,

        /**
         * True for children who came through admissions, false for those typed in by hand.
         *
         * <p>Asks whether {@code admissionApplicationDocsId} is there at all. The honest answer to
         * "which of these children did the CRM give us" — a transfer, a walk-in and the roll a
         * school already had when it started using the product are all false.
         */
        Boolean fromAdmissions,

        Integer page,
        Integer size,
        String sort) {
}
