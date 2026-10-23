package com.orbitastra.backend.models.crm.embedded;

import com.orbitastra.backend.models.common.enums.GuardianRelation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A prospective guardian snapshot embedded inside an Inquiry or
 * AdmissionApplication. It is not a separate collection and therefore does not
 * extend SchoolBase or have its own document id.
 *
 * <p>The service should store phone numbers in normalized E.164 form and emails
 * in trimmed lowercase form so the Inquiry indexes remain reliable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryGuardian {

    // Example: "Rohan Sharma"
    @NotBlank
    private String fullName;

    // Example: GuardianRelation.FATHER
    @NotNull
    private GuardianRelation relation;

    // Example: "+919876543210" (always stored in normalized E.164 format)
    private String phoneNumber;

    // Example: "rohan.sharma@example.com" (always trimmed and stored in lowercase)
    private String emailAddress;

    // Example: "12 MG Road, Pune, Maharashtra 411001"
    private String address;

    // Example: "Software Engineer"
    private String occupation;

    // Example: true
    @Builder.Default
    private Boolean primaryContact = false;
}
