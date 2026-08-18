package com.orbitastra.backend.models.new_new.health;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.health.enums.ImmunizationVerificationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One vaccination a child has had.
 *
 * <p>Schools are asked for this by boards and by health authorities, and a family
 * moving schools is asked for it again. It is not tied to an academic year, because
 * a jab given at eighteen months is still the answer when the child is fourteen.
 *
 * <p>{@code verificationStatus} is the field that stops this being a list of
 * claims. A parent saying a jab was given and a certificate proving it are not the
 * same thing, and an authority asking for proof will not accept the first. Keeping
 * them apart means the school knows which of its records would survive being
 * checked.
 *
 * <p>{@code nextDoseDueOn} is what makes the record useful rather than only
 * historical. A course of jabs given at the wrong spacing does not work, so the
 * school can tell a family a dose is coming due instead of finding out years later
 * that one was skipped.
 *
 * <p>The vaccine is named as text rather than coded. A school is copying what is
 * written on a card a parent brought in; inventing a code for it would only add a
 * way to get it wrong.
 *
 * <p>The service checks that the same vaccine and dose number is not recorded twice
 * for one child, that {@code administeredOn} is not in the future, and that reading
 * these records needs the HEALTH module.
 */
@Document(collection = "immunization_records")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_student_vaccine_dose_uniq",
                def = "{'schoolId': 1, 'studentDocsId': 1, 'vaccineName': 1, 'doseNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_immunization_due_idx",
                def = "{'schoolId': 1, 'nextDoseDueOn': 1}",
                partialFilter = "{'nextDoseDueOn': {'$type': 'date'}}"),
        @CompoundIndex(
                name = "school_immunization_verification_idx",
                def = "{'schoolId': 1, 'verificationStatus': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImmunizationRecord extends SchoolBase {

    // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
    @Indexed
    @NotBlank
    private String studentDocsId;

    // Name of the vaccine as written on the card the family brought in.
    // Example: "MMR"
    @NotBlank
    private String vaccineName;

    // Which dose in the course this was, starting at 1. Example: 2
    @NotNull
    @Builder.Default
    private Integer doseNo = 1;

    // The day it was given. Example: 2019-11-14
    @NotNull
    private LocalDate administeredOn;

    // When the next dose is due, so a family can be reminded before it is missed.
    // Null when the course is finished. Example: 2020-05-14
    private LocalDate nextDoseDueOn;

    // Who gave it. Example: "Sunrise Clinic, Dr Anita Rao"
    private String providerName;

    // How much the school knows this really happened.
    // Example: ImmunizationVerificationStatus.VERIFIED
    @NotNull
    @Builder.Default
    private ImmunizationVerificationStatus verificationStatus =
            ImmunizationVerificationStatus.PARENT_REPORTED;

    // Links to DocumentRecord.id for the scanned card or certificate.
    // Example: "67b71126dc3f7d0055667788"
    private String evidenceDocumentDocsId;

    // Links to the staff identity that checked the evidence.
    // Example: "67aa15d9dc3f7d0044444444"
    private String verifiedByDocsId;

    // Anything worth knowing, including a batch number in the rare case the school
    // gave the jab itself and wants one on record.
    // Example: "Card was faded; mother confirmed the date."
    private String remarks;
}
