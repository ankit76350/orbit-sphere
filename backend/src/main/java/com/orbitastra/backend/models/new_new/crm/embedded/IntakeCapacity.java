package com.orbitastra.backend.models.new_new.crm.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeCapacity {

    // Example: "67aa15d9dc3f7d0033333333"
    private String classDocsId;

    // Example: 60
    private Integer totalSeats;

    // Example: 10
    @Builder.Default
    private Integer reservedSeats = 0;

    // Example: 15
    @Builder.Default
    private Integer offeredSeats = 0;

    // Example: 32
    @Builder.Default
    private Integer enrolledSeats = 0;

    // Example: 8
    @Builder.Default
    private Integer waitlistedApplicants = 0;
}
