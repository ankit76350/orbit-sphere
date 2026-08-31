package com.orbitastra.backend.models.core.embedded;

import com.orbitastra.backend.models.core.enums.HolidayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One reason the school is closed on a particular day.
 *
 * <p>Sits inside a {@link HolidayDetail}, which owns the date. A day can carry several of these:
 * a Sunday that is also Holi is genuinely two reasons, and each has its own name and type.
 *
 * <p>Keeping them separate rather than collapsing to one label matters for reporting. "How many
 * festivals did we close for" and "was the weekly off honoured that week" are different
 * questions, and a single row saying "Weekly Off / Holi" answers neither.
 *
 * <p>There is deliberately no date here. The date belongs to the day, not to each reason on it —
 * storing it on both would be two fields that can disagree.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayEvent {

    // Example: "Diwali"
    @NotBlank
    private String name;

    // Example: "School closed for the Diwali festival"
    private String description;

    // Example: HolidayType.FESTIVAL
    @NotNull
    private HolidayType type;
}
