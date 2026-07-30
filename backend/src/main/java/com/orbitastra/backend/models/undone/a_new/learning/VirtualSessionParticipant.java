package com.orbitastra.backend.models.undone.a_new.learning;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "virtual_session_participants")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_virtual_session_person_uniq",
                def = "{'tenantId':1,'virtualLearningSessionDocsId':1,'personDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_virtual_person_join_idx",
                def = "{'tenantId':1,'personDocsId':1,'firstJoinedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualSessionParticipant extends TenantScopedDocument {

    private String virtualLearningSessionDocsId;
    private String personDocsId;
    private String participantRole;
    private String attendanceStatus;
    private Instant firstJoinedAt;
    private Instant lastLeftAt;
    private Duration attendedDuration;
    private Integer reconnectCount;
    private Boolean consentToRecording;
}
