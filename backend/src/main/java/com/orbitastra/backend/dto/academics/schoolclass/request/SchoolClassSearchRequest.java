package com.orbitastra.backend.dto.academics.schoolclass.request;

/**
 * Everything a school can ask of one year's classes. Endpoint #28.
 *
 * <p><b>Every field is optional.</b> A bare
 * {@code GET /schools/current/academic-years/2026-2027/classes} is the first page of that year's
 * classes in {@code displayOrder} — which is the screen a school opens to see its own structure.
 *
 * <p>The filters combine with <b>AND</b>. None of them ORs within itself, unlike #30's status
 * list: there is nothing here a school would ask two values of at once.
 *
 * <p><b>The year is not a filter and is not in this record.</b> It is the parent key, it comes
 * from the URL, and it is always applied — the same reasoning that keeps {@code schoolId} out of
 * {@code SubscriptionSearchRequest}. A field here would make it look optional, and a class list
 * with no year would span every year the school has ever run.
 */
public record SchoolClassSearchRequest(

        /**
         * Only the classes in use, or only the retired ones. Example: true
         *
         * <p>The headline filter, and the one with an index behind it:
         * {@code school_year_class_active_idx} is {@code {schoolId, academicYear, active}}.
         *
         * <p><b>Not defaulted to {@code true}.</b> A list endpoint that quietly hid rows would
         * make {@code totalElements} disagree with the collection, and a school wondering where
         * a class went would have no way to ask.
         */
        Boolean active,

        /**
         * Case-insensitive, matches anywhere in the name. Example: "grade"
         *
         * <p>"grade" finds "Grade 7" and "Upper Grade"; "7" finds "Grade 7" and "XII-7". A
         * contains match rather than a prefix because a school types the part it remembers, and
         * class names are short enough that the middle is as likely as the start.
         *
         * <p><b>It cannot use an index, and that is acceptable here.</b> A case-insensitive
         * contains regex is a scan — but the query is already pinned to one school and one year
         * before the regex is applied, so it scans that year's classes and nothing else. A school
         * has tens of classes in a year, not thousands. The same shape would be wrong on a
         * student collection.
         *
         * <p>Blank is treated as absent rather than as "match everything with an empty string",
         * so a search box the user has cleared behaves like no search.
         */
        String search,

        /**
         * Only classes running under one board programme. Example: "67aa15d9dc3f7d0011111111"
         *
         * <p>Answers "which classes are on the CBSE affiliation" for a school running more than
         * one board. Not indexed, and pinned to one year first for the same reason as
         * {@code search}.
         *
         * <p>An id matching no programme gives an <b>empty page</b>, not a 404: a filter that
         * matches nothing is a legitimate answer, and this endpoint does not resolve the id — it
         * only compares it.
         */
        String affiliationProgrammeDocsId,

        /**
         * Only classes that have at least one section, or only those with none. Example: false
         *
         * <p><b>{@code false} is the setup checklist</b>, and it is the reason this filter
         * exists: a class with no section cannot hold a student, because
         * {@code StudentAcademicRecord} stores {@code sectionNo}. So "which classes are not
         * finished yet" is the question a school asks while setting a year up, and nothing else
         * on this endpoint can answer it.
         *
         * <p>Asked of the array's first element rather than a stored count, so there is no second
         * field to keep in step with the list itself.
         */
        Boolean hasSections,

        /** The same for subjects. {@code false} is "nothing is taught in this class yet". */
        Boolean hasSubjects,

        /** Zero-based. Defaults to 0. */
        Integer page,

        /** Defaults to 20, capped at 100. Above that is a 400 rather than a silent clamp. */
        Integer size,

        /**
         * {@code field,direction} — for example {@code name,desc}.
         *
         * <p>Sortable on {@code name}, {@code createdAt} and {@code updatedAt}. An allow-list, so
         * a caller cannot order by a field with nothing behind it or probe the document's shape
         * by guessing names.
         *
         * <p><b>Defaults to {@code name} ascending</b>, and that needs saying because it used to
         * default to a school-defined {@code displayOrder}, which was removed on 2026-09-11.
         * Alphabetical is not the order a school reads its classes in — "Grade 10" sorts before
         * "Grade 2", and "Nursery, LKG, UKG, 1, 2, 3" cannot be expressed at all. That is the
         * cost of dropping the field, and it is a real one.
         *
         * <p>It needs no tiebreaker: {@code name} is unique within the year, so it is a total
         * order — and {@code school_year_class_name_uniq} serves it, so the sort comes from the
         * index rather than a blocking in-memory pass.
         */
        String sort) {
}
