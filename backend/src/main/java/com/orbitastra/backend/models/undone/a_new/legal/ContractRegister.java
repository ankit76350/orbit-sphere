package com.orbitastra.backend.models.undone.a_new.legal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "contract_register")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_contract_no_uniq",
                def = "{'tenantId':1,'contractNo':1}", unique = true),
        @CompoundIndex(name = "tenant_contract_state_expiry_idx",
                def = "{'tenantId':1,'state':1,'endDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContractRegister extends TenantScopedDocument {

    private String contractNo;
    private String contractType;
    private String counterpartyType;
    private String counterpartyDocsId;
    private String title;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate noticeDate;
    private BigDecimal contractValue;
    private String currencyCode;
    private String ownerDocsId;
    private String signedDocumentDocsId;
    private String parentContractDocsId;

    @Builder.Default
    private List<String> renewalReminderDays = new ArrayList<>();
}
