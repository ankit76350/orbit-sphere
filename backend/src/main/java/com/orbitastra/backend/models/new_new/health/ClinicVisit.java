package com.orbitastra.backend.models.new_new.health;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.health.embedded.GuardianInformed;
import com.orbitastra.backend.models.new_new.health.enums.ClinicVisitOutcome;

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
 * One time a child came to the school nurse.
 *
 * <p>The profile is what is always true about a child; this is what happened on
 * Tuesday. A child with asthma has one profile and, over five years, perhaps forty
 * of these.
 *
 * <p>It replaces the old {@code academics/MedicalRecord}, which held a diagnosis, a
 * list of medicines and a doctor's name, and nothing about whether the parents were
 * told or what happened to the child afterwards. Those two gaps are the ones a
 * school gets asked about.
 *
 * <p>{@code outcome} is the field a parent asks about and the one that decides
 * whether anybody else needs telling. SENT_HOME and worse all mean a guardian must
 * be contacted, which is why {@code guardiansInformed} sits next to it. A child sent
 * home with nobody recorded as having been told is the situation this model exists to
 * make visible.
 *
 * <p>{@code guardiansInformed} is a list because ringing one guardian often is not
 * the end of it. The nurse tries the mother, gets no answer, tries the father,
 * reaches him. There is no separate "was anybody told" flag: a flag could be set true
 * with the list empty, which is exactly the claim nobody should be able to make. An
 * empty list is the only way of saying nobody knows.
 *
 * <p>{@code studentOutPassDocsId} links a child sent home to the gate record that
 * let them out. Being sent home ill and being collected early are the same event
 * seen from two places, and joining them means "who took my child and when" has one
 * answer rather than two half-answers.
 *
 * <p>Being sent home ill is not the same as being absent, and this model does not
 * touch attendance. The child was present in the morning. How a half day is
 * recorded belongs to the attendance models.
 *
 * <p>{@code encryptedNotes} holds what the nurse actually wrote. It is encrypted
 * because it is the most private text the school stores, and it is free text
 * because a school nurse is not going to code symptoms against a medical
 * dictionary. The reference sketch had coded symptom and diagnosis lists; a school
 * clinic will never fill them in, and a field nobody fills in is worse than no
 * field.
 *
 * <p>The service checks that an outcome of SENT_HOME or worse has a guardian
 * recorded as informed, that the visit falls inside the academic year, and that
 * reading a visit needs the HEALTH module.
 */
@Document(collection = "clinic_visits")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_clinic_visit_no_uniq",
                def = "{'schoolId': 1, 'visitNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_clinic_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'visitDate': -1}"),
        @CompoundIndex(
                name = "school_clinic_day_idx",
                def = "{'schoolId': 1, 'visitDate': -1, 'outcome': 1}"),
        @CompoundIndex(
                name = "school_clinic_guardian_idx",
                def = "{'schoolId': 1, 'guardiansInformed.guardianDocsId': 1, 'visitDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicVisit extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type CLINIC_VISIT, so a visit can
    // be quoted to a parent or a doctor. Example: "CV/2026/000318"
    @NotBlank
    private String visitNo;

    // The day of the visit. Example: 2026-08-19
    @NotNull
    private LocalDate visitDate;

    // When the child arrived at the clinic. Example: 2026-08-19T05:20:00Z
    @NotNull
    private Instant arrivedAt;

    // When they left it. Null while they are still there.
    // Example: 2026-08-19T05:55:00Z
    private Instant leftAt;

    // What the child said was wrong, in their words or the teacher's.
    // Example: "Stomach pain since the morning break."
    @NotBlank
    private String reportedComplaint;

    // What the nurse saw and measured, encrypted before saving. The most private
    // text the school keeps. Example: "enc:v1:3f2e1d0c9b8a7654"
    private String encryptedNotes;

    // Temperature in degrees Celsius, when it was taken. Example: 38.2
    private Double temperatureCelsius;

    // What was done. Example: "Rested for 30 minutes, given water."
    private String treatmentGiven;

    // How the visit ended. Example: ClinicVisitOutcome.SENT_HOME
    @NotNull
    private ClinicVisitOutcome outcome;

    // Links to Staff.id for the nurse or teacher who saw the child.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String attendedByStaffDocsId;

    // Links to Staff.id for whoever sent the child to the clinic.
    // Example: "67aa15d9dc3f7d0077777777"
    private String referredByStaffDocsId;

    // Every guardian who was told, each with the time they were told. Empty means
    // nobody has been told yet, which must not stay true for an outcome of
    // SENT_HOME or worse. Guardians who could not be reached are not listed here;
    // those attempts go in remarks.
    @Valid
    @Builder.Default
    private List<GuardianInformed> guardiansInformed = new ArrayList<>();

    // Links to StudentOutPass.id when the child was collected and taken home.
    // Example: "67b61126dc3f7d0055667788"
    private String studentOutPassDocsId;

    // Where the child was sent on to, when they were.
    // Example: "Sunrise Clinic, with the mother."
    private String referredTo;

    // Anything worth knowing.
    // Example: "Second stomach complaint this week; mentioned to the mother."
    private String remarks;
}
