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
import com.orbitastra.backend.models.staff.Staff;

@ExtendWith(MockitoExtension.class)
class StaffFieldMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    private StaffFieldMigration migration;

    @Test
    void migrateEmployeeNumber_renamesOnlyWhenNewFieldIsMissing() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Staff.class)))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        migration.migrateEmployeeNumber();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), eq(Staff.class));

        assertEquals(Document.parse(
                "{\"$and\":[{\"employeeId\":{\"$exists\":true}},"
                        + "{\"employeeNo\":{\"$exists\":false}}]}"),
                queryCaptor.getValue().getQueryObject());
        assertEquals(Document.parse("{\"$rename\":{\"employeeId\":\"employeeNo\"}}"),
                updateCaptor.getValue().getUpdateObject());
    }
}
