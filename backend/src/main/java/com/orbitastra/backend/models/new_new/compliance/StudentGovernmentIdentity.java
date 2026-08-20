package com.orbitastra.backend.models.new_new.compliance;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.common.enums.GovernmentIdentityType;
import com.orbitastra.backend.models.new_new.common.enums.IdentityVerificationStatus;
import com.orbitastra.backend.models.new_new.compliance.enums.ApaarStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One government identity number belonging to one student.
 *
 * <p>Staff have had StaffGovernmentIdentity since the people package was built. Students had
 * nothing, which meant a school being asked for its APAAR coverage had nowhere to hold the
 * numbers. This closes that.
 *
 * <p>One row per student per identity type, so a child can have an Aadhaar row, an APAAR row
 * and a state PEN row without any of them getting in the way of the others.
 *
 * <p>**The number is never stored in plain text.** The reference sketch kept
 * {@code aadhaarNo} as a plain string, which is both a security problem and a legal one:
 * holding Aadhaar numbers in the clear is restricted, and a leaked database of children's
 * Aadhaar numbers is about the worst thing this system could do. So the same three fields as
 * StaffGovernmentIdentity, each with one job: the encrypted value holds the real number, the
 * lookup hash spots a duplicate without anything being decrypted, and the masked version is
 * the only one that may ever reach a screen or a report.
 *
 * <p>{@code apaarStatus} and {@code digiLockerLinked} apply only to an APAAR row. APAAR has a
 * lifecycle the other identities do not: it is applied for, generated from Aadhaar with the
 * family's consent, and can come back refused when details do not match. A school asked for
 * its coverage needs the list of children nobody applied for and the list that failed, and
 * those are different problems.
 *
 * <p>An APAAR row may only exist where the family has granted APAAR_GENERATION consent. The
 * number is derived from a child's Aadhaar, so generating it without consent is exactly what
 * the consent exists to prevent.
 *
 * <p>The service checks that no plaintext number is ever persisted or returned, that an APAAR
 * row has a granted consent behind it, that the masked value carries no more than the last
 * few digits, and that reading these records needs the COMPLIANCE module rather than plain
 * student access.
 */
@Document(collection = "student_government_identities")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_student_identity_type_uniq",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'identityType': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_student_identity_lookup_idx",
                def = "{'schoolId': 1, 'identityType': 1, 'identityNumberLookupHash': 1}"),
        @CompoundIndex(
                name = "school_student_apaar_status_idx",
                def = "{'schoolId': 1, 'apaarStatus': 1}",
                partialFilter = "{'apaarStatus': {'$exists': true}}"),
        @CompoundIndex(
                name = "school_student_identity_verification_idx",
                def = "{'schoolId': 1, 'verificationStatus': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGovernmentIdentity extends SchoolBase {

    // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // Which kind of number this row holds.
    // Example: GovernmentIdentityType.APAAR
    @NotNull
    private GovernmentIdentityType identityType;

    // The real number, encrypted before it is saved. Never a plain value.
    // Example: "enc:v1:4f3e2d1c0b9a8877"
    @NotBlank
    private String encryptedIdentityNumber;

    // One-way hash, so the same number can be spotted twice without decrypting.
    // Example: "sha256:7e6d5c4b3a2918070011223344556677"
    @NotBlank
    private String identityNumberLookupHash;

    // The only version that may reach a screen or a report. Example: "XXXX XXXX 4821"
    @NotBlank
    private String maskedIdentityNumber;

    // How far the school has got in checking it is genuine.
    // Example: IdentityVerificationStatus.VERIFIED
    @NotNull
    @Builder.Default
    private IdentityVerificationStatus verificationStatus = IdentityVerificationStatus.PENDING;

    // Where the APAAR application has got to. Only set on an APAAR row.
    // Example: ApaarStatus.GENERATED
    private ApaarStatus apaarStatus;

    // Whether the student's DigiLocker account is linked, which is how APAAR documents
    // reach the family. Only meaningful on an APAAR row. Example: true
    @NotNull
    @Builder.Default
    private Boolean digiLockerLinked = false;

    //! Links to DpdpConsent.id for the APAAR_GENERATION consent this row relies on.
    // Required on an APAAR row: the number comes from the child's Aadhaar.
    // Example: "67bf1122dc3f7d0011223344"
    private String apaarConsentDocsId;

    // Links to DocumentRecord.id for the scanned card or certificate.
    // Example: "67bf1123dc3f7d0022334455"
    private String evidenceDocumentDocsId;

    // Links to the staff identity that checked the evidence.
    // Example: "67aa15d9dc3f7d0044444444"
    private String verifiedByDocsId;

    // Example: 2026-06-18T05:40:00Z
    private Instant verifiedAt;

    // Example: "Name on the Aadhaar reads Arjun Kumar Sharma; school records say Arjun
    // Sharma. Correction applied for."
    private String remarks;
}
