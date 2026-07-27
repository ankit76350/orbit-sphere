package com.orbitastra.backend.models.undone.mess;

@Document(collection = "mess_kitchen_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessKitchenTransaction extends SchoolBase {

    @Indexed
    private String kitchenItemDocsId;

    private KitchenTransactionType transactionType;

    /**
     * Quantity moved.
     */
    private BigDecimal quantity;

    /**
     * Quantity before transaction.
     */
    private BigDecimal quantityBefore;

    /**
     * Quantity after transaction.
     */
    private BigDecimal quantityAfter;

    /**
     * Invoice
     * Vendor Bill
     * Waste Entry
     */
    private String referenceNumber;

    private String remarks;
}
