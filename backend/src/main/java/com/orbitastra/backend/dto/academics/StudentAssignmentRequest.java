package com.orbitastra.backend.dto.academics;

import java.util.List;
import java.util.stream.Collectors;

import com.orbitastra.backend.models.academics.Homework;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * A per-student homework assignment as supplied by the client. Only the two
 * fields a teacher sets are exposed — submission text, grades, status and
 * timestamps are managed server-side and cannot be injected here.
 */
@Data
public class StudentAssignmentRequest {

    @NotBlank(message = "studentId is required")
    private String studentId;

    private String customInstructions;

    public Homework.StudentAssignment toModel() {
        return Homework.StudentAssignment.builder()
                .studentId(studentId)
                .customInstructions(customInstructions)
                .build();
    }

    public static List<Homework.StudentAssignment> toModels(List<StudentAssignmentRequest> requests) {
        if (requests == null) return null;
        return requests.stream().map(StudentAssignmentRequest::toModel).collect(Collectors.toList());
    }
}
