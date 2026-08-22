package com.orbitastra.backend.models.facilities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.facilities.enums.AssetCustodianType;
import com.orbitastra.backend.models.facilities.enums.AssetStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One individually tracked object, with a tag on it.
 *
 * <p>**The line between this and inventory is one question: does the school need to answer
 * things about *this specific one*?**
 *
 * <pre>
 * "How many microscopes do we have?"            -> inventory. A quantity of 30.
 * "When was microscope 14 last serviced?"        -> here. Thirty rows, thirty tags.
 * </pre>
 *
 * <p>`inventory` was built first and deliberately holds no individual identity: thirty
 * microscopes are a StockBalance of thirty, which is right for issuing them and useless for
 * servicing them. In practice a thing crosses over when it gets an asset tag, has a service
 * life, and is worth enough to be carried as an asset rather than written off when bought. A
 * box of chalk never crosses. A projector always does. A football goes either way and the
 * school decides.
 *
 * <p>The two are joined, not duplicated. {@code inventoryItemDocsId} says what kind of thing
 * this is, so the item master is not re-keyed here, and {@code goodsReceiptDocsId} says which
 * delivery it arrived on. Creating asset rows from an accepted GoodsReceipt of a
 * NON_CONSUMABLE item is how a register gets populated without anybody typing it twice — and it
 * closes the loop from `procurement` through `inventory` to here.
 *
 * <p>{@code custodianType} and {@code custodianDocsId} answer "who is answerable for this",
 * which has three honest answers in a school and only one of them is a person. A laptop is one
 * teacher's. Lab apparatus belongs to the science department, and holding one of four teachers
 * responsible is how nobody is. A ceiling fan belongs to the room.
 *
 * <p>{@code facilityResourceDocsId} is where it physically is, which is a different question
 * from who is answerable. A projector in the assembly hall may be the AV coordinator's
 * responsibility.
 *
 * <p>**LOST is a status on purpose.** A register whose only exits are DISPOSED and WRITTEN_OFF
 * turns every missing microscope into a disposal, and then nobody can count what the school
 * cannot find. See AssetStatus.
 *
 * <p>{@code acquisitionCost}, {@code usefulLifeYears} and {@code salvageValue} are the inputs a
 * depreciation figure is worked out from. **There is no depreciation schedule here and no
 * accumulated figure.** Straight-line depreciation from those three numbers and a date is
 * arithmetic, and a stored schedule is a table that goes stale the day somebody revalues
 * anything. The bookkeeping models were deleted on 2026-08-12, so there is nowhere to post a
 * depreciation entry to yet either; when they return, this is where the inputs are.
 *
 * <p>{@code parentAssetDocsId} is for a component that is worth tagging on its own but belongs
 * to something bigger — a lens on a microscope, a battery bank on an inverter. Null for almost
 * everything, and a school that fills it in for every keyboard has misunderstood it.
 *
 * <p>The service checks that {@code assetTag} is never reused even after disposal, that a
 * disposal carries a date and what was received, that LOST carries a note saying when it was
 * last seen, that the custodian exists in the collection its type names, and that an asset is
 * not marked IN_USE while its facility resource is DECOMMISSIONED.
 */
@Document(collection = "asset_register_items")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_asset_tag_uniq",
                def = "{'schoolId': 1, 'assetTag': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_asset_serial_idx",
                def = "{'schoolId': 1, 'serialNumber': 1}",
                partialFilter = "{'serialNumber': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_asset_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'name': 1}"),
        @CompoundIndex(
                name = "school_asset_location_idx",
                def = "{'schoolId': 1, 'facilityResourceDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_asset_custodian_idx",
                def = "{'schoolId': 1, 'custodianType': 1, 'custodianDocsId': 1}"),
        @CompoundIndex(
                name = "school_asset_item_idx",
                def = "{'schoolId': 1, 'inventoryItemDocsId': 1}",
                partialFilter = "{'inventoryItemDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_asset_warranty_idx",
                def = "{'schoolId': 1, 'warrantyUntil': 1}",
                partialFilter = "{'warrantyUntil': {'$type': 'date'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRegisterItem extends SchoolBase {

    // The number physically stuck on the object, from NumberSequence type ASSET_TAG.
    // Never reused, even after the thing is disposed of: a tag that comes back means two
    // service histories merge into one. Example: "AST-2026-00418"
    @NotBlank
    private String assetTag;

    // What it is, in words on the register. Example: "Compound microscope, 1000x"
    @NotBlank
    private String name;

    // Links to InventoryItem.id for the kind of thing this is, so the item master is not
    // re-keyed here. Null for something the store never stocked, such as a building's
    // lift. Example: "67bc1124dc3f7d0033445566"
    private String inventoryItemDocsId;

    // The manufacturer's own serial number, which is what a warranty claim and a police
    // report both need. Example: "OLY-CX23-884471"
    private String serialNumber;

    // Example: "Olympus"
    private String make;

    // Example: "CX23"
    private String model;

    // Where it is. Links to FacilityResource.id. Example: "67c31122dc3f7d0011223344"
    private String facilityResourceDocsId;

    // Which collection the custodian is in. Example: AssetCustodianType.DEPARTMENT
    private AssetCustodianType custodianType;

    // Links to the record named by custodianType — the person, department or room
    // answerable for this. Example: "67aa2211dc3f7d0011223344"
    private String custodianDocsId;

    // Links to AssetRegisterItem.id when this is a tagged component of something bigger.
    // Another row in this same collection. Null for almost everything.
    // Example: "67c31125dc3f7d0044556677"
    private String parentAssetDocsId;

    // Where it stands. Example: AssetStatus.IN_USE
    @NotNull
    @Builder.Default
    private AssetStatus status = AssetStatus.IN_USE;

    // Links to Vendor.id it was bought from. Example: "67bd1122dc3f7d0011223344"
    private String vendorDocsId;

    // Links to PurchaseOrder.id it was ordered on. Example: "67bd1126dc3f7d0055667788"
    private String purchaseOrderDocsId;

    // Links to GoodsReceipt.id it arrived on. This is what lets a register be built from
    // deliveries instead of typed by hand. Example: "67bd1128dc3f7d0077889900"
    private String goodsReceiptDocsId;

    // When the school got it. Example: 2026-08-20
    private LocalDate acquisitionDate;

    // What it cost. The starting figure any depreciation is worked out from.
    // Example: 42000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal acquisitionCost;

    // Example: "INR"
    private String currencyCode;

    // How many years it is expected to last. With the cost and the salvage value, this is
    // everything a depreciation figure needs; no schedule is stored. Example: 10
    private Integer usefulLifeYears;

    // What it is expected to be worth at the end of that life. Example: 2000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal salvageValue;

    // When the warranty runs out. Indexed, because "what is still under warranty" is the
    // question asked at the moment something breaks. Example: 2029-08-19
    private LocalDate warrantyUntil;

    // When it was disposed of, lost or written off. Example: 2033-04-02
    private LocalDate disposedOn;

    // What the school got for it, if anything. Example: 1500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal disposalProceeds;

    // Why it left the register, or when it was last seen if it is LOST. Required for
    // LOST, DISPOSED and WRITTEN_OFF.
    // Example: "Not in the lab at the March stock check. Last seen in December."
    private String disposalNote;

    // Links to DocumentRecord.id for the invoice, the warranty card, a photograph.
    // Example: ["67c31123dc3f7d0022334455"]
    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();

    // Anything worth knowing.
    // Example: "Stage clip replaced in 2028. Original was cracked on delivery."
    private String remarks;
}
