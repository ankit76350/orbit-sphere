package com.orbitastra.backend.repositories.student.guardian;

import java.util.List;

import com.orbitastra.backend.models.student.Guardian;

/**
 * The one guardian read a method name cannot express.
 *
 * <p>#6 asks about a phone number the way a person says it, not the way it is stored, so the
 * comparison is on the digits with anything allowed between them. That is a regex built while the
 * query runs.
 */
public interface GuardianRepositoryCustom {

    /**
     * Everybody whose number is this number, however either side spelled it.
     *
     * <p>#6 turns the answer into children: the number a school holds for a seven year old is
     * their mother's, so a child is found through their guardian far more often than through
     * their own contact details.
     */
    List<Guardian> findByLoosePhone(String schoolId, String digits, boolean wholeNumber, int limit);
}
