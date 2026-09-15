package com.orbitastra.backend.dto.people.organization.request;

/**
 * What #12 will filter a school's departments by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?active=} left off returns retired units as well as active ones,
 * which is the difference between "show me the whole chart" and "show me what is closed".
 *
 * <p><b>{@code tree} changes the shape of the response, not merely its contents</b> — see
 * {@link com.orbitastra.backend.dto.people.organization.response.DepartmentTreeResponse}. It is
 * the one parameter here that does.
 */
public record DepartmentSearchRequest(

        /** Units in use, or retired ones. Absent returns both. */
        Boolean active,

        /** Case-insensitive, matches anywhere in `name` OR `departmentCode`. Blank is absent. */
        String search,

        /** The children of one unit. Absent returns every depth. */
        String parentDepartmentDocsId,

        /** Units headed by one person — the "what does this person run" question. */
        String headStaffDocsId,

        /** True for top-level units only, false for nested ones only. Absent returns both. */
        Boolean topLevelOnly,

        /** Return the nesting instead of a flat page. Changes the response shape. */
        Boolean tree,

        Integer page,
        Integer size,
        String sort) {
}
