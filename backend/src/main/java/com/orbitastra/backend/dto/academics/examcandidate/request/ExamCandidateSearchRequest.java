package com.orbitastra.backend.dto.academics.examcandidate.request;

import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamDebarmentReason;

/**
 * The filters examination #44 takes, all optional and all combined with AND.
 *
 * <p>Bound from the query string, so a status or reason that does not exist is a 400.
 * {@code ?status=DEBARRED&debarmentReason=FEES_DUE} is the fee defaulters.
 *
 * @param classDocsId     only children whose academic record is in this class.
 * @param sectionNo       only this section, ignoring case. Usually with classDocsId.
 * @param status          only ELIGIBLE, or only DEBARRED.
 * @param debarmentReason only children debarred for this reason.
 */
public record ExamCandidateSearchRequest(
        String classDocsId,
        String sectionNo,
        ExamCandidateStatus status,
        ExamDebarmentReason debarmentReason) {
}
