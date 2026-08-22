package com.orbitastra.backend.models.new_new.facilities.embedded;

import java.time.LocalDate;

import com.orbitastra.backend.models.new_new.facilities.enums.FindingSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One thing an inspection found.
 *
 * <p>{@code workOrderDocsId} is the field that makes an inspection worth doing. A round that
 * produces a list of problems and no jobs is a round somebody read once and filed, and the
 * next inspector finds the same three things a year later. Linking the finding to the work
 * order raised for it means "what did we actually do about the last fire inspection" is a
 * query rather than an argument.
 *
 * <p>It is null while nothing has been raised yet, and **that null is the useful part** — open
 * findings with no work order are the list the school is actually behind on.
 *
 * <p>{@code location} is free text on purpose. The useful answer is "the third window from the
 * corridor end", which no room record holds and no dropdown will ever contain.
 *
 * <p>Embedded rather than a collection because findings are always read with their inspection
 * and there is no query that wants a finding without knowing which round it came from. The
 * cross-round question — "is this the same crack the last inspector saw?" — is a human reading
 * two reports, not a join.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionFinding {

    // Order this finding appears in the report. Example: 1
    @NotNull
    private Integer findingNo;

    // How bad it is. CRITICAL means stop using the space now.
    // Example: FindingSeverity.MAJOR
    @NotNull
    private FindingSeverity severity;

    // What was found, in the inspector's words.
    // Example: "Two extinguishers on the first floor are past their service date."
    @NotBlank
    private String description;

    // Whereabouts, in words. Example: "First floor corridor, near the stairwell"
    private String location;

    // What the inspector says should be done.
    // Example: "Have both refilled and re-tagged by the supplier."
    private String recommendation;

    // The date the finding has to be dealt with by, where the inspector set one.
    // Example: 2026-09-15
    private LocalDate rectifyBy;

    // Links to MaintenanceWorkOrder.id raised for this. Null until one is, and that
    // null is what says the school has not acted on it yet.
    // Example: "67c31124dc3f7d0033445566"
    private String workOrderDocsId;

    // Whether this has been dealt with. Set when the work order completes, or by hand
    // for something that needed no job. Example: false
    @NotNull
    @Builder.Default
    private Boolean resolved = false;

    // When it was dealt with. Example: 2026-09-11
    private LocalDate resolvedOn;

    // How it was dealt with, when that is not obvious from the work order.
    // Example: "Both refilled on site by the supplier during the visit."
    private String resolutionNote;
}
