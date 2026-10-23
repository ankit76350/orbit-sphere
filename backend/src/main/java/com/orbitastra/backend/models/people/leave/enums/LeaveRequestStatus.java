package com.orbitastra.backend.models.people.leave.enums;

/**
 * Workflow lifecycle of a StaffLeaveRequest.
 */
public enum LeaveRequestStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED
}
