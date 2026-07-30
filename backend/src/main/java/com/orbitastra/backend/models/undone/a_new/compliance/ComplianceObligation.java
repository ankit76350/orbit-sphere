package com.orbitastra.backend.models.undone.a_new.compliance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "compliance_obligations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_obligation_code_version_uniq",
                def = "{'tenantId':1,'obligationCode':1,'obligationVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_obligation_status_due_idx",
                def = "{'tenantId':1,'status':1,'nextDueDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceObligation extends CampusScopedDocument {

    private String obligationCode;
    private Integer obligationVersion;
    private String authorityCode;
    private String title;
    private String description;
    private String jurisdictionCode;
    private String programmeDocsId;
    private String recurrenceExpression;
    private LocalDate nextDueDate;
    private Integer reminderDays;
    private String ownerDocsId;
    private String status;

    @Builder.Default
    private List<String> evidenceRequirementCodes = new ArrayList<>();
}
