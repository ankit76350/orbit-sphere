package com.orbitastra.backend.models.new_new.crm.embedded;

import com.orbitastra.backend.models.student.enums.GuardianRelation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryGuardian {

    // Example: "Rohan Sharma"
    private String fullName;

    // Example: GuardianRelation.FATHER
    private GuardianRelation relation;

    // Example: "+919876543210"
    private String phoneNumber;

    // Example: "rohan.sharma@example.com"
    private String emailAddress;

    // Example: "12 MG Road, Pune, Maharashtra 411001"
    private String address;

    // Example: "Software Engineer"
    private String occupation;

    // Example: true
    @Builder.Default
    private Boolean primaryContact = false;
}
