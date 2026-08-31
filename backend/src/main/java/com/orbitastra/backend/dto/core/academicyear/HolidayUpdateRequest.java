package com.orbitastra.backend.dto.core.academicyear;

import com.orbitastra.backend.models.core.enums.HolidayType;

import jakarta.validation.constraints.Size;

/**
 * Edits one holiday, found by its date. Endpoint #22.
 *
 * <p><b>The date is not on this record and cannot be changed.</b> It is the key in the URL, and
 * a PATCH that could move the thing it is addressing would be its own puzzle: the request would
 * name one date and mean another. Moving a holiday is a delete followed by a post, which also
 * makes the two dates visible in the log rather than one silent edit.
 *
 * <p>Partial, like the other PATCHes here: null leaves a field alone, {@code ""} clears an
 * optional one. {@code name} and {@code type} cannot be cleared — both are required on the model,
 * and a holiday with no name is not a state worth supporting.
 */
public record HolidayUpdateRequest(

        /** Example: "Diwali (day 2)". Cannot be cleared. */
        @Size(max = 120) String name,

        /** Example: "School closed for the second day". Send "" to remove. */
        @Size(max = 300) String description,

        /** Example: HolidayType.FESTIVAL. Cannot be cleared. */
        HolidayType type) {

    public boolean isEmpty() {
        return name == null && description == null && type == null;
    }
}
