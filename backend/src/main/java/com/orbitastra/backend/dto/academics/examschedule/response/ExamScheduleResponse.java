package com.orbitastra.backend.dto.academics.examschedule.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;

/**
 * One paper of an exam — one subject (and component) for one section — with its class, subject,
 * room and invigilators named.
 */
public record ExamScheduleResponse(

        String examScheduleDocsId,

        String examDocsId,

        String classDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String className,

        String sectionNo,

        String subjectCode,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String subjectName,

        /** The part of the subject. Absent when the subject has one paper. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String componentName,

        LocalDate examDate,

        LocalTime startTime,

        LocalTime endTime,

        BigDecimal maximumMarks,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal passingMarks,

        /** Absent while no room has been given. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String facilityResourceDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String roomName,

        List<Invigilator> invigilators,

        ExamScheduleStatus status,

        /** For the next write to this paper. */
        Long version) {

    /** One member of staff on duty, with their name. */
    public record Invigilator(String staffDocsId, String fullName) {
    }

    /** Builds the answer from the saved paper and the names behind its ids. */
    public static ExamScheduleResponse of(ExamSchedule paper, String className, String subjectName,
            String roomName, List<Invigilator> invigilators) {

        return new ExamScheduleResponse(
                paper.getId(),
                paper.getExamDocsId(),
                paper.getClassDocsId(),
                className,
                paper.getSectionNo(),
                paper.getSubjectCode(),
                subjectName,
                paper.getComponentName(),
                paper.getExamDate(),
                paper.getStartTime(),
                paper.getEndTime(),
                paper.getMaximumMarks(),
                paper.getPassingMarks(),
                paper.getFacilityResourceDocsId(),
                roomName,
                invigilators,
                paper.getStatus(),
                paper.getVersion());
    }
}
