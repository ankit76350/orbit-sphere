package com.orbitastra.backend.config;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Applies the project-wide identifier naming convention to existing MongoDB
 * documents.
 *
 * <p>MongoDB document references use {@code *DocsId}; human-readable
 * identifiers use {@code *No}. The document's own {@code id} and the shared
 * tenant {@code schoolId} remain unchanged. Every rename is idempotent and
 * refuses to overwrite an already-present target field.
 */
@Component
@RequiredArgsConstructor
public class ProjectFieldNamingMigration {

    private static final Logger log = LoggerFactory.getLogger(ProjectFieldNamingMigration.class);

    private static final List<FieldRename> TOP_LEVEL_RENAMES = List.of(
            rename("staffs", "employeeId", "employeeNo"),

            rename("students", "admissionId", "admissionDocsId"),
            rename("students", "walletId", "walletDocsId"),
            rename("students", "medicalRecordId", "medicalRecordDocsId"),
            rename("students", "currentAcademicRecordId", "currentAcademicRecordDocsId"),
            rename("inquiries", "counselorId", "counselorDocsId"),
            rename("inquiries", "admissionId", "admissionDocsId"),
            rename("admissions", "inquiryId", "inquiryDocsId"),
            rename("admissions", "studentId", "studentDocsId"),
            rename("classes", "classTeacherId", "classTeacherDocsId"),

            rename("student_reviews", "studentId", "studentDocsId"),
            rename("student_reviews", "teacherId", "teacherDocsId"),
            rename("student_reviews", "reviewCycleId", "reviewCycleDocsId"),
            rename("teacher_performance_reviews", "teacherId", "teacherDocsId"),
            rename("teacher_performance_reviews", "reviewerId", "reviewerDocsId"),
            rename("teacher_performance_reviews", "reviewCycleId", "reviewCycleDocsId"),
            rename("teacher_reviews", "teacherId", "teacherDocsId"),
            rename("teacher_reviews", "studentId", "studentDocsId"),
            rename("teacher_reviews", "parentId", "parentDocsId"),
            rename("teacher_reviews", "reviewCycleId", "reviewCycleDocsId"),

            rename("academic_results", "studentId", "studentDocsId"),
            rename("attendance", "studentId", "studentDocsId"),
            rename("attendance", "presentBy", "presentByDocsId"),
            rename("discipline_logs", "studentId", "studentDocsId"),
            rename("fee_invoices", "studentId", "studentDocsId"),
            rename("fee_payments", "studentId", "studentDocsId"),
            rename("fee_payments", "feeId", "feeDocsId"),
            rename("fee_payments", "collectedBy", "collectedByDocsId"),
            rename("homework", "classId", "classDocsId"),
            rename("homework", "teacherId", "teacherDocsId"),
            rename("medical_records", "studentId", "studentDocsId"),
            rename("notifications", "recipientId", "recipientDocsId"),
            rename("student_wallets", "studentId", "studentDocsId"),
            rename("wallet_transactions", "studentId", "studentDocsId"),
            rename("student_academic_records", "studentDocId", "studentDocsId"),
            rename("student_academic_records", "studentId", "studentDocsId"),
            rename("student_academic_records", "classDocId", "classDocsId"),
            rename("student_academic_records", "classId", "classDocsId"),

            rename("ai_approved_remarks", "studentId", "studentDocsId"),
            rename("ai_approved_remarks", "approvedBy", "approvedByName"),
            rename("ai_audit_entries", "actor", "actorName"),
            rename("alumni_donations", "alumniId", "alumniDocsId"),
            rename("alumni_donations", "campaignId", "campaignDocsId"),
            rename("job_postings", "alumniId", "alumniDocsId"),
            rename("mentorship_programs", "mentorAlumniId", "mentorAlumniDocsId"),
            rename("mentorship_programs", "studentId", "studentDocsId"),
            rename("audit_logs", "userId", "userDocsId"),
            rename("birthday_cards", "personId", "personDocsId"),
            rename("birthday_gallery", "personId", "personDocsId"),
            rename("birthday_notifications", "personId", "personDocsId"),
            rename("comm_broadcasts", "templateId", "templateDocsId"),
            rename("comm_broadcasts", "sentBy", "sentByName"),
            rename("diary_entries", "teacher", "teacherName"),
            rename("message_templates", "dltId", "dltNo"),
            rename("apaar_records", "studentId", "studentDocsId"),
            rename("apaar_records", "apaarId", "apaarNo"),
            rename("dpdp_consents", "studentId", "studentDocsId"),
            rename("holistic_progress_cards", "studentId", "studentDocsId"),
            rename("document_approvals", "documentId", "documentDocsId"),
            rename("document_approvals", "requestorId", "requestorDocsId"),
            rename("document_approvals", "approverId", "approverDocsId"),
            rename("document_signatures", "signerId", "signerDocsId"),
            rename("generated_documents", "documentNumber", "documentNo"),
            rename("generated_documents", "entityId", "entityDocsId"),
            rename("id_cards", "holderId", "holderDocsId"),
            rename("exam_marks_sheets", "examId", "examDocsId"),
            rename("concession_requests", "studentId", "studentDocsId"),
            rename("concession_requests", "policyId", "policyDocsId"),
            rename("concession_requests", "requestedBy", "requestedByName"),
            rename("concession_requests", "reviewedBy", "reviewedByName"),
            rename("fee_reminder_logs", "studentId", "studentDocsId"),
            rename("fee_structures", "feeHeadIds", "feeHeadDocsIds"),
            rename("upi_mandates", "studentId", "studentDocsId"),
            rename("upi_mandates", "parentId", "parentDocsId"),
            rename("gallery_media", "albumId", "albumDocsId"),
            rename("outpasses", "studentId", "studentDocsId"),
            rename("outpasses", "approvedBy", "approvedByName"),
            rename("visitors", "studentId", "studentDocsId"),
            rename("hostel_rooms", "buildingId", "buildingDocsId"),
            rename("payslips", "staffId", "staffDocsId"),
            rename("salary_increments", "staffId", "staffDocsId"),
            rename("salary_increments", "appliedBy", "appliedByName"),
            rename("salary_structures", "staffId", "staffDocsId"),
            rename("statutory_filings", "filedBy", "filedByName"),
            rename("promotion_batches", "executedBy", "executedByName"),
            rename("saved_reports", "createdBy", "createdByName"),
            rename("call_logs", "assignedTo", "assignedToName"),
            rename("grievances", "assignedTo", "assignedToName"),
            rename("postal_entries", "handler", "handlerName"),
            rename("camera_assignments", "cameraId", "cameraDocsId"),
            rename("camera_assignments", "gradeId", "gradeNo"),
            rename("camera_assignments", "classId", "classNo"),
            rename("camera_assignments", "sectionId", "sectionNo"),
            rename("camera_recordings", "cameraId", "cameraDocsId"),
            rename("security_incidents", "cameraId", "cameraDocsId"),
            rename("drivers", "vehicleId", "vehicleDocsId"),
            rename("transport_allocations", "studentId", "studentDocsId"),
            rename("transport_allocations", "routeId", "routeDocsId"),
            rename("transport_attendance", "studentId", "studentDocsId"),
            rename("transport_attendance", "routeId", "routeDocsId"),
            rename("transport_routes", "routeCode", "routeNo"),
            rename("transport_routes", "vehicleNumber", "vehicleNo"),
            rename("users", "referenceId", "referenceDocsId"),
            rename("class_recordings", "classId", "classDocsId"),
            rename("ai_notes", "classId", "classDocsId"),
            rename("online_classes", "subjectId", "subjectNo"),
            rename("online_classes", "teacherId", "teacherDocsId"),
            rename("online_classes", "classId", "classNo"),
            rename("online_classes", "sectionId", "sectionNo"));

    private static final List<EmbeddedFieldRename> EMBEDDED_RENAMES = List.of(
            embeddedRename("students", "guardians", "guardianId", "guardianDocsId"),
            embeddedRename("inquiries", "followUps", "counselorId", "counselorDocsId"),
            embeddedRename("classes", "subjects", "teacherId", "teacherDocsId"),
            embeddedRename("daily_timetables", "entries", "classId", "classDocsId"),
            embeddedRename("daily_timetables", "entries", "teacherId", "teacherDocsId"),
            embeddedRename("homework", "studentAssignments", "studentId", "studentDocsId"),
            embeddedRename("ptm_events", "bookings", "teacherId", "teacherDocsId"),
            embeddedRename("exam_marks_sheets", "marks", "studentId", "studentDocsId"),
            embeddedRename("exams", "datesheet", "invigilator", "invigilatorName"));

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateProjectFieldNames() {
        long migrated = TOP_LEVEL_RENAMES.stream()
                .mapToLong(this::migrateTopLevelField)
                .sum();
        migrated += EMBEDDED_RENAMES.stream()
                .mapToLong(this::renameEmbeddedWhenTargetMissing)
                .sum();

        if (migrated > 0) {
            log.info("Migrated {} legacy identifier field value(s)", migrated);
        }
    }

    private long migrateTopLevelField(FieldRename fieldRename) {
        return renameWhenTargetMissing(fieldRename)
                + removeLegacyWhenTargetPresent(fieldRename);
    }

    long renameWhenTargetMissing(FieldRename fieldRename) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(fieldRename.source()).exists(true),
                Criteria.where(fieldRename.target()).exists(false)));
        return mongoTemplate.updateMulti(
                query,
                new Update().rename(fieldRename.source(), fieldRename.target()),
                fieldRename.collection()).getModifiedCount();
    }

    long removeLegacyWhenTargetPresent(FieldRename fieldRename) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(fieldRename.source()).exists(true),
                Criteria.where(fieldRename.target()).exists(true)));
        return mongoTemplate.updateMulti(
                query,
                new Update().unset(fieldRename.source()),
                fieldRename.collection()).getModifiedCount();
    }

    long renameEmbeddedWhenTargetMissing(EmbeddedFieldRename fieldRename) {
        String sourcePath = fieldRename.arrayField() + "." + fieldRename.source();
        Query query = Query.query(Criteria.where(sourcePath).exists(true));
        List<Document> documents = mongoTemplate.find(
                query, Document.class, fieldRename.collection());

        long migrated = 0;
        for (Document document : documents) {
            Object embeddedValues = document.get(fieldRename.arrayField());
            if (!renameInEmbeddedValues(
                    embeddedValues, fieldRename.source(), fieldRename.target())) {
                continue;
            }

            Query byId = Query.query(Criteria.where("_id").is(document.get("_id")));
            migrated += mongoTemplate.updateFirst(
                    byId,
                    new Update().set(fieldRename.arrayField(), embeddedValues),
                    fieldRename.collection()).getModifiedCount();
        }
        return migrated;
    }

    @SuppressWarnings("unchecked")
    static boolean renameInEmbeddedValues(Object value, String source, String target) {
        if (!(value instanceof List<?> values)) {
            return false;
        }

        boolean changed = false;
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) rawMap;
            if (map.containsKey(source)) {
                if (!map.containsKey(target)) {
                    map.put(target, map.get(source));
                }
                map.remove(source);
                changed = true;
            }
        }
        return changed;
    }

    private static FieldRename rename(String collection, String source, String target) {
        return new FieldRename(collection, source, target);
    }

    private static EmbeddedFieldRename embeddedRename(
            String collection, String arrayField, String source, String target) {
        return new EmbeddedFieldRename(collection, arrayField, source, target);
    }

    record FieldRename(String collection, String source, String target) {
    }

    record EmbeddedFieldRename(String collection, String arrayField, String source, String target) {
    }
}
