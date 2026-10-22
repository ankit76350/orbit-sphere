package com.orbitastra.backend.dto.academics.examattendance.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;

/**
 * What examination #14 answers: one paper's hall register — the paper, its rooms, the counts, and
 * every child expected in roll order with their hall entry or none.
 *
 * <p>A debarred child is not listed by name. {@code counts.debarred} says how many, so the
 * invigilator knows a gap is deliberate.
 */
public record ExamHallRegisterResponse(

        String examScheduleDocsId,

        String examDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String examName,

        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        String subjectCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String componentName,

        LocalDate examDate,

        LocalTime startTime,

        LocalTime endTime,

        ExamScheduleStatus status,

        /** The paper's rooms — a hall entry names one of these. */
        List<Room> rooms,

        Counts counts,

        /** Every child expected, in roll order, then by name. */
        List<Row> students,

        /** The paper's version, for the next hall save (#15). */
        Long version) {

    /** One of the paper's rooms. */
    public record Room(String roomDocsId, String roomName) {
    }

    /**
     * The register's numbers.
     *
     * @param expected      every row listed
     * @param marked        rows with a hall entry
     * @param notMarked     rows without one — #16 refuses while this is above 0
     * @param byStatus      marked rows per attendance status, every status listed
     * @param debarred      debarred children in the section with no entry — not listed by name
     * @param notCandidates children in the section who are not on the exam's candidate list
     */
    public record Counts(int expected, int marked, int notMarked, Map<String, Integer> byStatus,
            int debarred, int notCandidates) {
    }

    /**
     * One child on the register.
     *
     * @param candidateStatus ELIGIBLE, or DEBARRED for a child debarred after they sat this paper
     * @param inSection       false for a child marked here who has since left the section
     * @param entry           their hall entry, or null when not marked yet
     */
    public record Row(String studentDocsId, String fullName, String admissionNo, String rollNo,
            String examCandidateDocsId, ExamCandidateStatus candidateStatus, boolean inSection,
            Entry entry) {
    }

    /**
     * A hall entry. {@code late} and {@code leftEarly} are worked out from the times against the
     * paper's, in the school's time zone — they are not stored.
     */
    public record Entry(String id, ExamAttendanceStatus attendanceStatus, String answerCopyNo,
            List<String> additionalAnswerCopyNos, String roomDocsId, String roomName, String seatNo,
            Instant reportedAt, Instant submittedAt, boolean late, boolean leftEarly, String remarks,
            Instant markedAt, String markedByDocsId) {
    }
}
