package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "data_disclosures")
@CompoundIndex(name = "tenant_subject_disclosed_time_idx",
        def = "{'tenantId':1,'subjectDocsId':1,'disclosedAt':-1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DataDisclosure extends TenantScopedDocument {

    private PersonType subjectType;
    private String subjectDocsId;
    private String recipientType;
    private String recipientName;
    private String recipientCountryCode;
    private String purpose;
    private String lawfulBasis;
    private String authorizedByDocsId;
    private Instant disclosedAt;
    private String secureTransferMethod;
    private String evidenceDocumentDocsId;

    @Builder.Default
    private List<String> dataCategoryKeys = new ArrayList<>();
}
