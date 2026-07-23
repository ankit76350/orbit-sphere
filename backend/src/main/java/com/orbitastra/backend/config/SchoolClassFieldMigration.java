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
import com.orbitastra.backend.models.academics.SchoolClass;

import lombok.RequiredArgsConstructor;

/**
 * Migrates the teacher references in existing class documents after the
 * classTeacher was renamed to classTeacherDocsId and the nested subject
 * teacher field was renamed to teacherDocsId.
 */
@Component
@RequiredArgsConstructor
public class SchoolClassFieldMigration {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SchoolClassFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateSchoolClassFields() {
        long classTeacherCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, SchoolClass.class, "classTeacher", "classTeacherDocsId");
        long subjectTeacherCount = migrateSubjectTeachers();

        if (classTeacherCount > 0 || subjectTeacherCount > 0) {
            log.info("Migrated SchoolClass teacher references: classTeacherDocsId={}, subjects.teacherDocsId={}",
                    classTeacherCount, subjectTeacherCount);
        }
    }

    /**
     * MongoDB's $rename does not support array elements, so subject teacher
     * references are moved with an idempotent update pipeline. Existing
     * teacherDocsId values always win when both fields are present.
     */
    private long migrateSubjectTeachers() {
        MongoCollection<Document> collection = mongoTemplate.getCollection(
                mongoTemplate.getCollectionName(SchoolClass.class));

        Bson filter = Filters.elemMatch("subjects", Filters.exists("teacher", true));
        Document subjectCondition = new Document("$and", List.of(
                new Document("$ne", List.of("$$subject.teacher", null)),
                new Document("$eq", List.of("$$subject.teacherDocsId", null))));
        Document migratedSubject = new Document("$mergeObjects", List.of(
                "$$subject",
                new Document("teacherDocsId", "$$subject.teacher")));
        Document subjectMap = new Document("$map", new Document()
                .append("input", new Document("$ifNull", List.of("$subjects", List.of())))
                .append("as", "subject")
                .append("in", new Document("$cond", List.of(
                        subjectCondition,
                        migratedSubject,
                        "$$subject"))));
        List<Bson> pipeline = List.of(
                new Document("$set", new Document("subjects", subjectMap)),
                new Document("$unset", "subjects.teacher"));

        UpdateResult result = collection.updateMany(filter, pipeline);
        return result.getModifiedCount();
    }
}
