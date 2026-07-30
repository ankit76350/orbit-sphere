package com.orbitastra.backend.models.undone.a_new.conduct;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "student_conduct_cases")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_conduct_case_no_uniq",
                def = "{'tenantId':1,'caseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_conduct_status_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'openedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentConductCase extends AcademicScopedDocument {

    private String caseNo;
    private String studentDocsId;
    private String categoryCode;
    private String severity;
    private Confidentiality confidentiality;
    private String status;
    private String assignedToDocsId;
    private String safeguardingCaseDocsId;
    private String sourceEventDocsId;
    private Instant openedAt;
    private Instant dueAt;
    private Instant closedAt;
    private String closureOutcome;
}
