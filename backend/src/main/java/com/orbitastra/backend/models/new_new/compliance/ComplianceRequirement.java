package com.orbitastra.backend.models.new_new.compliance;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.compliance.enums.ComplianceAuthority;
import com.orbitastra.backend.models.new_new.compliance.enums.ComplianceRequirementType;
import com.orbitastra.backend.models.new_new.compliance.enums.RequirementFrequency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One standing obligation the school has to somebody outside it.
 *
 * <p>
 * "File the UDISE+ return every year by 30 September." "Renew the fire
 * no-objection
 * certificate every two years." "Get the kitchen inspected by the health
 * department
 * annually."
 *
 * <p>
 * This is the **rule**, not any one round of doing it. Each round is a
 * ComplianceSubmission. The split is the same one this codebase uses
 * everywhere: something
 * standing on one side, something that happens on a date on the other.
 *
 * <p>
 * Without the split there is nowhere to hold "this comes round every September"
 * — so
 * either the school remembers, or it finds out it has missed something when an
 * inspector
 * asks. The whole value of the model is being warned before the date rather
 * than after.
 *
 * <p>
 * {@code frequency} is what lets the next submission be created as soon as the
 * last one is
 * accepted, so nobody has to remember in eleven months.
 *
 * <p>
 * {@code responsibleStaffDocsId} is who answers for it. An obligation with
 * nobody against
 * it is the one that gets missed, because everybody assumes somebody else is
 * doing it.
 *
 * <p>
 * {@code active} being false retires an obligation without deleting it, so the
 * submissions
 * already filed against it still make sense.
 *
 * <p>
 * The service checks that a requirement with submissions against it is never
 * deleted, and
 * that a recurring one always has a lead time so a warning can be raised before
 * the date
 * rather than on it.
 */
@Document(collection = "compliance_requirements")
@CompoundIndexes({
                @CompoundIndex(name = "school_compliance_requirement_name_uniq", def = "{'schoolId': 1, 'name': 1}", unique = true),
                @CompoundIndex(name = "school_compliance_requirement_active_idx", def = "{'schoolId': 1, 'active': 1, 'authority': 1}"),
                @CompoundIndex(name = "school_compliance_requirement_type_idx", def = "{'schoolId': 1, 'requirementType': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRequirement extends SchoolBase {
        // ! Between school ↔ government / board
        //? EX: 1. Board affiliation — renew every 3 years, 2. Fire NOC — every 2 years

        // What it is, in the school's own words. Example: "Annual UDISE+ data return"
        @NotBlank
        private String name;

        // Who wants it. Example: ComplianceAuthority.UDISE
        @NotNull
        private ComplianceAuthority authority;

        // What sort of thing it is.
        // Example: ComplianceRequirementType.DATA_SUBMISSION
        @NotNull
        private ComplianceRequirementType requirementType;

        // How often it comes round. Example: RequirementFrequency.ANNUAL
        @NotNull
        @Builder.Default
        private RequirementFrequency frequency = RequirementFrequency.ANNUAL;

        // For a MULTI_YEAR requirement, how many years between rounds. Example: 3
        private Integer intervalYears;

        // The usual due date, written as a day and month for a recurring obligation so
        // the
        // next round can be worked out. Example: "30 September"
        private String usualDueDate;

        // How many days before the due date somebody should be warned. Being told on
        // the day
        // is being told too late. Example: 45
        @NotNull
        @Builder.Default
        private Integer reminderLeadDays = 30;

        // Links to Staff.id for whoever answers for this. An obligation with nobody
        // against
        // it is the one that gets missed. Example: "67aa15d9dc3f7d0044444444"
        private String responsibleStaffDocsId;

        // What has to be sent or done, written for whoever picks it up next year.
        // Example: "Enrolment, staff and infrastructure data on the UDISE+ portal,
        // signed off
        // by the principal."
        private String description;

        // Where it is filed, so nobody hunts for the portal.
        // Example: "https://udiseplus.gov.in"
        private String referenceUrl;

        // What happens if the school misses it, written down because it is what gets a
        // deadline taken seriously.
        // Example: "Affiliation review, and the school does not appear in the national
        // enrolment figures."
        private String consequenceOfMissing;

        // Whether this obligation still applies. Example: true
        @NotNull
        @Builder.Default
        private Boolean active = true;
}
