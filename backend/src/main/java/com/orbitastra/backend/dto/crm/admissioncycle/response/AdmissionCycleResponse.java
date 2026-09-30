package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;

/**
 * One admission cycle, as the API gives it back. Endpoint #1, and the reads when they are built.
 *
 * <p>The dates go back exactly as they were sent, in ISO-8601. A screen turns them into something
 * a person can read; the API does not, because a client may be in a different timezone than the
 * school.
 */
public record AdmissionCycleResponse(

        /** The id that applications will store as {@code admissionCycleDocsId}. */
        String admissionCycleId,

        String academicYear,
        String name,
        AdmissionCycleStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant applicationOpenAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant applicationCloseAt,

        /**
         * How many classes have seats set up. Zero on a new cycle.
         *
         * <p>A count and not the table itself: the table can be long, and the only thing a caller
         * needs right after creating a cycle is whether it is still empty.
         */
        int capacityCount,

        /**
         * The questions this round asks, in the order they are stored, each with the id it was
         * given. Empty when the round asks nothing extra.
         *
         * <p><b>The whole list and not a count, unlike the seats.</b> The ids are made by the
         * server, so this response is the only place the caller can learn them — and without an
         * id there is no way to store an answer against a question. A count would send them back
         * to a read to find out what they just created.
         */
        List<Question> questions,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notes,

        /**
         * What to do next. A <b>write</b> field — it says what just happened.
         *
         * <p>Left out of the JSON when null, which is what the reads pass. A read changed nothing,
         * so it has nothing to say about what happens next.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** The same cycle, for a read. No {@code nextStep}: nothing just happened. */
    public static AdmissionCycleResponse fromCycle(AdmissionCycle cycle) {
        return fromCycle(cycle, null);
    }

    public static AdmissionCycleResponse fromCycle(AdmissionCycle cycle, String nextStep) {
        return new AdmissionCycleResponse(
                cycle.getId(),
                cycle.getAcademicYear(),
                cycle.getName(),
                cycle.getStatus(),
                cycle.getApplicationOpenAt(),
                cycle.getApplicationCloseAt(),
                // Null safe because a cycle stored with an explicit null reads back null, even
                // though a missing field reads back as an empty list.
                cycle.getCapacities() == null ? 0 : cycle.getCapacities().size(),
                // Null safe for the same reason as the capacities above.
                cycle.getQuestions() == null ? List.of()
                        : cycle.getQuestions().stream()
                                .map(one -> new Question(one.getId(), one.getQuestion(),
                                        Boolean.TRUE.equals(one.getRequired())))
                                .toList(),
                cycle.getNotes(),
                nextStep);
    }

    /**
     * One question on the form.
     *
     * <p>{@code required} is never null here even though the stored field can be: a question
     * saved before this field existed reads back null, and a caller should not have to tell that
     * apart from "not required".
     */
    public record Question(String id, String question, boolean required) {
    }
}
