package com.orbitastra.backend.services.academics.utils;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.academics.examination.ExamSchedule;

/**
 * Small checks the paper endpoints share. A method here never calls another method here.
 */
@Component
public class ExamScheduleServiceUtils {

    /**
     * Whether another paper's time overlaps this one's. Touching ends do not overlap — a paper
     * ending at 12:00 and one starting at 12:00 can share a room.
     *
     * Used by:
     * - addPapers()
     */
    public static boolean overlaps(ExamSchedule other, LocalTime start, LocalTime end) {
        return other.getStartTime().isBefore(end) && start.isBefore(other.getEndTime());
    }

    /**
     * Whether another paper is part of the same sitting — the same exam, subject, part (ignoring
     * case, and none matching none), date and times. Sections of one sitting may share a room and
     * invigilators.
     *
     * Used by:
     * - addPapers()
     */
    public static boolean sameSitting(ExamSchedule other, String examId, String subjectCode,
            String componentName, LocalDate examDate, LocalTime start, LocalTime end) {
        boolean samePart = componentName == null
                ? other.getComponentName() == null
                : componentName.equalsIgnoreCase(other.getComponentName());
        return examId.equals(other.getExamDocsId())
                && subjectCode.equals(other.getSubjectCode())
                && samePart
                && examDate.equals(other.getExamDate())
                && start.equals(other.getStartTime())
                && end.equals(other.getEndTime());
    }

    /**
     * A paper as people say it — the subject, then its part when it has one: "MATHEMATICS" or
     * "SCIENCE (Practical)". For refusal messages.
     *
     * Used by:
     * - addPapers()
     */
    public static String paperLabel(String subjectCode, String componentName) {
        return componentName == null ? subjectCode : subjectCode + " (" + componentName + ")";
    }
}
