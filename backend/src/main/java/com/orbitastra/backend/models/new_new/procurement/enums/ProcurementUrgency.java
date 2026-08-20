package com.orbitastra.backend.models.new_new.procurement.enums;

/**
 * How soon something is needed, which decides how much checking it gets.
 *
 * <p>This is not decoration. It is the field that says which approval path a request
 * takes. A school that treats every purchase the same either makes everybody wait a week
 * for a light bulb, or lets a large order through with nobody looking at it.
 *
 * <p>EMERGENCY is deliberately narrow: the kitchen has no gas and lunch is in three hours.
 * It exists so that buying first and approving afterwards is a recorded, countable thing
 * rather than something people do quietly. A school with many emergency purchases has a
 * planning problem, and it should be able to see that.
 */
public enum ProcurementUrgency {
    /** The ordinary case. Goes through the full approval path. */
    NORMAL,

    /** Needed soon enough that it should jump the queue. */
    URGENT,

    /** Needed today. May be bought before approval, and approved afterwards. */
    EMERGENCY
}
