package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "conversation_threads")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_thread_no_uniq",
                def = "{'tenantId':1,'threadNo':1}", unique = true),
        @CompoundIndex(name = "tenant_participant_updated_idx",
                def = "{'tenantId':1,'participantDocsIds':1,'status':1,'lastMessageAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationThread extends TenantScopedDocument {

    private String threadNo;
    private String threadType;
    private String subject;
    private String status;
    private String relatedEntityType;
    private String relatedEntityDocsId;
    private Instant lastMessageAt;
    private String ownerDocsId;

    @Builder.Default
    private List<String> participantDocsIds = new ArrayList<>();
}
