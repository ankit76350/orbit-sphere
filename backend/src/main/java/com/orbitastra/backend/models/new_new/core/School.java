package com.orbitastra.backend.models.new_new.core;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AuditedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "schools")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class School extends AuditedDocument {

    private String schoolName;

    private String accountHolderName;

    @Indexed(unique = true)
    private String subdomain;

    private String logo;

    private String address;

    private String phone;

    private String email;

    private String encryptionKeyReference;

    private String defaultLocale;

    private String defaultTimeZone;

    private String addressLine;

    private Integer pincode;

    private String city;

    private String state;


    @Builder.Default
    private SchoolStatus status = SchoolStatus.PROVISIONING;

    public enum SchoolStatus {
        TRIAL,
        PROVISIONING,
        ACTIVE,
        SUSPENDED,
        OFFBOARDING,
        CLOSED,
        DELETION_PENDING,
        DELETED
    }

    private Instant activatedAt;

    private Instant suspendedAt;
}
