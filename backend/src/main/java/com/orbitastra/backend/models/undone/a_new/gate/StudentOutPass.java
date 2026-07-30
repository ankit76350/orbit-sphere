package com.orbitastra.backend.models.undone.a_new.gate;

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

@Document(collection = "student_out_passes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_outpass_no_uniq",
                def = "{'tenantId':1,'outPassNo':1}", unique = true),
        @CompoundIndex(name = "tenant_outpass_student_status_exit_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'expectedExitAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOutPass extends AcademicScopedDocument {

    private String outPassNo;
    private String studentDocsId;
    private String requestedByGuardianDocsId;
    private String reason;
    private Boolean emergency;
    private String status;
    private Instant expectedExitAt;
    private Instant expectedReturnAt;
    private Instant actualExitAt;
    private Instant actualReturnAt;
    private String approvedByDocsId;
    private String pickupAuthorizationDocsId;
    private String workflowRunDocsId;
}
