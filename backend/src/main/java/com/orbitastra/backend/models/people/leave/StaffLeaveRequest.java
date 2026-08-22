package com.orbitastra.backend.models.new_new.people.leave;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.leave.enums.LeaveRequestStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Leave request submitted by a Staff member against one StaffLeaveBalance.
 *
 * <p>Approval or cancellation must update the linked balance transactionally.
 * Request-format checks, date calculations, holiday exclusion, and allowed
 * status transitions belong to request DTOs and the leave service.
 */
@Document(collection = "staff_leave_requests")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_leave_request_no_uniq",
                def = "{'schoolId': 1, 'requestNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_staff_leave_dates_status_idx",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'status': 1, 'fromDate': 1, 'toDate': 1}"),
        @CompoundIndex(
                name = "school_leave_decision_queue_idx",
                def = "{'schoolId': 1, 'status': 1, 'submittedAt': 1}"),
        @CompoundIndex(
                name = "school_year_staff_leave_history_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'staffDocsId': 1, 'submittedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveRequest extends SchoolBase {

    // Generated using NumberSequenceType.STAFF_LEAVE_REQUEST.
    // Example: "LEAVE/2026/000001"
    @NotBlank
    private String requestNo;

    // Links to Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Links to LeaveType.id.
    // Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String leaveTypeDocsId;

    // Links to StaffLeaveBalance.id.
    //! Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String staffLeaveBalanceDocsId;

    // Stores AcademicYear.name, never its document id. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Example: 2026-08-10
    @NotNull
    private LocalDate fromDate;

    // Example: 2026-08-12
    @NotNull
    private LocalDate toDate;

    // Supports full and partial days. Example: 2.5
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal requestedDays;

    // Example: "Medical recovery"
    private String reason;

    // Example: LeaveRequestStatus.SUBMITTED
    @NotNull
    @Builder.Default
    private LeaveRequestStatus status = LeaveRequestStatus.DRAFT;

    // Whether another employee must cover duties. Example: true
    @NotNull
    @Builder.Default
    private Boolean coverRequired = false;

    //! Optionally links to the covering Staff.id.
    // Example: "67aa15d9dc3f7d0044444444"
    private String coverStaffDocsId;

    // References DocumentRecord.id values for medical/supporting documents.
    // Example: ["67aa15d9dc3f7d0055555555"]
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Example: 2026-08-01T09:00:00Z
    private Instant submittedAt;

    // Links to the Staff or identity account that approved/rejected the request.
    // Example: "67aa15d9dc3f7d0066666666"
    private String decidedByDocsId;

    // Example: 2026-08-02T10:30:00Z
    private Instant decidedAt;

    // Example: "Approved after arranging class coverage."
    private String decisionNote;
}
