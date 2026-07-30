package com.orbitastra.backend.models.undone.a_new.privacy;

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

@Document(collection = "data_processors")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_processor_code_uniq",
                def = "{'tenantId':1,'processorCode':1}", unique = true),
        @CompoundIndex(name = "tenant_processor_risk_review_idx",
                def = "{'tenantId':1,'riskRating':1,'nextReviewDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DataProcessor extends TenantScopedDocument {

    private String processorCode;
    private String vendorDocsId;
    private String legalName;
    private String serviceDescription;
    private String processingCountryCode;
    private String hostingRegion;
    private String riskRating;
    private LocalDate agreementValidUntil;
    private LocalDate nextReviewDate;
    private String dataProcessingAgreementDocsId;

    @Builder.Default
    private List<String> subprocessors = new ArrayList<>();

    @Builder.Default
    private List<String> certifications = new ArrayList<>();
}
