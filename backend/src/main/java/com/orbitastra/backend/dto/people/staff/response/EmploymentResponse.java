package com.orbitastra.backend.dto.people.staff.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.people.staff.EmploymentRecord;
import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.people.staff.enums.EmploymentType;

/**
 * One period of employment, as #16 returns it.
 *
 * <p><b>The record that was closed comes back beside the one that was opened</b>, because a
 * promotion is one event with two halves and a caller that saw only the new row would have to read
 * again to know what happened to the old one.
 *
 * <p><b>Nothing here is resolved to a name.</b> {@code positionDocsId} and {@code managerDocsId}
 * come back raw, the same call every write in {@code people} makes: one place decides how a seat
 * and a person are presented, and a write's response is not it.
 */
public record EmploymentResponse(
        String employmentDocsId,
        String staffDocsId,
        String positionDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String managerDocsId,

        EmploymentStatus status,
        EmploymentType employmentType,
        LocalDate effectiveFrom,

        /** Null while this record is current. Set on the one this write closed. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate effectiveUntil,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate probationUntil,

        /** Why the record is at its current status. Five of the seven statuses require one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String statusReason,

        /** Set when a terminal status ended the employment. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String separationReason,

        Boolean current) {

    public static EmploymentResponse fromRecord(EmploymentRecord record) {
        return new EmploymentResponse(
                record.getId(),
                record.getStaffDocsId(),
                record.getPositionDocsId(),
                record.getManagerDocsId(),
                record.getStatus(),
                record.getEmploymentType(),
                record.getEffectiveFrom(),
                record.getEffectiveUntil(),
                record.getProbationUntil(),
                record.getStatusReason(),
                record.getSeparationReason(),
                record.getCurrent());
    }
}
