package com.orbitastra.backend.models.undone.a_new.common;

/**
 * Shared, stable codes used by multiple new-domain models. Module-specific
 * lifecycle enums remain beside their aggregate to avoid one unbounded enum.
 */
public final class PlatformEnums {

    private PlatformEnums() {
    }

    public enum RecordState {
        ACTIVE,
        INACTIVE,
        ARCHIVED,
        DELETED
    }

    public enum ApprovalState {
        DRAFT,
        SUBMITTED,
        IN_REVIEW,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public enum Confidentiality {
        PUBLIC,
        INTERNAL,
        CONFIDENTIAL,
        RESTRICTED,
        HIGHLY_RESTRICTED
    }

    public enum PersonType {
        STUDENT,
        GUARDIAN,
        STAFF,
        APPLICANT,
        ALUMNUS,
        VENDOR_CONTACT,
        EXTERNAL
    }

    public enum ScopeType {
        TENANT,
        LEGAL_ENTITY,
        CAMPUS,
        ACADEMIC_YEAR,
        PROGRAMME,
        DEPARTMENT,
        GRADE,
        CLASS,
        SECTION,
        SUBJECT,
        HOUSE,
        HOSTEL,
        ROUTE,
        SELF
    }
}
