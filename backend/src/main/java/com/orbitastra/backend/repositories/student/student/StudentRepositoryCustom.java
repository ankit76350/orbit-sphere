package com.orbitastra.backend.repositories.student.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.student.student.request.StudentSearchRequest;
import com.orbitastra.backend.models.student.Student;

/**
 * The one read a method name cannot express.
 *
 * <p>#4 filters on whichever of five things the caller sent, so the query has to be built while it
 * runs rather than declared as a method name.
 *
 * <p><b>It had a second until 2026-10-08</b> — a read behind the removed #6, which matched one
 * needle across a child's own contact <i>and</i> their guardians'. It went with the endpoint
 * rather than being left for something that might want it one day.
 */
public interface StudentRepositoryCustom {

    /**
     * #4 — the roll, filtered and paged.
     *
     * <p>{@code guardianDocsIds} is what the phone and email filters resolved to: the service looks
     * them up in {@code guardians} first and passes the ids in, because a child is found through
     * their parent far more often than through their own contact details. Empty means those filters
     * were not sent, or matched nobody.
     *
     * <p>{@code digits} and {@code wholeNumber} are the phone question already worked out, so this
     * query and the guardian one agree about what "the same number" means.
     */
    Page<Student> search(String schoolId, StudentSearchRequest request, String digits,
            boolean wholeNumber, java.util.Collection<String> guardianDocsIds, Pageable pageable);
}
