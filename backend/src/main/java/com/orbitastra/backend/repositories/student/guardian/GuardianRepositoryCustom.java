package com.orbitastra.backend.repositories.student.guardian;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.student.guardian.request.GuardianSearchRequest;
import com.orbitastra.backend.models.student.Guardian;

/**
 * The guardian reads a method name cannot express.
 *
 * <p>Both are about a phone number asked the way a person says it rather than the way it is
 * stored, so the comparison is on the digits with anything allowed between them — a regex built
 * while the query runs. #9 adds two more optional filters on top, which a derived method name
 * cannot carry either.
 */
public interface GuardianRepositoryCustom {

    /**
     * Everybody whose number is this number, however either side spelled it.
     *
     * <p>#6 turns the answer into children: the number a school holds for a seven year old is
     * their mother's, so a child is found through their guardian far more often than through
     * their own contact details.
     *
     * <p><b>{@code includeAlternate} is the difference between finding a person and identifying
     * one.</b> #6 and #9 want the family landline counted — it is how a second parent is found.
     * #7 must <b>not</b> count it: the alternate number is deliberately shared, and refusing on it
     * would make a mother impossible to add once the father listed the landline as his second
     * number. The unique index is on {@code phoneNumber} alone, and #7's refusal follows it.
     */
    List<Guardian> findByLoosePhone(String schoolId, String digits, boolean wholeNumber,
            boolean includeAlternate, int limit);

    /**
     * One page of this school's guardians, filtered by whichever of the three were sent.
     *
     * <p><b>No filters at all is every guardian in the school</b>, and that is the point: #9 is
     * the only read of this collection, so it has to be able to list it. See
     * {@link GuardianSearchRequest} for why that differs from the endpoints it resembles.
     *
     * <p>{@code digits} and {@code wholeNumber} are the phone question already worked out by the
     * service, the same pair {@link #findByLoosePhone} takes — so the two reads agree about what
     * "the same number" means.
     */
    Page<Guardian> search(String schoolId, GuardianSearchRequest request, String digits,
            boolean wholeNumber, Pageable pageable);
}
