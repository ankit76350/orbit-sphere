package com.orbitastra.backend.dto.people.staff.request;

import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;

/**
 * What #7 filters a school's people by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?hasEmail=} left off returns people with and without one, which
 * is the difference between "show me everybody" and "show me who is missing an address".
 *
 * <h2>The four filters the plan asked for are not here, and that is not an oversight</h2>
 *
 * <p>The plan's filters are {@code ?employed=}, {@code ?departmentDocsId=},
 * {@code ?positionDocsId=} and {@code ?employmentType=} — the ones a teacher picker actually
 * wants. <b>Every one of them lives on {@code EmploymentRecord}</b>, and #16 is what writes one:
 * there is no repository, no endpoint and, as of 2026-09-15, no {@code employment_records}
 * collection in the database at all.
 *
 * <p>Accepting them now would mean a parameter that silently matches nothing — a filter that
 * looks like it works and returns an empty page is worse than one that is documented as absent.
 * They arrive with #16, and the domain plan's open item 3 already settled how: page on
 * {@code employment_records}, then read {@code staff} by id, <b>because every narrowing filter
 * lives on the first collection and paging the second gives pages that shrink after filtering.</b>
 *
 * <p>What is here is everything answerable from {@code staff} alone.
 */
public record StaffSearchRequest(

        /**
         * Case-insensitive, matches anywhere in {@code fullName} OR {@code employeeNo}.
         *
         * <p>Both, because which one a person remembers is not this endpoint's to decide — an
         * office looks somebody up by name and a payroll run looks them up by number.
         */
        String search,

        /** One of the three. Absent returns every gender. */
        Gender gender,

        /**
         * One country, from the closed set. Absent returns every nationality.
         *
         * <p><b>Bound by Spring, so an unknown code is a 400 before the query runs</b> rather
         * than an empty page that looks like "nobody is Indian". It stopped being a free string
         * on 2026-09-15, which also removed the case-folding this used to need.
         */
        CountryCode nationalityCode,

        /**
         * True for people who have an email address, false for those who do not.
         *
         * <p>Absent returns both. <b>"Who are we missing contact details for" is a real question
         * a school asks</b> at the start of a term, and it is the only way to ask it.
         */
        Boolean hasEmail,

        /** The same, for a phone number. */
        Boolean hasPhone,

        Integer page,
        Integer size,
        String sort) {
}
