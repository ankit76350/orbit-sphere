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

    /** #4 — the roll, filtered and paged. */
    Page<Student> search(String schoolId, StudentSearchRequest request, Pageable pageable);
}
