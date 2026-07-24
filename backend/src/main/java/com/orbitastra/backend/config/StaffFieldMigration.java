package com.orbitastra.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.staff.StudentReview;
import com.orbitastra.backend.models.staff.TeacherPerformanceReview;
import com.orbitastra.backend.models.staff.TeacherReview;

import lombok.RequiredArgsConstructor;

/**
 * Migrates legacy staff-related field names. A value is renamed only when the
 * replacement field is absent, making every operation idempotent and
 * non-overwriting.
 */
@Component
@RequiredArgsConstructor
public class StaffFieldMigration {

    private static final Logger log = LoggerFactory.getLogger(StaffFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateStaffFields() {
        long migrated = 0;
        migrated += renameWhenTargetMissing(Staff.class, "employeeId", "employeeNo");

        migrated += renameWhenTargetMissing(StudentReview.class, "studentId", "studentDocsId");
        migrated += renameWhenTargetMissing(StudentReview.class, "teacherId", "teacherDocsId");
        migrated += renameWhenTargetMissing(StudentReview.class, "reviewCycleId", "reviewCycleDocsId");

        migrated += renameWhenTargetMissing(
                TeacherPerformanceReview.class, "teacherId", "teacherDocsId");
        migrated += renameWhenTargetMissing(
                TeacherPerformanceReview.class, "reviewerId", "reviewerDocsId");
        migrated += renameWhenTargetMissing(
                TeacherPerformanceReview.class, "reviewCycleId", "reviewCycleDocsId");

        migrated += renameWhenTargetMissing(TeacherReview.class, "teacherId", "teacherDocsId");
        migrated += renameWhenTargetMissing(TeacherReview.class, "studentId", "studentDocsId");
        migrated += renameWhenTargetMissing(TeacherReview.class, "parentId", "parentDocsId");
        migrated += renameWhenTargetMissing(
                TeacherReview.class, "reviewCycleId", "reviewCycleDocsId");

        if (migrated > 0) {
            log.info("Migrated {} legacy staff field value(s)", migrated);
        }
    }

    private long renameWhenTargetMissing(Class<?> documentType, String source, String target) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(source).exists(true),
                Criteria.where(target).exists(false)));
        return mongoTemplate.updateMulti(
                query, new Update().rename(source, target), documentType).getModifiedCount();
    }
}
