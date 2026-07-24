package com.orbitastra.backend.models.undone.communication;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

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
public class MessageTemplate extends SchoolBase {

    // DLT-registered template id.
    private String dltNo;

    private String name;

    private String body;
}
