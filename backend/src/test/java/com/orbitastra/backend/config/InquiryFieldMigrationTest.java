package com.orbitastra.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;

@ExtendWith(MockitoExtension.class)
class InquiryFieldMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    private ProjectFieldNamingMigration migration;

    @Test
    void inquiryCounselorRename_isIdempotentAndNonOverwriting() {
        when(mongoTemplate.updateMulti(
                any(Query.class), any(Update.class), eq("inquiries")))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        migration.renameWhenTargetMissing(
                new ProjectFieldNamingMigration.FieldRename(
                        "inquiries", "counselorId", "counselorDocsId"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), eq("inquiries"));

        assertEquals(new Document("$and", List.of(
                new Document("counselorId", new Document("$exists", true)),
                new Document("counselorDocsId", new Document("$exists", false)))),
                queryCaptor.getValue().getQueryObject());
        assertEquals(new Document("$rename", new Document(
                "counselorId", "counselorDocsId")),
                updateCaptor.getValue().getUpdateObject());
    }
}
