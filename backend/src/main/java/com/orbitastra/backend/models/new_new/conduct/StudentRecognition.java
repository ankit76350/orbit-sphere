package com.orbitastra.backend.models.new_new.conduct;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.conduct.enums.RecognitionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One time a child was recognised for something good.
 *
 * <p>It sits in the same package as discipline on purpose. A school that records only
 * what children do wrong ends up with a file on every child that reads like a charge
 * sheet, and the child who was kind every day for five years has nothing written down
 * at all. Both belong in the same place because both are the school's record of how a
 * child conducted themselves.
 *
 * <p>The old {@code academics/DisciplineLog} had no equivalent, which is why this is
 * new rather than a redesign.
 *
 * <p>{@code housePoints} is here rather than in its own model because a house points
 * system is a running total of recognitions, not a separate thing. Adding up this
 * column for a house over a term is the leaderboard, and no extra collection is needed
 * to hold a number that can be derived.
 *
 * <p>{@code issuedDocumentDocsId} links to a certificate when one was printed. The
 * documents package already numbers and verifies certificates, so nothing about
 * printing lives here.
 *
 * <p>{@code publicationConsent} matters more than it looks. Putting a child's name and
 * photograph on a noticeboard, a newsletter or a social media post needs the family's
 * agreement, and some families withhold it for reasons the school does not need to
 * know. Recording it means somebody can check before publishing rather than
 * afterwards.
 *
 * <p>The service checks that a recognition with points carries a positive number, that
 * publishing anything requires {@code publicationConsent} to be true, and that the
 * award date falls inside the academic year.
 */
@Document(collection = "student_recognitions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_recognition_no_uniq",
                def = "{'schoolId': 1, 'recognitionNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_recognition_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'awardedOn': -1}"),
        @CompoundIndex(
                name = "school_year_recognition_type_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'recognitionType': 1, 'awardedOn': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRecognition extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type STUDENT_RECOGNITION.
    // Example: "REC/2026/000512"
    @NotBlank
    private String recognitionNo;

    // What it is for. Example: RecognitionType.GOOD_CONDUCT
    @NotNull
    private RecognitionType recognitionType;

    // Short name, as it would appear on a certificate or a noticeboard.
    // Example: "Helping a younger child who had fallen"
    @NotBlank
    private String title;

    // What happened, in plain words.
    // Example: "Stayed with a Class II child who fell in the corridor and walked
    // her to the clinic instead of going to lunch."
    private String description;

    // The day it was awarded. Example: 2026-08-19
    @NotNull
    private LocalDate awardedOn;

    // Points towards the child's house, when the school runs a house system. The
    // house leaderboard is this column added up, so no separate model holds it.
    // Example: 5
    private Integer housePoints;

    // Links to Staff.id for whoever put the child forward.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String nominatedByStaffDocsId;

    // Links to Staff.id for whoever agreed it, where the school requires that.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // Example: 2026-08-19T09:00:00Z
    private Instant approvedAt;

    // Links to IssuedDocument.id when a certificate was printed.
    // Example: "67b51122dc3f7d0011223344"
    private String issuedDocumentDocsId;

    // Whether the family agreed to the child's name and photograph being used
    // outside the school. Nothing is published without this. Example: true
    @NotNull
    @Builder.Default
    private Boolean publicationConsent = false;

    // Links to DocumentRecord.id for the signed consent, where one was given.
    // Example: "67b81125dc3f7d0044556677"
    private String publicationConsentDocumentDocsId;

    // Anything worth knowing.
    // Example: "Read out in assembly; mother came to watch."
    private String remarks;
}
