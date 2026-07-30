package com.orbitastra.backend.models.undone.a_new.facilities;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "asset_register_items")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_asset_no_uniq",
                def = "{'tenantId':1,'assetNo':1}", unique = true),
        @CompoundIndex(name = "tenant_serial_lookup_uniq",
                def = "{'tenantId':1,'serialNo':1}", unique = true,
                partialFilter = "{'serialNo':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_facility_asset_status_idx",
                def = "{'tenantId':1,'facilityResourceDocsId':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRegisterItem extends CampusScopedDocument {

    public enum AssetStatus {
        IN_STOCK,
        IN_USE,
        UNDER_MAINTENANCE,
        LOST,
        DISPOSED
    }

    private String assetNo;
    private String categoryCode;
    private String name;
    private String serialNo;
    private String facilityResourceDocsId;
    private String custodianType;
    private String custodianDocsId;
    private String vendorDocsId;
    private String purchaseOrderDocsId;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionCost;
    private String currencyCode;
    private LocalDate warrantyUntil;
    private Integer usefulLifeMonths;
    private BigDecimal residualValue;
    private String depreciationMethod;
    private AssetStatus status;
}
