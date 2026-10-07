package com.orbitastra.backend.repositories.student.guardian;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.student.Guardian;

/**
 * Reads and writes for the {@code guardians} collection.
 *
 * <p><b>Matching is the whole reason this exists separately.</b> A guardian is one real person per
 * school, and two siblings share a father — so #1 has to find the person who is already there and
 * link them, rather than writing a second row. The database agrees:
 * {@code school_guardian_phone_uniq} and {@code school_guardian_email_uniq} are both unique, so
 * the second sibling's admission would fail on a duplicate key if we got this wrong.
 */
public interface GuardianRepository
        extends MongoRepository<Guardian, String>, GuardianRepositoryCustom {

    /**
     * The match, by phone. This is the one that matters: the phone number is what the unique index
     * says identifies a person here.
     *
     * <p>Scoped by school in the query, like everything else. A number is unique <i>per school</i>,
     * not across the product.
     */
    Optional<Guardian> findBySchoolIdAndPhoneNumber(String schoolId, String phoneNumber);

    /**
     * The match, by email — for the family that gives an address and no number.
     *
     * <p>Tried second and only when the phone found nobody, because the phone is what a school
     * actually rings.
     */
    Optional<Guardian> findBySchoolIdAndEmailAddress(String schoolId, String emailAddress);

    /**
     * Several guardians at once, for a child's page and for a list of rows.
     *
     * <p><b>ONE QUERY FOR A WHOLE PAGE, not one per row.</b> A roll of thirty children with two
     * contacts each is one read, not sixty — the N+1 this project keeps naming.
     */
    List<Guardian> findBySchoolIdAndIdIn(String schoolId, Collection<String> ids);

}
