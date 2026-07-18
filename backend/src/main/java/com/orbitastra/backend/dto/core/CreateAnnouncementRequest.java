package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for posting an announcement. Server-owned fields ({@code id}
 * and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateAnnouncementRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "title is required")
    private String title;

    private String content;

    private String target;

    private String date;

    private String sender;
}
