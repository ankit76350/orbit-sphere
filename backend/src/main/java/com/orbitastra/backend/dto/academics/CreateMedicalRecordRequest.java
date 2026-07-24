package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for a student medical/infirmary record. Server-owned fields
 * ({@code id} and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateMedicalRecordRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "studentDocsId is required")
    private String studentDocsId;

    private LocalDate visitDate;

    private String diagnosis;

    private List<String> medicines;

    private String doctorName;
}
