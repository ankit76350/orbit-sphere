package com.orbitastra.backend.models.undone.feeengine;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure extends BaseDocument {

    // References AcademicYear.name (unique per school), e.g. "2026-2027".
    @Indexed
    private String academicYear;

    private String grade;

    // References FeeHead.id for each head included in this structure.
    @Builder.Default
    private List<String> feeHeadIds = new java.util.ArrayList<>();
}
