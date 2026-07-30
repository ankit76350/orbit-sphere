package com.orbitastra.backend.models.undone.a_new.aid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "aid_programmes")
@CompoundIndex(name = "tenant_campus_aid_code_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'programmeCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidProgramme extends CampusScopedDocument {

    public enum AidType {
        MERIT,
        NEED,
        RTE_EWS,
        SIBLING,
        STAFF_CHILD,
        SPORTS,
        SPONSOR,
        DONOR,
        OTHER
    }

    private String programmeCode;
    private String name;
    private AidType type;
    private String fundingSourceDocsId;
    private String eligibilityExpression;
    private BigDecimal budgetAmount;
    private String currencyCode;
    private BigDecimal maximumAwardAmount;
    private LocalDate applicationOpenDate;
    private LocalDate applicationCloseDate;
    private Boolean renewable;

    @Builder.Default
    private List<String> eligibleFeeHeadDocsIds = new ArrayList<>();
}
