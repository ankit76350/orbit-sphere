package com.orbitastra.backend.models.undone.aihub;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.undone.aihub.enums.AiPersona;
import com.orbitastra.backend.models.undone.aihub.enums.AiTier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token-usage meter for one AI copilot persona at a school — how much of the
 * tier's quota has been consumed.
 */
@Document(collection = "ai_usage")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage extends BaseDocument {

    private AiPersona persona;

    private String label;

    // Tokens consumed against the quota.
    @Builder.Default
    private Long usedTokens = 0L;

    private Long quota;

    private AiTier tier;

    private String description;
}
