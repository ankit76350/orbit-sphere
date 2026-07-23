package com.orbitastra.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
        long walletCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Student.class, "walletId", "walletDocsId");
        long medicalRecordCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Student.class, "medicalRecordId", "medicalRecordDocsId");
        long currentAcademicRecordCount = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Student.class,
                "currentAcademicRecordId", "currentAcademicRecordDocsId");
        long documentsCount = MongoFieldMigrationSupport.initialiseMissingList(
                mongoTemplate, Student.class, "documents");
        long medicalRemarkCount = MongoFieldMigrationSupport.initialiseMissingList(
                mongoTemplate, Student.class, "medicalRemark");

        long total = walletCount + medicalRecordCount + currentAcademicRecordCount
                + documentsCount + medicalRemarkCount;
        if (total > 0) {
            log.info("Migrated {} Student field value(s): wallet={}, medicalRecord={}, currentAcademicRecord={}, documents={}, medicalRemark={}",
                    total, walletCount, medicalRecordCount, currentAcademicRecordCount,
                    documentsCount, medicalRemarkCount);
        }
    }

}
