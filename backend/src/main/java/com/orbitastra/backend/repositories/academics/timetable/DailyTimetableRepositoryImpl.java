package com.orbitastra.backend.repositories.academics.timetable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;

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

    //! the three single-entry writes — #3, #4 and #5 --------------------------------------

    /**
     * Which fields of an entry a {@code $set} may name.
     *
     * <p><b>The service already builds the map from fixed DTO fields</b>, so no caller-controlled
     * string can reach here today. This is the second lock: {@code $set entries.$[e].<key>} with a
     * key that came from a request body would be field injection into an embedded document, and a
     * list is cheaper than the audit that would otherwise be needed every time this is read.
     *
     * <p>{@code classDocsId}, {@code sectionNo}, {@code slotType} and {@code _id} are deliberately
     * absent — moving a period to another section is deleting one and adding another, and changing
     * the slot type in place would leave a subject on a break.
     */
    private static final Set<String> PATCHABLE = Set.of(
            "periodCode", "subjectCode", "teacherDocsId", "slotLabel",
            "startTime", "endTime", "facilityResourceDocsId");

    @Override
    public long pushEntry(String schoolId, LocalDate date, TimetableEntry entry) {
        //! THE PERIOD CODE IS GUARDED IN THE QUERY, not just checked before it. The service
        //! checks it too, so no sequential test can tell this guard from that one - it exists for
        //! the RACE: two clerks adding "P3" to one section in the same instant, where the second
        //! matches no document and is told so. It is the only conflict rule expressible without
        //! comparing times, because a stored LocalTime carries the day it was WRITTEN.
        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .and("date").is(atMidnight(date))
                .norOperator(Criteria.where("entries").elemMatch(
                        Criteria.where("classDocsId").is(entry.getClassDocsId())
                                .and("sectionNo").is(entry.getSectionNo())
                                .and("periodCode").is(entry.getPeriodCode()))));

        Update update = new Update()
                .push("entries", entry)
                //! BELT AND BRACES. Spring Data adds its own $inc for a versioned entity when
                //! the update does not carry one - measured by removing this and watching the
                //! version still move - and it does not double up when it does. Explicit anyway:
                //! #2's required version depends on this moving, and a future switch to a raw
                //! MongoCollection call would lose it with nothing to notice.
                .inc("version", 1)
                .set("updatedAt", Instant.now());

        // TODO: update daily timetable (add one period)
        return mongo.updateFirst(query, update, DailyTimetable.class).getModifiedCount();
    }

    @Override
    public long patchEntry(String schoolId, LocalDate date, String entryId, Long expectedVersion,
            Map<String, Object> set, Set<String> unset) {

        //! THE ARRAY FILTER BELOW IS WHERE THIS IS LOAD-BEARING. Spring Data's query mapper
        //! converts a String for entries._id because it knows the field's targetType - measured,
        //! and a mutation that passes the raw String to the QUERY survives every test. It cannot
        //! do that for entry._id in an array filter: that path belongs to no entity, so a String
        //! matches no element, the $set applies to nothing, and the document still reports as
        //! MATCHED. A silent success, which is what rule 2 exists to prevent.
        ObjectId id = new ObjectId(entryId);

        Criteria criteria = Criteria.where("schoolId").is(schoolId)
                .and("date").is(atMidnight(date))
                .and("entries._id").is(id);

        //! THE VERSION IS HONOURED WHEN SENT AND NOT REQUIRED, per open item 1: a targeted write
        //! cannot lose somebody else's edit to a DIFFERENT period, so demanding it would refuse
        //! two clerks working on two sections.
        //!
        //! The service compares it too, for the message. This one is for the window between that
        //! read and this write, so no sequential test can tell the two apart.
        if (expectedVersion != null) {
            criteria = criteria.and("version").is(expectedVersion);
        }

        Update update = new Update();
        for (Map.Entry<String, Object> one : set.entrySet()) {
            if (!PATCHABLE.contains(one.getKey())) {
                throw new IllegalArgumentException("not a patchable field: " + one.getKey());
            }
            update.set("entries.$[entry]." + one.getKey(), one.getValue());
        }
        for (String field : unset) {
            if (!PATCHABLE.contains(field)) {
                throw new IllegalArgumentException("not a patchable field: " + field);
            }
            update.unset("entries.$[entry]." + field);
        }

        update.inc("version", 1).set("updatedAt", Instant.now());

        //! THE IDENTIFIER HERE AND IN entries.$[entry] ABOVE MUST AGREE. MongoDB refuses the
        //! update outright when they do not, which is the good failure - a filter naming nothing
        //! would otherwise match every element.
        update.filterArray(Criteria.where("entry._id").is(id));

        //! MATCHED, NOT MODIFIED. The two agree today only because the $inc and $set above make
        //! the document change every time; matched is what actually answers "was the entry there".
        // TODO: update daily timetable (correct one period)
        return mongo.updateFirst(new Query(criteria), update, DailyTimetable.class)
                .getMatchedCount();
    }

    @Override
    public long pullEntry(String schoolId, LocalDate date, String entryId) {
        ObjectId id = new ObjectId(entryId);

        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .and("date").is(atMidnight(date)));

        Update update = new Update()
                .pull("entries", new Document("_id", id))
                .inc("version", 1)
                .set("updatedAt", Instant.now());

        //! MODIFIED IS THE RIGHT SIGNAL HERE, unlike #4: a $pull that removes nothing modifies
        //! nothing, so 0 means the entry was not there.
        // TODO: update daily timetable (remove one period)
        return mongo.updateFirst(query, update, DailyTimetable.class).getModifiedCount();
    }

    /**
     * The instant a {@code LocalDate} is stored as.
     *
     * <p><b>Local midnight, not UTC midnight</b> — {@code 2026-08-03} is written as
     * {@code 2026-08-02T18:30:00Z} in IST, so a query built from {@code Date.from(date.atStartOfDay
     * (ZoneOffset.UTC))} would miss it by the zone offset. The same conversion the mapper makes.
     */
    private static Date atMidnight(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
