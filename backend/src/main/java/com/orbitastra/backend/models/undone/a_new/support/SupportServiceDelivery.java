package com.orbitastra.backend.models.undone.a_new.support;

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

@Document(collection = "support_service_deliveries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_plan_delivery_time_idx",
                def = "{'tenantId':1,'inclusionPlanDocsId':1,'deliveredAt':-1}"),
        @CompoundIndex(name = "tenant_provider_delivery_time_idx",
                def = "{'tenantId':1,'providerDocsId':1,'deliveredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportServiceDelivery extends AcademicScopedDocument {

    private String inclusionPlanDocsId;
    private String studentDocsId;
    private String serviceCode;
    private String providerDocsId;
    private Instant scheduledAt;
    private Instant deliveredAt;
    private Integer durationMinutes;
    private String attendanceStatus;
    private String goalKey;
    private String outcomeCode;
    private String confidentialCaseNoteDocsId;
}
