package com.orbitastra.backend.common.web;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.common.error.exception.ApiException;

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
 *
 * <p><b>It also reads the request that asked for the page</b> — see {@link #pageableOf} — so both
 * halves of pagination live in the one type every list endpoint already goes through, rather than
 * being copied into each service.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    /** Used when the caller does not say. Twenty rows is a screen without a scrollbar. */
    public static final int DEFAULT_SIZE = 20;

    /** The most a caller may ask for at once, refused above rather than clamped. */
    public static final int MAX_SIZE = 100;

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

    /**
     * The other direction: {@code ?page=&size=&sort=} turned into a {@link Pageable}.
     *
     * <p><b>Here rather than in a class of its own, and shared rather than written per endpoint.</b>
     * The plan catalogue had these lines inline, the subscription history needed the identical
     * ones, and #30 and #32 are list endpoints too. Copies of "is the page negative" is one
     * chance per copy to answer {@code 200} to {@code ?page=-1} or to spell the error code
     * differently, and a client cannot handle an error format that varies by endpoint. It sits on
     * this record because this is already the pagination type both ends of the call go through.
     *
     * <p>The four refusals — {@code INVALID_PAGE}, {@code INVALID_PAGE_SIZE},
     * {@code INVALID_SORT_FIELD}, {@code INVALID_SORT_DIRECTION} — and their exact wording are
     * the ones the plan catalogue already returned, so nothing about its contract changed.
     *
     * <p><b>Refused, not clamped.</b> {@code ?size=5000} is a 400 rather than a silent 100: a
     * caller who asked for five thousand rows and got a hundred has been handed a page they will
     * read as the whole answer, and the mistake surfaces as missing data much later.
     *
     * @param sortable      lowercase name a caller may type -> the field on the document. An
     *                      allow-list rather than a pass-through, so nobody can order by a field
     *                      with no index behind it, and nobody can probe the document's shape by
     *                      guessing field names.
     * @param sortableNames the same names spelled as they should be typed, for the refusal
     * @param fallback      the default order, and also the <b>tiebreaker</b> appended under
     *                      whatever the caller asked for. It has to end in something unique or
     *                      pagination is not stable: two rows that compare equal on the sort key
     *                      may come back in either order, so one can appear on page one and again
     *                      on page two while another is never seen at all.
     */
    public static Pageable pageableOf(Integer page, Integer size, String sort,
            Map<String, String> sortable, String sortableNames, Sort fallback) {

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw ApiException.badRequest(
                    "INVALID_PAGE",
                    "page cannot be negative. Received: " + resolvedPage);
        }

        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw ApiException.badRequest(
                    "INVALID_PAGE_SIZE",
                    "size must be between 1 and " + MAX_SIZE + ". Received: " + resolvedSize);
        }

        return PageRequest.of(resolvedPage, resolvedSize,
                sortOf(sort, sortable, sortableNames, fallback));
    }

    /**
     * The order, which is the fallback unless the caller named a field.
     *
     * <p>A caller's field goes <b>first</b> and the fallback follows it, so the fallback keeps
     * doing its second job as the tiebreaker even when it is no longer the primary order.
     *
     * <p><b>A field the caller named is removed from the tail, and that is not tidiness.</b> A
     * Mongo sort is a document, and a document cannot hold the same key twice — the driver keeps
     * the last one. So appending a fallback of {@code planCode ASC} under a caller's
     * {@code planCode DESC} produces {@code {planCode: -1, planCode: 1}}, which sorts
     * <b>ascending</b>: the direction the caller asked for is silently discarded.
     *
     * <p>It is not hypothetical. Before this filter existed, {@code ?sort=planCode,desc} on the
     * plan catalogue returned plans in <i>ascending</i> code order, and had done since that
     * endpoint was written.
     */
    private static Sort sortOf(String sort, Map<String, String> sortable,
            String sortableNames, Sort fallback) {

        if (sort == null || sort.isBlank()) {
            return fallback;
        }

        String[] parts = sort.split(",");
        String requested = parts[0].trim();
        String field = sortable.get(requested.toLowerCase());

        if (field == null) {
            throw ApiException.badRequest(
                    "INVALID_SORT_FIELD",
                    "'" + requested + "' cannot be sorted on. Allowed: " + sortableNames + ".");
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length > 1 && !parts[1].isBlank()) {
            String requestedDirection = parts[1].trim();

            if (requestedDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            } else if (!requestedDirection.equalsIgnoreCase("asc")) {
                throw ApiException.badRequest(
                        "INVALID_SORT_DIRECTION",
                        "'" + requestedDirection + "' is not a direction. Use asc or desc.");
            }
        }

        // Only the orders the caller did NOT name, so no key is emitted twice.
        List<Sort.Order> tail = fallback.stream()
                .filter(order -> !order.getProperty().equals(field))
                .toList();

        Sort primary = Sort.by(direction, field);

        return tail.isEmpty() ? primary : primary.and(Sort.by(tail));
    }
}
