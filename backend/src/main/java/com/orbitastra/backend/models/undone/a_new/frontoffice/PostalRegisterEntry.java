package com.orbitastra.backend.models.undone.a_new.frontoffice;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "postal_register_entries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_campus_postal_reference_uniq",
                def = "{'tenantId':1,'campusDocsId':1,'referenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_postal_direction_date_idx",
                def = "{'tenantId':1,'campusDocsId':1,'direction':1,'registerDate':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PostalRegisterEntry extends CampusScopedDocument {

    private String referenceNo;
    private String direction;
    private LocalDate registerDate;
    private String modeCode;
    private String encryptedPartyDetails;
    private String subject;
    private String trackingReference;
    private String handledByDocsId;
    private String status;
    private String documentRecordDocsId;
    private Instant dispatchedAt;
    private Instant receivedAt;
}
