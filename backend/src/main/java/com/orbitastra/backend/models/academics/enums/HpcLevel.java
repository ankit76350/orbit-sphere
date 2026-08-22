package com.orbitastra.backend.models.academics.enums;

/**
 * How far along a child is in one learning domain.
 *
 * <p>These are the words the Holistic Progress Card actually uses. They are deliberately
 * not marks, not grades and not percentages, and the whole point of the card is that they
 * are not comparable between children the way a mark is.
 *
 * <p>The metaphor runs uphill and is meant to be read to a child: a stream is where
 * everybody starts, and the sky is not a place anybody is expected to arrive at by a
 * particular age. A child at STREAM in one domain and MOUNTAIN in another is normal, and a
 * card full of SKY would be a sign the teacher had misunderstood the exercise.
 *
 * <p>There is deliberately no failing level. A traditional report card can say a child
 * failed; this cannot, because a level is a description of where somebody is on the way
 * rather than a judgement about whether they got there.
 */
public enum HpcLevel {
    /** Beginning. Needs steady support to take part. */
    STREAM,

    /** Growing. Takes part with some help and is starting to manage alone. */
    RIVER,

    /** Confident. Manages alone and applies it in new situations. */
    MOUNTAIN,

    /** Flourishing. Extends it further and helps others do the same. */
    SKY
}
