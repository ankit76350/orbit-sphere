package com.orbitastra.backend.dto.core.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The school's language and time zone. Endpoint #8.
 *
 * <p>A PATCH: send either field or both. Neither can be cleared — both are {@code @NotBlank} on
 * the model, and a school with no time zone cannot work out what day anything happened on.
 *
 * <p><b>{@code defaultTimeZone} is the most dangerous field in this package.</b> Every
 * {@code Instant} in the database is UTC, so changing the zone rewrites nothing — and that is
 * exactly the problem. It silently reinterprets every school-local decision already made:
 *
 * <ul>
 * <li>which calendar date an attendance record falls on</li>
 * <li>whether a holiday covers a given day</li>
 * <li>when a timetable period starts</li>
 * <li>which day a transport trip ran</li>
 * </ul>
 *
 * <p>A school moving {@code Asia/Kolkata} to {@code Asia/Dubai} mid-year has an attendance
 * register that shifts under it, with no error anywhere. So the service refuses the change while
 * an academic year is in progress, and {@code confirmTimeZoneChange} makes the caller say out
 * loud that they mean it.
 */
public record SchoolLocalizationRequest(

        /** IETF language tag. Example: "en-IN", "hi-IN" */
        @Pattern(regexp = "^$|^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$",
                message = "must be an IETF language tag such as en-IN")
        @Size(max = 35) String defaultLocale,

        /** IANA zone id, checked against the JVM's zone set. Example: "Asia/Kolkata" */
        @Size(max = 64) String defaultTimeZone,

        /**
         * Must be true to change the time zone. Ignored when only the locale is being changed.
         *
         * <p>A deliberate speed bump rather than decoration: this is the one field here whose
         * effects are invisible at the moment of the change and show up weeks later in the
         * attendance register.
         */
        Boolean confirmTimeZoneChange) {

    public boolean isEmpty() {
        return defaultLocale == null && defaultTimeZone == null;
    }
}
