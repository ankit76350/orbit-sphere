package com.orbitastra.backend.dto.crm.inquiry.request;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.dto.crm.inquiry.request.InquiryCreateRequest.Guardian;
import com.orbitastra.backend.models.common.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * What #9 carries — a correction to what the front desk wrote down.
 *
 * <p><b>A lead is the school's own notes, not a declaration the family signed.</b> That is the one
 * sentence the whole endpoint follows from, and it is what makes it different from #18: an
 * application is frozen the moment the family submits it, because what they declared <i>then</i> is
 * the thing an admissions record is for. Nobody declares a lead. Somebody took a phone call and
 * wrote down what they heard, and the commonest thing that happens to a phone call is mishearing
 * it.
 *
 * <p><b>So there is no status gate.</b> A {@code LOST} lead can still have a misspelt name
 * corrected, and correcting one that reached {@code APPLICATION_SUBMITTED} touches no application:
 * #17 <i>copies</i> the guardians onto the form at the start, so the two have been separate
 * records ever since.
 *
 * <p><b>Every field is optional and a body carrying none is {@code 400 NOTHING_TO_UPDATE}</b>, the
 * same shape #18, #27 and #29b have.
 *
 * <p><b>A blank string clears an optional field.</b> {@code ""} is how a caller says "they no
 * longer have a class in mind" or "that note was a mistake", and an absent field leaves what is
 * there alone. A <i>required</i> field refuses a blank instead — the same rule #18 has for
 * {@code applicantName}, because "" there is somebody trying to remove a name the document must
 * carry.
 *
 * <p><b>{@code dateOfBirth} and {@code gender} cannot be cleared, and that is a gap rather than a
 * decision.</b> Neither is a string, so neither has a blank to send, and inventing a sentinel for
 * two fields would be worse than saying so. A date of birth entered by mistake can be corrected to
 * the right one; it cannot be taken back off the record.
 *
 * <p><b>What is NOT here is what another endpoint owns</b> — the rule this module follows
 * everywhere:
 *
 * <ul>
 *   <li>{@code status} is #12's, which is the only thing that may walk the transition table, and
 *       the only thing that may set {@code LOST} with a reason. An edit that could set the status
 *       would be a way round the table.</li>
 *   <li>{@code lostReason} is #12's for the same reason — it is meaningless without the status
 *       move that goes with it.</li>
 *   <li>{@code assignedCounselorDocsId} is #11's. Handing a lead to somebody is an event, and this
 *       module gives events verbs.</li>
 *   <li>{@code nextFollowUpAt} and {@code followUps} are #10's, which writes both together. A
 *       chase date moved without a call logged beside it is a promise with no record of who made
 *       it.</li>
 *   <li>{@code inquiryNo} is generated. Nobody picks their own.</li>
 * </ul>
 *
 * <p><b>{@code academicYear} IS here, unlike #18's cycle</b>, and the difference is real. A form
 * belongs to the round it was created against — the round decides the seat table and the window —
 * so moving it is not a correction. A lead's year is a label on a phone call: "next year" is the
 * first thing anybody says and the first thing anybody mishears. Nothing downstream reads it (#17
 * takes its year from the <i>cycle</i>, not from the lead), so the only thing it constrains is the
 * interested class — which the service re-checks when the year moves.
 */
public record InquiryUpdateRequest(

        /** Required on the document, so {@code ""} is {@code 400 BLANK_STUDENT_NAME}. */
        @Size(max = 160) String prospectiveStudentName,

        /** Required too, and it must exist in this school. Re-checks the class when it moves. */
        @Size(max = 20) String academicYear,

        @Past LocalDate dateOfBirth,

        Gender gender,

        /** A class of the year the lead is about. {@code ""} clears it. */
        @Size(max = 60) String interestedClassDocsId,

        /**
         * The guardians, <b>replaced whole</b>. {@code []} therefore clears them.
         *
         * <p><b>The guardian shape is #8's</b>, imported rather than declared again: two records
         * with identical fields are two things that drift the first time a field is added to one.
         *
         * <p>A list is one value. Merging would leave no way to remove a guardian added by
         * mistake, and there is no id on a guardian to merge <i>by</i> — they are embedded, not
         * documents.
         *
         * <p><b>No {@code @Size(min = 1)} here, unlike #18.</b> An application with no guardian is
         * not one a school can act on; a <i>lead</i> with none is the walk-in who gave a child's
         * name and left, which #8 is built to accept. Refusing to clear them would be this
         * endpoint disagreeing with the one that creates them.
         */
        @Size(max = 10) List<@Valid Guardian> guardians,

        /** Free text. {@code ""} clears it. */
        @Size(max = 60) String source,

        /** {@code ""} clears it. */
        @Size(max = 200) String sourceDetails,

        /** {@code ""} clears it. */
        @Size(max = 2000) String notes,

        Long version) {
}
