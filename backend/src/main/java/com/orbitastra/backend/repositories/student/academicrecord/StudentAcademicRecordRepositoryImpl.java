package com.orbitastra.backend.repositories.student.academicrecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordSearchRequest;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StudentAcademicRecordRepositoryImpl implements StudentAcademicRecordRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<StudentAcademicRecord> search(String schoolId, String academicYear,
            StudentAcademicRecordSearchRequest request, Pageable pageable) {

        //! step 1 - the school and the year always, then whichever filters were sent. The first
        //! keys match school_year_class_section_roster_idx, so the class and section filters use it.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        if (request.classDocsId() != null && !request.classDocsId().isBlank()) {
            filters.add(Criteria.where("classDocsId").is(request.classDocsId().trim()));
        }
        if (request.sectionNo() != null && !request.sectionNo().isBlank()) {
            filters.add(Criteria.where("sectionNo").is(request.sectionNo().trim()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }
        if (request.studentDocsId() != null && !request.studentDocsId().isBlank()) {
            filters.add(Criteria.where("studentDocsId").is(request.studentDocsId().trim()));
        }

        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 2 - how many match, with the filter and nothing else
        // TODO: read student academic records (how many match)
        long total = mongo.count(new Query(criteria), StudentAcademicRecord.class);

        //! step 3 - one page of them, the same filter plus the paging and the order
        // TODO: read student academic records (one page of them)
        List<StudentAcademicRecord> rows =
                mongo.find(new Query(criteria).with(pageable), StudentAcademicRecord.class);

        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public List<SectionCount> countActiveBySection(String schoolId, String academicYear) {

        //! ONE AGGREGATION FOR THE WHOLE YEAR, not a count per section: keep the ACTIVE records of
        //! this school and year, then count them per class and section.
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("academicYear").is(academicYear)
                        .and("status").is(AcademicRecordStatus.ACTIVE)),
                Aggregation.group("classDocsId", "sectionNo").count().as("enrolled"));

        // TODO: read student academic records (count per class and section)
        List<Document> groups = mongo.aggregate(pipeline, StudentAcademicRecord.class,
                Document.class).getMappedResults();

        List<SectionCount> counts = new ArrayList<>();
        for (Document group : groups) {
            //! A GROUP ON TWO FIELDS puts both under _id.
            Document key = group.get("_id", Document.class);
            counts.add(new SectionCount(
                    key.getString("classDocsId"),
                    key.getString("sectionNo"),
                    ((Number) group.get("enrolled")).longValue()));
        }
        return counts;
    }

    @Override
    public List<StudentAcademicRecord> findInSectionOn(String schoolId, String academicYear,
            String classDocsId, String sectionNo, LocalDate date) {

        //! The first keys match school_year_class_section_roster_idx. The placement has to have
        //! started by the date, and not ended before it; an open record has no effectiveUntil.
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("schoolId").is(schoolId),
                Criteria.where("academicYear").is(academicYear),
                Criteria.where("classDocsId").is(classDocsId),
                Criteria.where("sectionNo").is(sectionNo),
                Criteria.where("status").ne(AcademicRecordStatus.CANCELLED),
                Criteria.where("effectiveFrom").lte(date),
                new Criteria().orOperator(
                        Criteria.where("effectiveUntil").exists(false),
                        Criteria.where("effectiveUntil").is(null),
                        Criteria.where("effectiveUntil").gte(date)));

        // TODO: read student academic records (who was in this section on this date)
        return mongo.find(new Query(criteria), StudentAcademicRecord.class);
    }

    @Override
    public List<SectionCount> countInSectionsOn(String schoolId, String academicYear,
            LocalDate date) {

        //! THE SAME DATE RULE AS findInSectionOn, for every section at once: started on or before
        //! the date, not ended before it, not cancelled. Counted per class and section.
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("schoolId").is(schoolId),
                Criteria.where("academicYear").is(academicYear),
                Criteria.where("status").ne(AcademicRecordStatus.CANCELLED),
                Criteria.where("effectiveFrom").lte(date),
                new Criteria().orOperator(
                        Criteria.where("effectiveUntil").exists(false),
                        Criteria.where("effectiveUntil").is(null),
                        Criteria.where("effectiveUntil").gte(date)));

        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("classDocsId", "sectionNo").count().as("enrolled"));

        // TODO: read student academic records (how many in each section on this date)
        List<Document> groups = mongo.aggregate(pipeline, StudentAcademicRecord.class,
                Document.class).getMappedResults();

        List<SectionCount> counts = new ArrayList<>();
        for (Document group : groups) {
            Document key = group.get("_id", Document.class);
            counts.add(new SectionCount(key.getString("classDocsId"), key.getString("sectionNo"),
                    ((Number) group.get("enrolled")).longValue()));
        }
        return counts;
    }

    @Override
    public List<StudentAcademicRecord> findInSectionBetween(String schoolId, String academicYear,
            String classDocsId, String sectionNo, LocalDate from, LocalDate to) {

        //! The range version of findInSectionOn: the placement overlaps the range at all.
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("schoolId").is(schoolId),
                Criteria.where("academicYear").is(academicYear),
                Criteria.where("classDocsId").is(classDocsId),
                Criteria.where("sectionNo").is(sectionNo),
                Criteria.where("status").ne(AcademicRecordStatus.CANCELLED),
                Criteria.where("effectiveFrom").lte(to),
                new Criteria().orOperator(
                        Criteria.where("effectiveUntil").exists(false),
                        Criteria.where("effectiveUntil").is(null),
                        Criteria.where("effectiveUntil").gte(from)));

        // TODO: read student academic records (who was in this section during the range)
        return mongo.find(new Query(criteria), StudentAcademicRecord.class);
    }
}
