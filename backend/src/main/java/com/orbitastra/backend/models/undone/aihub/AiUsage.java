package com.orbitastra.backend.models.undone.aihub;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private AiPersona persona;

    private String label;

    // Tokens consumed against the quota.
    @Builder.Default
    private Long usedTokens = 0L;

    private Long quota;

    private AiTier tier;

    private String description;
}
