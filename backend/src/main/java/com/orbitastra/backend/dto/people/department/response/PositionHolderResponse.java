package com.orbitastra.backend.dto.people.department.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.staff.EmploymentRecord;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.people.staff.enums.EmploymentType;

/**
 * One person who currently holds a seat, as #53 names them.
 *
 * <h2>The person, and what they do here — and nothing else about them</h2>
 *
 * <p>The same boundary {@link StaffSummaryResponse} draws, for the same reason. A {@code Staff}
 * document carries a date of birth, two addresses and an emergency contact; a page asking "who is
 * in this seat" needs a name, a number to look them up by, and the terms of the posting. Returning
 * the record would leak the most sensitive data this product holds through an endpoint nobody
 * would think to check — and the module plan calls authorization the open item that matters most
 * here, which does not exist yet.
 *
 * <p><b>{@code staffDocsId} is on the row so the page can link to #8</b>, which is the endpoint
 * that answers the fuller question and the one that will carry the permission check when there is
 * one.
 *
 * <h2>Both ids are here on purpose</h2>
 *
 * <p>{@code staffDocsId} addresses the person and {@code employmentDocsId} addresses the posting.
 * #18 edits a record and is addressed by the second, because a person has several and the URL has
 * to say which; #2 edits a person and is addressed by the first. A row carrying only one of them
 * makes the wrong edit reachable from this page.
 */
public record PositionHolderResponse(
        String staffDocsId,
        String employmentDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String employeeNo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String fullName,

        EmploymentStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        EmploymentType employmentType,

        LocalDate effectiveFrom,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate probationUntil,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String managerDocsId,

        /**
         * Present only when the employment record names somebody this school has no staff row
         * for. See {@link #of} — the row is returned regardless.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note) {

    /** What is said about a holder whose staff record could not be read. */
    private static final String ORPHANED =
            "This employment record names a staff id that no longer resolves in this school. "
                    + "The posting is real; the person it points at is missing.";

    /**
     * One holder, from the record and the person it names.
     *
     * <p><b>{@code staff} may be null, and the row is still returned.</b> An employment record
     * whose {@code staffDocsId} resolves to nothing is a broken reference, and hiding it would
     * make a seat look less filled than it is — the count and the list would then disagree, which
     * is the one thing a page like this must not do. It is marked instead.
     */
    public static PositionHolderResponse of(EmploymentRecord record, Staff staff) {
        return new PositionHolderResponse(
                record.getStaffDocsId(),
                record.getId(),
                staff == null ? null : staff.getEmployeeNo(),
                staff == null ? null : staff.getFullName(),
                record.getStatus(),
                record.getEmploymentType(),
                record.getEffectiveFrom(),
                record.getProbationUntil(),
                record.getManagerDocsId(),
                staff == null ? ORPHANED : null);
    }
}
