package com.orbitastra.backend.models.undone.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;
import com.orbitastra.backend.models.undone.compliance.enums.ConsentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DPDP Act consent record: a guardian's consent for a specific data-processing
 * purpose regarding a (minor) student, with the channel it was captured through
 * and its grant/withdrawal timeline.
 */
@Document(collection = "dpdp_consents")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DpdpConsent extends SchoolDocs {

    @Indexed
    private String studentId;

    // The data-processing purpose consent is sought for (e.g. "Photos & Media", "Health Data").
    private String consentType;

    @Builder.Default
    private ConsentStatus status = ConsentStatus.PENDING;

    // How consent was captured, e.g. "Parent App", "OTP e-Sign".
    private String channel;

    private LocalDateTime grantedAt;

    private LocalDateTime withdrawnAt;
}
