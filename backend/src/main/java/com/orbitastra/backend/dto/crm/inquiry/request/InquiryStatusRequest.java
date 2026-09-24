package com.orbitastra.backend.dto.crm.inquiry.request;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #12 carries — <b>move the lead, and say why when it is being given up on</b>.
 *
 * <p><b>#10 can move a lead too, and the split between them is the point.</b> #10 logs a call that
 * <i>happened to</i> move it; #12 is the move on its own. A counsellor who rings and books a visit
 * uses #10 and the note goes on the timeline beside the move. A school writing a family off in
 * January because nobody has answered since October uses this: there was no call, and pretending
 * there was one to record the outcome would put a fiction in the timeline.
 *
 * <p><b>#12 is the only thing that may set {@code LOST}</b>, because it is the only one with
 * somewhere to put the reason. #10 refuses it with {@code 409 LOST_NEEDS_A_REASON} and names this
 * endpoint.
 *
 * <p><b>It still cannot set the two the application half owns.</b> {@code APPLICATION_STARTED} and
 * {@code APPLICATION_SUBMITTED} are facts about a form — #17 and #19 set them — and they are on
 * the transition table as legal <i>moves</i> rather than as things anybody may type.
 */
public record InquiryStatusRequest(

        /**
         * Where the lead is going. <b>Required — a move to nowhere is not a move.</b>
         *
         * <p>Walks the transition table. Sending the status it already has is
         * {@code 409 INQUIRY_TRANSITION_NOT_ALLOWED} here, <b>unlike #10</b>, and the difference
         * is deliberate: #10's status is a detail of a call that did happen, so echoing the
         * current one is harmless. This endpoint's whole job is the move, and a request that moves
         * nothing has asked for nothing.
         */
        @NotNull InquiryStatus status,

        /**
         * Why the family is being given up on. <b>Required when — and only when — the status is
         * {@code LOST}.</b>
         *
         * <p><b>A lead marked lost with no reason is a record that answers nothing.</b> The
         * question anybody asks of a lost lead six months later is <i>why</i>, and "LOST" on its
         * own is the one thing that cannot answer it — which is the whole reason #10 may not set
         * this status.
         *
         * <p>Sending it with any other status is {@code 400 LOST_REASON_NOT_ALLOWED} rather than
         * being quietly dropped: a reason attached to a move that is not a loss is a caller who
         * has misunderstood something, and silence would let them go on believing it.
         */
        @Size(max = 500) String lostReason,

        /**
         * What to put on the timeline beside the move. <b>Optional.</b>
         *
         * <p><b>Every move lands on the timeline whether this is sent or not</b>, because a
         * history that shows every phone call but not the moment a family was written off would be
         * misleading about the one thing that matters most. This is only the words.
         *
         * <p>For a {@code LOST} move it defaults to the reason, which is almost always the
         * sentence somebody would have typed here anyway.
         */
        @Size(max = 2000) String note,

        /**
         * Who moved it. <b>This school's staff, when it is sent.</b>
         *
         * <p>Optional for the reason it is optional on #10: nothing in this project knows who is
         * asking yet, so the only way to fill it is for the caller to say.
         */
        @Size(max = 60) String counselorDocsId,

        /** Optimistic check. Absent skips it. */
        Long version) {
}
