package com.orbitastra.backend.dto.academics.exam.request;

import com.orbitastra.backend.models.academics.enums.ExamStatus;
import com.orbitastra.backend.models.academics.enums.ExamType;

/**
 * The filters examination #6 takes, all optional and all combined with AND.
 *
 * <p>Bound from the query string, so a value that is not a known type or status is a 400.
 */
public record ExamSearchRequest(

        /** Only this term's exams. */
        String termDocsId,

        /** Only exams of this type, such as MIDTERM. */
        ExamType examType,

        /** Only exams in this status, such as DRAFT. */
        ExamStatus status,

        /** Part of the name, ignoring upper and lower case. Blank is treated as absent. */
        String search,

        Integer page,
        Integer size,
        String sort) {
}
