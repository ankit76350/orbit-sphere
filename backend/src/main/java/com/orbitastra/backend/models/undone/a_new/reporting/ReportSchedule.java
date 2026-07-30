package com.orbitastra.backend.models.undone.a_new.reporting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "report_schedules")
@CompoundIndex(name = "tenant_report_schedule_key_uniq",
        def = "{'tenantId':1,'scheduleKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSchedule extends TenantScopedDocument {

    private String scheduleKey;
    private String reportDefinitionDocsId;
    private String cronExpression;
    private String timeZone;
    private String deliveryChannel;
    private String status;
    private Instant nextRunAt;
    private Instant lastRunAt;
    private String ownerDocsId;

    @Builder.Default
    private List<String> recipientDocsIds = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();
}
