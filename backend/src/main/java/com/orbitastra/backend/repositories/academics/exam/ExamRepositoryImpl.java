package com.orbitastra.backend.repositories.academics.exam;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.academics.exam.request.ExamSearchRequest;
import com.orbitastra.backend.models.academics.examination.Exam;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExamRepositoryImpl implements ExamRepositoryCustom {

    private final MongoTemplate mongo;

    /**
     * One page of a year's exams. The school and the year always apply; the term, type, status
     * and name filters only when sent.
     */
    @Override
    public Page<Exam> search(String schoolId, String academicYear, ExamSearchRequest request,
            Pageable pageable) {

        //! step 1 - the school and the year, then whichever filters were sent
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("academicYear").is(academicYear));

        if (request.termDocsId() != null && !request.termDocsId().isBlank()) {
            filters.add(Criteria.where("termDocsId").is(request.termDocsId().trim()));
        }
        if (request.examType() != null) {
            filters.add(Criteria.where("examType").is(request.examType()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }
        //! The search text is quoted, so "(" is looked for as a bracket rather than breaking the query.
        if (request.search() != null && !request.search().isBlank()) {
            filters.add(Criteria.where("name").regex(Pattern.quote(request.search().trim()), "i"));
        }
        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 2 - how many match, for totalElements
        // TODO: count exams
        long total = mongo.count(new Query(criteria), Exam.class);

        //! step 3 - one page of them, in the order asked for
        // TODO: search exams
        List<Exam> rows = mongo.find(new Query(criteria).with(pageable), Exam.class);

        return new PageImpl<>(rows, pageable, total);
    }
}
