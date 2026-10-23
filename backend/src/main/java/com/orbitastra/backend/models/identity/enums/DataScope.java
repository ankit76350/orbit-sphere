package com.orbitastra.backend.models.identity.enums;

/**
 * How many records a permission reaches.
 *
 * <p>Two teachers can both have VIEW on attendance and still not see the same
 * thing. This is what tells them apart. Without it, letting a teacher mark
 * attendance would also let them read every other class in the school.
 *
 * <p>The scope is worked out by the service when it builds the query, and it
 * always narrows the results. It never widens them.
 */
public enum DataScope {
    /** Only records about the person themself, such as a parent's own children. */
    OWN,

    /** Records for the classes, sections or students the person is assigned to. */
    ASSIGNED,

    /** Every record in the school. */
    SCHOOL
}
