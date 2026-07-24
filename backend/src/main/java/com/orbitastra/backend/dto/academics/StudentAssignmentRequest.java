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

    @NotBlank(message = "studentDocsId is required")
    private String studentDocsId;

    private String customInstructions;

    public Homework.StudentAssignment toModel() {
        return Homework.StudentAssignment.builder()
                .studentDocsId(studentDocsId)
                .customInstructions(customInstructions)
                .build();
    }

    public static List<Homework.StudentAssignment> toModels(List<StudentAssignmentRequest> requests) {
        if (requests == null) return null;
        return requests.stream().map(StudentAssignmentRequest::toModel).collect(Collectors.toList());
    }
}
