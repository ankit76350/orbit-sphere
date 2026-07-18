package com.orbitastra.backend.dto.academics;

import java.time.LocalTime;

import com.orbitastra.backend.models.academics.enums.SlotType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetablePeriod {
    // Defaults to LESSON. BREAK periods must not have a teacherId and the
    // subject is an optional label (defaults to "Break").
    private SlotType type;

    private String subject;

    // Staff document id of the teacher (LESSON only)
    private String teacherId;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;
}
