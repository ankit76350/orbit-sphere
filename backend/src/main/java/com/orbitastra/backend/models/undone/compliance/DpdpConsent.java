package com.orbitastra.backend.models.undone.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.compliance.enums.ConsentChannel;
import com.orbitastra.backend.models.undone.compliance.enums.ConsentPurpose;
import com.orbitastra.backend.models.undone.compliance.enums.ConsentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DPDP Act consent record: a guardian's consent for a specific
 * data-processing
 * purpose regarding a (minor) student, with the channel it was captured through
 * and its grant/withdrawal timeline.
 */
@Document(collection = "dpdp_consents")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DpdpConsent extends SchoolBase {

    @Indexed(unique = true)
    private String studentDocsId;

    @Indexed
    private String guardianDocsId;

    private ConsentPurpose consentPurpose;

    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    private ConsentChannel channel;

    private LocalDateTime grantedAt;

    private LocalDateTime withdrawnAt;

    /**
     * Date and time until which this consent remains valid.
     * Null indicates that the consent remains valid until it is withdrawn.
     */
    private LocalDateTime expiryAt;

    // ! consentDocumentUrl = https://storage.example.com/consents/consent_123.pdf
    private String consentDocumentUrl;

    /**
     * Optional notes explaining rejection, withdrawal,
     * or any additional information related to this consent.
     */
    private String remarks;
}
