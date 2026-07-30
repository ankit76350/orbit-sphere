package com.orbitastra.backend.models.undone.a_new.facilities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

@Document(collection = "maintenance_work_orders")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_work_order_no_uniq",
                def = "{'tenantId':1,'workOrderNo':1}", unique = true),
        @CompoundIndex(name = "tenant_assignee_status_sla_idx",
                def = "{'tenantId':1,'assignedToDocsId':1,'status':1,'slaDueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceWorkOrder extends CampusScopedDocument {

    public enum WorkOrderStatus {
        OPEN,
        TRIAGED,
        ASSIGNED,
        IN_PROGRESS,
        AWAITING_PARTS,
        COMPLETED,
        INSPECTED,
        CLOSED,
        CANCELLED
    }

    private String workOrderNo;
    private String maintenancePlanDocsId;
    private String assetDocsId;
    private String facilityResourceDocsId;
    private String reportedByDocsId;
    private String assignedToDocsId;
    private String vendorDocsId;
    private String title;
    private String description;
    private String priority;
    private WorkOrderStatus status;
    private Instant reportedAt;
    private Instant slaDueAt;
    private Instant completedAt;
    private String completionNotes;
    private BigDecimal cost;
    private String currencyCode;
    private LocalDate nextMaintenanceDate;

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
