package com.orbitastra.backend.models.undone.a_working.communication;


import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.communication.enums.CommunicationChannel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "message_templates")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageTemplate extends SchoolBase {

    /**
     * Template name.
     */
    @Indexed(unique = true)
    private String name;

    /**
     * EMAIL / SMS / PUSH / etc.
     */
    private CommunicationChannel channel;

    /**
     * Email subject.
     * Null for SMS/Push if not required.
     */
    private String subject;

    /**
     * Message body.
     *
     * Example:
     * Hello {{studentName}}
     */
    private String body;

    /**
     * Variables used in template.
     *
     * Example:
     * studentName
     * feeAmount
     */
    private List<String> variables;

    /**
     * External provider template id.
     *
     * DLT
     * SES
     * SendGrid
     * WhatsApp
     */
    private String providerTemplateId;

    /**
     * Active template.
     */
    @Indexed
    private Boolean active;
}
