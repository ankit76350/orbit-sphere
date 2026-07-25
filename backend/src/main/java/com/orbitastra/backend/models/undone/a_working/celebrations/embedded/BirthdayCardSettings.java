package com.orbitastra.backend.models.undone.a_working.celebrations.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayCardSettings {

    /**
     * Automatically generate birthday cards.
     */
    @Builder.Default
    private Boolean autoGenerate = true;

    /**
     * Allow users to create custom birthday cards.
     */
    @Builder.Default
    private Boolean allowCustomCards = true;

    /**
     * Default birthday card template.
     */
    private String defaultTemplateDocsId;
}