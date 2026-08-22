package com.orbitastra.backend.models.procurement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.procurement.embedded.PurchaseOrderLine;
import com.orbitastra.backend.models.procurement.enums.PurchaseOrderStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The order actually placed with one vendor.
 *
 * <p>A request is the school talking to itself. This is the school talking to somebody
 * outside it, and that is the whole difference between the two models. Once this has been
 * sent, the school is committed: the vendor will deliver and will expect to be paid.
 *
 * <p>**An ISSUED order is not editable.** Changing what it says after it has gone out means
 * the school and the vendor are holding two different pieces of paper, and the argument that
 * follows cannot be settled from either. A change means cancelling this order and raising a
 * new one, so the change has a date and a reason attached, the same rule an issued FeeInvoice
 * follows.
 *
 * <p>One vendor per order, always. A request for rice, oil and vegetables from three
 * different shops becomes three orders, because an order is a document that goes to one
 * business and gets paid to one bank account. The lines each carry
 * {@code procurementRequestDocsId} so all three still trace back to the one request, and
 * {@code procurementRequestDocsIds} at the header repeats it for a quick lookup.
 *
 * <p>{@code vendorNameSnapshot} is copied in when the order is raised. A purchase order is a
 * document that left the building, so it has to keep showing who it was sent to even after
 * the vendor's registered name changes. This is the same reason FeeInvoiceLine keeps its own
 * copy of the fee head name — and it is deliberately not the same as snapshotting a name
 * onto something still being negotiated, which only manufactures staleness.
 *
 * <p>{@code deliveryStoreDocsId} is the other half of the seam with inventory. An order that
 * does not say which store the goods are for leaves the store keeper deciding, and the goods
 * receipt then has to guess. Rice for the hostel kitchen and rice for the main store are
 * different balances.
 *
 * <p>{@code otherChargesAmount} is freight, loading, packing — the charges that appear on a
 * vendor's bill and are not on any line. Keeping them at the header rather than inventing a
 * line for them means the line totals still equal quantity times rate, which is what makes
 * checking a bill line by line possible at all.
 *
 * <p>SHORT_CLOSED is the status that matters most; see PurchaseOrderStatus. Without it, an
 * order where fifteen kilograms never arrived sits at PARTIALLY_RECEIVED forever, and
 * looks exactly like an order still on its way.
 *
 * <p>The service checks that the vendor is ACTIVE at the moment of issue, that the line
 * totals add up to the header totals, that an ISSUED order is never edited, that a
 * cancellation carries a reason, that an order is not cancelled once goods have been
 * received against it, and that the status follows the received quantities on the lines
 * rather than being set by hand.
 */
@Document(collection = "purchase_orders")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_purchase_order_no_uniq",
                def = "{'schoolId': 1, 'orderNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_purchase_order_vendor_idx",
                def = "{'schoolId': 1, 'vendorDocsId': 1, 'orderDate': -1}"),
        @CompoundIndex(
                name = "school_purchase_order_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'expectedDeliveryDate': 1}"),
        @CompoundIndex(
                name = "school_purchase_order_request_idx",
                def = "{'schoolId': 1, 'procurementRequestDocsIds': 1}"),
        @CompoundIndex(
                name = "school_purchase_order_store_idx",
                def = "{'schoolId': 1, 'deliveryStoreDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_purchase_order_date_idx",
                def = "{'schoolId': 1, 'orderDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder extends SchoolBase {

    // School-scoped number from NumberSequence type PURCHASE_ORDER. Printed on the
    // order, quoted by the vendor on their bill, and the thing everybody says on the
    // telephone. Example: "PO/2026/000241"
    @NotBlank
    private String orderNo;

    // Links to Vendor.id. Exactly one, because an order goes to one business and is
    // paid into one account. Example: "67bd1122dc3f7d0011223344"
    @NotBlank
    private String vendorDocsId;

    // The vendor's registered name copied in when the order was raised, so a reprint
    // years later still shows who it was sent to.
    // Example: "Shree Traders Private Limited"
    @NotBlank
    private String vendorNameSnapshot;

    // Links to every ProcurementRequest.id this order answers, so a department can see
    // what happened to what it asked for. Empty for a direct purchase with no request
    // behind it, which should be rare and is worth reporting on.
    // Example: ["67bd1124dc3f7d0033445566"]
    @Builder.Default
    private List<String> procurementRequestDocsIds = new ArrayList<>();

    // The day the order is dated. Example: 2026-08-16
    @NotNull
    private LocalDate orderDate;

    // When the goods are due. What a "nothing has arrived" report is built on.
    // Example: 2026-08-20
    private LocalDate expectedDeliveryDate;

    // Links to InventoryStore.id the goods are for. The order says where they go, so
    // the goods receipt does not have to guess. Example: "67bc1125dc3f7d0044556677"
    @NotBlank
    private String deliveryStoreDocsId;

    // Where the vendor should actually deliver, when that needs saying in words.
    // Example: "Hostel kitchen, rear gate on Ranade Road. Ask for the cook."
    private String deliveryAddressNote;

    // What is being ordered. At least one line, because an order for nothing is not an
    // order.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    // The line amounts before tax, added up. Example: 12300.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotalAmount;

    // Discount given at the whole-order level, over and above any line discounts.
    // Example: 0.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal discountAmount;

    // The line taxes added up. Example: 615.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxAmount;

    // Freight, loading and packing. Kept at the header so the line totals stay equal to
    // quantity times rate, which is what makes checking a bill line by line possible.
    // Example: 250.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal otherChargesAmount;

    // What the school has committed to pay. Example: 13165.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Copied from the vendor when the order was raised, because the terms agreed on
    // this order are what its bills are due under, even if the vendor's standing terms
    // change later. Example: 30
    @Builder.Default
    private Integer paymentTermDays = 0;

    // Where the order has got to. Example: PurchaseOrderStatus.ISSUED
    @NotNull
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    // Links to Staff.id of whoever approved it inside the school.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // When they did. Example: 2026-08-16T05:00:00Z
    private Instant approvedAt;

    // When it was actually sent to the vendor. This is the moment the order stops being
    // editable. Example: 2026-08-16T05:30:00Z
    private Instant issuedAt;

    // Links to Staff.id of whoever sent it. Example: "67aa15d9dc3f7d0044444444"
    private String issuedByStaffDocsId;

    // Conditions printed on the order: what happens if goods are late, who pays return
    // freight, whether part delivery is accepted.
    // Example: "Part delivery accepted. Damaged goods returned at supplier's cost."
    private String termsAndConditions;

    // Why it was called off. Required for CANCELLED.
    // Example: "Vendor could not supply before the term started."
    private String cancellationReason;

    // Why the school stopped waiting for the rest. Required for SHORT_CLOSED, because
    // this is the field that turns a missing fifteen kilograms from an open question
    // into a decision somebody made.
    // Example: "185 of 200 kg received. Vendor credited the balance; not chasing."
    private String shortCloseReason;

    // Links to DocumentRecord.id for the signed order as it went out.
    // Example: "67bd1125dc3f7d0044556677"
    private String orderDocumentDocsId;

    // Anything worth knowing.
    // Example: "Rate held from the July order after negotiation."
    private String remarks;
}
