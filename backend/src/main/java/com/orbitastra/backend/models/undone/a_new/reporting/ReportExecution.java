package com.orbitastra.backend.models.undone.a_new.reporting;

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

@Document(collection = "report_executions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_report_execution_no_uniq",
                def = "{'tenantId':1,'executionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_report_status_started_idx",
                def = "{'tenantId':1,'reportDefinitionDocsId':1,'status':1,'startedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecution extends TenantScopedDocument {

    private String executionNo;
    private String reportDefinitionDocsId;
    private String reportScheduleDocsId;
    private String requestedByDocsId;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private Long rowCount;
    private String outputDocumentDocsId;
    private String outputHash;
    private String errorCode;
    private String errorMessage;
}
