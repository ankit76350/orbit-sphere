package com.orbitastra.backend.repositories.academics.timetable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;

/**
 * The part of #10's read that a derived query cannot express.
 *
 * <p>Two things put it here: the filters reach <i>inside</i> the embedded periods — which needs an
 * {@code $elemMatch} to pair a class with a section — and the row is five counts over an array
 * this endpoint deliberately never ships.
 */
public interface DailyTimetableRepositoryCustom {

    /**
     * One page of a year's days, as counts rather than periods.
     *
     * <p><b>It returns the response type, and that is deliberate.</b> The rows are not
     * {@code DailyTimetable} documents — they are an aggregation's output, and inventing an
     * intermediate type to carry five ints between two layers would be a class whose only purpose
     * is to be copied into another.
     */
    Page<DailyTimetableSummaryResponse> search(String schoolId, String academicYear,
            DailyTimetableSearchRequest request, Pageable pageable);
}
