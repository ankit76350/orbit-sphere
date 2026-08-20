package com.orbitastra.backend.models.new_new.audit.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One field that changed, and what it changed from and to.
 *
 * <p>It has no collection of its own. The changes in one event are always read with it.
 *
 * <p>This is what makes an audit trail useful rather than merely present. "Somebody updated
 * this invoice" tells nobody anything. "The due date moved from 10 April to 30 April" is the
 * answer to the question that was actually asked.
 *
 * <p>Values are held as text on purpose. A field being audited might be a date, an amount, an
 * enum or an id, and the trail has to hold all of them in one shape. It is written for a
 * person to read, not for anything to compute from, so a formatted string is the right
 * choice and trying to preserve the original type would only add ways to get it wrong.
 *
 * <p>{@code redacted} is the field that keeps this collection safe. Auditing a change to a
 * salary, an Aadhaar number or a password hash must not copy the value into a second
 * collection that people can read more freely than the original. When it is true, the two
 * value fields stay null and the row records only that the field changed. **An audit trail
 * that leaks what it was auditing is worse than no audit trail**, because the leak is now in
 * a place nobody thought to protect.
 *
 * <p>Fields whose value never changes in a way anybody needs, such as an updated timestamp or
 * a version counter, are left out of the list altogether rather than filling every row with
 * noise.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFieldChange {

    // Name of the field as it appears on the model, so somebody can find it.
    // Example: "dueDate"
    @NotBlank
    private String fieldName;

    // Label a person would recognise, for an audit screen. Example: "Due date"
    private String fieldLabel;

    // What it was, formatted for a person to read. Null when redacted, or when the
    // field is being set for the first time. Example: "2026-04-10"
    private String oldValue;

    // What it became. Null when redacted, or when the field was cleared.
    // Example: "2026-04-30"
    private String newValue;

    // True when the values are too sensitive to copy here. The row then says only that
    // the field changed. Example: false
    @NotNull
    @Builder.Default
    private Boolean redacted = false;
}
