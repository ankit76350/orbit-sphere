package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "diary_entries")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_course_diary_publish_idx",
                def = "{'tenantId':1,'courseOfferingDocsId':1,'publishedAt':-1}"),
        @CompoundIndex(name = "tenant_student_diary_publish_idx",
                def = "{'tenantId':1,'studentDocsIds':1,'publishedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntry extends AcademicScopedDocument {

    private String entryType;
    private String courseOfferingDocsId;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String subjectNodeDocsId;
    private String authorDocsId;
    private String title;
    private String content;
    private Instant publishedAt;
    private Instant acknowledgementDueAt;

    @Builder.Default
    private List<String> studentDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> attachmentDocumentDocsIds = new ArrayList<>();
}
