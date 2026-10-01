package com.orbitastra.backend.models.crm.embedded;

import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One extra question a school asks on its application form, embedded in an AdmissionCycle.
 *
 * <p>It has no separate collection identity, the same as IntakeCapacity. The questions belong to
 * one admission round and nothing outside that round reads them, so they live on the cycle
 * instead of in a collection of their own.
 *
 * <p>The family's answer is saved in {@code AdmissionApplication.formAnswers}, which is a map
 * keyed by <b>the question itself</b> — the wording below, not the id. Decided 2026-10-01: the
 * answers are read by people far more often than they are joined on, and a map of ObjectIds to
 * answers cannot be read at all without the cycle open beside it.
 *
 * <p><b>What that costs:</b> reword a question and the answers already given are stored under the
 * old wording with nothing to match them to. Nothing checks the keys, so this is a convention
 * rather than a rule the API enforces.
 *
 * <p>Before this existed nothing could check the answers at all. The cycle used to point at a
 * form definition document through {@code applicationFormDefinitionDocsId}, but that model was
 * never built, so the field was removed on 2026-09-21 and the answers were stored with nothing
 * checking them. This is the smaller version of that idea: the questions themselves, kept with
 * the round that asks them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionFormQuestion {

    // MongoDB _id. Example: "67aa15d9dc3f7d0098765432"
    //
    // THE SERVICE HAS TO SET THIS. Only the id of the document being saved is filled in for us;
    // this one is inside a list, so nothing fills it in and it stays empty unless we make one.
    // Measured 2026-09-30 by saving a value with it left empty: nothing was written for it.
    //
    // WHAT IT IS FOR is editing the question, not storing the answer. #2 replaces the question
    // list whole, and a question sent back with this id is reworded in place rather than removed
    // and re-added. The ANSWER is keyed by the wording below -- changed 2026-10-01.
    @NotBlank
    @Id
    private String id;

    // What the family actually reads on the form, and the key its answer is saved under in
    // AdmissionApplication.formAnswers.
    // Example: "Which school did the child go to before?"
    //
    // REWORDING IT LOSES THE ANSWERS ALREADY GIVEN, because they are stored under the old wording
    // and nothing moves them. That is the price of a map a person can read; the id above is what
    // keeps the QUESTION itself identifiable while the wording changes.
    @NotBlank
    @Size(max = 500)
    private String question;

    // Whether the family has to answer it before the form can be sent.
    // Example: true
    //
    // Nothing checks this yet. It is the school's answer to "must they fill this in", and the
    // check that uses it belongs to the endpoint that submits a form, which is built later.
    @NotNull
    @Builder.Default
    private Boolean required = false;
}
