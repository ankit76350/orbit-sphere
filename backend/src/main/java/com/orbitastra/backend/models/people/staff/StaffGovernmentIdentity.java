package com.orbitastra.backend.models.people.staff;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.common.enums.GovernmentIdentityType;
import com.orbitastra.backend.models.common.enums.IdentityVerificationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Encrypted government or tax identity belonging to one Staff member.
 *
 * <p>Plaintext identity numbers must never be persisted. The encrypted value is
 * used only for authorized retrieval, while the keyed lookup hash provides
 * exact duplicate detection without exposing the original number.
 */
@Document(collection = "staff_government_identities")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_identity_type_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'identityType': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_identity_lookup_uniq",
                def = "{'schoolId': 1, 'identityType': 1, 'identityNumberLookupHash': 1}",
                unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StaffGovernmentIdentity extends SchoolBase {

    // Links to Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Example: GovernmentIdentityType.AADHAAR
    @NotNull
    private GovernmentIdentityType identityType;

    // ISO 3166-1 alpha-2 issuing country. Example: "IN"
    @NotBlank
    private String issuingCountryCode;

    // Application-encrypted value; never plaintext.
    @NotBlank
    private String encryptedIdentityNumber;

    // Keyed HMAC/blind index used only for exact equality lookup.
    // Example: "hmac-sha256:3a8f..."
    @NotBlank
    private String identityNumberLookupHash;

    // Safe value for UI display. Example: "XXXX-XXXX-1234"
    private String maskedIdentityNumber;

    // References DocumentRecord.id for the uploaded evidence.
    // Example: "67aa15d9dc3f7d0044444444"
    private String evidenceDocumentDocsId;

    // Example: IdentityVerificationStatus.UNVERIFIED
    @NotNull
    @Builder.Default
    private IdentityVerificationStatus verificationStatus =
            IdentityVerificationStatus.UNVERIFIED;

    // Links to the verifying Staff or identity account.
    // Example: "67aa15d9dc3f7d0055555555"
    private String verifiedByDocsId;

    // Example: 2026-07-31T09:30:00Z
    private Instant verifiedAt;
}
