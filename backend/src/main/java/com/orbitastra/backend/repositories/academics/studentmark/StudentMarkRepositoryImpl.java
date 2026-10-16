package com.orbitastra.backend.repositories.academics.studentmark;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import com.orbitastra.backend.models.academics.examination.StudentMark;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StudentMarkRepositoryImpl implements StudentMarkRepositoryCustom {

    private final MongoTemplate mongo;

    /** One aggregation: the exam's marks, counted per status. */
    @Override
    public Map<String, Long> countByStatusForExam(String schoolId, String examDocsId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("examDocsId").is(examDocsId)),
                Aggregation.group("status").count().as("marks"));

        Map<String, Long> counts = new HashMap<>();
        // TODO: count student marks (per status)
        for (Document row : mongo.aggregate(aggregation, StudentMark.class, Document.class)) {
            counts.put(String.valueOf(row.get("_id")), ((Number) row.get("marks")).longValue());
        }
        return counts;
    }
}
