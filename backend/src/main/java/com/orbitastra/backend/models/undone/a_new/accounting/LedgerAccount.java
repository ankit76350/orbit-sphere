package com.orbitastra.backend.models.undone.a_new.accounting;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ledger_accounts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_account_code_uniq",
                def = "{'tenantId':1,'legalEntityDocsId':1,'accountCode':1}", unique = true),
        @CompoundIndex(name = "tenant_parent_account_idx",
                def = "{'tenantId':1,'parentAccountDocsId':1,'sortOrder':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount extends TenantScopedDocument {

    public enum AccountType {
        ASSET,
        LIABILITY,
        EQUITY,
        REVENUE,
        EXPENSE
    }

    private String legalEntityDocsId;
    private String parentAccountDocsId;
    private String accountCode;
    private String name;
    private AccountType accountType;
    private String currencyCode;
    private Boolean postingAllowed;
    private Boolean reconciliationRequired;
    private Integer sortOrder;
}
