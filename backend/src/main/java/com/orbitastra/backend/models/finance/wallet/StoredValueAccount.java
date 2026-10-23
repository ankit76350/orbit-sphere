package com.orbitastra.backend.models.finance.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.finance.enums.WalletAccountStatus;
import com.orbitastra.backend.models.finance.enums.WalletAccountType;
import com.orbitastra.backend.models.finance.enums.WalletOwnerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A wallet holding money the school has taken but not yet earned, such as fees
 * paid in advance or pocket money kept for a student.
 *
 * <p>This is the replacement for the older student wallet that stored only a
 * balance. The balance here is a running total, and StoredValueLedgerEntry holds
 * every movement that made it. If the two ever disagree, the entries win and the
 * balance is rebuilt from them.
 *
 * <p>Money in a wallet still belongs to the family, so in the books it is a
 * liability, not income. It only becomes income once it is used to pay an
 * invoice.
 *
 * <p>{@code availableBalance} is what can be spent now. {@code heldBalance} is
 * money set aside for something already agreed, such as a trip that has not been
 * billed yet. The real total the school is holding is the two added together.
 *
 * <p>{@code lastLedgerSequence} is the number given to the newest entry. The
 * service reads it, adds one, and writes the next entry with that number, which
 * is what keeps the wallet history in a strict order with no gaps.
 */
@Document(collection = "stored_value_accounts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_wallet_account_no_uniq",
                def = "{'schoolId': 1, 'accountNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_wallet_owner_type_uniq",
                def = "{'schoolId': 1, 'ownerType': 1, 'ownerDocsId': 1, 'accountType': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_wallet_status_balance_idx",
                def = "{'schoolId': 1, 'status': 1, 'availableBalance': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StoredValueAccount extends SchoolBase {

    // School-scoped number from NumberSequence type WALLET_ACCOUNT.
    // Example: "WLT/2026/000341"
    @NotBlank
    private String accountNo;

    // Who the money belongs to. Example: WalletOwnerType.STUDENT
    @NotNull
    private WalletOwnerType ownerType;

    // Links to the owner named by ownerType, usually Student.id.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String ownerDocsId;

    // What the money is kept for. Example: WalletAccountType.FEE_ADVANCE
    @NotNull
    private WalletAccountType accountType;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Money that can be spent now. Example: 3200.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    // Money set aside for something already agreed. Example: 800.00
    @NotNull
    @Builder.Default
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal heldBalance = BigDecimal.ZERO;

    // Example: WalletAccountStatus.ACTIVE
    @NotNull
    @Builder.Default
    private WalletAccountStatus status = WalletAccountStatus.ACTIVE;

    // Number given to the newest ledger entry. Example: 47
    @NotNull
    @Builder.Default
    private Long lastLedgerSequence = 0L;

    // When the newest entry was written. Example: 2026-06-14T08:30:00Z
    private Instant lastEntryAt;

    // Balance below which the family is told to top up. Example: 500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lowBalanceThreshold;

    // Whether wallet money may be used automatically for a new invoice.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean autoPayFromWallet = false;

    // Why the wallet was frozen. Example: "Frozen while a parent query is open."
    private String freezeReason;

    // Example: 2027-04-15T05:00:00Z
    private Instant closedAt;
}
