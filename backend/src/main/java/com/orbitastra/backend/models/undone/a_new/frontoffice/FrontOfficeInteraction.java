package com.orbitastra.backend.models.undone.a_new.frontoffice;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "front_office_interactions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_frontoffice_interaction_no_uniq",
                def = "{'tenantId':1,'interactionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_frontoffice_handler_time_idx",
                def = "{'tenantId':1,'handledByDocsId':1,'occurredAt':-1}"),
        @CompoundIndex(name = "tenant_frontoffice_contact_lookup_idx",
                def = "{'tenantId':1,'contactLookupHash':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FrontOfficeInteraction extends CampusScopedDocument {

    private String interactionNo;
    private String channel;
    private String direction;
    private String purposeCode;
    private String partyType;
    private String partyDocsId;
    private String encryptedPartyContact;
    private String contactLookupHash;
    private String handledByDocsId;
    private String summary;
    private String status;
    private String linkedEntityType;
    private String linkedEntityDocsId;
    private Instant occurredAt;
    private Instant followUpAt;
}
