package com.orbitastra.backend.models.undone.a_new.it;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "service_tickets")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_ticket_no_uniq",
                def = "{'tenantId':1,'ticketNo':1}", unique = true),
        @CompoundIndex(name = "tenant_queue_status_sla_idx",
                def = "{'tenantId':1,'queueKey':1,'status':1,'slaDueAt':1}"),
        @CompoundIndex(name = "tenant_requester_status_idx",
                def = "{'tenantId':1,'requesterDocsId':1,'status':1,'createdAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicket extends CampusScopedDocument {

    private String ticketNo;
    private String requesterDocsId;
    private String requesterType;
    private String queueKey;
    private String category;
    private String priority;
    private String status;
    private String title;
    private String description;
    private String assignedToDocsId;
    private String managedDeviceDocsId;
    private String facilityResourceDocsId;
    private Instant slaDueAt;
    private Instant firstResponseAt;
    private Instant resolvedAt;
    private String resolution;
    private String parentTicketDocsId;

    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();
}
