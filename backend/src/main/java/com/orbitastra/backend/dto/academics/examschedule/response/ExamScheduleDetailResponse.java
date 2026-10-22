package com.orbitastra.backend.dto.academics.examschedule.response;

import java.util.Map;

/**
 * One paper in full (examination #12): the paper with its names, then who is expected in the hall
 * and how they were marked, then the marks entered for it.
 */
public record ExamScheduleDetailResponse(

        /** The paper itself, the same shape #8 and #11 return. */
        ExamScheduleResponse paper,

        String examName,

        Hall hall,

        Marks marks) {

    /**
     * Who should be in the hall, and what the hall register says so far.
     *
     * @param sectionSize   children in the section on the paper's date
     * @param eligible      of those, the ELIGIBLE candidates of the exam
     * @param debarred      of those, the DEBARRED candidates — not on the register
     * @param notCandidates of those, children who are not candidates at all — not on the register
     * @param expected      who the register lists: eligible candidates, plus anyone already marked
     * @param present       hall entries marked PRESENT
     * @param absent        marked ABSENT
     * @param unfairMeans   marked UNFAIR_MEANS
     * @param exempt        marked EXEMPT
     * @param notMarked     expected, and not marked yet
     */
    public record Hall(int sectionSize, int eligible, int debarred, int notCandidates,
            int expected, int present, int absent, int unfairMeans, int exempt, int notMarked) {
    }

    /**
     * The marks entered for this paper.
     *
     * @param byStatus   marks per status — DRAFT, PUBLISHED; every status listed
     * @param entered    marks entered in all
     * @param notEntered children marked PRESENT who have no mark yet
     */
    public record Marks(Map<String, Long> byStatus, long entered, long notEntered) {
    }
}
