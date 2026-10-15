package com.orbitastra.backend.repositories.academics.attendance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import com.orbitastra.backend.models.academics.attendance.StudentAttendanceRecord;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StudentAttendanceRecordRepositoryImpl implements StudentAttendanceRecordRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public List<SessionStatusCount> countBySessionAndStatus(String schoolId,
            Collection<String> attendanceSessionDocsIds) {

        //! ONE AGGREGATION for the whole page: keep the marks of these registers, count them per
        //! register and status. The match uses school_session_student_attendance_uniq.
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("attendanceSessionDocsId").in(attendanceSessionDocsIds)),
                Aggregation.group("attendanceSessionDocsId", "status").count().as("count"));

        // TODO: read student attendance records (count per register and status)
        List<Document> groups = mongo.aggregate(pipeline, StudentAttendanceRecord.class,
                Document.class).getMappedResults();

        List<SessionStatusCount> counts = new ArrayList<>();
        for (Document group : groups) {
            //! A GROUP ON TWO FIELDS puts both under _id.
            Document key = group.get("_id", Document.class);
            Object status = key.get("status");
            counts.add(new SessionStatusCount(key.getString("attendanceSessionDocsId"),
                    status == null ? null : status.toString(),
                    ((Number) group.get("count")).longValue()));
        }
        return counts;
    }
}
