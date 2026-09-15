package com.orbitastra.backend.dto.people.organization.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new org unit. Endpoint #9.
 *
 * <h2>departmentCode is given, never derived</h2>
 *
 * <p>The rule this project settled on 2026-09-12 for {@code termCode}: deriving a code from a name
 * ties two fields that do not move together. A school renaming "Academics" to "Teaching &amp;
 * Learning" must not be offered a new code for a unit twenty positions reference — the name is
 * what a person reads, the code is what a filter and an export are written against.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>No {@code active}.</b> It starts {@code true} and retiring is #11, an event with its own
 * endpoint — the way every lifecycle flag in this project works. A unit created already retired is
 * a state nothing asked for.
 *
 * <p><b>No {@code schoolId}.</b> It comes from the header through {@code CurrentSchoolResolver},
 * never from the body: a tenant a caller can send is a tenant a caller can get wrong.
 */
public record DepartmentCreateRequest(

        /** Unique within the school, and never changed afterwards. "ACADEMICS", "ADMIN". */
        @NotBlank @Size(max = 40) String departmentCode,

        /** What a person reads. Editable through #10. */
        @NotBlank @Size(max = 120) String name,

        /** Free text. */
        @Size(max = 500) String description,

        /** Another department of this school to nest under, or null for a top-level unit. */
        @Size(max = 60) String parentDepartmentDocsId,

        /**
         * The head's {@code Staff.id}, or null.
         *
         * <p>Validated to <b>exist</b>, not to be employed — during setup a school enters its org
         * chart before its employment records, and refusing this would force it to work backwards.
         */
        @Size(max = 60) String headStaffDocsId) {
}
