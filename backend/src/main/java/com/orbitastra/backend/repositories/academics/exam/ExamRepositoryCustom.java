package com.orbitastra.backend.repositories.academics.exam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.exam.request.ExamSearchRequest;
import com.orbitastra.backend.models.academics.examination.Exam;

/** The exam reads that need a query built by hand. */
public interface ExamRepositoryCustom {

    /** One page of a year's exams, with whichever filters were sent. */
    Page<Exam> search(String schoolId, String academicYear, ExamSearchRequest request,
            Pageable pageable);
}
