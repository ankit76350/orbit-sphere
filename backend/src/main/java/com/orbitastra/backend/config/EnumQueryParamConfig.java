package com.orbitastra.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.SchoolLocale;

/**
 * Teaches query parameters to accept the same spellings a request body does.
 *
 * <p><b>Why this has to exist.</b> Jackson reads an enum from a body through its
 * {@code @JsonCreator} — so {@code "in"} in a body becomes {@link CountryCode#IN}. Spring's own
 * converter for a query parameter does not use that creator: it matches the constant name
 * exactly, so {@code ?nationalityCode=in} failed while the same value in a body succeeded.
 *
 * <p>Two boundaries disagreeing about what a country is, in the same request shape, is the kind
 * of thing a caller reports as "the filter is broken". Caught 2026-09-15 when
 * {@code nationalityCode} stopped being a free string.
 *
 * <p><b>They delegate to the enum's own factory</b> rather than re-implementing the parse, so the
 * refusal message is identical on both paths — one sentence naming the bad value, not a wall of
 * 249 accepted constants.
 */
@Configuration
public class EnumQueryParamConfig {

    /** {@code ?nationalityCode=in} and {@code =IN} both resolve; anything else is refused. */
    @Bean
    Converter<String, CountryCode> countryCodeQueryParamConverter() {
        return CountryCode::fromCode;
    }

    /** {@code ?locale=en-IN}, the tag rather than the constant name. */
    @Bean
    Converter<String, SchoolLocale> schoolLocaleQueryParamConverter() {
        return SchoolLocale::fromTag;
    }
}
