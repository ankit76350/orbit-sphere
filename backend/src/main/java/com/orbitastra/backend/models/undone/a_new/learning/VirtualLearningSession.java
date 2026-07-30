package com.orbitastra.backend.models.undone.a_new.learning;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "virtual_learning_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_virtual_provider_meeting_uniq",
                def = "{'tenantId':1,'providerKey':1,'providerMeetingId':1}", unique = true),
        @CompoundIndex(name = "tenant_course_virtual_start_idx",
                def = "{'tenantId':1,'courseOfferingDocsId':1,'startsAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualLearningSession extends AcademicScopedDocument {

    private String courseOfferingDocsId;
    private String scheduleOccurrenceDocsId;
    private String hostPersonDocsId;
    private String title;
    private String providerKey;
    private String integrationConnectionDocsId;
    private String providerMeetingId;
    private String encryptedHostCredentialReference;
    private Instant startsAt;
    private Instant endsAt;
    private String timeZone;
    private String status;
    private Boolean recordingAllowed;
    private String recordingConsentBasis;
}
