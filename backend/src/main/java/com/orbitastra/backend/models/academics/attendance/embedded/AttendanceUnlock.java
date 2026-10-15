package com.orbitastra.backend.models.academics.attendance.embedded;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One time a locked AttendanceSession was opened again, and why.
 *
 * <p>Kept as a list on the session rather than one field, so a register unlocked twice keeps both
 * reasons — re-opening a final register is exactly what somebody asks about later. The lock it
 * undid is copied here too, because unlocking clears it from the session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUnlock {

    // Why it was opened again. Example: "Ayan was marked absent by mistake"
    @NotBlank
    private String reason;

    // Example: 2026-10-16T05:30:00Z
    @NotNull
    private Instant unlockedAt;

    // Links to the Staff.id who unlocked it, when the caller was signed in as staff.
    // Example: "67aa15d9dc3f7d0044444444"
    private String unlockedByDocsId;

    // When the lock being undone was made. Example: 2026-10-15T09:00:00Z
    private Instant previousLockedAt;

    // Links to the Staff.id who made the lock being undone.
    // Example: "67aa15d9dc3f7d0055555555"
    private String previousLockedByDocsId;
}
