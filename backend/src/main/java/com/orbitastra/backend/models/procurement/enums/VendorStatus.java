package com.orbitastra.backend.models.procurement.enums;

/**
 * Whether the school may buy from this vendor.
 *
 * <p>BLACKLISTED and INACTIVE both stop new orders, and they are kept apart on purpose.
 * INACTIVE is a vendor nobody uses any more: the shop closed, or the school found somebody
 * cheaper. BLACKLISTED is a vendor the school has decided it will never buy from again,
 * because they short-delivered, billed for goods that never arrived, or supplied something
 * unsafe.
 *
 * <p>Merging the two would lose the reason, and the reason is the whole value. A year
 * later somebody raising a purchase sees a name in a dropdown and needs to know whether it
 * is merely dormant or whether the school was cheated by it.
 *
 * <p>SUSPENDED is the middle state, and it is the honest one for most disputes: an unpaid
 * bill or a bad delivery is being argued about and nothing new should be ordered until it
 * is settled. Without it, every argument becomes a permanent judgement.
 */
public enum VendorStatus {
    /** Approved, and may be named on a new purchase order. */
    ACTIVE,

    /** Something is being sorted out. No new orders until it is. */
    SUSPENDED,

    /** No longer used, with nothing held against them. */
    INACTIVE,

    /** Barred. The school has decided never to buy from them again. */
    BLACKLISTED
}
