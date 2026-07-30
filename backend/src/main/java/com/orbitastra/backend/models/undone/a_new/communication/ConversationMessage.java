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

@Document(collection = "conversation_messages")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_thread_sequence_uniq",
                def = "{'tenantId':1,'conversationThreadDocsId':1,'sequenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_sender_message_time_idx",
                def = "{'tenantId':1,'senderDocsId':1,'sentAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage extends TenantScopedDocument {

    private String conversationThreadDocsId;
    private Long sequenceNo;
    private String senderDocsId;
    private String senderRoleKey;
    private String body;
    private Instant sentAt;
    private Instant editedAt;
    private Instant messageDeletedAt;
    private String moderationStatus;

    @Builder.Default
    private List<String> attachmentDocumentDocsIds = new ArrayList<>();
}
