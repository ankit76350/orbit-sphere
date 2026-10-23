package com.orbitastra.backend.models.finance.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.finance.enums.GatewayConnectionStatus;
import com.orbitastra.backend.models.finance.enums.GatewayProvider;
import com.orbitastra.backend.models.common.enums.PaymentMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A school's online payment setup with one provider, such as Razorpay.
 *
 * <p>This is the connection, not a payment. It holds which provider is used, what
 * the provider charges, and where the money is paid out to.
 *
 * <p>No API key or secret is ever stored here. {@code credentialVaultKey} and
 * {@code webhookSecretVaultKey} hold only the names used to look the real secrets
 * up in a vault, so a leaked database dump cannot be used to take payments in the
 * school's name.
 *
 * <p>{@code mdrFeePercent} is what the provider keeps out of each payment. It is
 * stored so an expected payout can be worked out and compared against what
 * actually reached the bank in a SettlementBatch.
 */
@Document(collection = "payment_gateways")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_gateway_provider_uniq",
                def = "{'schoolId': 1, 'provider': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_gateway_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'primaryGateway': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGateway extends SchoolBase {

    // Example: GatewayProvider.RAZORPAY
    @NotNull
    private GatewayProvider provider;

    // Name shown to staff. Example: "Razorpay - Fee Collection"
    @NotBlank
    private String displayName;

    // Example: GatewayConnectionStatus.CONNECTED
    @NotNull
    @Builder.Default
    private GatewayConnectionStatus status = GatewayConnectionStatus.NOT_CONNECTED;

    // The school's merchant id with the provider, safe to show.
    // Example: "acc_R7pMerchant01"
    private String merchantReference;

    // Name used to look the API key up in the vault. Never the key itself.
    // Example: "school/67aa15d9/razorpay/api-key"
    private String credentialVaultKey;

    // Name used to look the webhook secret up in the vault.
    // Example: "school/67aa15d9/razorpay/webhook-secret"
    private String webhookSecretVaultKey;

    // Share the provider keeps out of each payment. Example: 1.80
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal mdrFeePercent;

    // Fixed charge the provider adds per payment. Example: 2.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal perTransactionFee;

    // Tax the provider adds on its own charge. Example: 18.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal feeTaxRatePercent;

    // Ways of paying this gateway is set up to accept.
    @Builder.Default
    private List<PaymentMode> supportedModes = new ArrayList<>();

    // Whether this gateway can set up UPI AutoPay mandates. Example: true
    @NotNull
    @Builder.Default
    private Boolean mandateSupported = false;

    // Bank account the provider pays the collections out to.
    // Example: "67ad8899dc3f7d0088990011"
    private String bankAccountDocsId;

    // Whether a signed test callback has been received. Example: true
    @NotNull
    @Builder.Default
    private Boolean webhookVerified = false;

    // The gateway used by default when a parent pays online. Only one per school
    // should have this set, which the service checks. Example: true
    @NotNull
    @Builder.Default
    private Boolean primaryGateway = false;

    // Days the provider normally takes to pay out. Example: 2
    private Integer settlementCycleDays;

    // Example: 2026-03-15T08:00:00Z
    private Instant connectedAt;

    // Why the gateway was blocked or switched off.
    // Example: "Turned off while the school changes its bank account."
    private String statusReason;
}
