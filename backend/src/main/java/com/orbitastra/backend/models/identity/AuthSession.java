package com.orbitastra.backend.models.identity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.identity.enums.SessionEndReason;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One person signed in on one device.
 *
 * <p>A new row is made every time somebody signs in, and it is closed when they
 * sign out, when it runs past its end time, or when somebody ends it for them.
 *
 * <p>This is what makes "sign out everywhere" possible. Without a record of the
 * open sessions there is nothing to close, so a parent whose phone was stolen has
 * no way to shut the thief out. It also lets a person see the list of devices
 * they are signed in on, which is often how they notice something is wrong.
 *
 * <p>The sign-in token itself is never saved. Only {@code refreshTokenHash} is
 * kept, so a stolen copy of this collection cannot be used to sign in as anybody.
 * The same reasoning as the password on UserAccount: we keep enough to check a
 * value somebody gives us, and not enough to produce it ourselves.
 *
 * <p>{@code lastSeenAt} is written often, on most requests, and is the only field
 * here that changes regularly. Everything else is written once.
 *
 * <p>Closed rows are kept for a while rather than deleted, because "which device
 * was that done from" is a question that gets asked after something goes wrong,
 * not before. How long to keep them is a service setting, not a model one.
 *
 * <p>The service checks that a session is refused once the account is suspended
 * or closed, and that a session which started before
 * {@code UserAccount.passwordChangedAt} is treated as closed even if its own end
 * time has not arrived.
 */
@Document(collection = "auth_sessions")
@CompoundIndexes({
        @CompoundIndex(
                name = "session_refresh_token_uniq",
                def = "{'refreshTokenHash': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_account_session_active_idx",
                def = "{'schoolId': 1, 'userAccountDocsId': 1, 'active': 1, 'lastSeenAt': -1}"),
        @CompoundIndex(
                name = "session_expiry_idx",
                def = "{'active': 1, 'expiresAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession extends SchoolBase {

    // Links to UserAccount.id. Example: "67b11223dc3f7d0011223344"
    @NotBlank
    private String userAccountDocsId;

    // One-way hash of the refresh token. The token itself is never saved.
    // Not scoped by school, because a token has to be unique everywhere.
    // Example: "sha256:8e1b47c2a90f5d3e6b7c4a2f1d908e35"
    @NotBlank
    private String refreshTokenHash;

    // Name of the device, shown to the person on their list of sign-ins.
    // Example: "Priya's iPhone"
    private String deviceName;

    // Browser or app that signed in, kept as it was sent.
    // Example: "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X)"
    private String userAgent;

    // Address the sign-in came from, used when looking into a problem.
    // Example: "203.0.113.45"
    private String ipAddress;

    // When the person signed in. Example: 2026-08-14T05:40:00Z
    @NotNull
    private Instant authenticatedAt;

    // Last time this session was used. Written on most requests.
    // Example: 2026-08-14T08:12:00Z
    @NotNull
    private Instant lastSeenAt;

    // When the session stops working on its own. Example: 2026-08-28T05:40:00Z
    @NotNull
    private Instant expiresAt;

    // Whether the session can still be used. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // When the session was closed. Null while it is still open.
    // Example: 2026-08-14T18:30:00Z
    private Instant endedAt;

    // Why it was closed. Null while it is still open.
    // Example: SessionEndReason.SIGNED_OUT
    private SessionEndReason endReason;

    // Links to the staff identity that closed it, when an administrator did.
    // Example: "67aa15d9dc3f7d0055555555"
    private String endedByDocsId;
}
