package com.orbitastra.backend.models.undone.a_new.procurement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "vendors")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_vendor_code_uniq",
                def = "{'tenantId':1,'vendorCode':1}", unique = true),
        @CompoundIndex(name = "tenant_vendor_tax_lookup_uniq",
                def = "{'tenantId':1,'taxIdLookupHash':1}", unique = true,
                partialFilter = "{'taxIdLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_vendor_status_expiry_idx",
                def = "{'tenantId':1,'status':1,'complianceValidUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor extends TenantScopedDocument {

    public enum VendorStatus {
        DRAFT,
        UNDER_REVIEW,
        APPROVED,
        SUSPENDED,
        BLOCKED,
        OFFBOARDED
    }

    private String vendorCode;
    private String legalName;
    private String displayName;
    private String encryptedTaxId;
    private String taxIdLookupHash;
    private String encryptedBankDetails;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private VendorStatus status;
    private LocalDate complianceValidUntil;
    private BigDecimalRating performanceRating;

    @Builder.Default
    private List<String> categoryCodes = new ArrayList<>();

    @Builder.Default
    private List<String> complianceDocumentDocsIds = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BigDecimalRating {
        private Integer score;
        private Integer maximum;
        private LocalDate assessedOn;
    }
}
