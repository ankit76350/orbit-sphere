package com.orbitastra.backend.models.undone.communication;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A pre-approved DLT (Distributed Ledger Technology) message template required by
 * Indian telecom regulation for transactional SMS. Broadcasts reference an
 * approved template by its DLT id.
 */
@Document(collection = "message_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate extends BaseDocument {

    // DLT-registered template id.
    private String dltId;

    private String name;

    private String body;
}
