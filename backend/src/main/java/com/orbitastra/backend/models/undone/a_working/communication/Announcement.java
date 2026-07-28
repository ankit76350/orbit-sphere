package com.orbitastra.backend.models.undone.a_working.communication;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.communication.enums.AudienceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "announcements")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Announcement extends SchoolBase {

    /**
     * Announcement title.
     */
    private String title;

    /**
     * Announcement body/content.
     */
    private String content;

    /**
     * Optional attachment.
     */
    private String attachmentUrl;

    /**
     * Target audience type.
     */
    private AudienceType audienceType;

    /**
     * Used when audienceType is
     * GRADE / CLASS / SECTION / CUSTOM.
     *
     * Examples:
     * Grade 10
     * Class A
     * Staff IDs
     */
    private List<String> audienceValues;

    /**
     * Start displaying announcement.
     */
    @Indexed
    private LocalDateTime publishFrom;

    /**
     * Hide announcement after this time.
     */
    @Indexed
    private LocalDateTime publishUntil;

    /**
     * Whether announcement is pinned.
     */
    private Boolean pinned;

    /**
     * Active / inactive.
     */
    @Indexed
    private Boolean active;

    /**
     * Staff/Admin who created it.
     */
    private String createdByDocsId;
}
