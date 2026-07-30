package com.orbitastra.backend.models.undone.a_new.alumni;

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

@Document(collection = "alumni_events_v2")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_event_code_uniq",
                def = "{'tenantId':1,'eventCode':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_event_status_start_idx",
                def = "{'tenantId':1,'status':1,'startsAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniEvent extends CampusScopedDocument {

    private String eventCode;
    private String title;
    private String eventType;
    private String description;
    private Instant startsAt;
    private Instant endsAt;
    private String timeZone;
    private String venueResourceDocsId;
    private String virtualSessionDocsId;
    private Integer capacity;
    private String status;
    private String registrationFormDefinitionDocsId;
}
