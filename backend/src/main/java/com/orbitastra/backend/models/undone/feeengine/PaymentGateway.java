package com.orbitastra.backend.models.undone.feeengine;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.feeengine.enums.GatewayProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A school's online payment-gateway integration config. Distinct from a payment
 * itself ({@code finance.FeePayment}) and from {@code PaymentMode.ONLINE} — this
 * stores the connection state and commercials of the provider.
 */
@Document(collection = "payment_gateways")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGateway {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private GatewayProvider provider;

    @Builder.Default
    private Boolean connected = false;

    // Merchant Discount Rate charged by the provider, as a percentage.
    private BigDecimal mdrFeePercent;

    // Enabled payment modes on this gateway (e.g. "UPI", "Card", "NetBanking").
    @Builder.Default
    private List<String> supportedModes = new java.util.ArrayList<>();
}
