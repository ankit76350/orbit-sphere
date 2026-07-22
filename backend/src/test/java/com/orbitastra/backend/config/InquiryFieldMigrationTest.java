package com.orbitastra.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.orbitastra.backend.models.crm.Inquiry;

@ExtendWith(MockitoExtension.class)
class InquiryFieldMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    private InquiryFieldMigration migration;

    @Test
    void migrateInquiryFields_renamesOnlyWhenNewFieldIsMissing() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Inquiry.class)))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        migration.migrateInquiryFields();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), eq(Inquiry.class));

        assertEquals(Document.parse(
                        "{\"$and\":[{\"counselorId\":{\"$exists\":true}},"
                                + "{\"counselorDocsId\":{\"$exists\":false}}]}"),
                queryCaptor.getValue().getQueryObject());
        assertEquals(Document.parse("{\"$rename\":{\"counselorId\":\"counselorDocsId\"}}"),
                updateCaptor.getValue().getUpdateObject());
    }
}
