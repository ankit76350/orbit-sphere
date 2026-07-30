package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.LocalTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "communication_preferences")
@CompoundIndex(name = "tenant_person_purpose_channel_uniq",
        def = "{'tenantId':1,'personType':1,'personDocsId':1,'purposeKey':1,'channel':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationPreference extends TenantScopedDocument {

    private PersonType personType;
    private String personDocsId;
    private String purposeKey;
    private String channel;
    private Boolean enabled;
    private String locale;
    private String timeZone;
    private LocalTime quietHoursFrom;
    private LocalTime quietHoursTo;
    private String consentRecordDocsId;
    private String source;
}
