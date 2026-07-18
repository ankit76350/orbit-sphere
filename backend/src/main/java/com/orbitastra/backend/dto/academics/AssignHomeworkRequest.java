package com.orbitastra.backend.dto.academics;

import java.util.List;

import com.orbitastra.backend.models.academics.enums.AssignmentScope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Typed replacement for the raw map previously accepted by the assign endpoint.
 * {@code studentAssignments} is required only for a SPECIFIC-scope assignment.
 */
@Data
public class AssignHomeworkRequest {

    @NotNull(message = "assignmentScope is required")
    private AssignmentScope assignmentScope;

    @Valid
    private List<StudentAssignmentRequest> studentAssignments;
}
