package com.orbitastra.backend.models.undone.feeengine;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGateway extends BaseDocument {

    private GatewayProvider provider;

    @Builder.Default
    private Boolean connected = false;

    // Merchant Discount Rate charged by the provider, as a percentage.
    private BigDecimal mdrFeePercent;

    // Enabled payment modes on this gateway (e.g. "UPI", "Card", "NetBanking").
    @Builder.Default
    private List<String> supportedModes = new java.util.ArrayList<>();
}
