package com.orbitastra.backend.models.new_new.academics.examination.embedded;

import java.util.ArrayList;
import java.util.List;

import com.orbitastra.backend.models.new_new.academics.enums.HpcLevel;
import com.orbitastra.backend.models.new_new.academics.enums.LearningDomain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a teacher has to say about one child in one area of their development.
 *
 * <p>It has no collection of its own. The domains of a card are always read together with it,
 * and a card has five to nine of them.
 *
 * <p>{@code observation} is the field that carries the whole value of a Holistic Progress
 * Card, and {@code level} is almost a footnote beside it. A level on its own says no more than
 * a grade did: "RIVER in Language and Literacy" tells a parent nothing they can act on.
 * "Reads aloud confidently now and asks what new words mean, though writing more than a few
 * lines still tires her" is why the card exists.
 *
 * <p>So the service requires an observation on every domain and treats a card of bare levels
 * as unfinished. A school that fills in only the levels has recreated a report card with
 * nicer words, which is exactly what this is meant to replace.
 *
 * <p>{@code evidenceDocumentDocsIds} is what stops an observation being an opinion. A
 * photograph of a model the child built, a scan of a page of their writing, a recording of
 * them reading — these are what a parent can see for themselves and what makes the same card
 * useful to next year's teacher.
 *
 * <p>{@code strengths} and {@code nextStep} are kept apart from the observation on purpose. An
 * observation describes; a next step commits the school to something. Merging them lets a card
 * describe a child warmly for three terms without anybody ever saying what will be done
 * differently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainAssessment {

    // Which area of development this is about.
    // Example: LearningDomain.LANGUAGE_AND_LITERACY
    @NotNull
    private LearningDomain domain;

    // Where the child is on the way. Almost a footnote beside the observation.
    // Example: HpcLevel.RIVER
    @NotNull
    private HpcLevel level;

    // What the teacher actually saw, in plain words a parent can act on. Required: a card
    // of bare levels is a report card with nicer words.
    // Example: "Reads aloud confidently now and asks what new words mean, though writing
    // more than a few lines still tires her."
    @NotBlank
    private String observation;

    // What the child is good at here, said plainly so it can be read back to them.
    // Example: "Remembers stories in detail and retells them to the class."
    private String strengths;

    // What the school will do differently next term. Kept apart from the observation
    // because describing a child is not the same as committing to anything.
    // Example: "Short daily writing tasks with a picture prompt rather than a blank page."
    private String nextStep;

    // Links to DocumentRecord.id for anything that shows this rather than asserts it: a
    // photograph of the child's work, a scan of their writing, a recording of them reading.
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
