package com.orbitastra.backend.common.time;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.School;

import lombok.RequiredArgsConstructor;

/**
 * Which zone a school's dates are read in, taken from the school rather than from the request.
 *
 * <p><b>Why the request does not carry it.</b> The zone is chosen once, when the school is
 * created, and stored on the school. Asking every date-bearing request to send it back would be
 * asking the caller to repeat something the server already knows — friction with no information
 * in it, and a new way for a request to be wrong. A body that named the zone was built and then
 * removed on 2026-09-10 for exactly that reason.
 *
 * <p><b>So the rule is one line long: the zone always comes from the school.</b> Every date this
 * system renders or compares goes through {@link Dates} with a zone, and that zone comes from
 * here or from a {@link School} already in hand. There is no third source, and nothing has to
 * agree with anything.
 *
 * <h2>Two ways in, because the two surfaces know the school differently</h2>
 *
 * <ul>
 * <li><b>{@link #current()}</b> — the school surface. No school is named in the URL, so it is
 *     resolved from the tenant header the same way every other {@code /schools/current} read
 *     resolves it.</li>
 * <li><b>{@link #of(School)}</b> — the platform surface, and anywhere a school has already been
 *     read. The school is named in the URL there, and the service has it in hand; going back to
 *     the resolver would be a second lookup for something already loaded.</li>
 * </ul>
 *
 * <p>Both return the same thing for the same school. The split is about where the school comes
 * from, not about what the zone means.
 *
 * <h2>It cannot be null, and that is the enum's doing</h2>
 *
 * <p>{@code School.defaultTimeZone} is {@code @NotNull} and typed as {@link SchoolTimeZone}, so
 * there is no unset case to handle and no fallback to pick. {@link Dates} still accepts a null
 * zone, but only for the platform surface's own dates — a plan's selling window belongs to no
 * school, and UTC is the honest answer there.
 */
@Component
@RequiredArgsConstructor
public class SchoolZone {

    private final CurrentSchoolResolver currentSchool;

    /**
     * The zone of the school this request is acting as. School surface only.
     *
     * <p>Resolves the school to read one field off it, which is the same cost every
     * {@code /schools/current} endpoint already pays. Where a service has already resolved the
     * school for its own work, use {@link #of(School)} rather than calling this again.
     */
    public SchoolTimeZone current() {
        return currentSchool.require().getDefaultTimeZone();
    }

    /**
     * The zone of a school already in hand.
     *
     * <p>An instance method rather than static so both forms read the same at the call site, and
     * so a caller that has the bean can use either without thinking about which is which.
     */
    public SchoolTimeZone of(School school) {
        return school.getDefaultTimeZone();
    }
}
