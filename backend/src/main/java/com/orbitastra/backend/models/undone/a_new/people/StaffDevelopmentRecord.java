package com.orbitastra.backend.models.undone.a_new.people;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "staff_development_records")
@CompoundIndex(name = "tenant_staff_development_date_idx",
        def = "{'tenantId':1,'staffDocsId':1,'state':1,'completedOn':-1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDevelopmentRecord extends TenantScopedDocument {

    private String staffDocsId;
    private String developmentType;
    private String title;
    private String provider;
    private LocalDate plannedOn;
    private LocalDate completedOn;
    private BigDecimal hours;
    private BigDecimal cost;
    private String currencyCode;
    private ApprovalState state;
    private String impactEvaluation;
    private String certificateDocumentDocsId;

    @Builder.Default
    private List<String> skillCodes = new ArrayList<>();
}
