package com.orbitastra.backend.models.undone.a_new.media;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "media_items")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_media_collection_sort_uniq",
                def = "{'tenantId':1,'mediaCollectionDocsId':1,'sortOrder':1}", unique = true),
        @CompoundIndex(name = "tenant_media_subject_idx",
                def = "{'tenantId':1,'recognizedSubjectPersonDocsIds':1,'capturedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem extends TenantScopedDocument {

    private String mediaCollectionDocsId;
    private Integer sortOrder;
    private String mediaType;
    private String originalStoredObjectDocsId;
    private String thumbnailStoredObjectDocsId;
    private String caption;
    private String altText;
    private String moderationStatus;
    private String visibility;
    private Boolean downloadable;
    private Instant capturedAt;

    @Builder.Default
    private List<String> recognizedSubjectPersonDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> publicationConsentRecordDocsIds = new ArrayList<>();
}
