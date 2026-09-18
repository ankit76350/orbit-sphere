package com.orbitastra.backend.repositories.people.department;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.dto.people.department.request.DepartmentSearchRequest;
import com.orbitastra.backend.models.people.department.Department;

/**
 * The two reads a derived method name cannot express: five optional filters in any combination,
 * and the same filter without paging.
 */
public interface DepartmentRepositoryCustom {

    /** One page of a school's departments. The flat shape. */
    Page<Department> search(String schoolId, DepartmentSearchRequest request, Pageable pageable);

    /**
     * Every department matching the filter, unpaged and ordered.
     *
     * <p><b>The tree is built from this — one read, not a query per level.</b> A school has tens
     * of departments; a read-per-level would be a query storm for a structure that fits in memory
     * comfortably.
     */
    List<Department> searchAll(String schoolId, DepartmentSearchRequest request, Sort sort);
}
