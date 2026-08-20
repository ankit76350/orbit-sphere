package com.orbitastra.backend.models.new_new.compliance;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentChannel;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentPurpose;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One family's answer to one question about their child's data.
 *
 * <p>Under the Digital Personal Data Protection Act a child's data may be processed only with
 * a verifiable parental consent, and the school has to be able to show it. This is what it
 * shows.
 *
 * <p>**One row per purpose, never one blanket agreement.** A family happy for the nurse to
 * hold medical details may still refuse to have their child's photograph on the school's
 * social media, and a single yes-or-no cannot hold both answers. It is also what makes a
 * withdrawal meaningful: taking back consent for photographs must not switch off the consent
 * that lets the school keep health records.
 *
 * <p>A withdrawal never deletes the row. A school asked in June why it published a photograph
 * in March has to show the consent that stood **in March**, and deleting the record when the
 * family withdrew in April would remove exactly that. So a withdrawal sets a status and a
 * date, and the row stays.
 *
 * <p>{@code channel} is kept because the answers carry different weight if anybody argues. A
 * signed form scanned into {@code consentDocumentDocsId} is the strongest thing the school
 * can produce; a verbal yes written down by a member of staff is the weakest. When a family
 * says they never agreed, this decides whether the school has anything to show at all.
 *
 * <p>{@code grantedByGuardianDocsId} must be a guardian of this child. A consent given by
 * somebody with no standing is not a consent, and it is the first thing that would be
 * challenged.
 *
 * <p>This is where the consent fields scattered across other packages should eventually
 * consolidate: {@code HostelAllocation.guardianConsentDocumentDocsId},
 * {@code HealthProfile.routineMedicineConsent},
 * {@code MedicationAdministration.guardianConsentDocumentDocsId} and
 * {@code StudentRecognition.publicationConsent} are all asking a version of this question in
 * four different shapes. They are left alone for now; moving them is a separate change and
 * this model is the destination.
 *
 * <p>The service checks that the grantor is a guardian of the child, that only a GRANTED and
 * unexpired consent permits the thing it covers, that a withdrawal is never a delete, and
 * that anything relying on a consent stops the moment it is withdrawn.
 */
@Document(collection = "dpdp_consents")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_consent_student_purpose_uniq",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'consentPurpose': 1}",
                unique = true,
                partialFilter = "{'status': {'$in': ['PENDING', 'GRANTED']}}"),
        @CompoundIndex(
                name = "school_consent_student_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'consentPurpose': 1, 'requestedAt': -1}"),
        @CompoundIndex(
                name = "school_consent_purpose_status_idx",
                def = "{'schoolId': 1, 'consentPurpose': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_consent_expiry_idx",
                def = "{'schoolId': 1, 'status': 1, 'expiresAt': 1}",
                partialFilter = "{'expiresAt': {'$type': 'date'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DpdpConsent extends SchoolBase {

    // Links to Student.id the consent is about. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // What is being asked. One row per purpose.
    // Example: ConsentPurpose.PHOTOGRAPH_AND_MEDIA
    @NotNull
    private ConsentPurpose consentPurpose;

    // Example: ConsentStatus.GRANTED
    @NotNull
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    // In the school's own words, so a family can see what they agreed to rather than a
    // code. Example: "Using photographs of your child on the school website and its
    // social media pages."
    @NotBlank
    private String purposeDescription;

    // Links to Guardian.id for whoever answered. Must be a guardian of this child.
    // Example: "67aa15d9dc3f7d0066666666"
    private String grantedByGuardianDocsId;

    // How they answered, which decides how much the school can show if challenged.
    // Example: ConsentChannel.SIGNED_FORM
    private ConsentChannel channel;

    // When the school asked. Example: 2026-04-02T05:00:00Z
    @NotNull
    private Instant requestedAt;

    // When they said yes. Null unless the status is GRANTED.
    // Example: 2026-04-05T07:20:00Z
    private Instant grantedAt;

    // When they took it back. The row is kept, never deleted.
    // Example: 2026-11-14T09:10:00Z
    private Instant withdrawnAt;

    // Why they took it back, where they said. Not required: a family does not owe the
    // school a reason. Example: "Asked for photographs to stop after the class trip."
    private String withdrawalReason;

    // When the consent runs out and has to be asked again. Null when it stands until
    // withdrawn. Example: 2027-03-31T18:29:59Z
    private Instant expiresAt;

    // Links to DocumentRecord.id for the signed form. The strongest evidence there is.
    // Example: "67bf1124dc3f7d0033445566"
    private String consentDocumentDocsId;

    // Links to the staff identity that recorded the answer, which matters most when the
    // channel was verbal. Example: "67aa15d9dc3f7d0044444444"
    private String recordedByStaffDocsId;

    // Example: "Father signed at the admission interview; mother was not present."
    private String remarks;
}
