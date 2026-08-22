package com.orbitastra.backend.models.new_new.payroll.enums;

/** Why somebody's pay changed. */
public enum SalaryRevisionType {
    /** The first structure a member of staff is given. */
    INITIAL,

    /** An annual or periodic rise in the same job. */
    INCREMENT,

    /** A rise because the job changed. */
    PROMOTION,

    /** The school changed its pay scales, affecting many people at once. */
    PAY_SCALE_REVISION,

    /** A contract renewed on new terms. */
    CONTRACT_RENEWAL,

    /** The previous structure was entered wrongly and is being put right. */
    CORRECTION,

    /** Anything the reasons above do not cover. */
    OTHER
}
