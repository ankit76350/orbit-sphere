package com.orbitastra.backend.models.undone.a_new.library;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_policies")
@CompoundIndex(name = "tenant_campus_borrower_policy_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'borrowerType':1,'policyVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryPolicy extends CampusScopedDocument {

    private String borrowerType;
    private Integer policyVersion;
    private Integer maximumOpenLoans;
    private Integer loanDays;
    private Integer renewalLimit;
    private BigDecimal dailyFine;
    private BigDecimal maximumFine;
    private String currencyCode;
    private Integer reservationHoldDays;
    private Boolean active;
}
