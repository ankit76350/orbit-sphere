package com.orbitastra.backend.repositories.academics.examschedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExamScheduleRepositoryImpl implements ExamScheduleRepositoryCustom {

    private final MongoTemplate mongo;

    /**
     * One aggregation: the papers of these exams, cancelled ones left out, counted per exam.
     */
    @Override
    public Map<String, Long> countPapersByExam(String schoolId, Collection<String> examDocsIds) {
        Map<String, Long> counts = new HashMap<>();
        if (examDocsIds == null || examDocsIds.isEmpty()) {
            return counts;
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("examDocsId").in(examDocsIds)
                        .and("status").ne(ExamScheduleStatus.CANCELLED)),
                Aggregation.group("examDocsId").count().as("papers"));

        // TODO: count exam schedules (papers per exam)
        for (Document row : mongo.aggregate(aggregation, ExamSchedule.class, Document.class)) {
            counts.put(row.getString("_id"), ((Number) row.get("papers")).longValue());
        }
        return counts;
    }

    /** One aggregation: the exam's papers, cancelled ones included, counted per status. */
    @Override
    public Map<String, Long> countByStatusForExam(String schoolId, String examDocsId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("examDocsId").is(examDocsId)),
                Aggregation.group("status").count().as("papers"));

        Map<String, Long> counts = new HashMap<>();
        // TODO: count exam schedules (per status)
        for (Document row : mongo.aggregate(aggregation, ExamSchedule.class, Document.class)) {
            counts.put(String.valueOf(row.get("_id")), ((Number) row.get("papers")).longValue());
        }
        return counts;
    }

    /**
     * An exam's papers for its datesheet. The school and the exam always apply; the class, section,
     * day and status only when sent. Ordered by date, then start time.
     */
    @Override
    public List<ExamSchedule> findDatesheet(String schoolId, String examDocsId, String classDocsId,
            String sectionNo, LocalDate date, ExamScheduleStatus status) {

        Criteria criteria = Criteria.where("schoolId").is(schoolId).and("examDocsId").is(examDocsId);
        if (classDocsId != null && !classDocsId.isBlank()) {
            criteria = criteria.and("classDocsId").is(classDocsId.trim());
        }
        //! The section is matched ignoring case, and quoted so it is looked for as plain text.
        if (sectionNo != null && !sectionNo.isBlank()) {
            criteria = criteria.and("sectionNo").regex("^" + Pattern.quote(sectionNo.trim()) + "$", "i");
        }
        if (date != null) {
            criteria = criteria.and("examDate").is(date);
        }
        if (status != null) {
            criteria = criteria.and("status").is(status);
        }

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.asc("examDate"), Sort.Order.asc("startTime")));

        // TODO: read exam schedules (the datesheet)
        return mongo.find(query, ExamSchedule.class);
    }
}
