package com.orbitastra.backend.models.undone.feeengine;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A named discount scheme (sibling waiver, scholarship, staff-child, RTE, ...).
 * The existing {@code finance.FeeInvoice.discount} is just a per-invoice amount;
 * this master defines the reusable rule that a {@link ConcessionRequest} applies.
 */
@Document(collection = "concession_policies")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConcessionPolicy extends SchoolDocs {

    private String name;

    // Percentage discount granted (0-100). A null percent implies a custom/amount-based concession.
    private BigDecimal percent;

    private String criteria;

    @Builder.Default
    private Boolean active = true;
}
