package com.orbitastra.backend.repositories.academics.timetable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link DailyTimetableRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls {@code search}.
 */
@RequiredArgsConstructor
public class DailyTimetableRepositoryImpl implements DailyTimetableRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<DailyTimetableSummaryResponse> search(String schoolId, String academicYear,
            DailyTimetableSearchRequest request, Pageable pageable) {

        //! step 1 - the filter: the tenant and the year, then whichever filters were sent
        Criteria criteria = buildCriteria(schoolId, academicYear, request);

        //! step 2 - how many days match, carrying the filter and nothing else
        // TODO: reading daily timetables (how many match)
        long total = mongo.count(new org.springframework.data.mongodb.core.query.Query(criteria),
                DailyTimetable.class);

        if (total == 0 || pageable.getOffset() >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        //! step 3 - the page itself, as COUNTS. The periods never leave the database: a full day
        //! is about 120 KB and a page of twenty would be two and a half megabytes shipped to
        //! render twenty dates and five numbers.
        List<AggregationOperation> stages = new ArrayList<>();
        stages.add(Aggregation.match(criteria));
        stages.add(Aggregation.sort(pageable.getSort()));
        stages.add(Aggregation.skip(pageable.getOffset()));
        stages.add(Aggregation.limit(pageable.getPageSize()));
        stages.add(countsProjection());

        // TODO: reading daily timetables (one page of them, as counts)
        AggregationResults<Document> rows = mongo.aggregate(
                Aggregation.newAggregation(stages), DailyTimetable.class, Document.class);

        List<DailyTimetableSummaryResponse> content = new ArrayList<>();
        for (Document row : rows) {
            content.add(toSummary(row));
        }

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * The five counts, worked out in the database.
     *
     * <p>Written as a raw {@code $project} rather than through Spring's fluent builder, because
     * two of them are a {@code $setUnion} over a {@code $map} and the fluent form of that is
     * harder to read than the stage it produces.
     *
     * <p><b>{@code sectionCount} pairs the class with the section.</b> "A" of one class and "A" of
     * another are two sections, and counting the bare {@code sectionNo} would report a school
     * teaching twelve classes as having three sections.
     */
    private AggregationOperation countsProjection() {
        Document entries = new Document("$ifNull", List.of("$entries", List.of()));

        Document lessons = new Document("$filter", new Document()
                .append("input", entries)
                .append("cond", new Document("$eq", List.of("$$this.slotType", "LESSON"))));

        Document sectionKeys = new Document("$map", new Document()
                .append("input", entries)
                .append("in", new Document("$concat",
                        List.of("$$this.classDocsId", "|", "$$this.sectionNo"))));

        //! Teachers are filtered first: a break with nobody supervising it carries no
        //! teacherDocsId, and $setUnion would otherwise count the absence as a person.
        //!
        //! $type, NOT $ne null - and that distinction is measured rather than assumed. An absent
        //! field is ABSENT in these documents, not stored as null, and in an AGGREGATION
        //! EXPRESSION a missing value compared with $ne against null evaluates TRUE. (The query
        //! language behaves the opposite way, which is what makes this easy to get wrong: there,
        //! `field: {$ne: null}` does exclude a missing field.) Measured on a day holding one
        //! lesson and one unsupervised break: $ne null counted 2 teachers, $type counted 1.
        //!
        //! It is also the same test the partial indexes in this project use for exactly this
        //! reason - see school_staff_phone_uniq.
        Document teachers = new Document("$map", new Document()
                .append("input", new Document("$filter", new Document()
                        .append("input", entries)
                        .append("cond", new Document("$eq", List.of(
                                new Document("$type", "$$this.teacherDocsId"), "string")))))
                .append("in", "$$this.teacherDocsId"));

        return context -> new Document("$project", new Document()
                .append("date", 1)
                .append("academicYear", 1)
                .append("entryCount", new Document("$size", entries))
                .append("lessonCount", new Document("$size", lessons))
                .append("classCount", new Document("$size",
                        new Document("$setUnion", List.of(
                                new Document("$map", new Document()
                                        .append("input", entries)
                                        .append("in", "$$this.classDocsId"))))))
                .append("sectionCount", new Document("$size",
                        new Document("$setUnion", List.of(sectionKeys))))
                .append("teacherCount", new Document("$size",
                        new Document("$setUnion", List.of(teachers)))));
    }

    /** One aggregation row as the response sees it. */
    private DailyTimetableSummaryResponse toSummary(Document row) {
        Object rawDate = row.get("date");

        //! A LocalDate is stored as a Date at the school's own local midnight, so it comes back
        //! from a raw aggregation as java.util.Date rather than as the LocalDate the mapper would
        //! have produced. Converted in the system zone, which is the zone it was written in.
        LocalDate date = rawDate instanceof Date value
                ? value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;

        return new DailyTimetableSummaryResponse(
                String.valueOf(row.get("_id")),
                date,
                row.getString("academicYear"),
                intOf(row.get("entryCount")),
                intOf(row.get("lessonCount")),
                intOf(row.get("classCount")),
                intOf(row.get("sectionCount")),
                intOf(row.get("teacherCount")));
    }

    /** {@code $size} answers with an Integer, but a Number is what the driver promises. */
    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Criteria buildCriteria(String schoolId, String academicYear,
            DailyTimetableSearchRequest request) {

        //! step 1 - the tenant and the year, always, and never from the caller's filters
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        //! step 2 - the date window. Either end may be left off, which is what makes "everything
        //! from here on" and "everything up to here" expressible without a sentinel date.
        if (request.from() != null && request.to() != null) {
            filters.add(Criteria.where("date").gte(request.from()).lte(request.to()));
        } else if (request.from() != null) {
            filters.add(Criteria.where("date").gte(request.from()));
        } else if (request.to() != null) {
            filters.add(Criteria.where("date").lte(request.to()));
        }

        //! step 3 - the filters that reach INSIDE the periods.
        //!
        //! A CLASS AND A SECTION ARE MATCHED AS ONE PERIOD, not as two conditions. A day holds
        //! every class's periods, so `entries.classDocsId = X` and `entries.sectionNo = A` as
        //! separate clauses match a day where one entry belongs to X and an entirely unrelated
        //! entry belongs to some other class's section A - which is nearly every day.
        boolean hasClass = request.classDocsId() != null && !request.classDocsId().isBlank();
        boolean hasSection = request.sectionNo() != null && !request.sectionNo().isBlank();

        if (hasClass && hasSection) {
            filters.add(Criteria.where("entries").elemMatch(
                    Criteria.where("classDocsId").is(request.classDocsId().trim())
                            .and("sectionNo").is(request.sectionNo().trim())));
        } else if (hasClass) {
            filters.add(Criteria.where("entries.classDocsId").is(request.classDocsId().trim()));
        } else if (hasSection) {
            filters.add(Criteria.where("entries.sectionNo").is(request.sectionNo().trim()));
        }

        //! step 4 - one person's working days, and one room's
        if (request.teacherDocsId() != null && !request.teacherDocsId().isBlank()) {
            filters.add(Criteria.where("entries.teacherDocsId")
                    .is(request.teacherDocsId().trim()));
        }

        if (request.facilityResourceDocsId() != null
                && !request.facilityResourceDocsId().isBlank()) {

            filters.add(Criteria.where("entries.facilityResourceDocsId")
                    .is(request.facilityResourceDocsId().trim()));
        }

        //! step 5 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
