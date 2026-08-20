package com.orbitastra.backend.models.new_new.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.health.embedded.HealthAlert;
import com.orbitastra.backend.models.new_new.health.enums.BloodGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What the school needs to know about one child's health, standing.
 *
 * <p>One per student, for as long as they are at the school. It is not tied to an
 * academic year: a peanut allergy does not end in March, and making somebody
 * re-enter it every April is how it goes missing in the year it matters.
 *
 * <p>This answers one question, and it has to answer it in seconds: **a child has
 * collapsed, what do we need to know and who do we ring.** Everything here is
 * shaped by that. It is not a medical history and the school is not a clinic.
 *
 * <p>{@code alerts} is the part staff actually read. Each one carries what to do
 * rather than only what is wrong, because "nut allergy" is no help at the moment it
 * counts. An alert at HIGH or LIFE_THREATENING severity appears wherever the child
 * is named, not only here, so a teacher taking a class on a trip cannot miss it.
 *
 * <p>{@code routineMedicineConsent} is the standing permission for the school to
 * give ordinary things like paracetamol. Without it the answer is no, whatever a
 * parent says on the phone, and every dose given has to point at a consent. A
 * school that gives a child medicine with nothing on file has no defence.
 *
 * <p>Health is the most private thing in this system. Free-text notes are kept
 * encrypted and only staff with the HEALTH module may read the record at all. The
 * alerts themselves are deliberately not encrypted: an alert locked away is an
 * alert nobody reads on a trip with no signal, and what protects those is who may
 * see the child, not a cipher.
 *
 * <p>The service checks that one profile exists per student, that an alert marked
 * LIFE_THREATENING carries a {@code whatToDo}, and that reading this record needs
 * the HEALTH module rather than plain student access.
 */
@Document(collection = "health_profiles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_health_profile_student_uniq",
                def = "{'schoolId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_health_profile_alert_idx",
                def = "{'schoolId': 1, 'alerts.severity': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile extends SchoolBase {

    // Links to Student.id. One profile per child, for as long as they are here.
    // Example: "67aa15d9dc3f7d0055555555"
    @Indexed
    @NotBlank
    private String studentDocsId;

    // Example: BloodGroup.O_POSITIVE
    @NotNull
    @Builder.Default
    private BloodGroup bloodGroup = BloodGroup.UNKNOWN;

    // Everything staff have to know, each with what to do about it.
    @Valid
    @Builder.Default
    private List<HealthAlert> alerts = new ArrayList<>();

    // Height in centimetres at the last check. Example: 142.5
    private Double heightCm;

    // Weight in kilograms at the last check. Example: 34.8
    private Double weightKg;

    // When the height and weight were last taken. Example: 2026-07-15
    private Instant measuredAt;

    // The family's own doctor, for the school to ring if it has to.
    // Example: "Dr Anita Rao, Sunrise Clinic, 022 2345 6789"
    private String familyDoctorContact;

    // Health insurance the family has, written down for a hospital to use.
    // Example: "Star Health, policy P/2024/889213"
    private String insuranceReference;

    // Longer standing notes from the nurse. Plain text, for the same reason as on a clinic
    // visit: the alerts beside it carry the real medical detail and are deliberately
    // unencrypted, so encrypting this alone would be inconsistent rather than safer.
    // Example: "Mother prefers to be called before any medicine is given, even paracetamol."
    private String nurseNotes;

    // Links to DocumentRecord.id for a doctor's letter or a care plan the family
    // has given. Example: "67b71122dc3f7d0011223344"
    private String carePlanDocumentDocsId;

    // Whether the school may give ordinary medicines such as paracetamol without
    // ringing first. When false the answer is no, whatever is said on the phone.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean routineMedicineConsent = false;

    // Links to DocumentRecord.id for the signed consent form.
    // Example: "67b71123dc3f7d0022334455"
    private String routineMedicineConsentDocumentDocsId;

    // When the consent was given. Example: 2026-04-05T06:30:00Z
    private Instant routineMedicineConsentAt;

    // Links to the staff identity that last checked this record against what the
    // family gave. Example: "67aa15d9dc3f7d0044444444"
    private String verifiedByDocsId;

    // When it was last checked. A profile nobody has looked at for two years is
    // worth re-asking about. Example: 2026-04-10T05:00:00Z
    private Instant verifiedAt;
}
