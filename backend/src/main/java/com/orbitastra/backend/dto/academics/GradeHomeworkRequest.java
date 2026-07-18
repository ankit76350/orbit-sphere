package com.orbitastra.backend.dto.academics;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Typed replacement for the raw map previously accepted by the grade endpoint.
 */
@Data
public class GradeHomeworkRequest {

    @Min(value = 0, message = "obtainedMarks cannot be negative")
    private Integer obtainedMarks;

    private String feedback;
}
