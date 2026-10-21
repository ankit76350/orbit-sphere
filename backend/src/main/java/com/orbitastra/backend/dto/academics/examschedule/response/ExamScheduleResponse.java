package com.orbitastra.backend.dto.academics.examschedule.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.academics.examination.embedded.ExamRoomAllocation;

/**
 * One paper of an exam — one subject (and part) for one section — with its class, subject, rooms
 * and invigilators named.
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

        /** The rooms, each with its seat count and invigilators. Empty while none is given. */
        List<RoomAllocation> rooms,

        /** All the rooms' seats added up — the most students this paper can seat for the section. */
        int totalSeats,

        ExamScheduleStatus status,

        /** For the next write to this paper. */
        Long version) {

    /** One member of staff on duty, with their name. */
    public record Invigilator(String staffDocsId, String fullName) {
    }

    /** One room of the paper, with its name, seats and the people on duty in it. */
    public record RoomAllocation(String roomDocsId, String roomName, int seatCount,
            List<Invigilator> invigilators) {
    }

    /** Builds the answer from the saved paper and the names behind its room and staff ids. */
    public static ExamScheduleResponse of(ExamSchedule paper, String className, String subjectName,
            Map<String, String> roomNames, Map<String, String> staffNames) {

        List<RoomAllocation> rooms = paper.getRooms().stream()
                .map(room -> new RoomAllocation(room.getRoomDocsId(),
                        roomNames.get(room.getRoomDocsId()), room.getSeatCount(),
                        room.getInvigilatorDocsIds().stream()
                                .map(id -> new Invigilator(id, staffNames.get(id)))
                                .toList()))
                .toList();
        int totalSeats = paper.getRooms().stream().mapToInt(ExamRoomAllocation::getSeatCount).sum();

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
                rooms,
                totalSeats,
                paper.getStatus(),
                paper.getVersion());
    }
}
