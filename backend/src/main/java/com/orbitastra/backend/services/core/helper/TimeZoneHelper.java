package com.orbitastra.backend.services.core.helper;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.exception.ConflictException;

/**
 * Checks that a time zone is one the JVM actually knows.
 *
 * <p>Rejected on the way in rather than stored, because a bad zone is not discovered until
 * something tries to work out which calendar date a timestamp falls on — and by then attendance
 * has been taken. {@code School.defaultTimeZone} is read by attendance, timetables, holidays,
 * transport trips and fee due dates, so a wrong value there is wrong in every one of them at
 * once.
 *
 * <p>A 409 rather than a 400: {@code "Asia/Pune"} is a perfectly well-formed string and a
 * reasonable guess, so telling the caller their request was malformed sends them looking for a
 * syntax error that is not there. The answer is that no such zone exists.
 *
 * <p>{@code @NotBlank} on the request cannot do this job. Nothing in Jakarta validation knows
 * the IANA zone list, and a regex over it would be wrong within a year — zones are added and
 * renamed. Asking the JVM is the only check that stays correct.
 *
 * <p>Its own class because {@code PATCH /schools/current/localization} needs exactly the same
 * check, and a copy of it there would drift.
 */
@Component
public class TimeZoneHelper {

    /**
     * Trims and returns the zone id, or throws if the JVM does not know it.
     *
     * @throws ConflictException code {@code TIME_ZONE_INVALID}
     */
    public String validateAndNormalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ConflictException("TIME_ZONE_REQUIRED", "A time zone is required.");
        }
        String candidate = raw.trim();
        if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
            throw new ConflictException("TIME_ZONE_INVALID",
                    "'" + candidate + "' is not a known IANA time zone id.");
        }
        return candidate;
    }
}
