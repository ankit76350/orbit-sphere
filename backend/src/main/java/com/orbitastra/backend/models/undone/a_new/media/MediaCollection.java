package com.orbitastra.backend.models.undone.a_new.media;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "media_collections")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_media_collection_slug_uniq",
                def = "{'tenantId':1,'slug':1}", unique = true),
        @CompoundIndex(name = "tenant_media_status_event_idx",
                def = "{'tenantId':1,'status':1,'eventDate':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCollection extends AcademicScopedDocument {

    private String slug;
    private String title;
    private String description;
    private String eventType;
    private String linkedEntityType;
    private String linkedEntityDocsId;
    private LocalDate eventDate;
    private String visibility;
    private String status;
    private String coverMediaItemDocsId;
    private Boolean featured;
    private String publicationWorkflowRunDocsId;
}
