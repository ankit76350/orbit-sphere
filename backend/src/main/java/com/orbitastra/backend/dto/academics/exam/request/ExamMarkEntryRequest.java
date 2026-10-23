package com.orbitastra.backend.dto.academics.exam.request;

/**
 * The filter examination #26 takes.
 *
 * <p>Bound from the query string, so a value that is not true or false is a 400.
 *
 * @param pending true lists only the papers still holding something up — a hall not closed, marks
 *                missing, or marks entered but not published. Left out, every paper is listed.
 */
public record ExamMarkEntryRequest(Boolean pending) {
}
