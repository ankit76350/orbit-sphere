package com.orbitastra.backend.dto.academics.examattendance.request;

import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.ExamAttendanceStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What examination #15 needs to mark a paper's hall — one child, several or all. Re-sending a
 * child corrects their entry.
 *
 * @param marks   one per child, each child once
 * @param version <b>the paper's version</b>, as #14 last gave it. It moves on with every save, so
 *                two invigilators saving the same register cannot overwrite each other.
 */
public record ExamHallMarksRequest(
        @NotEmpty @Size(max = 300) List<@NotNull @Valid Mark> marks,
        @NotNull Long version) {

    /**
     * One child's hall entry.
     *
     * @param studentDocsId           the child — one #14 lists
     * @param attendanceStatus        PRESENT, ABSENT, UNFAIR_MEANS or EXEMPT
     * @param answerCopyNo            the booklet. Required when PRESENT or UNFAIR_MEANS, not
     *                                allowed when ABSENT or EXEMPT. Used once in the whole exam.
     * @param additionalAnswerCopyNos supplementary booklets, under the same rules
     * @param roomDocsId              which of the paper's rooms. Optional when the paper has one
     *                                room — that room is used
     * @param seatNo                  seat or desk in that room
     * @param reportedAt              when the child came in
     * @param submittedAt             when they handed in the booklet. Not before reportedAt
     * @param remarks                 what the invigilator writes
     */
    public record Mark(
            @NotBlank @Size(max = 60) String studentDocsId,
            @NotNull ExamAttendanceStatus attendanceStatus,
            @Size(max = 40) String answerCopyNo,
            @Size(max = 20) List<@NotBlank @Size(max = 40) String> additionalAnswerCopyNos,
            @Size(max = 60) String roomDocsId,
            @Size(max = 40) String seatNo,
            Instant reportedAt,
            Instant submittedAt,
            @Size(max = 500) String remarks) {
    }
}
