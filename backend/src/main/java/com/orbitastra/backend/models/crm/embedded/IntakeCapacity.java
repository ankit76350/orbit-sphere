package com.orbitastra.backend.models.new_new.crm.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seat configuration for one class, embedded in an AdmissionCycle.
 *
 * <p>It has no separate collection identity. Offered, enrolled, and waitlisted
 * totals are calculated from AdmissionApplication records using
 * {@code admissionCycleDocsId}, {@code appliedClassDocsId}, and {@code status}.
 * They are not stored here, avoiding mutable counter drift.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeCapacity {

    // Links to the class/grade document. Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String classDocsId;

    // Example: 60
    @NotNull
    private Integer totalSeats;

    // Example: 10
    @NotNull
    @Builder.Default
    private Integer reservedSeats = 0;
}
