package com.orbitastra.backend.models.undone.a_new.studentlife;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "trip_plans")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_trip_no_uniq",
                def = "{'tenantId':1,'tripNo':1}", unique = true),
        @CompoundIndex(name = "tenant_trip_state_departure_idx",
                def = "{'tenantId':1,'state':1,'departAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlan extends AcademicScopedDocument {

    private String tripNo;
    private String title;
    private String tripType;
    private String destination;
    private Instant departAt;
    private Instant returnAt;
    private String leadStaffDocsId;
    private String riskAssessmentDocumentDocsId;
    private String itineraryDocumentDocsId;
    private ApprovalState state;
    private Integer capacity;
    private BigDecimal participantFee;
    private String currencyCode;
    private String transportProviderDocsId;
    private String emergencyContact;

    @Builder.Default
    private List<String> eligibleClassNodeDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> staffDocsIds = new ArrayList<>();
}
