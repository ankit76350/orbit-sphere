package com.orbitastra.backend.models.undone.a_new.billing;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "stored_value_accounts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_account_no_uniq",
                def = "{'tenantId':1,'accountNo':1}", unique = true),
        @CompoundIndex(name = "tenant_owner_account_type_uniq",
                def = "{'tenantId':1,'ownerType':1,'ownerDocsId':1,'accountType':1}", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StoredValueAccount extends TenantScopedDocument {

    private String accountNo;
    private String ownerType;
    private String ownerDocsId;
    private String accountType;
    private String currencyCode;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private String status;
    private Long lastLedgerSequence;
}
