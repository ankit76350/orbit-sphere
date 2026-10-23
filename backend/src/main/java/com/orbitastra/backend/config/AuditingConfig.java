package com.orbitastra.backend.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import com.orbitastra.backend.common.audit.SystemActors;
import com.orbitastra.backend.common.current.CurrentUserResolver;
import com.orbitastra.backend.common.error.exception.ApiException;

/**
 * Supplies the value Spring Data writes into {@code createdByDocsId} and
 * {@code updatedByDocsId}.
 *
 * <p>{@code @EnableMongoAuditing} on BackendApplication turns the auditing hooks on, but it has
 * no way to know who the current user is. Without an AuditorAware bean those two fields are
 * simply left null on every document in the system.
 *
 * <h2>Since 2026-09-19 this reads the signed-in staff member</h2>
 *
 * <p>The {@code staffDocsId} claim of the {@code idtoken} cookie, via {@link CurrentUserResolver},
 * falling back to the PLATFORM sentinel when nobody is named. Before this, it returned PLATFORM
 * unconditionally — which is why every row written through the API carried
 * {@code createdByDocsId: SYSTEM_PLATFORM} no matter who was signed in.
 *
 * <p><b>The author is therefore unauthenticated, caller-supplied data.</b> Anybody can mint a
 * token naming any {@code staffDocsId}, so this records who the caller <i>said</i> they were, not
 * who they are. It is written into a permanent audit field that nothing on screen displays. That is
 * the correct behaviour for a system with no authentication, and it stops being acceptable the
 * moment these fields are used to answer "who did this" for any purpose that matters.
 *
 * <p><b>The staff id is not checked to exist here.</b> {@link CurrentUserResolver#staffDocsIdOrNull}
 * reads the claim without loading the document, deliberately: auditing runs inside the write, and
 * an extra query on every insert to validate a field nothing reads back would cost more than it is
 * worth. A nonexistent id stored here is a consequence of the token being unauthenticated anyway.
 *
 * <p><b>Known wrinkle: platform endpoints.</b> PLATFORM means "no tenant user was behind this
 * write", and provisioning a school is why it exists. If somebody signs in as a school's staff
 * member and then calls {@code POST /platform/schools}, that staff id is now recorded as the new
 * school's author, which is not what PLATFORM meant. An AuditorAware cannot see which controller is
 * running, so fixing it properly means the platform controllers setting the actor themselves.
 * Recorded rather than papered over.
 *
 * <p>ANONYMOUS is still unused. <b>The one thing this must never do is record a real actor for a
 * write that promised anonymity</b> — see SystemActors, and note that feedback is not built yet.
 */
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider(CurrentUserResolver currentUser) {
        return () -> Optional.of(resolveActor(currentUser));
    }

    /**
     * Whoever the cookie names, or the platform sentinel.
     *
     * <p><b>Never empty and never null.</b> Spring Data leaves the field untouched for an empty
     * Optional, and a null author on a row is indistinguishable from a bug.
     */
    private static String resolveActor(CurrentUserResolver currentUser) {
        try {
            String staffDocsId = currentUser.staffDocsIdOrNull();
            return staffDocsId != null ? staffDocsId : SystemActors.PLATFORM;
        } catch (ApiException unusableToken) {
            //! A BROKEN COOKIE IS NOT THIS BEAN'S ARGUMENT TO HAVE. Policing the token is the
            //! resolver's job, at the controller gate, where refusing is the right answer. Here it
            //! would mean a stale cookie left in a browser starts failing /platform writes that
            //! never read the cookie at all - an endpoint breaking because of a cookie it does not
            //! use. The auditor's only question is "who", and the honest answer is "could not tell".
            return SystemActors.PLATFORM;
        } catch (IllegalStateException outsideARequest) {
            //! AUDITING ALSO RUNS WHERE THERE IS NO REQUEST - startup seeding, and anything
            //! scheduled later. The injected HttpServletRequest is a scoped proxy that throws
            //! "No thread-bound request found" there, and an audit provider that throws would
            //! fail the write itself. No request genuinely means no tenant user, which is exactly
            //! what PLATFORM is for.
            return SystemActors.PLATFORM;
        }
    }
}
