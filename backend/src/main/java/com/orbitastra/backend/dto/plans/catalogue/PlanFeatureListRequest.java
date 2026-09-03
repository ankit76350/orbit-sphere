package com.orbitastra.backend.dto.plans.catalogue;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The whole feature list, for endpoint #3.
 *
 * <p><b>A wrapper around the list rather than a bare array</b>, for the same reason the holiday
 * calendar has one: Spring reports a bad element of a bare array as a
 * {@code HandlerMethodValidationException} rather than the exception every other endpoint
 * produces, and the caller was handed a Java method signature and an error count instead of the
 * field that was wrong. As an object it validates like everything else.
 *
 * <p>{@code features} is required. An empty list is how a plan's features are cleared; a body of
 * {@code &#123;&#125;} is refused rather than quietly meaning the same thing.
 */
public record PlanFeatureListRequest(

        /** Each feature once. The same code twice is refused. */
        @NotNull @Valid List<PlanFeatureRequest> features) {
}
