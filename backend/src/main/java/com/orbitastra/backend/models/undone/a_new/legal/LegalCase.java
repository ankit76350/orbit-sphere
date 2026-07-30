package com.orbitastra.backend.models.undone.a_new.legal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "legal_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_legal_owner_status_hearing_idx",
                def = "{'tenantId':1,'ownerDocsId':1,'status':1,'nextHearingDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LegalCase extends TenantScopedDocument {

    private String caseNo;
    private String externalCaseNo;
    private String caseType;
    private String jurisdiction;
    private String title;
    private String status;
    private Confidentiality confidentiality;
    private String ownerDocsId;
    private String externalCounselVendorDocsId;
    private LocalDate filedOn;
    private LocalDate nextHearingDate;
    private String legalHoldDocsId;

    @Builder.Default
    private List<String> partyReferences = new ArrayList<>();

    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();
}
