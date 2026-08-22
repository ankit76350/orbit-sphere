package com.orbitastra.backend.models.gallery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.gallery.enums.GalleryMediaType;
import com.orbitastra.backend.models.gallery.enums.GalleryStatus;
import com.orbitastra.backend.models.gallery.enums.GalleryVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One photograph or video clip.
 *
 * <p>
 * The file itself is not here. {@code documentRecordDocsId} points at a
 * DocumentRecord,
 * which already owns private object storage and hands out short-lived signed
 * URLs after
 * checking who is asking. The reference sketch kept a {@code mediaUrl} string
 * instead, and a
 * URL sitting in a database is a link that either leaks or rots: leaks because
 * a public URL to
 * a child's photograph needs no permission to open, rots because storage moves.
 *
 * <p>
 * **{@code taggedStudentDocsIds} is the point of this model.** It lists the
 * children who can
 * be identified in the picture, and it is what makes consent enforceable.
 * Without it, a school
 * publishing an album has no way to know whose families said yes, and "we asked
 * everybody at
 * admission" is not an answer when one family says they refused.
 *
 * <p>
 * So publishing checks each tagged child's PHOTOGRAPH_AND_MEDIA consent in
 * GuardianConsent, and
 * {@code consentVerifiedAt} records that the check happened rather than that
 * somebody meant to
 * do it. A picture with an unconsented child in it is not published; it is
 * cropped, or it stays
 * at STAFF visibility, or it is left out.
 *
 * <p>
 * The harder half is **withdrawal after publication.** A family who agreed in
 * April may
 * change their mind in November, and they are entitled to. When a consent is
 * withdrawn,
 * everything tagging that child has to be found and reviewed, which is exactly
 * the query
 * {@code taggedStudentDocsIds} makes possible and nothing else does. That is
 * why the tag list
 * is indexed.
 *
 * <p>
 * {@code visibility} sits on the media as well as the album because one album
 * usually holds
 * a picture that is fine for the public website and another that is not. The
 * narrower of the
 * two wins: a PUBLIC album cannot make a STAFF photograph public.
 *
 * <p>
 * {@code altText} is not decoration. A parent using a screen reader, or on a
 * slow
 * connection, gets nothing at all from a photograph without it.
 *
 * <p>
 * {@code downloadable} matters more for a school than for most galleries.
 * Families want to
 * keep pictures of their own child, and the same setting is what stops a
 * stranger bulk-saving
 * a class.
 *
 * <p>
 * The service checks that publishing verifies consent for every tagged child,
 * that the
 * effective visibility is the narrower of media and album, that withdrawing a
 * child's consent
 * pulls or re-reviews everything tagging them, and that the album's count is
 * kept in step.
 */
@Document(collection = "gallery_media")
@CompoundIndexes({
                @CompoundIndex(name = "school_media_album_order_idx", def = "{'schoolId': 1, 'galleryAlbumDocsId': 1, 'status': 1, 'sortOrder': 1}"),
                @CompoundIndex(name = "school_media_tagged_student_idx", def = "{'schoolId': 1, 'taggedStudentDocsIds': 1, 'status': 1}"),
                @CompoundIndex(name = "school_media_consent_review_idx", def = "{'schoolId': 1, 'status': 1, 'consentVerifiedAt': 1}"),
                @CompoundIndex(name = "school_media_document_uniq", def = "{'schoolId': 1, 'documentRecordDocsId': 1}", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryMedia extends SchoolBase {

        // Links to GalleryAlbum.id. Example: "67c11123dc3f7d0022334455"
        @NotBlank
        private String galleryAlbumDocsId;

        // Links to DocumentRecord.id for the file. Never a URL: documents already owns
        // private
        // storage and short-lived signed links. Example: "67c11124dc3f7d0033445566"
        @NotBlank
        private String documentRecordDocsId;

        // Links to DocumentRecord.id for a smaller version, so a gallery grid does not
        // download
        // forty full-size photographs. Example: "67c11125dc3f7d0044556677"
        private String thumbnailDocumentDocsId;

        // Example: GalleryMediaType.IMAGE
        @NotNull
        private GalleryMediaType mediaType;

        // Links to Student.id for every child who can be identified in this picture.
        // What makes
        // consent enforceable, and the only way to find everything tagging a child when
        // their
        // family changes their mind. Indexed for exactly that.
        @Builder.Default
        private List<String> taggedStudentDocsIds = new ArrayList<>();

        // When somebody last checked that every tagged child has a granted photograph
        // consent.
        // Records that the check happened, not that somebody meant to do it. Null means
        // never
        // checked, and never checked means never publishable. Example:
        // 2026-12-16T06:20:00Z
        private Instant consentVerifiedAt;

        // Links to Staff.id for whoever verified the consents.
        // Example: "67aa15d9dc3f7d0055555555"
        private String consentVerifiedByStaffDocsId;

        // Who may see this particular file. The narrower of this and the album's wins.
        // Example: GalleryVisibility.PARENTS
        @NotNull
        @Builder.Default
        private GalleryVisibility visibility = GalleryVisibility.STAFF;

        // Example: GalleryStatus.PUBLISHED
        @NotNull
        @Builder.Default
        private GalleryStatus status = GalleryStatus.DRAFT;

        // What is happening in the picture, shown under it.
        // Example: "Class IV relay team after the final."
        private String caption;

        // A description for anybody who cannot see the image: a screen reader, or a
        // slow
        // connection. Not decoration.
        // Example: "Four children in blue house vests holding a trophy."
        private String altText;

        // Order within the album. Example: 12
        @NotNull
        @Builder.Default
        private Integer sortOrder = 0;

        // Whether a family may save a copy. Families want pictures of their own child;
        // the same
        // setting is what stops a stranger bulk-saving a class. Example: true
        @NotNull
        @Builder.Default
        private Boolean downloadable = true;

        // When the photograph was taken, which is not when it was uploaded.
        // Example: 2026-12-14T09:12:00Z
        private Instant capturedAt;

        // Why it was taken down. Required when the status is WITHDRAWN.
        // Example: "Consent withdrawn for a child in the front row."
        private String withdrawalReason;
}
