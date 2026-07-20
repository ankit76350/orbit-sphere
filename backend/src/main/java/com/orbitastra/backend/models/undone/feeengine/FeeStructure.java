package com.orbitastra.backend.models.undone.feeengine;

import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A per-grade bundle of {@link FeeHead}s for an academic year. Applying a
 * structure to a grade is what generates the students' {@code finance.FeeInvoice}
 * documents — the structure is the template, the invoice is the output.
 */
@Document(collection = "fee_structures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    // References AcademicYear.name (unique per school), e.g. "2026-2027".
    @Indexed
    private String academicYear;

    private String grade;

    // References FeeHead.id for each head included in this structure.
    @Builder.Default
    private List<String> feeHeadIds = new java.util.ArrayList<>();
}
