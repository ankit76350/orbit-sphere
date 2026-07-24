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

import lombok.RequiredArgsConstructor;

/**
 * Moves the legacy {@code employeeId} value to {@code employeeNo} in existing
 * Staff documents. Documents that already have {@code employeeNo} are left
 * untouched, which makes the migration safe to run on every startup.
 */
@Component
@RequiredArgsConstructor
public class StaffFieldMigration {

    private static final Logger log = LoggerFactory.getLogger(StaffFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateEmployeeNumber() {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("employeeId").exists(true),
                Criteria.where("employeeNo").exists(false)));
        Update update = new Update().rename("employeeId", "employeeNo");

        long migrated = mongoTemplate.updateMulti(query, update, Staff.class).getModifiedCount();
        if (migrated > 0) {
            log.info("Migrated employeeId to employeeNo on {} Staff document(s)", migrated);
        }
    }
}
