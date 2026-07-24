package com.orbitastra.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.staff.StudentReview;
import com.orbitastra.backend.models.staff.TeacherPerformanceReview;
import com.orbitastra.backend.models.staff.TeacherReview;

@ExtendWith(MockitoExtension.class)
class StaffFieldMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    private StaffFieldMigration migration;

    @Test
    void migrateStaffFields_usesNonOverwritingIdempotentRenames() {
        when(mongoTemplate.updateMulti(
                any(Query.class), any(Update.class), any(Class.class)))
                .thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        migration.migrateStaffFields();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ArgumentCaptor<Class<?>> typeCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Class.class);
        verify(mongoTemplate, times(11)).updateMulti(
                queryCaptor.capture(), updateCaptor.capture(), typeCaptor.capture());

        assertEquals(List.of(
                Staff.class,
                StudentReview.class, StudentReview.class, StudentReview.class,
                TeacherPerformanceReview.class, TeacherPerformanceReview.class,
                TeacherPerformanceReview.class,
                TeacherReview.class, TeacherReview.class, TeacherReview.class, TeacherReview.class),
                typeCaptor.getAllValues());

        assertRename(queryCaptor.getAllValues().get(0), updateCaptor.getAllValues().get(0),
                "employeeId", "employeeNo");
        assertRename(queryCaptor.getAllValues().get(1), updateCaptor.getAllValues().get(1),
                "studentId", "studentDocsId");
        assertRename(queryCaptor.getAllValues().get(2), updateCaptor.getAllValues().get(2),
                "teacherId", "teacherDocsId");
        assertRename(queryCaptor.getAllValues().get(3), updateCaptor.getAllValues().get(3),
                "reviewCycleId", "reviewCycleDocsId");
        assertRename(queryCaptor.getAllValues().get(4), updateCaptor.getAllValues().get(4),
                "teacherId", "teacherDocsId");
        assertRename(queryCaptor.getAllValues().get(5), updateCaptor.getAllValues().get(5),
                "reviewerId", "reviewerDocsId");
        assertRename(queryCaptor.getAllValues().get(6), updateCaptor.getAllValues().get(6),
                "reviewCycleId", "reviewCycleDocsId");
        assertRename(queryCaptor.getAllValues().get(7), updateCaptor.getAllValues().get(7),
                "teacherId", "teacherDocsId");
        assertRename(queryCaptor.getAllValues().get(8), updateCaptor.getAllValues().get(8),
                "studentId", "studentDocsId");
        assertRename(queryCaptor.getAllValues().get(9), updateCaptor.getAllValues().get(9),
                "parentId", "parentDocsId");
        assertRename(queryCaptor.getAllValues().get(10), updateCaptor.getAllValues().get(10),
                "reviewCycleId", "reviewCycleDocsId");
    }

    private void assertRename(Query query, Update update, String source, String target) {
        assertEquals(new Document("$and", List.of(
                new Document(source, new Document("$exists", true)),
                new Document(target, new Document("$exists", false)))),
                query.getQueryObject());
        assertEquals(new Document("$rename", new Document(source, target)),
                update.getUpdateObject());
    }
}
