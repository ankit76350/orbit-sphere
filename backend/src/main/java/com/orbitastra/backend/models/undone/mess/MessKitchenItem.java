package com.orbitastra.backend.models.undone.mess;

@Document(collection = "mess_kitchen_items")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessKitchenItem extends SchoolBase {

    @Indexed(unique = true)
    private String name;

    /**
     * Rice
     * Flour
     * Oil
     * Sugar
     */
    private KitchenUnit unit;

    /**
     * Current stock.
     */
    private BigDecimal currentQuantity;

    /**
     * Alert threshold.
     */
    private BigDecimal minimumQuantity;

    /**
     * Store Room A
     * Freezer
     */
    private String storageLocation;

    @Builder.Default
    private Boolean active = true;
}