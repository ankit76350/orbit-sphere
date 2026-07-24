package com.orbitastra.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

class StudentFieldMigrationTest {

    @Test
    void embeddedRename_movesLegacyValueAndPreservesExistingTarget() {
        Document legacy = new Document("studentId", "student-1");
        Document alreadyMigrated = new Document("studentId", "legacy-value")
                .append("studentDocsId", "current-value");
        List<Document> assignments = new ArrayList<>(
                List.of(legacy, alreadyMigrated));

        boolean changed = ProjectFieldNamingMigration.renameInEmbeddedValues(
                assignments, "studentId", "studentDocsId");

        assertTrue(changed);
        assertFalse(legacy.containsKey("studentId"));
        assertEquals("student-1", legacy.getString("studentDocsId"));
        assertFalse(alreadyMigrated.containsKey("studentId"));
        assertEquals("current-value", alreadyMigrated.getString("studentDocsId"));
    }

    @Test
    void embeddedRename_isIdempotent() {
        List<Document> assignments = new ArrayList<>(
                List.of(new Document("studentDocsId", "student-1")));

        assertFalse(ProjectFieldNamingMigration.renameInEmbeddedValues(
                assignments, "studentId", "studentDocsId"));
    }
}
