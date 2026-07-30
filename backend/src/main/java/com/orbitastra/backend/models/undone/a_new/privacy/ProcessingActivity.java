package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "processing_activities")
@CompoundIndex(name = "tenant_processing_activity_key_uniq",
        def = "{'tenantId':1,'activityKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingActivity extends TenantScopedDocument {

    private String activityKey;
    private String name;
    private String ownerDocsId;
    private String purpose;
    private String lawfulBasis;
    private Confidentiality highestClassification;
    private String sourceSystem;
    private String retentionRuleDocsId;
    private Boolean childrenData;
    private Boolean automatedDecisionMaking;
    private Boolean crossBorderTransfer;
    private String transferMechanism;
    private LocalDate nextReviewDate;

    @Builder.Default
    private List<String> dataCategoryKeys = new ArrayList<>();

    @Builder.Default
    private List<String> dataSubjectTypes = new ArrayList<>();

    @Builder.Default
    private List<String> processorDocsIds = new ArrayList<>();
}
