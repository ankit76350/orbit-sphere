package com.orbitastra.backend.models.health;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.health.enums.MedicationRoute;
import com.orbitastra.backend.models.health.enums.MedicationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One dose of medicine given to one child at school, or not given.
 *
 * <p>This is the most serious record in the package. Giving a child medicine is
 * doing something to them, and if it goes wrong the school will be asked exactly
 * what was given, how much, by whom, at what time, and who said it could be. Every
 * one of those is a field here, and none of them is optional in practice.
 *
 * <p>Nothing may be given without a consent behind it. Either the profile carries a
 * standing ROUTINE_MEDICATION consent on the profile for ordinary things like paracetamol, or
 * this record points at a specific consent from a guardian. A dose with no consent
 * on file leaves the school with no defence at all.
 *
 * <p>A dose that was **not** given is written down too, which is why
 * {@code MedicationStatus} has three ways of not happening. A missing row and a
 * skipped dose look identical, and only one of them needs a phone call home. A
 * child whose lunchtime inhaler was missed has to be visible, not absent from the
 * record.
 *
 * <p>{@code medicineName} is free text rather than a code from a drug dictionary.
 * The reference sketch had a coded medication field; a school nurse writing down
 * that a mother sent in a bottle of cough syrup is never going to look up its code,
 * and a field nobody fills in is worse than no field. What matters is that the name,
 * the dose and the route are recorded exactly as they were given.
 *
 * <p>{@code administeredByStaffDocsId} is who physically gave it. That is a
 * different person from whoever authorised it, and keeping them apart is the point:
 * one is accountable for the decision, the other for the act.
 *
 * <p>Rows are never edited once saved. A mistake is corrected by adding a row that
 * explains it, the same rule the gate log follows, because a medicine record that
 * can be tidied up afterwards is worth nothing when it is needed.
 *
 * <p>Which consent was relied on is not a separate field. A null
 * {@code guardianConsentDocsId} means the standing consent on the profile was used, and a
 * boolean saying so beside it could disagree with the pointer next to it.
 *
 * <p>The service checks that a consent exists before any dose is recorded as GIVEN,
 * that the medicine does not appear in the child's ALLERGY alerts, that a status
 * other than GIVEN carries a reason, and that recording a dose needs the HEALTH
 * module.
 */
@Document(collection = "medication_administrations")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_student_medication_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'scheduledFor': -1}"),
        @CompoundIndex(
                name = "school_medication_day_idx",
                def = "{'schoolId': 1, 'scheduledFor': -1, 'status': 1}"),
        @CompoundIndex(
                name = "school_medication_visit_idx",
                def = "{'schoolId': 1, 'clinicVisitDocsId': 1}",
                partialFilter = "{'clinicVisitDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAdministration extends AcademicStudentSchoolBase {

    // The day the dose belonged to. Example: 2026-08-19
    @NotNull
    private LocalDate scheduledFor;

    // Links to ClinicVisit.id when the dose was given during a visit. Null for a
    // regular daily dose given without one.
    // Example: "67b71124dc3f7d0033445566"
    private String clinicVisitDocsId;

    // Name of the medicine exactly as it was given, written as it appears on the
    // bottle. Example: "Crocin 250mg syrup"
    @NotBlank
    private String medicineName;

    // How much was given. Example: "5 ml"
    @NotBlank
    private String dose;

    // How it was given. Example: MedicationRoute.ORAL
    @NotNull
    private MedicationRoute route;

    // Whether it was given, and if not, in which of the three ways.
    // Example: MedicationStatus.GIVEN
    @NotNull
    private MedicationStatus status;

    // When it should have been given. Example: 2026-08-19T07:30:00Z
    private Instant scheduledAt;

    // When it actually was. Null unless the status is GIVEN.
    // Example: 2026-08-19T07:34:00Z
    private Instant administeredAt;

    // Links to Staff.id for whoever physically gave it. A different person from
    // whoever authorised it. Example: "67aa15d9dc3f7d0044444444"
    private String administeredByStaffDocsId;

    // Links to Staff.id for whoever said it could be given.
    // Example: "67aa15d9dc3f7d0055555555"
    private String authorisedByStaffDocsId;

    // Links to GuardianConsent.id for permission to give this particular medicine, when
    // it is not covered by the standing ROUTINE_MEDICATION consent on the profile.
    // RECORD_SPECIFIC, purpose MEDICAL_TREATMENT, with the medicine and the dates written
    // into the consent's purposeDescription. Example: "67bf1124dc3f7d0033445566"
    private String guardianConsentDocsId;

    // Why it was not given. Required whenever the status is not GIVEN.
    // Example: "Child said she had already taken it at home."
    private String notGivenReason;

    // Anything worth knowing, including the explanation for a correcting row.
    // Example: "Mother sent the bottle in with a note; note scanned to the file."
    private String remarks;
}
