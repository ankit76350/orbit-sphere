package com.orbitastra.backend.models.undone.communication;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A pre-approved DLT (Distributed Ledger Technology) message template required by
 * Indian telecom regulation for transactional SMS. Broadcasts reference an
 * approved template by its DLT id.
 */
@Document(collection = "message_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    // DLT-registered template id.
    private String dltId;

    private String name;

    private String body;
}
