package com.orbitastra.backend.models.undone.aihub;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A school's AI governance policy switches — the guardrails that require human
 * oversight before AI output is acted on. One document per school.
 */
@Document(collection = "ai_governance_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiGovernanceSetting extends BaseDocument {

    // Require human approval before AI-drafted remarks are released.
    @Builder.Default
    private Boolean remarksApproval = true;

    // Restrict the student chatbot to the sanctioned curriculum.
    @Builder.Default
    private Boolean botCurriculumBound = true;

    // Route AI-flagged at-risk students through human review.
    @Builder.Default
    private Boolean riskHumanReview = true;

    // Require a human to confirm AI-detected CCTV events.
    @Builder.Default
    private Boolean cctvHumanConfirm = true;

    // Apply data-minimisation to prompts sent to the model.
    @Builder.Default
    private Boolean dataMinimisation = true;

    // Hand off parent-bot conversations to a human on request.
    @Builder.Default
    private Boolean parentBotHandoff = true;
}
