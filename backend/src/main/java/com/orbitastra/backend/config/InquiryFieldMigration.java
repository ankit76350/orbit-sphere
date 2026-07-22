package com.orbitastra.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.crm.Inquiry;

import lombok.RequiredArgsConstructor;

/** Migrates renamed top-level fields in existing Inquiry documents. */
@Component
@RequiredArgsConstructor
public class InquiryFieldMigration {

    private static final Logger log = LoggerFactory.getLogger(InquiryFieldMigration.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateInquiryFields() {
        long migrated = MongoFieldMigrationSupport.renameWhenTargetMissing(
                mongoTemplate, Inquiry.class, "counselorId", "counselorDocsId");
        if (migrated > 0) {
            log.info("Migrated counselorId to counselorDocsId on {} Inquiry document(s)", migrated);
        }
    }
}
