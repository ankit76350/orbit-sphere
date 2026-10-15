package com.orbitastra.backend.dto.academics.attendance.request;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What attendance #2 needs to mark children in one register.
 *
 * <p><b>Each mark is saved on its own</b>: sending a child who already has a mark corrects it, and
 * children not sent are left as they are — so two teachers can each mark half the class.
 *
 * <p><b>No version.</b> Each child's mark is its own document; a version per row would make
 * marking a class of forty unusable. Two people correcting the same child at once — the last one
 * wins.
 */
public record AttendanceMarksRequest(

        /** The marks to save — at least one. */
        @NotNull @Size(max = 300) List<@Valid @NotNull Mark> marks) {

    /** One child's mark. */
    public record Mark(

            /** The child. Must have been in the section on the register's date. */
            @NotBlank @Size(max = 60) String studentDocsId,

            /** PRESENT, LATE, AUTHORISED_ABSENCE or UNAUTHORISED_ABSENCE. */
            @NotNull AttendanceStatus status,

            /** <b>Required unless PRESENT</b>, e.g. "MEDICAL" or "BUS". A blank one is missing. */
            @Size(max = 100) String reason,

            /** When the child arrived — useful with LATE. */
            Instant arrivalAt) {
    }
}
