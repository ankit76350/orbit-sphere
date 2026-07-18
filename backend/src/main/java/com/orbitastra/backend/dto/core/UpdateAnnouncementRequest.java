package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Full-replacement payload for an announcement (PUT). The update replaces every
 * field, so {@code schoolId} and {@code title} are required just as on create.
 * Server-owned fields ({@code id}, audit timestamps) are not accepted here.
 */
@Data
public class UpdateAnnouncementRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "title is required")
    private String title;

    private String content;

    private String target;

    private String date;

    private String sender;
}
