package com.orbitastra.backend.models.undone.a_new.hostel;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "hostel_leave_requests")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_hostel_leave_no_uniq",
                def = "{'tenantId':1,'requestNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_leave_window_idx",
                def = "{'tenantId':1,'studentDocsId':1,'departAt':1,'expectedReturnAt':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelLeaveRequest extends AcademicScopedDocument {

    private String requestNo;
    private String hostelStayDocsId;
    private String studentDocsId;
    private String guardianDocsId;
    private String reason;
    private Instant departAt;
    private Instant expectedReturnAt;
    private Instant actualReturnAt;
    private ApprovalState state;
    private String approvedByDocsId;
    private String outPassDocsId;
    private String destination;
    private String emergencyContact;
}
