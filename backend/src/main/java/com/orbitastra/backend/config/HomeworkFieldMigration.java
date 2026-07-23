package com.orbitastra.backend.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.academics.Homework;

import lombok.RequiredArgsConstructor;

/**
 * Keeps existing homework documents readable after the reference fields were
 * made explicit. The migration is idempotent and never overwrites a value
 * already written under the new field name.
 */
@Component
@RequiredArgsConstructor
public class HomeworkFieldMigration {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(HomeworkFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateHomeworkFields() {
        long classCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Homework.class, "classId", "classDocsId");
        long teacherCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Homework.class, "teacherId", "teacherDocsId");

        if (classCount > 0 || teacherCount > 0) {
            log.info("Migrated Homework references: classDocsId={}, teacherDocsId={}",
                    classCount, teacherCount);
        }
    }
}
