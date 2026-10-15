package com.orbitastra.backend.repositories.academics.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.academics.attendance.request.AttendanceSessionSearchRequest;
import com.orbitastra.backend.models.academics.attendance.AttendanceSession;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AttendanceSessionRepositoryImpl implements AttendanceSessionRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<AttendanceSession> search(String schoolId, String academicYear,
            AttendanceSessionSearchRequest request, Pageable pageable) {

        //! step 1 - the school and the year always, then whichever filters were sent
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        if (request.date() != null) {
            filters.add(Criteria.where("attendanceDate").is(request.date()));
        }
        if (request.classDocsId() != null && !request.classDocsId().isBlank()) {
            filters.add(Criteria.where("classDocsId").is(request.classDocsId().trim()));
        }
        if (request.sectionNo() != null && !request.sectionNo().isBlank()) {
            filters.add(Criteria.where("sectionNo").is(request.sectionNo().trim()));
        }
        if (request.sessionType() != null) {
            filters.add(Criteria.where("sessionType").is(request.sessionType()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 2 - how many match
        // TODO: read attendance sessions (how many match)
        long total = mongo.count(new Query(criteria), AttendanceSession.class);

        //! step 3 - one page of them
        // TODO: read attendance sessions (one page of them)
        List<AttendanceSession> rows =
                mongo.find(new Query(criteria).with(pageable), AttendanceSession.class);

        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public List<AttendanceSession> findDailyForSection(String schoolId,
            String academicYear, String classDocsId, String sectionNo, LocalDate from,
            LocalDate to) {

        //! The first keys match school_attendance_session_uniq.
        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .and("academicYear").is(academicYear)
                .and("classDocsId").is(classDocsId)
                .and("sectionNo").is(sectionNo)
                .and("sessionType").is(AttendanceSessionType.DAILY)
                .and("status").ne(AttendanceSessionStatus.CANCELLED)
                .and("attendanceDate").gte(from).lte(to))
                .with(Sort.by(Sort.Order.asc("attendanceDate")));

        // TODO: read attendance sessions (one section's daily registers in a range)
        return mongo.find(query, AttendanceSession.class);
    }
}
