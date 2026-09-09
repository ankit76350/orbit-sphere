package com.orbitastra.backend.common.web;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.common.error.exception.ApiException;

/**
 * Turning {@code ?page=&size=&sort=} into a {@link Pageable}, the same way every time.
 *
 * <p><b>Why this is shared rather than written per endpoint.</b> #8 had these forty lines inline,
 * #28 needed the identical forty, and #30, #31 and #32 are all list endpoints too. Five copies
 * of "is the page negative" is five chances for one of them to answer {@code 200} to
 * {@code ?page=-1}, or to spell the error code differently, and a client cannot handle an error
 * format that varies by endpoint.
 *
 * <p>The four refusals — {@code INVALID_PAGE}, {@code INVALID_PAGE_SIZE},
 * {@code INVALID_SORT_FIELD}, {@code INVALID_SORT_DIRECTION} — and their exact wording are the
 * ones #8 already returned before this class existed, so nothing about that endpoint's contract
 * changed when it moved here.
 *
 * <p><b>Refused, not clamped.</b> {@code ?size=5000} is a 400 rather than a silent 100: a caller
 * who asked for five thousand rows and got a hundred has been given a page they will read as the
 * whole answer, and the mistake surfaces as missing data somewhere much later.
 */
public final class Paging {

    /** Used when the caller does not say. Twenty rows is a screen without a scrollbar. */
    public static final int DEFAULT_SIZE = 20;

    /** The most a caller may ask for at once, refused above rather than clamped. */
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    /**
     * The page a caller asked for, validated.
     *
     * @param sortable      lowercase name a caller may type -> the field on the document. An
     *                      allow-list rather than a pass-through, so nobody can order by a field
     *                      with no index behind it, and nobody can probe the document's shape by
     *                      guessing field names.
     * @param sortableNames the same names spelled as they should be typed, for the refusal
     * @param fallback      the default order, and also the <b>tiebreaker</b> appended under
     *                      whatever the caller asked for. It has to end in something unique or
     *                      pagination is not stable: two rows that compare equal on the sort key
     *                      may come back in either order, so one of them can appear on page one
     *                      and again on page two while another is never seen at all.
     */
    public static Pageable of(Integer page, Integer size, String sort,
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
                resolveSort(sort, sortable, sortableNames, fallback));
    }

    /**
     * The order, which is the fallback unless the caller named a field.
     *
     * <p>A caller's field goes <b>first</b> and the fallback follows it, so the fallback keeps
     * doing its second job as the tiebreaker even when it is no longer the primary order.
     *
     * <p><b>A field the caller named is removed from the tail, and that is not tidiness.</b> A
     * Mongo sort is a document, and a document cannot hold the same key twice — the driver keeps
     * the last one. So appending a fallback of {@code currentPeriodStart DESC} under a caller's
     * {@code currentPeriodStart ASC} produces
     * {@code {currentPeriodStart: 1, currentPeriodStart: -1}}, which sorts <b>descending</b>: the
     * direction the caller asked for is silently discarded.
     *
     * <p>It is not a hypothetical. Before this filter existed, {@code ?sort=planCode,desc} on the
     * plan catalogue returned plans in <i>ascending</i> code order — the fallback's own
     * {@code planCode ASC} overwrote it — and had done since that endpoint was written. The same
     * shape as the duplicate-key trap in the subscription date filters, and it fails just as
     * quietly.
     */
    private static Sort resolveSort(String sort, Map<String, String> sortable,
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
