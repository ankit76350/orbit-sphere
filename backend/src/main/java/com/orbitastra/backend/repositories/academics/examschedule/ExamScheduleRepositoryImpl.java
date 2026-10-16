package com.orbitastra.backend.repositories.academics.examschedule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

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
}
