package com.orbitastra.backend.models.undone.feeengine;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.feeengine.enums.ConcessionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A maker-checker request to grant a {@link ConcessionPolicy} to a student.
 * Raised by one user, approved/rejected by another before the discount is
 * applied to the student's invoices.
 */
@Document(collection = "concession_requests")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConcessionRequest extends SchoolBase {

    private String studentId;

    // References ConcessionPolicy.id (null when it is a one-off custom concession).
    private String policyId;

    private BigDecimal amount;

    private String requestedBy; // references Staff.id / User.id

    @Builder.Default
    private ConcessionStatus status = ConcessionStatus.PENDING;

    private String reviewedBy; // references Staff.id / User.id

    private String remarks;
}
