package com.orbitastra.backend.repositories.people.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.people.staff.request.StaffSearchRequest;
import com.orbitastra.backend.models.people.staff.Staff;

/**
 * The filtered read behind #7. A derived query method cannot express it — the search is a regex
 * across two fields OR-ed together, and the two presence filters ask {@code $exists}.
 */
public interface StaffRepositoryCustom {

    Page<Staff> search(String schoolId, StaffSearchRequest request, Pageable pageable);
}
