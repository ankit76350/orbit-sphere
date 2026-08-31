package com.orbitastra.backend.common.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * One page of anything, with the numbers a caller needs to ask for the next one.
 *
 * <p>Generic and in {@code common} rather than beside the first endpoint that lists something,
 * because every list endpoint after it wants the identical envelope. Two list endpoints that
 * disagree about whether the field is {@code totalElements} or {@code total} is a thing every
 * client then has to know.
 *
 * <p><b>Not Spring's {@code Page} serialized directly.</b> That works and is tempting, and it
 * puts the whole {@code Pageable} — sort orders, offsets, {@code paged} flags — into the JSON,
 * where it becomes a contract we did not choose and cannot change without breaking callers.
 * Spring itself warns about serializing it. This is the six fields that were asked for.
 *
 * <p>{@code hasNext} and {@code hasPrevious} are included even though a caller could derive them
 * from {@code page} and {@code totalPages}. Deriving it is where the off-by-one lives.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    /** Wraps a Spring page, mapping each row through {@code toDto}. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> toDto) {
        return new PageResponse<>(
                page.getContent().stream().map(toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
