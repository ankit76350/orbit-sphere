package com.orbitastra.backend.models.new_new.library;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.library.enums.BorrowerType;
import com.orbitastra.backend.models.new_new.library.enums.ReservationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Somebody waiting for a title whose copies are all out.
 *
 * <p>A reservation is against the **title**, not a copy. The borrower does not care
 * which of the three copies they get, and holding a particular one would leave it on a
 * shelf while somebody else returns a different copy that then sits unclaimed.
 * {@code allocatedBookCopyDocsId} is filled in only once a copy actually comes back and
 * is set aside.
 *
 * <p>{@code queuePosition} is stored rather than worked out from the request times. It
 * has to survive somebody in the middle cancelling: recalculating positions on every
 * read means the person who was third becomes second without anybody deciding it, and a
 * librarian cannot then explain the order to a child who asks.
 *
 * <p>{@code holdExpiresAt} is the part that keeps the queue moving. A copy held for
 * somebody who never comes is a copy nobody else can borrow, so an uncollected hold
 * expires after the policy's {@code reservationHoldDays} and the next person is offered
 * it.
 *
 * <p>This is the most droppable model in the package. A school library that tells
 * children to come back and check does not need it. Everything else here works without
 * it.
 *
 * <p>The service checks that a borrower does not queue twice for the same title, that a
 * reservation is refused when a copy is already AVAILABLE, that allocating a copy sets
 * it to RESERVED so it cannot be lent to somebody else, and that an expired hold
 * releases the copy and moves the queue on.
 */
@Document(collection = "book_reservations")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_reservation_borrower_book_uniq",
                def = "{'schoolId': 1, 'libraryBookDocsId': 1, 'borrowerType': 1, 'borrowerDocsId': 1}",
                unique = true,
                partialFilter = "{'status': {'$in': ['WAITING', 'READY_FOR_COLLECTION']}}"),
        @CompoundIndex(
                name = "school_reservation_queue_idx",
                def = "{'schoolId': 1, 'libraryBookDocsId': 1, 'status': 1, 'queuePosition': 1}"),
        @CompoundIndex(
                name = "school_reservation_expiry_idx",
                def = "{'schoolId': 1, 'status': 1, 'holdExpiresAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookReservation extends SchoolBase {

    // Links to LibraryBook.id. Against the title, not a copy.
    // Example: "67b91124dc3f7d0033445566"
    @NotBlank
    private String libraryBookDocsId;

    // Whether a student or a staff member is waiting.
    // Example: BorrowerType.STUDENT
    @NotNull
    private BorrowerType borrowerType;

    // Links to Student.id or Staff.id, depending on borrowerType.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String borrowerDocsId;

    // Example: ReservationStatus.WAITING
    @NotNull
    @Builder.Default
    private ReservationStatus status = ReservationStatus.WAITING;

    // Place in the queue, starting at 1. Stored so it survives somebody in the
    // middle cancelling, and so a librarian can explain the order. Example: 2
    @NotNull
    private Integer queuePosition;

    // When they asked. Example: 2026-08-19T06:10:00Z
    @NotNull
    private Instant requestedAt;

    // Links to BookCopy.id, once a copy has come back and been set aside for them.
    // Null while still waiting. Example: "67b91125dc3f7d0044556677"
    private String allocatedBookCopyDocsId;

    // When a copy was set aside. Example: 2026-08-24T05:30:00Z
    private Instant readyAt;

    // When the hold runs out, worked out from the policy's reservationHoldDays. A
    // copy nobody collects has to go back into circulation.
    // Example: 2026-08-27T05:30:00Z
    private Instant holdExpiresAt;

    // Links to BookLoan.id once the reservation turned into a loan.
    // Example: "67b91126dc3f7d0055667788"
    private String bookLoanDocsId;

    // When it ended, however it ended. Example: 2026-08-25T04:20:00Z
    private Instant closedAt;

    // Why, when it was cancelled or expired.
    // Example: "Did not collect within three days."
    private String closureReason;
}
