package com.orbitastra.backend.dto.academics;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

/**
 * Client payload for adding one subject to an existing class.
 *
 * <p>The subject name is required. A teacher may be assigned later, but when
 * {@code teacherDocsId} is supplied the service verifies that the staff
 * document belongs to the same school as the class.</p>
 *
 * <p>Persistence-only fields such as {@code id}, audit timestamps and
 * {@code schoolId} are intentionally not accepted here.</p>
 */
@Data
public class AddClassSubjectRequest {

    @NotBlank(message = "subject name is required")
    private String name;

    private String teacherDocsId;

    /** Reject legacy names and server-owned fields instead of silently ignoring them. */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unsupported subject field '" + fieldName
                + "'. Use name and teacherDocsId only.");
    }
}
