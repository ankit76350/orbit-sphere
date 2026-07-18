package com.orbitastra.backend.dto.crm;

import java.util.List;
import java.util.stream.Collectors;

import com.orbitastra.backend.models.crm.InquiryGuardian;
import com.orbitastra.backend.models.student.enums.GuardianRelation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Prospective guardian captured on an inquiry/admission lead. Raw contact data —
 * no Guardian document exists yet; it is materialised on conversion.
 */
@Data
public class InquiryGuardianRequest {

    @NotBlank(message = "guardian name is required")
    private String name;

    private GuardianRelation relation;

    private String phone;

    @Email(message = "email must be a valid address")
    private String email;

    private String address;

    private String occupation;

    public InquiryGuardian toModel() {
        return InquiryGuardian.builder()
                .name(name)
                .relation(relation)
                .phone(phone)
                .email(email)
                .address(address)
                .occupation(occupation)
                .build();
    }

    public static List<InquiryGuardian> toModels(List<InquiryGuardianRequest> requests) {
        if (requests == null) return null;
        return requests.stream().map(InquiryGuardianRequest::toModel).collect(Collectors.toList());
    }
}
