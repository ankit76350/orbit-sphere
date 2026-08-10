package com.orbitastra.backend.models.new_new.audit.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One changed field recorded inside {@code AuditEvent.changes}.
 *
 * <p>Values are stored as their display strings rather than as their original
 * BSON types. An audit trail is read, not recomputed, and a fixed string type
 * keeps the collection free of schema drift as the audited models evolve.
 *
 * <p>When {@code redacted} is true the field name is kept but both values are
 * omitted. This is how restricted data — government identity numbers, health and
 * counselling notes, payroll amounts, safeguarding detail — is audited without
 * turning this collection into a second copy of the data it protects. The audit
 * trail proves the field changed; it does not disclose what it changed to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFieldChange {

    // Dotted path of the changed field. Example: "guardians[0].primaryContact"
    @NotBlank
    private String fieldPath;

    // Display value before the change; null when the field was unset or the
    // change is redacted. Example: "DRAFT"
    private String previousValue;

    // Display value after the change; null when the field was cleared or the
    // change is redacted. Example: "PUBLISHED"
    private String newValue;

    // True when the values are intentionally withheld because the field is
    // classified. Example: false
    @NotNull
    @Builder.Default
    private Boolean redacted = false;
}
