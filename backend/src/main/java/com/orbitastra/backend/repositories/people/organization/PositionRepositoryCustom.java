package com.orbitastra.backend.repositories.people.organization;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.dto.people.organization.request.PositionSearchRequest;
import com.orbitastra.backend.models.people.organization.Position;

/**
 * The part of #15's read that a derived query cannot express.
 *
 * <p>Four optional filters combine, and only three of them are on this collection — {@code vacant}
 * is answered from employment records, which is why the service has two paths and this interface
 * has two methods.
 */
public interface PositionRepositoryCustom {

    /**
     * One page of seats, filtered and ordered in the database.
     *
     * <p>{@code request.vacant()} is <b>ignored here</b>. It cannot be: vacancy is not a field on
     * this collection, and a filter applied after paging returns short pages. The service applies
     * it, using {@link #searchAll}.
     */
    Page<Position> search(String schoolId, PositionSearchRequest request, Pageable pageable);

    /**
     * Every matching seat, unpaged, in order — what the {@code vacant} filter is applied to.
     *
     * <p><b>Unpaged on purpose, and only on that path.</b> Vacancy is computed from another
     * collection, so the set has to be counted before it can be filtered and only then paged. A
     * school's seat count is tens, occasionally hundreds; the alternative is a page that silently
     * drops rows.
     */
    List<Position> searchAll(String schoolId, PositionSearchRequest request, Sort sort);
}
