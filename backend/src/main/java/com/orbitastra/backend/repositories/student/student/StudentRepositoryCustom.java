package com.orbitastra.backend.repositories.student.student;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.student.student.request.StudentSearchRequest;
import com.orbitastra.backend.models.student.Student;

/**
 * The two reads a method name cannot express.
 *
 * <p>#4 filters on whichever of five things the caller sent, and #6 matches one needle across the
 * child's own contact <b>and</b> their guardians' — which is an OR across two collections, not a
 * field comparison.
 */
public interface StudentRepositoryCustom {

    /** #4 — the roll, filtered and paged. */
    Page<Student> search(String schoolId, StudentSearchRequest request, Pageable pageable);

    /**
     * #6 — is this child already here?
     *
     * <p>{@code guardianDocsIds} is what the guardian half of the question resolved to: #6 looks
     * the phone up in {@code guardians} first and passes the ids in, because a child is nearly
     * always found by their parent's number rather than their own.
     */
    List<Student> findKnownChild(String schoolId, String digits, boolean wholeNumber,
            String admissionNo, String name, Collection<String> guardianDocsIds, int limit);
}
