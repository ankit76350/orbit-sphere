package com.orbitastra.backend.models.undone.a_new.people;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "staff_leave_requests")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_leave_request_no_uniq",
                def = "{'tenantId':1,'requestNo':1}", unique = true),
        @CompoundIndex(name = "tenant_staff_leave_dates_idx",
                def = "{'tenantId':1,'staffDocsId':1,'fromDate':1,'toDate':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveRequest extends CampusScopedDocument {

    private String requestNo;
    private String staffDocsId;
    private String leaveTypeCode;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal requestedDays;
    private String reason;
    private ApprovalState state;
    private String approvedByDocsId;
    private String coverRequired;
    private String workflowRunDocsId;
}
