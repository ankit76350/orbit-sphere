package com.orbitastra.backend.models.new_new.inventory.enums;

/**
 * What a store is mainly for.
 *
 * <p>The type does not restrict what may be kept where; it is for grouping and for
 * sending somebody to the right place. A school will keep sugar in the kitchen store and
 * chalk in the main store, but nothing stops a bag of sugar sitting in the main store
 * on the day it arrives.
 */
public enum StoreType {
    /** The central store everything usually arrives into first. */
    MAIN,

    /** The kitchen and mess store, where food is kept. */
    KITCHEN,

    /** A hostel's own store, for linen and daily needs. */
    HOSTEL,

    /** A laboratory's store, for apparatus and chemicals. */
    LABORATORY,

    /** The sports store. */
    SPORTS,

    /** Cleaning and housekeeping supplies. */
    HOUSEKEEPING,

    /** Electrical, plumbing and repair materials. */
    MAINTENANCE,

    /** Stationery and office supplies. */
    OFFICE,

    /** Anything the types above do not cover. */
    OTHER
}
