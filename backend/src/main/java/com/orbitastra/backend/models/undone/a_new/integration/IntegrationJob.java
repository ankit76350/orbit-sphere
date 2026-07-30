package com.orbitastra.backend.models.undone.a_new.integration;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "integration_jobs")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_integration_job_no_uniq",
                def = "{'tenantId':1,'jobNo':1}", unique = true),
        @CompoundIndex(name = "tenant_connection_status_schedule_idx",
                def = "{'tenantId':1,'integrationConnectionDocsId':1,'status':1,'nextRunAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationJob extends TenantScopedDocument {

    private String jobNo;
    private String integrationConnectionDocsId;
    private String jobType;
    private String direction;
    private String entityType;
    private String status;
    private String scheduleExpression;
    private String cursor;
    private Instant nextRunAt;
    private Instant startedAt;
    private Instant completedAt;
    private Long readCount;
    private Long successCount;
    private Long failureCount;
    private String lastErrorCode;
    private String lastErrorMessage;
}
