package com.orbitastra.backend.models.undone.a_new.dismissal;

import java.time.Instant;
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

@Document(collection = "pickup_authorizations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_pickup_authorization_no_uniq",
                def = "{'tenantId':1,'authorizationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_pickup_validity_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PickupAuthorization extends CampusScopedDocument {

    private String authorizationNo;
    private String studentDocsId;
    private String authorizedPersonDocsId;
    private String encryptedAuthorizedPersonProfile;
    private String identityLookupHash;
    private String relationship;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String status;
    private String approvedByGuardianDocsId;
    private String evidenceDocumentDocsId;
    private String verificationMethod;
    private Instant revokedAt;

    @Builder.Default
    private List<String> allowedWeekdays = new ArrayList<>();
}
