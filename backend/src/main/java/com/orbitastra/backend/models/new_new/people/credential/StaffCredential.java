package com.orbitastra.backend.models.new_new.people.credential;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.credential.enums.CredentialVerificationStatus;
import com.orbitastra.backend.models.new_new.people.credential.enums.StaffCredentialType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Qualification, licence, certificate, or compliance credential belonging to a
 * Staff member.
 *
 * <p>Credential numbers may be encrypted and searched using an optional keyed
 * lookup hash. The evidence document remains in the document-storage module.
 */
@Document(collection = "staff_credentials")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_credential_lookup_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'credentialType': 1, 'credentialNumberLookupHash': 1}",
                unique = true,
                partialFilter = "{'credentialNumberLookupHash': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_credential_status_expiry_idx",
                def = "{'schoolId': 1, 'verificationStatus': 1, 'validUntil': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCredential extends SchoolBase {

    // Links to Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Example: StaffCredentialType.TEACHING_LICENSE
    @NotNull
    private StaffCredentialType credentialType;

    // Example: "Bachelor of Education"
    @NotBlank
    private String title;

    // Example: "Savitribai Phule Pune University"
    private String issuingAuthority;

    // Application-encrypted credential number; never plaintext.
    private String encryptedCredentialNumber;

    // Optional keyed hash used for exact duplicate lookup.
    private String credentialNumberLookupHash;

    // Example: 2020-06-15
    private LocalDate issuedOn;

    // Null for credentials without expiry. Example: 2030-06-14
    private LocalDate validUntil;

    // Example: CredentialVerificationStatus.UNVERIFIED
    @NotNull
    @Builder.Default
    private CredentialVerificationStatus verificationStatus =
            CredentialVerificationStatus.UNVERIFIED;

    // Links to the verifying Staff or identity account.
    // Example: "67aa15d9dc3f7d0022222222"
    private String verifiedByDocsId;

    // Example: 2026-07-31T09:30:00Z
    private Instant verifiedAt;

    // Links to the uploaded supporting document.
    // Example: "67aa15d9dc3f7d0033333333"
    private String evidenceDocumentDocsId;
}
