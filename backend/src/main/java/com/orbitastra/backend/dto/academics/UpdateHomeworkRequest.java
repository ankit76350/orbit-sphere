package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;

import com.orbitastra.backend.models.academics.enums.AssignmentScope;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Partial-update payload for a homework's definition (PATCH). All fields
 * optional; only non-null fields are applied. Per-student submissions/grades are
 * changed through the submit/grade endpoints, not here.
 */
@Data
public class UpdateHomeworkRequest {

    private String schoolId;

    private String classId;

    private String subject;

    private String title;

    private String instructions;

    private LocalDate dueDate;

    private AssignmentScope assignmentScope;

    @Min(value = 0, message = "maxMarks cannot be negative")
    private Integer maxMarks;

    private String teacherId;
}
