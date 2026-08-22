package com.orbitastra.backend.models.library;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.library.enums.BorrowerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The library's borrowing rules for one kind of borrower.
 *
 * <p>A teacher normally keeps a book longer and may hold more at once than a Class III
 * child, so the rules live in a row per borrower type rather than as constants in code.
 * A school that wants to change its issue period should not need a deployment.
 *
 * <p>The rules here are the **current** ones. They are not the rules a particular book
 * went out under: each issued book copies {@code dailyFineAmount} and {@code maximumFineAmount}
 * onto itself when it is issued, so shortening the issue period in November cannot make
 * a book borrowed in October retroactively overdue, and raising the fine cannot change
 * what an existing borrower owes.
 *
 * <p>That is the same rule ConcessionRequest follows when it copies a rate from its
 * policy, and TransportAllocation when it copies a fare from a stop. A policy is a price
 * list, never a promise already made.
 *
 * <p>Because each issued book snapshots what it needs, this model has no version number. There
 * is one active row per borrower type and it may be edited freely.
 *
 * <p>{@code maximumFineAmount} matters more than it looks. Without it a book forgotten
 * over the summer holidays comes back owing more than the book cost, which no school
 * will actually collect, so the fine stops being a deterrent and becomes a number
 * everybody ignores.
 *
 * <p>The service checks that one active policy exists per borrower type, and that the
 * maximum fine is not less than a single day's fine.
 */
@Document(collection = "library_policies")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_library_policy_borrower_uniq",
                def = "{'schoolId': 1, 'borrowerType': 1}",
                unique = true,
                partialFilter = "{'active': true}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryPolicy extends SchoolBase {

    // Who these rules are for. Example: BorrowerType.STUDENT
    @NotNull
    private BorrowerType borrowerType;

    // How many books this kind of borrower may hold at once. Example: 2
    @NotNull
    @Positive
    private Integer maximumBooksAtOnce;

    // How many days a book may be kept. Example: 14
    @NotNull
    @Positive
    private Integer issuePeriodDays;

    // How many times it may be extended, when nobody else is waiting.
    // Example: 1
    @NotNull
    @Builder.Default
    private Integer renewalLimit = 0;

    // Charged for each day a book is late. Example: 2.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal dailyFineAmount;

    // The most a single late book may ever cost, however long it is out. Without
    // this a book forgotten over the holidays owes more than it cost.
    // Example: 200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumFineAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How many days a returned copy is held for whoever is next in the queue before
    // the hold expires. Example: 3
    @NotNull
    @Builder.Default
    private Integer reservationHoldDays = 3;

    // Links to FeeHead.id that late fines are billed under, normally a head with
    // FeeCategory.FINE. Example: "67ac1188dc3f7d0011aa22bb"
    private String fineFeeHeadDocsId;

    // Whether these are the rules in force. Old rows are kept turned off so a change
    // is readable. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
