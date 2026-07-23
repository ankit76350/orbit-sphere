package com.orbitastra.backend.config;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import com.orbitastra.backend.models.academics.DailyTimetable;

import lombok.RequiredArgsConstructor;

/**
 * Migrates embedded timetable entry references after classId/teacherId were
 * renamed to classDocsId/teacherDocsId.
 */
@Component
@RequiredArgsConstructor
public class DailyTimetableFieldMigration {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DailyTimetableFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateDailyTimetableFields() {
        long migrated = migrateEntryReferences();
        if (migrated > 0) {
            log.info("Migrated classId/teacherId in {} DailyTimetable document(s) to classDocsId/teacherDocsId",
                    migrated);
        }
    }

    /**
     * MongoDB's $rename does not support array elements, so embedded entries
     * are migrated with a pipeline. Existing new fields always win when both
     * the old and new field are present.
     */
    private long migrateEntryReferences() {
        MongoCollection<Document> collection = mongoTemplate.getCollection(
                mongoTemplate.getCollectionName(DailyTimetable.class));

        Bson filter = Filters.or(
                Filters.elemMatch("entries", Filters.exists("classId", true)),
                Filters.elemMatch("entries", Filters.exists("teacherId", true)));

        Document migratedEntry = new Document("$mergeObjects", List.of(
                "$$entry",
                new Document("$cond", List.of(
                        new Document("$and", List.of(
                                new Document("$ne", List.of("$$entry.classId", null)),
                                new Document("$eq", List.of("$$entry.classDocsId", null)))),
                        new Document("classDocsId", "$$entry.classId"),
                        new Document())),
                new Document("$cond", List.of(
                        new Document("$and", List.of(
                                new Document("$ne", List.of("$$entry.teacherId", null)),
                                new Document("$eq", List.of("$$entry.teacherDocsId", null)))),
                        new Document("teacherDocsId", "$$entry.teacherId"),
                        new Document()))));

        Document entryMap = new Document("$map", new Document()
                .append("input", new Document("$ifNull", List.of("$entries", List.of())))
                .append("as", "entry")
                .append("in", migratedEntry));

        List<Bson> pipeline = List.of(
                new Document("$set", new Document("entries", entryMap)),
                new Document("$unset", List.of("entries.classId", "entries.teacherId")));

        UpdateResult result = collection.updateMany(filter, pipeline);
        return result.getModifiedCount();
    }
}
