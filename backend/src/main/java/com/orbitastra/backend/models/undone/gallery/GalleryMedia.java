package com.orbitastra.backend.models.undone.gallery;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.gallery.enums.GalleryMediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "gallery_media")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryMedia extends SchoolBase {

    /**
     * Parent album.
     */
    private String albumDocsId;

    /**
     * IMAGE / VIDEO
     */
    private GalleryMediaType mediaType;

    /**
     * Original uploaded file.
     */
    private String mediaUrl;

    /**
     * Thumbnail for image/video.
     */
    private String thumbnailUrl;

    /**
     * Optional caption.
     */
    private String caption;

    /**
     * Display order.
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Size in bytes.
     */
    private Long fileSize;

    /**
     * Width in pixels.
     */
    private Integer width;

    /**
     * Height in pixels.
     */
    private Integer height;

    /**
     * Only for videos.
     */
    private Integer durationSeconds;

    /**
     * Original filename.
     */
    private String fileName;

    /**
     * Whether parents can download.
     */
    @Builder.Default
    private Boolean downloadable = true;
}