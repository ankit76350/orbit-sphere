package com.orbitastra.backend.models.new_new.gallery;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.gallery.enums.GalleryEventType;
import com.orbitastra.backend.models.new_new.gallery.enums.GalleryStatus;
import com.orbitastra.backend.models.new_new.gallery.enums.GalleryVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One occasion's photographs, gathered together.
 *
 * <p>
 * Sports Day 2026. The Class V trip to the planetarium. Diwali assembly.
 * Parents open this
 * more often than anything else in the app, and it is the cheapest thing a
 * school can do that
 * families genuinely value.
 *
 * <p>
 * {@code visibility} is the field that matters most, because it decides what
 * consent is
 * needed. The same photograph shown to the child's own parents and put on the
 * school's public
 * website are not the same act. PUBLIC is the one to be careful with: once a
 * picture of a child
 * is on an open page it can be copied, indexed and kept by anybody, and taking
 * it down later
 * does not undo that.
 *
 * <p>
 * An album is a container and holds no files. The photographs are GalleryMedia
 * rows, and
 * each one carries its own visibility and its own consent position, because one
 * album often
 * has a picture that is fine for the public website and another that is not.
 *
 * <p>
 * {@code coverMediaDocsId} points at one of its own media rather than holding a
 * separate
 * image, so a cover cannot outlive the picture it came from. If that photograph
 * is withdrawn
 * because a family changed their mind, the cover has to change too, and
 * pointing at the row
 * rather than copying the file is what makes that automatic.
 *
 * <p>
 * {@code mediaCount} is a running total kept so a list of albums loads without
 * counting
 * every photograph. It counts only PUBLISHED media, because a parent looking at
 * "Sports Day
 * (48)" and finding twelve pictures has been told something untrue.
 *
 * <p>
 * Publishing is not the uploader's decision. Photographs of children should not
 * go up
 * because one person had a camera, so an album waits at PENDING_APPROVAL for
 * somebody else to
 * look. Whoever published it is recorded, because that is the person who will
 * be asked.
 *
 * <p>
 * The service checks that publishing verifies consent for every child
 * identifiable in the
 * album's media, that the cover belongs to this album, that the count matches
 * the published
 * media, and that a withdrawal carries a reason.
 */
@Document(collection = "gallery_albums")
@CompoundIndexes({
                @CompoundIndex(name = "school_album_visible_idx", def = "{'schoolId': 1, 'status': 1, 'visibility': 1, 'eventDate': -1}"),
                @CompoundIndex(name = "school_year_album_idx", def = "{'schoolId': 1, 'academicYear': 1, 'eventDate': -1}"),
                @CompoundIndex(name = "school_album_event_type_idx", def = "{'schoolId': 1, 'eventType': 1, 'eventDate': -1}"),
                @CompoundIndex(name = "school_album_featured_idx", def = "{'schoolId': 1, 'featured': 1, 'eventDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAlbum extends SchoolBase {

        // What parents see as the album name. Example: "Sports Day 2026"
        @NotBlank
        private String title;

        // A sentence or two about the occasion.
        // Example: "Inter-house athletics at the district ground, 14 December."
        private String description;

        // What sort of occasion it was, for the filter parents actually use.
        // Example: GalleryEventType.SPORTS_DAY
        @NotNull
        private GalleryEventType eventType;

        // The day it happened, which is not the day the photographs were uploaded.
        // Albums are ordered by this. Example: 2026-12-14
        @NotNull
        private LocalDate eventDate;

        // Links to AcademicYear.name, so a family can look back at a whole year.
        // Example: "2026-2027"
        @Indexed
        @NotBlank
        private String academicYear;

        // Who may see it, and therefore what consent is needed.
        // Example: GalleryVisibility.PARENTS
        @NotNull
        @Builder.Default
        private GalleryVisibility visibility = GalleryVisibility.STAFF;

        // Example: GalleryStatus.PUBLISHED
        @NotNull
        @Builder.Default
        private GalleryStatus status = GalleryStatus.DRAFT;

        // Links to GalleryMedia.id inside this album, used as the cover. Pointing at
        // the row
        // rather than copying the file means a withdrawn photograph cannot go on being
        // the
        // cover. Example: "67c11122dc3f7d0011223344"
        private String coverMediaDocsId;

        // How many PUBLISHED photographs are in it. A running total, so a list of
        // albums does
        // not count everything. Never includes drafts: "Sports Day (48)" that opens to
        // twelve
        // has told the parent something untrue. Example: 48
        @NotNull
        @Builder.Default
        private Integer mediaCount = 0;

        // Whether it sits at the top of the app for a while. Example: false
        @NotNull
        @Builder.Default
        private Boolean featured = false;

        // Links to SchoolClass.id when the album is about one class rather than the
        // school,
        // such as a class trip. Null for a whole-school occasion.
        // Example: "67ab3322dc3f7d0044556677"
        private String classDocsId;

        // Links to Staff.id for whoever put the album together.
        // Example: "67aa15d9dc3f7d0044444444"
        @NotBlank
        private String createdByStaffDocsId;

        // Links to Staff.id for whoever approved it going up. Never the uploader:
        // photographs
        // of children should not be published because one person had a camera.
        // Example: "67aa15d9dc3f7d0055555555"
        private String publishedByStaffDocsId;

        // Example: 2026-12-16T06:30:00Z
        private Instant publishedAt;

        // Why it was taken down. Required when the status is WITHDRAWN.
        // Example: "A family withdrew photograph consent; album pulled pending review."
        private String withdrawalReason;
}
