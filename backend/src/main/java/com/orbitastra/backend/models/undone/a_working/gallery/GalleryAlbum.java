package com.orbitastra.backend.models.undone.a_working.gallery;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.gallery.enums.GalleryEventType;
import com.orbitastra.backend.models.undone.a_working.gallery.enums.GalleryStatus;
import com.orbitastra.backend.models.undone.a_working.gallery.enums.GalleryVisibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "gallery_albums")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAlbum extends SchoolBase {

    /**
     * Annual Day 2027
     * Sports Day
     * Independence Day
     */
    private String title;

    /**
     * Small description shown below album.
     */
    private String description;

    /**
     * Optional reference to SchoolEvent.
     */
    private String eventDocsId;

    /**
     * Annual Day
     * Sports Day
     * Picnic
     */
    private GalleryEventType eventType;

    /**
     * Date on which event happened.
     */
    private LocalDate eventDate;

    /**
     * Academic Year
     * 2026-2027
     */
    private String academicYear;

    /**
     * Cover image URL
     */
    private String coverImageUrl;

    /**
     * Number of media files.
     */
    @Builder.Default
    private Integer mediaCount = 0;

    /**
     * Parents / Staff / Public
     */
    @Builder.Default
    private GalleryVisibility visibility = GalleryVisibility.PARENTS;

    /**
     * Draft / Published / Archived
     */
    @Builder.Default
    private GalleryStatus status = GalleryStatus.DRAFT;

    /**
     * Show on dashboard/homepage.
     */
    @Builder.Default
    private Boolean featured = false;
}