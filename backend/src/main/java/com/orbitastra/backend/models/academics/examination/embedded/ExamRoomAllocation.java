package com.orbitastra.backend.models.academics.examination.embedded;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One room a paper is written in, how many students sit there, and who is on duty in it.
 *
 * <p>It has no collection of its own. It lives in {@code ExamSchedule.rooms}, because a paper
 * often needs more than one room — 200 children in the hall and 50 in Room 12. Each child's hall
 * entry ({@code ExamPaperAttendance.roomDocsId}) says which of these rooms they sat in.
 *
 * <p>The invigilators belong to the room, not the paper: the hall has its own people on duty, and
 * Room 12 has its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRoomAllocation {

    // Links to Room.id. Example: "67c31122dc3f7d0011223344"
    @NotBlank
    private String roomDocsId;

    // How many students sit in this room — all the sections of the sitting together (same exam,
    // subject, part, date and times), not one section. The same number is on each of those papers,
    // so capacity counts it once. Optional — but a room with no seat count cannot be shared with
    // another sitting, since nobody knows how full it is.
    // Example: 200
    @Min(1)
    private Integer seatCount;

    // Links to the Staff.id of everyone on duty in this room. Example: ["67aa15d9dc3f7d0077777777"]
    @Builder.Default
    private List<String> invigilatorDocsIds = new ArrayList<>();
}
