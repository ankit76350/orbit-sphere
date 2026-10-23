package com.orbitastra.backend.models.finance.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.finance.enums.FeeFrequency;
import com.orbitastra.backend.models.finance.enums.MandateStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A parent's standing permission for the school to debit fees automatically
 * through UPI AutoPay.
 *
 * <p>This is a permission, not a payment. Each debit taken under it creates its
 * own FeePayment that points back here through
 * {@code FeePayment.upiMandateDocsId}.
 *
 * <p>{@code maximumDebitAmount} is the cap the parent agreed to. The school may
 * never debit more than this in one go, whatever the invoice says, so a fee rise
 * mid-year needs a new mandate rather than a bigger debit.
 *
 * <p>The mandate is not tied to an academic year, because a parent's permission
 * usually carries across the year boundary. It sits on SchoolBase with a student
 * link instead.
 */
@Document(collection = "upi_mandates")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_upi_mandate_no_uniq",
                def = "{'schoolId': 1, 'mandateNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "provider_mandate_reference_uniq",
                def = "{'providerMandateReference': 1}",
                unique = true,
                partialFilter = "{'providerMandateReference': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_student_mandate_status_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_mandate_next_debit_idx",
                def = "{'schoolId': 1, 'status': 1, 'nextDebitDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UpiMandate extends SchoolBase {

    // School-scoped number from NumberSequence type UPI_MANDATE.
    // Example: "MND/2026/000073"
    @NotBlank
    private String mandateNo;

    // Links to Student.id the debits are for.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // Links to Guardian.id whose account is debited.
    // Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String guardianDocsId;

    // Links to PaymentGateway.id that set the mandate up.
    // Example: "67ad5566dc3f7d0055667788"
    @NotBlank
    private String paymentGatewayDocsId;

    // The UPI mandate number the provider gave back.
    // Example: "UMN7745120983RZP"
    private String providerMandateReference;

    // Parent's UPI id, kept so they can recognise the mandate.
    // Example: "priya@okhdfcbank"
    private String payerUpiHandle;

    // Most that may be debited in one go. Example: 15000.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumDebitAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How often a debit is expected. Example: FeeFrequency.MONTHLY
    @NotNull
    private FeeFrequency frequency;

    // Example: MandateStatus.ACTIVE
    @NotNull
    @Builder.Default
    private MandateStatus status = MandateStatus.PENDING_AUTHORIZATION;

    // First date a debit may be taken. Example: 2026-04-05
    @NotNull
    private LocalDate validFrom;

    // Last date a debit may be taken. Example: 2027-03-31
    @NotNull
    private LocalDate validUntil;

    // Next date a debit is due to be raised. Example: 2026-07-05
    private LocalDate nextDebitDate;

    // When the parent allowed the mandate in their UPI app.
    // Example: 2026-03-30T12:40:00Z
    private Instant authorizedAt;

    // When the last debit went through. Example: 2026-06-05T02:15:00Z
    private Instant lastDebitAt;

    // Debits taken under this mandate so far. Example: 3
    @NotNull
    @Builder.Default
    private Integer debitCount = 0;

    // Debits that failed in a row, used to decide when to stop trying.
    // Example: 0
    @NotNull
    @Builder.Default
    private Integer consecutiveFailureCount = 0;

    // Example: 2026-12-01T05:00:00Z
    private Instant cancelledAt;

    // Why the mandate was paused, cancelled or refused.
    // Example: "Parent asked for it to be stopped and will pay by hand."
    private String statusReason;
}
