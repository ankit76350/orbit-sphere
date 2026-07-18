package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Partial-update payload for a medical record (PATCH). All fields optional; only
 * non-null fields are applied.
 */
@Data
public class UpdateMedicalRecordRequest {

    private String schoolId;

    private String studentId;

    private LocalDate visitDate;

    private String diagnosis;

    private List<String> medicines;

    private String doctorName;
}
