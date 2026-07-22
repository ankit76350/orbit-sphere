package com.orbitastra.backend.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.client.result.UpdateResult;
import com.orbitastra.backend.models.student.Student;

import lombok.RequiredArgsConstructor;

/**
 * Applies idempotent Student document migrations for fields renamed in the
 * application model. A legacy value is moved only when its replacement field is
 * absent, so an already-populated new field is never overwritten.
 */
@Component
@RequiredArgsConstructor
public class StudentFieldMigration {

    private static final Logger log = LoggerFactory.getLogger(StudentFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateStudentFields() {
        long walletCount = renameWhenTargetMissing("walletId", "walletDocsId");
        long medicalRecordCount = renameWhenTargetMissing("medicalRecordId", "medicalRecordDocsId");
        long documentsCount = initialiseMissingList("documents");
        long medicalRemarkCount = initialiseMissingList("medicalRemark");

        long total = walletCount + medicalRecordCount + documentsCount + medicalRemarkCount;
        if (total > 0) {
            log.info("Migrated {} Student field value(s): wallet={}, medicalRecord={}, documents={}, medicalRemark={}",
                    total, walletCount, medicalRecordCount, documentsCount, medicalRemarkCount);
        }
    }

    private long renameWhenTargetMissing(String legacyField, String currentField) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(legacyField).exists(true),
                Criteria.where(currentField).exists(false)));
        UpdateResult result = mongoTemplate.updateMulti(
                query, new Update().rename(legacyField, currentField), Student.class);
        return result.getModifiedCount();
    }

    private long initialiseMissingList(String field) {
        Query query = Query.query(Criteria.where(field).is(null));
        UpdateResult result = mongoTemplate.updateMulti(
                query, new Update().set(field, List.of()), Student.class);
        return result.getModifiedCount();
    }
}
