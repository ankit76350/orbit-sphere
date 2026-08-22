package com.orbitastra.backend.models.new_new.compliance;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentChannel;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentPurpose;
import com.orbitastra.backend.models.new_new.compliance.enums.ConsentScope;
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
 * One family's answer to one question about their child.
 *
 * <p>**This is the only place a school records that a guardian agreed to something.** Before
 * 2026-08-20 there were seven: this model, plus a document link and in two cases a boolean
 * on HostelAllocation, HealthProfile, MedicationAdministration, StudentRecognition and
 * SupportPlan. Seven shapes for one question, none of which could be withdrawn, expired or
 * evidenced the same way as the others. They now all point here.
 *
 * <p>It was called DpdpConsent and was renamed in the same change. Under the Digital Personal
 * Data Protection Act a child's data may be processed only with a verifiable parental consent
 * and the school has to be able to show it, which is where this started. But permission to
 * give a child paracetamol is not a data-protection question, and filing it in a collection
 * called {@code dpdp_consents} is the kind of category error a data-protection audit trips
 * over. The mechanism is shared because it is genuinely identical; the name no longer claims
 * they are the same kind of thing. ConsentPurpose says which purposes are the DPDP ones.
 *
 * <p>**One row per purpose, never one blanket agreement.** A family happy for the nurse to
 * hold medical details may still refuse to have their child's photograph on the school's
 * social media, and a single yes-or-no cannot hold both answers. It is also what makes a
 * withdrawal meaningful: taking back consent for photographs must not switch off the consent
 * that lets the school keep health records.
 *
 * <p>{@code scope} is what lets one model serve both kinds of question. "May the nurse give
 * your child paracetamol when she needs it?" is STANDING — asked once, answered for the year,
 * one per student per purpose. "May we give this antibiotic three times a day for five days?"
 * is RECORD_SPECIFIC, and a child may have many of those. Without the distinction, either the
 * unique index lets a family consent to medical treatment once in their child's whole school
 * career, or the index goes and a school ends up with four contradictory standing photograph
 * consents and no way to say which is current.
 *
 * <p>A record-specific consent is found through the record that points at it —
 * {@code MedicationAdministration.guardianConsentDocsId} and the four others. The link is
 * deliberately **one-directional**: a pointer back from here would be a second fact able to
 * disagree with the first.
 *
 * <p>**A withdrawal never deletes the row.** A school asked in June why it published a
 * photograph in March has to show the consent that stood *in March*, and deleting the record
 * when the family withdrew in April would remove exactly that. So a withdrawal sets a status
 * and a date, and the row stays.
 *
 * <p>{@code channel} is kept because the answers carry different weight if anybody argues. A
 * signed form scanned into {@code consentDocumentDocsId} is the strongest thing the school can
 * produce; a verbal yes written down by a member of staff is the weakest. When a family says
 * they never agreed, this decides whether the school has anything to show at all.
 *
 * <p>{@code grantedByGuardianDocsId} must be a guardian of this child. A consent given by
 * somebody with no standing is not a consent, and it is the first thing that would be
 * challenged.
 *
 * <p>There is no boolean anywhere saying "consent given". {@code status} is the answer, and a
 * boolean beside it is a field that can still say true the day after a family withdrew — which
 * on the health side would mean giving a child medicine their family had said no to.
 *
 * <p>The service checks that the grantor is a guardian of the child, that only a GRANTED and
 * unexpired consent permits the thing it covers, that a withdrawal is never a delete, that
 * anything relying on a consent stops the moment it is withdrawn, and that a RECORD_SPECIFIC
 * consent is actually pointed at by a record.
 */
@Document(collection = "guardian_consents")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_consent_student_purpose_uniq",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'consentPurpose': 1}",
                unique = true,
                partialFilter = "{'scope': 'STANDING', 'status': {'$in': ['PENDING', 'GRANTED']}}"),
        @CompoundIndex(
                name = "school_consent_student_idx",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'consentPurpose': 1, 'requestedAt': -1}"),
        @CompoundIndex(
                name = "school_consent_purpose_status_idx",
                def = "{'schoolId': 1, 'consentPurpose': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_consent_scope_idx",
                def = "{'schoolId': 1, 'scope': 1, 'consentPurpose': 1, 'status': 1}"),
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
public class GuardianConsent extends SchoolBase {

    // Links to Student.id the consent is about. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String studentDocsId;

    // What is being asked. One row per purpose.
    // Example: ConsentPurpose.PHOTOGRAPH_AND_MEDIA
    @NotNull
    private ConsentPurpose consentPurpose;

    // Whether this stands for everything of its kind, or was given for one occasion that
    // points at it. Decides whether the one-per-student-per-purpose rule applies.
    // Example: ConsentScope.STANDING
    @NotNull
    @Builder.Default
    private ConsentScope scope = ConsentScope.STANDING;

    // Example: ConsentStatus.GRANTED
    @NotNull
    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    // In the school's own words, so a family can see what they agreed to rather than a
    // code. For a record-specific consent this is where the particulars go, because the
    // purpose alone does not say which medicine or which trip.
    // Example: "Amoxicillin 250mg, three times a day, 5 to 10 September."
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
