package com.orbitastra.backend.dto.core.academicyear;

import com.orbitastra.backend.models.core.enums.HolidayType;

import jakarta.validation.constraints.Size;

/**
 * Edits one reason on one closed day. Endpoint #22.
 *
 * <p><b>The date is not on this record and cannot be changed.</b> It is the key in the URL, and a
 * PATCH that could move the thing it is addressing would be its own puzzle: the request would
 * name one date and mean another. Moving a holiday is a delete followed by a post, which also
 * leaves both dates visible in the log rather than one silent edit.
 *
 * <p><b>Which reason is being edited comes from the {@code ?type=} query parameter, not from
 * here.</b> A day can hold several — a Sunday that is also Holi — so the URL alone no longer
 * identifies one. The parameter may be omitted when the day has exactly one reason, and is
 * required when it has more; guessing on the caller's behalf would mean editing the wrong entry
 * half the time.
 *
 * <p>That is why retyping is {@code newType} rather than {@code type}. The selector and the new
 * value are two different things, and one field named {@code type} appearing in both the query
 * string and the body — meaning "which one" in one place and "make it this" in the other — is a
 * bug waiting for whoever reads it next.
 *
 * <p>Partial, like the other PATCHes here: null leaves a field alone, {@code ""} clears an
 * optional one. {@code name} and the type cannot be cleared — both are required on the model,
 * and a reason with no name is not a state worth supporting.
 */
public record HolidayUpdateRequest(

        /** Example: "Diwali (day 2)". Cannot be cleared. */
        @Size(max = 120) String name,

        /** Example: "School closed for the second day". Send "" to remove. */
        @Size(max = 300) String description,

        /**
         * What this reason should become. Example: HolidayType.GOVERNMENT_HOLIDAY.
         *
         * <p>Refused if the day already carries another reason of that type — one day cannot
         * hold the same reason twice.
         */
        HolidayType newType) {

    public boolean isEmpty() {
        return name == null && description == null && newType == null;
    }
}
