package com.orbitastra.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.academics.Homework;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.student.StudentAcademicRecord;

import lombok.RequiredArgsConstructor;

/**
 * Drops indexes left behind by earlier schema versions. With
 * {@code spring.data.mongodb.auto-index-creation=true} Spring creates the indexes
 * declared on the models, but it never drops ones that were later renamed or
 * removed — so an obsolete unique index keeps enforcing a stale constraint against
 * fields that no longer exist on the document. This runs once on startup and is
 * idempotent (only drops an index that is actually present).
 */
@Component
@RequiredArgsConstructor
public class ObsoleteIndexCleanup {

    private static final Logger log = LoggerFactory.getLogger(ObsoleteIndexCleanup.class);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void dropObsoleteIndexes() {
        // Superseded by 'class_doc_section_year_roll_unique_idx' (on classDocId/academicYear).
        // The old index was defined on the pre-rename fields classId/academicYearId, which no
        // longer exist on the document — so both keys are always null and the unique constraint
        // collapses to (sectionId, rollNo), causing spurious E11000 duplicate-key errors when a
        // second student reuses a section+roll. See StudentAcademicRecord @CompoundIndexes.
        dropIfPresent(StudentAcademicRecord.class, "class_section_year_roll_unique_idx");

        // Superseded by 'school_year_student_no_unique_idx' after the studentId -> studentNo
        // rename. The old index is on the now-nonexistent 'studentId' field (always null), so it
        // wrongly enforces uniqueness on (schoolId, academicYear) alone.
        dropIfPresent(StudentAcademicRecord.class, "school_year_student_id_unique_idx");

        // Superseded by 'class_doc_section_no_year_roll_unique_idx' after the sectionId ->
        // sectionNo rename. The old index is on the now-nonexistent 'sectionId' field (always
        // null), so it wrongly enforces uniqueness on (classDocId, academicYear, rollNo) alone.
        dropIfPresent(StudentAcademicRecord.class, "class_doc_section_year_roll_unique_idx");

        // Superseded by the 'inquiryDocsId' index after the Admission.inquiryId -> inquiryDocsId
        // rename. The old index was named after the field ('inquiryId'), which no longer exists.
        // It is sparse so it indexes nothing now, but we drop it to keep things tidy.
        dropIfPresent(Admission.class, "inquiryId");

        // Superseded by the generated index on Homework.classDocsId after the
        // classId -> classDocsId reference rename.
        dropIfPresent(Homework.class, "classId");
    }

    private void dropIfPresent(Class<?> type, String indexName) {
        IndexOperations ops = mongoTemplate.indexOps(type);
        boolean exists = ops.getIndexInfo().stream().anyMatch(i -> indexName.equals(i.getName()));
        if (exists) {
            ops.dropIndex(indexName);
            log.info("Dropped obsolete MongoDB index '{}' on {}", indexName, type.getSimpleName());
        }
    }
}
