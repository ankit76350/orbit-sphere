package com.orbitastra.backend.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import com.orbitastra.backend.common.audit.SystemActors;

/**
 * Supplies the value Spring Data writes into {@code createdByDocsId} and
 * {@code updatedByDocsId}.
 *
 * <p>{@code @EnableMongoAuditing} on BackendApplication turns the auditing hooks on, but it has
 * no way to know who the current user is. Without an AuditorAware bean those two fields are
 * simply left null on every document in the system.
 *
 * <p>Right now there is no authentication, so this always returns the PLATFORM sentinel. **That
 * is correct for the endpoints that exist** — provisioning a school genuinely has no tenant user
 * behind it.
 *
 * <p>When sessions are built, this becomes: the authenticated UserAccount id when there is one,
 * PLATFORM when a platform operator is acting, and ANONYMOUS where a feature promised not to
 * record the author. **The one thing it must never do is fall back to a logged-in user for a
 * write that promised anonymity** — see SystemActors.
 */
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Always the platform sentinel until authentication exists. Deliberately not null:
        // a null author on a row is indistinguishable from a bug.
        return () -> Optional.of(SystemActors.PLATFORM);
    }
}
