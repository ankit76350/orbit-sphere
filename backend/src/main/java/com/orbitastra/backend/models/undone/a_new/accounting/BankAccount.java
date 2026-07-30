package com.orbitastra.backend.models.undone.a_new.accounting;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "bank_accounts")
@CompoundIndex(name = "tenant_bank_lookup_uniq",
        def = "{'tenantId':1,'accountNumberLookupHash':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount extends TenantScopedDocument {

    private String legalEntityDocsId;
    private String ledgerAccountDocsId;
    private String bankName;
    private String branchName;
    private String encryptedAccountNumber;
    private String accountNumberLookupHash;
    private String maskedAccountNumber;
    private String routingCode;
    private String currencyCode;
    private Boolean active;
}
