package com.orbitastra.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
import com.orbitastra.backend.models.student.Student;

@ExtendWith(MockitoExtension.class)
class StudentFieldMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    private StudentFieldMigration migration;

    @Test
    void migrateStudentFields_usesNonOverwritingIdempotentUpdates() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Student.class)))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        migration.migrateStudentFields();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate, times(4)).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), eq(Student.class));

        List<Query> queries = queryCaptor.getAllValues();
        List<Update> updates = updateCaptor.getAllValues();

        assertEquals(Document.parse("{\"$and\":[{\"walletId\":{\"$exists\":true}},{\"walletDocsId\":{\"$exists\":false}}]}"),
                queries.get(0).getQueryObject());
        assertEquals(Document.parse("{\"$rename\":{\"walletId\":\"walletDocsId\"}}"),
                updates.get(0).getUpdateObject());

        assertEquals(Document.parse("{\"$and\":[{\"medicalRecordId\":{\"$exists\":true}},{\"medicalRecordDocsId\":{\"$exists\":false}}]}"),
                queries.get(1).getQueryObject());
        assertEquals(Document.parse("{\"$rename\":{\"medicalRecordId\":\"medicalRecordDocsId\"}}"),
                updates.get(1).getUpdateObject());

        assertEquals(new Document("documents", null),
                queries.get(2).getQueryObject());
        assertEquals(new Document("$set", new Document("documents", List.of())),
                updates.get(2).getUpdateObject());

        assertEquals(new Document("medicalRemark", null),
                queries.get(3).getQueryObject());
        assertEquals(new Document("$set", new Document("medicalRemark", List.of())),
                updates.get(3).getUpdateObject());
    }
}
